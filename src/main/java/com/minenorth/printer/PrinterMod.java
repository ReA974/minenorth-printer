package com.minenorth.printer;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(PrinterMod.MODID)
public class PrinterMod {
    public static final String MODID = "minenorth_rp_printer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PrinterMod() {
        // config/printer-common.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PrinterConfig.SPEC, "printer-common.toml");
    }
}
