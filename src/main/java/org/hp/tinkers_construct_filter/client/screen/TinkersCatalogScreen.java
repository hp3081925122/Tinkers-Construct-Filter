package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
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
import org.hp.tinkers_construct_filter.client.screen.overlay.CatalogOverlayController;
import org.hp.tinkers_construct_filter.client.screen.overlay.CatalogOverlayContent;
import org.hp.tinkers_construct_filter.client.screen.overlay.CatalogOverlayRenderer;
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
    private static final int HISTORY_DELETE_WIDTH = 18;
    private static final int POPUP_Z = 200;
    private static final int CATEGORY_BAR_HEIGHT = 20;
    private static final int CATEGORY_GAP = 3;
    private static final int FILTER_CATEGORY_CLEAR_WIDTH = 38;
    private static final int IMPORTANT_BUTTON_WIDTH = 90;
    private static final int IMPORTANT_POPUP_MIN_WIDTH = 180;
    private static final long RECIPE_VARIANT_ANIMATION_MILLIS = 1600L;
    private static final int MODIFIER_DESCRIPTION_COLOR = 0xFFD0A0FF;
    private static final int RECIPE_INPUTS_COLOR = 0xFFFFB347;
    private static final int RECIPE_TOOLS_COLOR = 0xFF70D070;
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
    private final CatalogOverlayController overlayController = new CatalogOverlayController();
    private final CatalogOverlayRenderer overlayRenderer = new CatalogOverlayRenderer();

    private CatalogSnapshot snapshot = CatalogSnapshot.loading();
    private List<CatalogEntry> visibleEntries = List.of();
    private List<ItemStack> recipeOverlayTools = List.of();
    private List<CatalogApi.ModifierRecipeView> recipeOverlayRecipes = List.of();
    private ItemStack hoveredRecipeItem = ItemStack.EMPTY;
    private List<CatalogApi.PartView> materialInfoParts = List.of();
    private CatalogEntry.TraitTooltip hoveredMaterialTrait;
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
    private int mainScroll;
    private int filterCategoryScroll;
    private int sortCategoryScroll;
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
        materialButton = addRenderableWidget(new Button(panelX + 8, panelY + 52, NAV_WIDTH - 16, 20,
            Component.translatable("button.tinkers_construct_filter.materials"), button -> switchPage(Page.MATERIALS)));
        partButton = addRenderableWidget(new Button(panelX + 8, panelY + 76, NAV_WIDTH - 16, 20,
            Component.translatable("button.tinkers_construct_filter.parts"), button -> switchPage(Page.PARTS)));
        modifierButton = addRenderableWidget(new Button(panelX + 8, panelY + 100, NAV_WIDTH - 16, 20,
            Component.translatable("button.tinkers_construct_filter.modifiers"), button -> switchPage(Page.MODIFIERS)));

        importantButton = addRenderableWidget(new Button(contentX, panelY + 4, Math.min(IMPORTANT_BUTTON_WIDTH, contentWidth), 20,
            Component.translatable("button.tinkers_construct_filter.important_options"), button -> toggleImportantPopup()));

        int actionWidth = 54;
        int actionX = contentX + contentWidth - actionWidth * 3 - 8;
        int searchWidth = Math.max(88, actionX - contentX - 4);
        searchBox = addRenderableWidget(new EditBox(font, contentX, contentY, searchWidth, 20, Component.translatable("search.tinkers_construct_filter.hint")));
        searchBox.setMaxLength(120);
        searchBox.setSuggestion(Component.translatable("search.tinkers_construct_filter.hint").getString());
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::onSearchChanged);

        filterButton = addRenderableWidget(new Button(actionX, contentY, actionWidth, 20,
            Component.translatable("button.tinkers_construct_filter.filter"), button -> toggleFilterPopup()));
        sortButton = addRenderableWidget(new Button(actionX + actionWidth + 4, contentY, actionWidth, 20,
            Component.translatable("button.tinkers_construct_filter.sort"), button -> toggleSortPopup()));
        orderButton = addRenderableWidget(new Button(actionX + (actionWidth + 4) * 2, contentY, actionWidth, 20,
            Component.empty(), button -> toggleSortOrder()));
        updateNavigationButtons();
        updateOrderButton();
    }

    private void onSearchChanged(String value) {
        searchQuery = value;
        mainScroll = 0;
        clearInfoOverlay();
        rebuildVisibleEntries();
    }

    private void reloadCatalog() {
        CatalogExtensions.refreshDefinitions();
        snapshot = CatalogDataBuilder.build();
        catalogInitialized = true;
        mainScroll = 0;
        filterCategoryScroll = 0;
        sortCategoryScroll = 0;
        clearInfoOverlay();
        rebuildVisibleEntries();
        syncImportantSelection();
    }

    private void switchPage(Page nextPage) {
        if (page == nextPage) {
            return;
        }
        page = nextPage;
        mainScroll = 0;
        filterCategoryScroll = 0;
        sortCategoryScroll = 0;
        clearInfoOverlay();
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
        if (overlayController.isOpen(CatalogOverlayController.Type.FILTER)) {
            overlayController.clear();
        } else {
            clearInfoOverlay();
            overlayController.open(CatalogOverlayController.Type.FILTER);
        }
    }

    private void toggleSortPopup() {
        if (overlayController.isOpen(CatalogOverlayController.Type.SORT)) {
            overlayController.clear();
        } else {
            clearInfoOverlay();
            overlayController.open(CatalogOverlayController.Type.SORT);
        }
    }

    private void toggleImportantPopup() {
        if (overlayController.isOpen(CatalogOverlayController.Type.IMPORTANT)) {
            overlayController.clear();
        } else {
            clearInfoOverlay();
            overlayController.open(CatalogOverlayController.Type.IMPORTANT);
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
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        LegacyGuiGraphics graphics = new LegacyGuiGraphics(pose);
        renderBase(graphics, mouseX, mouseY);
        super.render(pose, mouseX, mouseY, partialTick);

        if (!overlayController.isModalOpen()) {
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
        if (overlayController.isModalOpen()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z);
            switch (overlayController.type()) {
                case HISTORY -> renderHistory(graphics, mouseX, mouseY);
                case FILTER -> renderFilterPopup(graphics, mouseX, mouseY);
                case SORT -> renderSortPopup(graphics, mouseX, mouseY);
                case IMPORTANT -> renderImportantPopup(graphics, mouseX, mouseY);
                default -> {
                }
            }
            graphics.pose().popPose();
        }
    }

    private void renderBase(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
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

    private void renderEntries(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        if (visibleEntries.isEmpty()) {
            return;
        }

        SortOption sort = activeSortOption(sortOptions());
        int rows = visibleRows();
        int end = Math.min(visibleEntries.size(), mainScroll + rows);
        boolean mouseOverPopup = overlayController.isOver(mouseX, mouseY);
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

    private void renderEntryIcon(LegacyGuiGraphics graphics, CatalogEntry entry, int x, int y) {
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

    private void renderScrollBar(LegacyGuiGraphics graphics, int total, int rows) {
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

    private void renderHistory(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<String> history = ClientConfig.getSearchHistory();
        int rows = historyRows();
        int x = historyX();
        int y = historyY();
        int width = historyWidth();
        int height = 18 + rows * POPUP_ROW_HEIGHT;
        overlayController.setBounds(x, y, width, height);
        overlayController.setContentMetrics(history.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height, 0xFF0F0F0F, 0xFF252525, 0);
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.history"), x + 5, y + 5, 0xFFFFFFFF, false);
        for (int row = 0; row < rows; row++) {
            int rowY = y + 18 + row * POPUP_ROW_HEIGHT;
            int index = overlayController.scroll() + row;
            boolean hovered = isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
            graphics.fill(x + 1, rowY, x + width - 1, rowY + POPUP_ROW_HEIGHT, hovered ? 0xFF505050 : 0xFF383838);
            String value = index < history.size() ? history.get(index) : "";
            graphics.drawString(font, clip(value, width - HISTORY_DELETE_WIDTH - 10), x + 5, rowY + 5, value.isEmpty() ? 0xFF777777 : 0xFFE5E5E5, false);
            if (!value.isEmpty()) {
                boolean deleteHovered = isHistoryDeleteHovered(mouseX, mouseY, x, rowY, width);
                String deleteText = Component.translatable("screen.tinkers_construct_filter.delete_history").getString();
                int deleteColor = deleteHovered ? 0xFFFF8080 : 0xFFE06060;
                graphics.drawString(font, deleteText, x + width - HISTORY_DELETE_WIDTH + 2, rowY + 5, deleteColor, false);
            }
        }
    }

    private void renderImportantPopup(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<ImportantPartOption> options = importantPartOptions();
        syncImportantSelection();
        int rows = importantRows(options.size());
        int x = importantPopupX();
        int y = importantPopupY();
        int width = importantPopupWidth();
        int height = importantPopupHeight(options.size());
        overlayController.setBounds(x, y, width, height);
        overlayController.setContentMetrics(options.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height, 0xFF0F0F0F, 0xFF252525, 0);
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
            int index = overlayController.scroll() + row;
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
        overlayRenderer.renderScrollBar(graphics, x, y + 18, width, rows, options.size(), overlayController.scroll(), height - 19);
    }

    private void renderFilterPopup(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<FilterCategory> categories = filterCategories();
        FilterCategory activeCategory = activeFilterCategory(categories);
        List<FilterOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int rows = filterRows();
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = filterPopupHeight();
        overlayController.setBounds(x, y, width, height);
        overlayController.setContentMetrics(options.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height, 0xFF0F0F0F, 0xFF252525, 0);
        renderFilterCategoryBar(graphics, categories, activeCategory, mouseX, mouseY);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_material_filters").getString(), width - 10), x + 5, filterOptionsY() + 7, 0xFFE0E0E0, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = overlayController.scroll() + row;
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

    private void renderFilterCategoryBar(LegacyGuiGraphics graphics, List<FilterCategory> categories, FilterCategory activeCategory, int mouseX, int mouseY) {
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

    private void renderSortPopup(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<SortCategory> categories = sortCategories();
        SortCategory activeCategory = activeSortCategory(categories);
        List<SortOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int rows = sortRows();
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = sortPopupHeight();
        overlayController.setBounds(x, y, width, height);
        overlayController.setContentMetrics(options.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height, 0xFF0F0F0F, 0xFF252525, 0);
        renderSortCategoryBar(graphics, categories, activeCategory, mouseX, mouseY);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_sort_options").getString(), width - 10), x + 5, sortOptionsY() + 7, 0xFFE0E0E0, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = overlayController.scroll() + row;
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

    private void renderSortCategoryBar(LegacyGuiGraphics graphics, List<SortCategory> categories, SortCategory activeCategory, int mouseX, int mouseY) {
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

    private void renderEntryTooltip(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        CatalogEntry entry = entryAt(mouseX, mouseY);
        if (entry == null) {
            return;
        }
        ItemStack stack = entry.getDisplayStack();
        if (!stack.isEmpty()) {
            renderTooltip(graphics.pose(), reorderTraitTooltip(entry, stack), stack.getTooltipImage(), mouseX, mouseY, font, stack);
        }
    }

    private boolean renderMaterialImportantOverlay(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        hoveredMaterialTrait = null;
        if (!snapshot.fullyLoaded() || page != Page.MATERIALS || selectedImportantPartTypes.isEmpty()) {
            clearInfoOverlay();
            return false;
        }

        if (!overlayController.isLocked() && !isOverMaterialInfoOverlay(mouseX, mouseY)) {
            CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
            if (!(rowEntry instanceof CatalogApi.MaterialView material)) {
                clearInfoOverlay();
                return false;
            }
            if (overlayController.target() != rowEntry) {
                overlayController.showDetail(CatalogOverlayController.Type.MATERIAL_INFO, rowEntry);
                materialInfoParts = selectedMaterialParts(material);
            }
        }
        if (!(overlayController.target() instanceof CatalogApi.MaterialView material)) {
            return false;
        }

        CatalogEntry target = overlayController.target();
        int entryIndex = visibleEntries.indexOf(target);
        if (entryIndex < mainScroll || entryIndex >= Math.min(visibleEntries.size(), mainScroll + visibleRows())) {
            clearInfoOverlay();
            return false;
        }

        CatalogOverlayContent overlayContent = buildMaterialInfoContent(materialInfoParts);
        int infoWidth = importantInfoWidth();
        int contentHeight = overlayContent.contentHeight(font, infoWidth);
        int maximumHeight = Math.max(MATERIAL_INFO_MIN_HEIGHT, listBottom - listY - 4);
        int infoHeight = Math.min(maximumHeight, contentHeight + 8);
        int viewportHeight = Math.max(1, infoHeight - 8);
        overlayController.setContentMetrics(contentHeight, viewportHeight);
        int infoX = contentX + contentWidth - infoWidth - 8;
        int rowY = listY + (entryIndex - mainScroll) * ROW_HEIGHT;
        int belowY = rowY + ROW_HEIGHT + 2;
        int aboveY = rowY - infoHeight - 2;
        int minimumY = listY;
        int maximumY = Math.max(minimumY, listBottom - infoHeight);
        int infoY = belowY + infoHeight <= listBottom ? belowY : aboveY;
        infoY = clamp(infoY, minimumY, maximumY);
        overlayController.setBounds(infoX, infoY, infoWidth, infoHeight);

        int contentTop = infoY + 4;
        int contentBottom = infoY + infoHeight - 4;
        CatalogOverlayContent.HoverResult hoverResult = overlayRenderer.renderContentPanel(graphics, infoX, infoY, infoWidth, infoHeight,
            contentTop, contentBottom, overlayController.scroll(), contentHeight, viewportHeight,
            mouseX, mouseY, font, overlayContent);
        hoveredMaterialTrait = hoverResult.trait();
        return true;
    }

    private void renderMaterialTraitTooltip(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
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
        renderTooltip(graphics.pose(), lines, mouseX, mouseY);
        graphics.pose().popPose();
    }

    private CatalogOverlayContent buildMaterialInfoContent(List<CatalogApi.PartView> parts) {
        List<CatalogOverlayContent.TextLine> lines = new ArrayList<>();
        lines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.material_info_hint"), 0xFFBFBFBF));
        lines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.material_info_subhint"), 0xFFA8A8A8));
        if (parts.isEmpty()) {
            lines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.no_matching_parts"), 0xFFE0E0E0));
            return new CatalogOverlayContent(lines, List.of());
        }
        for (int index = 0; index < parts.size(); index++) {
            CatalogApi.PartView part = parts.get(index);
            String partTitle = part.getPartTypeName().isEmpty() ? part.getPartType() : part.getPartTypeName();
            lines.add(CatalogOverlayContent.TextLine.plain(Component.literal(partTitle), importantPartColor(index)));
            if (part.getAttributeTexts().isEmpty()) {
                lines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.no_attributes"), 0xFFE0E0E0));
            } else {
                for (Map.Entry<String, String> attribute : part.getAttributeTexts().entrySet()) {
                    String title = importantAttributeTitle(attribute.getKey());
                    lines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.material_attribute", title, attribute.getValue()), 0xFFE0E0E0));
                }
            }
            lines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.traits"), 0xFFD0D0D0));
            if (part instanceof CatalogEntry entry) {
                for (CatalogEntry.TraitTooltip trait : entry.getTraitTooltips()) {
                    lines.add(CatalogOverlayContent.TextLine.trait(Component.literal("  " + trait.name()), 0xFFE0E0E0, trait));
                }
            }
            if (index + 1 < parts.size()) {
                lines.add(CatalogOverlayContent.TextLine.plain(Component.empty(), 0xFFE0E0E0));
            }
        }
        return new CatalogOverlayContent(lines, List.of());
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

    private boolean renderModifierRecipeOverlay(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        hoveredRecipeItem = ItemStack.EMPTY;
        if (!isOverRecipeOverlay(mouseX, mouseY)) {
            CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
            if (!overlayController.isLocked() && rowEntry instanceof CatalogApi.ModifierView modifier
                && (!modifier.getModifierDescriptions().isEmpty() || !modifier.getRecipeVariants().isEmpty() || !modifier.getRecipeTools().isEmpty())) {
                selectRecipeOverlay(modifier);
            } else if (!overlayController.isLocked()) {
                clearInfoOverlay();
                return false;
            }
        }

        if (!(overlayController.target() instanceof CatalogApi.ModifierView modifier)
            || (recipeOverlayTools.isEmpty() && recipeOverlayRecipes.isEmpty() && modifier.getModifierDescriptions().isEmpty())) {
            clearInfoOverlay();
            return false;
        }

        int entryIndex = visibleEntries.indexOf(overlayController.target());
        if (entryIndex < mainScroll || entryIndex >= Math.min(visibleEntries.size(), mainScroll + visibleRows())) {
            clearInfoOverlay();
            return false;
        }

        int overlayWidth = Math.min(260, Math.max(96, contentWidth - 12));
        List<String> descriptionLines = wrapModifierDescriptions(modifier.getModifierDescriptions(), overlayWidth - 10);
        List<List<ItemStack>> inputSlots = currentRecipeInputSlots();
        List<CatalogOverlayContent.TextLine> textLines = new ArrayList<>();
        textLines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.recipe_hint"), 0xFFBFBFBF));
        if (!descriptionLines.isEmpty()) {
            textLines.add(CatalogOverlayContent.TextLine.plain(Component.translatable("screen.tinkers_construct_filter.modifier_description"), MODIFIER_DESCRIPTION_COLOR));
            for (String description : descriptionLines) {
                textLines.add(CatalogOverlayContent.TextLine.plain(Component.literal(description), 0xFFE0E0E0));
            }
        }
        List<CatalogOverlayContent.ItemSlot> inputItems = inputSlots.stream()
            .map(CatalogOverlayContent.ItemSlot::new)
            .toList();
        List<CatalogOverlayContent.ItemSlot> toolItems = recipeOverlayTools.stream()
            .map(CatalogOverlayContent.ItemSlot::single)
            .toList();
        CatalogOverlayContent overlayContent = new CatalogOverlayContent(textLines, List.of(
            new CatalogOverlayContent.ItemSection(Component.translatable("screen.tinkers_construct_filter.recipe_inputs"), RECIPE_INPUTS_COLOR, inputItems),
            new CatalogOverlayContent.ItemSection(Component.translatable("screen.tinkers_construct_filter.recipe_tools"), RECIPE_TOOLS_COLOR, toolItems)
        ));
        int contentHeight = overlayContent.contentHeight(font, overlayWidth);
        int fullHeight = contentHeight + 10;
        int maximumHeight = Math.max(40, listBottom - listY - 4);
        int overlayHeight = Math.min(fullHeight, maximumHeight);
        int viewportHeight = Math.max(1, overlayHeight - 10);
        overlayController.setContentMetrics(contentHeight, viewportHeight);
        int overlayX = contentX + contentWidth - overlayWidth - 8;
        int rowY = listY + (entryIndex - mainScroll) * ROW_HEIGHT;
        int belowY = rowY + ROW_HEIGHT + 2;
        int aboveY = rowY - overlayHeight - 2;
        int minimumY = listY;
        int maximumY = Math.max(minimumY, listBottom - overlayHeight);
        int overlayY = belowY + overlayHeight <= listBottom ? belowY : aboveY;
        overlayY = clamp(overlayY, minimumY, maximumY);
        overlayController.setBounds(overlayX, overlayY, overlayWidth, overlayHeight);

        int contentTop = overlayY + 5;
        int contentBottom = overlayY + overlayHeight - 5;
        CatalogOverlayContent.HoverResult hoverResult = overlayRenderer.renderContentPanel(graphics, overlayX, overlayY, overlayWidth, overlayHeight,
            contentTop - 1, contentBottom, overlayController.scroll(), contentHeight, viewportHeight,
            mouseX, mouseY, font, overlayContent);
        hoveredRecipeItem = hoverResult.item();
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

    private List<List<ItemStack>> currentRecipeInputSlots() {
        if (recipeOverlayRecipes.isEmpty()) {
            return List.of();
        }
        int index = (int) ((System.currentTimeMillis() / RECIPE_VARIANT_ANIMATION_MILLIS) % recipeOverlayRecipes.size());
        return recipeOverlayRecipes.get(index).inputSlots();
    }

    private void renderItemTooltip(LegacyGuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        renderTooltip(graphics.pose(), stack.getTooltipLines(minecraft.player, flag), stack.getTooltipImage(), mouseX, mouseY, font, stack);
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
        if (overlayController.isModalOpen()) {
            if (overlayController.type() == CatalogOverlayController.Type.IMPORTANT
                && importantButton != null
                && isWithin(mouseX, mouseY, importantButton.x, importantButton.y, importantButton.getWidth(), importantButton.getHeight())) {
                toggleImportantPopup();
                return true;
            }
            if (overlayController.isOver(mouseX, mouseY)) {
                switch (overlayController.type()) {
                    case HISTORY -> handleHistoryClick(mouseX, mouseY);
                    case FILTER -> handleFilterClick(mouseX, mouseY);
                    case SORT -> handleSortClick(mouseX, mouseY);
                    case IMPORTANT -> handleImportantClick(mouseX, mouseY);
                    default -> {
                    }
                }
            } else {
                overlayController.clear();
            }
            return true;
        }
        if (isOverRecipeOverlay(mouseX, mouseY)) {
            overlayController.lock();
            return true;
        }
        if (isOverMaterialInfoOverlay(mouseX, mouseY)) {
            overlayController.lock();
            if (overlayController.target() != null) {
                TinkersConstructFilter.LOGGER.debug("Material information overlay locked by popup click: {}", overlayController.target().getId());
            }
            return true;
        }

        CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
        if (page == Page.MATERIALS && rowEntry instanceof CatalogApi.MaterialView material && !selectedImportantPartTypes.isEmpty()) {
            overlayController.showDetail(CatalogOverlayController.Type.MATERIAL_INFO, rowEntry);
            materialInfoParts = selectedMaterialParts(material);
            overlayController.lock();
            TinkersConstructFilter.LOGGER.debug("Material information overlay locked: {}", material.getId());
            return true;
        }
        if (rowEntry instanceof CatalogApi.ModifierView modifier) {
            if (!modifier.getModifierDescriptions().isEmpty() || !modifier.getRecipeVariants().isEmpty() || !modifier.getRecipeTools().isEmpty()) {
                selectRecipeOverlay(modifier);
                overlayController.lock();
                TinkersConstructFilter.LOGGER.debug("Modifier recipe overlay locked: {}", modifier.getId());
            } else {
                clearInfoOverlay();
            }
            return true;
        }

        overlayController.clear();
        hoveredRecipeItem = ItemStack.EMPTY;
        hoveredMaterialTrait = null;
        if (searchBox != null && isWithin(mouseX, mouseY, searchBox.x, searchBox.y, searchBox.getWidth(), searchBox.getHeight())) {
            clearInfoOverlay();
            overlayController.open(CatalogOverlayController.Type.HISTORY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleHistoryClick(double mouseX, double mouseY) {
        int row = (int) ((mouseY - historyY() - 18) / POPUP_ROW_HEIGHT);
        if (row < 0 || row >= historyRows()) {
            return;
        }
        List<String> history = ClientConfig.getSearchHistory();
        int index = overlayController.scroll() + row;
        if (index >= 0 && index < history.size()) {
            String value = history.get(index);
            if (isHistoryDeleteHovered(mouseX, mouseY, historyX(), historyY() + 18 + row * POPUP_ROW_HEIGHT, historyWidth())) {
                ClientConfig.removeSearch(value);
                TinkersConstructFilter.LOGGER.debug("Search history entry removed: {}", value);
                return;
            }
            searchBox.setValue(value);
            ClientConfig.rememberSearch(value);
            overlayController.clear();
        }
    }

    /** 判断鼠标是否位于搜索历史当前行右侧的删除叉区域。 */
    private boolean isHistoryDeleteHovered(double mouseX, double mouseY, int x, int rowY, int width) {
        return isWithin(mouseX, mouseY, x + width - HISTORY_DELETE_WIDTH, rowY, HISTORY_DELETE_WIDTH, POPUP_ROW_HEIGHT);
    }

    private void handleImportantClick(double mouseX, double mouseY) {
        List<ImportantPartOption> options = importantPartOptions();
        if (isWithinImportantHeader(mouseX, mouseY)) {
            if (mouseX >= importantSelectAllX() && mouseX < importantSelectAllX() + importantSelectAllWidth()) {
                selectedImportantPartTypes.clear();
                for (ImportantPartOption option : options) {
                    selectedImportantPartTypes.add(option.id());
                }
                overlayController.setScroll(0, 0);
            } else if (mouseX >= importantClearX() && mouseX < importantClearX() + importantClearWidth()) {
                selectedImportantPartTypes.clear();
                overlayController.setScroll(0, 0);
            }
            return;
        }
        int row = (int) ((mouseY - importantOptionsY()) / POPUP_ROW_HEIGHT);
        int index = overlayController.scroll() + row;
        if (index >= 0 && index < options.size()) {
            String id = options.get(index).id();
            if (!selectedImportantPartTypes.add(id)) {
                selectedImportantPartTypes.remove(id);
            }
            overlayController.setScroll(0, 0);
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
                    overlayController.setScroll(0, 0);
                }
            }
            return;
        }
        FilterCategory activeCategory = activeFilterCategory(categories);
        List<FilterOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int row = (int) ((mouseY - filterOptionsY()) / POPUP_ROW_HEIGHT);
        int index = overlayController.scroll() + row;
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
                overlayController.setScroll(0, 0);
            }
            return;
        }
        SortCategory activeCategory = activeSortCategory(categories);
        List<SortOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int row = (int) ((mouseY - sortOptionsY()) / POPUP_ROW_HEIGHT);
        int index = overlayController.scroll() + row;
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
        if (overlayController.isModalOpen()) {
            if (overlayController.isOver(mouseX, mouseY)) {
                switch (overlayController.type()) {
                    case HISTORY -> {
                        overlayController.scrollBy(direction);
                    }
                    case FILTER -> {
                        List<FilterCategory> categories = filterCategories();
                        if (isWithinFilterCategoryBar(mouseX, mouseY)) {
                            if (mouseX < clearFilterX()) {
                                int maximum = Math.max(0, filterCategoryContentWidth(categories) - filterCategoryAreaWidth());
                                filterCategoryScroll = clamp(filterCategoryScroll + direction * 18, 0, maximum);
                            }
                        } else {
                            overlayController.scrollBy(direction);
                        }
                    }
                    case SORT -> {
                        List<SortCategory> categories = sortCategories();
                        if (isWithinSortCategoryBar(mouseX, mouseY)) {
                            int maximum = Math.max(0, sortCategoryContentWidth(categories) - sortCategoryAreaWidth());
                            sortCategoryScroll = clamp(sortCategoryScroll + direction * 18, 0, maximum);
                        } else {
                            overlayController.scrollBy(direction);
                        }
                    }
                    case IMPORTANT -> {
                        overlayController.scrollBy(direction);
                    }
                    default -> {
                    }
                }
            }
            return true;
        }
        if (overlayController.isOpen(CatalogOverlayController.Type.MODIFIER_INFO)
            && overlayController.isOver(mouseX, mouseY)) {
            overlayController.scrollBy(direction * CatalogOverlayContent.TEXT_LINE_HEIGHT);
            return true;
        }
        if (overlayController.isOpen(CatalogOverlayController.Type.MATERIAL_INFO)
            && overlayController.isOver(mouseX, mouseY)) {
            overlayController.scrollBy(direction * CatalogOverlayContent.TEXT_LINE_HEIGHT);
            return true;
        }
        if (isWithin(mouseX, mouseY, contentX, listY, contentWidth, listBottom - listY)) {
            CatalogOverlayController.Type type = overlayController.type();
            boolean wasLocked = overlayController.isLocked();
            clearInfoOverlay();
            mainScroll = clamp(mainScroll + direction, 0, Math.max(0, visibleEntries.size() - visibleRows()));
            if (wasLocked && type == CatalogOverlayController.Type.MODIFIER_INFO) {
                TinkersConstructFilter.LOGGER.debug("Modifier recipe overlay unlocked by list scroll");
            }
            if (wasLocked && type == CatalogOverlayController.Type.MATERIAL_INFO) {
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
            overlayController.clear();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && overlayController.isModalOpen()) {
            overlayController.clear();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private CatalogEntry entryAt(double mouseX, double mouseY) {
        if (overlayController.isOver(mouseX, mouseY)) {
            return null;
        }
        return entryAtRow(mouseX, mouseY);
    }

    private CatalogEntry entryAtRow(double mouseX, double mouseY) {
        if (!snapshot.fullyLoaded() || overlayController.isOver(mouseX, mouseY)
            || !isWithin(mouseX, mouseY, contentX + 2, listY, contentWidth - 4, listBottom - listY)) {
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
        return searchBox.x;
    }

    private int historyY() {
        return searchBox.y + searchBox.getHeight() + 2;
    }

    private int historyWidth() {
        return searchBox.getWidth();
    }

    private int historyRows() {
        int availableRows = Math.max(1, (height - historyY() - 22) / POPUP_ROW_HEIGHT);
        return Math.min(ClientConfig.getSearchHistoryVisibleRows(), availableRows);
    }

    private boolean isOverRecipeOverlay(double mouseX, double mouseY) {
        return overlayController.isOpen(CatalogOverlayController.Type.MODIFIER_INFO)
            && overlayController.isOver(mouseX, mouseY);
    }

    private void selectRecipeOverlay(CatalogApi.ModifierView modifier) {
        if (!(modifier instanceof CatalogEntry entry)) {
            return;
        }
        overlayController.showDetail(CatalogOverlayController.Type.MODIFIER_INFO, entry);
        recipeOverlayTools = modifier.getRecipeTools();
        recipeOverlayRecipes = modifier.getRecipeVariants();
    }

    private boolean isOverMaterialInfoOverlay(double mouseX, double mouseY) {
        return overlayController.isOpen(CatalogOverlayController.Type.MATERIAL_INFO)
            && overlayController.isOver(mouseX, mouseY);
    }

    private void clearInfoOverlay() {
        overlayController.clear();
        recipeOverlayTools = List.of();
        recipeOverlayRecipes = List.of();
        hoveredRecipeItem = ItemStack.EMPTY;
        materialInfoParts = List.of();
        hoveredMaterialTrait = null;
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

    private record SortOption(String id, Component title, Boolean numeric, String attributeId, Function<CatalogEntry, Object> valueGetter, boolean defaultOrder) {
    }
}
