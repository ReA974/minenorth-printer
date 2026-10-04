package com.minenorth_printer.client;

import com.minenorth_printer.MineNorthPrinter;
import com.minenorth_printer.ModRegistry;
import com.minenorth_printer.block.PrinterStatus;
import com.minenorth_printer.item.UpgradeItem;
import com.minenorth_printer.item.UpgradeType;
import com.minenorth_printer.menu.PrinterMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.minenorth_printer.block.PrinterBlockEntity.*;
import static com.minenorth_printer.menu.PrinterMenu.*;

/**
 * Interface de l'imprimante, au style des écrans MineNorth (EuroBank) :
 * aplats indigo, bordure sombre, logo + titre en gras, libellés cyan, valeurs blanches, boutons plats.
 */
public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private static final ResourceLocation LOGO = new ResourceLocation(MineNorthPrinter.MODID, "textures/gui/logo.png");

    // Palette MineNorth (identique à l'ATM EuroBank)
    private static final int BORDER = 0xFF0E0E10;
    private static final int BLUE = 0xFF161048;
    private static final int SCREEN = 0xFF0E0A34;
    private static final int SLOT_BG = 0xFF2A2468;
    private static final int DARK = 0xFF4A3CB4;
    private static final int CYAN = 0xFF20AAEB;
    private static final int PINK = 0xFFC83CF0;
    private static final int TEXT = 0xFFCFE3FF;
    private static final int MUTED = 0xFF8FA8E0;
    private static final int WARN = 0xFFFFE066;
    private static final int ALERT = 0xFFFF6A9A;
    private static final int WHITE = 0xFFFFFFFF;

    // Zones (relatives à l'interface)
    private static final int HEADER_H = 26;
    private static final int INK_BAR_X = 29, NRG_BAR_X = 43, BAR_Y = 32, BAR_W = 10, BAR_H = 54;
    private static final int CENTER_X = 57, CENTER_Y = 32, CENTER_W = 56, CENTER_H = 54;
    private static final int BTN_X = 115, BTN_Y = 70, BTN_W = 54, BTN_H = 16;
    private static final int STATS_X = 84, STATS_Y = 91;

    public PrinterScreen(PrinterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = 124;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderCustomTooltips(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ fond
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Cadre façon ATM : bordure sombre de 3 px + aplat indigo
        g.fill(x - 3, y - 3, x + imageWidth + 3, y + imageHeight + 3, BORDER);
        g.fill(x, y, x + imageWidth, y + imageHeight, BLUE);
        // Bandeau d'en-tête + liseré cyan
        g.fill(x, y, x + imageWidth, y + HEADER_H, SCREEN);
        g.fill(x, y + HEADER_H, x + imageWidth, y + HEADER_H + 1, CYAN);
        // Séparateur avant l'inventaire
        g.fill(x + 7, y + 121, x + imageWidth - 7, y + 122, DARK);

        RenderSystem.enableBlend();
        g.blit(LOGO, x + 6, y + 3, 20, 20, 0, 0, 64, 64, 64, 64);
        RenderSystem.disableBlend();

        for (Slot s : menu.slots) slotFrame(g, x + s.x - 1, y + s.y - 1);

        ghost(g, SLOT_INK, new ItemStack(ModRegistry.INK_CARTRIDGE.get()));
        ghost(g, SLOT_BATTERY, new ItemStack(ModRegistry.BATTERY.get()));
        for (UpgradeType t : UpgradeType.values()) {
            ghost(g, SLOT_UPGRADE_START + t.ordinal(), new ItemStack(ModRegistry.UPGRADES.get(t).get()));
        }

        bar(g, x + INK_BAR_X, y + BAR_Y, menu.get(D_INK), menu.get(D_INK_MAX), menu.get(D_INK_COST), CYAN);
        bar(g, x + NRG_BAR_X, y + BAR_Y, menu.get(D_ENERGY), menu.get(D_ENERGY_MAX), menu.get(D_ENERGY_COST), WARN);

        drawPrinterPanel(g, x + CENTER_X, y + CENTER_Y);

        // Bouton plat (cyan = en marche, rose = à l'arrêt), éclairci au survol comme les AtmButton
        boolean hov = isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY);
        int c = menu.isEnabled() ? CYAN : PINK;
        g.fill(x + BTN_X, y + BTN_Y, x + BTN_X + BTN_W, y + BTN_Y + BTN_H, hov ? lighten(c) : c);
    }

    private void drawPrinterPanel(GuiGraphics g, int px, int py) {
        PrinterStatus status = menu.getStatus();
        float progress = menu.get(D_PROGRESS) / 1000F;
        long time = System.currentTimeMillis();

        // "écran" du panneau central
        g.fill(px, py, px + CENTER_W, py + CENTER_H, SCREEN);

        // Feuille / billet qui sort
        int sheetW = 30, sheetX = px + (CENTER_W - sheetW) / 2, sheetTop = py + 14;
        int sheetH = Math.round(progress * 18);
        if (sheetH > 0) {
            g.fill(sheetX, sheetTop, sheetX + sheetW, sheetTop + sheetH, 0xFFBFE8C9);
            for (int ly = sheetTop + 3; ly < sheetTop + sheetH; ly += 4) {
                g.fill(sheetX + 3, ly, sheetX + sheetW - 3, ly + 1, 0xFF8CCB9C);
            }
            if (sheetH >= 14) {
                g.fill(sheetX + 11, sheetTop + 4, sheetX + 19, sheetTop + 12, 0xFF5FAE74);
                g.fill(sheetX + 13, sheetTop + 6, sheetX + 17, sheetTop + 10, 0xFFBFE8C9);
            }
        }

        // Corps d'imprimante stylisé
        int bx = px + 6, by = py + 4, bw = CENTER_W - 12;
        g.fill(bx, by, bx + bw, by + 11, DARK);
        g.fill(bx + 4, by + 9, bx + bw - 4, by + 11, BORDER);
        boolean blink = status != PrinterStatus.RUNNING || (time / 300) % 2 == 0;
        g.fill(bx + bw - 7, by + 3, bx + bw - 3, by + 6, blink ? status.color : SLOT_BG);
        g.fill(bx + 3, by + 3, bx + 15, by + 6, status == PrinterStatus.RUNNING ? CYAN : SLOT_BG);

        // Barre de progression (rose, comme la barre de retrait rapide de l'ATM)
        int pbx = px + 4, pby = py + CENTER_H - 6, pbw = CENTER_W - 8;
        g.fill(pbx, pby, pbx + pbw, pby + 3, SLOT_BG);
        int filled = Math.round(pbw * progress);
        if (filled > 0) g.fill(pbx, pby, pbx + filled, pby + 3, ALERT);
    }

    // ------------------------------------------------------------------ textes
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Titre en gras, statut en dessous (pastille colorée)
        drawScaled(g, bold(title.getString().toUpperCase()), 31, 3, 1.3F, 136, WHITE);
        PrinterStatus status = menu.getStatus();
        g.fill(31, 17, 35, 21, status.color);
        g.drawString(font, Component.translatable(status.translationKey()), 38, 16, status.color, false);

        // Temps restant / pourcentage dans le panneau
        Component timeText = bold(status == PrinterStatus.RUNNING
                ? formatTime(menu.get(D_REMAINING))
                : menu.get(D_PROGRESS) / 10 + " %");
        g.drawString(font, timeText, CENTER_X + (CENTER_W - font.width(timeText)) / 2, CENTER_Y + 35, WHITE, false);

        // Bouton
        Component btn = bold(Component.translatable(menu.isEnabled()
                ? "gui.minenorth_printer.button.on" : "gui.minenorth_printer.button.off").getString());
        g.drawString(font, btn, BTN_X + (BTN_W - font.width(btn)) / 2, BTN_Y + 4, WHITE, false);

        // Améliorations + statistiques
        g.drawString(font, Component.translatable("gui.minenorth_printer.upgrades"), UPGRADE_X, STATS_Y, CYAN, false);
        stat(g, 0, "gui.minenorth_printer.stat.cycle", menu.get(D_CYCLE) + " s");
        stat(g, 1, "gui.minenorth_printer.stat.gain", formatEuros(menu.get(D_GAIN)));
        stat(g, 2, "gui.minenorth_printer.stat.total", formatEuros(menu.getTotalEuros()));

        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, CYAN, false);
    }

    private void stat(GuiGraphics g, int line, String key, String value) {
        int ly = STATS_Y + line * 10;
        g.drawString(font, Component.translatable(key), STATS_X, ly, CYAN, false);
        Component v = bold(value);
        g.drawString(font, v, imageWidth - 8 - font.width(v), ly, WHITE, false);
    }

    // ------------------------------------------------------------------ infobulles
    private void renderCustomTooltips(GuiGraphics g, int mx, int my) {
        if (!menu.getCarried().isEmpty()) return;
        List<Component> tip = new ArrayList<>();

        if (isHovering(INK_BAR_X, BAR_Y, BAR_W, BAR_H, mx, my)) {
            int ink = menu.get(D_INK), max = menu.get(D_INK_MAX), cost = Math.max(1, menu.get(D_INK_COST));
            tip.add(Component.translatable("gui.minenorth_printer.tip.ink").withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal(Math.min(ink, max) + " / " + max).withStyle(ChatFormatting.WHITE));
            tip.add(Component.translatable("gui.minenorth_printer.tip.cost", cost).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.minenorth_printer.tip.prints_left", ink / cost).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(NRG_BAR_X, BAR_Y, BAR_W, BAR_H, mx, my)) {
            int e = menu.get(D_ENERGY), max = menu.get(D_ENERGY_MAX), cost = Math.max(1, menu.get(D_ENERGY_COST));
            tip.add(Component.translatable("gui.minenorth_printer.tip.energy").withStyle(ChatFormatting.YELLOW));
            tip.add(Component.literal(Math.min(e, max) + " / " + max).withStyle(ChatFormatting.WHITE));
            tip.add(Component.translatable("gui.minenorth_printer.tip.cost", cost).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.minenorth_printer.tip.prints_left", e / cost).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(CENTER_X, CENTER_Y, CENTER_W, CENTER_H, mx, my)) {
            tip.add(Component.translatable("gui.minenorth_printer.tip.progress", menu.get(D_PROGRESS) / 10).withStyle(ChatFormatting.WHITE));
            PrinterStatus st = menu.getStatus();
            if (st == PrinterStatus.RUNNING) {
                tip.add(Component.translatable("gui.minenorth_printer.tip.next", formatTime(menu.get(D_REMAINING))).withStyle(ChatFormatting.GRAY));
            } else {
                tip.add(Component.translatable(st.translationKey() + ".hint").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(6, 3, 130, 20, mx, my) && !menu.getOwnerName().isEmpty()) {
            tip.add(Component.translatable("gui.minenorth_printer.tip.owner", menu.getOwnerName()).withStyle(ChatFormatting.AQUA));
        } else {
            for (UpgradeType t : UpgradeType.values()) {
                Slot s = menu.slots.get(SLOT_UPGRADE_START + t.ordinal());
                if (!s.hasItem() && isHovering(s.x, s.y, 16, 16, mx, my)) {
                    tip.add(Component.translatable("item.minenorth_printer.upgrade_" + t.id).withStyle(t.color));
                    tip.add(UpgradeItem.effectPerLevel(t).copy().withStyle(ChatFormatting.GRAY));
                }
            }
            Slot ink = menu.slots.get(SLOT_INK);
            if (!ink.hasItem() && isHovering(ink.x, ink.y, 16, 16, mx, my)) {
                tip.add(Component.translatable("gui.minenorth_printer.tip.ink_slot").withStyle(ChatFormatting.AQUA));
            }
            Slot bat = menu.slots.get(SLOT_BATTERY);
            if (!bat.hasItem() && isHovering(bat.x, bat.y, 16, 16, mx, my)) {
                tip.add(Component.translatable("gui.minenorth_printer.tip.battery_slot").withStyle(ChatFormatting.YELLOW));
            }
        }
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
    }

    // ------------------------------------------------------------------ clic bouton
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mx, my) && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PrinterMenu.BUTTON_TOGGLE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    // ------------------------------------------------------------------ helpers
    private void slotFrame(GuiGraphics g, int fx, int fy) {
        g.fill(fx, fy, fx + 18, fy + 18, SCREEN);
        g.fill(fx + 1, fy + 1, fx + 17, fy + 17, SLOT_BG);
    }

    private void ghost(GuiGraphics g, int slotIndex, ItemStack icon) {
        Slot s = menu.slots.get(slotIndex);
        if (s.hasItem()) return;
        int sx = leftPos + s.x, sy = topPos + s.y;
        g.renderFakeItem(icon, sx, sy);
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(sx, sy, sx + 16, sy + 16, 0xB82A2468);
        g.pose().popPose();
    }

    private void bar(GuiGraphics g, int bx, int by, int value, int max, int cost, int color) {
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SCREEN);
        g.fill(bx + 1, by + 1, bx + BAR_W - 1, by + BAR_H - 1, SLOT_BG);
        int inner = BAR_H - 2;
        int h = max <= 0 ? 0 : Math.round(inner * Math.min(1F, value / (float) max));
        if (h > 0) {
            g.fill(bx + 1, by + 1 + inner - h, bx + BAR_W - 1, by + 1 + inner, color);
            g.fill(bx + 2, by + 1 + inner - h, bx + 3, by + 1 + inner, lighten(color));
        }
        if (max > 0 && cost > 0 && cost < max) {
            int my = by + 1 + inner - Math.round(inner * cost / (float) max);
            g.fill(bx + 1, my, bx + BAR_W - 1, my + 1, ALERT);
        }
    }

    private void drawScaled(GuiGraphics g, Component c, int x, int y, float maxScale, int maxWidth, int color) {
        float s = Math.min(maxScale, maxWidth / (float) Math.max(1, font.width(c)));
        g.pose().pushPose();
        g.pose().scale(s, s, 1F);
        g.drawString(font, c, Math.round(x / s), Math.round(y / s), color, false);
        g.pose().popPose();
    }

    private static Component bold(String s) {
        return Component.literal(s).withStyle(ChatFormatting.BOLD);
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 35);
        int gr = Math.min(255, ((c >> 8) & 0xFF) + 35);
        int b = Math.min(255, (c & 0xFF) + 35);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private static String formatTime(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    /** 12500 -> "12 500 €" */
    private static String formatEuros(long euros) {
        String digits = Long.toString(euros);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return sb.append(" €").toString();
    }
}
