package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;
import slimeknights.tconstruct.tools.stats.PlatingMaterialStats;
import slimeknights.tconstruct.library.utils.HarvestTiers;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CatalogAttributes {
    private CatalogAttributes() {
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
            values.put("plating_armor", (double) plating.armor());
            values.put("plating_toughness", (double) plating.toughness());
            values.put("plating_knockback_resistance", (double) plating.knockbackResistance());
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
            texts.put("plating_armor", format("plating_armor", plating.armor()));
            texts.put("plating_toughness", format("plating_toughness", plating.toughness()));
            texts.put("plating_knockback_resistance", format("plating_knockback_resistance", plating.knockbackResistance()));
        }
        return texts;
    }

    public static Component title(String id) {
        return Component.translatable("sort.tinkers_construct_filter." + id);
    }

    public static String format(String id, double value) {
        if (id.endsWith("_durability") || id.endsWith("_level")) {
            return Integer.toString((int) Math.round(value));
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
