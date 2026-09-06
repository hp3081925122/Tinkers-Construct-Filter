package org.hp.tinkers_construct_filter.client.screen.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import org.hp.tinkers_construct_filter.client.screen.LegacyGuiGraphics;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;

import java.util.HashSet;
import java.util.Set;

/** 统一绘制图鉴弹层的外框、裁剪区域、滚动条和物品槽。 */
public final class CatalogOverlayRenderer {
    private static final int POPUP_Z = 200;
    private static final Set<String> LOGGED_DISPLAY_MAPPINGS = new HashSet<>();
    private boolean loggedItemDepth;

    /** 绘制带滚动内容的标准信息弹层，并统一处理层级、裁剪和滚动条。 */
    public CatalogOverlayContent.HoverResult renderContentPanel(LegacyGuiGraphics graphics, int x, int y, int width, int height,
                                                                int contentTop, int contentBottom, int scroll, int contentHeight,
                                                                int viewportHeight, int mouseX, int mouseY, Font font,
                                                                CatalogOverlayContent content) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z - 1);
        renderPanelFrame(graphics, x, y, width, height, 0xFF0F0714, 0xFF24152A, 0xFF5E2A85);
        graphics.enableScissor(x + 4, contentTop, x + width - 4, contentBottom);
        // 旧版物品渲染器不接收弹窗矩阵，需同步物品及数量文字的深度。
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        float previousItemDepth = itemRenderer.blitOffset;
        itemRenderer.blitOffset = previousItemDepth + POPUP_Z - 1;
        CatalogOverlayContent.HoverResult result;
        try {
            // 每个界面只记录一次层级，便于核对空槽是否由深度遮挡造成。
            if (!loggedItemDepth) {
                TinkersConstructFilter.LOGGER.debug("Overlay item depth synchronized: previous={}, popup={}, current={}",
                    previousItemDepth, POPUP_Z - 1, itemRenderer.blitOffset);
                loggedItemDepth = true;
            }
            result = content.render(this, graphics, font, x, contentTop, contentBottom, width, scroll, mouseX, mouseY);
            graphics.disableScissor();
            renderScrollBar(graphics, x, contentTop, width, viewportHeight, contentHeight, scroll, contentBottom - contentTop);
        } finally {
            // 恢复共享渲染状态，避免影响列表、背包和后续悬浮提示。
            itemRenderer.blitOffset = previousItemDepth;
            graphics.disableScissor();
            graphics.pose().popPose();
        }
        return result;
    }

    /** 绘制普通选择弹层的统一外框。 */
    public void renderPanelFrame(LegacyGuiGraphics graphics, int x, int y, int width, int height,
                                 int borderColor, int fillColor, int outlineColor) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        graphics.fill(x, y, x + width, y + height, fillColor);
        if (outlineColor != 0) {
            graphics.renderOutline(x, y, width, height, outlineColor);
        }
    }

    /** 绘制普通物品槽，并返回鼠标是否悬停在该槽位上。 */
    public boolean drawItem(LegacyGuiGraphics graphics, Font font, ItemStack stack, int itemX, int itemY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= itemX - 1 && mouseX < itemX + 17
            && mouseY >= itemY - 1 && mouseY < itemY + 17;
        graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, hovered ? 0xFF6A4A72 : 0xFF3A2B40);
        // 带材质的部件和已组装工具必须保留原始物品数据，仅空白工具使用展示模型。
        boolean useDisplayFallback = stack.getItem() instanceof IModifiableDisplay && !stack.hasTag();
        ItemStack renderStack = useDisplayFallback ? IModifiableDisplay.getDisplayStack(stack.getItem()) : stack;
        String itemId = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        // 每种渲染路径只记录一次，便于核对材质是否保留，避免逐帧刷屏。
        if (LOGGED_DISPLAY_MAPPINGS.add(itemId + ":" + useDisplayFallback)) {
            TinkersConstructFilter.LOGGER.debug("Overlay item render: item={}, originalTag={}, displayFallback={}",
                itemId, stack.hasTag(), useDisplayFallback);
        }
        graphics.renderItem(renderStack, itemX, itemY);
        graphics.renderItemDecorations(font, stack, itemX, itemY);
        return hovered;
    }

    /** 绘制统一样式的垂直滚动条。 */
    public void renderScrollBar(LegacyGuiGraphics graphics, int x, int y, int width, int visibleAmount,
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
