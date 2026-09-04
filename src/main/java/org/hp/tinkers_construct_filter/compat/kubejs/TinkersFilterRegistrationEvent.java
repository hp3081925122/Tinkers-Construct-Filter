package org.hp.tinkers_construct_filter.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.hp.tinkers_construct_filter.client.catalog.CatalogApi;
import org.hp.tinkers_construct_filter.client.catalog.CatalogExtensions;

public final class TinkersFilterRegistrationEvent extends EventJS {
    public void materialFilter(String id, String title, CatalogApi.MaterialFilter predicate) {
        CatalogExtensions.registerMaterialFilter(id, title, predicate);
    }

    public void partFilter(String id, String title, CatalogApi.PartFilter predicate) {
        CatalogExtensions.registerPartFilter(id, title, predicate);
    }

    public void materialSort(String id, String title, CatalogApi.MaterialSortValue valueGetter) {
        CatalogExtensions.registerMaterialSort(id, title, valueGetter);
    }

    public void partSort(String id, String title, CatalogApi.PartSortValue valueGetter) {
        CatalogExtensions.registerPartSort(id, title, valueGetter);
    }
}
