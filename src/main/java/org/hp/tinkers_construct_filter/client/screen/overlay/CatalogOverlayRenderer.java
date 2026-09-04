package org.hp.tinkers_construct_filter.client.screen.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** 统一绘制图鉴弹层的外框、裁剪区域、滚动条和物品槽。 */
public final class CatalogOverlayRenderer {
    private static final int POPUP_Z = 200;

    /** 绘制带滚动内容的标准信息弹层，并统一处理层级、裁剪和滚动条。 */
    public CatalogOverlayContent.HoverResult renderContentPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                                                int contentTop, int contentBottom, int scroll, int contentHeight,
                                                                int viewportHeight, int mouseX, int mouseY, Font font,
                                                                CatalogOverlayContent content) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z - 1);
        renderPanelFrame(graphics, x, y, width, height, 0xFF0F0714, 0xFF24152A, 0xFF5E2A85);
        graphics.enableScissor(x + 4, contentTop, x + width - 4, contentBottom);
        CatalogOverlayContent.HoverResult result = content.render(this, graphics, font, x, contentTop, contentBottom, width, scroll, mouseX, mouseY);
        graphics.disableScissor();
        renderScrollBar(graphics, x, contentTop, width, viewportHeight, contentHeight, scroll, contentBottom - contentTop);
        graphics.pose().popPose();
        return result;
    }

    /** 绘制普通选择弹层的统一外框。 */
    public void renderPanelFrame(GuiGraphics graphics, int x, int y, int width, int height,
                                 int borderColor, int fillColor, int outlineColor) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        graphics.fill(x, y, x + width, y + height, fillColor);
        if (outlineColor != 0) {
            graphics.renderOutline(x, y, width, height, outlineColor);
        }
    }

    /** 绘制普通物品槽，并返回鼠标是否悬停在该槽位上。 */
    public boolean drawItem(GuiGraphics graphics, Font font, ItemStack stack, int itemX, int itemY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= itemX - 1 && mouseX < itemX + 17
            && mouseY >= itemY - 1 && mouseY < itemY + 17;
        graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, hovered ? 0xFF6A4A72 : 0xFF3A2B40);
        graphics.renderItem(stack, itemX, itemY);
        graphics.renderItemDecorations(font, stack, itemX, itemY);
        return hovered;
    }

    /** 绘制统一样式的垂直滚动条。 */
    public void renderScrollBar(GuiGraphics graphics, int x, int y, int width, int visibleAmount,
                                int totalAmount, int scroll, int trackHeight) {
        if (totalAmount <= visibleAmount || trackHeight <= 0) {
            return;
        }
        int thumb = Math.max(10, trackHeight * visibleAmount / totalAmount);
        int maximumScroll = Math.max(1, totalAmount - visibleAmount);
        int boundedScroll = Math.max(0, Math.min(maximumScroll, scroll));
        int thumbY = y + Math.max(0, trackHeight - thumb) * boundedScroll / maximumScroll;
        graphics.fill(x + width - 5, y, x + width - 3, y + trackHeight, 0xFF171717);
        graphics.fill(x + width - 5, thumbY, x + width - 3, thumbY + thumb, 0xFFB0B0B0);
    }
}
