package com.minenorth_printer.block;

import com.minenorth_printer.ModRegistry;
import com.minenorth_printer.PrinterConfig;
import com.minenorth_printer.item.ChargeItem;
import com.minenorth_printer.item.UpgradeItem;
import com.minenorth_printer.item.UpgradeType;
import com.minenorth_printer.menu.PrinterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrinterBlockEntity extends BlockEntity implements MenuProvider {
    // ---- Slots ----
    public static final int SLOT_INK = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int UPGRADE_COUNT = UpgradeType.values().length; // 4
    public static final int SLOT_OUTPUT_START = SLOT_UPGRADE_START + UPGRADE_COUNT; // 6
    public static final int OUTPUT_COUNT = 6;
    public static final int SIZE = SLOT_OUTPUT_START + OUTPUT_COUNT; // 12

    // ---- Données synchronisées vers l'interface (16 bits signés max : 32767) ----
    public static final int D_INK = 0, D_INK_MAX = 1, D_ENERGY = 2, D_ENERGY_MAX = 3,
            D_PROGRESS = 4, D_REMAINING = 5, D_STATUS = 6, D_ENABLED = 7, D_CYCLE = 8,
            D_GAIN = 9, D_TOTAL_LO = 10, D_TOTAL_HI = 11, D_INK_COST = 12, D_ENERGY_COST = 13;
    public static final int DATA_COUNT = 14;

    private final ItemStackHandler items = createHandler(this::setChanged);

    private int ink;
    private int energy;
    private int progress;          // ticks écoulés dans le cycle en cours
    private boolean enabled = true;
    private long totalEuros;
    private PrinterStatus status = PrinterStatus.OFF;
    @Nullable private UUID owner;
    private String ownerName = "";

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case D_INK -> clamp(ink);
                case D_INK_MAX -> clamp(getInkCapacity());
                case D_ENERGY -> clamp(energy);
                case D_ENERGY_MAX -> clamp(getEnergyCapacity());
                case D_PROGRESS -> (int) Math.min(1000L, 1000L * progress / Math.max(1, getCycleTicks()));
                case D_REMAINING -> clamp((Math.max(0, getCycleTicks() - progress) + 19) / 20);
                case D_STATUS -> status.ordinal();
                case D_ENABLED -> enabled ? 1 : 0;
                case D_CYCLE -> clamp((getCycleTicks() + 19) / 20);
                case D_GAIN -> clamp(getEurosPerPrint());
                case D_TOTAL_LO -> (int) (Math.min(totalEuros, 0x3FFFFFFFL) & 0x7FFF);
                case D_TOTAL_HI -> (int) ((Math.min(totalEuros, 0x3FFFFFFFL) >> 15) & 0x7FFF);
                case D_INK_COST -> clamp(getInkCost());
                case D_ENERGY_COST -> clamp(getEnergyCost());
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.PRINTER_BE.get(), pos, state);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(v, Short.MAX_VALUE));
    }

    /** Inventaire avec les règles de slot (utilisé aussi côté client pour le menu). */
    public static ItemStackHandler createHandler(@Nullable Runnable onChange) {
        return new ItemStackHandler(SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                if (onChange != null) onChange.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isValidFor(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                if (slot == SLOT_INK || slot == SLOT_BATTERY) return 1;
                if (slot >= SLOT_UPGRADE_START && slot < SLOT_OUTPUT_START) return PrinterConfig.get(PrinterConfig.UPGRADE_MAX_LEVEL);
                return 64;
            }
        };
    }

    public static boolean isValidFor(int slot, ItemStack stack) {
        if (slot == SLOT_INK) return stack.getItem() instanceof ChargeItem c && c.kind == ChargeItem.Kind.INK;
        if (slot == SLOT_BATTERY) return stack.getItem() instanceof ChargeItem c && c.kind == ChargeItem.Kind.ENERGY;
        if (slot >= SLOT_UPGRADE_START && slot < SLOT_OUTPUT_START) {
            return stack.getItem() instanceof UpgradeItem u && u.type.ordinal() == slot - SLOT_UPGRADE_START;
        }
        return false; // sorties : on ne peut que retirer
    }

    // ---- Statistiques calculées avec les améliorations ----
    public int getLevel(UpgradeType t) {
        ItemStack s = items.getStackInSlot(SLOT_UPGRADE_START + t.ordinal());
        return s.getItem() instanceof UpgradeItem u && u.type == t
                ? Math.min(s.getCount(), PrinterConfig.get(PrinterConfig.UPGRADE_MAX_LEVEL)) : 0;
    }

    public int getInkCapacity() {
        return PrinterConfig.get(PrinterConfig.BASE_INK_CAPACITY)
                + getLevel(UpgradeType.INK) * PrinterConfig.get(PrinterConfig.INK_CAPACITY_PER_LEVEL);
    }

    public int getEnergyCapacity() {
        return PrinterConfig.get(PrinterConfig.ENERGY_CAPACITY);
    }

    public int getCycleTicks() {
        double factor = Math.max(0.1, 1.0 - getLevel(UpgradeType.SPEED) * PrinterConfig.get(PrinterConfig.SPEED_PER_LEVEL));
        return Math.max(20, (int) Math.round(PrinterConfig.get(PrinterConfig.PRINT_TIME_SECONDS) * 20 * factor));
    }

    public int getInkCost() {
        return PrinterConfig.get(PrinterConfig.INK_PER_PRINT);
    }

    public int getEnergyCost() {
        double factor = Math.max(0.0, 1.0 - getLevel(UpgradeType.EFFICIENCY) * PrinterConfig.get(PrinterConfig.EFFICIENCY_PER_LEVEL));
        return Math.max(1, (int) Math.round(PrinterConfig.get(PrinterConfig.ENERGY_PER_PRINT) * factor));
    }

    public int getEurosPerPrint() {
        return PrinterConfig.get(PrinterConfig.EUROS_PER_PRINT)
                + getLevel(UpgradeType.YIELD) * PrinterConfig.get(PrinterConfig.YIELD_EUROS_PER_LEVEL);
    }

    // ---- Tick serveur ----
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrinterBlockEntity be) {
        be.tick(level, pos, state);
    }

    /** Côté client : démarre le bruit de fonctionnement quand l'imprimante est allumée (bloc LIT). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, PrinterBlockEntity be) {
        if (state.getValue(PrinterBlock.LIT)) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.minenorth_printer.client.PrinterSounds.ensurePlaying(be));
        }
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean dirty = false;
        dirty |= pullFrom(SLOT_INK, true);
        dirty |= pullFrom(SLOT_BATTERY, false);

        List<ItemStack> money = makeMoney(getEurosPerPrint());
        PrinterStatus next;
        if (!enabled) next = PrinterStatus.OFF;
        else if (money.isEmpty()) next = PrinterStatus.CONFIG_ERROR;
        else if (ink < getInkCost()) next = PrinterStatus.NO_INK;
        else if (energy < getEnergyCost()) next = PrinterStatus.NO_ENERGY;
        else if (!insertOutputs(money, true)) next = PrinterStatus.OUTPUT_FULL;
        else next = PrinterStatus.RUNNING;
        status = next;

        if (status == PrinterStatus.RUNNING) {
            progress++;
            dirty = true;
            if (progress >= getCycleTicks()) {
                progress = 0;
                ink -= getInkCost();
                energy -= getEnergyCost();
                insertOutputs(money, false);
                totalEuros += getEurosPerPrint();
                if (PrinterConfig.get(PrinterConfig.PRINT_SOUND)) {
                    level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.6F, 1.0F);
                }
            }
        }

        boolean lit = status == PrinterStatus.RUNNING;
        if (state.getValue(PrinterBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(PrinterBlock.LIT, lit), 3);
        }
        // Sécurité : certaines manipulations d'inventaire modifient les piles sur place sans notifier
        if (dirty || level.getGameTime() % 20 == 0) setChanged();
    }

    /** Transfère progressivement le contenu d'une cartouche/batterie dans l'imprimante. */
    private boolean pullFrom(int slot, boolean isInk) {
        ItemStack stack = items.getStackInSlot(slot);
        if (!(stack.getItem() instanceof ChargeItem refill)) return false;
        boolean destroyEmpty = PrinterConfig.get(PrinterConfig.DESTROY_EMPTY_REFILLS);
        int charge = refill.getCharge(stack);
        if (charge <= 0) {
            if (destroyEmpty) {
                items.setStackInSlot(slot, ItemStack.EMPTY);
                return true;
            }
            return false;
        }
        int current = isInk ? ink : energy;
        int max = isInk ? getInkCapacity() : getEnergyCapacity();
        int move = Math.min(Math.min(max - current, charge), PrinterConfig.get(PrinterConfig.TRANSFER_PER_TICK));
        if (move <= 0) return false;

        ItemStack updated = stack.copy();
        updated.setCount(1);
        refill.setCharge(updated, charge - move);
        if (destroyEmpty && charge - move <= 0) updated = ItemStack.EMPTY;
        items.setStackInSlot(slot, updated);
        if (isInk) ink += move; else energy += move;
        return true;
    }

    /** Découpe un montant en billets selon la config. Liste vide si aucun billet valide. */
    public static List<ItemStack> makeMoney(int euros) {
        List<ItemStack> out = new ArrayList<>();
        for (PrinterConfig.Denomination d : PrinterConfig.denominations()) {
            int n = euros / d.euros();
            euros %= d.euros();
            int maxStack = new ItemStack(d.item()).getMaxStackSize();
            while (n > 0) {
                int c = Math.min(n, maxStack);
                out.add(new ItemStack(d.item(), c));
                n -= c;
            }
        }
        return out;
    }

    /** Range les billets dans les slots de sortie. En simulation, ne modifie rien. */
    private boolean insertOutputs(List<ItemStack> stacks, boolean simulate) {
        ItemStack[] slots = new ItemStack[OUTPUT_COUNT];
        for (int i = 0; i < OUTPUT_COUNT; i++) slots[i] = items.getStackInSlot(SLOT_OUTPUT_START + i).copy();

        for (ItemStack in : stacks) {
            ItemStack rem = in.copy();
            for (int i = 0; i < OUTPUT_COUNT && !rem.isEmpty(); i++) {
                if (!slots[i].isEmpty() && ItemStack.isSameItemSameTags(slots[i], rem)) {
                    int room = Math.min(slots[i].getMaxStackSize(), 64) - slots[i].getCount();
                    int m = Math.min(room, rem.getCount());
                    if (m > 0) {
                        slots[i].grow(m);
                        rem.shrink(m);
                    }
                }
            }
            for (int i = 0; i < OUTPUT_COUNT && !rem.isEmpty(); i++) {
                if (slots[i].isEmpty()) {
                    slots[i] = rem.copy();
                    rem = ItemStack.EMPTY;
                }
            }
            if (!rem.isEmpty()) return false;
        }
        if (!simulate) {
            for (int i = 0; i < OUTPUT_COUNT; i++) items.setStackInSlot(SLOT_OUTPUT_START + i, slots[i]);
        }
        return true;
    }

    // ---- Actions ----
    public void toggle() {
        enabled = !enabled;
        setChanged();
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s.copy());
        }
    }

    // ---- MenuProvider ----
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.minenorth_printer.printer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PrinterMenu(id, inv, worldPosition, items, data, ownerName, this);
    }

    // ---- Sauvegarde ----
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Ink", ink);
        tag.putInt("Energy", energy);
        tag.putInt("Progress", progress);
        tag.putBoolean("Enabled", enabled);
        tag.putLong("TotalEuros", totalEuros);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        ink = tag.getInt("Ink");
        energy = tag.getInt("Energy");
        progress = tag.getInt("Progress");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        totalEuros = tag.getLong("TotalEuros");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }
}
