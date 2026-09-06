package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public final class CatalogApi {
    private CatalogApi() {
    }

    public interface EntryView {
        String getId();

        String getName();

        int getMaterialLevel();

        List<String> getTraitNames();

        List<String> getTraitDescriptions();

        Map<String, Double> getAttributeValues();

        Map<String, String> getAttributeTexts();
    }

    public interface MaterialView extends EntryView {
        int getDefaultSortOrder();

        int getAvailablePartCount();

        ItemStack getMaterialDisplayItem();
    }

    public interface PartView extends EntryView {
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
        /** 返回构建快照时按词条 ID 反向关联的材料和部件。 */
        default List<CatalogEntry> getSourceMaterials() { return List.of(); }

        /** 部件保留材料化物品，供悬浮窗显示准确名称及物品信息。 */
        default List<CatalogEntry> getSourceParts() { return List.of(); }

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
}
