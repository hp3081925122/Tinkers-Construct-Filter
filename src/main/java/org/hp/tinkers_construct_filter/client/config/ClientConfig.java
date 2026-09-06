package org.hp.tinkers_construct_filter.client.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SEARCH_HISTORY_LIMIT;
    public static final ForgeConfigSpec.IntValue SEARCH_HISTORY_VISIBLE_ROWS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SEARCH_HISTORY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("search");
        SEARCH_HISTORY_LIMIT = builder.comment("保存的搜索历史数量，0 表示不保存。")
            .defineInRange("historyLimit", 5, 0, 50);
        SEARCH_HISTORY_VISIBLE_ROWS = builder.comment("搜索历史弹层同时显示的行数。")
            .defineInRange("historyVisibleRows", 5, 1, 50);
        SEARCH_HISTORY = builder.comment("最近使用的搜索词。")
            .defineListAllowEmpty(List.of("history"), List::of, value -> value instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static List<String> getSearchHistory() {
        int limit = SEARCH_HISTORY_LIMIT.get();
        if (limit <= 0) {
            return List.of();
        }

        LinkedHashSet<String> history = new LinkedHashSet<>();
        for (String value : SEARCH_HISTORY.get()) {
            if (value != null && !value.isBlank()) {
                history.add(value.trim());
                if (history.size() >= limit) {
                    break;
                }
            }
        }
        return List.copyOf(history);
    }

    public static int getSearchHistoryVisibleRows() {
        return SEARCH_HISTORY_VISIBLE_ROWS.get();
    }

    public static void rememberSearch(String value) {
        String query = value == null ? "" : value.trim();
        int limit = SEARCH_HISTORY_LIMIT.get();
        if (query.isEmpty() || limit <= 0) {
            return;
        }

        List<String> history = new ArrayList<>(getSearchHistory());
        history.removeIf(query::equals);
        history.add(0, query);
        if (history.size() > limit) {
            history.subList(limit, history.size()).clear();
        }
        SEARCH_HISTORY.set(List.copyOf(history));
        SEARCH_HISTORY.save();
        TinkersConstructFilter.LOGGER.debug("Search history saved: {}", query);
    }

    /** 删除搜索历史中的指定记录，并立即保存客户端配置。 */
    public static void removeSearch(String value) {
        String query = value == null ? "" : value.trim();
        if (query.isEmpty()) {
            return;
        }

        List<String> history = new ArrayList<>(getSearchHistory());
        if (history.remove(query)) {
            SEARCH_HISTORY.set(List.copyOf(history));
            SEARCH_HISTORY.save();
        }
    }
}
