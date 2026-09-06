package org.hp.tinkers_construct_filter.client.catalog;

import org.hp.tinkers_construct_filter.TinkersConstructFilter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 四页共用的客户端脚本扩展注册表，不修改配方及工具定义。 */
public final class CatalogExtensions {
    private static final Set<String> PAGES = Set.of("materials", "parts", "modifiers", "traits");
    private static final Map<String, FilterDefinition> FILTERS = new LinkedHashMap<>();
    private static final Map<String, SortDefinition> SORTS = new LinkedHashMap<>();
    private static final Map<String, FilterCategoryDefinition> CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, List<TraitCategory>> TRAIT_CATEGORIES = new LinkedHashMap<>();
    private static final Set<String> INVALID_ITEMS = new LinkedHashSet<>();
    private static Runnable refreshHook = () -> { };

    private CatalogExtensions() {
    }

    /** 未安装 KubeJS 时使用空钩子，目录功能不依赖脚本模组。 */
    public static synchronized void setKubeJsRefreshHook(Runnable hook) {
        refreshHook = hook == null ? () -> { } : hook;
    }

    /** 每轮重新注册前清空旧定义及失效状态，删除脚本后不残留旧分类。 */
    public static void refreshDefinitions() {
        Runnable hook;
        synchronized (CatalogExtensions.class) {
            FILTERS.clear();
            SORTS.clear();
            CATEGORIES.clear();
            TRAIT_CATEGORIES.clear();
            INVALID_ITEMS.clear();
            hook = refreshHook;
        }
        try {
            hook.run();
        } catch (RuntimeException exception) {
            TinkersConstructFilter.LOGGER.error("Failed to refresh Tinkers Filter KubeJS definitions", exception);
        }
        TinkersConstructFilter.LOGGER.debug("Tinkers Filter definitions refreshed: filters={}, sorts={}, categories={}, traitOverrides={}",
            FILTERS.size(), SORTS.size(), CATEGORIES.size(), TRAIT_CATEGORIES.size());
    }

    /** 保留原材料入口；旧脚本进入全部满足的默认自定义分类。 */
    public static void registerMaterialFilter(String id, String title, CatalogApi.MaterialFilter predicate) {
        registerFilter("materials", "custom", id, title, predicate == null ? null : view -> predicate.test((CatalogApi.MaterialView) view));
    }

    /** 保留原部件入口，与材料使用同一条注册和执行路径。 */
    public static void registerPartFilter(String id, String title, CatalogApi.PartFilter predicate) {
        registerFilter("parts", "custom", id, title, predicate == null ? null : view -> predicate.test((CatalogApi.PartView) view));
    }

    /** 保留原材料排序入口。 */
    public static void registerMaterialSort(String id, String title, CatalogApi.MaterialSortValue getter) {
        registerSort("materials", id, title, getter == null ? null : view -> getter.getValue((CatalogApi.MaterialView) view));
    }

    /** 保留原部件排序入口。 */
    public static void registerPartSort(String id, String title, CatalogApi.PartSortValue getter) {
        registerSort("parts", id, title, getter == null ? null : view -> getter.getValue((CatalogApi.PartView) view));
    }

    /** 分类按页面隔离，同名分类后注册覆盖前一次定义。 */
    public static synchronized void registerFilterCategory(String page, String id, String title, String mode) {
        page = requirePage(page);
        id = requireId(id);
        CATEGORIES.put(key(page, id), new FilterCategoryDefinition(page, id, normalizeTitle(title, id), FilterMatchMode.parse(mode)));
    }

    /** 四页筛选共用身份空间，同页面同标识只保留最后一次注册。 */
    public static synchronized void registerFilter(String page, String category, String id, String title, CatalogApi.EntryFilter predicate) {
        page = requirePage(page);
        category = requireId(category);
        id = requireId(id);
        if (predicate == null) {
            throw new IllegalArgumentException("Filter predicate must not be null");
        }
        FILTERS.put(key(page, id), new FilterDefinition(page, category, id, normalizeTitle(title, id), predicate));
    }

    /** 统一排序入口，值仍限制为有限数字、文本或空值。 */
    public static synchronized void registerSort(String page, String id, String title, CatalogApi.EntrySortValue getter) {
        page = requirePage(page);
        id = requireId(id);
        if (getter == null) {
            throw new IllegalArgumentException("Sort value getter must not be null");
        }
        SORTS.put(key(page, id), new SortDefinition(page, id, normalizeTitle(title, id), getter));
    }

    /** 脚本可覆盖任意命名空间的效果归类，空列表明确归为其他。 */
    public static synchronized void registerTraitCategories(String modifierId, List<String> categories) {
        modifierId = requireId(modifierId);
        if (!modifierId.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || categories == null) {
            throw new IllegalArgumentException("Trait categories require a full modifier ID and a category list");
        }
        Set<TraitCategory> resolved = new LinkedHashSet<>();
        for (String id : categories) {
            TraitCategory category = java.util.Arrays.stream(TraitCategory.values())
                .filter(value -> value.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown trait category: " + id));
            resolved.add(category);
        }
        // 其他仅作兜底，不能与已有明确分类同时命中。
        if (resolved.size() > 1) {
            resolved.remove(TraitCategory.OTHER);
        }
        TRAIT_CATEGORIES.put(modifierId, resolved.isEmpty() ? List.of(TraitCategory.OTHER) : List.copyOf(resolved));
    }

    /** 没有脚本覆盖时完全沿用当前版本内置词条分类。 */
    public static synchronized List<TraitCategory> traitCategories(String modifierId) {
        List<TraitCategory> override = TRAIT_CATEGORIES.get(modifierId);
        return override == null ? TraitCategory.classify(modifierId) : override;
    }

    /** 返回当前页面有效筛选，未定义分类单独记录并跳过，不混入其他分类。 */
    public static synchronized List<FilterDefinition> filters(String page) {
        return FILTERS.values().stream().filter(definition -> definition.page().equals(page))
            .filter(definition -> !INVALID_ITEMS.contains("filter:" + key(page, definition.id())))
            .filter(definition -> {
                if (definition.category().equals("custom") || CATEGORIES.containsKey(key(page, definition.category()))) {
                    return true;
                }
                String missing = "category:" + key(page, definition.category());
                if (INVALID_ITEMS.add(missing)) {
                    TinkersConstructFilter.LOGGER.error("Skipping filters with unregistered category: page={}, category={}", page, definition.category());
                }
                return false;
            }).toList();
    }

    /** 分类标题为空时由界面使用现有本地化的自定义标题。 */
    public static synchronized FilterCategoryDefinition filterCategory(String page, String category) {
        return CATEGORIES.getOrDefault(key(page, category),
            new FilterCategoryDefinition(page, category, "", FilterMatchMode.ALL));
    }

    /** 排序定义的顺序与脚本注册顺序一致。 */
    public static synchronized List<SortDefinition> sorts(String page) {
        return SORTS.values().stream().filter(definition -> definition.page().equals(page))
            .filter(definition -> !INVALID_ITEMS.contains("sort:" + key(page, definition.id()))).toList();
    }

    /** 回调异常只停用当前定义并放行条目，防止一个脚本让整个目录不可用。 */
    public static boolean test(FilterDefinition definition, CatalogApi.EntryView view) {
        String key = "filter:" + key(definition.page(), definition.id());
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

    /** 拒绝布尔、对象及非有限数值，不让不稳定比较污染排序结果。 */
    public static Object value(SortDefinition definition, CatalogApi.EntryView view) {
        String key = "sort:" + key(definition.page(), definition.id());
        if (isInvalid(key)) {
            return null;
        }
        try {
            Object value = definition.valueGetter().getValue(view);
            if (value == null || value instanceof CharSequence) {
                return value == null ? null : value.toString();
            }
            if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                return number.doubleValue();
            }
            throw new IllegalArgumentException("Sort values must be finite numbers or text");
        } catch (RuntimeException exception) {
            disable(key, exception);
            return null;
        }
    }

    /** 失效状态只记录到下次目录刷新。 */
    private static synchronized boolean isInvalid(String key) {
        return INVALID_ITEMS.contains(key);
    }

    /** 每个定义最多输出一次异常日志。 */
    private static synchronized void disable(String key, RuntimeException exception) {
        if (INVALID_ITEMS.add(key)) {
            TinkersConstructFilter.LOGGER.error("Skipping invalid Tinkers Filter KubeJS definition '{}'", key, exception);
        }
    }

    /** 页面使用固定复数标识，避免拼写错误注册到永远不显示的页面。 */
    private static String requirePage(String page) {
        if (!PAGES.contains(page == null ? "" : page)) {
            throw new IllegalArgumentException("Page must be materials, parts, modifiers or traits");
        }
        return page;
    }

    /** 禁止空标识，标题则可回退到标识本身。 */
    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Definition ID must not be blank");
        }
        return id.trim();
    }

    /** 页面前缀隔离各自的注册标识。 */
    private static String key(String page, String id) {
        return page + ":" + id;
    }

    /** 保留脚本提供的显示标题。 */
    private static String normalizeTitle(String title, String fallback) {
        return title == null || title.isBlank() ? fallback : title.trim();
    }

    /** 分类、筛选和排序的不可变定义供界面直接读取。 */
    public record FilterCategoryDefinition(String page, String id, String title, FilterMatchMode mode) { }
    public record FilterDefinition(String page, String category, String id, String title, CatalogApi.EntryFilter predicate) { }
    public record SortDefinition(String page, String id, String title, CatalogApi.EntrySortValue valueGetter) { }
}
