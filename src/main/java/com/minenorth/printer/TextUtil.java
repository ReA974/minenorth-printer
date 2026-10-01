package com.minenorth.printer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class TextUtil {
    private TextUtil() {}

    /** Convertit les codes &a, &c... en codes couleur Minecraft. */
    public static Component color(String s) {
        return Component.literal(s.replace('&', '\u00a7'));
    }

    /** Retire les codes couleur pour comparer des noms d'items. */
    public static String strip(String s) {
        return s.replaceAll("(?i)[&\u00a7][0-9a-fk-or]", "").replaceAll("\\s+", " ").trim();
    }

    public static void msg(ServerPlayer p, String s) {
        p.sendSystemMessage(color(PrinterConfig.PREFIX.get() + " " + s));
    }

    public static String num(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
    }
}
