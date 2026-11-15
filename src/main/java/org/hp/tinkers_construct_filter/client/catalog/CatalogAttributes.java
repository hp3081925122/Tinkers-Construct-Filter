package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.utils.HarvestTiers;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;
import slimeknights.tconstruct.tools.stats.PlatingMaterialStats;
import slimeknights.tconstruct.tools.stats.SkullStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CatalogAttributes {
    private static final Set<String> PERCENT_ATTRIBUTES = Set.of(
        "handle_durability",
        "handle_mining_speed",
        "handle_melee_speed",
        "handle_attack_damage",
        "grip_durability"
    );

    private static final Set<String> SIGNED_BONUS_ATTRIBUTES = Set.of(
        "limb_draw_speed",
        "limb_velocity",
        "limb_accuracy",
        "grip_accuracy"
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)(%?)");
    private static final Map<String, String> DYNAMIC_TITLES = new ConcurrentHashMap<>();

    private CatalogAttributes() {
    }

    public static void clearDynamicTitles() {
        DYNAMIC_TITLES.clear();
    }

    public static Map<String, Double> collect(IMaterialStats stats) {
        Map<String, Double> values = new LinkedHashMap<>();
        if (stats instanceof HeadMaterialStats head) {
            values.put("head_durability", (double) head.durability());
            values.put("head_mining_speed", (double) head.miningSpeed());
            values.put("head_mining_level", (double) head.tier().getLevel());
            values.put("head_attack", (double) head.attack());
        } else if (stats instanceof HandleMaterialStats handle) {
            values.put("handle_durability", (double) handle.durability());
            values.put("handle_mining_speed", (double) handle.miningSpeed());
            values.put("handle_melee_speed", (double) handle.meleeSpeed());
            values.put("handle_attack_damage", (double) handle.attackDamage());
        } else if (stats instanceof LimbMaterialStats limb) {
            values.put("limb_durability", (double) limb.durability());
            values.put("limb_draw_speed", (double) limb.drawSpeed());
            values.put("limb_velocity", (double) limb.velocity());
            values.put("limb_accuracy", (double) limb.accuracy());
        } else if (stats instanceof GripMaterialStats grip) {
            values.put("grip_durability", (double) grip.durability());
            values.put("grip_accuracy", (double) grip.accuracy());
            values.put("grip_melee_damage", (double) grip.meleeDamage());
        } else if (stats instanceof PlatingMaterialStats plating) {
            values.put("plating_durability", (double) plating.durability());
            if (!PlatingMaterialStats.SHIELD.equals(plating.getType())) {
                values.put("plating_armor", (double) plating.armor());
            }
            values.put("plating_toughness", (double) plating.toughness());
            values.put("plating_knockback_resistance", (double) (plating.knockbackResistance() * 10.0F));
        } else if (stats instanceof SkullStats skull) {
            values.put("skull_durability", (double) skull.durability());
            values.put("skull_armor", (double) skull.armor());
        } else if (stats != null && !(stats instanceof StatlessMaterialStats)) {
            collectDynamicStats(stats, values, null);
        }
        return values;
    }

    public static Map<String, String> collectTexts(IMaterialStats stats) {
        Map<String, String> texts = new LinkedHashMap<>();
        if (stats instanceof HeadMaterialStats head) {
            texts.put("head_durability", format("head_durability", head.durability()));
            texts.put("head_mining_speed", format("head_mining_speed", head.miningSpeed()));
            texts.put("head_mining_level", HarvestTiers.getName(head.tier()).getString());
            texts.put("head_attack", format("head_attack", head.attack()));
        } else if (stats instanceof HandleMaterialStats handle) {
            texts.put("handle_durability", format("handle_durability", handle.durability()));
            texts.put("handle_mining_speed", format("handle_mining_speed", handle.miningSpeed()));
            texts.put("handle_melee_speed", format("handle_melee_speed", handle.meleeSpeed()));
            texts.put("handle_attack_damage", format("handle_attack_damage", handle.attackDamage()));
        } else if (stats instanceof LimbMaterialStats limb) {
            texts.put("limb_durability", format("limb_durability", limb.durability()));
            texts.put("limb_draw_speed", format("limb_draw_speed", limb.drawSpeed()));
            texts.put("limb_velocity", format("limb_velocity", limb.velocity()));
            texts.put("limb_accuracy", format("limb_accuracy", limb.accuracy()));
        } else if (stats instanceof GripMaterialStats grip) {
            texts.put("grip_durability", format("grip_durability", grip.durability()));
            texts.put("grip_accuracy", format("grip_accuracy", grip.accuracy()));
            texts.put("grip_melee_damage", format("grip_melee_damage", grip.meleeDamage()));
        } else if (stats instanceof PlatingMaterialStats plating) {
            texts.put("plating_durability", format("plating_durability", plating.durability()));
            if (!PlatingMaterialStats.SHIELD.equals(plating.getType())) {
                texts.put("plating_armor", format("plating_armor", plating.armor()));
            }
            texts.put("plating_toughness", format("plating_toughness", plating.toughness()));
            texts.put("plating_knockback_resistance", format("plating_knockback_resistance", plating.knockbackResistance() * 10.0F));
        } else if (stats instanceof SkullStats skull) {
            texts.put("skull_durability", format("skull_durability", skull.durability()));
            texts.put("skull_armor", format("skull_armor", skull.armor()));
        } else if (stats != null && !(stats instanceof StatlessMaterialStats)) {
            collectDynamicStats(stats, null, texts);
        }
        return texts;
    }

    public static void mergeIntoMaterial(Map<String, Double> targetValues, Map<String, String> targetTexts,
                                         Map<String, Double> partValues, Map<String, String> partTexts) {
        for (Map.Entry<String, Double> entry : partValues.entrySet()) {
            String key = entry.getKey();
            Double candidate = entry.getValue();
            if (candidate == null) {
                continue;
            }
            Double existing = targetValues.get(key);
            if (existing == null || candidate > existing) {
                targetValues.put(key, candidate);
                String text = partTexts.get(key);
                targetTexts.put(key, text != null ? text : format(key, candidate));
            }
        }
        for (Map.Entry<String, String> entry : partTexts.entrySet()) {
            targetTexts.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static void collectDynamicStats(IMaterialStats stats, Map<String, Double> values, Map<String, String> texts) {
        List<Component> lines;
        try {
            lines = stats.getLocalizedInfo();
        } catch (RuntimeException ignored) {
            return;
        }
        if (lines == null || lines.isEmpty()) {
            return;
        }
        String statPrefix = stats.getIdentifier().toString();
        String statTypeName = stats.getLocalizedName().getString();
        for (int index = 0; index < lines.size(); index++) {
            String raw = lines.get(index).getString().strip();
            if (raw.isEmpty()) {
                continue;
            }
            int sep = findSeparator(raw);
            String shortLabel;
            String valStr;
            if (sep > 0 && sep < raw.length() - 1) {
                shortLabel = raw.substring(0, sep).strip();
                valStr = raw.substring(sep + 1).strip();
            } else {
                shortLabel = statTypeName;
                valStr = raw;
            }
            String key = "addon:" + statPrefix + ":" + index;
            String fullTitle = statTypeName.isEmpty() || shortLabel.startsWith(statTypeName)
                ? shortLabel
                : statTypeName + " · " + shortLabel;
            DYNAMIC_TITLES.put(key, fullTitle);
            DYNAMIC_TITLES.put(key + "#short", shortLabel);
            if (texts != null) {
                texts.put(key, valStr);
            }
            if (values != null) {
                Matcher matcher = NUMBER_PATTERN.matcher(valStr.replace(",", ""));
                if (matcher.find()) {
                    try {
                        double parsed = Double.parseDouble(matcher.group(1));
                        if ("%".equals(matcher.group(2))) {
                            parsed /= 100.0D;
                        }
                        if (Double.isFinite(parsed)) {
                            values.put(key, parsed);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }

    private static int findSeparator(String text) {
        int colonZh = text.indexOf('：');
        int colonEn = text.indexOf(':');
        if (colonZh > 0 && colonEn > 0) {
            return Math.min(colonZh, colonEn);
        }
        return colonZh > 0 ? colonZh : colonEn;
    }

    public static Component title(String id) {
        if (id.startsWith("addon:")) {
            return Component.literal(DYNAMIC_TITLES.getOrDefault(id, id));
        }
        return Component.translatable("sort.tinkers_construct_filter." + id);
    }

    public static String shortTitle(String id) {
        if (id.startsWith("addon:")) {
            return DYNAMIC_TITLES.getOrDefault(id + "#short", DYNAMIC_TITLES.getOrDefault(id, id));
        }
        return Component.translatable("attribute.short.tinkers_construct_filter." + id).getString();
    }

    public static String format(String id, double value) {
        if (PERCENT_ATTRIBUTES.contains(id)) {
            double pct = value * 100.0D;
            if (Math.abs(pct) < 0.005D) {
                return "+0%";
            }
            if (Math.abs(pct - Math.rint(pct)) < 0.01D) {
                return String.format(Locale.ROOT, "%+d%%", (int) Math.rint(pct));
            }
            return String.format(Locale.ROOT, "%+.1f%%", pct);
        }
        if (SIGNED_BONUS_ATTRIBUTES.contains(id)) {
            if (Math.abs(value) < 0.0005D) {
                return "+0";
            }
            if (Math.abs(value - Math.rint(value)) < 0.0001D) {
                return String.format(Locale.ROOT, "%+d", (int) Math.rint(value));
            }
            return String.format(Locale.ROOT, "%+.2f", value);
        }
        if (id.endsWith("_durability") || id.endsWith("_level")) {
            return Integer.toString((int) Math.round(value));
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
