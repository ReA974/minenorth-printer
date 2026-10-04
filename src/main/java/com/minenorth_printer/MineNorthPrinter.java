package com.minenorth_printer;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MineNorthPrinter.MODID)
public class MineNorthPrinter {
    public static final String MODID = "minenorth_printer";

    public MineNorthPrinter() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.BLOCKS.register(bus);
        ModRegistry.ITEMS.register(bus);
        ModRegistry.BLOCK_ENTITIES.register(bus);
        ModRegistry.MENUS.register(bus);
        ModRegistry.SOUNDS.register(bus);
        ModRegistry.TABS.register(bus);
        // Config SERVER : world/serverconfig/minenorth_printer-server.toml, synchronisée automatiquement aux clients
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PrinterConfig.SPEC);
    }
}
