package org.hp.tinkers_construct_filter.client.catalog;

import java.util.List;

/** 不依赖游戏启动的分类回归测试，可直接通过 Java 主方法执行。 */
public final class TraitCategoryTest {
    public static void main(String[] args) {
        // 校验攻击、防御、资源及未分类回退。
        check("tconstruct:sharpness", List.of(TraitCategory.ATTACK));
        check("tconstruct:revitalizing", List.of(TraitCategory.DEFENSE));
        check("tconstruct:luck", List.of(TraitCategory.RESOURCE));
        check("tconstruct:lustrous", List.of(TraitCategory.RESOURCE));
        check("tconstruct:stonebound", List.of(TraitCategory.OTHER));
        check("tconstruct:worldbound", List.of(TraitCategory.OTHER));
        // 同名附属词条不能冒用匠魂定义，多效果词条不生成混合分类。
        check("addon:sharpness", List.of(TraitCategory.OTHER));
        check("tconstruct:unknown", List.of(TraitCategory.OTHER));
        check("tconstruct:dragonborn", List.of(TraitCategory.ATTACK, TraitCategory.DEFENSE));
        System.out.println("Trait category regression checks passed: 9");
    }

    /** 验证完整分类结果，避免遗漏其他分类的排他性。 */
    private static void check(String id, List<TraitCategory> expected) {
        if (!TraitCategory.classify(id).equals(expected)) {
            throw new AssertionError(id + ": " + TraitCategory.classify(id) + " != " + expected);
        }
    }
}
