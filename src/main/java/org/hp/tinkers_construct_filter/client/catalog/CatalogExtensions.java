package org.hp.tinkers_construct_filter.client.catalog;

import org.hp.tinkers_construct_filter.TinkersConstructFilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogExtensions {
    private static final Map<String, MaterialFilterDefinition> MATERIAL_FILTERS = new LinkedHashMap<>();
    private static final Map<String, PartFilterDefinition> PART_FILTERS = new LinkedHashMap<>();
    private static final Map<String, MaterialSortDefinition> MATERIAL_SORTS = new LinkedHashMap<>();
    private static final Map<String, PartSortDefinition> PART_SORTS = new LinkedHashMap<>();
    private static final Set<String> INVALID_ITEMS = new LinkedHashSet<>();
    private static Runnable refreshHook = () -> {
    };

    private CatalogExtensions() {
    }

    public static synchronized void setKubeJsRefreshHook(Runnable hook) {
        refreshHook = hook == null ? () -> {
        } : hook;
    }

    public static void refreshDefinitions() {
        Runnable hook;
        synchronized (CatalogExtensions.class) {
            MATERIAL_FILTERS.clear();
            PART_FILTERS.clear();
            MATERIAL_SORTS.clear();
            PART_SORTS.clear();
            INVALID_ITEMS.clear();
            hook = refreshHook;
        }

        try {
            hook.run();
        } catch (RuntimeException exception) {
            TinkersConstructFilter.LOGGER.error("Failed to refresh Tinkers Filter KubeJS definitions", exception);
        }
    }

    public static synchronized void registerMaterialFilter(String id, String title, CatalogApi.MaterialFilter predicate) {
        String normalizedId = normalizeId(id);
        if (normalizedId != null && predicate != null) {
            MATERIAL_FILTERS.put(normalizedId, new MaterialFilterDefinition(normalizedId, normalizeTitle(title, normalizedId), predicate));
        }
    }

    public static synchronized void registerPartFilter(String id, String title, CatalogApi.PartFilter predicate) {
        String normalizedId = normalizeId(id);
        if (normalizedId != null && predicate != null) {
            PART_FILTERS.put(normalizedId, new PartFilterDefinition(normalizedId, normalizeTitle(title, normalizedId), predicate));
        }
    }

    public static synchronized void registerMaterialSort(String id, String title, CatalogApi.MaterialSortValue valueGetter) {
        String normalizedId = normalizeId(id);
        if (normalizedId != null && valueGetter != null) {
            MATERIAL_SORTS.put(normalizedId, new MaterialSortDefinition(normalizedId, normalizeTitle(title, normalizedId), valueGetter));
        }
    }

    public static synchronized void registerPartSort(String id, String title, CatalogApi.PartSortValue valueGetter) {
        String normalizedId = normalizeId(id);
        if (normalizedId != null && valueGetter != null) {
            PART_SORTS.put(normalizedId, new PartSortDefinition(normalizedId, normalizeTitle(title, normalizedId), valueGetter));
        }
    }

    public static synchronized List<MaterialFilterDefinition> materialFilters() {
        return activeDefinitions(MATERIAL_FILTERS, "material-filter");
    }

    public static synchronized List<PartFilterDefinition> partFilters() {
        return activeDefinitions(PART_FILTERS, "part-filter");
    }

    public static synchronized List<MaterialSortDefinition> materialSorts() {
        return activeDefinitions(MATERIAL_SORTS, "material-sort");
    }

    public static synchronized List<PartSortDefinition> partSorts() {
        return activeDefinitions(PART_SORTS, "part-sort");
    }

    public static boolean test(MaterialFilterDefinition definition, CatalogApi.MaterialView view) {
        String key = "material-filter:" + definition.id();
        if (isInvalid(key)) {
            return true;
        }
        try {
            return definition.predicate().test(view);
        } catch (RuntimeException exception) {
            disable(key, exception);
            return true;
        }
    }

    public static boolean test(PartFilterDefinition definition, CatalogApi.PartView view) {
        String key = "part-filter:" + definition.id();
        if (isInvalid(key)) {
            return true;
        }
        try {
            return definition.predicate().test(view);
        } catch (RuntimeException exception) {
            disable(key, exception);
            return true;
        }
    }

    public static Object value(MaterialSortDefinition definition, CatalogApi.MaterialView view) {
        return normalizeSortValue("material-sort:" + definition.id(), () -> definition.valueGetter().getValue(view));
    }

    public static Object value(PartSortDefinition definition, CatalogApi.PartView view) {
        return normalizeSortValue("part-sort:" + definition.id(), () -> definition.valueGetter().getValue(view));
    }

    private static synchronized <T> List<T> activeDefinitions(Map<String, T> definitions, String category) {
        List<T> result = new ArrayList<>();
        for (Map.Entry<String, T> entry : definitions.entrySet()) {
            if (!INVALID_ITEMS.contains(category + ":" + entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return List.copyOf(result);
    }

    private static Object normalizeSortValue(String key, ValueSupplier supplier) {
        if (isInvalid(key)) {
            return null;
        }
        try {
            Object value = supplier.get();
            if (value == null || value instanceof CharSequence) {
                return value == null ? null : value.toString();
            }
            if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                return number.doubleValue();
            }
            throw new IllegalArgumentException("Sort values must be numbers or text");
        } catch (RuntimeException exception) {
            disable(key, exception);
            return null;
        }
    }

    private static synchronized boolean isInvalid(String key) {
        return INVALID_ITEMS.contains(key);
    }

    private static synchronized void disable(String key, RuntimeException exception) {
        if (INVALID_ITEMS.add(key)) {
            TinkersConstructFilter.LOGGER.error("Skipping invalid Tinkers Filter KubeJS definition '{}'", key, exception);
        }
    }

    private static String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        String result = id.trim();
        return result.isEmpty() ? null : result;
    }

    private static String normalizeTitle(String title, String fallback) {
        return title == null || title.isBlank() ? fallback : title.trim();
    }

    @FunctionalInterface
    private interface ValueSupplier {
        Object get();
    }

    public record MaterialFilterDefinition(String id, String title, CatalogApi.MaterialFilter predicate) {
    }

    public record PartFilterDefinition(String id, String title, CatalogApi.PartFilter predicate) {
    }

    public record MaterialSortDefinition(String id, String title, CatalogApi.MaterialSortValue valueGetter) {
    }

    public record PartSortDefinition(String id, String title, CatalogApi.PartSortValue valueGetter) {
    }
}
