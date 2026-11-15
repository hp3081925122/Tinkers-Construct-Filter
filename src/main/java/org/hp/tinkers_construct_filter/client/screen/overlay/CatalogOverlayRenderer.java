package org.hp.tinkers_construct_filter.client.screen.overlay;

import net.minecraft.client.gui.Font;
import org.hp.tinkers_construct_filter.client.screen.LegacyGuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.hp.tinkers_construct_filter.client.screen.CatalogSkin;

public final class CatalogOverlayRenderer {
    public static final int HERO_HEIGHT = 32;

    public CatalogOverlayContent.HoverResult renderInspector(LegacyGuiGraphics graphics, int x, int y, int width, int height,
                                                             CatalogOverlayController controller, Font font,
                                                             CatalogOverlayContent content, int mouseX, int mouseY,
                                                             boolean interactive, boolean floating) {
        if (floating) {
            CatalogSkin.draw(graphics, CatalogSkin.POPUP, x, y, width, height);
        } else {
            CatalogSkin.draw(graphics, CatalogSkin.DETAIL_PANEL, x, y, width, height);
        }

        ItemStack hoveredItem = ItemStack.EMPTY;
        boolean pinHovered = false;
        int bodyTop = y + 6;

        CatalogOverlayContent.HeroHeader header = content.header();
        if (header != null) {
            int pedX = x + 6;
            int pedY = y + 6;
            CatalogSkin.draw(graphics, CatalogSkin.SLOT_PEDESTAL, pedX, pedY, 22, 22);
            if (!header.icon().isEmpty()) {
                graphics.renderItem(header.icon(), pedX + 3, pedY + 3);
                if (interactive && mouseX >= pedX && mouseX < pedX + 22 && mouseY >= pedY && mouseY < pedY + 22) {
                    hoveredItem = header.icon();
                }
            }

            int pinSize = 16;
            int pinX = x + width - 6 - pinSize;
            int pinY = y + 7;
            controller.setPinButtonBounds(pinX, pinY, pinSize, pinSize);
            pinHovered = interactive && mouseX >= pinX && mouseX < pinX + pinSize && mouseY >= pinY && mouseY < pinY + pinSize;
            boolean pinned = controller.isDetailPinned();
            CatalogSkin.Sprite pinBg = pinned ? CatalogSkin.BUTTON_SELECTED : pinHovered ? CatalogSkin.BUTTON_HOVER : CatalogSkin.BUTTON;
            CatalogSkin.draw(graphics, pinBg, pinX, pinY, pinSize, pinSize);
            CatalogSkin.icon(graphics, CatalogSkin.ICON_PIN, pinX + 4, pinY + 4,
                pinned ? CatalogSkin.ACCENT : pinHovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT_MUTED);

            int textLeft = pedX + 26;
            int textRight = pinX - 4;
            int badgeW = 0;
            if (header.badge() != null && !header.badge().isEmpty()) {
                badgeW = font.width(header.badge()) + 6;
                int bx = Math.max(textLeft, textRight - badgeW);
                CatalogSkin.draw(graphics, CatalogSkin.BADGE, bx, y + 7, badgeW, 11);
                graphics.drawString(font, header.badge(), bx + 3, y + 9, header.badgeColor(), false);
            }
            int titleMax = Math.max(16, textRight - textLeft - (badgeW > 0 ? badgeW + 4 : 0));
            graphics.drawString(font, clip(font, header.title(), titleMax), textLeft, y + 8, CatalogSkin.TEXT_BRIGHT, false);
            if (header.subtitle() != null && !header.subtitle().isEmpty()) {
                graphics.drawString(font, clip(font, header.subtitle(), Math.max(16, x + width - 8 - textLeft)),
                    textLeft, y + 19, CatalogSkin.COPPER, false);
            }

            CatalogSkin.copperDivider(graphics, x + 6, y + 31, width - 12);
            bodyTop = y + 35;
        } else {
            controller.setPinButtonBounds(0, 0, 0, 0);
        }

        int bodyBottom = y + height - 6;
        int viewportHeight = Math.max(1, bodyBottom - bodyTop);
        int prelimWidth = width - 12;
        int prelimContentHeight = content.contentHeight(font, prelimWidth);
        boolean needsScroll = prelimContentHeight > viewportHeight;
        int cardAreaWidth = needsScroll ? width - 19 : width - 12;
        int contentHeight = content.contentHeight(font, cardAreaWidth);
        controller.setDetailMetrics(contentHeight, viewportHeight);

        graphics.enableScissor(x + 5, bodyTop, x + width - 5, bodyBottom);
        CatalogOverlayContent.HoverResult bodyHover = content.render(this, graphics, font,
            x + 6, bodyTop, bodyBottom, cardAreaWidth, controller.detailScroll(), mouseX, mouseY, interactive);
        graphics.disableScissor();

        if (contentHeight > viewportHeight) {
            boolean barHovered = interactive && mouseX >= x + width - 11 && mouseX < x + width - 4 && mouseY >= bodyTop && mouseY < bodyBottom;
            renderScrollBarAt(graphics, x + width - 10, bodyTop, viewportHeight, viewportHeight, contentHeight, controller.detailScroll(), barHovered);
        }

        ItemStack finalItem = !bodyHover.item().isEmpty() ? bodyHover.item() : hoveredItem;
        return new CatalogOverlayContent.HoverResult(finalItem, bodyHover.trait(), bodyHover.jumpTarget(), pinHovered);
    }

    public void renderPanelFrame(LegacyGuiGraphics graphics, int x, int y, int width, int height) {
        CatalogSkin.draw(graphics, CatalogSkin.POPUP, x - 1, y - 1, width + 2, height + 2);
    }

    public boolean drawItem(LegacyGuiGraphics graphics, Font font, ItemStack stack, int itemX, int itemY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= itemX - 1 && mouseX < itemX + 17
            && mouseY >= itemY - 1 && mouseY < itemY + 17;
        CatalogSkin.draw(graphics, hovered ? CatalogSkin.SLOT_HOVER : CatalogSkin.SLOT, itemX - 1, itemY - 1, 18, 18);
        graphics.renderItem(stack, itemX, itemY);
        graphics.renderItemDecorations(font, stack, itemX, itemY);
        return hovered;
    }

    public void renderScrollBar(LegacyGuiGraphics graphics, int x, int y, int width, int visibleAmount,
                                int totalAmount, int scroll, int trackHeight) {
        renderScrollBarAt(graphics, x + width - 8, y, trackHeight, visibleAmount, totalAmount, scroll, false);
    }

    public void renderScrollBarAt(LegacyGuiGraphics graphics, int barX, int barY, int trackHeight,
                                  int visibleAmount, int totalAmount, int scroll, boolean hovered) {
        if (totalAmount <= visibleAmount || trackHeight <= 4) {
            return;
        }
        int thumb = Math.max(12, trackHeight * visibleAmount / totalAmount);
        int maximumScroll = Math.max(1, totalAmount - visibleAmount);
        int boundedScroll = Math.max(0, Math.min(maximumScroll, scroll));
        int thumbY = barY + Math.max(0, trackHeight - thumb) * boundedScroll / maximumScroll;
        CatalogSkin.draw(graphics, CatalogSkin.SCROLL_TRACK, barX, barY, 5, trackHeight);
        CatalogSkin.draw(graphics, hovered ? CatalogSkin.SCROLL_THUMB_HOVER : CatalogSkin.SCROLL_THUMB, barX, thumbY, 5, thumb);
    }

    private static String clip(Font font, String value, int maxWidth) {
        if (maxWidth <= 0 || value == null || value.isEmpty()) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
