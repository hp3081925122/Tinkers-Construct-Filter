package org.hp.tinkers_construct_filter.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

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
