package org.hp.tinkers_construct_filter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import org.hp.tinkers_construct_filter.client.screen.CatalogButton;
import org.hp.tinkers_construct_filter.client.screen.TinkersCatalogScreen;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;
import slimeknights.tconstruct.tables.client.inventory.ModifierWorktableScreen;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

import java.lang.reflect.Method;
import java.util.List;

public final class ScreenWidgetRegistrar {
    private static final Method ADD_RENDERABLE_WIDGET = findAddRenderableWidget();

    private ScreenWidgetRegistrar() {
    }

    public static void addRenderableWidget(Screen screen, GuiEventListener widget) {
        try {
            ADD_RENDERABLE_WIDGET.invoke(screen, widget);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register Screen widget", exception);
        }
    }

    public static boolean supportsCatalogButton(BaseTabbedScreen<?, ?> screen) {
        return screen instanceof TinkerStationScreen
            || screen instanceof PartBuilderScreen
            || screen instanceof ModifierWorktableScreen;
    }

    public static List<Rect2i> safeModuleAreas(BaseTabbedScreen<?, ?> screen) {
        try {
            List<Rect2i> areas = screen.getModuleAreas();
            return areas == null ? List.of() : areas;
        } catch (RuntimeException exception) {
            TinkersConstructFilter.LOGGER.debug("Deferred module area query for {}: {}", screen.getClass().getSimpleName(), exception.toString());
            return List.of();
        }
    }

    public static Rect2i computeStationButtonArea(BaseTabbedScreen<?, ?> screen, List<Rect2i> rawModuleAreas) {
        int mainRight = screen.cornerX + screen.realWidth;
        int rightEdge = Math.max(mainRight, screen.getGuiLeft() + screen.getXSize());
        if (rawModuleAreas != null) {
            for (Rect2i area : rawModuleAreas) {
                if (area != null && area.getX() >= mainRight) {
                    rightEdge = Math.max(rightEdge, area.getX() + area.getWidth());
                }
            }
        }

        int buttonX;
        int buttonY;
        if (rightEdge >= mainRight + 24 && screen.cornerY >= 24) {
            buttonX = Math.min(Math.max(2, screen.width - 24), rightEdge - 22);
            buttonY = screen.cornerY - 24;
        } else {
            buttonX = Math.min(Math.max(2, screen.width - 24), rightEdge + 4);
            buttonY = Math.max(2, screen.cornerY);
        }
        Rect2i result = new Rect2i(buttonX, buttonY, 22, 22);
        TinkersConstructFilter.LOGGER.debug(
            "Positioned {} catalog button at ({}, {}), screen={}x{}, main=({}, {}, {}x{}), gui=({}, {}, {}x{}), rightEdge={}",
            screen.getClass().getSimpleName(),
            buttonX,
            buttonY,
            screen.width,
            screen.height,
            screen.cornerX,
            screen.cornerY,
            screen.realWidth,
            screen.realHeight,
            screen.getGuiLeft(),
            screen.getGuiTop(),
            screen.getXSize(),
            screen.getYSize(),
            rightEdge
        );
        return result;
    }

    public static CatalogButton createStationButton(BaseTabbedScreen<?, ?> screen, Rect2i area) {
        TinkersCatalogScreen.InitialTab initialTab = screen instanceof PartBuilderScreen
            ? TinkersCatalogScreen.InitialTab.PARTS
            : screen instanceof ModifierWorktableScreen
                ? TinkersCatalogScreen.InitialTab.MODIFIERS
                : TinkersCatalogScreen.InitialTab.MATERIALS;
        CatalogButton button = CatalogButton.createStationEntry(
            area.getX(),
            area.getY(),
            Component.translatable("button.tinkers_construct_filter.open_catalog"),
            pressed -> Minecraft.getInstance().setScreen(new TinkersCatalogScreen(screen, initialTab))
        );
        addRenderableWidget(screen, button);
        return button;
    }

    private static Method findAddRenderableWidget() {
        for (String methodName : new String[]{"addRenderableWidget", "m_142416_"}) {
            try {
                Method method = Screen.class.getDeclaredMethod(methodName, GuiEventListener.class);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException | SecurityException ignored) {
            }
        }
        throw new ExceptionInInitializerError("Unable to find Screen widget registration method");
    }
}
