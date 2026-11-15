package org.hp.tinkers_construct_filter.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class LegacyGuiGraphics {
    private final PoseStack pose;
    private float itemZOffset;

    public LegacyGuiGraphics(PoseStack pose) {
        this.pose = pose;
    }

    public PoseStack pose() {
        return pose;
    }

    public void addItemZOffset(float delta) {
        this.itemZOffset += delta;
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

    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawCenteredString(pose, font, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawCenteredString(pose, font, text, x, y, color);
    }

    public void blit(ResourceLocation texture, int x, int y, int width, int height,
                     float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GuiComponent.blit(pose, x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        float previous = renderer.blitOffset;
        renderer.blitOffset = previous + itemZOffset;
        try {
            renderer.renderAndDecorateItem(stack, x, y);
        } finally {
            renderer.blitOffset = previous;
        }
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        float previous = renderer.blitOffset;
        renderer.blitOffset = previous + itemZOffset;
        try {
            renderer.renderGuiItemDecorations(font, stack, x, y);
        } finally {
            renderer.blitOffset = previous;
        }
    }

    public void renderTooltip(Font font, Component text, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.renderTooltip(pose, text, mouseX, mouseY);
        }
    }

    public void renderTooltip(Font font, List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.renderTooltip(pose, lines, mouseX, mouseY, font);
        }
    }

    public void renderTooltip(Font font, List<Component> lines, Optional<TooltipComponent> visual,
                              ItemStack stack, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.renderTooltip(pose, lines, visual, mouseX, mouseY, font, stack);
        }
    }

    public void enableScissor(int left, int top, int right, int bottom) {
        GuiComponent.enableScissor(left, top, right, bottom);
    }

    public void disableScissor() {
        GuiComponent.disableScissor();
    }
}
