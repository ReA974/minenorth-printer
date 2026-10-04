package com.minenorth_printer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Cartouche d'encre ou batterie. Un item neuf n'a pas de NBT (= plein) : les neufs s'empilent,
 * ceux entamés stockent la quantité déjà utilisée dans "Used".
 */
public class ChargeItem extends Item {
    public enum Kind { INK, ENERGY }

    private static final String TAG_USED = "Used";

    public final Kind kind;
    private final IntSupplier capacity;

    public ChargeItem(Kind kind, IntSupplier capacity) {
        super(new Item.Properties().stacksTo(16));
        this.kind = kind;
        this.capacity = capacity;
    }

    public int getMax() {
        return Math.max(1, capacity.getAsInt());
    }

    public int getCharge(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int used = tag == null ? 0 : tag.getInt(TAG_USED);
        return Math.max(0, getMax() - used);
    }

    public void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt(TAG_USED, getMax() - Math.max(0, Math.min(charge, getMax())));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMax();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / getMax());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return kind == Kind.INK ? 0x29B6F6 : 0x7CD641;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String key = kind == Kind.INK ? "tooltip.minenorth_printer.ink" : "tooltip.minenorth_printer.energy";
        int c = getCharge(stack);
        tip.add(Component.translatable(key, c, getMax())
                .withStyle(c == 0 ? ChatFormatting.RED : (kind == Kind.INK ? ChatFormatting.AQUA : ChatFormatting.GREEN)));
        tip.add(Component.translatable("tooltip.minenorth_printer.refill_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
