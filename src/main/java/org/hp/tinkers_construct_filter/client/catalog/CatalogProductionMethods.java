package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.CompositeCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.PartRecipe;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 根据当前配方输出建立材料化部件的制作方式索引。 */
final class CatalogProductionMethods {
    private CatalogProductionMethods() {
    }

    /** 仅在目录重建时扫描配方，筛选和绘制时直接读取缓存。 */
    static Map<String, Set<String>> collect() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Map.of();
        }
        Collection<Recipe<?>> recipes = minecraft.level.getRecipeManager().getRecipes();
        Map<String, Set<String>> result = new HashMap<>();
        Map<MaterialId, List<MaterialVariantId>> materialInputs = new HashMap<>();
        // 材料必须有实际输入物品，空标签或被删除的材料配方不能算可加工。
        for (Recipe<?> recipe : recipes) {
            if (recipe instanceof MaterialRecipe material && material.getDisplayItems().stream().anyMatch(stack -> !stack.isEmpty())) {
                materialInputs.computeIfAbsent(material.getMaterial().getId(), ignored -> new ArrayList<>())
                    .add(material.getMaterial().getVariant());
            }
        }
        // 复用匠魂展开后的输出，保留材料限制、部件限制和复合浇铸规则。
        for (Recipe<?> recipe : recipes) {
            try {
                if (recipe instanceof PartRecipe partRecipe) {
                    for (IDisplayPartBuilderRecipe display : partRecipe.getRecipes()) {
                        addPartRecipe(result, materialInputs, display);
                    }
                } else if (recipe instanceof IDisplayPartBuilderRecipe display) {
                    addPartRecipe(result, materialInputs, display);
                } else if (recipe instanceof MaterialCastingRecipe casting) {
                    String method = casting instanceof CompositeCastingRecipe ? "composite_casting" : "casting";
                    for (IDisplayableCastingRecipe display : casting.getRecipes()) {
                        addCastingRecipe(result, display, method);
                    }
                } else if (recipe instanceof IDisplayableCastingRecipe display) {
                    addCastingRecipe(result, display, "casting");
                }
            } catch (RuntimeException exception) {
                TinkersConstructFilter.LOGGER.debug("Skipping unavailable production recipe: {}", recipe.getId(), exception);
            }
        }
        // 固化索引，供不同材料条目与部件条目安全共享。
        result.replaceAll((key, methods) -> Set.copyOf(methods));
        TinkersConstructFilter.LOGGER.debug("Production method index built: parts={}", result.size());
        return Map.copyOf(result);
    }

    /** 检查图案及材料输入后，记录部件加工台的实际输出。 */
    private static void addPartRecipe(Map<String, Set<String>> result, Map<MaterialId, List<MaterialVariantId>> inputs,
                                      IDisplayPartBuilderRecipe recipe) {
        // 配方材料是匹配条件：基础材料接受其所有变体，具体变体仍只接受匠魂规则允许的输入。
        if (recipe.getPatternItems().stream().noneMatch(stack -> !stack.isEmpty())
            || inputs.getOrDefault(recipe.getMaterial().getId(), List.of()).stream()
                .noneMatch(material -> recipe.getMaterial().getVariant().matchesVariant(material))) {
            TinkersConstructFilter.LOGGER.debug("Skipping part builder production recipe with no usable pattern or material: recipe={}, material={}",
                recipe.getId(), recipe.getMaterial().getVariant());
            return;
        }
        // 保留实际配方输出，不根据材料名称或部件编号补造制作途径。
        addOutput(result, recipe.getResultItem(), "part_builder");
    }

    /** 没有流体或必要铸模的配方不计入可识别的浇铸途径。 */
    private static void addCastingRecipe(Map<String, Set<String>> result, IDisplayableCastingRecipe recipe, String method) {
        if (recipe.getFluids().stream().noneMatch(fluid -> !fluid.isEmpty())
            || (recipe.hasCast() && recipe.getCastItems().stream().noneMatch(stack -> !stack.isEmpty()))) {
            return;
        }
        addOutput(result, recipe.getOutput(), method);
    }

    /** 使用材料与部件物品的联合键，避免同类属性部件互相误匹配。 */
    private static void addOutput(Map<String, Set<String>> result, ItemStack output, String method) {
        if (output.isEmpty() || !(output.getItem() instanceof IToolPart part)) {
            return;
        }
        String key = part.getMaterial(output).getId() + "@" + ForgeRegistries.ITEMS.getKey(output.getItem());
        result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(method);
    }
}
