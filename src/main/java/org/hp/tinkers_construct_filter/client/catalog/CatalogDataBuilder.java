package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialFluidRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CatalogDataBuilder {
    private static final int MAX_MATERIAL_ITEMS = 36;

    private CatalogDataBuilder() {
    }

    public static CatalogSnapshot build() {
        if (!MaterialRegistry.isFullyLoaded() || !ModifierManager.INSTANCE.isDynamicModifiersLoaded()) {
            return CatalogSnapshot.loading();
        }

        CatalogAttributes.clearDynamicTitles();
        IMaterialRegistry registry = MaterialRegistry.getInstance();
        List<PartTemplate> partTemplates = collectPartTemplates(registry);
        Map<String, Set<String>> productionMethods = CatalogProductionMethods.collect();
        Map<MaterialId, List<MaterialRecipe>> materialRecipes = collectMaterialRecipes();
        List<CatalogEntry> materials = new ArrayList<>();
        List<CatalogEntry> parts = new ArrayList<>();
        List<CatalogEntry> traits = buildModifiers();
        List<CatalogEntry> modifiers = traits.stream()
            .filter(entry -> entry instanceof CatalogApi.ModifierView view && view.hasModifierRecipe())
            .toList();

        for (IMaterial material : registry.getVisibleMaterials()) {
            if (material.isHidden()) {
                continue;
            }

            MaterialId materialId = material.getIdentifier();
            MaterialVariant variant = MaterialVariant.of(material);
            Map<String, TraitData> materialTraitMap = new LinkedHashMap<>();
            addTraits(registry.getDefaultTraits(materialId), materialTraitMap);

            Map<String, Double> materialAttributes = new LinkedHashMap<>();
            Map<String, String> materialAttributeTexts = new LinkedHashMap<>();
            for (IMaterialStats stats : registry.getAllStats(materialId)) {
                CatalogAttributes.mergeIntoMaterial(
                    materialAttributes,
                    materialAttributeTexts,
                    CatalogAttributes.collect(stats),
                    CatalogAttributes.collectTexts(stats)
                );
            }

            List<ItemStack> representativeItems = collectMaterialItems(materialId, variant, materialRecipes);
            ItemStack fallbackDisplay = representativeItems.isEmpty() ? ItemStack.EMPTY : representativeItems.get(0).copy();
            List<String> castingFluids = collectMaterialFluids(variant);
            int availablePartCount = 0;
            Set<String> materialProductionMethods = new LinkedHashSet<>();
            Set<MaterialStatsId> coveredStatTypes = new HashSet<>();

            for (PartTemplate template : partTemplates) {
                if (!template.part().canUseMaterial(materialId)) {
                    continue;
                }

                IMaterialStats stats = findStats(registry, materialId, template.statType());
                if (stats == null) {
                    continue;
                }

                ItemStack displayStack = template.part().withMaterialForDisplay(variant.getVariant());
                if (displayStack.isEmpty()) {
                    continue;
                }

                coveredStatTypes.add(template.statType());
                availablePartCount++;
                if (fallbackDisplay.isEmpty()) {
                    fallbackDisplay = displayStack.copy();
                }

                List<TraitData> partTraits = collectTraits(registry, materialId, template.statType());
                for (TraitData trait : partTraits) {
                    materialTraitMap.merge(trait.id(), trait, (left, right) -> left.level() >= right.level() ? left : right);
                }

                Set<String> partProd = productionMethods.getOrDefault(materialId + "@" + template.itemId(), Set.of("unknown"));
                parts.add(new PartEntry(
                    materialId + "@" + template.itemId(),
                    displayStack,
                    materialId.toString(),
                    materialName(materialId),
                    material.getTier(),
                    partTraits,
                    CatalogAttributes.collect(stats),
                    CatalogAttributes.collectTexts(stats),
                    materialDescription(materialId, template.statType()),
                    template.itemId(),
                    template.typeId(),
                    template.typeName(),
                    template.toolCategories(),
                    partProd
                ));
                materialProductionMethods.addAll(partProd);
            }

            List<CatalogApi.PartView> extraStatCards = new ArrayList<>();
            for (IMaterialStats stats : registry.getAllStats(materialId)) {
                MaterialStatsId statId = stats.getIdentifier();
                if (coveredStatTypes.contains(statId)) {
                    continue;
                }
                List<TraitData> extraTraits = collectTraits(registry, materialId, statId);
                for (TraitData trait : extraTraits) {
                    materialTraitMap.merge(trait.id(), trait, (left, right) -> left.level() >= right.level() ? left : right);
                }
                Map<String, Double> extraValues = CatalogAttributes.collect(stats);
                Map<String, String> extraTexts = CatalogAttributes.collectTexts(stats);
                String extraDesc = materialDescription(materialId, statId);
                if (!(stats instanceof StatlessMaterialStats) || !extraTraits.isEmpty() || !extraDesc.isBlank()) {
                    extraStatCards.add(new PartEntry(
                        materialId + "@stat:" + statId,
                        fallbackDisplay,
                        materialId.toString(),
                        materialName(materialId),
                        material.getTier(),
                        extraTraits,
                        extraValues,
                        extraTexts,
                        extraDesc,
                        statId.toString(),
                        statId.toString(),
                        statTypeName(registry, statId),
                        Map.of(),
                        Set.of("unknown")
                    ));
                }
            }

            if (materialProductionMethods.isEmpty() && !castingFluids.isEmpty()) {
                materialProductionMethods.add("casting");
            }

            materials.add(new MaterialEntry(
                materialId.toString(),
                fallbackDisplay,
                materialName(materialId),
                material.getTier(),
                List.copyOf(materialTraitMap.values()),
                materialAttributes,
                materialAttributeTexts,
                material.getSortOrder(),
                availablePartCount,
                materialProductionMethods.isEmpty() ? Set.of("unknown") : materialProductionMethods,
                representativeItems,
                castingFluids,
                generalMaterialDescription(materialId),
                extraStatCards
            ));
        }

        TinkersConstructFilter.LOGGER.debug("Catalog trait metadata collected: materials={}, parts={}", materials.size(), parts.size());
        Map<String, List<CatalogEntry>> materialSources = new HashMap<>();
        Map<String, List<CatalogEntry>> partSources = new HashMap<>();
        for (CatalogEntry entry : materials) {
            for (TraitData trait : ((BaseEntry) entry).traits) {
                materialSources.computeIfAbsent(trait.id(), ignored -> new ArrayList<>()).add(entry);
            }
        }
        for (CatalogEntry entry : parts) {
            for (TraitData trait : ((BaseEntry) entry).traits) {
                partSources.computeIfAbsent(trait.id(), ignored -> new ArrayList<>()).add(entry);
            }
        }

        Map<String, List<ItemStack>> toolSources = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof IModifiable modifiable)) {
                continue;
            }
            try {
                ToolDefinition definition = modifiable.getToolDefinition();
                if (definition == null || !definition.isDataLoaded()) {
                    continue;
                }
                List<ModifierEntry> innateTraits = ToolTraitHook.getTraits(definition, MaterialNBT.EMPTY).getModifiers();
                if (innateTraits.isEmpty()) {
                    continue;
                }
                ItemStack displayStack = IModifiableDisplay.getDisplayStack(item).copy();
                for (ModifierEntry trait : innateTraits) {
                    toolSources.computeIfAbsent(trait.getId().toString(), ignored -> new ArrayList<>()).add(displayStack);
                }
            } catch (RuntimeException exception) {
                TinkersConstructFilter.LOGGER.debug("Skipping unavailable innate tool traits: {}", ForgeRegistries.ITEMS.getKey(item), exception);
            }
        }
        TinkersConstructFilter.LOGGER.debug("Innate tool trait source index built: traits={}", toolSources.size());
        for (CatalogEntry entry : traits) {
            ModifierCatalogEntry modifier = (ModifierCatalogEntry) entry;
            modifier.sourceMaterials = List.copyOf(materialSources.getOrDefault(entry.getId(), List.of()));
            modifier.sourceParts = List.copyOf(partSources.getOrDefault(entry.getId(), List.of()));
            modifier.sourceTools = toolSources.getOrDefault(entry.getId(), List.of()).stream().map(ItemStack::copy).toList();
        }
        TinkersConstructFilter.LOGGER.debug("Trait source index built: traits={}, materials={}, parts={}",
            traits.size(), materialSources.size(), partSources.size());
        return new CatalogSnapshot(true, List.copyOf(materials), List.copyOf(parts), modifiers, traits);
    }

    private static List<CatalogEntry> buildModifiers() {
        Map<String, MutableModifierMetadata> metadata = collectModifierMetadata();
        List<CatalogEntry> result = new ArrayList<>();
        ModifierManager.INSTANCE.getAllValues()
            .filter(modifier -> modifier != null && modifier.shouldDisplay(false))
            .sorted(Comparator.comparing(modifier -> modifier.getId().toString()))
            .forEach(modifier -> {
                try {
                    String id = modifier.getId().toString();
                    MutableModifierMetadata data = metadata.get(id);
                    boolean hasModifierRecipe = data != null;
                    if (!hasModifierRecipe) {
                        data = new MutableModifierMetadata();
                        data.slotCategories.add("non-craftable");
                    } else {
                        addTagMetadata(modifier, data);
                    }
                    if (data.slotCategories.isEmpty()) {
                        data.slotCategories.add("slotless");
                    }
                    String name = modifier.getDisplayName().getString();
                    List<String> descriptions = modifier.getDescriptionList().stream()
                        .map(Component::getString)
                        .filter(description -> !description.isBlank())
                        .toList();
                    String slotSummary = String.join(" / ", data.slotSummaries);
                    String levelSummary = formatAggregateLevel(data.minRecipeLevel, data.maxRecipeLevel, data.unboundedLevel, hasModifierRecipe);
                    int effectiveMaxLevel = data.unboundedLevel ? 99 : data.maxRecipeLevel;
                    result.add(new ModifierCatalogEntry(
                        id,
                        slimeknights.tconstruct.tools.item.ModifierCrystalItem.withModifier(modifier.getId()),
                        name,
                        data.slotCategories,
                        data.toolCategories,
                        descriptions,
                        data.recipeTools,
                        data.recipeVariants,
                        hasModifierRecipe,
                        slotSummary,
                        levelSummary,
                        effectiveMaxLevel
                    ));
                } catch (RuntimeException exception) {
                    TinkersConstructFilter.LOGGER.debug("Skipping unavailable modifier", exception);
                }
            });
        return List.copyOf(result);
    }

    private static Map<String, MutableModifierMetadata> collectModifierMetadata() {
        Map<String, MutableModifierMetadata> result = new HashMap<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return result;
        }
        for (Recipe<?> recipe : minecraft.level.getRecipeManager().getRecipes()) {
            if (!(recipe instanceof IDisplayModifierRecipe modifierRecipe)) {
                continue;
            }
            try {
                ModifierEntry displayResult = modifierRecipe.getDisplayResult();
                if (displayResult == null || displayResult.getModifier() == null) {
                    continue;
                }
                String id = displayResult.getId().toString();
                MutableModifierMetadata data = result.computeIfAbsent(id, ignored -> new MutableModifierMetadata());
                addSlotCategory(modifierRecipe.getSlotType(), data.slotCategories);

                String recipeSlotSummary = formatSlotCount(modifierRecipe.getSlots());
                if (!recipeSlotSummary.isEmpty()) {
                    data.slotSummaries.add(recipeSlotSummary);
                }

                IntRange levelRange = modifierRecipe.getLevel();
                String recipeLevelSummary = formatRecipeLevel(levelRange);
                if (levelRange != null) {
                    int min = Math.max(1, levelRange.min());
                    int max = levelRange.max();
                    data.minRecipeLevel = Math.min(data.minRecipeLevel, min);
                    if (max >= 1000) {
                        data.unboundedLevel = true;
                    } else if (max > 0) {
                        data.maxRecipeLevel = Math.max(data.maxRecipeLevel, max);
                    }
                }

                for (ItemStack tool : modifierRecipe.getToolWithoutModifier()) {
                    addToolType(tool, data.toolCategories);
                    if (!tool.isEmpty() && data.recipeToolKeys.add(stackIdentityKey(tool, false))) {
                        data.recipeTools.add(tool.copy());
                    }
                }
                List<List<ItemStack>> inputSlots = new ArrayList<>();
                int alternativeSlotCount = 0;
                boolean hasInput = false;
                for (int input = 0; input < modifierRecipe.getInputCount(); input++) {
                    List<ItemStack> slot = modifierRecipe.getDisplayItems(input).stream()
                        .filter(stack -> !stack.isEmpty())
                        .map(ItemStack::copy)
                        .toList();
                    if (!slot.isEmpty()) {
                        hasInput = true;
                    }
                    if (slot.size() > 1) {
                        alternativeSlotCount++;
                    }
                    inputSlots.add(slot);
                }
                if (hasInput) {
                    String recipeKey = buildRecipeVariantKey(inputSlots, recipeSlotSummary, recipeLevelSummary);
                    if (data.recipeKeys.add(recipeKey)) {
                        data.recipeVariants.add(new CatalogApi.ModifierRecipeView(inputSlots, recipeSlotSummary, recipeLevelSummary));
                        if (alternativeSlotCount > 0) {
                            TinkersConstructFilter.LOGGER.debug("Collected modifier recipe {} with {} input slots and {} alternative slots", id, inputSlots.size(), alternativeSlotCount);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                TinkersConstructFilter.LOGGER.debug("Skipping unavailable modifier recipe", exception);
            }
        }
        return result;
    }

    private static String stackIdentityKey(ItemStack stack, boolean includeCountAndTag) {
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String base = itemKey == null ? stack.getItem().getDescriptionId() : itemKey.toString();
        if (!includeCountAndTag) {
            return base;
        }
        String tagStr = stack.getTag() == null ? "" : stack.getTag().toString();
        return base + "#" + stack.getCount() + "#" + tagStr;
    }

    private static String buildRecipeVariantKey(List<List<ItemStack>> inputSlots, String slotSummary, String levelSummary) {
        StringBuilder builder = new StringBuilder();
        builder.append(slotSummary).append('|').append(levelSummary).append('|');
        for (List<ItemStack> slot : inputSlots) {
            builder.append('[');
            for (int i = 0; i < slot.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(stackIdentityKey(slot.get(i), true));
            }
            builder.append(']');
        }
        return builder.toString();
    }

    private static String formatSlotCount(SlotType.SlotCount slots) {
        if (slots == null || slots.count() <= 0 || slots.type() == null) {
            return Component.translatable("filter.tinkers_construct_filter.modifier_slotless").getString();
        }
        return Component.translatable("screen.tinkers_construct_filter.slot_cost",
            slots.count(), slots.type().getDisplayName()).getString();
    }

    private static String formatRecipeLevel(IntRange range) {
        if (range == null) {
            return "";
        }
        int min = Math.max(1, range.min());
        int max = range.max();
        if (max >= 1000) {
            return min > 1
                ? Component.translatable("screen.tinkers_construct_filter.level_from_unbounded", min).getString()
                : Component.translatable("screen.tinkers_construct_filter.level_unbounded").getString();
        }
        if (min == 1 && max == 1) {
            return Component.translatable("screen.tinkers_construct_filter.level_single").getString();
        }
        if (min == max) {
            return "Lv." + max;
        }
        if (min > 1) {
            return "Lv." + min + "-" + max;
        }
        return Component.translatable("screen.tinkers_construct_filter.level_max", max).getString();
    }

    private static String formatAggregateLevel(int minLevel, int maxLevel, boolean unbounded, boolean hasRecipe) {
        if (!hasRecipe) {
            return "";
        }
        if (unbounded) {
            return Component.translatable("screen.tinkers_construct_filter.level_unbounded").getString();
        }
        if (maxLevel <= 0) {
            return "";
        }
        if (maxLevel == 1 && minLevel <= 1) {
            return Component.translatable("screen.tinkers_construct_filter.level_single").getString();
        }
        return Component.translatable("screen.tinkers_construct_filter.level_max", maxLevel).getString();
    }

    private static void addTagMetadata(Modifier modifier, MutableModifierMetadata metadata) {
        if (modifier.is(TinkerTags.Modifiers.UPGRADES)) {
            metadata.slotCategories.add("upgrade");
        }
        if (modifier.is(TinkerTags.Modifiers.DEFENSE)) {
            metadata.slotCategories.add("defense");
        }
        if (modifier.is(TinkerTags.Modifiers.ABILITIES)) {
            metadata.slotCategories.add("ability");
        }
        if (modifier.is(TinkerTags.Modifiers.SLOTLESS)) {
            metadata.slotCategories.add("slotless");
        }
    }

    private static void addSlotCategory(SlotType slotType, Set<String> categories) {
        if (slotType == null) {
            categories.add("slotless");
        } else if (SlotType.UPGRADE.equals(slotType)) {
            categories.add("upgrade");
        } else if (SlotType.DEFENSE.equals(slotType)) {
            categories.add("defense");
        } else if (SlotType.ABILITY.equals(slotType)) {
            categories.add("ability");
        } else if (SlotType.SOUL.equals(slotType)) {
            categories.add("soul");
        } else {
            categories.add("slot:" + slotType.getName());
        }
    }

    private static void addToolType(ItemStack stack, Map<String, String> categories) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable modifiable)) {
            return;
        }
        ToolDefinition definition = modifiable.getToolDefinition();
        if (definition == null || definition.getId() == null) {
            return;
        }
        String id = definition.getId().toString();
        String name = stack.getHoverName().getString();
        if (stack.getItem() instanceof ITinkerStationDisplay display) {
            name = display.getLocalizedName().getString();
        }
        if (name.isBlank() || name.equals(id)) {
            name = id;
        }
        categories.putIfAbsent(id, name);
    }

    private static String slotCategoryName(String category) {
        if (category.startsWith("slot:")) {
            String rawName = category.substring("slot:".length());
            SlotType custom = SlotType.getIfPresent(rawName);
            if (custom != null) {
                return custom.getDisplayName().getString();
            }
            return prettyCategoryName(rawName);
        }
        return switch (category) {
            case "upgrade" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_upgrade").getString();
            case "defense" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_defense").getString();
            case "ability" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_ability").getString();
            case "soul" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_soul").getString();
            case "slotless" -> Component.translatable("filter.tinkers_construct_filter.modifier_slotless").getString();
            case "non-craftable" -> Component.translatable("filter.tinkers_construct_filter.non_craftable_trait").getString();
            default -> prettyCategoryName(category);
        };
    }

    private static String prettyCategoryName(String category) {
        String[] parts = category.split("/");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(" / ");
            }
            result.append(part.replace('_', ' '));
        }
        return result.toString();
    }

    private static Map<String, String> titledCategories(Set<String> categories) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String category : categories) {
            result.put(category, slotCategoryName(category));
        }
        return result;
    }

    private static List<PartTemplate> collectPartTemplates(IMaterialRegistry registry) {
        Map<String, Map<String, String>> toolsByPart = new LinkedHashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof IModifiable modifiable)) {
                continue;
            }
            ResourceLocation toolId = ForgeRegistries.ITEMS.getKey(item);
            if (toolId == null) {
                continue;
            }
            try {
                ToolDefinition definition = modifiable.getToolDefinition();
                if (definition == null || !definition.isDataLoaded()) {
                    TinkersConstructFilter.LOGGER.debug("Skipping unloaded tool definition for part filters: {}", toolId);
                    continue;
                }
                List<IToolPart> toolParts = ToolPartsHook.parts(definition);
                if (toolParts.isEmpty()) {
                    continue;
                }
                String toolName = item instanceof ITinkerStationDisplay display
                    ? display.getLocalizedName().getString()
                    : Component.translatable(item.getDescriptionId()).getString();

                for (IToolPart toolPart : toolParts) {
                    ResourceLocation partId = ForgeRegistries.ITEMS.getKey(toolPart.asItem());
                    if (partId != null) {
                        toolsByPart.computeIfAbsent(partId.toString(), ignored -> new LinkedHashMap<>())
                            .putIfAbsent(toolId.toString(), toolName);
                    }
                }
            } catch (RuntimeException exception) {
                TinkersConstructFilter.LOGGER.debug("Skipping unavailable tool definition for part filters: {}", toolId, exception);
            }
        }
        TinkersConstructFilter.LOGGER.debug("Collected tool associations for {} distinct part items", toolsByPart.size());

        List<PartTemplate> result = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof IToolPart part)) {
                continue;
            }

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) {
                continue;
            }

            MaterialStatsId statType = part.getStatType();
            result.add(new PartTemplate(part, itemId.toString(), statType, statType.toString(), statTypeName(registry, statType),
                toolsByPart.getOrDefault(itemId.toString(), Map.of())));
        }
        result.sort(Comparator.comparing(PartTemplate::typeId).thenComparing(PartTemplate::itemId));
        return List.copyOf(result);
    }

    private static String statTypeName(IMaterialRegistry registry, MaterialStatsId statType) {
        try {
            IMaterialStats stats = registry.getDefaultStats(statType);
            if (stats != null) {
                String name = stats.getLocalizedName().getString();
                if (!name.isBlank()) {
                    return name;
                }
            }
            return statType.toString();
        } catch (RuntimeException exception) {
            return statType.toString();
        }
    }

    private static IMaterialStats findStats(IMaterialRegistry registry, MaterialId materialId, MaterialStatsId statType) {
        for (IMaterialStats stats : registry.getAllStats(materialId)) {
            if (stats.getIdentifier().equals(statType)) {
                return stats;
            }
        }
        return null;
    }

    private static List<TraitData> collectTraits(IMaterialRegistry registry, MaterialId materialId, MaterialStatsId statType) {
        Map<String, TraitData> traits = new LinkedHashMap<>();
        addTraits(registry.getTraits(materialId, statType), traits);
        return List.copyOf(traits.values());
    }

    private static void addTraits(Collection<ModifierEntry> entries, Map<String, TraitData> traits) {
        for (ModifierEntry entry : entries) {
            try {
                Modifier modifier = entry.getModifier();
                String id = entry.getId().toString();
                String name = entry.getDisplayName().getString();
                List<String> descriptions = modifier == null ? List.of() : modifier.getDescriptionList().stream()
                    .map(Component::getString)
                    .filter(description -> !description.isBlank())
                    .toList();
                TraitData trait = new TraitData(id, name, entry.getLevel(), descriptions);
                traits.merge(id, trait, (left, right) -> left.level() >= right.level() ? left : right);
            } catch (RuntimeException exception) {
                TinkersConstructFilter.LOGGER.debug("Skipping unavailable material trait", exception);
            }
        }
    }

    private static Map<MaterialId, List<MaterialRecipe>> collectMaterialRecipes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Map.of();
        }
        Map<MaterialId, List<MaterialRecipe>> byMaterial = new HashMap<>();
        for (Recipe<?> recipe : minecraft.level.getRecipeManager().getRecipes()) {
            if (recipe instanceof MaterialRecipe materialRecipe && materialRecipe.getMaterial() != null) {
                byMaterial.computeIfAbsent(materialRecipe.getMaterial().getId(), ignored -> new ArrayList<>())
                    .add(materialRecipe);
            }
        }
        return byMaterial;
    }

    private static ItemStack copySingle(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static List<ItemStack> collectMaterialItems(MaterialId materialId, MaterialVariant variant,
                                                        Map<MaterialId, List<MaterialRecipe>> materialRecipes) {
        List<ItemStack> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<MaterialRecipe> recipes = materialRecipes.getOrDefault(materialId, List.of());
        if (recipes.isEmpty()) {
            return List.of();
        }
        try {
            MaterialVariantId targetVariant = variant.getVariant();
            for (MaterialRecipe recipe : recipes) {
                if (recipe.getMaterial().matchesVariant(targetVariant)) {
                    for (ItemStack stack : recipe.getDisplayItems()) {
                        if (!stack.isEmpty() && seen.add(stackIdentityKey(stack, true))) {
                            result.add(copySingle(stack));
                            if (result.size() >= MAX_MATERIAL_ITEMS) {
                                return List.copyOf(result);
                            }
                        }
                    }
                }
            }
            for (MaterialRecipe recipe : recipes) {
                for (ItemStack stack : recipe.getDisplayItems()) {
                    if (!stack.isEmpty() && seen.add(stackIdentityKey(stack, true))) {
                        result.add(copySingle(stack));
                        if (result.size() >= MAX_MATERIAL_ITEMS) {
                            return List.copyOf(result);
                        }
                    }
                }
            }
        } catch (RuntimeException exception) {
            TinkersConstructFilter.LOGGER.debug("Skipping unavailable material recipe items for {}", materialId, exception);
        }
        return List.copyOf(result);
    }

    private static List<String> collectMaterialFluids(MaterialVariant variant) {
        Set<String> lines = new LinkedHashSet<>();
        try {
            for (MaterialFluidRecipe recipe : MaterialCastingLookup.getCastingFluids(variant.getVariant())) {
                if (recipe == null || recipe == MaterialFluidRecipe.EMPTY) {
                    continue;
                }
                int temp = recipe.getTemperature();
                for (FluidStack fluid : recipe.getFluids()) {
                    if (!fluid.isEmpty()) {
                        lines.add(Component.translatable("screen.tinkers_construct_filter.fluid_temp",
                            fluid.getDisplayName(), temp).getString());
                        break;
                    }
                }
            }
            for (MaterialFluidRecipe recipe : MaterialCastingLookup.getCompositeFluids(variant.getVariant())) {
                if (recipe == null || recipe == MaterialFluidRecipe.EMPTY) {
                    continue;
                }
                int temp = recipe.getTemperature();
                String inputName = recipe.getInput() != null && !recipe.getInput().isEmpty()
                    ? materialName(recipe.getInput().getId())
                    : "";
                for (FluidStack fluid : recipe.getFluids()) {
                    if (!fluid.isEmpty()) {
                        if (!inputName.isEmpty()) {
                            lines.add(Component.translatable("screen.tinkers_construct_filter.composite_fluid_temp",
                                fluid.getDisplayName(), inputName, temp).getString());
                        } else {
                            lines.add(Component.translatable("screen.tinkers_construct_filter.fluid_temp",
                                fluid.getDisplayName(), temp).getString());
                        }
                        break;
                    }
                }
            }
        } catch (RuntimeException exception) {
            TinkersConstructFilter.LOGGER.debug("Skipping unavailable material fluid info for {}", variant.getVariant(), exception);
        }
        return List.copyOf(lines);
    }

    private static String materialName(MaterialId id) {
        String translationKey = "material." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        String name = Component.translatable(translationKey).getString();
        return translationKey.equals(name) ? id.toString() : name;
    }

    private static String generalMaterialDescription(MaterialId id) {
        String prefix = "material." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        for (String key : List.of(
            prefix + ".encyclopedia",
            prefix + ".flavor",
            prefix + ".skull_encyclopedia",
            prefix + ".encyclopedia.melee_harvest",
            prefix + ".encyclopedia.armor",
            prefix + ".encyclopedia.ranged"
        )) {
            if (!I18n.exists(key)) {
                continue;
            }
            String value = ForgeI18n.getPattern(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String materialDescription(MaterialId id, MaterialStatsId statType) {
        String prefix = "material." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        String suffix = switch (statType.getPath()) {
            case "head", "handle", "binding" -> "melee_harvest";
            case "limb", "grip", "bowstring" -> "ranged";
            case "arrow_head", "arrow_shaft", "fletching" -> "ammo";
            case "maille", "shield_core", "cuirass", "plating_helmet", "plating_chestplate", "plating_leggings", "plating_boots", "plating_shield" -> "armor";
            case "slime" -> "slime";
            case "shell" -> "shell";
            case "laces" -> "laces";
            case "ribcage" -> "ribcage";
            case "skull" -> "skull";
            default -> "";
        };

        List<String> keys = new ArrayList<>();
        if ("skull".equals(suffix)) {
            keys.add(prefix + ".skull_encyclopedia");
        }
        if (!suffix.isEmpty()) {
            keys.add(prefix + ".encyclopedia." + suffix);
            keys.add(prefix + "." + suffix);
        }
        keys.add(prefix + ".encyclopedia");
        for (String key : keys) {
            if (!I18n.exists(key)) {
                continue;
            }
            String value = ForgeI18n.getPattern(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record PartTemplate(IToolPart part, String itemId, MaterialStatsId statType, String typeId, String typeName,
                                Map<String, String> toolCategories) {
        private PartTemplate {
            toolCategories = Collections.unmodifiableMap(new LinkedHashMap<>(toolCategories));
        }
    }

    private record TraitData(String id, String name, int level, List<String> descriptions) {
    }

    private abstract static class BaseEntry implements CatalogEntry {
        private final String id;
        private final String name;
        private final int materialLevel;
        private final List<TraitData> traits;
        private final List<CatalogEntry.TraitTooltip> traitTooltips;
        private final List<String> traitNames;
        private final List<String> traitDescriptions;
        private final Map<String, Integer> traitLevels;
        private final Map<String, Double> attributeValues;
        private final Map<String, String> attributeTexts;
        private final ItemStack displayStack;
        private final String searchText;

        private BaseEntry(String id, ItemStack displayStack, String name, int materialLevel, List<TraitData> traits, Map<String, Double> attributeValues) {
            this(id, displayStack, name, materialLevel, traits, attributeValues, Map.of());
        }

        private BaseEntry(String id, ItemStack displayStack, String name, int materialLevel, List<TraitData> traits, Map<String, Double> attributeValues,
                          Map<String, String> suppliedAttributeTexts) {
            this.id = id;
            this.name = name;
            this.materialLevel = materialLevel;
            this.traits = List.copyOf(traits);
            Map<String, Integer> levels = new LinkedHashMap<>();
            this.traits.forEach(trait -> levels.merge(trait.id(), trait.level(), Math::max));
            this.traitLevels = Collections.unmodifiableMap(levels);
            this.traitTooltips = this.traits.stream()
                .map(trait -> new CatalogEntry.TraitTooltip(trait.id(), trait.name(), trait.descriptions()))
                .toList();
            this.traitNames = this.traitTooltips.stream().map(CatalogEntry.TraitTooltip::name).toList();
            this.traitDescriptions = this.traitTooltips.stream().flatMap(trait -> trait.descriptions().stream()).toList();
            this.attributeValues = Collections.unmodifiableMap(new LinkedHashMap<>(attributeValues));
            Map<String, String> texts = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : attributeValues.entrySet()) {
                texts.put(entry.getKey(), suppliedAttributeTexts.getOrDefault(entry.getKey(), CatalogAttributes.format(entry.getKey(), entry.getValue())));
            }
            for (Map.Entry<String, String> entry : suppliedAttributeTexts.entrySet()) {
                texts.putIfAbsent(entry.getKey(), entry.getValue());
            }
            this.attributeTexts = Collections.unmodifiableMap(texts);
            this.displayStack = displayStack.copy();
            List<String> searchParts = new ArrayList<>();
            searchParts.add(id);
            searchParts.add(name);
            searchParts.addAll(this.traitNames);
            searchParts.addAll(this.traitDescriptions);
            this.searchText = String.join("\n", searchParts).toLowerCase(Locale.ROOT);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getMaterialLevel() {
            return materialLevel;
        }

        @Override
        public List<String> getTraitNames() {
            return traitNames;
        }

        @Override
        public Map<String, Integer> getTraitLevels() {
            return traitLevels;
        }

        @Override
        public List<String> getTraitDescriptions() {
            return traitDescriptions;
        }

        @Override
        public List<CatalogEntry.TraitTooltip> getTraitTooltips() {
            return traitTooltips;
        }

        @Override
        public Map<String, Double> getAttributeValues() {
            return attributeValues;
        }

        @Override
        public Map<String, String> getAttributeTexts() {
            return attributeTexts;
        }

        @Override
        public ItemStack getDisplayStack() {
            return displayStack.copy();
        }

        @Override
        public String getSearchText() {
            return searchText;
        }
    }

    private static final class MaterialEntry extends BaseEntry implements CatalogApi.MaterialView {
        private final int defaultSortOrder;
        private final int availablePartCount;
        private final Set<String> productionMethods;
        private final List<ItemStack> materialItems;
        private final List<String> castingFluids;
        private final String generalDescription;
        private final List<CatalogApi.PartView> extraStatCards;

        private MaterialEntry(String id, ItemStack displayStack, String name, int materialLevel, List<TraitData> traits, Map<String, Double> attributeValues,
                              Map<String, String> attributeTexts,
                              int defaultSortOrder, int availablePartCount, Set<String> productionMethods,
                              List<ItemStack> materialItems, List<String> castingFluids, String generalDescription,
                              List<CatalogApi.PartView> extraStatCards) {
            super(id, displayStack, name, materialLevel, traits, attributeValues, attributeTexts);
            this.defaultSortOrder = defaultSortOrder;
            this.availablePartCount = availablePartCount;
            this.productionMethods = Set.copyOf(productionMethods);
            this.materialItems = materialItems.stream().map(ItemStack::copy).toList();
            this.castingFluids = List.copyOf(castingFluids);
            this.generalDescription = generalDescription == null ? "" : generalDescription;
            this.extraStatCards = List.copyOf(extraStatCards);
        }

        @Override
        public Set<String> getProductionMethods() {
            return productionMethods;
        }

        @Override
        public int getDefaultSortOrder() {
            return defaultSortOrder;
        }

        @Override
        public int getAvailablePartCount() {
            return availablePartCount;
        }

        @Override
        public ItemStack getMaterialDisplayItem() {
            return getDisplayStack();
        }

        @Override
        public List<ItemStack> getMaterialItems() {
            return materialItems.stream().map(ItemStack::copy).toList();
        }

        @Override
        public List<String> getCastingFluids() {
            return castingFluids;
        }

        @Override
        public String getGeneralDescription() {
            return generalDescription;
        }

        @Override
        public List<CatalogApi.PartView> getExtraStatCards() {
            return extraStatCards;
        }

        @Override
        public String getSearchText() {
            StringBuilder extra = new StringBuilder(super.getSearchText());
            for (ItemStack item : materialItems) {
                extra.append('\n').append(item.getHoverName().getString());
            }
            for (String fluid : castingFluids) {
                extra.append('\n').append(fluid);
            }
            if (!generalDescription.isEmpty()) {
                extra.append('\n').append(generalDescription);
            }
            return extra.toString().toLowerCase(Locale.ROOT);
        }
    }

    private static final class PartEntry extends BaseEntry implements CatalogApi.PartView {
        private final String materialId;
        private final String materialName;
        private final String partId;
        private final String partType;
        private final String partTypeName;
        private final String materialDescription;
        private final Map<String, String> toolCategories;
        private final Set<String> productionMethods;

        private PartEntry(String id, ItemStack displayStack, String materialId, String materialName, int materialLevel, List<TraitData> traits, Map<String, Double> attributeValues,
                          Map<String, String> attributeTexts,
                          String materialDescription,
                          String partId, String partType, String partTypeName, Map<String, String> toolCategories, Set<String> productionMethods) {
            super(id, displayStack, displayStack.isEmpty() ? materialName : displayStack.getHoverName().getString(), materialLevel, traits, attributeValues, attributeTexts);
            this.materialId = materialId;
            this.materialName = materialName;
            this.partId = partId;
            this.partType = partType;
            this.partTypeName = partTypeName;
            this.materialDescription = materialDescription;
            this.toolCategories = toolCategories;
            this.productionMethods = Set.copyOf(productionMethods);
        }

        @Override
        public Set<String> getProductionMethods() {
            return productionMethods;
        }

        @Override
        public String getMaterialId() {
            return materialId;
        }

        @Override
        public String getMaterialName() {
            return materialName;
        }

        @Override
        public String getPartId() {
            return partId;
        }

        @Override
        public String getPartType() {
            return partType;
        }

        @Override
        public String getPartTypeName() {
            return partTypeName;
        }

        @Override
        public String getMaterialDescription() {
            return materialDescription;
        }

        @Override
        public Map<String, String> getToolCategories() {
            return toolCategories;
        }

        @Override
        public ItemStack getMaterializedItem() {
            return getDisplayStack();
        }

        @Override
        public String getSearchText() {
            return (super.getSearchText() + "\n" + materialId + "\n" + materialName + "\n" + partId + "\n" + partType + "\n" + materialDescription).toLowerCase(Locale.ROOT);
        }
    }

    private static final class ModifierCatalogEntry extends BaseEntry implements CatalogApi.ModifierView {
        private List<CatalogEntry> sourceMaterials = List.of();
        private List<CatalogEntry> sourceParts = List.of();
        private List<ItemStack> sourceTools = List.of();

        @Override
        public List<ItemStack> getSourceTools() {
            return sourceTools.stream().map(ItemStack::copy).toList();
        }

        @Override
        public List<CatalogEntry> getSourceMaterials() {
            return sourceMaterials;
        }

        @Override
        public List<CatalogEntry> getSourceParts() {
            return sourceParts;
        }

        private final Map<String, String> slotCategories;
        private final Map<String, String> toolCategories;
        private final List<String> descriptions;
        private final boolean hasModifierRecipe;
        private final List<ItemStack> recipeTools;
        private final List<CatalogApi.ModifierRecipeView> recipeVariants;
        private final String slotRequirementSummary;
        private final String levelSummary;
        private final int maxLevel;

        private ModifierCatalogEntry(String id, ItemStack displayStack, String name, Set<String> slotCategories, Map<String, String> toolCategories, List<String> descriptions,
                                     List<ItemStack> recipeTools, List<CatalogApi.ModifierRecipeView> recipeVariants, boolean hasModifierRecipe,
                                     String slotRequirementSummary, String levelSummary, int maxLevel) {
            super(id, displayStack, name, 0, List.of(), Map.of());
            this.slotCategories = Collections.unmodifiableMap(titledCategories(slotCategories));
            this.toolCategories = Collections.unmodifiableMap(new LinkedHashMap<>(toolCategories));
            this.descriptions = List.copyOf(descriptions);
            this.hasModifierRecipe = hasModifierRecipe;
            this.recipeTools = recipeTools.stream().map(ItemStack::copy).toList();
            this.recipeVariants = recipeVariants.stream()
                .map(recipe -> new CatalogApi.ModifierRecipeView(recipe.inputSlots(), recipe.slotSummary(), recipe.levelSummary()))
                .toList();
            this.slotRequirementSummary = slotRequirementSummary == null ? "" : slotRequirementSummary;
            this.levelSummary = levelSummary == null ? "" : levelSummary;
            this.maxLevel = maxLevel;
        }

        @Override
        public boolean hasModifierRecipe() {
            return hasModifierRecipe;
        }

        @Override
        public Map<String, String> getSlotCategories() {
            return slotCategories;
        }

        @Override
        public Map<String, String> getToolCategories() {
            return toolCategories;
        }

        @Override
        public List<String> getModifierDescriptions() {
            return descriptions;
        }

        @Override
        public List<ItemStack> getRecipeTools() {
            return recipeTools.stream().map(ItemStack::copy).toList();
        }

        @Override
        public List<CatalogApi.ModifierRecipeView> getRecipeVariants() {
            return recipeVariants.stream()
                .map(recipe -> new CatalogApi.ModifierRecipeView(recipe.inputSlots(), recipe.slotSummary(), recipe.levelSummary()))
                .toList();
        }

        @Override
        public String getSlotRequirementSummary() {
            return slotRequirementSummary;
        }

        @Override
        public String getLevelSummary() {
            return levelSummary;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }

        @Override
        public List<String> getTraitDescriptions() {
            return descriptions;
        }

        @Override
        public List<CatalogEntry.TraitTooltip> getTraitTooltips() {
            return List.of(new CatalogEntry.TraitTooltip(getId(), getName(), descriptions));
        }

        @Override
        public String getSearchText() {
            return (super.getSearchText() + "\n" + String.join("\n", descriptions) + "\n"
                + String.join("\n", slotCategories.values()) + "\n" + String.join("\n", toolCategories.values())
                + "\n" + slotRequirementSummary + "\n" + levelSummary)
                .toLowerCase(Locale.ROOT);
        }
    }

    private static final class MutableModifierMetadata {
        private final Set<String> slotCategories = new LinkedHashSet<>();
        private final Set<String> slotSummaries = new LinkedHashSet<>();
        private final Map<String, String> toolCategories = new LinkedHashMap<>();
        private final Set<String> recipeToolKeys = new HashSet<>();
        private final List<ItemStack> recipeTools = new ArrayList<>();
        private final Set<String> recipeKeys = new HashSet<>();
        private final List<CatalogApi.ModifierRecipeView> recipeVariants = new ArrayList<>();
        private int minRecipeLevel = Integer.MAX_VALUE;
        private int maxRecipeLevel = 0;
        private boolean unboundedLevel = false;
    }
}
