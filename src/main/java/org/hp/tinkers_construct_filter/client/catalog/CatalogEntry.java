package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface CatalogEntry extends CatalogApi.EntryView {
    ItemStack getDisplayStack();

    String getSearchText();

    List<TraitTooltip> getTraitTooltips();

    record TraitTooltip(String id, String name, List<String> descriptions) {
        public TraitTooltip(String name, List<String> descriptions) {
            this("", name, descriptions);
        }

        public TraitTooltip {
            id = id == null ? "" : id;
            descriptions = List.copyOf(descriptions);
        }
    }
}
