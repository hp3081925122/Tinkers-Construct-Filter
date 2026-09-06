package org.hp.tinkers_construct_filter.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import org.hp.tinkers_construct_filter.client.catalog.CatalogApi;
import org.hp.tinkers_construct_filter.client.catalog.CatalogExtensions;

public final class TinkersFilterKubeJsPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        TinkersFilterKubeJsEvents.register();
    }

    @Override
    public void clientInit() {
        CatalogExtensions.setKubeJsRefreshHook(TinkersFilterKubeJsEvents::postRegistration);
    }

    @Override
    public void registerClasses(ScriptType scriptType, ClassFilter filter) {
        if (scriptType != ScriptType.CLIENT) {
            return;
        }
        filter.allow(TinkersFilterRegistrationEvent.class);
        filter.allow(CatalogApi.EntryView.class);
        filter.allow(CatalogApi.MaterialView.class);
        filter.allow(CatalogApi.PartView.class);
        filter.allow(CatalogApi.MaterialFilter.class);
        filter.allow(CatalogApi.PartFilter.class);
        filter.allow(CatalogApi.MaterialSortValue.class);
        filter.allow(CatalogApi.PartSortValue.class);
        // 允许新回调及返回视图，保持仅在客户端开放。
        filter.allow(CatalogApi.ModifierView.class);
        filter.allow(CatalogApi.ModifierFilter.class);
        filter.allow(CatalogApi.ModifierSortValue.class);
        filter.allow(CatalogApi.ModifierRecipeView.class);
        filter.allow(org.hp.tinkers_construct_filter.client.catalog.CatalogEntry.class);
    }
}
