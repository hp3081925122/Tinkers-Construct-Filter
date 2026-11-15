package org.hp.tinkers_construct_filter.mixin.client;

import org.hp.tinkers_construct_filter.client.StationCatalogButtonAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

@Mixin(TinkerStationScreen.class)
public abstract class TinkerStationScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void tinkersConstructFilter$repositionAfterInit(CallbackInfo callbackInfo) {
        ((StationCatalogButtonAccess) this).tinkersConstructFilter$repositionButton();
    }
}
