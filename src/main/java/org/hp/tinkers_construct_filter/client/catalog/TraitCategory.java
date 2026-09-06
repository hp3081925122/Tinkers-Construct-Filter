package org.hp.tinkers_construct_filter.client.catalog;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** 按 1.20.1 匠魂效果描述归类，使用完整词条 ID，避免语言切换和同名附属词条误判。 */
public enum TraitCategory {
    ATTACK("attack", Set.of("emerald", "diamond", "netherite", "fiery", "piercing", "pierce", "smite",
        "bane_of_sssss", "antiaquatic", "killager", "cooling", "sweeping_edge", "sharpness", "swiftstrike",
        "power", "quick_charge", "impaling", "multishot", "dragonborn", "thorns", "armor_power", "strength",
        "ambidextrous", "reflecting", "bonking", "jagged", "scorching", "raging", "antitoxin", "lacerating",
        "crystalstrike", "lightweight", "insatiable", "kinetic", "conducting", "sharpweight", "holy",
        "ductile", "invariant", "decay", "withered", "wildfire", "plague", "breathtaking", "firebreath", "revenge",
        "channeling", "charge_attack", "conductive", "dragonshot", "drill_attack", "explosive", "fins_ammo",
        "fishing", "flamestance", "keen", "rebound", "shock", "solid", "spiny", "throwing", "valiant", "venom")),
    DEFENSE("defense", Set.of("necrotic", "knockback_resistance", "revitalizing", "protection", "melee_protection",
        "fire_protection", "scorch_protection", "blast_protection", "magic_protection", "projectile_protection",
        "turtle_shell", "dragonborn", "shulking", "boundless", "respiration", "feather_falling", "long_fall",
        "bouncy", "reflecting", "blocking", "parrying", "depth_protection", "recurrent_protection", "flame_barrier",
        "consecrated", "ductile", "enderdodging", "strong_bones", "mithridatism", "gold_guard",
        "blockade", "entangled", "flamestance", "overshield", "solid", "stalwart", "tempered_protection", "vital_protection")),
    RESOURCE("resource", Set.of("experienced", "magnetic", "severing", "silky", "autosmelt", "luck", "fortune",
        "looting", "shears", "silky_shears", "silky_aoe_shears", "harvest", "melting", "bucketing", "lustrous",
        "olympic", "chrysophilite", "snowdrift", "brushing", "collecting", "economical", "lure", "lure_rod", "smelting")),
    OTHER("other", Set.of());

    private final String id;
    private final Set<String> modifiers;

    /** 保存类别及已核对的原版匠魂词条路径。 */
    TraitCategory(String id, Set<String> modifiers) {
        this.id = id;
        this.modifiers = modifiers;
    }

    /** 提供稳定的筛选标识与语言键后缀。 */
    public String id() {
        return id;
    }

    /** 多效果词条可命中多类；没有明确归类依据的词条只进入其他。 */
    public static List<TraitCategory> classify(String modifierId) {
        if (!modifierId.startsWith("tconstruct:")) {
            return List.of(OTHER);
        }
        String path = modifierId.substring("tconstruct:".length());
        List<TraitCategory> categories = Arrays.stream(values())
            .filter(category -> category != OTHER && category.modifiers.contains(path)).toList();
        return categories.isEmpty() ? List.of(OTHER) : categories;
    }
}
