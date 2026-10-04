package com.minenorth_printer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Configuration SERVER (world/serverconfig/minenorth_printer-server.toml).
 * Forge la synchronise aux clients à la connexion, donc les infobulles affichent les bonnes valeurs.
 * Les plafonds des valeurs restent sous 32 767 car l'interface les synchronise sur 16 bits.
 */
public final class PrinterConfig {
    private PrinterConfig() {}

    public static final ForgeConfigSpec SPEC;

    // Imprimante
    public static final ForgeConfigSpec.IntValue PRINT_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue INK_PER_PRINT;
    public static final ForgeConfigSpec.IntValue ENERGY_PER_PRINT;
    public static final ForgeConfigSpec.IntValue BASE_INK_CAPACITY;
    public static final ForgeConfigSpec.IntValue ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue TRANSFER_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue ONLY_OWNER_CAN_OPEN;
    public static final ForgeConfigSpec.BooleanValue PRINT_SOUND;
    public static final ForgeConfigSpec.BooleanValue DESTROY_EMPTY_REFILLS;
    public static final ForgeConfigSpec.BooleanValue RUNNING_SOUND;
    public static final ForgeConfigSpec.DoubleValue RUNNING_SOUND_VOLUME;

    // Argent
    public static final ForgeConfigSpec.IntValue EUROS_PER_PRINT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DENOMINATIONS;

    // Recharges
    public static final ForgeConfigSpec.IntValue INK_CARTRIDGE;
    public static final ForgeConfigSpec.IntValue INK_CARTRIDGE_LARGE;
    public static final ForgeConfigSpec.IntValue BATTERY;
    public static final ForgeConfigSpec.IntValue BATTERY_LARGE;

    // Améliorations
    public static final ForgeConfigSpec.IntValue UPGRADE_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SPEED_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue INK_CAPACITY_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue YIELD_EUROS_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue EFFICIENCY_PER_LEVEL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Fonctionnement de l'imprimante").push("printer");
        PRINT_TIME_SECONDS = b.comment("Durée d'une impression (secondes), avant amélioration de vitesse")
                .defineInRange("printTimeSeconds", 30, 1, 3600);
        INK_PER_PRINT = b.comment("Encre consommée par impression")
                .defineInRange("inkPerPrint", 50, 1, 30000);
        ENERGY_PER_PRINT = b.comment("Énergie consommée par impression, avant amélioration d'efficacité")
                .defineInRange("energyPerPrint", 100, 1, 30000);
        BASE_INK_CAPACITY = b.comment("Capacité du réservoir d'encre sans amélioration")
                .defineInRange("baseInkCapacity", 1000, 100, 10000);
        ENERGY_CAPACITY = b.comment("Capacité de la réserve d'énergie")
                .defineInRange("energyCapacity", 2000, 100, 30000);
        TRANSFER_PER_TICK = b.comment("Quantité transférée par tick depuis une cartouche/batterie vers l'imprimante")
                .defineInRange("transferPerTick", 25, 1, 30000);
        ONLY_OWNER_CAN_OPEN = b.comment("Si true, seul le joueur qui a posé l'imprimante (ou un op) peut l'ouvrir")
                .define("onlyOwnerCanOpen", false);
        PRINT_SOUND = b.comment("Joue un son à chaque impression")
                .define("printSound", true);
        RUNNING_SOUND = b.comment("Bruit de fonctionnement en boucle quand l'imprimante tourne (audible à ~16 blocs)")
                .define("runningSound", true);
        RUNNING_SOUND_VOLUME = b.comment("Volume du bruit de fonctionnement (0.0 - 1.0)")
                .defineInRange("runningSoundVolume", 0.6, 0.0, 1.0);
        DESTROY_EMPTY_REFILLS = b.comment("Si true, les cartouches et batteries vides disparaissent ; sinon elles restent dans le slot")
                .define("destroyEmptyRefills", false);
        b.pop();

        b.comment("Argent produit").push("money");
        EUROS_PER_PRINT = b.comment("Euros imprimés par cycle, avant amélioration de rendement")
                .defineInRange("eurosPerPrint", 50, 1, 10000);
        DENOMINATIONS = b.comment(
                        "Billets utilisés pour sortir l'argent, format \"modid:item=valeurEnEuros\".",
                        "Le montant est découpé du plus gros au plus petit billet ; le reste trop petit est perdu.")
                .defineListAllowEmpty(List.of("denominations"), () -> List.of(
                        "minenorth_eurobank:bill_500e=500",
                        "minenorth_eurobank:bill_200e=200",
                        "minenorth_eurobank:bill_100e=100",
                        "minenorth_eurobank:bill_50e=50",
                        "minenorth_eurobank:bill_20e=20",
                        "minenorth_eurobank:bill_10e=10",
                        "minenorth_eurobank:bill_5e=5"), o -> o instanceof String s && s.contains("="));
        b.pop();

        b.comment("Contenance des recharges").push("refills");
        INK_CARTRIDGE = b.defineInRange("inkCartridge", 500, 1, 1_000_000);
        INK_CARTRIDGE_LARGE = b.defineInRange("inkCartridgeLarge", 2000, 1, 1_000_000);
        BATTERY = b.defineInRange("battery", 1000, 1, 1_000_000);
        BATTERY_LARGE = b.defineInRange("batteryLarge", 4000, 1, 1_000_000);
        b.pop();

        b.comment("Améliorations (on empile les items dans leur slot pour monter de niveau)").push("upgrades");
        UPGRADE_MAX_LEVEL = b.comment("Niveau max = nombre d'items empilables dans un slot d'amélioration")
                .defineInRange("maxLevel", 3, 1, 4);
        SPEED_PER_LEVEL = b.comment("Réduction du temps d'impression par niveau (0.2 = -20 %)")
                .defineInRange("speedPerLevel", 0.20, 0.0, 0.25);
        INK_CAPACITY_PER_LEVEL = b.comment("Capacité d'encre ajoutée par niveau")
                .defineInRange("inkCapacityPerLevel", 1000, 0, 5000);
        YIELD_EUROS_PER_LEVEL = b.comment("Euros ajoutés par impression et par niveau")
                .defineInRange("yieldEurosPerLevel", 25, 0, 5000);
        EFFICIENCY_PER_LEVEL = b.comment("Réduction de l'énergie consommée par niveau (0.2 = -20 %)")
                .defineInRange("efficiencyPerLevel", 0.20, 0.0, 0.25);
        b.pop();

        SPEC = b.build();
    }

    /** Lecture sûre : renvoie la valeur par défaut si la config n'est pas encore chargée (menu principal...). */
    public static int get(ForgeConfigSpec.IntValue v) {
        try { return v.get(); } catch (Exception e) { return v.getDefault(); }
    }

    public static double get(ForgeConfigSpec.DoubleValue v) {
        try { return v.get(); } catch (Exception e) { return v.getDefault(); }
    }

    public static boolean get(ForgeConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (Exception e) { return v.getDefault(); }
    }

    // ---- Billets ----
    public record Denomination(Item item, int euros) {}

    private static List<? extends String> cachedRaw;
    private static List<Denomination> cachedDenoms = List.of();

    /** Billets valides, triés du plus gros au plus petit. */
    public static synchronized List<Denomination> denominations() {
        List<? extends String> raw;
        try { raw = DENOMINATIONS.get(); } catch (Exception e) { raw = DENOMINATIONS.getDefault(); }
        if (raw == cachedRaw) return cachedDenoms;
        List<Denomination> out = new ArrayList<>();
        for (String line : raw) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) continue;
            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            if (id == null) continue;
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) continue;
            try {
                int value = Integer.parseInt(parts[1].trim());
                if (value > 0) out.add(new Denomination(item, value));
            } catch (NumberFormatException ignored) {}
        }
        out.sort(Comparator.comparingInt(Denomination::euros).reversed());
        cachedRaw = raw;
        cachedDenoms = List.copyOf(out);
        return cachedDenoms;
    }
}
