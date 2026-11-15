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

        default Map<String, Integer> getTraitLevels() {
            return Map.of();
        }

        default Set<String> getTraitIds() {
            return getTraitLevels().keySet();
        }

        List<String> getTraitDescriptions();

        Map<String, Double> getAttributeValues();

        Map<String, String> getAttributeTexts();
    }

    public interface MaterialView extends EntryView {
        default Set<String> getProductionMethods() {
            return Set.of("unknown");
        }

        int getDefaultSortOrder();

        int getAvailablePartCount();

        ItemStack getMaterialDisplayItem();

        default List<ItemStack> getMaterialItems() {
            return List.of();
        }

        default List<String> getCastingFluids() {
            return List.of();
        }

        default String getGeneralDescription() {
            return "";
        }

        default List<PartView> getExtraStatCards() {
            return List.of();
        }
    }

    public interface PartView extends EntryView {
        default Set<String> getProductionMethods() {
            return Set.of("unknown");
        }

        String getMaterialId();

        String getMaterialName();

        String getPartId();

        String getPartType();

        String getPartTypeName();

        default String getMaterialDescription() {
            return "";
        }

        default Map<String, String> getToolCategories() {
            return Map.of();
        }

        ItemStack getMaterializedItem();
    }

    public interface ModifierView extends EntryView {
        default List<String> getEffectCategories() {
            return CatalogExtensions.traitCategories(getId()).stream().map(TraitCategory::id).toList();
        }

        default List<CatalogEntry> getSourceMaterials() {
            return List.of();
        }

        default List<CatalogEntry> getSourceParts() {
            return List.of();
        }

        default List<ItemStack> getSourceTools() {
            return List.of();
        }

        boolean hasModifierRecipe();

        Map<String, String> getSlotCategories();

        Map<String, String> getToolCategories();

        List<String> getModifierDescriptions();

        List<ItemStack> getRecipeTools();

        List<ModifierRecipeView> getRecipeVariants();

        default String getSlotRequirementSummary() {
            return "";
        }

        default String getLevelSummary() {
            return "";
        }

        default int getMaxLevel() {
            return 0;
        }
    }

    public record ModifierRecipeView(List<List<ItemStack>> inputSlots, String slotSummary, String levelSummary) {
        public ModifierRecipeView(List<List<ItemStack>> inputSlots) {
            this(inputSlots, "", "");
        }

        public ModifierRecipeView {
            inputSlots = inputSlots.stream()
                .map(slot -> slot.stream().map(ItemStack::copy).toList())
                .toList();
            slotSummary = slotSummary == null ? "" : slotSummary;
            levelSummary = levelSummary == null ? "" : levelSummary;
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

    @FunctionalInterface
    public interface ModifierFilter {
        boolean test(ModifierView view);
    }

    @FunctionalInterface
    public interface ModifierSortValue {
        Object getValue(ModifierView view);
    }

    @FunctionalInterface
    public interface EntryFilter {
        boolean test(EntryView view);
    }

    @FunctionalInterface
    public interface EntrySortValue {
        Object getValue(EntryView view);
    }
}
