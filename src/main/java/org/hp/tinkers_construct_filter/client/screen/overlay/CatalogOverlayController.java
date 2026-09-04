package org.hp.tinkers_construct_filter.client.screen.overlay;

import org.hp.tinkers_construct_filter.client.catalog.CatalogEntry;

/** 统一管理图鉴界面的弹层状态、命中范围、锁定状态和滚动位置。 */
public final class CatalogOverlayController {
    public enum Type {
        NONE,
        HISTORY,
        FILTER,
        SORT,
        IMPORTANT,
        MATERIAL_INFO,
        MODIFIER_INFO
    }

    private Type type = Type.NONE;
    private CatalogEntry target;
    private boolean locked;
    private int scroll;
    private int contentHeight;
    private int viewportHeight;
    private Bounds bounds = Bounds.EMPTY;

    /** 打开一个没有目标条目的选择弹层，并清理旧的目标和锁定状态。 */
    public void open(Type type) {
        this.type = type;
        this.target = null;
        this.locked = false;
        this.scroll = 0;
        this.contentHeight = 0;
        this.viewportHeight = 0;
        this.bounds = Bounds.EMPTY;
    }

    /** 选择一个信息弹层目标；目标发生变化时只重置一次滚动位置。 */
    public void showDetail(Type type, CatalogEntry target) {
        if (this.type != type || this.target != target) {
            this.scroll = 0;
            this.locked = false;
            this.contentHeight = 0;
            this.viewportHeight = 0;
        }
        this.type = type;
        this.target = target;
        this.bounds = Bounds.EMPTY;
    }

    /** 关闭当前弹层并清理所有交互状态。 */
    public void clear() {
        type = Type.NONE;
        target = null;
        locked = false;
        scroll = 0;
        contentHeight = 0;
        viewportHeight = 0;
        bounds = Bounds.EMPTY;
    }

    public Type type() {
        return type;
    }

    public CatalogEntry target() {
        return target;
    }

    public boolean isOpen() {
        return type != Type.NONE;
    }

    public boolean isOpen(Type expected) {
        return type == expected;
    }

    public boolean isModalOpen() {
        return type == Type.HISTORY || type == Type.FILTER || type == Type.SORT || type == Type.IMPORTANT;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        locked = true;
    }

    public int scroll() {
        return scroll;
    }

    public void setScroll(int scroll, int maximum) {
        this.scroll = clamp(scroll, 0, Math.max(0, maximum));
    }

    public void scrollBy(int amount, int maximum) {
        setScroll(scroll + amount, maximum);
    }

    public void scrollBy(int amount) {
        setScroll(scroll + amount, maximumScroll());
    }

    public int contentHeight() {
        return contentHeight;
    }

    public int viewportHeight() {
        return viewportHeight;
    }

    public int maximumScroll() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    /** 更新当前弹层内容尺寸，并将滚动位置限制在新的内容范围内。 */
    public void setContentMetrics(int contentHeight, int viewportHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        this.viewportHeight = Math.max(0, viewportHeight);
        setScroll(scroll, maximumScroll());
    }

    /** 更新当前弹层矩形，后续命中测试和鼠标事件都使用同一份矩形。 */
    public void setBounds(int x, int y, int width, int height) {
        bounds = new Bounds(x, y, width, height);
    }

    public boolean isOver(double mouseX, double mouseY) {
        return isOpen() && bounds.contains(mouseX, mouseY);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Bounds(int x, int y, int width, int height) {
        private static final Bounds EMPTY = new Bounds(0, 0, 0, 0);

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
