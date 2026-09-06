package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 只替换图鉴按钮外观，继续使用原版按钮的点击、键盘和朗读行为。 */
public final class CatalogButton extends Button {
    private boolean selected;

    /** 使用当前版本按钮构造器，保留默认无障碍朗读。 */
    public CatalogButton(int x, int y, int width, int height, Component title, OnPress onPress) {
        super(x, y, width, height, title, onPress, DEFAULT_NARRATION);
    }

    /** 将当前页及已展开弹层标为选中，不与真正的禁用状态混淆。 */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** 选中、禁用、悬停与普通状态使用同一图集的独立材质。 */
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int sprite = selected ? CatalogSkin.BUTTON_SELECTED : !active ? CatalogSkin.BUTTON_DISABLED
            : isHoveredOrFocused() ? CatalogSkin.BUTTON_HOVER : CatalogSkin.BUTTON;
        CatalogSkin.draw(graphics, sprite, getX(), getY(), width, height);
        int color = selected ? CatalogSkin.ACCENT : active ? 0xFFF4EBDE : 0xFF8F8980;
        renderString(graphics, Minecraft.getInstance().font, color);
    }
}
