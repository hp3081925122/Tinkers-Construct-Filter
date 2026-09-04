package org.hp.tinkers_construct_filter.client.screen.overlay;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.hp.tinkers_construct_filter.client.catalog.CatalogEntry;

import java.util.List;

/** 描述图鉴信息弹层中的文字行、物品分组和动态物品槽。 */
public final class CatalogOverlayContent {
    public static final int TEXT_LINE_HEIGHT = 12;
    public static final int SECTION_TITLE_HEIGHT = 16;
    public static final int ITEM_ROW_HEIGHT = 20;
    public static final int SECTION_GAP = 2;
    private static final long ITEM_ANIMATION_MILLIS = 800L;

    private final List<TextLine> textLines;
    private final List<ItemSection> sections;

    public CatalogOverlayContent(List<TextLine> textLines, List<ItemSection> sections) {
        this.textLines = List.copyOf(textLines);
        this.sections = List.copyOf(sections);
    }

    /** 根据当前宽度计算内容总高度，供控制器统一限制滚动范围。 */
    public int contentHeight(Font font, int width) {
        int columns = columns(width);
        int height = textLines.size() * TEXT_LINE_HEIGHT;
        for (int index = 0; index < sections.size(); index++) {
            ItemSection section = sections.get(index);
            int rows = Math.max(1, (section.slots().size() + columns - 1) / columns);
            height += SECTION_TITLE_HEIGHT + rows * ITEM_ROW_HEIGHT;
            if (index > 0) {
                height += SECTION_GAP;
            }
        }
        return Math.max(TEXT_LINE_HEIGHT, height);
    }

    /** 绘制内容并返回当前鼠标悬停的物品和词条。 */
    public HoverResult render(CatalogOverlayRenderer renderer, GuiGraphics graphics, Font font,
                              int x, int top, int bottom, int width, int scroll, int mouseX, int mouseY) {
        int columns = columns(width);
        int currentY = top - scroll;
        ItemStack hoveredItem = ItemStack.EMPTY;
        CatalogEntry.TraitTooltip hoveredTrait = null;
        for (TextLine line : textLines) {
            if (currentY + TEXT_LINE_HEIGHT > top && currentY < bottom) {
                String text = line.text().getString();
                Component display = line.underlined()
                    ? Component.literal(text).withStyle(style -> style.withUnderlined(true))
                    : line.text();
                graphics.drawString(font, display, x + 5, currentY, line.color(), false);
                if (line.trait() != null && mouseX >= x + 5 && mouseX < x + 5 + font.width(text)
                    && mouseY >= currentY && mouseY < currentY + TEXT_LINE_HEIGHT) {
                    hoveredTrait = line.trait();
                }
            }
            currentY += TEXT_LINE_HEIGHT;
        }
        long animationTick = System.currentTimeMillis() / ITEM_ANIMATION_MILLIS;
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            ItemSection section = sections.get(sectionIndex);
            if (sectionIndex > 0) {
                currentY += SECTION_GAP;
            }
            if (currentY + SECTION_TITLE_HEIGHT > top && currentY < bottom) {
                graphics.drawString(font, section.title(), x + 5, currentY, section.color(), false);
            }
            currentY += SECTION_TITLE_HEIGHT;
            for (int index = 0; index < section.slots().size(); index++) {
                int itemX = x + 5 + (index % columns) * 18;
                int itemY = currentY + (index / columns) * ITEM_ROW_HEIGHT;
                ItemSlot slot = section.slots().get(index);
                if (slot.candidates().isEmpty()) {
                    if (itemY + 18 > top && itemY < bottom) {
                        graphics.drawString(font, "—", itemX, itemY + 2, 0xFFBFBFBF, false);
                    }
                    continue;
                }
                ItemStack stack = slot.candidates().get((int) (animationTick % slot.candidates().size()));
                if (itemY + 18 > top && itemY < bottom && renderer.drawItem(graphics, font, stack, itemX, itemY, mouseX, mouseY)) {
                    hoveredItem = stack;
                }
            }
            currentY += Math.max(1, (section.slots().size() + columns - 1) / columns) * ITEM_ROW_HEIGHT;
        }
        return new HoverResult(hoveredItem, hoveredTrait);
    }

    private int columns(int width) {
        return Math.max(1, (width - 10) / 18);
    }

    public record TextLine(Component text, int color, boolean underlined, CatalogEntry.TraitTooltip trait) {
        public static TextLine plain(Component text, int color) {
            return new TextLine(text, color, false, null);
        }

        public static TextLine trait(Component text, int color, CatalogEntry.TraitTooltip trait) {
            return new TextLine(text, color, true, trait);
        }
    }

    public record ItemSection(Component title, int color, List<ItemSlot> slots) {
        public ItemSection {
            slots = List.copyOf(slots);
        }
    }

    public record ItemSlot(List<ItemStack> candidates) {
        public ItemSlot {
            candidates = List.copyOf(candidates);
        }

        public static ItemSlot single(ItemStack stack) {
            return new ItemSlot(List.of(stack));
        }
    }

    public record HoverResult(ItemStack item, CatalogEntry.TraitTooltip trait) {
    }
}
