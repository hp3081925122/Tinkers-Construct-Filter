package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface CatalogEntry extends CatalogApi.EntryView {
    ItemStack getDisplayStack();

    String getSearchText();

    List<TraitTooltip> getTraitTooltips();

    record TraitTooltip(String name, List<String> descriptions) {
        public TraitTooltip {
            descriptions = List.copyOf(descriptions);
        }
    }
}
