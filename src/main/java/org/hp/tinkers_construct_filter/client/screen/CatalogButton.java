package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 只替换图鉴按钮外观，继续使用原版按钮的点击、键盘和朗读行为。 */
public final class CatalogButton extends Button {
    private boolean selected;

    /** 使用当前版本按钮构造器，保留默认无障碍朗读。 */
    public CatalogButton(int x, int y, int width, int height, Component title, OnPress onPress) {
        super(x, y, width, height, title, onPress);
    }

    /** 将当前页及已展开弹层标为选中，不与真正的禁用状态混淆。 */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** 选中、禁用、悬停与普通状态使用同一图集的独立材质。 */
    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        LegacyGuiGraphics graphics = new LegacyGuiGraphics(pose);
        int sprite = selected ? CatalogSkin.BUTTON_SELECTED : !active ? CatalogSkin.BUTTON_DISABLED
            : isHoveredOrFocused() ? CatalogSkin.BUTTON_HOVER : CatalogSkin.BUTTON;
        CatalogSkin.draw(graphics, sprite, x, y, width, height);
        int color = selected ? CatalogSkin.ACCENT : active ? 0xFFF4EBDE : 0xFF8F8980;
        // 旧版没有滚动按钮文本接口，按可用宽度裁切，保持边框内可读。
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, font.substrByWidth(getMessage(), Math.max(0, width - 8)).getString(),
            x + width / 2, y + (height - 8) / 2, color);
        if (isHoveredOrFocused()) {
            renderToolTip(pose, mouseX, mouseY);
        }
    }
}
