package org.hp.tinkers_construct_filter.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;

public final class JeiCompat {
    private JeiCompat() {
    }

    public static boolean handleKeyPressed(ItemStack hoveredStack, int keyCode, int scanCode) {
        if (hoveredStack == null || hoveredStack.isEmpty() || !ModList.get().isLoaded("jei")) {
            return false;
        }
        try {
            InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
            return JeiRuntimeBridge.handleKey(hoveredStack, key);
        } catch (Throwable throwable) {
            TinkersConstructFilter.LOGGER.debug("Skipping unavailable JEI keybind lookup", throwable);
            return false;
        }
    }
}
