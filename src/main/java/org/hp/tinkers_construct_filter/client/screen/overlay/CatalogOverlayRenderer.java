package org.hp.tinkers_construct_filter.client.screen.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.hp.tinkers_construct_filter.client.screen.CatalogSkin;

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
        renderPanelFrame(graphics, x, y, width, height);
        graphics.enableScissor(x + 4, contentTop, x + width - 4, contentBottom);
        CatalogOverlayContent.HoverResult result = content.render(this, graphics, font, x, contentTop, contentBottom, width, scroll, mouseX, mouseY);
        graphics.disableScissor();
        renderScrollBar(graphics, x, contentTop, width, viewportHeight, contentHeight, scroll, contentBottom - contentTop);
        graphics.pose().popPose();
        return result;
    }

    /** 绘制普通选择弹层的统一外框。 */
    public void renderPanelFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        // 详情和选择弹层共用铜边暗底，保留调用方的深度与裁剪状态。
        CatalogSkin.draw(graphics, CatalogSkin.POPUP, x - 1, y - 1, width + 2, height + 2);
    }

    /** 绘制普通物品槽，并返回鼠标是否悬停在该槽位上。 */
    public boolean drawItem(GuiGraphics graphics, Font font, ItemStack stack, int itemX, int itemY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= itemX - 1 && mouseX < itemX + 17
            && mouseY >= itemY - 1 && mouseY < itemY + 17;
        CatalogSkin.draw(graphics, hovered ? CatalogSkin.SLOT_HOVER : CatalogSkin.SLOT, itemX - 1, itemY - 1, 18, 18);
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
        CatalogSkin.draw(graphics, CatalogSkin.SCROLL_TRACK, x + width - 5, y, 2, trackHeight);
        CatalogSkin.draw(graphics, CatalogSkin.SCROLL_THUMB, x + width - 5, thumbY, 2, thumb);
    }
}
