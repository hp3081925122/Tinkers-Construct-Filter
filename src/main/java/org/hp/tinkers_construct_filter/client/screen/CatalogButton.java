package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class CatalogButton extends Button {
    private boolean selected;
    private boolean tab;
    private boolean stationEntry;
    private boolean iconOnly;
    private CatalogSkin.Sprite icon;
    private String badge = "";

    public CatalogButton(int x, int y, int width, int height, Component title, OnPress onPress) {
        super(x, y, width, height, title, onPress, DEFAULT_NARRATION);
    }

    public static CatalogButton createStationEntry(int x, int y, Component tooltipTitle, OnPress onPress) {
        CatalogButton button = new CatalogButton(x, y, 22, 22, tooltipTitle, onPress);
        button.stationEntry = true;
        button.icon = CatalogSkin.ICON_LOGO;
        button.setTooltip(Tooltip.create(tooltipTitle));
        return button;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public CatalogButton icon(CatalogSkin.Sprite icon) {
        this.icon = icon;
        return this;
    }

    public CatalogButton tab() {
        this.tab = true;
        return this;
    }

    public CatalogButton iconOnly() {
        this.iconOnly = true;
        return this;
    }

    public void setBadge(String badge) {
        this.badge = badge == null ? "" : badge;
    }

    public void moveTo(int newX, int newY) {
        setPosition(newX, newY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        if (stationEntry) {
            renderStationEntry(graphics);
            return;
        }
        if (tab) {
            renderTab(graphics, font);
            return;
        }
        boolean hovered = isHovered();
        CatalogSkin.Sprite sprite = selected ? CatalogSkin.BUTTON_SELECTED : !active ? CatalogSkin.BUTTON_DISABLED
            : hovered ? CatalogSkin.BUTTON_HOVER : CatalogSkin.BUTTON;
        CatalogSkin.draw(graphics, sprite, getX(), getY(), width, height);
        int color = selected ? CatalogSkin.ACCENT : !active ? CatalogSkin.TEXT_DIM : hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT;
        if (iconOnly && icon != null) {
            CatalogSkin.icon(graphics, icon, getX() + (width - icon.width()) / 2, getY() + (height - icon.height()) / 2, color);
            return;
        }
        String text = getMessage().getString();
        boolean compact = width <= 26;
        String displayText = compact ? "" : text;
        String badgeText = badge.isEmpty() || compact ? "" : " " + badge;
        int iconWidth = icon == null ? 0 : icon.width() + (displayText.isEmpty() ? 0 : 4);
        String clipped = clip(font, displayText, width - 8 - iconWidth - font.width(badgeText));
        int contentWidth = iconWidth + font.width(clipped) + font.width(badgeText);
        int left = getX() + (width - contentWidth) / 2;
        if (icon != null) {
            CatalogSkin.icon(graphics, icon, left, getY() + (height - icon.height()) / 2, color);
        }
        if (!clipped.isEmpty()) {
            int textX = graphics.drawString(font, clipped, left + iconWidth, getY() + (height - 8) / 2, color, false);
            if (!badgeText.isEmpty()) {
                graphics.drawString(font, badgeText, textX, getY() + (height - 8) / 2, selected ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.ACCENT, false);
            }
        }
    }

    private void renderStationEntry(GuiGraphics graphics) {
        CatalogSkin.draw(graphics, isHovered() ? CatalogSkin.STATION_BTN_HOVER : CatalogSkin.STATION_BTN, getX(), getY(), width, height);
        if (icon != null) {
            CatalogSkin.icon(graphics, icon, getX() + (width - icon.width()) / 2, getY() + (height - icon.height()) / 2, 0xFFFFFFFF);
        }
    }

    private void renderTab(GuiGraphics graphics, Font font) {
        boolean hovered = isHovered();
        CatalogSkin.Sprite bg = selected ? CatalogSkin.TAB_SELECTED : hovered ? CatalogSkin.TAB_HOVER : CatalogSkin.TAB_IDLE;
        CatalogSkin.draw(graphics, bg, getX(), getY(), width, height);
        int textY = getY() + (height - 8) / 2;
        if (icon != null) {
            CatalogSkin.icon(graphics, icon, getX() + 5, getY() + (height - icon.height()) / 2, 0xFFFFFFFF);
        }
        int badgeBoxWidth = 0;
        if (!badge.isEmpty()) {
            int rawBadgeWidth = font.width(badge);
            badgeBoxWidth = rawBadgeWidth + 6;
            int badgeX = getX() + width - 3 - badgeBoxWidth;
            int badgeY = getY() + (height - 11) / 2;
            CatalogSkin.draw(graphics, CatalogSkin.BADGE, badgeX, badgeY, badgeBoxWidth, 11);
            graphics.drawString(font, badge, badgeX + 3, badgeY + 2, selected ? CatalogSkin.ACCENT : CatalogSkin.TEXT_MUTED, false);
        }
        int textX = getX() + (icon == null ? 6 : 22);
        int color = selected ? CatalogSkin.ACCENT : hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT;
        int maxTextWidth = getX() + width - textX - (badgeBoxWidth > 0 ? badgeBoxWidth + 5 : 4);
        graphics.drawString(font, clip(font, getMessage().getString(), maxTextWidth), textX, textY, color, false);
    }

    private static String clip(Font font, String value, int maxWidth) {
        if (maxWidth <= 0 || value.isEmpty()) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
