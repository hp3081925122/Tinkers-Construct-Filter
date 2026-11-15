package org.hp.tinkers_construct_filter.client.screen.overlay;

import org.hp.tinkers_construct_filter.client.catalog.CatalogEntry;

public final class CatalogOverlayController {
    public enum ModalType {
        NONE,
        HISTORY,
        FILTER,
        SORT,
        IMPORTANT
    }

    private ModalType modalType = ModalType.NONE;
    private int modalScroll;
    private int modalContentHeight;
    private int modalViewportHeight;
    private Bounds modalBounds = Bounds.EMPTY;

    private CatalogEntry inspectedEntry;
    private boolean detailPinned;
    private int detailScroll;
    private int detailContentHeight;
    private int detailViewportHeight;
    private Bounds detailBounds = Bounds.EMPTY;
    private Bounds pinButtonBounds = Bounds.EMPTY;

    public void openModal(ModalType type) {
        this.modalType = type;
        this.modalScroll = 0;
        this.modalContentHeight = 0;
        this.modalViewportHeight = 0;
        this.modalBounds = Bounds.EMPTY;
    }

    public void closeModal() {
        this.modalType = ModalType.NONE;
        this.modalScroll = 0;
        this.modalContentHeight = 0;
        this.modalViewportHeight = 0;
        this.modalBounds = Bounds.EMPTY;
    }

    public ModalType modalType() {
        return modalType;
    }

    public boolean isModalOpen() {
        return modalType != ModalType.NONE;
    }

    public boolean isModalOpen(ModalType expected) {
        return modalType == expected;
    }

    public int modalScroll() {
        return modalScroll;
    }

    public void setModalScroll(int scroll, int maximum) {
        this.modalScroll = clamp(scroll, 0, Math.max(0, maximum));
    }

    public void scrollModalBy(int amount) {
        setModalScroll(modalScroll + amount, Math.max(0, modalContentHeight - modalViewportHeight));
    }

    public void setModalMetrics(int contentHeight, int viewportHeight) {
        this.modalContentHeight = Math.max(0, contentHeight);
        this.modalViewportHeight = Math.max(0, viewportHeight);
        setModalScroll(modalScroll, Math.max(0, this.modalContentHeight - this.modalViewportHeight));
    }

    public void setModalBounds(int x, int y, int width, int height) {
        this.modalBounds = new Bounds(x, y, width, height);
    }

    public boolean isOverModal(double mouseX, double mouseY) {
        return isModalOpen() && modalBounds.contains(mouseX, mouseY);
    }

    public void inspect(CatalogEntry entry, boolean pin) {
        if (entry == null) {
            clearDetail();
            return;
        }
        if (this.inspectedEntry != entry) {
            this.detailScroll = 0;
            this.detailContentHeight = 0;
            this.detailViewportHeight = 0;
        }
        this.inspectedEntry = entry;
        if (pin) {
            this.detailPinned = true;
        }
    }

    public void togglePin() {
        if (this.inspectedEntry != null) {
            this.detailPinned = !this.detailPinned;
        }
    }

    public void setDetailPinned(boolean pinned) {
        this.detailPinned = pinned && this.inspectedEntry != null;
    }

    public void clearDetail() {
        this.inspectedEntry = null;
        this.detailPinned = false;
        this.detailScroll = 0;
        this.detailContentHeight = 0;
        this.detailViewportHeight = 0;
        this.detailBounds = Bounds.EMPTY;
        this.pinButtonBounds = Bounds.EMPTY;
    }

    public CatalogEntry inspectedEntry() {
        return inspectedEntry;
    }

    public boolean hasDetail() {
        return inspectedEntry != null;
    }

    public boolean isDetailPinned() {
        return detailPinned && inspectedEntry != null;
    }

    public int detailScroll() {
        return detailScroll;
    }

    public int detailMaximumScroll() {
        return Math.max(0, detailContentHeight - detailViewportHeight);
    }

    public void setDetailScroll(int scroll) {
        this.detailScroll = clamp(scroll, 0, detailMaximumScroll());
    }

    public void scrollDetailBy(int amount) {
        setDetailScroll(detailScroll + amount);
    }

    public int detailContentHeight() {
        return detailContentHeight;
    }

    public int detailViewportHeight() {
        return detailViewportHeight;
    }

    public void setDetailMetrics(int contentHeight, int viewportHeight) {
        this.detailContentHeight = Math.max(0, contentHeight);
        this.detailViewportHeight = Math.max(0, viewportHeight);
        setDetailScroll(detailScroll);
    }

    public void setDetailBounds(int x, int y, int width, int height) {
        this.detailBounds = new Bounds(x, y, width, height);
    }

    public int detailX() {
        return detailBounds.x();
    }

    public int detailY() {
        return detailBounds.y();
    }

    public void setPinButtonBounds(int x, int y, int width, int height) {
        this.pinButtonBounds = new Bounds(x, y, width, height);
    }

    public boolean isOverDetail(double mouseX, double mouseY) {
        return hasDetail() && detailBounds.contains(mouseX, mouseY);
    }

    public boolean isOverPinButton(double mouseX, double mouseY) {
        return hasDetail() && pinButtonBounds.contains(mouseX, mouseY);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Bounds(int x, int y, int width, int height) {
        private static final Bounds EMPTY = new Bounds(0, 0, 0, 0);

        private boolean contains(double mouseX, double mouseY) {
            return width > 0 && height > 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
