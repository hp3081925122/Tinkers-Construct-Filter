package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 只替换图鉴按钮外观，继续使用原版按钮的点击、键盘和朗读行为。 */
public final class CatalogButton extends Button {
    private boolean selected;
    private boolean tab;
    private CatalogSkin.Sprite icon;
    private String badge = "";

    /** 使用当前版本按钮构造器，保留默认无障碍朗读。 */
    public CatalogButton(int x, int y, int width, int height, Component title, OnPress onPress) {
        super(x, y, width, height, title, onPress, DEFAULT_NARRATION);
    }

    /** 将当前页及已展开弹层标为选中，不与真正的禁用状态混淆。 */
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

    public void setBadge(String badge) {
        this.badge = badge == null ? "" : badge;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        if (tab) {
            renderTab(graphics, font);
            return;
        }
        boolean hovered = isHovered();
        CatalogSkin.Sprite sprite = selected ? CatalogSkin.BUTTON_SELECTED : !active ? CatalogSkin.BUTTON_DISABLED
            : hovered ? CatalogSkin.BUTTON_HOVER : CatalogSkin.BUTTON;
        CatalogSkin.draw(graphics, sprite, getX(), getY(), width, height);
        int color = selected ? CatalogSkin.ACCENT : !active ? CatalogSkin.TEXT_DIM : hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT;
        String text = getMessage().getString();
        String badgeText = badge.isEmpty() ? "" : " " + badge;
        int iconWidth = icon == null ? 0 : icon.width() + (text.isEmpty() ? 0 : 4);
        String clipped = clip(font, text, width - 8 - iconWidth - font.width(badgeText));
        int contentWidth = iconWidth + font.width(clipped) + font.width(badgeText);
        int left = getX() + (width - contentWidth) / 2;
        if (icon != null) {
            CatalogSkin.icon(graphics, icon, left, getY() + (height - icon.height()) / 2, color);
        }
        int textX = graphics.drawString(font, clipped, left + iconWidth, getY() + (height - 8) / 2, color, false);
        if (!badgeText.isEmpty()) {
            graphics.drawString(font, badgeText, textX, getY() + (height - 8) / 2, selected ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.ACCENT, false);
        }
    }

    /** 侧栏页签：图标、名称与右侧条目数，选中时左侧金条。 */
    private void renderTab(GuiGraphics graphics, Font font) {
        boolean hovered = isHovered();
        if (selected) {
            CatalogSkin.draw(graphics, CatalogSkin.TAB_SELECTED, getX(), getY(), width, height);
        } else if (hovered) {
            CatalogSkin.draw(graphics, CatalogSkin.TAB_HOVER, getX(), getY(), width, height);
        }
        int textY = getY() + (height - 8) / 2;
        if (icon != null) {
            CatalogSkin.icon(graphics, icon, getX() + 5, getY() + (height - icon.height()) / 2, 0xFFFFFFFF);
        }
        int badgeWidth = font.width(badge);
        if (!badge.isEmpty()) {
            graphics.drawString(font, badge, getX() + width - 5 - badgeWidth, textY, selected ? CatalogSkin.ACCENT : CatalogSkin.TEXT_DIM, false);
        }
        int textX = getX() + (icon == null ? 6 : 25);
        int color = selected ? CatalogSkin.ACCENT : hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT_MUTED;
        graphics.drawString(font, clip(font, getMessage().getString(), getX() + width - textX - badgeWidth - 8), textX, textY, color, false);
    }

    private static String clip(Font font, String value, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
