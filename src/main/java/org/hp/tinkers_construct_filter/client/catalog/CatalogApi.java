package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public final class CatalogApi {
    private CatalogApi() {
    }

    public interface EntryView {
        String getId();

        String getName();

        int getMaterialLevel();

        List<String> getTraitNames();

        List<String> getTraitDescriptions();

        Map<String, Double> getAttributeValues();

        Map<String, String> getAttributeTexts();
    }

    public interface MaterialView extends EntryView {
        int getDefaultSortOrder();

        int getAvailablePartCount();

        ItemStack getMaterialDisplayItem();
    }

    public interface PartView extends EntryView {
        String getMaterialId();

        String getMaterialName();

        String getPartId();

        String getPartType();

        String getPartTypeName();

        ItemStack getMaterializedItem();
    }

    public interface ModifierView extends EntryView {
        Map<String, String> getSlotCategories();

        Map<String, String> getToolCategories();

        List<String> getModifierDescriptions();

        List<ItemStack> getRecipeTools();

        List<List<ItemStack>> getRecipeInputs();
    }

    @FunctionalInterface
    public interface MaterialFilter {
        boolean test(MaterialView view);
    }

    @FunctionalInterface
    public interface PartFilter {
        boolean test(PartView view);
    }

    @FunctionalInterface
    public interface MaterialSortValue {
        Object getValue(MaterialView view);
    }

    @FunctionalInterface
    public interface PartSortValue {
        Object getValue(PartView view);
    }
}
