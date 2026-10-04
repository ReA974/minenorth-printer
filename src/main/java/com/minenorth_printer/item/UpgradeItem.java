package com.minenorth_printer.item;

import com.minenorth_printer.PrinterConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class UpgradeItem extends Item {
    public final UpgradeType type;

    public UpgradeItem(UpgradeType type) {
        super(new Item.Properties().stacksTo(16));
        this.type = type;
    }

    /** Texte de l'effet par niveau, ex. "-20 % de temps d'impression". */
    public static Component effectPerLevel(UpgradeType type) {
        return switch (type) {
            case SPEED -> Component.translatable("tooltip.minenorth_printer.upgrade.speed",
                    Math.round(PrinterConfig.get(PrinterConfig.SPEED_PER_LEVEL) * 100));
            case INK -> Component.translatable("tooltip.minenorth_printer.upgrade.ink",
                    PrinterConfig.get(PrinterConfig.INK_CAPACITY_PER_LEVEL));
            case YIELD -> Component.translatable("tooltip.minenorth_printer.upgrade.yield",
                    PrinterConfig.get(PrinterConfig.YIELD_EUROS_PER_LEVEL));
            case EFFICIENCY -> Component.translatable("tooltip.minenorth_printer.upgrade.efficiency",
                    Math.round(PrinterConfig.get(PrinterConfig.EFFICIENCY_PER_LEVEL) * 100));
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(effectPerLevel(type).copy().withStyle(type.color));
        tip.add(Component.translatable("tooltip.minenorth_printer.upgrade.stack",
                PrinterConfig.get(PrinterConfig.UPGRADE_MAX_LEVEL)).withStyle(ChatFormatting.GRAY));
    }
}
