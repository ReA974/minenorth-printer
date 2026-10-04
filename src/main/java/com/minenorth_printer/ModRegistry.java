package com.minenorth_printer;

import com.minenorth_printer.block.PrinterBlock;
import com.minenorth_printer.block.PrinterBlockEntity;
import com.minenorth_printer.item.ChargeItem;
import com.minenorth_printer.item.UpgradeItem;
import com.minenorth_printer.item.UpgradeType;
import com.minenorth_printer.menu.PrinterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MineNorthPrinter.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineNorthPrinter.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MineNorthPrinter.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MineNorthPrinter.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MineNorthPrinter.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MineNorthPrinter.MODID);

    // ---- Sons ----
    /** Bourdonnement en boucle quand l'imprimante tourne (portée 16 blocs). */
    public static final RegistryObject<SoundEvent> PRINTER_RUNNING = SOUNDS.register("printer_running",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(MineNorthPrinter.MODID, "printer_running"), 16.0F));

    // ---- Bloc ----
    public static final RegistryObject<PrinterBlock> PRINTER = BLOCKS.register("printer", () -> new PrinterBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(PrinterBlock.LIT) ? 6 : 0)));

    public static final RegistryObject<Item> PRINTER_ITEM = ITEMS.register("printer",
            () -> new BlockItem(PRINTER.get(), new Item.Properties()));

    // ---- Recharges ----
    public static final RegistryObject<ChargeItem> INK_CARTRIDGE = ITEMS.register("ink_cartridge",
            () -> new ChargeItem(ChargeItem.Kind.INK, () -> PrinterConfig.get(PrinterConfig.INK_CARTRIDGE)));
    public static final RegistryObject<ChargeItem> INK_CARTRIDGE_LARGE = ITEMS.register("ink_cartridge_large",
            () -> new ChargeItem(ChargeItem.Kind.INK, () -> PrinterConfig.get(PrinterConfig.INK_CARTRIDGE_LARGE)));
    public static final RegistryObject<ChargeItem> BATTERY = ITEMS.register("battery",
            () -> new ChargeItem(ChargeItem.Kind.ENERGY, () -> PrinterConfig.get(PrinterConfig.BATTERY)));
    public static final RegistryObject<ChargeItem> BATTERY_LARGE = ITEMS.register("battery_large",
            () -> new ChargeItem(ChargeItem.Kind.ENERGY, () -> PrinterConfig.get(PrinterConfig.BATTERY_LARGE)));

    // ---- Améliorations ----
    public static final Map<UpgradeType, RegistryObject<UpgradeItem>> UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        for (UpgradeType t : UpgradeType.values()) {
            UPGRADES.put(t, ITEMS.register("upgrade_" + t.id, () -> new UpgradeItem(t)));
        }
    }

    // ---- Block entity / menu ----
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PrinterBlockEntity>> PRINTER_BE = BLOCK_ENTITIES.register("printer",
            () -> BlockEntityType.Builder.of(PrinterBlockEntity::new, PRINTER.get()).build(null));

    public static final RegistryObject<MenuType<PrinterMenu>> PRINTER_MENU = MENUS.register("printer",
            () -> IForgeMenuType.create(PrinterMenu::new));

    // ---- Onglet créatif ----
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.minenorth_printer"))
            .icon(() -> new ItemStack(PRINTER_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(PRINTER_ITEM.get());
                out.accept(INK_CARTRIDGE.get());
                out.accept(INK_CARTRIDGE_LARGE.get());
                out.accept(BATTERY.get());
                out.accept(BATTERY_LARGE.get());
                for (UpgradeType t : UpgradeType.values()) out.accept(UPGRADES.get(t).get());
            })
            .build());
}
