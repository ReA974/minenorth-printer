package com.minenorth.printer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public final class PrinterConfig {
    public static final ForgeConfigSpec SPEC;

    // ---- Blocs / items (format namespace:id) ----
    public static final ForgeConfigSpec.ConfigValue<String> PRINTER_BLOCK, BILL_ITEM, INK_ITEM,
            BATTERY_ITEM, CAPACITY_ITEM, SPEED_ITEM;

    // ---- Textes ----
    public static final ForgeConfigSpec.ConfigValue<String> PREFIX, MSG_NO_PERM, MSG_BREAK,
            MSG_BREAK_NO, MSG_BREAK_ADMIN;

    // ---- Batteries (reconnues par leur nom) ----
    public static final ForgeConfigSpec.ConfigValue<String> BATTERY_NAME_1, BATTERY_NAME_2, BATTERY_NAME_3;
    public static final ForgeConfigSpec.IntValue BATTERY_AMOUNT_1, BATTERY_AMOUNT_2, BATTERY_AMOUNT_3;

    // ---- Gameplay ----
    public static final ForgeConfigSpec.IntValue PERMISSION_LEVEL, DEFAULT_CAPACITY, CAPACITY_STEP,
            MAX_SPEED, INK_NEEDED, PRINT_STEPS, POWER_PER_STAGE, TIME_TO_BREAK, BREAK_RADIUS;

    // ---- Classes ----
    public static final ClassCfg SILVER, GOLD, PLATINE;
    public static final ClassCfg[] CLASSES;

    public static final class ClassCfg {
        public final String key;
        public final String color;
        public final ForgeConfigSpec.ConfigValue<String> itemName;
        public final ForgeConfigSpec.IntValue dropMin, dropMax, capLimit;

        ClassCfg(ForgeConfigSpec.Builder b, String key, String color, String name, int min, int max, int cap) {
            this.key = key;
            this.color = color;
            b.push(key);
            itemName = b.comment("Nom (renommage) de l'item Printer posé qui donne cette classe. Codes & acceptés, couleurs ignorées à la comparaison.")
                    .define("itemName", name);
            dropMin = b.comment("Nombre minimum de billets par impression").defineInRange("dropMin", min, 0, 1000);
            dropMax = b.comment("Nombre maximum de billets par impression").defineInRange("dropMax", max, 0, 1000);
            capLimit = b.comment("Capacité de batterie maximale (%) atteignable avec les upgrades").defineInRange("maxCapacity", cap, 100, 100000);
            b.pop();
        }
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Blocs et items (format namespace:id)").push("items");
        PRINTER_BLOCK = b.comment("Bloc Printer")
                .define("printerBlock", "yuushya:mini_printer");
        BILL_ITEM = b.comment("Item donné à la fin d'une impression")
                .define("billItem", "bubusteinmoneymod:fifty_euros");
        INK_ITEM = b.comment("Item d'encre (5 clics = lancement)")
                .define("inkItem", "minecraft:ink_sac");
        BATTERY_ITEM = b.comment("Item de batterie (le nom détermine la recharge, voir [batteries])")
                .define("batteryItem", "mts:oamp.auxiliary_battery");
        CAPACITY_ITEM = b.comment("Item qui augmente la capacité max de batterie")
                .define("capacityUpgradeItem", "minecraft:redstone");
        SPEED_ITEM = b.comment("Item qui augmente la vitesse")
                .define("speedUpgradeItem", "minecraft:sugar");
        b.pop();

        b.comment("Textes (codes couleur avec &)").push("messages");
        PREFIX = b.define("prefix", "&a[&cPrinter&a]");
        MSG_NO_PERM = b.define("noPermission", "&4Tu n'as pas la permission !");
        MSG_BREAK = b.define("breakOk", "&aTu as cassé un printer !");
        MSG_BREAK_NO = b.define("breakNotOwner", "&aTu ne peux pas casser le Printer !");
        MSG_BREAK_ADMIN = b.define("breakAdminHint", "&aFait &c/Printer Break pour casser un Printer");
        b.pop();

        b.comment("Batteries : reconnues par le nom de l'item (couleurs ignorées)").push("batteries");
        BATTERY_NAME_1 = b.define("name1", "&b[&cBatterie&b] &7 100%");
        BATTERY_AMOUNT_1 = b.defineInRange("amount1", 100, 1, 100000);
        BATTERY_NAME_2 = b.define("name2", "&b[&cBatterie&b] &7 50%");
        BATTERY_AMOUNT_2 = b.defineInRange("amount2", 50, 1, 100000);
        BATTERY_NAME_3 = b.define("name3", "&b[&cBatterie&b] &7 25%");
        BATTERY_AMOUNT_3 = b.defineInRange("amount3", 25, 1, 100000);
        b.pop();

        b.comment("Réglages").push("gameplay");
        PERMISSION_LEVEL = b.comment("Niveau d'op requis pour /printer (2 = op). Sous Arclight, les permissions Bukkit/LuckPerms ne sont pas lues.")
                .defineInRange("commandPermissionLevel", 2, 0, 4);
        DEFAULT_CAPACITY = b.comment("Capacité de batterie de départ (%)").defineInRange("defaultCapacity", 100, 1, 100000);
        CAPACITY_STEP = b.comment("Capacité ajoutée par upgrade (%)").defineInRange("capacityStep", 50, 1, 100000);
        MAX_SPEED = b.comment("Vitesse maximale (x)").defineInRange("maxSpeed", 10, 1, 100);
        INK_NEEDED = b.comment("Nombre d'encres pour lancer une impression").defineInRange("inkNeeded", 5, 1, 64);
        PRINT_STEPS = b.comment("Nombre d'étapes d'une impression (1 étape = 1 s à vitesse 1x). Multiple de 10 conseillé.")
                .defineInRange("printSteps", 60, 10, 6000);
        POWER_PER_STAGE = b.comment("Batterie consommée à chaque palier de 10 %").defineInRange("powerPerStage", 10, 0, 1000);
        TIME_TO_BREAK = b.comment("Temps pour détruire un printer en secondes").defineInRange("timeToBreak",20,10,60);
        BREAK_RADIUS = b.comment("Nombre de blocs autours du printer pour informer qu'il va être detruit").defineInRange("timeToBreak",10,10,60);
        b.pop();

        b.comment("Classes de Printer").push("classes");
        SILVER = new ClassCfg(b, "Silver", "&8", "&7Printer &f[&8Silver&f]", 1, 3, 200);
        GOLD = new ClassCfg(b, "Gold", "&6", "&7Printer &f[&6Gold&f]", 3, 6, 300);
        PLATINE = new ClassCfg(b, "Platine", "&b", "&7Printer &f[&bPlatine&f]", 6, 10, 300);
        b.pop();
        CLASSES = new ClassCfg[]{SILVER, GOLD, PLATINE};

        SPEC = b.build();
    }

    private PrinterConfig() {}

    // ------------------------------------------------------------------

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return Items.AIR;
        Item it = ForgeRegistries.ITEMS.getValue(rl);
        return it == null ? Items.AIR : it;
    }

    public static Block block() {
        ResourceLocation rl = ResourceLocation.tryParse(PRINTER_BLOCK.get());
        if (rl == null) return Blocks.AIR;
        Block bl = ForgeRegistries.BLOCKS.getValue(rl);
        return bl == null ? Blocks.AIR : bl;
    }

    public static boolean isPrinterBlock(BlockState state) {
        Block b = block();
        return b != Blocks.AIR && state.is(b);
    }

    public static boolean matches(ItemStack stack, ForgeConfigSpec.ConfigValue<String> cfg) {
        if (stack.isEmpty()) return false;
        Item it = item(cfg.get());
        return it != Items.AIR && stack.getItem() == it;
    }

    public static ClassCfg byKey(String key) {
        for (ClassCfg c : CLASSES) if (c.key.equals(key)) return c;
        return SILVER;
    }

    /** Classe déduite du nom de l'item Printer, ou null si non renommé / inconnu. */
    public static String classFromName(ItemStack stack) {
        if (!stack.hasCustomHoverName()) return null;
        String n = TextUtil.strip(stack.getHoverName().getString());
        for (ClassCfg c : CLASSES) {
            if (n.equals(TextUtil.strip(c.itemName.get()))) return c.key;
        }
        return null;
    }

    /** Recharge apportée par la batterie tenue, -1 si ce n'est pas une batterie reconnue. */
    public static int batteryAmount(ItemStack stack) {
        if (!matches(stack, BATTERY_ITEM)) return -1;
        String n = TextUtil.strip(stack.getHoverName().getString());
        if (n.equals(TextUtil.strip(BATTERY_NAME_1.get()))) return BATTERY_AMOUNT_1.get();
        if (n.equals(TextUtil.strip(BATTERY_NAME_2.get()))) return BATTERY_AMOUNT_2.get();
        if (n.equals(TextUtil.strip(BATTERY_NAME_3.get()))) return BATTERY_AMOUNT_3.get();
        return -1;
    }
}
