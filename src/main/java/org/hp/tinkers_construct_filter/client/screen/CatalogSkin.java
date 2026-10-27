package org.hp.tinkers_construct_filter.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;

/** 图鉴专用逐像素图集，边框按 1:1 像素绘制，不修改原版全局界面资源。 */
public final class CatalogSkin {
    public static final ResourceLocation TEXTURE = new ResourceLocation(TinkersConstructFilter.MOD_ID, "textures/gui/catalog_atlas.png");
    private static final int ATLAS_SIZE = 256;

    public static final Sprite WINDOW = new Sprite(0, 0, 32, 32, 6);
    public static final Sprite SIDEBAR = new Sprite(32, 0, 16, 16, 2);
    public static final Sprite HEADER = new Sprite(48, 0, 16, 16, 3);
    public static final Sprite POPUP = new Sprite(64, 0, 16, 16, 3);
    public static final Sprite LIST = new Sprite(80, 0, 16, 16, 2);
    public static final Sprite CHIP = new Sprite(96, 0, 16, 12, 3);
    public static final Sprite CHIP_HOVER = new Sprite(112, 0, 16, 12, 3);
    public static final Sprite BUTTON = new Sprite(0, 32, 16, 16, 3);
    public static final Sprite BUTTON_HOVER = new Sprite(16, 32, 16, 16, 3);
    public static final Sprite BUTTON_SELECTED = new Sprite(32, 32, 16, 16, 3);
    public static final Sprite BUTTON_DISABLED = new Sprite(48, 32, 16, 16, 3);
    public static final Sprite TAB_HOVER = new Sprite(64, 32, 16, 16, 2);
    public static final Sprite TAB_SELECTED = new Sprite(80, 32, 16, 16, 3);
    public static final Sprite ROW = new Sprite(0, 48, 16, 16, 2);
    public static final Sprite ROW_ALT = new Sprite(16, 48, 16, 16, 2);
    public static final Sprite ROW_HOVER = new Sprite(32, 48, 16, 16, 2);
    public static final Sprite ROW_ACTIVE = new Sprite(48, 48, 16, 16, 2);
    public static final Sprite SLOT = new Sprite(64, 48, 18, 18, 1);
    public static final Sprite SLOT_HOVER = new Sprite(82, 48, 18, 18, 2);
    public static final Sprite INPUT = new Sprite(0, 64, 16, 16, 2);
    public static final Sprite INPUT_FOCUSED = new Sprite(16, 64, 16, 16, 2);
    public static final Sprite SCROLL_TRACK = new Sprite(32, 64, 6, 16, 2);
    public static final Sprite SCROLL_THUMB = new Sprite(40, 64, 6, 16, 2);
    public static final Sprite SCROLL_THUMB_HOVER = new Sprite(48, 64, 6, 16, 2);

    public static final Sprite ICON_MATERIALS = new Sprite(0, 96, 16, 16, 0);
    public static final Sprite ICON_PARTS = new Sprite(16, 96, 16, 16, 0);
    public static final Sprite ICON_MODIFIERS = new Sprite(32, 96, 16, 16, 0);
    public static final Sprite ICON_TRAITS = new Sprite(48, 96, 16, 16, 0);
    public static final Sprite ICON_LOGO = new Sprite(64, 96, 16, 16, 0);
    public static final Sprite ICON_SEARCH = new Sprite(0, 112, 9, 9, 0);
    public static final Sprite ICON_FILTER = new Sprite(9, 112, 9, 9, 0);
    public static final Sprite ICON_SORT = new Sprite(18, 112, 9, 9, 0);
    public static final Sprite ICON_ASCENDING = new Sprite(27, 112, 9, 9, 0);
    public static final Sprite ICON_DESCENDING = new Sprite(36, 112, 9, 9, 0);
    public static final Sprite ICON_STAR = new Sprite(45, 112, 9, 9, 0);
    public static final Sprite ICON_CLOSE = new Sprite(54, 112, 9, 9, 0);
    public static final Sprite ICON_HISTORY = new Sprite(63, 112, 9, 9, 0);
    public static final Sprite ICON_REMOVE = new Sprite(72, 112, 9, 9, 0);
    public static final Sprite CHECK_OFF = new Sprite(0, 121, 9, 9, 0);
    public static final Sprite CHECK_ON = new Sprite(9, 121, 9, 9, 0);
    public static final Sprite RADIO_OFF = new Sprite(18, 121, 9, 9, 0);
    public static final Sprite RADIO_ON = new Sprite(27, 121, 9, 9, 0);

    public static final int TEXT = 0xFFECE6DA;
    public static final int TEXT_BRIGHT = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFF9AA0A8;
    public static final int TEXT_DIM = 0xFF6B7078;
    public static final int ACCENT = 0xFFE8B54D;
    public static final int DANGER = 0xFFE8705E;
    public static final int DIVIDER_DARK = 0xFF0E1013;
    public static final int DIVIDER_LIGHT = 0xFF2C3036;

    private CatalogSkin() {
    }

    /** 九宫格绘制：边角与边框保持原像素大小，中心纯色区域拉伸，最多九个四边形。 */
    public static void draw(GuiGraphics graphics, Sprite sprite, int x, int y, int width, int height) {
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

    /** 以原尺寸绘制图标，白色图标可按文字颜色着色，彩色图标传入纯白保持原色。 */
    public static void icon(GuiGraphics graphics, Sprite sprite, int x, int y, int color) {
        RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
        innerBlit(graphics, x, y, sprite.width(), sprite.height(), sprite.u(), sprite.v(), sprite.width(), sprite.height());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** 绘制两像素斜面分隔线，与面板边框的明暗方向一致。 */
    public static void divider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIVIDER_DARK);
        graphics.fill(x, y + 1, x + width, y + 2, DIVIDER_LIGHT);
    }

    private static void blit(GuiGraphics graphics, Sprite sprite, int x, int y, int width, int height, int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        innerBlit(graphics, x, y, width, height, sprite.u() + sourceX, sprite.v() + sourceY, sourceWidth, sourceHeight);
    }

    /** 当前版本普通 blit 不开启混合，透明图标需要手动开启并在绘制后恢复。 */
    private static void innerBlit(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(TEXTURE, x, y, width, height, (float) u, (float) v, sourceWidth, sourceHeight, ATLAS_SIZE, ATLAS_SIZE);
        RenderSystem.disableBlend();
    }

    public record Sprite(int u, int v, int width, int height, int border) {
    }
}
