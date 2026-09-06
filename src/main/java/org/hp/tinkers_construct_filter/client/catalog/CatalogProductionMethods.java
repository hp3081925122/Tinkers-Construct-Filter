package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.CompositeCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.PartRecipe;
import slimeknights.tconstruct.library.tools.part.IToolPart;

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
        // 复用匠魂展开后的输出，保留材料限制、部件限制和复合浇铸规则。
        for (Recipe<?> recipe : recipes) {
            try {
                if (recipe instanceof PartRecipe partRecipe) {
                    for (IDisplayPartBuilderRecipe display : partRecipe.getRecipes(minecraft.level.registryAccess())) {
                        addPartRecipe(result, display);
                    }
                } else if (recipe instanceof IDisplayPartBuilderRecipe display) {
                    addPartRecipe(result, display);
                } else if (recipe instanceof MaterialCastingRecipe casting) {
                    String method = casting instanceof CompositeCastingRecipe ? "composite_casting" : "casting";
                    for (IDisplayableCastingRecipe display : casting.getRecipes(minecraft.level.registryAccess())) {
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
    private static void addPartRecipe(Map<String, Set<String>> result, IDisplayPartBuilderRecipe recipe) {
        // 展示配方可能合并多个材料变体，直接使用匠魂展开的输入，不能拿变体反向匹配基础材料。
        if (recipe.getPatternItems().stream().noneMatch(stack -> !stack.isEmpty())
            || recipe.getMaterialItems().stream().noneMatch(stack -> !stack.isEmpty())) {
            TinkersConstructFilter.LOGGER.debug("Skipping part builder production recipe with no usable pattern or material: recipe={}, material={}",
                recipe.getId(), recipe.getMaterial().getVariant());
            return;
        }
        // 合并展示配方的每个输出都参与索引，诊断只在目录重建时记录。
        List<ItemStack> outputs = recipe.getResultItems();
        if (outputs.size() > 1) {
            TinkersConstructFilter.LOGGER.debug("Indexing grouped part builder production recipe: recipe={}, material={}, inputs={}, outputs={}",
                recipe.getId(), recipe.getMaterial().getVariant(), recipe.getMaterialItems().size(), outputs.size());
        }
        for (ItemStack output : outputs) {
            addOutput(result, output, "part_builder");
        }
    }

    /** 没有流体或必要铸模的配方不计入可识别的浇铸途径。 */
    private static void addCastingRecipe(Map<String, Set<String>> result, IDisplayableCastingRecipe recipe, String method) {
        if (recipe.getFluids().stream().noneMatch(fluid -> !fluid.isEmpty())
            || (recipe.hasCast() && recipe.getCastItems().stream().noneMatch(stack -> !stack.isEmpty()))) {
            return;
        }
        for (ItemStack output : recipe.getOutputs()) {
            addOutput(result, output, method);
        }
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
