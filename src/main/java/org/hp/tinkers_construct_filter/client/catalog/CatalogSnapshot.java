package org.hp.tinkers_construct_filter.client.catalog;

import java.util.List;

// 全词条与可制作强化共用条目对象，分别提供独立列表。
public record CatalogSnapshot(boolean fullyLoaded, List<CatalogEntry> materials, List<CatalogEntry> parts, List<CatalogEntry> modifiers, List<CatalogEntry> traits) {
    public static CatalogSnapshot loading() {
        return new CatalogSnapshot(false, List.of(), List.of(), List.of(), List.of());
    }
}
