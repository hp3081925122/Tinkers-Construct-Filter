package org.hp.tinkers_construct_filter.client.catalog;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.common.TinkerTags;

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
    private CatalogDataBuilder() {
    }

    public static CatalogSnapshot build() {
        if (!MaterialRegistry.isFullyLoaded() || !ModifierManager.INSTANCE.isDynamicModifiersLoaded()) {
            return CatalogSnapshot.loading();
        }

        IMaterialRegistry registry = MaterialRegistry.getInstance();
        List<PartTemplate> partTemplates = collectPartTemplates(registry);
        List<CatalogEntry> materials = new ArrayList<>();
        List<CatalogEntry> parts = new ArrayList<>();
        // 一次收集全部词条，再按真实配方区分强化列表。
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
                materialAttributes.putAll(CatalogAttributes.collect(stats));
                materialAttributeTexts.putAll(CatalogAttributes.collectTexts(stats));
            }

            ItemStack fallbackDisplay = materialDisplay(variant);
            int availablePartCount = 0;
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

                availablePartCount++;
                if (fallbackDisplay.isEmpty()) {
                    fallbackDisplay = displayStack.copy();
                }

                List<TraitData> partTraits = collectTraits(registry, materialId, template.statType());
                for (TraitData trait : partTraits) {
                    materialTraitMap.putIfAbsent(trait.id(), trait);
                }

                parts.add(new PartEntry(
                    materialId + "@" + template.itemId(),
                    displayStack,
                    materialId.toString(),
                    materialName(materialId),
                    material.getTier(),
                    partTraits,
                    CatalogAttributes.collect(stats),
                    CatalogAttributes.collectTexts(stats),
                    template.itemId(),
                    template.typeId(),
                    template.typeName(),
                    template.toolCategories()
                ));
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
                availablePartCount
            ));
        }

        // 快照构建时建立反向索引，不在每一帧扫描全部材料和部件。
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
        for (CatalogEntry entry : traits) {
            ModifierCatalogEntry modifier = (ModifierCatalogEntry) entry;
            modifier.sourceMaterials = List.copyOf(materialSources.getOrDefault(entry.getId(), List.of()));
            modifier.sourceParts = List.copyOf(partSources.getOrDefault(entry.getId(), List.of()));
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
                    // 全词条保留无配方项目，但不将其标记为无槽位强化。
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
                    result.add(new ModifierCatalogEntry(
                        id,
                        slimeknights.tconstruct.tools.item.ModifierCrystalItem.withModifier(modifier.getId()),
                        name,
                        data.slotCategories,
                        data.toolCategories,
                        descriptions,
                        data.recipeTools,
                        data.recipeVariants,
                        hasModifierRecipe
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
                for (ItemStack tool : modifierRecipe.getToolWithoutModifier()) {
                    addToolType(tool, data.toolCategories);
                    if (!tool.isEmpty() && data.recipeToolKeys.add(tool.toString())) {
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
                    String recipeKey = inputSlots.toString();
                    if (data.recipeKeys.add(recipeKey)) {
                        data.recipeVariants.add(new CatalogApi.ModifierRecipeView(inputSlots));
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
        return switch (category) {
            case "upgrade" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_upgrade").getString();
            case "defense" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_defense").getString();
            case "ability" -> Component.translatable("filter.tinkers_construct_filter.modifier_slot_ability").getString();
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
        // 每次构建快照只扫描一次工具定义，按具体部件物品建立反向索引，兼容附属工具。
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

                // 使用部件注册 ID 匹配，避免把属性类型相同但不能互换的部件混为一类。
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

        // 每种部件共享一份工具索引，各材质变体无需重复查询工具定义。
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
            String name = stats.getLocalizedName().getString();
            return name.equals(stats.getLocalizedName().getString()) ? name : statType.toString();
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
        addTraits(registry.getDefaultTraits(materialId), traits);
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
                traits.putIfAbsent(id, new TraitData(id, name, descriptions));
            } catch (RuntimeException exception) {
                TinkersConstructFilter.LOGGER.debug("Skipping unavailable material trait", exception);
            }
        }
    }

    private static ItemStack materialDisplay(MaterialVariant variant) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return ItemStack.EMPTY;
            }
            for (Recipe<?> recipe : minecraft.level.getRecipeManager().getRecipes()) {
                if (recipe instanceof MaterialRecipe materialRecipe
                    && materialRecipe.getMaterial().matchesVariant(variant.getVariant())) {
                    return materialRecipe.getDisplayItems().stream()
                        .filter(stack -> !stack.isEmpty())
                        .findFirst()
                        .map(ItemStack::copy)
                        .orElse(ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private static String materialName(MaterialId id) {
        String translationKey = "material." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        String name = Component.translatable(translationKey).getString();
        return translationKey.equals(name) ? id.toString() : name;
    }

    private record PartTemplate(IToolPart part, String itemId, MaterialStatsId statType, String typeId, String typeName,
                                Map<String, String> toolCategories) {
        // 固化索引，保证所有材质变体安全共享。
        private PartTemplate {
            toolCategories = Collections.unmodifiableMap(new LinkedHashMap<>(toolCategories));
        }
    }

    private record TraitData(String id, String name, List<String> descriptions) {
    }

    private abstract static class BaseEntry implements CatalogEntry {
        private final String id;
        private final String name;
        private final int materialLevel;
        private final List<TraitData> traits;
        private final List<CatalogEntry.TraitTooltip> traitTooltips;
        private final List<String> traitNames;
        private final List<String> traitDescriptions;
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
            this.traitTooltips = this.traits.stream()
                .map(trait -> new CatalogEntry.TraitTooltip(trait.name(), trait.descriptions()))
                .toList();
            this.traitNames = this.traitTooltips.stream().map(CatalogEntry.TraitTooltip::name).toList();
            this.traitDescriptions = this.traitTooltips.stream().flatMap(trait -> trait.descriptions().stream()).toList();
            this.attributeValues = Collections.unmodifiableMap(new LinkedHashMap<>(attributeValues));
            Map<String, String> texts = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : attributeValues.entrySet()) {
                texts.put(entry.getKey(), suppliedAttributeTexts.getOrDefault(entry.getKey(), CatalogAttributes.format(entry.getKey(), entry.getValue())));
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

        private MaterialEntry(String id, ItemStack displayStack, String name, int materialLevel, List<TraitData> traits, Map<String, Double> attributeValues,
                              Map<String, String> attributeTexts,
                              int defaultSortOrder, int availablePartCount) {
            super(id, displayStack, name, materialLevel, traits, attributeValues, attributeTexts);
            this.defaultSortOrder = defaultSortOrder;
            this.availablePartCount = availablePartCount;
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
    }

    private static final class PartEntry extends BaseEntry implements CatalogApi.PartView {
        private final String materialId;
        private final String materialName;
        private final String partId;
        private final String partType;
        private final String partTypeName;
        private final Map<String, String> toolCategories;

        private PartEntry(String id, ItemStack displayStack, String materialId, String materialName, int materialLevel, List<TraitData> traits, Map<String, Double> attributeValues,
                          Map<String, String> attributeTexts,
                          String partId, String partType, String partTypeName, Map<String, String> toolCategories) {
            super(id, displayStack, displayStack.getHoverName().getString(), materialLevel, traits, attributeValues, attributeTexts);
            this.materialId = materialId;
            this.materialName = materialName;
            this.partId = partId;
            this.partType = partType;
            this.partTypeName = partTypeName;
            this.toolCategories = toolCategories;
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

        /** 提供快照中已缓存的工具筛选选项。 */
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
            return (super.getSearchText() + "\n" + materialId + "\n" + materialName + "\n" + partId + "\n" + partType).toLowerCase(Locale.ROOT);
        }
    }

    private static final class ModifierCatalogEntry extends BaseEntry implements CatalogApi.ModifierView {
        // 仅在快照发布之前写入，后续悬浮查询直接复用不可变列表。
        private List<CatalogEntry> sourceMaterials = List.of();
        private List<CatalogEntry> sourceParts = List.of();

        /** 返回含有该词条的材料。 */
        @Override
        public List<CatalogEntry> getSourceMaterials() { return sourceMaterials; }

        /** 返回含有该词条的材料化部件。 */
        @Override
        public List<CatalogEntry> getSourceParts() { return sourceParts; }

        private final Map<String, String> slotCategories;
        private final Map<String, String> toolCategories;
        private final List<String> descriptions;
        // 配方存在性独立于输入物品数量，避免把特殊配方误判为无配方。
        private final boolean hasModifierRecipe;

        private ModifierCatalogEntry(String id, ItemStack displayStack, String name, Set<String> slotCategories, Map<String, String> toolCategories, List<String> descriptions,
                                     List<ItemStack> recipeTools, List<CatalogApi.ModifierRecipeView> recipeVariants, boolean hasModifierRecipe) {
            super(id, displayStack, name, 0, List.of(), Map.of());
            this.slotCategories = Collections.unmodifiableMap(titledCategories(slotCategories));
            this.toolCategories = Collections.unmodifiableMap(new LinkedHashMap<>(toolCategories));
            this.descriptions = List.copyOf(descriptions);
            this.hasModifierRecipe = hasModifierRecipe;
            this.recipeTools = recipeTools.stream().map(ItemStack::copy).toList();
            this.recipeVariants = recipeVariants.stream()
                .map(recipe -> new CatalogApi.ModifierRecipeView(recipe.inputSlots()))
                .toList();
        }

        private final List<ItemStack> recipeTools;
        private final List<CatalogApi.ModifierRecipeView> recipeVariants;

        /** 返回当前词条是否有可展示的强化配方。 */
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
                .map(recipe -> new CatalogApi.ModifierRecipeView(recipe.inputSlots()))
                .toList();
        }

        @Override
        public List<String> getTraitDescriptions() {
            return descriptions;
        }

        @Override
        public List<CatalogEntry.TraitTooltip> getTraitTooltips() {
            return List.of(new CatalogEntry.TraitTooltip(getName(), descriptions));
        }

        @Override
        public String getSearchText() {
            return (super.getSearchText() + "\n" + String.join("\n", descriptions) + "\n"
                + String.join("\n", slotCategories.values()) + "\n" + String.join("\n", toolCategories.values()))
                .toLowerCase(Locale.ROOT);
        }
    }

    private static final class MutableModifierMetadata {
        private final Set<String> slotCategories = new LinkedHashSet<>();
        private final Map<String, String> toolCategories = new LinkedHashMap<>();
        private final Set<String> recipeToolKeys = new HashSet<>();
        private final List<ItemStack> recipeTools = new ArrayList<>();
        private final Set<String> recipeKeys = new HashSet<>();
        private final List<CatalogApi.ModifierRecipeView> recipeVariants = new ArrayList<>();
    }

}
