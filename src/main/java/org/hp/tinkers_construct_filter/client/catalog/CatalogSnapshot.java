package org.hp.tinkers_construct_filter.client.catalog;

import java.util.List;

public record CatalogSnapshot(boolean fullyLoaded, List<CatalogEntry> materials, List<CatalogEntry> parts, List<CatalogEntry> modifiers) {
    public static CatalogSnapshot loading() {
        return new CatalogSnapshot(false, List.of(), List.of(), List.of());
    }
}
