package com.minenorth_printer.menu;

import com.minenorth_printer.ModRegistry;
import com.minenorth_printer.block.PrinterBlockEntity;
import com.minenorth_printer.block.PrinterStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

import static com.minenorth_printer.block.PrinterBlockEntity.*;

public class PrinterMenu extends AbstractContainerMenu {
    // Positions (coin haut-gauche de l'item, relatif à l'interface) — partagées avec l'écran
    public static final int INK_X = 8, INK_Y = 33;
    public static final int BATTERY_X = 8, BATTERY_Y = 69;
    public static final int UPGRADE_X = 8, UPGRADE_Y = 101;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 33;
    public static final int INV_X = 8, INV_Y = 133, HOTBAR_Y = 191;

    private final BlockPos pos;
    private final ContainerData data;
    private final String ownerName;
    private final ContainerLevelAccess access;
    @Nullable private final PrinterBlockEntity be;

    /** Constructeur client (ouvert via NetworkHooks). */
    public PrinterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), PrinterBlockEntity.createHandler(null),
                new SimpleContainerData(DATA_COUNT), buf.readUtf(), null);
    }

    public PrinterMenu(int id, Inventory inv, BlockPos pos, IItemHandler handler, ContainerData data,
                       String ownerName, @Nullable PrinterBlockEntity be) {
        super(ModRegistry.PRINTER_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.ownerName = ownerName;
        this.be = be;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        addSlot(new SlotItemHandler(handler, SLOT_INK, INK_X, INK_Y));
        addSlot(new SlotItemHandler(handler, SLOT_BATTERY, BATTERY_X, BATTERY_Y));
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            addSlot(new SlotItemHandler(handler, SLOT_UPGRADE_START + i, UPGRADE_X + i * 18, UPGRADE_Y));
        }
        for (int i = 0; i < OUTPUT_COUNT; i++) {
            addSlot(new SlotItemHandler(handler, SLOT_OUTPUT_START + i, OUTPUT_X + (i % 3) * 18, OUTPUT_Y + (i / 3) * 18));
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, INV_X + c * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    // ---- Lecture des données synchronisées ----
    public int get(int index) {
        return data.get(index);
    }

    public PrinterStatus getStatus() {
        return PrinterStatus.byIndex(data.get(D_STATUS));
    }

    public boolean isEnabled() {
        return data.get(D_ENABLED) != 0;
    }

    public long getTotalEuros() {
        return ((long) (data.get(D_TOTAL_HI) & 0x7FFF) << 15) | (data.get(D_TOTAL_LO) & 0x7FFF);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BlockPos getPos() {
        return pos;
    }

    // ---- Bouton marche/arrêt (paquet vanilla, aucun réseau custom) ----
    public static final int BUTTON_TOGGLE = 0;

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && be != null) {
            be.toggle();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistry.PRINTER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineEnd = SIZE;
        int invEnd = slots.size();

        if (index < machineEnd) {
            // Imprimante -> inventaire joueur (hotbar en priorité pour les billets)
            if (!moveItemStackTo(stack, machineEnd, invEnd, true)) return ItemStack.EMPTY;
        } else {
            // Inventaire -> slots d'entrée de l'imprimante (cartouche, batterie, améliorations)
            if (!moveItemStackTo(stack, 0, SLOT_OUTPUT_START, false)) {
                // sinon, échange inventaire <-> hotbar
                int hotbarStart = invEnd - 9;
                if (index < hotbarStart) {
                    if (!moveItemStackTo(stack, hotbarStart, invEnd, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, machineEnd, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        // set() (et pas seulement setChanged) pour que l'imprimante soit bien marquée à sauvegarder
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
