package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.hp.tinkers_construct_filter.TinkersConstructFilter;
import org.hp.tinkers_construct_filter.client.catalog.CatalogApi;
import org.hp.tinkers_construct_filter.client.catalog.CatalogAttributes;
import org.hp.tinkers_construct_filter.client.catalog.CatalogDataBuilder;
import org.hp.tinkers_construct_filter.client.catalog.CatalogEntry;
import org.hp.tinkers_construct_filter.client.catalog.CatalogExtensions;
import org.hp.tinkers_construct_filter.client.catalog.CatalogSnapshot;
import org.hp.tinkers_construct_filter.client.config.ClientConfig;
import org.lwjgl.glfw.GLFW;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TinkersCatalogScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 920;
    private static final int PANEL_MAX_HEIGHT = 640;
    private static final int NAV_WIDTH = 112;
    private static final int ROW_HEIGHT = 38;
    private static final int POPUP_ROW_HEIGHT = 18;
    private static final int POPUP_Z = 200;
    private static final int CATEGORY_BAR_HEIGHT = 20;
    private static final int CATEGORY_GAP = 3;
    private static final int FILTER_CATEGORY_CLEAR_WIDTH = 38;
    private static final int IMPORTANT_BUTTON_WIDTH = 90;
    private static final int IMPORTANT_POPUP_MIN_WIDTH = 180;
    private static final int RECIPE_OVERLAY_LINE_HEIGHT = 12;
    private static final int MATERIAL_INFO_LINE_HEIGHT = 12;
    private static final int MATERIAL_INFO_MIN_HEIGHT = 60;

    private final Screen parent;
    private final EnumMap<Page, LinkedHashSet<String>> selectedFilters = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> selectedFilterCategories = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> selectedSorts = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> selectedSortCategories = new EnumMap<>(Page.class);
    private final EnumMap<Page, Boolean> sortDescending = new EnumMap<>(Page.class);
    private final EnumMap<Page, Boolean> sortAutomatic = new EnumMap<>(Page.class);
    private final Map<CatalogEntry, Object> activeSortValues = new HashMap<>();
    private final LinkedHashSet<String> selectedImportantPartTypes = new LinkedHashSet<>();

    private CatalogSnapshot snapshot = CatalogSnapshot.loading();
    private List<CatalogEntry> visibleEntries = List.of();
    private CatalogApi.ModifierView recipeOverlayEntry;
    private List<ItemStack> recipeOverlayTools = List.of();
    private List<ItemStack> recipeOverlayItems = List.of();
    private int recipeOverlayX;
    private int recipeOverlayY;
    private int recipeOverlayWidth;
    private int recipeOverlayHeight;
    private int recipeOverlayScroll;
    private int recipeOverlayContentHeight;
    private int recipeOverlayViewportHeight;
    private ItemStack hoveredRecipeItem = ItemStack.EMPTY;
    private boolean recipeOverlayLocked;
    private CatalogApi.MaterialView materialInfoEntry;
    private List<CatalogApi.PartView> materialInfoParts = List.of();
    private int materialInfoX;
    private int materialInfoY;
    private int materialInfoWidth;
    private int materialInfoHeight;
    private int materialInfoScroll;
    private int materialInfoContentHeight;
    private int materialInfoViewportHeight;
    private CatalogEntry.TraitTooltip hoveredMaterialTrait;
    private boolean materialInfoLocked;
    private Page page = Page.MATERIALS;
    private EditBox searchBox;
    private Button materialButton;
    private Button partButton;
    private Button modifierButton;
    private Button filterButton;
    private Button sortButton;
    private Button orderButton;
    private Button importantButton;
    private String searchQuery = "";
    private boolean catalogInitialized;
    private boolean historyOpen;
    private boolean filterOpen;
    private boolean sortOpen;
    private boolean importantOpen;
    private int mainScroll;
    private int historyScroll;
    private int filterScroll;
    private int filterCategoryScroll;
    private int sortScroll;
    private int sortCategoryScroll;
    private int importantScroll;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int listY;
    private int listBottom;

    public TinkersCatalogScreen(Screen parent) {
        super(Component.translatable("screen.tinkers_construct_filter.catalog"));
        this.parent = parent;
        for (Page value : Page.values()) {
            selectedFilters.put(value, new LinkedHashSet<>());
            sortDescending.put(value, false);
            sortAutomatic.put(value, true);
        }
        selectedSorts.put(Page.MATERIALS, "default-material");
        selectedSorts.put(Page.PARTS, "default-part");
        selectedSorts.put(Page.MODIFIERS, "default-modifier");
    }

    @Override
    protected void init() {
        super.init();
        updateLayout();
        createWidgets();
        if (!catalogInitialized || !snapshot.fullyLoaded()) {
            reloadCatalog();
        } else {
            rebuildVisibleEntries();
        }
    }

    @Override
    public void tick() {
        if (searchBox != null) {
            searchBox.tick();
        }
        if (!snapshot.fullyLoaded() && MaterialRegistry.isFullyLoaded() && ModifierManager.INSTANCE.isDynamicModifiersLoaded()) {
            reloadCatalog();
        }
    }

    private void updateLayout() {
        panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(360, width - 20));
        panelHeight = Math.min(PANEL_MAX_HEIGHT, Math.max(300, height - 20));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentX = panelX + NAV_WIDTH + 10;
        contentY = panelY + 31;
        contentWidth = panelWidth - NAV_WIDTH - 20;
        listY = panelY + 80;
        listBottom = panelY + panelHeight - 13;
    }

    private void createWidgets() {
        materialButton = addRenderableWidget(Button.builder(Component.translatable("button.tinkers_construct_filter.materials"), button -> switchPage(Page.MATERIALS))
            .bounds(panelX + 8, panelY + 52, NAV_WIDTH - 16, 20)
            .build());
        partButton = addRenderableWidget(Button.builder(Component.translatable("button.tinkers_construct_filter.parts"), button -> switchPage(Page.PARTS))
            .bounds(panelX + 8, panelY + 76, NAV_WIDTH - 16, 20)
            .build());
        modifierButton = addRenderableWidget(Button.builder(Component.translatable("button.tinkers_construct_filter.modifiers"), button -> switchPage(Page.MODIFIERS))
            .bounds(panelX + 8, panelY + 100, NAV_WIDTH - 16, 20)
            .build());

        importantButton = addRenderableWidget(Button.builder(Component.translatable("button.tinkers_construct_filter.important_options"), button -> toggleImportantPopup())
            .bounds(contentX, panelY + 4, Math.min(IMPORTANT_BUTTON_WIDTH, contentWidth), 20)
            .build());

        int actionWidth = 54;
        int actionX = contentX + contentWidth - actionWidth * 3 - 8;
        int searchWidth = Math.max(88, actionX - contentX - 4);
        searchBox = addRenderableWidget(new EditBox(font, contentX, contentY, searchWidth, 20, Component.translatable("search.tinkers_construct_filter.hint")));
        searchBox.setMaxLength(120);
        searchBox.setHint(Component.translatable("search.tinkers_construct_filter.hint"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::onSearchChanged);

        filterButton = addRenderableWidget(Button.builder(Component.translatable("button.tinkers_construct_filter.filter"), button -> toggleFilterPopup())
            .bounds(actionX, contentY, actionWidth, 20)
            .build());
        sortButton = addRenderableWidget(Button.builder(Component.translatable("button.tinkers_construct_filter.sort"), button -> toggleSortPopup())
            .bounds(actionX + actionWidth + 4, contentY, actionWidth, 20)
            .build());
        orderButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleSortOrder())
            .bounds(actionX + (actionWidth + 4) * 2, contentY, actionWidth, 20)
            .build());
        updateNavigationButtons();
        updateOrderButton();
    }

    private void onSearchChanged(String value) {
        searchQuery = value;
        mainScroll = 0;
        clearMaterialInfoOverlay();
        rebuildVisibleEntries();
    }

    private void reloadCatalog() {
        CatalogExtensions.refreshDefinitions();
        snapshot = CatalogDataBuilder.build();
        catalogInitialized = true;
        mainScroll = 0;
        filterScroll = 0;
        filterCategoryScroll = 0;
        sortScroll = 0;
        sortCategoryScroll = 0;
        importantScroll = 0;
        rebuildVisibleEntries();
        syncImportantSelection();
        clearMaterialInfoOverlay();
    }

    private void switchPage(Page nextPage) {
        if (page == nextPage) {
            return;
        }
        page = nextPage;
        mainScroll = 0;
        filterScroll = 0;
        filterCategoryScroll = 0;
        sortScroll = 0;
        sortCategoryScroll = 0;
        clearRecipeOverlay();
        clearMaterialInfoOverlay();
        historyOpen = false;
        filterOpen = false;
        sortOpen = false;
        importantOpen = false;
        updateNavigationButtons();
        updateOrderButton();
        rebuildVisibleEntries();
    }

    private void updateNavigationButtons() {
        if (materialButton == null || partButton == null || modifierButton == null || importantButton == null) {
            return;
        }
        materialButton.active = page != Page.MATERIALS;
        partButton.active = page != Page.PARTS;
        modifierButton.active = page != Page.MODIFIERS;
        importantButton.visible = page == Page.MATERIALS;
        importantButton.active = page == Page.MATERIALS;
    }

    private void toggleFilterPopup() {
        filterOpen = !filterOpen;
        sortOpen = false;
        historyOpen = false;
        importantOpen = false;
        filterScroll = 0;
        clearMaterialInfoOverlay();
        if (filterOpen) {
            clearRecipeOverlay();
        }
    }

    private void toggleSortPopup() {
        sortOpen = !sortOpen;
        filterOpen = false;
        historyOpen = false;
        importantOpen = false;
        sortScroll = 0;
        clearMaterialInfoOverlay();
        if (sortOpen) {
            clearRecipeOverlay();
        }
    }

    private void toggleImportantPopup() {
        importantOpen = !importantOpen;
        historyOpen = false;
        filterOpen = false;
        sortOpen = false;
        importantScroll = 0;
        clearRecipeOverlay();
        clearMaterialInfoOverlay();
        if (importantOpen) {
            syncImportantSelection();
        }
    }

    private void toggleSortOrder() {
        sortAutomatic.put(page, false);
        sortDescending.put(page, !sortDescending.get(page));
        updateOrderButton();
        rebuildVisibleEntries();
    }

    private void updateOrderButton() {
        if (orderButton != null) {
            orderButton.setMessage(Component.translatable(sortDescending.get(page)
                ? "button.tinkers_construct_filter.descending"
                : "button.tinkers_construct_filter.ascending"));
        }
    }

    private void rebuildVisibleEntries() {
        activeSortValues.clear();
        if (!snapshot.fullyLoaded()) {
            visibleEntries = List.of();
            return;
        }

        List<FilterOption> filters = filterOptions();
        Set<String> filterIds = new HashSet<>();
        for (FilterOption filter : filters) {
            filterIds.add(filter.id());
        }
        selectedFilters.get(page).retainAll(filterIds);

        List<CatalogEntry> result = new ArrayList<>();
        for (CatalogEntry entry : sourceEntries()) {
            if (matchesSearch(entry) && matchesFilters(entry, filters)) {
                result.add(entry);
            }
        }

        SortOption sort = activeSortOption(sortOptions());
        if (sort.defaultOrder()) {
            result.sort(defaultComparator());
        } else {
            for (CatalogEntry entry : result) {
                activeSortValues.put(entry, sort.valueGetter().apply(entry));
            }
            if (sortAutomatic.get(page)) {
                Boolean numeric = sort.numeric();
                if (numeric == null) {
                    numeric = activeSortValues.values().stream().filter(value -> value != null).findFirst().map(value -> value instanceof Number).orElse(false);
                }
                sortDescending.put(page, numeric);
                updateOrderButton();
            }
            boolean descending = sortDescending.get(page);
            result.sort((left, right) -> {
                int compare = compareSortValues(activeSortValues.get(left), activeSortValues.get(right), descending);
                if (compare != 0) {
                    return compare;
                }
                return left.getName().compareToIgnoreCase(right.getName());
            });
        }

        visibleEntries = List.copyOf(result);
        mainScroll = clamp(mainScroll, 0, Math.max(0, visibleEntries.size() - visibleRows()));
    }

    private boolean matchesSearch(CatalogEntry entry) {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        String source = entry.getSearchText();
        for (String token : query.split("\\s+")) {
            if (!token.isEmpty() && !source.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesFilters(CatalogEntry entry, List<FilterOption> filters) {
        if (selectedFilters.get(page).isEmpty()) {
            return true;
        }
        for (FilterOption filter : filters) {
            if (selectedFilters.get(page).contains(filter.id()) && !filter.predicate().test(entry)) {
                return false;
            }
        }
        return true;
    }

    private List<CatalogEntry> sourceEntries() {
        return switch (page) {
            case MATERIALS -> snapshot.materials();
            case PARTS -> snapshot.parts();
            case MODIFIERS -> snapshot.modifiers();
        };
    }

    private List<FilterOption> filterOptions() {
        List<FilterOption> result = new ArrayList<>();
        for (FilterCategory category : filterCategories()) {
            result.addAll(category.options());
        }
        return List.copyOf(result);
    }

    private List<FilterCategory> filterCategories() {
        List<FilterOption> result = new ArrayList<>();
        if (page == Page.PARTS) {
            Map<String, String> types = new LinkedHashMap<>();
            for (CatalogEntry entry : snapshot.parts()) {
                if (entry instanceof CatalogApi.PartView view) {
                    types.putIfAbsent(view.getPartType(), view.getPartTypeName());
                }
            }
            for (Map.Entry<String, String> type : types.entrySet()) {
                result.add(new FilterOption("type:" + type.getKey(), type.getValue(), entry -> entry instanceof CatalogApi.PartView view && view.getPartType().equals(type.getKey())));
            }
            for (CatalogExtensions.PartFilterDefinition definition : CatalogExtensions.partFilters()) {
                result.add(new FilterOption("custom:" + definition.id(), definition.title(), entry -> entry instanceof CatalogApi.PartView view && CatalogExtensions.test(definition, view)));
            }
            return result.isEmpty() ? List.of() : List.of(new FilterCategory("tinkers-parts",
                Component.translatable("filter.tinkers_construct_filter.part_category").getString(), List.copyOf(result)));
        } else if (page == Page.MATERIALS) {
            List<FilterOption> levels = new ArrayList<>();
            int minimumLevel = Integer.MAX_VALUE;
            int maximumLevel = Integer.MIN_VALUE;
            for (CatalogEntry entry : snapshot.materials()) {
                if (entry instanceof CatalogApi.MaterialView) {
                    minimumLevel = Math.min(minimumLevel, entry.getMaterialLevel());
                    maximumLevel = Math.max(maximumLevel, entry.getMaterialLevel());
                }
            }
            if (minimumLevel <= maximumLevel) {
                for (int level = minimumLevel; level <= maximumLevel; level++) {
                    int selectedLevel = level;
                    levels.add(new FilterOption("material-level:" + selectedLevel,
                        Component.translatable("filter.tinkers_construct_filter.material_level", selectedLevel).getString(),
                        entry -> entry instanceof CatalogApi.MaterialView && entry.getMaterialLevel() == selectedLevel));
                }
            }
            List<FilterCategory> categories = new ArrayList<>();
            if (!levels.isEmpty()) {
                categories.add(new FilterCategory("material-levels",
                    Component.translatable("filter.tinkers_construct_filter.material_level_category").getString(), List.copyOf(levels)));
            }
            for (CatalogExtensions.MaterialFilterDefinition definition : CatalogExtensions.materialFilters()) {
                result.add(new FilterOption("custom:" + definition.id(), definition.title(), entry -> entry instanceof CatalogApi.MaterialView view && CatalogExtensions.test(definition, view)));
            }
            if (!result.isEmpty()) {
                categories.add(new FilterCategory("custom",
                    Component.translatable("filter.tinkers_construct_filter.custom_category").getString(), List.copyOf(result)));
            }
            return List.copyOf(categories);
        } else {
            Map<String, String> slots = new LinkedHashMap<>();
            Map<String, String> tools = new LinkedHashMap<>();
            for (CatalogEntry entry : snapshot.modifiers()) {
                if (entry instanceof CatalogApi.ModifierView view) {
                    slots.putAll(view.getSlotCategories());
                    tools.putAll(view.getToolCategories());
                }
            }
            List<FilterCategory> categories = new ArrayList<>();
            for (Map.Entry<String, String> slot : slots.entrySet()) {
                result.add(new FilterOption("modifier-slot:" + slot.getKey(), slot.getValue(),
                    entry -> entry instanceof CatalogApi.ModifierView view && view.getSlotCategories().containsKey(slot.getKey())));
            }
            if (!result.isEmpty()) {
                categories.add(new FilterCategory("modifier-slots",
                    Component.translatable("filter.tinkers_construct_filter.modifier_slot_category").getString(), List.copyOf(result)));
            }
            result.clear();
            for (Map.Entry<String, String> tool : tools.entrySet()) {
                result.add(new FilterOption("modifier-tool:" + tool.getKey(), tool.getValue(),
                    entry -> entry instanceof CatalogApi.ModifierView view && view.getToolCategories().containsKey(tool.getKey())));
            }
            if (!result.isEmpty()) {
                categories.add(new FilterCategory("modifier-tools",
                    Component.translatable("filter.tinkers_construct_filter.modifier_tool_category").getString(), List.copyOf(result)));
            }
            return List.copyOf(categories);
        }
    }

    private FilterCategory activeFilterCategory(List<FilterCategory> categories) {
        String selected = selectedFilterCategories.get(page);
        for (FilterCategory category : categories) {
            if (category.id().equals(selected)) {
                return category;
            }
        }
        if (categories.isEmpty()) {
            selectedFilterCategories.remove(page);
            return null;
        }
        FilterCategory fallback = categories.get(0);
        selectedFilterCategories.put(page, fallback.id());
        return fallback;
    }

    private List<SortOption> sortOptions() {
        List<SortOption> result = new ArrayList<>();
        for (SortCategory category : sortCategories()) {
            result.addAll(category.options());
        }
        return List.copyOf(result);
    }

    private List<SortCategory> sortCategories() {
        List<SortOption> common = new ArrayList<>();
        List<SortOption> attributes = new ArrayList<>();
        List<SortOption> custom = new ArrayList<>();
        if (page == Page.MATERIALS) {
            common.add(new SortOption("default-material", Component.translatable("sort.tinkers_construct_filter.default_material"), false, null, null, true));
            common.add(new SortOption("material-name", Component.translatable("sort.tinkers_construct_filter.name"), false, null, CatalogEntry::getName, false));
            common.add(new SortOption("material-level", Component.translatable("sort.tinkers_construct_filter.material_level"), true, null, CatalogEntry::getMaterialLevel, false));
            common.add(new SortOption("material-parts", Component.translatable("sort.tinkers_construct_filter.available_part_count"), true, null,
                entry -> entry instanceof CatalogApi.MaterialView view ? view.getAvailablePartCount() : null, false));
            common.add(new SortOption("material-traits", Component.translatable("sort.tinkers_construct_filter.trait_count"), true, null,
                entry -> entry.getTraitNames().size(), false));
            addAttributeSorts(attributes);
            for (CatalogExtensions.MaterialSortDefinition definition : CatalogExtensions.materialSorts()) {
                custom.add(new SortOption("custom:" + definition.id(), Component.literal(definition.title()), null, null,
                    entry -> entry instanceof CatalogApi.MaterialView view ? CatalogExtensions.value(definition, view) : null, false));
            }
        } else if (page == Page.PARTS) {
            common.add(new SortOption("default-part", Component.translatable("sort.tinkers_construct_filter.default_part"), false, null, null, true));
            common.add(new SortOption("part-name", Component.translatable("sort.tinkers_construct_filter.part_name"), false, null, CatalogEntry::getName, false));
            common.add(new SortOption("part-type", Component.translatable("sort.tinkers_construct_filter.part_type"), false, null,
                entry -> entry instanceof CatalogApi.PartView view ? view.getPartType() : null, false));
            common.add(new SortOption("part-material-name", Component.translatable("sort.tinkers_construct_filter.material_name"), false, null,
                entry -> entry instanceof CatalogApi.PartView view ? view.getMaterialName() : null, false));
            common.add(new SortOption("part-material-level", Component.translatable("sort.tinkers_construct_filter.material_level"), true, null, CatalogEntry::getMaterialLevel, false));
            common.add(new SortOption("part-traits", Component.translatable("sort.tinkers_construct_filter.trait_count"), true, null,
                entry -> entry.getTraitNames().size(), false));
            addAttributeSorts(attributes);
            for (CatalogExtensions.PartSortDefinition definition : CatalogExtensions.partSorts()) {
                custom.add(new SortOption("custom:" + definition.id(), Component.literal(definition.title()), null, null,
                    entry -> entry instanceof CatalogApi.PartView view ? CatalogExtensions.value(definition, view) : null, false));
            }
        } else {
            common.add(new SortOption("default-modifier", Component.translatable("sort.tinkers_construct_filter.default_modifier"), false, null, null, true));
            common.add(new SortOption("modifier-name", Component.translatable("sort.tinkers_construct_filter.modifier_name"), false, null, CatalogEntry::getName, false));
            common.add(new SortOption("modifier-slot", Component.translatable("sort.tinkers_construct_filter.modifier_slot"), false, null,
                entry -> entry instanceof CatalogApi.ModifierView view ? String.join(" · ", view.getSlotCategories().values()) : null, false));
            common.add(new SortOption("modifier-tool", Component.translatable("sort.tinkers_construct_filter.modifier_tool"), false, null,
                entry -> entry instanceof CatalogApi.ModifierView view ? String.join(" · ", view.getToolCategories().values()) : null, false));
            common.add(new SortOption("modifier-descriptions", Component.translatable("sort.tinkers_construct_filter.modifier_description_count"), true, null,
                entry -> entry instanceof CatalogApi.ModifierView view ? view.getModifierDescriptions().size() : null, false));
        }
        List<SortCategory> categories = new ArrayList<>();
        if (!common.isEmpty()) {
            categories.add(new SortCategory("common", Component.translatable("sort.tinkers_construct_filter.common_category").getString(), List.copyOf(common)));
        }
        if (!attributes.isEmpty()) {
            categories.add(new SortCategory("attributes", Component.translatable("sort.tinkers_construct_filter.attribute_category").getString(), List.copyOf(attributes)));
        }
        if (!custom.isEmpty()) {
            categories.add(new SortCategory("custom", Component.translatable("sort.tinkers_construct_filter.custom_category").getString(), List.copyOf(custom)));
        }
        return List.copyOf(categories);
    }

    private void addAttributeSorts(List<SortOption> result) {
        Set<String> attributes = new LinkedHashSet<>();
        for (CatalogEntry entry : sourceEntries()) {
            attributes.addAll(entry.getAttributeValues().keySet());
        }
        for (String attribute : attributes) {
            result.add(new SortOption("attribute:" + attribute, CatalogAttributes.title(attribute), true, attribute,
                entry -> entry.getAttributeValues().get(attribute), false));
        }
    }

    private SortCategory activeSortCategory(List<SortCategory> categories) {
        String selected = selectedSortCategories.get(page);
        for (SortCategory category : categories) {
            if (category.id().equals(selected)) {
                return category;
            }
        }
        if (categories.isEmpty()) {
            selectedSortCategories.remove(page);
            return null;
        }
        String selectedSort = selectedSorts.get(page);
        for (SortCategory category : categories) {
            for (SortOption option : category.options()) {
                if (option.id().equals(selectedSort)) {
                    selectedSortCategories.put(page, category.id());
                    return category;
                }
            }
        }
        SortCategory fallback = categories.get(0);
        selectedSortCategories.put(page, fallback.id());
        return fallback;
    }

    private SortOption activeSortOption(List<SortOption> options) {
        String selected = selectedSorts.get(page);
        for (SortOption option : options) {
            if (option.id().equals(selected)) {
                return option;
            }
        }
        SortOption fallback = options.get(0);
        selectedSorts.put(page, fallback.id());
        sortAutomatic.put(page, true);
        sortDescending.put(page, false);
        updateOrderButton();
        return fallback;
    }

    private Comparator<CatalogEntry> defaultComparator() {
        if (page == Page.MATERIALS) {
            return Comparator.comparingInt(CatalogEntry::getMaterialLevel)
                .thenComparingInt(entry -> entry instanceof CatalogApi.MaterialView view ? view.getDefaultSortOrder() : Integer.MAX_VALUE)
                .thenComparing(CatalogEntry::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CatalogEntry::getId, String.CASE_INSENSITIVE_ORDER);
        }
        if (page == Page.MODIFIERS) {
            return Comparator.comparingInt(this::modifierSlotOrder)
                .thenComparing(CatalogEntry::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CatalogEntry::getId, String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing((CatalogEntry entry) -> entry instanceof CatalogApi.PartView view ? view.getPartType() : "", String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(CatalogEntry::getMaterialLevel)
            .thenComparing(entry -> entry instanceof CatalogApi.PartView view ? view.getMaterialName() : "", String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogEntry::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogEntry::getId, String.CASE_INSENSITIVE_ORDER);
    }

    private int compareSortValues(Object left, Object right, boolean descending) {
        if (left == null || right == null) {
            if (left == right) {
                return 0;
            }
            return left == null ? 1 : -1;
        }
        int value;
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            value = Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
        } else {
            value = left.toString().compareToIgnoreCase(right.toString());
        }
        return descending ? -value : value;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderBase(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!historyOpen && !filterOpen && !sortOpen && !importantOpen) {
            if (page == Page.MATERIALS && renderMaterialImportantOverlay(graphics, mouseX, mouseY)) {
                renderMaterialTraitTooltip(graphics, mouseX, mouseY);
            } else if (renderModifierRecipeOverlay(graphics, mouseX, mouseY)) {
                if (!hoveredRecipeItem.isEmpty()) {
                    renderItemTooltip(graphics, hoveredRecipeItem, mouseX, mouseY);
                }
            } else {
                renderEntryTooltip(graphics, mouseX, mouseY);
            }
        }
        if (historyOpen || filterOpen || sortOpen || importantOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z);
            if (historyOpen) {
                renderHistory(graphics, mouseX, mouseY);
            }
            if (filterOpen) {
                renderFilterPopup(graphics, mouseX, mouseY);
            }
            if (sortOpen) {
                renderSortPopup(graphics, mouseX, mouseY);
            }
            if (importantOpen) {
                renderImportantPopup(graphics, mouseX, mouseY);
            }
            graphics.pose().popPose();
        }
    }

    private void renderBase(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0x70000000);
        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF111111);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF2B2B2B);
        graphics.fill(panelX + 2, panelY + 2, panelX + NAV_WIDTH, panelY + panelHeight - 2, 0xFF202020);
        graphics.fill(panelX + NAV_WIDTH + 4, panelY + 2, panelX + panelWidth - 2, panelY + 25, 0xFF3D3D3D);
        graphics.drawCenteredString(font, title, panelX + NAV_WIDTH / 2, panelY + 10, 0xFFFFFFFF);

        int navSelectionY = switch (page) {
            case MATERIALS -> panelY + 50;
            case PARTS -> panelY + 74;
            case MODIFIERS -> panelY + 98;
        };
        graphics.fill(panelX + 5, navSelectionY, panelX + NAV_WIDTH - 5, navSelectionY + 24, 0xFF4B4B4B);
        graphics.fill(contentX, listY - 3, contentX + contentWidth, listBottom + 1, 0xFF171717);
        graphics.fill(contentX + 1, listY - 2, contentX + contentWidth - 1, listBottom, 0xFF303030);

        if (!snapshot.fullyLoaded()) {
            graphics.drawCenteredString(font, Component.translatable("screen.tinkers_construct_filter.loading"), contentX + contentWidth / 2, listY + 18, 0xFFE0E0E0);
            return;
        }

        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.result_count", visibleEntries.size()), contentX, listY - 15, 0xFFBDBDBD, false);
        renderEntries(graphics, mouseX, mouseY);
    }

    private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
        if (visibleEntries.isEmpty()) {
            return;
        }

        SortOption sort = activeSortOption(sortOptions());
        int rows = visibleRows();
        int end = Math.min(visibleEntries.size(), mainScroll + rows);
        boolean mouseOverPopup = historyOpen || filterOpen || sortOpen || importantOpen
            || isOverRecipeOverlay(mouseX, mouseY) || isOverMaterialInfoOverlay(mouseX, mouseY);
        for (int index = mainScroll; index < end; index++) {
            int rowY = listY + (index - mainScroll) * ROW_HEIGHT;
            CatalogEntry entry = visibleEntries.get(index);
            boolean hovered = !mouseOverPopup && isWithin(mouseX, mouseY, contentX + 2, rowY, contentWidth - 4, ROW_HEIGHT - 1);
            graphics.fill(contentX + 2, rowY, contentX + contentWidth - 2, rowY + ROW_HEIGHT - 1, hovered ? 0xFF4A4A4A : 0xFF383838);
            renderEntryIcon(graphics, entry, contentX + 6, rowY + 10);

            String sortText = sortValueText(entry, sort);
            int sortWidth = sortText.isEmpty() ? 0 : Math.min(contentWidth / 3, font.width(sortText));
            String name = clip(entry.getName(), contentWidth - 41 - sortWidth - 12);
            graphics.drawString(font, name, contentX + 29, rowY + 6, 0xFFFFFFFF, false);
            if (!sortText.isEmpty()) {
                graphics.drawString(font, clip(sortText, contentWidth / 3), contentX + contentWidth - 6 - sortWidth, rowY + 6, 0xFFFFD86B, false);
            }

            String traitText = entry.getTraitNames().isEmpty() ? "—" : String.join(" · ", entry.getTraitNames());
            graphics.drawString(font, clip(entrySecondaryText(entry, traitText), contentWidth - 36), contentX + 29, rowY + 21, 0xFFBFBFBF, false);
        }
        renderScrollBar(graphics, visibleEntries.size(), rows);
    }

    private void renderEntryIcon(GuiGraphics graphics, CatalogEntry entry, int x, int y) {
        ItemStack stack = entry.getDisplayStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
        }
    }

    private String entrySecondaryText(CatalogEntry entry, String fallback) {
        if (entry instanceof CatalogApi.ModifierView modifier) {
            List<String> values = new ArrayList<>(modifier.getSlotCategories().values());
            return values.isEmpty() ? "—" : String.join(" · ", values);
        }
        return fallback;
    }

    private int modifierSlotOrder(CatalogEntry entry) {
        if (!(entry instanceof CatalogApi.ModifierView modifier)) {
            return Integer.MAX_VALUE;
        }
        if (modifier.getSlotCategories().containsKey("upgrade")) {
            return 0;
        }
        if (modifier.getSlotCategories().containsKey("defense")) {
            return 1;
        }
        if (modifier.getSlotCategories().containsKey("ability")) {
            return 2;
        }
        return 3;
    }

    private void renderScrollBar(GuiGraphics graphics, int total, int rows) {
        if (total <= rows) {
            return;
        }
        int height = listBottom - listY - 2;
        int thumb = Math.max(10, height * rows / total);
        int travel = height - thumb;
        int maximum = Math.max(1, total - rows);
        int y = listY + 1 + travel * mainScroll / maximum;
        graphics.fill(contentX + contentWidth - 5, listY + 1, contentX + contentWidth - 3, listBottom - 1, 0xFF222222);
        graphics.fill(contentX + contentWidth - 5, y, contentX + contentWidth - 3, y + thumb, 0xFFB0B0B0);
    }

    private void renderHistory(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> history = ClientConfig.getSearchHistory();
        int rows = historyRows();
        historyScroll = clamp(historyScroll, 0, Math.max(0, history.size() - rows));
        int x = historyX();
        int y = historyY();
        int width = historyWidth();
        int height = 18 + rows * POPUP_ROW_HEIGHT;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF0F0F0F);
        graphics.fill(x, y, x + width, y + height, 0xFF252525);
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.history"), x + 5, y + 5, 0xFFFFFFFF, false);
        for (int row = 0; row < rows; row++) {
            int rowY = y + 18 + row * POPUP_ROW_HEIGHT;
            int index = historyScroll + row;
            boolean hovered = isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
            graphics.fill(x + 1, rowY, x + width - 1, rowY + POPUP_ROW_HEIGHT, hovered ? 0xFF505050 : 0xFF383838);
            String value = index < history.size() ? history.get(index) : "";
            graphics.drawString(font, clip(value, width - 10), x + 5, rowY + 5, value.isEmpty() ? 0xFF777777 : 0xFFE5E5E5, false);
        }
    }

    private void renderImportantPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ImportantPartOption> options = importantPartOptions();
        syncImportantSelection();
        int rows = importantRows(options.size());
        importantScroll = clamp(importantScroll, 0, Math.max(0, options.size() - rows));
        int x = importantPopupX();
        int y = importantPopupY();
        int width = importantPopupWidth();
        int height = importantPopupHeight(options.size());
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF0F0F0F);
        graphics.fill(x, y, x + width, y + height, 0xFF252525);
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.important_options"), x + 5, y + 5, 0xFFFFFFFF, false);
        String selectAll = Component.translatable("screen.tinkers_construct_filter.select_all").getString();
        String clear = Component.translatable("screen.tinkers_construct_filter.clear").getString();
        graphics.drawString(font, selectAll, importantSelectAllX(), y + 5, 0xFFFFD86B, false);
        graphics.drawString(font, clear, importantClearX(), y + 5, 0xFFFFD86B, false);
        graphics.fill(x + 1, y + 17, x + width - 1, y + 18, 0xFF444444);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_important_options").getString(), width - 10), x + 5, y + 25, 0xFFE0E0E0, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = importantScroll + row;
            if (index >= options.size()) {
                break;
            }
            int rowY = importantOptionsY() + row * POPUP_ROW_HEIGHT;
            ImportantPartOption option = options.get(index);
            boolean selected = selectedImportantPartTypes.contains(option.id());
            boolean hovered = isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
            graphics.fill(x + 1, rowY, x + width - 1, rowY + POPUP_ROW_HEIGHT, hovered ? 0xFF505050 : 0xFF383838);
            graphics.drawString(font, clip((selected ? "[√] " : "[] ") + option.title(), width - 10), x + 5, rowY + 5,
                selected ? 0xFFFFD86B : 0xFFE0E0E0, false);
        }
        renderPopupScrollBar(graphics, x, y + 18, width, rows, options.size(), importantScroll, height - 19);
    }

    private void renderFilterPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FilterCategory> categories = filterCategories();
        FilterCategory activeCategory = activeFilterCategory(categories);
        List<FilterOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int rows = filterRows();
        filterScroll = clamp(filterScroll, 0, Math.max(0, options.size() - rows));
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = filterPopupHeight();
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF0F0F0F);
        graphics.fill(x, y, x + width, y + height, 0xFF252525);
        renderFilterCategoryBar(graphics, categories, activeCategory, mouseX, mouseY);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_material_filters").getString(), width - 10), x + 5, filterOptionsY() + 7, 0xFFE0E0E0, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = filterScroll + row;
            if (index >= options.size()) {
                break;
            }
            int rowY = filterOptionsY() + row * POPUP_ROW_HEIGHT;
            FilterOption option = options.get(index);
            boolean selected = selectedFilters.get(page).contains(option.id());
            boolean hovered = isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
            graphics.fill(x + 1, rowY, x + width - 1, rowY + POPUP_ROW_HEIGHT, hovered ? 0xFF505050 : 0xFF383838);
            graphics.drawString(font, clip((selected ? "[√] " : "[] ") + option.title(), width - 10), x + 5, rowY + 5, selected ? 0xFFFFD86B : 0xFFE0E0E0, false);
        }
    }

    private void renderFilterCategoryBar(GuiGraphics graphics, List<FilterCategory> categories, FilterCategory activeCategory, int mouseX, int mouseY) {
        int x = filterCategoryAreaX();
        int y = popupY() + 2;
        int width = filterCategoryAreaWidth();
        int height = CATEGORY_BAR_HEIGHT - 4;
        graphics.fill(popupX() + 1, popupY() + CATEGORY_BAR_HEIGHT - 1, popupX() + popupWidth() - 1, popupY() + CATEGORY_BAR_HEIGHT, 0xFF444444);
        clampFilterCategoryScroll(categories);
        graphics.enableScissor(x, y, x + width, y + height);
        int buttonX = x - filterCategoryScroll;
        for (FilterCategory category : categories) {
            int buttonWidth = filterCategoryButtonWidth(category);
            boolean selected = activeCategory != null && activeCategory.id().equals(category.id());
            boolean hovered = isWithin(mouseX, mouseY, buttonX, y, buttonWidth, height);
            graphics.fill(buttonX, y, buttonX + buttonWidth, y + height, selected ? 0xFF6A6A6A : hovered ? 0xFF505050 : 0xFF383838);
            graphics.renderOutline(buttonX, y, buttonWidth, height, selected ? 0xFFE0E0E0 : 0xFF151515);
            graphics.drawCenteredString(font, category.title(), buttonX + buttonWidth / 2, y + 4, selected ? 0xFFFFFFFF : 0xFFD0D0D0);
            buttonX += buttonWidth + CATEGORY_GAP;
        }
        graphics.disableScissor();
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.clear"), clearFilterX() + 4, popupY() + 6, 0xFFFFD86B, false);
    }

    private void renderSortPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        List<SortCategory> categories = sortCategories();
        SortCategory activeCategory = activeSortCategory(categories);
        List<SortOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int rows = sortRows();
        sortScroll = clamp(sortScroll, 0, Math.max(0, options.size() - rows));
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = sortPopupHeight();
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF0F0F0F);
        graphics.fill(x, y, x + width, y + height, 0xFF252525);
        renderSortCategoryBar(graphics, categories, activeCategory, mouseX, mouseY);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_sort_options").getString(), width - 10), x + 5, sortOptionsY() + 7, 0xFFE0E0E0, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = sortScroll + row;
            if (index >= options.size()) {
                break;
            }
            int rowY = sortOptionsY() + row * POPUP_ROW_HEIGHT;
            SortOption option = options.get(index);
            boolean selected = selectedSorts.get(page).equals(option.id());
            boolean hovered = isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
            graphics.fill(x + 1, rowY, x + width - 1, rowY + POPUP_ROW_HEIGHT, hovered ? 0xFF505050 : 0xFF383838);
            graphics.drawString(font, clip((selected ? "[√] " : "[] ") + option.title().getString(), width - 10), x + 5, rowY + 5, selected ? 0xFFFFD86B : 0xFFE0E0E0, false);
        }
    }

    private void renderSortCategoryBar(GuiGraphics graphics, List<SortCategory> categories, SortCategory activeCategory, int mouseX, int mouseY) {
        int x = sortCategoryAreaX();
        int y = popupY() + 2;
        int width = sortCategoryAreaWidth();
        int height = CATEGORY_BAR_HEIGHT - 4;
        graphics.fill(popupX() + 1, popupY() + CATEGORY_BAR_HEIGHT - 1, popupX() + popupWidth() - 1, popupY() + CATEGORY_BAR_HEIGHT, 0xFF444444);
        clampSortCategoryScroll(categories);
        graphics.enableScissor(x, y, x + width, y + height);
        int buttonX = x - sortCategoryScroll;
        for (SortCategory category : categories) {
            int buttonWidth = sortCategoryButtonWidth(category);
            boolean selected = activeCategory != null && activeCategory.id().equals(category.id());
            boolean hovered = isWithin(mouseX, mouseY, buttonX, y, buttonWidth, height);
            graphics.fill(buttonX, y, buttonX + buttonWidth, y + height, selected ? 0xFF6A6A6A : hovered ? 0xFF505050 : 0xFF383838);
            graphics.renderOutline(buttonX, y, buttonWidth, height, selected ? 0xFFE0E0E0 : 0xFF151515);
            graphics.drawCenteredString(font, category.title(), buttonX + buttonWidth / 2, y + 4, selected ? 0xFFFFFFFF : 0xFFD0D0D0);
            buttonX += buttonWidth + CATEGORY_GAP;
        }
        graphics.disableScissor();
    }

    private void renderEntryTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        CatalogEntry entry = entryAt(mouseX, mouseY);
        if (entry == null) {
            return;
        }
        ItemStack stack = entry.getDisplayStack();
        if (!stack.isEmpty()) {
            graphics.renderTooltip(font, reorderTraitTooltip(entry, stack), stack.getTooltipImage(), stack, mouseX, mouseY);
        }
    }

    private boolean renderMaterialImportantOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        hoveredMaterialTrait = null;
        if (!snapshot.fullyLoaded() || page != Page.MATERIALS || selectedImportantPartTypes.isEmpty()) {
            clearMaterialInfoOverlay();
            return false;
        }

        if (!materialInfoLocked && !isOverMaterialInfoOverlay(mouseX, mouseY)) {
            CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
            if (!(rowEntry instanceof CatalogApi.MaterialView material)) {
                clearMaterialInfoOverlay();
                return false;
            }
            if (materialInfoEntry != material) {
                materialInfoEntry = material;
                materialInfoParts = selectedMaterialParts(material);
                materialInfoScroll = 0;
            }
        }
        if (materialInfoEntry == null) {
            return false;
        }

        int entryIndex = visibleEntries.indexOf(materialInfoEntry);
        if (entryIndex < mainScroll || entryIndex >= Math.min(visibleEntries.size(), mainScroll + visibleRows())) {
            clearMaterialInfoOverlay();
            return false;
        }

        List<MaterialInfoLine> lines = buildMaterialInfoLines(materialInfoParts);
        materialInfoWidth = importantInfoWidth();
        materialInfoContentHeight = Math.max(MATERIAL_INFO_LINE_HEIGHT, lines.size() * MATERIAL_INFO_LINE_HEIGHT);
        int maximumHeight = Math.max(MATERIAL_INFO_MIN_HEIGHT, listBottom - listY - 4);
        materialInfoHeight = Math.min(maximumHeight, materialInfoContentHeight + 8);
        materialInfoViewportHeight = Math.max(1, materialInfoHeight - 8);
        materialInfoScroll = clamp(materialInfoScroll, 0, Math.max(0, materialInfoContentHeight - materialInfoViewportHeight));
        materialInfoX = contentX + contentWidth - materialInfoWidth - 8;
        int rowY = listY + (entryIndex - mainScroll) * ROW_HEIGHT;
        int belowY = rowY + ROW_HEIGHT + 2;
        int aboveY = rowY - materialInfoHeight - 2;
        int minimumY = listY;
        int maximumY = Math.max(minimumY, listBottom - materialInfoHeight);
        materialInfoY = belowY + materialInfoHeight <= listBottom ? belowY : aboveY;
        materialInfoY = clamp(materialInfoY, minimumY, maximumY);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z - 1);
        graphics.fill(materialInfoX - 1, materialInfoY - 1, materialInfoX + materialInfoWidth + 1, materialInfoY + materialInfoHeight + 1, 0xFF0F0714);
        graphics.fill(materialInfoX, materialInfoY, materialInfoX + materialInfoWidth, materialInfoY + materialInfoHeight, 0xFF24152A);
        graphics.renderOutline(materialInfoX, materialInfoY, materialInfoWidth, materialInfoHeight, 0xFF5E2A85);
        int contentTop = materialInfoY + 4;
        int contentBottom = materialInfoY + materialInfoHeight - 4;
        graphics.enableScissor(materialInfoX + 4, contentTop, materialInfoX + materialInfoWidth - 4, contentBottom);
        for (int index = 0; index < lines.size(); index++) {
            MaterialInfoLine line = lines.get(index);
            int lineY = contentTop + index * MATERIAL_INFO_LINE_HEIGHT - materialInfoScroll;
            if (lineY + MATERIAL_INFO_LINE_HEIGHT <= contentTop || lineY >= contentBottom) {
                continue;
            }
            String text = clip(line.text(), materialInfoWidth - 14);
            MutableComponent component = Component.literal(text);
            if (line.trait() != null) {
                component = component.withStyle(style -> style.withUnderlined(true));
                if (isWithin(mouseX, mouseY, materialInfoX + 5, lineY, font.width(text), MATERIAL_INFO_LINE_HEIGHT)) {
                    hoveredMaterialTrait = line.trait();
                }
            }
            graphics.drawString(font, component, materialInfoX + 5, lineY, line.color(), false);
        }
        graphics.disableScissor();
        renderPopupScrollBar(graphics, materialInfoX, materialInfoY + 4, materialInfoWidth, materialInfoViewportHeight,
            materialInfoContentHeight, materialInfoScroll, materialInfoHeight - 8);
        graphics.pose().popPose();
        return true;
    }

    private void renderMaterialTraitTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredMaterialTrait == null) {
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.literal(hoveredMaterialTrait.name()).withStyle(style -> style.withUnderlined(true)), 320));
        for (String description : hoveredMaterialTrait.descriptions()) {
            lines.addAll(font.split(Component.literal(description), 320));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 1);
        graphics.renderTooltip(font, lines, mouseX, mouseY);
        graphics.pose().popPose();
    }

    private List<MaterialInfoLine> buildMaterialInfoLines(List<CatalogApi.PartView> parts) {
        List<MaterialInfoLine> lines = new ArrayList<>();
        lines.add(new MaterialInfoLine(Component.translatable("screen.tinkers_construct_filter.material_info_hint").getString(), 0xFFBFBFBF, null));
        lines.add(new MaterialInfoLine(Component.translatable("screen.tinkers_construct_filter.material_info_subhint").getString(), 0xFFA8A8A8, null));
        if (parts.isEmpty()) {
            lines.add(new MaterialInfoLine(Component.translatable("screen.tinkers_construct_filter.no_matching_parts").getString(), 0xFFE0E0E0, null));
            return lines;
        }
        for (int index = 0; index < parts.size(); index++) {
            CatalogApi.PartView part = parts.get(index);
            String partTitle = part.getPartTypeName().isEmpty() ? part.getPartType() : part.getPartTypeName();
            lines.add(new MaterialInfoLine(partTitle, importantPartColor(index), null));
            if (part.getAttributeTexts().isEmpty()) {
                lines.add(new MaterialInfoLine(Component.translatable("screen.tinkers_construct_filter.no_attributes").getString(), 0xFFE0E0E0, null));
            } else {
                for (Map.Entry<String, String> attribute : part.getAttributeTexts().entrySet()) {
                    String title = importantAttributeTitle(attribute.getKey());
                    lines.add(new MaterialInfoLine(Component.translatable("screen.tinkers_construct_filter.material_attribute", title, attribute.getValue()).getString(), 0xFFE0E0E0, null));
                }
            }
            lines.add(new MaterialInfoLine(Component.translatable("screen.tinkers_construct_filter.traits").getString(), 0xFFD0D0D0, null));
            if (part instanceof CatalogEntry entry) {
                for (CatalogEntry.TraitTooltip trait : entry.getTraitTooltips()) {
                    lines.add(new MaterialInfoLine("  " + trait.name(), 0xFFE0E0E0, trait));
                }
            }
            if (index + 1 < parts.size()) {
                lines.add(new MaterialInfoLine("", 0xFFE0E0E0, null));
            }
        }
        return List.copyOf(lines);
    }

    private List<CatalogApi.PartView> selectedMaterialParts(CatalogApi.MaterialView material) {
        Map<String, CatalogApi.PartView> available = new LinkedHashMap<>();
        for (CatalogEntry entry : snapshot.parts()) {
            if (entry instanceof CatalogApi.PartView part && material.getId().equals(part.getMaterialId())) {
                available.putIfAbsent(part.getPartType(), part);
            }
        }
        List<CatalogApi.PartView> result = new ArrayList<>();
        for (ImportantPartOption option : importantPartOptions()) {
            if (selectedImportantPartTypes.contains(option.id()) && available.containsKey(option.id())) {
                result.add(available.get(option.id()));
            }
        }
        return List.copyOf(result);
    }

    private int importantPartColor(int index) {
        return switch (index % 6) {
            case 0 -> 0xFFFFB347;
            case 1 -> 0xFF70D070;
            case 2 -> 0xFF70C8FF;
            case 3 -> 0xFFFF78C8;
            case 4 -> 0xFFD0A0FF;
            default -> 0xFFFFD86B;
        };
    }

    private String importantAttributeTitle(String id) {
        String title = CatalogAttributes.title(id).getString();
        if (id.startsWith("head_")) {
            return removeAttributePrefix(title, "头部", "Head ");
        }
        if (id.startsWith("handle_")) {
            return removeAttributePrefix(title, "手柄", "Handle ");
        }
        if (id.startsWith("limb_")) {
            return removeAttributePrefix(title, "远程肢体", "Limb ");
        }
        if (id.startsWith("grip_")) {
            return removeAttributePrefix(title, "握把", "Grip ");
        }
        if (id.startsWith("plating_")) {
            return removeAttributePrefix(title, "护甲板", "Plating ");
        }
        return title;
    }

    private String removeAttributePrefix(String title, String chinesePrefix, String englishPrefix) {
        if (title.startsWith(chinesePrefix)) {
            return title.substring(chinesePrefix.length());
        }
        if (title.startsWith(englishPrefix)) {
            return title.substring(englishPrefix.length());
        }
        return title;
    }

    private boolean renderModifierRecipeOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        hoveredRecipeItem = ItemStack.EMPTY;
        if (isOverOpenPopup(mouseX, mouseY)) {
            clearRecipeOverlay();
            return false;
        }

        if (!isOverRecipeOverlay(mouseX, mouseY)) {
            CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
            if (!recipeOverlayLocked && rowEntry instanceof CatalogApi.ModifierView modifier
                && (!modifier.getModifierDescriptions().isEmpty() || !modifier.getRecipeInputs().isEmpty() || !modifier.getRecipeTools().isEmpty())) {
                if (recipeOverlayEntry != modifier) {
                    recipeOverlayScroll = 0;
                }
                recipeOverlayEntry = modifier;
                recipeOverlayTools = modifier.getRecipeTools();
                recipeOverlayItems = flattenRecipeInputs(modifier);
            } else if (!recipeOverlayLocked) {
                clearRecipeOverlay();
                return false;
            }
        }

        if (recipeOverlayEntry == null || (recipeOverlayTools.isEmpty() && recipeOverlayItems.isEmpty() && recipeOverlayEntry.getModifierDescriptions().isEmpty())) {
            clearRecipeOverlay();
            return false;
        }

        int entryIndex = visibleEntries.indexOf(recipeOverlayEntry);
        if (entryIndex < mainScroll || entryIndex >= Math.min(visibleEntries.size(), mainScroll + visibleRows())) {
            clearRecipeOverlay();
            return false;
        }

        recipeOverlayWidth = Math.min(260, Math.max(96, contentWidth - 12));
        List<String> descriptionLines = wrapModifierDescriptions(recipeOverlayEntry.getModifierDescriptions(), recipeOverlayWidth - 10);
        int columns = Math.max(1, (recipeOverlayWidth - 10) / 18);
        int toolRows = Math.max(1, (recipeOverlayTools.size() + columns - 1) / columns);
        int inputRows = Math.max(1, (recipeOverlayItems.size() + columns - 1) / columns);
        int instructionHeight = 16;
        int descriptionTitleHeight = descriptionLines.isEmpty() ? 0 : 16;
        int sectionTitleHeight = 16;
        int sectionGap = 2;
        int itemRowHeight = 20;
        recipeOverlayContentHeight = instructionHeight + descriptionTitleHeight + descriptionLines.size() * RECIPE_OVERLAY_LINE_HEIGHT
            + sectionTitleHeight + inputRows * itemRowHeight + sectionGap
            + sectionTitleHeight + toolRows * itemRowHeight;
        int fullHeight = recipeOverlayContentHeight + 10;
        int maximumHeight = Math.max(40, listBottom - listY - 4);
        recipeOverlayHeight = Math.min(fullHeight, maximumHeight);
        recipeOverlayViewportHeight = Math.max(1, recipeOverlayHeight - 10);
        recipeOverlayScroll = clamp(recipeOverlayScroll, 0,
            Math.max(0, recipeOverlayContentHeight - recipeOverlayViewportHeight));
        recipeOverlayX = contentX + contentWidth - recipeOverlayWidth - 8;
        int rowY = listY + (entryIndex - mainScroll) * ROW_HEIGHT;
        int belowY = rowY + ROW_HEIGHT + 2;
        int aboveY = rowY - recipeOverlayHeight - 2;
        int minimumY = listY;
        int maximumY = Math.max(minimumY, listBottom - recipeOverlayHeight);
        recipeOverlayY = belowY + recipeOverlayHeight <= listBottom ? belowY : aboveY;
        recipeOverlayY = clamp(recipeOverlayY, minimumY, maximumY);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z - 1);
        graphics.fill(recipeOverlayX - 1, recipeOverlayY - 1, recipeOverlayX + recipeOverlayWidth + 1, recipeOverlayY + recipeOverlayHeight + 1, 0xFF0F0714);
        graphics.fill(recipeOverlayX, recipeOverlayY, recipeOverlayX + recipeOverlayWidth, recipeOverlayY + recipeOverlayHeight, 0xFF24152A);
        graphics.renderOutline(recipeOverlayX, recipeOverlayY, recipeOverlayWidth, recipeOverlayHeight, 0xFF5E2A85);
        int contentTop = recipeOverlayY + 5;
        int contentBottom = recipeOverlayY + recipeOverlayHeight - 5;
        graphics.enableScissor(recipeOverlayX + 4, contentTop - 1, recipeOverlayX + recipeOverlayWidth - 4, contentBottom);
        int contentY = contentTop - recipeOverlayScroll;
        graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.recipe_hint").getString(), recipeOverlayWidth - 10), recipeOverlayX + 5, contentY, 0xFFBFBFBF, false);
        int inputTitleY = contentY + instructionHeight;
        if (!descriptionLines.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.modifier_description"), recipeOverlayX + 5, inputTitleY, 0xFFFFFFFF, false);
            int descriptionY = inputTitleY + descriptionTitleHeight;
            for (String description : descriptionLines) {
                graphics.drawString(font, description, recipeOverlayX + 5, descriptionY, 0xFFE0E0E0, false);
                descriptionY += RECIPE_OVERLAY_LINE_HEIGHT;
            }
            inputTitleY += descriptionTitleHeight + descriptionLines.size() * RECIPE_OVERLAY_LINE_HEIGHT;
        }
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.recipe_inputs"), recipeOverlayX + 5, inputTitleY, 0xFFFFFFFF, false);
        int inputItemsY = inputTitleY + sectionTitleHeight;
        drawRecipeItems(graphics, recipeOverlayItems, inputItemsY, columns, mouseX, mouseY);
        int toolsTitleY = inputItemsY + inputRows * itemRowHeight + sectionGap;
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.recipe_tools"), recipeOverlayX + 5, toolsTitleY, 0xFFFFFFFF, false);
        drawRecipeItems(graphics, recipeOverlayTools, toolsTitleY + sectionTitleHeight, columns, mouseX, mouseY);
        graphics.disableScissor();
        renderPopupScrollBar(graphics, recipeOverlayX, contentTop, recipeOverlayWidth, recipeOverlayViewportHeight,
            recipeOverlayContentHeight, recipeOverlayScroll, recipeOverlayViewportHeight);
        graphics.pose().popPose();
        return true;
    }

    private List<String> wrapModifierDescriptions(List<String> descriptions, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String description : descriptions) {
            for (String paragraph : description.split("\\R", -1)) {
                String remaining = paragraph;
                while (!remaining.isEmpty()) {
                    String line = font.plainSubstrByWidth(remaining, maxWidth);
                    if (line.isEmpty()) {
                        break;
                    }
                    result.add(line);
                    remaining = remaining.substring(line.length()).stripLeading();
                }
            }
        }
        return List.copyOf(result);
    }

    private void drawRecipeItems(GuiGraphics graphics, List<ItemStack> stacks, int itemY, int columns, int mouseX, int mouseY) {
        if (stacks.isEmpty()) {
            graphics.drawString(font, "—", recipeOverlayX + 5, itemY + 2, 0xFFBFBFBF, false);
            return;
        }
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            int itemX = recipeOverlayX + 5 + (index % columns) * 18;
            int stackY = itemY + (index / columns) * 20;
            drawRecipeItem(graphics, stack, itemX, stackY, mouseX, mouseY);
        }
    }

    private void drawRecipeItem(GuiGraphics graphics, ItemStack stack, int itemX, int itemY, int mouseX, int mouseY) {
            boolean hovered = isWithin(mouseX, mouseY, itemX - 1, itemY - 1, 18, 18);
            graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, hovered ? 0xFF6A4A72 : 0xFF3A2B40);
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
            if (hovered) {
                hoveredRecipeItem = stack;
            }
    }

    private List<ItemStack> flattenRecipeInputs(CatalogApi.ModifierView modifier) {
        List<ItemStack> result = new ArrayList<>();
        for (List<ItemStack> inputs : modifier.getRecipeInputs()) {
            for (ItemStack stack : inputs) {
                if (!stack.isEmpty()) {
                    result.add(stack);
                }
            }
        }
        return List.copyOf(result);
    }

    private void renderItemTooltip(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        graphics.renderTooltip(font, stack.getTooltipLines(minecraft.player, flag), stack.getTooltipImage(), stack, mouseX, mouseY);
    }

    private List<Component> reorderTraitTooltip(CatalogEntry entry, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> source = stack.getTooltipLines(minecraft.player, flag);
        List<CatalogEntry.TraitTooltip> traits = entry.getTraitTooltips();
        if (traits.isEmpty()) {
            return source;
        }

        Set<Integer> usedLines = new HashSet<>();
        List<List<Integer>> traitLines = new ArrayList<>();
        int insertionIndex = source.size();
        for (CatalogEntry.TraitTooltip trait : traits) {
            int nameLine = findTraitNameLine(source, trait.name(), usedLines);
            if (nameLine < 0) {
                return source;
            }

            List<Integer> lines = new ArrayList<>();
            lines.add(nameLine);
            usedLines.add(nameLine);
            insertionIndex = Math.min(insertionIndex, nameLine);
            for (String description : trait.descriptions()) {
                int descriptionLine = findExactTooltipLine(source, description, usedLines);
                if (descriptionLine < 0) {
                    return source;
                }
                lines.add(descriptionLine);
                usedLines.add(descriptionLine);
            }
            traitLines.add(lines);
        }

        List<Component> ordered = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            if (index == insertionIndex) {
                for (List<Integer> lines : traitLines) {
                    for (int line : lines) {
                        ordered.add(source.get(line));
                    }
                }
            }
            if (!usedLines.contains(index)) {
                ordered.add(source.get(index));
            }
        }
        return ordered;
    }

    private int findTraitNameLine(List<Component> lines, String name, Set<Integer> usedLines) {
        for (int index = 0; index < lines.size(); index++) {
            if (usedLines.contains(index)) {
                continue;
            }
            String text = lines.get(index).getString();
            if (text.equals(name) || text.endsWith(name) && text.length() <= name.length() + 4) {
                return index;
            }
        }
        return -1;
    }

    private int findExactTooltipLine(List<Component> lines, String expected, Set<Integer> usedLines) {
        for (int index = 0; index < lines.size(); index++) {
            if (!usedLines.contains(index) && lines.get(index).getString().equals(expected)) {
                return index;
            }
        }
        return -1;
    }

    private String sortValueText(CatalogEntry entry, SortOption sort) {
        if (sort.defaultOrder()) {
            if (page == Page.MATERIALS && entry instanceof CatalogApi.MaterialView material) {
                return "L" + entry.getMaterialLevel() + " / " + material.getDefaultSortOrder();
            }
            if (entry instanceof CatalogApi.PartView part) {
                return part.getPartTypeName() + " / L" + entry.getMaterialLevel();
            }
            if (entry instanceof CatalogApi.ModifierView modifier) {
                return String.join(" · ", modifier.getSlotCategories().values());
            }
            return "";
        }
        Object value = activeSortValues.get(entry);
        if (value == null) {
            return "—";
        }
        String display = sort.attributeId() == null
            ? value.toString()
            : entry.getAttributeTexts().getOrDefault(sort.attributeId(), CatalogAttributes.format(sort.attributeId(), ((Number) value).doubleValue()));
        return Component.translatable("screen.tinkers_construct_filter.sort_value", sort.title().getString(), display).getString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (importantOpen) {
            if (importantButton != null && isWithin(mouseX, mouseY, importantButton.getX(), importantButton.getY(), importantButton.getWidth(), importantButton.getHeight())) {
                toggleImportantPopup();
                return true;
            }
            if (isWithin(mouseX, mouseY, importantPopupX(), importantPopupY(), importantPopupWidth(), importantPopupHeight(importantPartOptions().size()))) {
                handleImportantClick(mouseX, mouseY);
            } else {
                importantOpen = false;
                importantScroll = 0;
            }
            return true;
        }
        if (historyOpen) {
            if (isWithin(mouseX, mouseY, historyX(), historyY(), historyWidth(), 18 + historyRows() * POPUP_ROW_HEIGHT)) {
                handleHistoryClick(mouseX, mouseY);
            } else {
                historyOpen = false;
                historyScroll = 0;
            }
            return true;
        }
        if (filterOpen) {
            if (isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), filterPopupHeight())) {
                handleFilterClick(mouseX, mouseY);
            } else {
                filterOpen = false;
            }
            return true;
        }
        if (sortOpen) {
            if (isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), sortPopupHeight())) {
                handleSortClick(mouseX, mouseY);
            } else {
                sortOpen = false;
            }
            return true;
        }
        if (isOverRecipeOverlay(mouseX, mouseY)) {
            return true;
        }
        if (isOverMaterialInfoOverlay(mouseX, mouseY)) {
            materialInfoLocked = true;
            if (materialInfoEntry != null) {
                TinkersConstructFilter.LOGGER.debug("Material information overlay locked by popup click: {}", materialInfoEntry.getId());
            }
            return true;
        }

        CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
        if (page == Page.MATERIALS && rowEntry instanceof CatalogApi.MaterialView material && !selectedImportantPartTypes.isEmpty()) {
            materialInfoEntry = material;
            materialInfoParts = selectedMaterialParts(material);
            materialInfoScroll = 0;
            materialInfoLocked = true;
            TinkersConstructFilter.LOGGER.debug("Material information overlay locked: {}", material.getId());
            return true;
        }
        if (rowEntry instanceof CatalogApi.ModifierView modifier) {
            if (!modifier.getModifierDescriptions().isEmpty() || !modifier.getRecipeInputs().isEmpty() || !modifier.getRecipeTools().isEmpty()) {
                recipeOverlayEntry = modifier;
                recipeOverlayTools = modifier.getRecipeTools();
                recipeOverlayItems = flattenRecipeInputs(modifier);
                recipeOverlayLocked = true;
                TinkersConstructFilter.LOGGER.debug("Modifier recipe overlay locked: {}", modifier.getId());
            } else {
                clearRecipeOverlay();
            }
            return true;
        }

        historyOpen = false;
        filterOpen = false;
        sortOpen = false;
        importantOpen = false;
        if (searchBox != null && isWithin(mouseX, mouseY, searchBox.getX(), searchBox.getY(), searchBox.getWidth(), searchBox.getHeight())) {
            historyOpen = true;
            clearRecipeOverlay();
            clearMaterialInfoOverlay();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleHistoryClick(double mouseX, double mouseY) {
        int row = (int) ((mouseY - historyY() - 18) / POPUP_ROW_HEIGHT);
        if (row < 0 || row >= historyRows()) {
            return;
        }
        List<String> history = ClientConfig.getSearchHistory();
        int index = historyScroll + row;
        if (index >= 0 && index < history.size()) {
            searchBox.setValue(history.get(index));
            ClientConfig.rememberSearch(history.get(index));
            historyOpen = false;
            historyScroll = 0;
        }
    }

    private void handleImportantClick(double mouseX, double mouseY) {
        List<ImportantPartOption> options = importantPartOptions();
        if (isWithinImportantHeader(mouseX, mouseY)) {
            if (mouseX >= importantSelectAllX() && mouseX < importantSelectAllX() + importantSelectAllWidth()) {
                selectedImportantPartTypes.clear();
                for (ImportantPartOption option : options) {
                    selectedImportantPartTypes.add(option.id());
                }
                materialInfoScroll = 0;
            } else if (mouseX >= importantClearX() && mouseX < importantClearX() + importantClearWidth()) {
                selectedImportantPartTypes.clear();
                materialInfoScroll = 0;
            }
            return;
        }
        int row = (int) ((mouseY - importantOptionsY()) / POPUP_ROW_HEIGHT);
        int index = importantScroll + row;
        if (index >= 0 && index < options.size()) {
            String id = options.get(index).id();
            if (!selectedImportantPartTypes.add(id)) {
                selectedImportantPartTypes.remove(id);
            }
            materialInfoScroll = 0;
        }
    }

    private void handleFilterClick(double mouseX, double mouseY) {
        List<FilterCategory> categories = filterCategories();
        if (isWithinFilterCategoryBar(mouseX, mouseY)) {
            if (mouseX >= clearFilterX()) {
                selectedFilters.get(page).clear();
                rebuildVisibleEntries();
            } else {
                FilterCategory category = filterCategoryAt(mouseX, mouseY, categories);
                if (category != null) {
                    selectedFilterCategories.put(page, category.id());
                    filterScroll = 0;
                }
            }
            return;
        }
        FilterCategory activeCategory = activeFilterCategory(categories);
        List<FilterOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int row = (int) ((mouseY - filterOptionsY()) / POPUP_ROW_HEIGHT);
        int index = filterScroll + row;
        if (index >= 0 && index < options.size()) {
            String id = options.get(index).id();
            if (!selectedFilters.get(page).add(id)) {
                selectedFilters.get(page).remove(id);
            }
            rebuildVisibleEntries();
        }
    }

    private void handleSortClick(double mouseX, double mouseY) {
        List<SortCategory> categories = sortCategories();
        if (isWithinSortCategoryBar(mouseX, mouseY)) {
            SortCategory category = sortCategoryAt(mouseX, mouseY, categories);
            if (category != null) {
                selectedSortCategories.put(page, category.id());
                sortScroll = 0;
            }
            return;
        }
        SortCategory activeCategory = activeSortCategory(categories);
        List<SortOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int row = (int) ((mouseY - sortOptionsY()) / POPUP_ROW_HEIGHT);
        int index = sortScroll + row;
        if (index >= 0 && index < options.size()) {
            SortOption option = options.get(index);
            selectedSorts.put(page, option.id());
            selectedSortCategories.put(page, activeCategory.id());
            sortAutomatic.put(page, true);
            sortDescending.put(page, option.numeric() != null && option.numeric());
            rebuildVisibleEntries();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int direction = delta > 0 ? -1 : 1;
        if (historyOpen) {
            if (isWithin(mouseX, mouseY, historyX(), historyY(), historyWidth(), 18 + historyRows() * POPUP_ROW_HEIGHT)) {
                int max = Math.max(0, ClientConfig.getSearchHistory().size() - historyRows());
                historyScroll = clamp(historyScroll + direction, 0, max);
            }
            return true;
        }
        if (filterOpen) {
            if (isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), filterPopupHeight())) {
                List<FilterCategory> categories = filterCategories();
                if (isWithinFilterCategoryBar(mouseX, mouseY)) {
                    if (mouseX < clearFilterX()) {
                        int maximum = Math.max(0, filterCategoryContentWidth(categories) - filterCategoryAreaWidth());
                        filterCategoryScroll = clamp(filterCategoryScroll + direction * 18, 0, maximum);
                    }
                    return true;
                }
                FilterCategory activeCategory = activeFilterCategory(categories);
                int optionCount = activeCategory == null ? 0 : activeCategory.options().size();
                filterScroll = clamp(filterScroll + direction, 0, Math.max(0, optionCount - filterRows()));
            }
            return true;
        }
        if (sortOpen) {
            if (isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), sortPopupHeight())) {
                List<SortCategory> categories = sortCategories();
                if (isWithinSortCategoryBar(mouseX, mouseY)) {
                    int maximum = Math.max(0, sortCategoryContentWidth(categories) - sortCategoryAreaWidth());
                    sortCategoryScroll = clamp(sortCategoryScroll + direction * 18, 0, maximum);
                    return true;
                }
                SortCategory activeCategory = activeSortCategory(categories);
                int optionCount = activeCategory == null ? 0 : activeCategory.options().size();
                sortScroll = clamp(sortScroll + direction, 0, Math.max(0, optionCount - sortRows()));
            }
            return true;
        }
        if (importantOpen) {
            if (isWithin(mouseX, mouseY, importantPopupX(), importantPopupY(), importantPopupWidth(), importantPopupHeight(importantPartOptions().size()))) {
                int maximum = Math.max(0, importantPartOptions().size() - importantRows(importantPartOptions().size()));
                importantScroll = clamp(importantScroll + direction, 0, maximum);
            }
            return true;
        }
        if (isOverRecipeOverlay(mouseX, mouseY)) {
            recipeOverlayScroll = clamp(recipeOverlayScroll + direction * RECIPE_OVERLAY_LINE_HEIGHT, 0,
                Math.max(0, recipeOverlayContentHeight - recipeOverlayViewportHeight));
            return true;
        }
        if (isOverMaterialInfoOverlay(mouseX, mouseY)) {
            materialInfoScroll = clamp(materialInfoScroll + direction * MATERIAL_INFO_LINE_HEIGHT, 0,
                Math.max(0, materialInfoContentHeight - materialInfoViewportHeight));
            return true;
        }
        if (isWithin(mouseX, mouseY, contentX, listY, contentWidth, listBottom - listY)) {
            boolean wasRecipeOverlayLocked = recipeOverlayLocked;
            boolean wasMaterialInfoLocked = materialInfoLocked;
            clearRecipeOverlay();
            clearMaterialInfoOverlay();
            mainScroll = clamp(mainScroll + direction, 0, Math.max(0, visibleEntries.size() - visibleRows()));
            if (wasRecipeOverlayLocked) {
                TinkersConstructFilter.LOGGER.debug("Modifier recipe overlay unlocked by list scroll");
            }
            if (wasMaterialInfoLocked) {
                TinkersConstructFilter.LOGGER.debug("Material information overlay unlocked by list scroll");
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            ClientConfig.rememberSearch(searchBox.getValue());
            historyOpen = false;
            historyScroll = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (historyOpen || filterOpen || sortOpen || importantOpen)) {
            historyOpen = false;
            filterOpen = false;
            sortOpen = false;
            importantOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private CatalogEntry entryAt(double mouseX, double mouseY) {
        if (isOverRecipeOverlay(mouseX, mouseY)) {
            return null;
        }
        return entryAtRow(mouseX, mouseY);
    }

    private CatalogEntry entryAtRow(double mouseX, double mouseY) {
        if (!snapshot.fullyLoaded() || isOverOpenPopup(mouseX, mouseY) || !isWithin(mouseX, mouseY, contentX + 2, listY, contentWidth - 4, listBottom - listY)) {
            return null;
        }
        int index = mainScroll + (int) ((mouseY - listY) / ROW_HEIGHT);
        return index >= 0 && index < visibleEntries.size() ? visibleEntries.get(index) : null;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listY) / ROW_HEIGHT);
    }

    private List<ImportantPartOption> importantPartOptions() {
        Map<String, String> types = new LinkedHashMap<>();
        for (CatalogEntry entry : snapshot.parts()) {
            if (entry instanceof CatalogApi.PartView part && !part.getPartType().isEmpty()) {
                String title = part.getPartTypeName().isEmpty() ? part.getPartType() : part.getPartTypeName();
                types.putIfAbsent(part.getPartType(), title);
            }
        }
        List<ImportantPartOption> result = new ArrayList<>();
        for (Map.Entry<String, String> type : types.entrySet()) {
            result.add(new ImportantPartOption(type.getKey(), type.getValue()));
        }
        return List.copyOf(result);
    }

    private void syncImportantSelection() {
        Set<String> available = new HashSet<>();
        for (ImportantPartOption option : importantPartOptions()) {
            available.add(option.id());
        }
        selectedImportantPartTypes.retainAll(available);
    }

    private int importantPopupX() {
        return contentX;
    }

    private int importantPopupY() {
        return contentY;
    }

    private int importantPopupWidth() {
        return Math.min(320, Math.max(IMPORTANT_POPUP_MIN_WIDTH, contentWidth - 8));
    }

    private int importantRows(int optionCount) {
        int availableRows = Math.max(3, (listBottom - importantPopupY() - 8 - 18) / POPUP_ROW_HEIGHT);
        return Math.max(3, Math.min(14, Math.min(availableRows, Math.max(3, optionCount))));
    }

    private int importantPopupHeight(int optionCount) {
        return 18 + importantRows(optionCount) * POPUP_ROW_HEIGHT;
    }

    private int importantOptionsY() {
        return importantPopupY() + 18;
    }

    private int importantSelectAllX() {
        return importantClearX() - importantSelectAllWidth() - 8;
    }

    private int importantSelectAllWidth() {
        return font.width(Component.translatable("screen.tinkers_construct_filter.select_all"));
    }

    private int importantClearX() {
        return importantPopupX() + importantPopupWidth() - importantClearWidth() - 5;
    }

    private int importantClearWidth() {
        return font.width(Component.translatable("screen.tinkers_construct_filter.clear"));
    }

    private boolean isWithinImportantHeader(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, importantPopupX(), importantPopupY(), importantPopupWidth(), 18);
    }

    private int importantInfoWidth() {
        return Math.min(360, Math.max(IMPORTANT_POPUP_MIN_WIDTH, contentWidth - 12));
    }

    private void renderPopupScrollBar(GuiGraphics graphics, int x, int y, int width, int visibleAmount, int totalAmount, int scroll, int trackHeight) {
        if (totalAmount <= visibleAmount || trackHeight <= 0) {
            return;
        }
        int thumb = Math.max(10, trackHeight * visibleAmount / totalAmount);
        int maximumScroll = Math.max(1, totalAmount - visibleAmount);
        int thumbY = y + Math.max(0, trackHeight - thumb) * clamp(scroll, 0, maximumScroll) / maximumScroll;
        graphics.fill(x + width - 5, y, x + width - 3, y + trackHeight, 0xFF171717);
        graphics.fill(x + width - 5, thumbY, x + width - 3, thumbY + thumb, 0xFFB0B0B0);
    }

    private int popupX() {
        return contentX + Math.max(0, contentWidth - popupWidth());
    }

    private int popupY() {
        return contentY + 23;
    }

    private int popupWidth() {
        return Math.min(300, contentWidth);
    }

    private int filterRows() {
        return Math.max(4, Math.min(12, (panelY + panelHeight - popupY() - 10 - CATEGORY_BAR_HEIGHT) / POPUP_ROW_HEIGHT));
    }

    private int filterPopupHeight() {
        return CATEGORY_BAR_HEIGHT + Math.max(filterRows(), 2) * POPUP_ROW_HEIGHT;
    }

    private int filterOptionsY() {
        return popupY() + CATEGORY_BAR_HEIGHT;
    }

    private int filterCategoryAreaX() {
        return popupX() + 4;
    }

    private int filterCategoryAreaWidth() {
        return Math.max(1, popupWidth() - FILTER_CATEGORY_CLEAR_WIDTH - 8);
    }

    private int clearFilterX() {
        return popupX() + popupWidth() - FILTER_CATEGORY_CLEAR_WIDTH;
    }

    private boolean isWithinFilterCategoryBar(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), CATEGORY_BAR_HEIGHT);
    }

    private FilterCategory filterCategoryAt(double mouseX, double mouseY, List<FilterCategory> categories) {
        if (!isWithin(mouseX, mouseY, filterCategoryAreaX(), popupY() + 2, filterCategoryAreaWidth(), CATEGORY_BAR_HEIGHT - 4)) {
            return null;
        }
        clampFilterCategoryScroll(categories);
        int buttonX = filterCategoryAreaX() - filterCategoryScroll;
        for (FilterCategory category : categories) {
            int buttonWidth = filterCategoryButtonWidth(category);
            if (isWithin(mouseX, mouseY, buttonX, popupY() + 2, buttonWidth, CATEGORY_BAR_HEIGHT - 4)) {
                return category;
            }
            buttonX += buttonWidth + CATEGORY_GAP;
        }
        return null;
    }

    private int filterCategoryButtonWidth(FilterCategory category) {
        return Math.max(46, Math.min(120, font.width(category.title()) + 12));
    }

    private int filterCategoryContentWidth(List<FilterCategory> categories) {
        int width = 0;
        for (FilterCategory category : categories) {
            width += filterCategoryButtonWidth(category) + CATEGORY_GAP;
        }
        return Math.max(0, width - CATEGORY_GAP);
    }

    private void clampFilterCategoryScroll(List<FilterCategory> categories) {
        int maximum = Math.max(0, filterCategoryContentWidth(categories) - filterCategoryAreaWidth());
        filterCategoryScroll = clamp(filterCategoryScroll, 0, maximum);
    }

    private int sortRows() {
        return Math.max(4, Math.min(12, (panelY + panelHeight - popupY() - 10 - CATEGORY_BAR_HEIGHT) / POPUP_ROW_HEIGHT));
    }

    private int sortPopupHeight() {
        return CATEGORY_BAR_HEIGHT + Math.max(sortRows(), 2) * POPUP_ROW_HEIGHT;
    }

    private int sortOptionsY() {
        return popupY() + CATEGORY_BAR_HEIGHT;
    }

    private int sortCategoryAreaX() {
        return popupX() + 4;
    }

    private int sortCategoryAreaWidth() {
        return Math.max(1, popupWidth() - 8);
    }

    private boolean isWithinSortCategoryBar(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), CATEGORY_BAR_HEIGHT);
    }

    private SortCategory sortCategoryAt(double mouseX, double mouseY, List<SortCategory> categories) {
        if (!isWithin(mouseX, mouseY, sortCategoryAreaX(), popupY() + 2, sortCategoryAreaWidth(), CATEGORY_BAR_HEIGHT - 4)) {
            return null;
        }
        clampSortCategoryScroll(categories);
        int buttonX = sortCategoryAreaX() - sortCategoryScroll;
        for (SortCategory category : categories) {
            int buttonWidth = sortCategoryButtonWidth(category);
            if (isWithin(mouseX, mouseY, buttonX, popupY() + 2, buttonWidth, CATEGORY_BAR_HEIGHT - 4)) {
                return category;
            }
            buttonX += buttonWidth + CATEGORY_GAP;
        }
        return null;
    }

    private int sortCategoryButtonWidth(SortCategory category) {
        return Math.max(46, Math.min(120, font.width(category.title()) + 12));
    }

    private int sortCategoryContentWidth(List<SortCategory> categories) {
        int width = 0;
        for (SortCategory category : categories) {
            width += sortCategoryButtonWidth(category) + CATEGORY_GAP;
        }
        return Math.max(0, width - CATEGORY_GAP);
    }

    private void clampSortCategoryScroll(List<SortCategory> categories) {
        int maximum = Math.max(0, sortCategoryContentWidth(categories) - sortCategoryAreaWidth());
        sortCategoryScroll = clamp(sortCategoryScroll, 0, maximum);
    }

    private int historyX() {
        return searchBox.getX();
    }

    private int historyY() {
        return searchBox.getY() + searchBox.getHeight() + 2;
    }

    private int historyWidth() {
        return searchBox.getWidth();
    }

    private int historyRows() {
        int availableRows = Math.max(1, (height - historyY() - 22) / POPUP_ROW_HEIGHT);
        return Math.min(ClientConfig.getSearchHistoryVisibleRows(), availableRows);
    }

    private boolean isOverOpenPopup(double mouseX, double mouseY) {
        if (historyOpen && isWithin(mouseX, mouseY, historyX(), historyY(), historyWidth(), 18 + historyRows() * POPUP_ROW_HEIGHT)) {
            return true;
        }
        if (filterOpen && isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), filterPopupHeight())) {
            return true;
        }
        if (sortOpen && isWithin(mouseX, mouseY, popupX(), popupY(), popupWidth(), sortPopupHeight())) {
            return true;
        }
        if (importantOpen && isWithin(mouseX, mouseY, importantPopupX(), importantPopupY(), importantPopupWidth(), importantPopupHeight(importantPartOptions().size()))) {
            return true;
        }
        return isOverMaterialInfoOverlay(mouseX, mouseY);
    }

    private boolean isOverRecipeOverlay(double mouseX, double mouseY) {
        return recipeOverlayEntry != null && isWithin(mouseX, mouseY, recipeOverlayX, recipeOverlayY, recipeOverlayWidth, recipeOverlayHeight);
    }

    private void clearRecipeOverlay() {
        recipeOverlayEntry = null;
        recipeOverlayTools = List.of();
        recipeOverlayItems = List.of();
        hoveredRecipeItem = ItemStack.EMPTY;
        recipeOverlayLocked = false;
        recipeOverlayX = 0;
        recipeOverlayY = 0;
        recipeOverlayWidth = 0;
        recipeOverlayHeight = 0;
        recipeOverlayScroll = 0;
        recipeOverlayContentHeight = 0;
        recipeOverlayViewportHeight = 0;
    }

    private boolean isOverMaterialInfoOverlay(double mouseX, double mouseY) {
        return materialInfoEntry != null && isWithin(mouseX, mouseY, materialInfoX, materialInfoY, materialInfoWidth, materialInfoHeight);
    }

    private void clearMaterialInfoOverlay() {
        materialInfoEntry = null;
        materialInfoParts = List.of();
        materialInfoX = 0;
        materialInfoY = 0;
        materialInfoWidth = 0;
        materialInfoHeight = 0;
        materialInfoScroll = 0;
        materialInfoContentHeight = 0;
        materialInfoViewportHeight = 0;
        hoveredMaterialTrait = null;
        materialInfoLocked = false;
    }

    private String clip(String value, int maxWidth) {
        if (maxWidth <= 0 || value.isEmpty() || font.width(value) <= maxWidth) {
            return maxWidth <= 0 ? "" : value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Page {
        MATERIALS,
        PARTS,
        MODIFIERS
    }

    private record FilterOption(String id, String title, Predicate<CatalogEntry> predicate) {
    }

    private record FilterCategory(String id, String title, List<FilterOption> options) {
    }

    private record SortCategory(String id, String title, List<SortOption> options) {
    }

    private record ImportantPartOption(String id, String title) {
    }

    private record MaterialInfoLine(String text, int color, CatalogEntry.TraitTooltip trait) {
    }

    private record SortOption(String id, Component title, Boolean numeric, String attributeId, Function<CatalogEntry, Object> valueGetter, boolean defaultOrder) {
    }
}
