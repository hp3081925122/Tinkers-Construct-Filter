package org.hp.tinkers_construct_filter.client.catalog;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** 不启动游戏的扩展回归测试，覆盖注册入口、页面隔离、分类规则与异常恢复。 */
public final class CatalogExtensionsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        // 分类内任意满足、全部满足和未选择的边界。
        List<String> options = List.of("one", "two");
        check(FilterMatchMode.ANY.matches(options, Set.of("one", "two"), value -> value, "one"::equals), "any union");
        check(!FilterMatchMode.ALL.matches(options, Set.of("one", "two"), value -> value, "one"::equals), "all intersection");
        check(FilterMatchMode.ANY.matches(options, Set.of(), value -> value, value -> false), "empty any");
        check(FilterMatchMode.ALL.matches(options, Set.of(), value -> value, value -> false), "empty all");
        check(!FilterMatchMode.ANY.matches(options, Set.of("two"), value -> value, "one"::equals), "only selected options");
        check(FilterMatchMode.parse(" ANY ") == FilterMatchMode.ANY, "mode normalization");
        expectInvalid(() -> FilterMatchMode.parse("either"), "invalid mode");

        // 通过真实事件类调用所有页面入口；仅避免测试编译期依赖可选 KubeJS 类型。
        Object event = Class.forName("org.hp.tinkers_construct_filter.compat.kubejs.TinkersFilterRegistrationEvent").getConstructor().newInstance();
        CatalogApi.MaterialView material = view(CatalogApi.MaterialView.class);
        CatalogApi.PartView part = view(CatalogApi.PartView.class);
        CatalogApi.ModifierView modifier = view(CatalogApi.ModifierView.class);
        invoke(event, "materialFilter", new Class<?>[] {String.class, String.class, CatalogApi.MaterialFilter.class},
            "legacy", "旧材料筛选", (CatalogApi.MaterialFilter) entry -> entry.getMaterialLevel() == 2);
        invoke(event, "partFilter", new Class<?>[] {String.class, String.class, CatalogApi.PartFilter.class},
            "legacy", "旧部件筛选", (CatalogApi.PartFilter) entry -> entry.getPartId().equals("test:part"));
        invoke(event, "materialSort", new Class<?>[] {String.class, String.class, CatalogApi.MaterialSortValue.class},
            "legacy", "旧材料排序", (CatalogApi.MaterialSortValue) entry -> entry.getMaterialLevel());
        invoke(event, "partSort", new Class<?>[] {String.class, String.class, CatalogApi.PartSortValue.class},
            "legacy", "旧部件排序", (CatalogApi.PartSortValue) CatalogApi.PartView::getPartId);
        check(CatalogExtensions.test(CatalogExtensions.filters("materials").get(0), material), "legacy material predicate");
        check(CatalogExtensions.test(CatalogExtensions.filters("parts").get(0), part), "legacy part predicate");
        check(CatalogExtensions.value(CatalogExtensions.sorts("materials").get(0), material).equals(2.0), "legacy numeric sort");
        check(CatalogExtensions.value(CatalogExtensions.sorts("parts").get(0), part).equals("test:part"), "legacy text sort");
        check(CatalogExtensions.filterCategory("parts", "custom").mode() == FilterMatchMode.ALL, "legacy all mode");

        // 四页使用同名分类和标识时仍然独立，验证带分类重载。
        for (String page : List.of("materials", "parts", "modifiers", "traits")) {
            invoke(event, "filterCategory", new Class<?>[] {String.class, String.class, String.class, String.class},
                page, "purpose", "用途", "any");
        }
        invoke(event, "materialFilter", new Class<?>[] {String.class, String.class, String.class, CatalogApi.MaterialFilter.class},
            "purpose", "grouped", "材料用途", (CatalogApi.MaterialFilter) entry -> entry.getTraitIds().contains("test:trait"));
        invoke(event, "partFilter", new Class<?>[] {String.class, String.class, String.class, CatalogApi.PartFilter.class},
            "purpose", "grouped", "部件用途", (CatalogApi.PartFilter) entry -> entry.getTraitLevels().get("test:trait") == 3);
        for (String prefix : List.of("modifier", "trait")) {
            invoke(event, prefix + "Filter", new Class<?>[] {String.class, String.class, CatalogApi.ModifierFilter.class},
                "legacy", "默认筛选", (CatalogApi.ModifierFilter) CatalogApi.ModifierView::hasModifierRecipe);
            invoke(event, prefix + "Filter", new Class<?>[] {String.class, String.class, String.class, CatalogApi.ModifierFilter.class},
                "purpose", "grouped", "来源", (CatalogApi.ModifierFilter) entry -> entry.getSourceParts().isEmpty());
            invoke(event, prefix + "Sort", new Class<?>[] {String.class, String.class, CatalogApi.ModifierSortValue.class},
                "new", "名称", (CatalogApi.ModifierSortValue) CatalogApi.EntryView::getName);
        }
        for (String page : List.of("materials", "parts", "modifiers", "traits")) {
            check(CatalogExtensions.filters(page).size() == 2, "page filters " + page);
            check(CatalogExtensions.filterCategory(page, "purpose").mode() == FilterMatchMode.ANY, "page category " + page);
        }
        check(CatalogExtensions.test(CatalogExtensions.filters("materials").get(1), material), "material trait IDs");
        check(CatalogExtensions.test(CatalogExtensions.filters("parts").get(1), part), "part trait levels");
        check(CatalogExtensions.test(CatalogExtensions.filters("modifiers").get(0), modifier), "modifier predicate");
        check(CatalogExtensions.test(CatalogExtensions.filters("traits").get(1), modifier), "trait predicate");
        check(CatalogExtensions.value(CatalogExtensions.sorts("traits").get(0), modifier).equals("测试条目"), "trait sort");

        // 覆盖任意命名空间的词条归类，重复分类去重，其他只作回退。
        invoke(event, "traitCategories", new Class<?>[] {String.class, List.class}, "test:trait", List.of("attack", "defense", "attack", "other"));
        check(CatalogExtensions.traitCategories("test:trait").equals(List.of(TraitCategory.ATTACK, TraitCategory.DEFENSE)), "trait override");
        invoke(event, "traitCategories", new Class<?>[] {String.class, List.class}, "test:trait", List.of());
        check(CatalogExtensions.traitCategories("test:trait").equals(List.of(TraitCategory.OTHER)), "empty override");
        check(CatalogExtensions.traitCategories("tconstruct:sharpness").equals(TraitCategory.classify("tconstruct:sharpness")), "built-in fallback");
        expectInvalid(() -> CatalogExtensions.registerTraitCategories("test:trait", List.of("typo")), "invalid trait category");
        expectInvalid(() -> CatalogExtensions.registerFilterCategory("material", "purpose", "用途", "any"), "invalid page");
        expectInvalid(() -> CatalogExtensions.registerFilterCategory("parts", "", "用途", "any"), "empty category ID");

        // 同页面同标识覆盖，未定义分类跳过，回调异常隔离且只执行一次。
        CatalogExtensions.registerFilter("traits", "purpose", "grouped", "覆盖", entry -> false);
        check(CatalogExtensions.filters("traits").size() == 2, "duplicate replacement");
        check(!CatalogExtensions.test(CatalogExtensions.filters("traits").get(1), modifier), "replacement predicate");
        CatalogExtensions.registerFilter("traits", "missing", "unresolved", "未定义", entry -> true);
        check(CatalogExtensions.filters("traits").size() == 2, "unresolved category skipped");
        AtomicInteger calls = new AtomicInteger();
        CatalogExtensions.registerFilter("traits", "custom", "broken", "异常", entry -> {
            calls.incrementAndGet();
            throw new IllegalStateException("Expected regression test failure");
        });
        CatalogExtensions.FilterDefinition broken = CatalogExtensions.filters("traits").get(2);
        check(CatalogExtensions.test(broken, modifier), "failed predicate passes through");
        check(CatalogExtensions.test(broken, modifier) && calls.get() == 1, "failed predicate not repeated");
        check(CatalogExtensions.filters("traits").size() == 2, "failed predicate disabled");
        CatalogExtensions.registerSort("traits", "invalid", "异常数值", entry -> Double.NaN);
        CatalogExtensions.SortDefinition invalid = CatalogExtensions.sorts("traits").get(1);
        check(CatalogExtensions.value(invalid, modifier) == null, "invalid sort fallback");
        check(CatalogExtensions.sorts("traits").size() == 1, "invalid sort disabled");

        // 重建定义清除所有旧状态，再次执行注册钩子；模拟移除脚本后的恢复。
        CatalogExtensions.registerTraitCategories("test:trait", List.of("attack"));
        CatalogExtensions.setKubeJsRefreshHook(() -> CatalogExtensions.registerFilter("traits", "custom", "broken", "已恢复", entry -> false));
        CatalogExtensions.refreshDefinitions();
        check(CatalogExtensions.filters("materials").isEmpty(), "stale filters cleared");
        check(CatalogExtensions.sorts("traits").isEmpty(), "stale sorts cleared");
        check(CatalogExtensions.filters("traits").size() == 1, "refresh hook called");
        check(!CatalogExtensions.test(CatalogExtensions.filters("traits").get(0), modifier), "disabled state cleared");
        check(CatalogExtensions.filterCategory("traits", "purpose").mode() == FilterMatchMode.ALL, "stale category cleared");
        check(CatalogExtensions.traitCategories("test:trait").equals(List.of(TraitCategory.OTHER)), "stale trait override cleared");
        CatalogExtensions.setKubeJsRefreshHook(null);
        CatalogExtensions.refreshDefinitions();
        check(CatalogExtensions.filters("traits").isEmpty(), "no KubeJS fallback");

        // 使用当前项目真实 Rhino 执行文档示例，核对重载、数组转列表及脚本回调转换。
        Class<?> contextType = Class.forName("dev.latvian.mods.rhino.Context");
        Class<?> scriptableType = Class.forName("dev.latvian.mods.rhino.Scriptable");
        Class<?> objectType = Class.forName("dev.latvian.mods.rhino.ScriptableObject");
        Object context = contextType.getMethod("enter").invoke(null);
        Object scope = contextType.getMethod("initStandardObjects").invoke(context);
        Object wrappedEvent = contextType.getMethod("javaToJS", contextType, Object.class, scriptableType)
            .invoke(null, context, event, scope);
        objectType.getMethod("putProperty", scriptableType, String.class, Object.class, contextType)
            .invoke(null, scope, "registrationEvent", wrappedEvent, context);
        String script = "var TinkersFilterEvents = { register: callback => callback(registrationEvent) };\n"
            + java.nio.file.Files.readString(java.nio.file.Path.of(args.length == 0 ? "docs/examples/catalog_extensions.js" : args[0]), java.nio.charset.StandardCharsets.UTF_8);
        contextType.getMethod("evaluateString", scriptableType, String.class, String.class, int.class, Object.class)
            .invoke(context, scope, script, "catalog_extensions.js", 1, null);
        for (String page : List.of("materials", "parts", "modifiers", "traits")) {
            CatalogApi.EntryView entry = page.equals("materials") ? material : page.equals("parts") ? part : modifier;
            List<CatalogExtensions.FilterDefinition> definitions = CatalogExtensions.filters(page);
            check(definitions.size() == (page.equals("materials") ? 3 : 2), "Rhino sample registration " + page);
            definitions.forEach(definition -> CatalogExtensions.test(definition, entry));
            check(CatalogExtensions.filters(page).size() == definitions.size(), "Rhino sample filters " + page);
            CatalogExtensions.sorts(page).forEach(definition -> CatalogExtensions.value(definition, entry));
            check(CatalogExtensions.sorts(page).size() == 1, "Rhino sample sorts " + page);
        }
        check(CatalogExtensions.traitCategories("tconstruct:sharpness").equals(List.of(TraitCategory.ATTACK)), "Rhino array conversion");
        System.out.println("Catalog extension regression checks passed: " + checks);
    }

    /** 使用轻量只读视图替身，验证回调不要求启动游戏或读取注册表。 */
    private static <T> T view(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            // 替身覆盖词条索引默认实现，其余默认方法仍由真实接口执行。
            if (method.getName().equals("getTraitLevels")) {
                return Map.of("test:trait", 3);
            }
            if (method.isDefault()) {
                return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
            }
            return switch (method.getName()) {
                case "getId" -> "test:trait";
                case "getName" -> "测试条目";
                case "getMaterialLevel" -> 2;
                case "getAvailablePartCount" -> 4;
                case "getToolCategories" -> Map.of("test:tool", "测试工具");
                case "getPartId" -> "test:part";
                case "hasModifierRecipe" -> true;
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }));
    }

    /** 反射只用于测试可选依赖入口，生产代码不使用反射兼容。 */
    private static void invoke(Object event, String name, Class<?>[] types, Object... args) throws Exception {
        event.getClass().getMethod(name, types).invoke(event, args);
    }

    /** 验证参数校验明确失败，不吞掉拼写错误。 */
    private static void expectInvalid(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    /** 输出明确失败位置，便于两版共用同一组回归用例。 */
    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
