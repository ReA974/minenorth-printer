package com.minenorth_printer.block;

public enum PrinterStatus {
    OFF("off", 0xFF8FA8E0),
    RUNNING("running", 0xFF5BE38A),
    NO_INK("no_ink", 0xFFFFE066),
    NO_ENERGY("no_energy", 0xFFFFE066),
    OUTPUT_FULL("output_full", 0xFFFFE066),
    CONFIG_ERROR("config_error", 0xFFFF6A9A);

    public final String id;
    public final int color;

    PrinterStatus(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String translationKey() {
        return "gui.minenorth_printer.status." + id;
    }

    public static PrinterStatus byIndex(int i) {
        PrinterStatus[] v = values();
        return i >= 0 && i < v.length ? v[i] : OFF;
    }
}
