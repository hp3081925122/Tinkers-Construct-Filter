package org.hp.tinkers_construct_filter.client.screen.overlay;

import net.minecraft.client.gui.Font;
import org.hp.tinkers_construct_filter.client.screen.LegacyGuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.hp.tinkers_construct_filter.client.catalog.CatalogEntry;
import org.hp.tinkers_construct_filter.client.screen.CatalogSkin;

import java.util.ArrayList;
import java.util.List;

public final class CatalogOverlayContent {
    public static final int SCROLL_STEP = 16;
    private static final int CARD_GAP = 5;
    private static final int CARD_PAD_X = 6;
    private static final int CARD_PAD_Y = 5;
    private static final int LINE_HEIGHT = 11;
    private static final int STAT_ROW_HEIGHT = 12;
    private static final int PILL_HEIGHT = 13;
    private static final int PILL_GAP_X = 4;
    private static final int PILL_GAP_Y = 3;
    private static final int ITEM_CELL = 19;
    private static final int ITEM_ROW_HEIGHT = 20;
    private static final long ITEM_ANIMATION_MILLIS = 900L;

    private final HeroHeader header;
    private final List<Card> cards;

    public CatalogOverlayContent(HeroHeader header, List<Card> cards) {
        this.header = header;
        this.cards = List.copyOf(cards);
    }

    public HeroHeader header() {
        return header;
    }

    public int contentHeight(Font font, int availableWidth) {
        int cardWidth = Math.max(40, availableWidth);
        int innerWidth = Math.max(24, cardWidth - CARD_PAD_X * 2);
        int total = 0;
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                total += CARD_GAP;
            }
            total += cardHeight(font, cards.get(i), innerWidth);
        }
        return Math.max(LINE_HEIGHT, total);
    }

    private int cardHeight(Font font, Card card, int innerWidth) {
        int height = CARD_PAD_Y * 2;
        List<CardBlock> blocks = card.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                height += 4;
            }
            height += blockHeight(font, blocks.get(i), innerWidth);
        }
        return height;
    }

    private int blockHeight(Font font, CardBlock block, int innerWidth) {
        if (block instanceof TitleBlock) {
            return 13;
        }
        if (block instanceof DescriptionBlock desc) {
            return Math.max(1, wrapLines(font, desc.lines(), innerWidth).size()) * LINE_HEIGHT;
        }
        if (block instanceof StatGridBlock stats) {
            return Math.max(1, stats.rows().size()) * STAT_ROW_HEIGHT;
        }
        if (block instanceof TraitPillsBlock pills) {
            int headerH = pills.label() == null ? 0 : 12;
            if (pills.traits().isEmpty()) {
                return headerH + LINE_HEIGHT;
            }
            return headerH + measurePillRows(font, pills.traits(), innerWidth) * (PILL_HEIGHT + PILL_GAP_Y) - PILL_GAP_Y;
        }
        if (block instanceof ItemGridBlock items) {
            int titleH = items.title() == null ? 0 : 13;
            int cols = Math.max(1, (innerWidth + 1) / ITEM_CELL);
            int rows = Math.max(1, (items.slots().size() + cols - 1) / cols);
            return titleH + rows * ITEM_ROW_HEIGHT;
        }
        return LINE_HEIGHT;
    }

    private int measurePillRows(Font font, List<CatalogEntry.TraitTooltip> traits, int innerWidth) {
        int rows = 1;
        int curX = 0;
        for (CatalogEntry.TraitTooltip trait : traits) {
            int pillW = Math.min(innerWidth, font.width(trait.name()) + 10);
            if (curX > 0 && curX + pillW > innerWidth) {
                rows++;
                curX = 0;
            }
            curX += pillW + PILL_GAP_X;
        }
        return rows;
    }

    public HoverResult render(CatalogOverlayRenderer renderer, LegacyGuiGraphics graphics, Font font,
                              int x, int top, int bottom, int availableWidth, int scroll,
                              int mouseX, int mouseY, boolean interactive) {
        int cardWidth = Math.max(40, availableWidth);
        int innerWidth = Math.max(24, cardWidth - CARD_PAD_X * 2);
        int currentY = top - scroll;
        ItemStack hoveredItem = ItemStack.EMPTY;
        CatalogEntry.TraitTooltip hoveredTrait = null;
        JumpTarget hoveredJump = null;
        long animationTick = System.currentTimeMillis() / ITEM_ANIMATION_MILLIS;
        boolean pointerInViewport = interactive && mouseY >= top && mouseY < bottom && mouseX >= x && mouseX < x + cardWidth;

        for (int cardIndex = 0; cardIndex < cards.size(); cardIndex++) {
            if (cardIndex > 0) {
                currentY += CARD_GAP;
            }
            Card card = cards.get(cardIndex);
            int cardH = cardHeight(font, card, innerWidth);
            if (currentY + cardH > top && currentY < bottom) {
                CatalogSkin.draw(graphics, CatalogSkin.DETAIL_CARD, x, currentY, cardWidth, cardH);
                int blockY = currentY + CARD_PAD_Y;
                int innerX = x + CARD_PAD_X;
                List<CardBlock> blocks = card.blocks();
                for (int b = 0; b < blocks.size(); b++) {
                    if (b > 0) {
                        blockY += 4;
                    }
                    CardBlock block = blocks.get(b);
                    int bH = blockHeight(font, block, innerWidth);
                    if (blockY + bH > top && blockY < bottom) {
                        if (block instanceof TitleBlock titleBlock) {
                            String clipped = clip(font, titleBlock.title().getString(), innerWidth);
                            boolean clickable = !titleBlock.hoverItem().isEmpty() || titleBlock.jumpTarget() != null;
                            Component display = clickable
                                ? Component.literal(clipped).withStyle(s -> s.withUnderlined(true))
                                : Component.literal(clipped);
                            graphics.drawString(font, display, innerX, blockY, titleBlock.color(), false);
                            graphics.fill(innerX, blockY + 10, innerX + innerWidth, blockY + 11, CatalogSkin.DIVIDER_DARK);
                            if (pointerInViewport && clickable
                                && mouseX >= innerX && mouseX < innerX + font.width(clipped)
                                && mouseY >= blockY && mouseY < blockY + 11) {
                                if (!titleBlock.hoverItem().isEmpty()) {
                                    hoveredItem = titleBlock.hoverItem();
                                }
                                if (titleBlock.jumpTarget() != null) {
                                    hoveredJump = titleBlock.jumpTarget();
                                }
                            }
                        } else if (block instanceof DescriptionBlock descBlock) {
                            int ly = blockY;
                            for (String line : wrapLines(font, descBlock.lines(), innerWidth)) {
                                if (ly + LINE_HEIGHT > top && ly < bottom) {
                                    graphics.drawString(font, line, innerX, ly + 1, descBlock.color(), false);
                                }
                                ly += LINE_HEIGHT;
                            }
                        } else if (block instanceof StatGridBlock statBlock) {
                            int sy = blockY;
                            for (StatRow row : statBlock.rows()) {
                                if (sy + STAT_ROW_HEIGHT > top && sy < bottom) {
                                    String valText = clip(font, row.value(), innerWidth * 3 / 5);
                                    int valW = font.width(valText);
                                    String keyText = clip(font, row.label(), innerWidth - valW - 8);
                                    int keyW = font.width(keyText);
                                    graphics.drawString(font, keyText, innerX, sy + 2, CatalogSkin.TEXT_MUTED, false);
                                    int dotStart = innerX + keyW + 4;
                                    int dotEnd = innerX + innerWidth - valW - 4;
                                    for (int dx = dotStart; dx < dotEnd; dx += 4) {
                                        graphics.fill(dx, sy + 6, dx + 1, sy + 7, 0xFF423934);
                                    }
                                    graphics.drawString(font, valText, innerX + innerWidth - valW, sy + 2, row.valueColor(), false);
                                }
                                sy += STAT_ROW_HEIGHT;
                            }
                        } else if (block instanceof TraitPillsBlock pillsBlock) {
                            int py = blockY;
                            if (pillsBlock.label() != null) {
                                graphics.drawString(font, clip(font, pillsBlock.label().getString(), innerWidth), innerX, py + 1, CatalogSkin.TEXT_MUTED, false);
                                py += 12;
                            }
                            if (pillsBlock.traits().isEmpty()) {
                                graphics.drawString(font, "—", innerX, py + 1, CatalogSkin.TEXT_DIM, false);
                            } else {
                                int curX = 0;
                                for (CatalogEntry.TraitTooltip trait : pillsBlock.traits()) {
                                    String tName = clip(font, trait.name(), innerWidth - 10);
                                    int pillW = Math.min(innerWidth, font.width(tName) + 10);
                                    if (curX > 0 && curX + pillW > innerWidth) {
                                        curX = 0;
                                        py += PILL_HEIGHT + PILL_GAP_Y;
                                    }
                                    int drawX = innerX + curX;
                                    boolean pillHovered = pointerInViewport
                                        && mouseX >= drawX && mouseX < drawX + pillW
                                        && mouseY >= py && mouseY < py + PILL_HEIGHT;
                                    if (py + PILL_HEIGHT > top && py < bottom) {
                                        CatalogSkin.draw(graphics, pillHovered ? CatalogSkin.TRAIT_PILL_HOVER : CatalogSkin.TRAIT_PILL, drawX, py, pillW, PILL_HEIGHT);
                                        graphics.drawString(font, tName, drawX + 5, py + 3, pillHovered ? CatalogSkin.ACCENT : CatalogSkin.TEXT_BRIGHT, false);
                                    }
                                    if (pillHovered) {
                                        hoveredTrait = trait;
                                        if (pillsBlock.clickable() && !trait.id().isEmpty()) {
                                            hoveredJump = new JumpTarget("traits", trait.id());
                                        }
                                    }
                                    curX += pillW + PILL_GAP_X;
                                }
                            }
                        } else if (block instanceof ItemGridBlock itemBlock) {
                            int iy = blockY;
                            if (itemBlock.title() != null) {
                                String tStr = clip(font, itemBlock.title().getString(), innerWidth);
                                boolean clickableTitle = !itemBlock.titleItem().isEmpty() || itemBlock.titleJumpTarget() != null;
                                Component display = clickableTitle
                                    ? Component.literal(tStr).withStyle(s -> s.withUnderlined(true))
                                    : Component.literal(tStr);
                                graphics.drawString(font, display, innerX, iy + 1, itemBlock.titleColor(), false);
                                if (pointerInViewport && clickableTitle
                                    && mouseX >= innerX && mouseX < innerX + font.width(tStr)
                                    && mouseY >= iy && mouseY < iy + 11) {
                                    if (!itemBlock.titleItem().isEmpty()) {
                                        hoveredItem = itemBlock.titleItem();
                                    }
                                    if (itemBlock.titleJumpTarget() != null) {
                                        hoveredJump = itemBlock.titleJumpTarget();
                                    }
                                }
                                iy += 13;
                            }
                            int cols = Math.max(1, (innerWidth + 1) / ITEM_CELL);
                            if (itemBlock.slots().isEmpty()) {
                                graphics.drawString(font, "—", innerX + 2, iy + 4, CatalogSkin.TEXT_DIM, false);
                            } else {
                                for (int sIdx = 0; sIdx < itemBlock.slots().size(); sIdx++) {
                                    int itemX = innerX + 1 + (sIdx % cols) * ITEM_CELL;
                                    int itemY = iy + 1 + (sIdx / cols) * ITEM_ROW_HEIGHT;
                                    ItemSlot slot = itemBlock.slots().get(sIdx);
                                    if (slot.candidates().isEmpty()) {
                                        if (itemY + 16 > top && itemY < bottom) {
                                            graphics.drawString(font, "—", itemX + 4, itemY + 4, CatalogSkin.TEXT_DIM, false);
                                        }
                                        continue;
                                    }
                                    ItemStack stack = slot.candidates().get((int) (animationTick % slot.candidates().size()));
                                    if (itemY + 18 > top && itemY < bottom) {
                                        boolean slotHovered = renderer.drawItem(graphics, font, stack, itemX, itemY,
                                            pointerInViewport ? mouseX : -1000, pointerInViewport ? mouseY : -1000);
                                        if (slotHovered && pointerInViewport) {
                                            hoveredItem = stack;
                                            if (slot.jumpTarget() != null) {
                                                hoveredJump = slot.jumpTarget();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    blockY += bH;
                }
            }
            currentY += cardH;
        }
        return new HoverResult(hoveredItem, hoveredTrait, hoveredJump, false);
    }

    public static List<String> wrapLines(Font font, List<String> paragraphs, int maxWidth) {
        List<String> result = new ArrayList<>();
        int safeWidth = Math.max(24, maxWidth);
        for (String text : paragraphs) {
            if (text == null || text.isBlank()) {
                continue;
            }
            for (String rawLine : text.split("\\R", -1)) {
                String remaining = rawLine.strip();
                while (!remaining.isEmpty()) {
                    String segment = font.plainSubstrByWidth(remaining, safeWidth);
                    if (segment.isEmpty()) {
                        break;
                    }
                    result.add(segment);
                    remaining = remaining.substring(segment.length()).stripLeading();
                }
            }
        }
        return List.copyOf(result);
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

    public record JumpTarget(String targetPage, String entryId) {
    }

    public record HeroHeader(ItemStack icon, String title, String badge, int badgeColor, String subtitle) {
    }

    public record Card(List<CardBlock> blocks) {
        public Card {
            blocks = List.copyOf(blocks);
        }
    }

    public sealed interface CardBlock permits TitleBlock, DescriptionBlock, StatGridBlock, TraitPillsBlock, ItemGridBlock {
    }

    public record TitleBlock(Component title, int color, ItemStack hoverItem, JumpTarget jumpTarget) implements CardBlock {
        public TitleBlock(Component title, int color) {
            this(title, color, ItemStack.EMPTY, null);
        }

        public TitleBlock(Component title, int color, ItemStack hoverItem) {
            this(title, color, hoverItem, null);
        }
    }

    public record DescriptionBlock(List<String> lines, int color) implements CardBlock {
        public DescriptionBlock {
            lines = List.copyOf(lines);
        }
    }

    public record StatRow(String label, String value, int valueColor) {
    }

    public record StatGridBlock(List<StatRow> rows) implements CardBlock {
        public StatGridBlock {
            rows = List.copyOf(rows);
        }
    }

    public record TraitPillsBlock(Component label, List<CatalogEntry.TraitTooltip> traits, boolean clickable) implements CardBlock {
        public TraitPillsBlock(Component label, List<CatalogEntry.TraitTooltip> traits) {
            this(label, traits, true);
        }

        public TraitPillsBlock {
            traits = List.copyOf(traits);
        }
    }

    public record ItemGridBlock(Component title, int titleColor, List<ItemSlot> slots, ItemStack titleItem, JumpTarget titleJumpTarget) implements CardBlock {
        public ItemGridBlock(Component title, int titleColor, List<ItemSlot> slots) {
            this(title, titleColor, slots, ItemStack.EMPTY, null);
        }

        public ItemGridBlock(Component title, int titleColor, List<ItemSlot> slots, ItemStack titleItem) {
            this(title, titleColor, slots, titleItem, null);
        }

        public ItemGridBlock {
            slots = List.copyOf(slots);
        }
    }

    public record ItemSlot(List<ItemStack> candidates, JumpTarget jumpTarget) {
        public ItemSlot(List<ItemStack> candidates) {
            this(candidates, null);
        }

        public ItemSlot {
            candidates = List.copyOf(candidates);
        }

        public static ItemSlot single(ItemStack stack) {
            return new ItemSlot(List.of(stack), null);
        }

        public static ItemSlot single(ItemStack stack, JumpTarget jumpTarget) {
            return new ItemSlot(List.of(stack), jumpTarget);
        }
    }

    public record HoverResult(ItemStack item, CatalogEntry.TraitTooltip trait, JumpTarget jumpTarget, boolean pinHovered) {
        public static final HoverResult EMPTY = new HoverResult(ItemStack.EMPTY, null, null, false);
    }
}
