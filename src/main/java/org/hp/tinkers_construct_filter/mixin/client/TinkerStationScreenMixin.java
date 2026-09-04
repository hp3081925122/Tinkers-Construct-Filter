package org.hp.tinkers_construct_filter.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.hp.tinkers_construct_filter.client.ScreenWidgetRegistrar;
import org.hp.tinkers_construct_filter.client.screen.TinkersCatalogScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

@Mixin(TinkerStationScreen.class)
public abstract class TinkerStationScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void tinkersConstructFilter$addBrowserButton(CallbackInfo callbackInfo) {
        TinkerStationScreen screen = (TinkerStationScreen) (Object) this;
        ScreenWidgetRegistrar.addRenderableWidget(screen, Button.builder(Component.translatable("button.tinkers_construct_filter.open_catalog"), button ->
            Minecraft.getInstance().setScreen(new TinkersCatalogScreen(screen)))
            .bounds(screen.cornerX + screen.realWidth + 4, screen.cornerY - 24, 80, 20)
            .build());
    }
}
