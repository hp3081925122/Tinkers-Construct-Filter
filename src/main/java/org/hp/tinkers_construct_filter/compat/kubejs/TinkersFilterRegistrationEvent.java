package org.hp.tinkers_construct_filter.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import org.hp.tinkers_construct_filter.client.catalog.CatalogApi;
import org.hp.tinkers_construct_filter.client.catalog.CatalogExtensions;

import java.util.List;

public final class TinkersFilterRegistrationEvent extends EventJS {
    /** 为指定页面新增分类；分类内使用 any 或 all，分类之间取交集。 */
    public void filterCategory(String page, String id, String title, String mode) {
        CatalogExtensions.registerFilterCategory(page, id, title, mode);
    }

    /** 带分类的材料筛选，三个参数的旧入口仍然有效。 */
    public void materialFilter(String category, String id, String title, CatalogApi.MaterialFilter predicate) {
        CatalogExtensions.registerFilter("materials", category, id, title, view -> predicate.test((CatalogApi.MaterialView) view));
    }

    /** 带分类的部件筛选。 */
    public void partFilter(String category, String id, String title, CatalogApi.PartFilter predicate) {
        CatalogExtensions.registerFilter("parts", category, id, title, view -> predicate.test((CatalogApi.PartView) view));
    }

    /** 强化页默认自定义分类入口。 */
    public void modifierFilter(String id, String title, CatalogApi.ModifierFilter predicate) {
        modifierFilter("custom", id, title, predicate);
    }

    /** 强化页分类筛选，不影响全词条页。 */
    public void modifierFilter(String category, String id, String title, CatalogApi.ModifierFilter predicate) {
        CatalogExtensions.registerFilter("modifiers", category, id, title, view -> predicate.test((CatalogApi.ModifierView) view));
    }

    /** 词条页默认自定义分类入口。 */
    public void traitFilter(String id, String title, CatalogApi.ModifierFilter predicate) {
        traitFilter("custom", id, title, predicate);
    }

    /** 全词条页分类筛选，包含有配方及无配方词条。 */
    public void traitFilter(String category, String id, String title, CatalogApi.ModifierFilter predicate) {
        CatalogExtensions.registerFilter("traits", category, id, title, view -> predicate.test((CatalogApi.ModifierView) view));
    }

    /** 强化页数值或文本排序。 */
    public void modifierSort(String id, String title, CatalogApi.ModifierSortValue getter) {
        CatalogExtensions.registerSort("modifiers", id, title, view -> getter.getValue((CatalogApi.ModifierView) view));
    }

    /** 全词条页数值或文本排序。 */
    public void traitSort(String id, String title, CatalogApi.ModifierSortValue getter) {
        CatalogExtensions.registerSort("traits", id, title, view -> getter.getValue((CatalogApi.ModifierView) view));
    }

    /** 覆盖指定词条的效果分类，支持多个分类，不限定模组命名空间。 */
    public void traitCategories(String modifierId, List<String> categories) {
        CatalogExtensions.registerTraitCategories(modifierId, categories);
    }

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
