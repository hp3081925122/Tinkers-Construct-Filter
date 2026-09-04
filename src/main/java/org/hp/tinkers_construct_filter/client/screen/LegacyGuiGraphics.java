package org.hp.tinkers_construct_filter.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 为1.19.2提供与1.20.1图形封装相同的图鉴绘制接口。 */
public final class LegacyGuiGraphics {
    private final PoseStack pose;

    public LegacyGuiGraphics(PoseStack pose) {
        this.pose = pose;
    }

    public PoseStack pose() {
        return pose;
    }

    public void fill(int left, int top, int right, int bottom, int color) {
        GuiComponent.fill(pose, left, top, right, bottom, color);
    }

    public void renderOutline(int left, int top, int width, int height, int color) {
        fill(left, top, left + width, top + 1, color);
        fill(left, top + height - 1, left + width, top + height, color);
        fill(left, top, left + 1, top + height, color);
        fill(left + width - 1, top, left + width, top + height, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawCenteredString(pose, font, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawCenteredString(pose, font, text, x, y, color);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
    }

    public void enableScissor(int left, int top, int right, int bottom) {
        GuiComponent.enableScissor(left, top, right, bottom);
    }

    public void disableScissor() {
        GuiComponent.disableScissor();
    }
}
