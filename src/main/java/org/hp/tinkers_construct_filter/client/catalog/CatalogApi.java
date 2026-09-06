package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogApi {
    private CatalogApi() {
    }

    public interface EntryView {
        String getId();

        String getName();

        int getMaterialLevel();

        List<String> getTraitNames();

        /** 材料取各部件与默认定义中同词条的最高等级；部件返回自身定义等级，不代表成品工具等级。 */
        default Map<String, Integer> getTraitLevels() { return Map.of(); }

        /** 返回稳定词条标识，不受客户端语言影响。 */
        default Set<String> getTraitIds() { return getTraitLevels().keySet(); }

        List<String> getTraitDescriptions();

        Map<String, Double> getAttributeValues();

        Map<String, String> getAttributeTexts();
    }

    public interface MaterialView extends EntryView {
        /** 返回旗下部件制作方式的并集，不代表所有部件都支持这些方式。 */
        default Set<String> getProductionMethods() {
            return Set.of("unknown");
        }

        int getDefaultSortOrder();

        int getAvailablePartCount();

        ItemStack getMaterialDisplayItem();
    }

    public interface PartView extends EntryView {
        /** 返回实际配方识别出的制作方式，未识别时返回 unknown。 */
        default Set<String> getProductionMethods() {
            return Set.of("unknown");
        }

        String getMaterialId();

        String getMaterialName();

        String getPartId();

        String getPartType();

        String getPartTypeName();

        /** 返回可使用此部件组装的工具物品 ID 与本地化名称。 */
        default Map<String, String> getToolCategories() {
            return Map.of();
        }

        ItemStack getMaterializedItem();
    }

    public interface ModifierView extends EntryView {
        /** 返回脚本归类覆盖后的效果分类标识，不修改词条实际效果。 */
        default List<String> getEffectCategories() {
            return CatalogExtensions.traitCategories(getId()).stream().map(TraitCategory::id).toList();
        }
        /** 返回构建快照时按词条 ID 反向关联的材料和部件。 */
        default List<CatalogEntry> getSourceMaterials() { return List.of(); }

        /** 部件保留材料化物品，供悬浮窗显示准确名称及物品信息。 */
        default List<CatalogEntry> getSourceParts() { return List.of(); }

        /** 返回工具定义本身提供当前词条的工具，不包含材料或额外强化来源。 */
        default List<ItemStack> getSourceTools() { return List.of(); }

        /** 区分可通过配方添加的强化与仅作为特性存在的词条。 */
        boolean hasModifierRecipe();

        Map<String, String> getSlotCategories();

        Map<String, String> getToolCategories();

        List<String> getModifierDescriptions();

        List<ItemStack> getRecipeTools();

        List<ModifierRecipeView> getRecipeVariants();
    }

    public record ModifierRecipeView(List<List<ItemStack>> inputSlots) {
        public ModifierRecipeView {
            inputSlots = inputSlots.stream()
                .map(slot -> slot.stream().map(ItemStack::copy).toList())
                .toList();
        }

        @Override
        public List<List<ItemStack>> inputSlots() {
            return inputSlots.stream()
                .map(slot -> slot.stream().map(ItemStack::copy).toList())
                .toList();
        }
    }

    @FunctionalInterface
    public interface MaterialFilter {
        boolean test(MaterialView view);
    }

    @FunctionalInterface
    public interface PartFilter {
        boolean test(PartView view);
    }

    @FunctionalInterface
    public interface MaterialSortValue {
        Object getValue(MaterialView view);
    }

    @FunctionalInterface
    public interface PartSortValue {
        Object getValue(PartView view);
    }

    /** 强化和词条共用只读数据形状，通过不同注册入口指定目标页面。 */
    @FunctionalInterface
    public interface ModifierFilter {
        boolean test(ModifierView view);
    }

    /** 每次刷新每条目只求值一次，不向脚本暴露排序比较器。 */
    @FunctionalInterface
    public interface ModifierSortValue {
        Object getValue(ModifierView view);
    }

    /** 统一扩展存储的内部回调，由各页面的类型化入口适配。 */
    @FunctionalInterface
    public interface EntryFilter {
        boolean test(EntryView view);
    }

    /** 统一扩展存储的排序值回调。 */
    @FunctionalInterface
    public interface EntrySortValue {
        Object getValue(EntryView view);
    }
}
