package org.hp.tinkers_construct_filter.client.catalog;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** 分类内多选规则；分类之间的交集由调用方统一处理。 */
public enum FilterMatchMode {
    ANY, ALL;

    /** 只接受明确规则，拼写错误不能悄悄改变筛选含义。 */
    public static FilterMatchMode parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Filter match mode must be any or all");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /** 未勾选的分类不限制结果；只计算已勾选的选项。 */
    public <T> boolean matches(Collection<T> options, Set<String> selected, Function<T, String> id, Predicate<T> predicate) {
        boolean hasSelection = false;
        for (T option : options) {
            if (!selected.contains(id.apply(option))) {
                continue;
            }
            hasSelection = true;
            boolean match = predicate.test(option);
            if (this == ANY && match) {
                return true;
            }
            if (this == ALL && !match) {
                return false;
            }
        }
        return !hasSelection || this == ALL;
    }
}
