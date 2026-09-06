package org.hp.tinkers_construct_filter.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;

/** 图鉴专用锻铁铜边材质，固定边角尺寸，不修改原版全局界面资源。 */
public final class CatalogSkin {
    // 图集使用归一化的逻辑尺寸，允许资源包替换为不同分辨率的同布局图片。
    public static final ResourceLocation TEXTURE = new ResourceLocation(TinkersConstructFilter.MOD_ID, "textures/gui/catalog_atlas.png");
    public static final int WINDOW = 0;
    public static final int SIDEBAR = 1;
    public static final int HEADER = 2;
    public static final int POPUP = 3;
    public static final int BUTTON = 4;
    public static final int BUTTON_HOVER = 5;
    public static final int BUTTON_SELECTED = 6;
    public static final int BUTTON_DISABLED = 7;
    public static final int ROW = 8;
    public static final int ROW_HOVER = 9;
    public static final int SLOT = 10;
    public static final int SLOT_HOVER = 11;
    public static final int INPUT = 12;
    public static final int INPUT_FOCUSED = 13;
    public static final int SCROLL_TRACK = 14;
    public static final int SCROLL_THUMB = 15;
    public static final int ACCENT = 0xFFFFCF87;
    private static final int ATLAS_SIZE = 256;
    private static final int CELL_SIZE = 64;
    private static final int SOURCE_BORDER = 10;
    private static final int BORDER = 3;

    private CatalogSkin() {
    }

    /** 绘制完整九宫格；物品槽使用单个四边形，避免每个物品增加九次绘制。 */
    public static void draw(GuiGraphics graphics, int sprite, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (sprite == SLOT || sprite == SLOT_HOVER || sprite == SCROLL_TRACK || sprite == SCROLL_THUMB) {
            blit(graphics, x, y, width, height, sprite % 4 * CELL_SIZE, sprite / 4 * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            return;
        }
        paint(graphics, sprite, x, y, width, height, true);
    }

    /** 搜索框只覆盖边框，保留原版文本、光标、选区和鼠标定位。 */
    public static void frame(GuiGraphics graphics, int sprite, int x, int y, int width, int height) {
        paint(graphics, sprite, x, y, width, height, false);
    }

    /** 每个面板最多九个四边形，中心拉伸而边角保持三个界面像素。 */
    private static void paint(GuiGraphics graphics, int sprite, int x, int y, int width, int height, boolean center) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int borderX = Math.min(BORDER, width / 2);
        int borderY = Math.min(BORDER, height / 2);
        int u = sprite % 4 * CELL_SIZE;
        int v = sprite / 4 * CELL_SIZE;
        for (int row = 0; row < 3; row++) {
            int top = row == 0 ? 0 : row == 1 ? borderY : height - borderY;
            int drawHeight = row == 1 ? height - borderY * 2 : borderY;
            int sourceY = row == 0 ? 0 : row == 1 ? SOURCE_BORDER : CELL_SIZE - SOURCE_BORDER;
            int sourceHeight = row == 1 ? CELL_SIZE - SOURCE_BORDER * 2 : SOURCE_BORDER;
            for (int column = 0; column < 3; column++) {
                // 边框模式跳过中心，极小尺寸跳过零面积区域，避免反向拉伸。
                if (!center && row == 1 && column == 1) {
                    continue;
                }
                int left = column == 0 ? 0 : column == 1 ? borderX : width - borderX;
                int drawWidth = column == 1 ? width - borderX * 2 : borderX;
                int sourceX = column == 0 ? 0 : column == 1 ? SOURCE_BORDER : CELL_SIZE - SOURCE_BORDER;
                int sourceWidth = column == 1 ? CELL_SIZE - SOURCE_BORDER * 2 : SOURCE_BORDER;
                if (drawWidth > 0 && drawHeight > 0) {
                    blit(graphics, x + left, y + top, drawWidth, drawHeight, u + sourceX, v + sourceY, sourceWidth, sourceHeight);
                }
            }
        }
    }

    /** 使用当前版本已核对的缩放纹理接口，清除物品渲染遗留的颜色调制。 */
    private static void blit(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, x, y, width, height, (float) u, (float) v, sourceWidth, sourceHeight, ATLAS_SIZE, ATLAS_SIZE);
    }
}
