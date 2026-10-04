package com.minenorth_printer.item;

import net.minecraft.ChatFormatting;

public enum UpgradeType {
    SPEED("speed", ChatFormatting.YELLOW),
    INK("ink", ChatFormatting.AQUA),
    YIELD("yield", ChatFormatting.GREEN),
    EFFICIENCY("efficiency", ChatFormatting.GOLD);

    public final String id;
    public final ChatFormatting color;

    UpgradeType(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }
}
