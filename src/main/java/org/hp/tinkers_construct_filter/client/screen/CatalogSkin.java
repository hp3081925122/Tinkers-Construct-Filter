package org.hp.tinkers_construct_filter.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;

public final class CatalogSkin {
    public static final ResourceLocation TEXTURE = new ResourceLocation(TinkersConstructFilter.MOD_ID, "textures/gui/catalog_atlas.png");
    private static final int ATLAS_SIZE = 256;

    public static final Sprite WINDOW = new Sprite(0, 0, 48, 48, 8);
    public static final Sprite HEADER = new Sprite(52, 0, 32, 24, 5);
    public static final Sprite SIDEBAR = new Sprite(88, 0, 24, 24, 4);
    public static final Sprite LIST = new Sprite(116, 0, 24, 24, 4);
    public static final Sprite DETAIL_PANEL = new Sprite(144, 0, 32, 32, 6);
    public static final Sprite DETAIL_CARD = new Sprite(180, 0, 24, 24, 4);
    public static final Sprite POPUP = new Sprite(208, 0, 28, 28, 5);

    public static final Sprite BUTTON = new Sprite(0, 52, 20, 20, 4);
    public static final Sprite BUTTON_HOVER = new Sprite(24, 52, 20, 20, 4);
    public static final Sprite BUTTON_SELECTED = new Sprite(48, 52, 20, 20, 4);
    public static final Sprite BUTTON_DISABLED = new Sprite(72, 52, 20, 20, 4);
    public static final Sprite TAB_IDLE = new Sprite(96, 52, 24, 22, 4);
    public static final Sprite TAB_HOVER = new Sprite(124, 52, 24, 22, 4);
    public static final Sprite TAB_SELECTED = new Sprite(152, 52, 24, 22, 4);
    public static final Sprite STATION_BTN = new Sprite(180, 52, 22, 22, 4);
    public static final Sprite STATION_BTN_HOVER = new Sprite(206, 52, 22, 22, 4);

    public static final Sprite ROW = new Sprite(0, 80, 24, 24, 3);
    public static final Sprite ROW_ALT = new Sprite(28, 80, 24, 24, 3);
    public static final Sprite ROW_HOVER = new Sprite(56, 80, 24, 24, 3);
    public static final Sprite ROW_ACTIVE = new Sprite(84, 80, 24, 24, 4);
    public static final Sprite SLOT = new Sprite(112, 80, 18, 18, 2);
    public static final Sprite SLOT_HOVER = new Sprite(134, 80, 18, 18, 2);
    public static final Sprite SLOT_PEDESTAL = new Sprite(156, 80, 22, 22, 3);
    public static final Sprite CHIP = new Sprite(182, 80, 20, 13, 3);
    public static final Sprite CHIP_HOVER = new Sprite(206, 80, 20, 13, 3);
    public static final Sprite TRAIT_PILL = new Sprite(182, 96, 20, 13, 3);
    public static final Sprite TRAIT_PILL_HOVER = new Sprite(206, 96, 20, 13, 3);
    public static final Sprite BADGE = new Sprite(230, 80, 16, 11, 3);

    public static final Sprite INPUT = new Sprite(0, 112, 20, 20, 3);
    public static final Sprite INPUT_HOVER = new Sprite(24, 112, 20, 20, 3);
    public static final Sprite INPUT_FOCUSED = new Sprite(48, 112, 20, 20, 3);
    public static final Sprite SCROLL_TRACK = new Sprite(72, 112, 6, 16, 2);
    public static final Sprite SCROLL_THUMB = new Sprite(82, 112, 6, 16, 2);
    public static final Sprite SCROLL_THUMB_HOVER = new Sprite(92, 112, 6, 16, 2);

    public static final Sprite ICON_MATERIALS = new Sprite(0, 140, 16, 16, 0);
    public static final Sprite ICON_PARTS = new Sprite(20, 140, 16, 16, 0);
    public static final Sprite ICON_MODIFIERS = new Sprite(40, 140, 16, 16, 0);
    public static final Sprite ICON_TRAITS = new Sprite(60, 140, 16, 16, 0);
    public static final Sprite ICON_LOGO = new Sprite(80, 140, 16, 16, 0);

    public static final Sprite ICON_SEARCH = new Sprite(0, 164, 9, 9, 0);
    public static final Sprite ICON_FILTER = new Sprite(12, 164, 9, 9, 0);
    public static final Sprite ICON_SORT = new Sprite(24, 164, 9, 9, 0);
    public static final Sprite ICON_ASCENDING = new Sprite(36, 164, 9, 9, 0);
    public static final Sprite ICON_DESCENDING = new Sprite(48, 164, 9, 9, 0);
    public static final Sprite ICON_STAR = new Sprite(60, 164, 9, 9, 0);
    public static final Sprite ICON_CLOSE = new Sprite(72, 164, 9, 9, 0);
    public static final Sprite ICON_HISTORY = new Sprite(84, 164, 9, 9, 0);
    public static final Sprite ICON_REMOVE = new Sprite(96, 164, 9, 9, 0);
    public static final Sprite ICON_PIN = new Sprite(108, 164, 9, 9, 0);

    public static final Sprite CHECK_OFF = new Sprite(0, 180, 9, 9, 0);
    public static final Sprite CHECK_ON = new Sprite(12, 180, 9, 9, 0);
    public static final Sprite RADIO_OFF = new Sprite(24, 180, 9, 9, 0);
    public static final Sprite RADIO_ON = new Sprite(36, 180, 9, 9, 0);

    public static final int TEXT = 0xFFEAE0D2;
    public static final int TEXT_BRIGHT = 0xFFFFF8EC;
    public static final int TEXT_MUTED = 0xFFA89B8E;
    public static final int TEXT_DIM = 0xFF73685E;
    public static final int ACCENT = 0xFFF2B84B;
    public static final int COPPER = 0xFFD98A54;
    public static final int DANGER = 0xFFE86A4A;
    public static final int DIVIDER_DARK = 0xFF141110;
    public static final int DIVIDER_LIGHT = 0xFF3E3530;
    public static final int DIVIDER_COPPER = 0xFF7D4627;

    private CatalogSkin() {
    }

    public static int tierColor(int level) {
        if (level <= 1) {
            return 0xFFC2B7AA;
        }
        if (level == 2) {
            return 0xFFE39356;
        }
        if (level == 3) {
            return 0xFF66C8F7;
        }
        return 0xFFD48CFF;
    }

    public static void draw(LegacyGuiGraphics graphics, Sprite sprite, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (sprite.border() == 0 || width == sprite.width() && height == sprite.height()) {
            blit(graphics, sprite, x, y, width, height, 0, 0, sprite.width(), sprite.height());
            return;
        }
        int border = sprite.border();
        int borderX = Math.min(border, width / 2);
        int borderY = Math.min(border, height / 2);
        for (int row = 0; row < 3; row++) {
            int top = row == 0 ? 0 : row == 1 ? borderY : height - borderY;
            int drawHeight = row == 1 ? height - borderY * 2 : borderY;
            int sourceY = row == 0 ? 0 : row == 1 ? border : sprite.height() - borderY;
            int sourceHeight = row == 1 ? sprite.height() - border * 2 : borderY;
            for (int column = 0; column < 3; column++) {
                int left = column == 0 ? 0 : column == 1 ? borderX : width - borderX;
                int drawWidth = column == 1 ? width - borderX * 2 : borderX;
                int sourceX = column == 0 ? 0 : column == 1 ? border : sprite.width() - borderX;
                int sourceWidth = column == 1 ? sprite.width() - border * 2 : borderX;
                if (drawWidth > 0 && drawHeight > 0) {
                    blit(graphics, sprite, x + left, y + top, drawWidth, drawHeight, sourceX, sourceY, sourceWidth, sourceHeight);
                }
            }
        }
    }

    public static void icon(LegacyGuiGraphics graphics, Sprite sprite, int x, int y, int color) {
        RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
        innerBlit(graphics, x, y, sprite.width(), sprite.height(), sprite.u(), sprite.v(), sprite.width(), sprite.height());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void divider(LegacyGuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, DIVIDER_DARK);
        graphics.fill(x, y + 1, x + width, y + 2, DIVIDER_LIGHT);
    }

    public static void copperDivider(LegacyGuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, DIVIDER_COPPER);
        graphics.fill(x, y + 1, x + width, y + 2, DIVIDER_DARK);
    }

    private static void blit(LegacyGuiGraphics graphics, Sprite sprite, int x, int y, int width, int height, int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        innerBlit(graphics, x, y, width, height, sprite.u() + sourceX, sprite.v() + sourceY, sourceWidth, sourceHeight);
    }

    private static void innerBlit(LegacyGuiGraphics graphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(TEXTURE, x, y, width, height, (float) u, (float) v, sourceWidth, sourceHeight, ATLAS_SIZE, ATLAS_SIZE);
        RenderSystem.disableBlend();
    }

    public record Sprite(int u, int v, int width, int height, int border) {
    }
}
