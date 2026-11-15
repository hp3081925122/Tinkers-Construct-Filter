package org.hp.tinkers_construct_filter.mixin.client;

import net.minecraft.client.renderer.Rect2i;
import org.hp.tinkers_construct_filter.client.ScreenWidgetRegistrar;
import org.hp.tinkers_construct_filter.client.StationCatalogButtonAccess;
import org.hp.tinkers_construct_filter.client.screen.CatalogButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

import java.util.ArrayList;
import java.util.List;

@Mixin(BaseTabbedScreen.class)
public abstract class BaseTabbedScreenMixin implements StationCatalogButtonAccess {
    @Unique
    private CatalogButton tinkersConstructFilter$button;

    @Unique
    private Rect2i tinkersConstructFilter$buttonArea;

    @Inject(method = "init", at = @At("TAIL"))
    private void tinkersConstructFilter$initCatalogButton(CallbackInfo callbackInfo) {
        BaseTabbedScreen<?, ?> screen = (BaseTabbedScreen<?, ?>) (Object) this;
        tinkersConstructFilter$button = null;
        tinkersConstructFilter$buttonArea = null;
        if (!ScreenWidgetRegistrar.supportsCatalogButton(screen) || screen instanceof TinkerStationScreen) {
            return;
        }
        tinkersConstructFilter$repositionButton();
    }

    @Override
    public void tinkersConstructFilter$repositionButton() {
        BaseTabbedScreen<?, ?> screen = (BaseTabbedScreen<?, ?>) (Object) this;
        if (!ScreenWidgetRegistrar.supportsCatalogButton(screen)) {
            return;
        }
        tinkersConstructFilter$buttonArea = null;
        Rect2i area = ScreenWidgetRegistrar.computeStationButtonArea(screen, ScreenWidgetRegistrar.safeModuleAreas(screen));
        if (tinkersConstructFilter$button == null) {
            tinkersConstructFilter$button = ScreenWidgetRegistrar.createStationButton(screen, area);
        } else {
            tinkersConstructFilter$button.moveTo(area.getX(), area.getY());
        }
        tinkersConstructFilter$buttonArea = area;
    }

    @Inject(method = "getModuleAreas", at = @At("RETURN"), cancellable = true, remap = false)
    private void tinkersConstructFilter$appendButtonArea(CallbackInfoReturnable<List<Rect2i>> cir) {
        if (tinkersConstructFilter$buttonArea == null) {
            return;
        }
        List<Rect2i> original = cir.getReturnValue();
        List<Rect2i> areas = original == null ? new ArrayList<>() : new ArrayList<>(original);
        areas.add(tinkersConstructFilter$buttonArea);
        cir.setReturnValue(areas);
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void tinkersConstructFilter$preventOutsideDropOnButton(
        double mouseX,
        double mouseY,
        int guiLeft,
        int guiTop,
        int mouseButton,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Rect2i area = tinkersConstructFilter$buttonArea;
        if (area != null && area.contains((int) mouseX, (int) mouseY)) {
            cir.setReturnValue(false);
        }
    }
}
