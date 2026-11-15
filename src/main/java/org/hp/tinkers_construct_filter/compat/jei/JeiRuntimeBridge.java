package org.hp.tinkers_construct_filter.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiKeyMappings;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;

final class JeiRuntimeBridge {
    private static volatile IJeiRuntime runtime;

    private JeiRuntimeBridge() {
    }

    static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static boolean handleKey(ItemStack stack, InputConstants.Key key) {
        IJeiRuntime active = runtime;
        if (active == null || stack.isEmpty()) {
            return false;
        }
        IJeiKeyMappings keyMappings = active.getKeyMappings();
        IFocusFactory focusFactory = active.getJeiHelpers().getFocusFactory();
        if (keyMappings.getShowRecipe().isActiveAndMatches(key)) {
            TinkersConstructFilter.LOGGER.debug("Opening JEI recipes for {}", stack.getItem());
            active.getRecipesGui().show(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack.copy()));
            return true;
        }
        if (keyMappings.getShowUses().isActiveAndMatches(key)) {
            TinkersConstructFilter.LOGGER.debug("Opening JEI uses for {}", stack.getItem());
            active.getRecipesGui().show(focusFactory.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack.copy()));
            return true;
        }
        return false;
    }
}
