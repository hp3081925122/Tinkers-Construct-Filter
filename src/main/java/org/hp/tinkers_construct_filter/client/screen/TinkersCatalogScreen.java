package org.hp.tinkers_construct_filter.client.screen;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
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
import org.hp.tinkers_construct_filter.client.catalog.FilterMatchMode;
import org.hp.tinkers_construct_filter.client.catalog.TraitCategory;
import org.hp.tinkers_construct_filter.client.config.ClientConfig;
import org.hp.tinkers_construct_filter.client.screen.overlay.CatalogOverlayContent;
import org.hp.tinkers_construct_filter.client.screen.overlay.CatalogOverlayController;
import org.hp.tinkers_construct_filter.client.screen.overlay.CatalogOverlayRenderer;
import org.hp.tinkers_construct_filter.compat.jei.JeiCompat;
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
    private static final int PANEL_MAX_WIDTH = 820;
    private static final int PANEL_MAX_HEIGHT = 500;
    private static final int FRAME = 7;
    private static final int HEADER_HEIGHT = 22;
    private static final int NAV_WIDTH_WIDE = 108;
    private static final int NAV_WIDTH_COMPACT = 102;
    private static final int TAB_HEIGHT = 22;
    private static final int TOOLBAR_HEIGHT = 20;
    private static final int STATUS_HEIGHT = 13;
    private static final int SCROLL_GUTTER = 7;
    private static final int ROW_HEIGHT = 28;
    private static final int POPUP_ROW_HEIGHT = 18;
    private static final int POPUP_HEADER_HEIGHT = 19;
    private static final int HISTORY_DELETE_WIDTH = 18;
    private static final int POPUP_Z = 200;
    private static final int CATEGORY_BAR_HEIGHT = 20;
    private static final int CATEGORY_GAP = 3;
    private static final int FILTER_CATEGORY_CLEAR_WIDTH = 38;
    private static final int IMPORTANT_POPUP_MIN_WIDTH = 180;
    private static final int SPLIT_MIN_CONTENT_WIDTH = 280;
    private static final long RECIPE_VARIANT_ANIMATION_MILLIS = 1600L;
    private static final int MODIFIER_DESCRIPTION_COLOR = 0xFFD9B3FF;
    private static final int RECIPE_INPUTS_COLOR = 0xFFFFB85C;
    private static final int RECIPE_TOOLS_COLOR = 0xFF7CD67C;

    private final Screen parent;
    private final EnumMap<Page, LinkedHashSet<String>> selectedFilters = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> selectedFilterCategories = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> selectedSorts = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> selectedSortCategories = new EnumMap<>(Page.class);
    private final EnumMap<Page, Boolean> sortDescending = new EnumMap<>(Page.class);
    private final EnumMap<Page, Boolean> sortAutomatic = new EnumMap<>(Page.class);
    private final Map<CatalogEntry, Object> activeSortValues = new HashMap<>();
    private final LinkedHashSet<String> selectedImportantPartTypes = new LinkedHashSet<>();
    private boolean importantPartTypesInitialized;
    private final LinkedHashSet<String> selectedTraitSources = new LinkedHashSet<>(List.of("materials", "parts", "modifiers", "tools"));
    private final CatalogOverlayController overlayController = new CatalogOverlayController();
    private final CatalogOverlayRenderer overlayRenderer = new CatalogOverlayRenderer();

    private CatalogSnapshot snapshot = CatalogSnapshot.loading();
    private List<CatalogEntry> visibleEntries = List.of();
    private List<ItemStack> recipeOverlayTools = List.of();
    private List<CatalogApi.ModifierRecipeView> recipeOverlayRecipes = List.of();
    private List<CatalogApi.PartView> materialInfoParts = List.of();
    private ItemStack materialInfoItem = ItemStack.EMPTY;
    private CatalogEntry.TraitTooltip hoveredDetailTrait;
    private ItemStack hoveredDetailItem = ItemStack.EMPTY;
    private CatalogOverlayContent.JumpTarget hoveredJumpTarget;
    private boolean hoveredPinButton;
    private int lastMouseX;
    private int lastMouseY;
    private String lastCommittedSearch = "";

    private Page page = Page.MATERIALS;
    private EditBox searchBox;
    private CatalogButton materialButton;
    private CatalogButton partButton;
    private CatalogButton modifierButton;
    private CatalogButton traitButton;
    private CatalogButton filterButton;
    private CatalogButton sortButton;
    private CatalogButton orderButton;
    private CatalogButton importantButton;
    private final List<FilterChip> filterChips = new ArrayList<>();
    private String searchQuery = "";
    private boolean catalogInitialized;
    private int mainScroll;
    private int filterCategoryScroll;
    private int sortCategoryScroll;
    private boolean draggingListScrollbar;
    private boolean draggingDetailScrollbar;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int navWidth;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int bodyTop;
    private int bodyBottom;
    private int searchFrameWidth;
    private int statusY;
    private int listFrameY;
    private int listFrameWidth;
    private int listY;
    private int listBottom;
    private int rowX;
    private int rowWidth;
    private boolean splitView;
    private int detailX;
    private int detailY;
    private int detailWidth;
    private int detailHeight;
    private int importantOptionCount;

    public enum InitialTab {
        MATERIALS,
        PARTS,
        MODIFIERS,
        TRAITS
    }

    public TinkersCatalogScreen(Screen parent) {
        this(parent, InitialTab.MATERIALS);
    }

    public TinkersCatalogScreen(Screen parent, InitialTab initialTab) {
        super(Component.translatable("screen.tinkers_construct_filter.catalog"));
        this.parent = parent;
        if (initialTab != null) {
            this.page = switch (initialTab) {
                case MATERIALS -> Page.MATERIALS;
                case PARTS -> Page.PARTS;
                case MODIFIERS -> Page.MODIFIERS;
                case TRAITS -> Page.TRAITS;
            };
        }
        for (Page value : Page.values()) {
            selectedFilters.put(value, new LinkedHashSet<>());
            sortDescending.put(value, false);
            sortAutomatic.put(value, true);
        }
        selectedSorts.put(Page.MATERIALS, "default-material");
        selectedSorts.put(Page.PARTS, "default-part");
        selectedSorts.put(Page.MODIFIERS, "default-modifier");
        selectedSorts.put(Page.TRAITS, "default-trait");
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
        panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(340, width - 16));
        panelHeight = Math.min(PANEL_MAX_HEIGHT, Math.max(260, height - 16));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        navWidth = panelWidth < 460 ? NAV_WIDTH_COMPACT : NAV_WIDTH_WIDE;
        bodyTop = panelY + FRAME + HEADER_HEIGHT + 4;
        bodyBottom = panelY + panelHeight - FRAME;
        contentX = panelX + FRAME + navWidth + 5;
        contentY = bodyTop;
        contentWidth = panelX + panelWidth - FRAME - contentX;
        statusY = contentY + TOOLBAR_HEIGHT + 4;
        listFrameY = statusY + STATUS_HEIGHT + 3;
        listY = listFrameY + 3;
        listBottom = bodyBottom - 3;

        splitView = contentWidth >= SPLIT_MIN_CONTENT_WIDTH;
        if (splitView) {
            listFrameWidth = Math.max(156, (int) (contentWidth * 0.51F));
            detailX = contentX + listFrameWidth + 5;
            detailY = listFrameY;
            detailWidth = contentX + contentWidth - detailX;
            detailHeight = bodyBottom - listFrameY;
        } else {
            listFrameWidth = contentWidth;
            detailX = contentX + 8;
            detailY = listFrameY + 4;
            detailWidth = Math.max(120, contentWidth - 16);
            detailHeight = bodyBottom - listFrameY - 8;
        }
        rowX = contentX + 3;
        rowWidth = listFrameWidth - 6 - SCROLL_GUTTER;
    }

    private void createWidgets() {
        int tabX = panelX + FRAME + 3;
        int tabWidth = navWidth - 6;
        materialButton = addRenderableWidget(new CatalogButton(tabX, bodyTop + 4, tabWidth, TAB_HEIGHT,
            Component.translatable("button.tinkers_construct_filter.materials"), button -> switchPage(Page.MATERIALS))
            .icon(CatalogSkin.ICON_MATERIALS).tab());
        partButton = addRenderableWidget(new CatalogButton(tabX, bodyTop + 4 + (TAB_HEIGHT + 2), tabWidth, TAB_HEIGHT,
            Component.translatable("button.tinkers_construct_filter.parts"), button -> switchPage(Page.PARTS))
            .icon(CatalogSkin.ICON_PARTS).tab());
        modifierButton = addRenderableWidget(new CatalogButton(tabX, bodyTop + 4 + (TAB_HEIGHT + 2) * 2, tabWidth, TAB_HEIGHT,
            Component.translatable("button.tinkers_construct_filter.modifiers"), button -> switchPage(Page.MODIFIERS))
            .icon(CatalogSkin.ICON_MODIFIERS).tab());
        traitButton = addRenderableWidget(new CatalogButton(tabX, bodyTop + 4 + (TAB_HEIGHT + 2) * 3, tabWidth, TAB_HEIGHT,
            Component.translatable("button.tinkers_construct_filter.traits"), button -> switchPage(Page.TRAITS))
            .icon(CatalogSkin.ICON_TRAITS).tab());

        importantButton = addRenderableWidget(new CatalogButton(panelX + FRAME + 4, bodyBottom - 24, navWidth - 8, 20,
            Component.translatable("button.tinkers_construct_filter.part_attributes"), button -> toggleImportantPopup())
            .icon(CatalogSkin.ICON_STAR));
        addRenderableWidget(new CatalogButton(panelX + panelWidth - FRAME - 19, panelY + FRAME + 3, 16, 16,
            Component.translatable("gui.done"), button -> onClose()).icon(CatalogSkin.ICON_CLOSE).iconOnly());

        String filterLabel = Component.translatable("button.tinkers_construct_filter.filter").getString();
        String sortLabel = Component.translatable("button.tinkers_construct_filter.sort").getString();
        int filterWidth = toolbarButtonWidth(filterLabel) + font.width(" 99");
        int sortWidth = toolbarButtonWidth(sortLabel);
        int orderWidth = Math.max(toolbarButtonWidth(Component.translatable("button.tinkers_construct_filter.ascending").getString()),
            toolbarButtonWidth(Component.translatable("button.tinkers_construct_filter.descending").getString()));
        if (contentWidth - filterWidth - sortWidth - orderWidth - 10 < 96) {
            filterWidth = 22;
            sortWidth = 22;
            orderWidth = 22;
        }
        int orderX = contentX + contentWidth - orderWidth;
        int sortX = orderX - 3 - sortWidth;
        int filterX = sortX - 3 - filterWidth;
        searchFrameWidth = Math.max(64, filterX - 4 - contentX);
        searchBox = addRenderableWidget(new EditBox(font, contentX + 18, contentY + 6, Math.max(24, searchFrameWidth - 34), 12,
            Component.translatable("search.tinkers_construct_filter.hint")));
        searchBox.setBordered(false);
        searchBox.setTextColor(CatalogSkin.TEXT);
        searchBox.setMaxLength(120);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::onSearchChanged);

        filterButton = addRenderableWidget(new CatalogButton(filterX, contentY, filterWidth, TOOLBAR_HEIGHT,
            Component.literal(filterLabel), button -> toggleFilterPopup()).icon(CatalogSkin.ICON_FILTER));
        sortButton = addRenderableWidget(new CatalogButton(sortX, contentY, sortWidth, TOOLBAR_HEIGHT,
            Component.literal(sortLabel), button -> toggleSortPopup()).icon(CatalogSkin.ICON_SORT));
        orderButton = addRenderableWidget(new CatalogButton(orderX, contentY, orderWidth, TOOLBAR_HEIGHT,
            Component.empty(), button -> toggleSortOrder()));
        updateNavigationButtons();
        updateOrderButton();
    }

    private int toolbarButtonWidth(String label) {
        return Math.max(44, font.width(label) + 9 + 4 + 14);
    }

    private void onSearchChanged(String value) {
        searchQuery = value;
        mainScroll = 0;
        if (!splitView) {
            clearDetailSelection();
        }
        rebuildVisibleEntries();
    }

    private void reloadCatalog() {
        CatalogExtensions.refreshDefinitions();
        snapshot = CatalogDataBuilder.build();
        if (!importantPartTypesInitialized && snapshot.fullyLoaded()) {
            for (CatalogEntry entry : snapshot.parts()) {
                if (entry instanceof CatalogApi.PartView part && !part.getPartType().isEmpty()) {
                    selectedImportantPartTypes.add(part.getPartType());
                }
            }
            importantPartTypesInitialized = true;
        }
        catalogInitialized = true;
        updateNavigationButtons();
        mainScroll = 0;
        filterCategoryScroll = 0;
        sortCategoryScroll = 0;
        overlayController.closeModal();
        clearDetailSelection();
        syncImportantSelection();
        rebuildVisibleEntries();
    }

    private void switchPage(Page nextPage) {
        if (page == nextPage) {
            return;
        }
        commitSearchHistoryIfNeeded();
        page = nextPage;
        mainScroll = 0;
        filterCategoryScroll = 0;
        sortCategoryScroll = 0;
        overlayController.closeModal();
        clearDetailSelection();
        updateNavigationButtons();
        updateOrderButton();
        rebuildVisibleEntries();
    }

    private void updateNavigationButtons() {
        if (materialButton == null || partButton == null || modifierButton == null || traitButton == null || importantButton == null) {
            return;
        }
        materialButton.active = page != Page.MATERIALS;
        partButton.active = page != Page.PARTS;
        modifierButton.active = page != Page.MODIFIERS;
        traitButton.active = page != Page.TRAITS;
        materialButton.setSelected(page == Page.MATERIALS);
        partButton.setSelected(page == Page.PARTS);
        modifierButton.setSelected(page == Page.MODIFIERS);
        traitButton.setSelected(page == Page.TRAITS);
        importantButton.visible = page == Page.MATERIALS || page == Page.TRAITS;
        importantButton.active = page == Page.MATERIALS || page == Page.TRAITS;
        importantButton.setMessage(Component.translatable(page == Page.TRAITS
            ? "button.tinkers_construct_filter.trait_sources" : "button.tinkers_construct_filter.part_attributes"));
        boolean loaded = snapshot.fullyLoaded();
        materialButton.setBadge(loaded ? String.valueOf(snapshot.materials().size()) : "");
        partButton.setBadge(loaded ? String.valueOf(snapshot.parts().size()) : "");
        modifierButton.setBadge(loaded ? String.valueOf(snapshot.modifiers().size()) : "");
        traitButton.setBadge(loaded ? String.valueOf(snapshot.traits().size()) : "");
    }

    private void toggleFilterPopup() {
        if (overlayController.isModalOpen(CatalogOverlayController.ModalType.FILTER)) {
            overlayController.closeModal();
        } else {
            overlayController.openModal(CatalogOverlayController.ModalType.FILTER);
        }
    }

    private void toggleSortPopup() {
        if (overlayController.isModalOpen(CatalogOverlayController.ModalType.SORT)) {
            overlayController.closeModal();
        } else {
            overlayController.openModal(CatalogOverlayController.ModalType.SORT);
        }
    }

    private void toggleImportantPopup() {
        if (overlayController.isModalOpen(CatalogOverlayController.ModalType.IMPORTANT)) {
            overlayController.closeModal();
        } else {
            overlayController.openModal(CatalogOverlayController.ModalType.IMPORTANT);
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
            boolean descending = sortDescending.get(page);
            orderButton.setMessage(Component.translatable(descending
                ? "button.tinkers_construct_filter.descending"
                : "button.tinkers_construct_filter.ascending"));
            orderButton.icon(descending ? CatalogSkin.ICON_DESCENDING : CatalogSkin.ICON_ASCENDING);
        }
    }

    private void rebuildVisibleEntries() {
        activeSortValues.clear();
        filterChips.clear();
        if (!snapshot.fullyLoaded()) {
            visibleEntries = List.of();
            return;
        }

        List<FilterCategory> categories = filterCategories();
        List<FilterOption> filters = categories.stream().flatMap(category -> category.options().stream()).toList();
        Set<String> filterIds = new HashSet<>();
        for (FilterOption filter : filters) {
            filterIds.add(filter.id());
        }
        selectedFilters.get(page).retainAll(filterIds);
        for (FilterOption filter : filters) {
            if (selectedFilters.get(page).contains(filter.id())) {
                filterChips.add(new FilterChip(filter.id(), filter.title()));
            }
        }

        List<CatalogEntry> result = new ArrayList<>();
        for (CatalogEntry entry : sourceEntries()) {
            if (matchesSearch(entry) && matchesFilters(entry, categories)) {
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
                    numeric = activeSortValues.values().stream()
                        .filter(value -> value != null)
                        .findFirst()
                        .map(value -> value instanceof Number)
                        .orElse(false);
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

        CatalogEntry current = overlayController.inspectedEntry();
        if (current != null && !visibleEntries.contains(current)) {
            clearDetailSelection();
            current = null;
        }
        if (splitView && current == null && !visibleEntries.isEmpty()) {
            selectDetailEntry(visibleEntries.get(0), false);
        } else if (current != null) {
            refreshInspectedCache(current);
        }
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

    private boolean matchesFilters(CatalogEntry entry, List<FilterCategory> categories) {
        for (FilterCategory category : categories) {
            if (!category.mode().matches(category.options(), selectedFilters.get(page),
                FilterOption::id, option -> option.predicate().test(entry))) {
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
            case TRAITS -> snapshot.traits();
        };
    }

    private FilterCategory productionFilterCategory() {
        List<FilterOption> options = new ArrayList<>();
        for (String method : List.of("part_builder", "casting", "composite_casting", "unknown")) {
            options.add(new FilterOption("production:" + method,
                Component.translatable("filter.tinkers_construct_filter.production_" + method).getString(),
                entry -> entry instanceof CatalogApi.MaterialView material
                    ? material.getProductionMethods().contains(method)
                    : entry instanceof CatalogApi.PartView part && part.getProductionMethods().contains(method)));
        }
        return new FilterCategory("production-methods",
            Component.translatable("filter.tinkers_construct_filter.production_category").getString(), List.copyOf(options), FilterMatchMode.ANY);
    }

    private List<FilterCategory> filterCategories() {
        List<FilterCategory> categories = new ArrayList<>(builtInFilterCategories());
        String pageId = page.name().toLowerCase(Locale.ROOT);
        Map<String, List<FilterOption>> groups = new LinkedHashMap<>();
        for (CatalogExtensions.FilterDefinition definition : CatalogExtensions.filters(pageId)) {
            groups.computeIfAbsent(definition.category(), ignored -> new ArrayList<>())
                .add(new FilterOption("custom:" + definition.id(), definition.title(), entry -> CatalogExtensions.test(definition, entry)));
        }
        for (Map.Entry<String, List<FilterOption>> group : groups.entrySet()) {
            CatalogExtensions.FilterCategoryDefinition definition = CatalogExtensions.filterCategory(pageId, group.getKey());
            String title = definition.title().isEmpty()
                ? Component.translatable("filter.tinkers_construct_filter.custom_category").getString() : definition.title();
            categories.add(new FilterCategory("custom:" + definition.id(), title, List.copyOf(group.getValue()), definition.mode()));
        }
        return List.copyOf(categories);
    }

    private List<FilterCategory> builtInFilterCategories() {
        List<FilterOption> result = new ArrayList<>();
        if (page == Page.PARTS) {
            Map<String, String> types = new LinkedHashMap<>();
            Map<String, String> tools = new LinkedHashMap<>();
            for (CatalogEntry entry : snapshot.parts()) {
                if (entry instanceof CatalogApi.PartView view) {
                    types.putIfAbsent(view.getPartType(), view.getPartTypeName());
                    tools.putAll(view.getToolCategories());
                }
            }
            for (Map.Entry<String, String> type : types.entrySet()) {
                result.add(new FilterOption("type:" + type.getKey(), type.getValue(),
                    entry -> entry instanceof CatalogApi.PartView view && view.getPartType().equals(type.getKey())));
            }
            List<FilterCategory> categories = new ArrayList<>();
            if (!result.isEmpty()) {
                categories.add(new FilterCategory("tinkers-parts",
                    Component.translatable("filter.tinkers_construct_filter.part_category").getString(), List.copyOf(result), FilterMatchMode.ANY));
            }
            List<FilterOption> toolOptions = new ArrayList<>();
            tools.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByValue(String.CASE_INSENSITIVE_ORDER).thenComparing(Map.Entry::getKey))
                .forEach(tool -> toolOptions.add(new FilterOption("part-tool:" + tool.getKey(), tool.getValue(),
                    entry -> entry instanceof CatalogApi.PartView view && view.getToolCategories().containsKey(tool.getKey()))));
            if (!toolOptions.isEmpty()) {
                categories.add(new FilterCategory("part-tools",
                    Component.translatable("filter.tinkers_construct_filter.part_tool_category").getString(), List.copyOf(toolOptions), FilterMatchMode.ANY));
            }
            categories.add(productionFilterCategory());
            return List.copyOf(categories);
        } else if (page == Page.MATERIALS) {
            List<FilterOption> levels = new ArrayList<>();
            int maximumLevel = ClientConfig.MATERIAL_LEVEL_FILTER_MAX.get();
            if (maximumLevel < 0) {
                maximumLevel = 0;
                for (CatalogEntry entry : snapshot.materials()) {
                    if (entry instanceof CatalogApi.MaterialView) {
                        maximumLevel = Math.max(maximumLevel, entry.getMaterialLevel());
                    }
                }
            }
            for (int level = 0; level <= maximumLevel; level++) {
                int selectedLevel = level;
                levels.add(new FilterOption("material-level:" + selectedLevel,
                    Component.translatable("filter.tinkers_construct_filter.material_level", selectedLevel).getString(),
                    entry -> entry instanceof CatalogApi.MaterialView && entry.getMaterialLevel() == selectedLevel));
            }
            List<FilterCategory> categories = new ArrayList<>();
            if (!levels.isEmpty()) {
                categories.add(new FilterCategory("material-levels",
                    Component.translatable("filter.tinkers_construct_filter.material_level_category").getString(), List.copyOf(levels), FilterMatchMode.ANY));
            }
            categories.add(productionFilterCategory());
            return List.copyOf(categories);
        } else if (page == Page.TRAITS) {
            for (TraitCategory category : TraitCategory.values()) {
                result.add(new FilterOption("trait-type:" + category.id(),
                    Component.translatable("filter.tinkers_construct_filter.trait_" + category.id()).getString(),
                    entry -> CatalogExtensions.traitCategories(entry.getId()).contains(category)));
            }
            return List.of(new FilterCategory("trait-types",
                Component.translatable("filter.tinkers_construct_filter.trait_type").getString(), List.copyOf(result), FilterMatchMode.ANY));
        } else {
            Map<String, String> slots = new LinkedHashMap<>();
            Map<String, String> tools = new LinkedHashMap<>();
            for (CatalogEntry entry : sourceEntries()) {
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
        } else if (page == Page.MODIFIERS) {
            common.add(new SortOption("default-modifier",
                Component.translatable("sort.tinkers_construct_filter.default_modifier"), false, null, null, true));
            common.add(new SortOption("modifier-name",
                Component.translatable("sort.tinkers_construct_filter.modifier_name"), false, null, CatalogEntry::getName, false));
            common.add(new SortOption("modifier-slot",
                Component.translatable("sort.tinkers_construct_filter.modifier_slot"), false, null,
                entry -> entry instanceof CatalogApi.ModifierView view ? String.join(" / ", view.getSlotCategories().values()) : null, false));
            common.add(new SortOption("modifier-tools",
                Component.translatable("sort.tinkers_construct_filter.modifier_tool"), true, null,
                entry -> entry instanceof CatalogApi.ModifierView view ? view.getToolCategories().size() : null, false));
            common.add(new SortOption("modifier-max-level",
                Component.translatable("sort.tinkers_construct_filter.modifier_max_level"), true, null,
                entry -> entry instanceof CatalogApi.ModifierView view && view.getMaxLevel() > 0 ? view.getMaxLevel() : null, false));
        } else {
            common.add(new SortOption("default-trait",
                Component.translatable("sort.tinkers_construct_filter.default_trait"), false, null, null, true));
            common.add(new SortOption("trait-name",
                Component.translatable("sort.tinkers_construct_filter.trait_name"), false, null, CatalogEntry::getName, false));
            common.add(new SortOption("trait-sources",
                Component.translatable("sort.tinkers_construct_filter.trait_source_count"), true, null,
                entry -> entry instanceof CatalogApi.ModifierView view
                    ? view.getSourceMaterials().size() + view.getSourceParts().size() + view.getSourceTools().size() + (view.hasModifierRecipe() ? 1 : 0)
                    : null, false));
        }
        for (CatalogExtensions.SortDefinition definition : CatalogExtensions.sorts(page.name().toLowerCase(Locale.ROOT))) {
            custom.add(new SortOption("custom:" + definition.id(), Component.literal(definition.title()), null, null,
                entry -> CatalogExtensions.value(definition, entry), false));
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
        if (page == Page.MODIFIERS || page == Page.TRAITS) {
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
        LegacyGuiGraphics graphics = new LegacyGuiGraphics(pose);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        renderBackground(pose);
        updateHoverDetailTarget(mouseX, mouseY);
        renderBase(graphics, mouseX, mouseY);
        filterButton.setSelected(overlayController.isModalOpen(CatalogOverlayController.ModalType.FILTER));
        sortButton.setSelected(overlayController.isModalOpen(CatalogOverlayController.ModalType.SORT));
        importantButton.setSelected(overlayController.isModalOpen(CatalogOverlayController.ModalType.IMPORTANT));
        int selectedFilterCount = selectedFilters.get(page).size();
        filterButton.setBadge(selectedFilterCount > 0 && filterButton.getWidth() > 26 ? String.valueOf(selectedFilterCount) : "");
        if (searchBox != null && searchQuery.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("search.tinkers_construct_filter.hint").getString(), searchBox.getWidth()),
                searchBox.x, searchBox.y, CatalogSkin.TEXT_DIM, false);
        }
        super.render(pose, mouseX, mouseY, partialTick);

        if (splitView) {
            renderSplitInspector(graphics, mouseX, mouseY);
        } else if (!overlayController.isModalOpen() && overlayController.hasDetail()) {
            renderFloatingInspector(graphics, mouseX, mouseY);
        }

        if (!overlayController.isModalOpen()) {
            renderActiveTooltips(graphics, mouseX, mouseY);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z);
            switch (overlayController.modalType()) {
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

    private void updateHoverDetailTarget(int mouseX, int mouseY) {
        if (!snapshot.fullyLoaded() || overlayController.isModalOpen() || overlayController.isDetailPinned()
            || draggingListScrollbar || draggingDetailScrollbar) {
            return;
        }
        if (!splitView && overlayController.isOverDetail(mouseX, mouseY)) {
            return;
        }
        CatalogEntry hoveredRow = entryAtRow(mouseX, mouseY);
        if (hoveredRow != null) {
            selectDetailEntry(hoveredRow, false);
        } else if (!splitView) {
            clearDetailSelection();
        }
    }

    private void renderBase(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0x7A090707);
        CatalogSkin.draw(graphics, CatalogSkin.WINDOW, panelX, panelY, panelWidth, panelHeight);
        int headerX = panelX + FRAME;
        int headerY = panelY + FRAME;
        CatalogSkin.draw(graphics, CatalogSkin.HEADER, headerX, headerY, panelWidth - FRAME * 2, HEADER_HEIGHT);
        CatalogSkin.icon(graphics, CatalogSkin.ICON_LOGO, headerX + 5, headerY + 2, 0xFFFFFFFF);
        int titleEnd = graphics.drawString(font, title, headerX + 25, headerY + 6, CatalogSkin.ACCENT, false);
        graphics.drawString(font, "›  " + pageTitle().getString(), titleEnd + 6, headerY + 6, CatalogSkin.TEXT_MUTED, false);

        int sidebarX = panelX + FRAME;
        CatalogSkin.draw(graphics, CatalogSkin.SIDEBAR, sidebarX, bodyTop, navWidth, bodyBottom - bodyTop);
        if (importantButton.visible) {
            CatalogSkin.copperDivider(graphics, sidebarX + 5, importantButton.getY() - 6, navWidth - 10);
        }

        boolean modal = overlayController.isModalOpen();
        boolean searchFocused = searchBox.isFocused();
        boolean searchHovered = !modal && isWithin(mouseX, mouseY, contentX, contentY, searchFrameWidth, TOOLBAR_HEIGHT);
        CatalogSkin.Sprite inputSprite = searchFocused ? CatalogSkin.INPUT_FOCUSED : searchHovered ? CatalogSkin.INPUT_HOVER : CatalogSkin.INPUT;
        CatalogSkin.draw(graphics, inputSprite, contentX, contentY, searchFrameWidth, TOOLBAR_HEIGHT);
        CatalogSkin.icon(graphics, CatalogSkin.ICON_SEARCH, contentX + 5, contentY + 5,
            searchFocused ? CatalogSkin.ACCENT : searchHovered ? CatalogSkin.TEXT : CatalogSkin.TEXT_DIM);
        if (!searchQuery.isEmpty()) {
            boolean clearHovered = !modal && isOverSearchClear(mouseX, mouseY);
            CatalogSkin.icon(graphics, CatalogSkin.ICON_REMOVE, searchClearX() + 2, contentY + 5,
                clearHovered ? CatalogSkin.DANGER : CatalogSkin.TEXT_MUTED);
        }

        CatalogSkin.draw(graphics, CatalogSkin.LIST, contentX, listFrameY, listFrameWidth, bodyBottom - listFrameY);
        if (!snapshot.fullyLoaded()) {
            graphics.drawCenteredString(font, Component.translatable("screen.tinkers_construct_filter.loading"),
                contentX + listFrameWidth / 2, listY + 22, CatalogSkin.TEXT_MUTED);
            return;
        }

        renderStatusLine(graphics, mouseX, mouseY);
        if (visibleEntries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.tinkers_construct_filter.no_results"),
                contentX + listFrameWidth / 2, listY + 22, CatalogSkin.TEXT_DIM);
            return;
        }
        renderEntries(graphics, mouseX, mouseY);
    }

    private Component pageTitle() {
        return Component.translatable(switch (page) {
            case MATERIALS -> "button.tinkers_construct_filter.materials";
            case PARTS -> "button.tinkers_construct_filter.parts";
            case MODIFIERS -> "button.tinkers_construct_filter.modifiers";
            case TRAITS -> "button.tinkers_construct_filter.traits";
        });
    }

    private void renderStatusLine(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        int countEnd = graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.result_count", visibleEntries.size()),
            contentX + 2, statusY + 3, CatalogSkin.TEXT_MUTED, false);
        boolean modal = overlayController.isModalOpen();
        int chipX = countEnd + 8;
        int limit = contentX + contentWidth;
        for (int index = 0; index < filterChips.size(); index++) {
            FilterChip chip = filterChips.get(index);
            int chipWidth = chipWidth(chip);
            String more = "+" + (filterChips.size() - index);
            if (chipX + chipWidth > limit - (index + 1 < filterChips.size() ? font.width(more) + 4 : 0)) {
                graphics.drawString(font, more, chipX, statusY + 3, CatalogSkin.ACCENT, false);
                break;
            }
            boolean hovered = !modal && isWithin(mouseX, mouseY, chipX, statusY, chipWidth, STATUS_HEIGHT);
            CatalogSkin.draw(graphics, hovered ? CatalogSkin.CHIP_HOVER : CatalogSkin.CHIP, chipX, statusY, chipWidth, STATUS_HEIGHT);
            graphics.drawString(font, chip.title(), chipX + 5, statusY + 3, hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT, false);
            CatalogSkin.icon(graphics, CatalogSkin.ICON_REMOVE, chipX + chipWidth - 12, statusY + 2,
                hovered ? CatalogSkin.DANGER : CatalogSkin.ACCENT);
            chipX += chipWidth + 3;
        }
    }

    private int chipWidth(FilterChip chip) {
        return font.width(chip.title()) + 18;
    }

    private FilterChip chipAt(double mouseX, double mouseY) {
        if (!snapshot.fullyLoaded() || mouseY < statusY || mouseY >= statusY + STATUS_HEIGHT) {
            return null;
        }
        int chipX = contentX + 2 + font.width(Component.translatable("screen.tinkers_construct_filter.result_count", visibleEntries.size())) + 8;
        int limit = contentX + contentWidth;
        for (int index = 0; index < filterChips.size(); index++) {
            FilterChip chip = filterChips.get(index);
            int chipWidth = chipWidth(chip);
            String more = "+" + (filterChips.size() - index);
            if (chipX + chipWidth > limit - (index + 1 < filterChips.size() ? font.width(more) + 4 : 0)) {
                return null;
            }
            if (mouseX >= chipX && mouseX < chipX + chipWidth) {
                return chip;
            }
            chipX += chipWidth + 3;
        }
        return null;
    }

    private void renderEntries(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        SortOption sort = activeSortOption(sortOptions());
        int rows = visibleRows();
        int end = Math.min(visibleEntries.size(), mainScroll + rows);
        boolean blocked = overlayController.isModalOpen() || (!splitView && overlayController.isOverDetail(mouseX, mouseY));
        CatalogEntry inspected = overlayController.inspectedEntry();
        boolean pinned = overlayController.isDetailPinned();

        for (int index = mainScroll; index < end; index++) {
            int rowY = listY + (index - mainScroll) * ROW_HEIGHT;
            CatalogEntry entry = visibleEntries.get(index);
            boolean hovered = !blocked && isWithin(mouseX, mouseY, rowX, rowY, rowWidth, ROW_HEIGHT);
            boolean activeRow = entry == inspected && (pinned || splitView);
            CatalogSkin.Sprite rowSprite = (entry == inspected && pinned) ? CatalogSkin.ROW_ACTIVE
                : (hovered || activeRow) ? CatalogSkin.ROW_HOVER
                : index % 2 == 0 ? CatalogSkin.ROW : CatalogSkin.ROW_ALT;
            CatalogSkin.draw(graphics, rowSprite, rowX, rowY, rowWidth, ROW_HEIGHT);

            boolean slotHovered = !blocked && isWithin(mouseX, mouseY, rowX + 5, rowY + 5, 18, 18);
            CatalogSkin.draw(graphics, slotHovered ? CatalogSkin.SLOT_HOVER : CatalogSkin.SLOT, rowX + 5, rowY + 5, 18, 18);
            renderEntryIcon(graphics, entry, rowX + 6, rowY + 6);

            String rightText = rowRightText(entry, sort);
            int rightColor = rowRightColor(entry, sort);
            int rightWidth = rightText.isEmpty() ? 0 : Math.min(rowWidth / 2 - 12, font.width(rightText));
            String name = clip(entry.getName(), rowWidth - 32 - (rightWidth > 0 ? rightWidth + 8 : 0));
            graphics.drawString(font, name, rowX + 28, rowY + 5,
                hovered || entry == inspected ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT, false);
            if (!rightText.isEmpty()) {
                String clippedRight = clip(rightText, rowWidth / 2 - 12);
                int actualRightW = font.width(clippedRight);
                graphics.drawString(font, clippedRight, rowX + rowWidth - 5 - actualRightW, rowY + 5, rightColor, false);
            }

            String secondary = entrySecondaryText(entry);
            graphics.drawString(font, clip(secondary, rowWidth - 34), rowX + 28, rowY + 16, CatalogSkin.TEXT_MUTED, false);
        }
        renderListScrollBar(graphics, visibleEntries.size(), rows, mouseX, mouseY);
    }

    private String rowRightText(CatalogEntry entry, SortOption sort) {
        if (!sort.defaultOrder()) {
            Object value = activeSortValues.get(entry);
            if (value == null) {
                return "—";
            }
            return sort.attributeId() == null
                ? value.toString()
                : entry.getAttributeTexts().getOrDefault(sort.attributeId(), CatalogAttributes.format(sort.attributeId(), ((Number) value).doubleValue()));
        }
        if (page == Page.MATERIALS) {
            return Component.translatable("screen.tinkers_construct_filter.tier_badge", entry.getMaterialLevel()).getString();
        }
        if (page == Page.PARTS) {
            return Component.translatable("screen.tinkers_construct_filter.tier_badge", entry.getMaterialLevel()).getString();
        }
        return "";
    }

    private int rowRightColor(CatalogEntry entry, SortOption sort) {
        if (!sort.defaultOrder()) {
            return CatalogSkin.ACCENT;
        }
        if (page == Page.MATERIALS || page == Page.PARTS) {
            return CatalogSkin.tierColor(entry.getMaterialLevel());
        }
        return CatalogSkin.TEXT_MUTED;
    }

    private String entrySecondaryText(CatalogEntry entry) {
        if (page == Page.MATERIALS && entry instanceof CatalogApi.MaterialView material) {
            if (!entry.getTraitNames().isEmpty()) {
                return String.join(" · ", entry.getTraitNames());
            }
            return Component.translatable("screen.tinkers_construct_filter.part_count_short", material.getAvailablePartCount()).getString();
        }
        if (page == Page.PARTS && entry instanceof CatalogApi.PartView part) {
            String traits = entry.getTraitNames().isEmpty() ? "" : String.join(" · ", entry.getTraitNames());
            String typeName = part.getPartTypeName();
            if (!typeName.isEmpty() && !traits.isEmpty()) {
                return typeName + " · " + traits;
            }
            return !traits.isEmpty() ? traits : (!typeName.isEmpty() ? typeName : "—");
        }
        if (page == Page.TRAITS) {
            return String.join(" · ", CatalogExtensions.traitCategories(entry.getId()).stream()
                .map(category -> Component.translatable("filter.tinkers_construct_filter.trait_" + category.id()).getString()).toList());
        }
        if (entry instanceof CatalogApi.ModifierView modifier) {
            String slotReq = modifier.getSlotRequirementSummary();
            if (!slotReq.isEmpty()) {
                return slotReq;
            }
            List<String> values = new ArrayList<>(modifier.getSlotCategories().values());
            return values.isEmpty() ? "—" : String.join(" · ", values);
        }
        return "—";
    }

    private void renderEntryIcon(LegacyGuiGraphics graphics, CatalogEntry entry, int x, int y) {
        ItemStack stack = entry.getDisplayStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
        }
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
        if (modifier.getSlotCategories().containsKey("soul")) {
            return 3;
        }
        if (modifier.getSlotCategories().containsKey("slotless")) {
            return 4;
        }
        return 5;
    }

    private void renderListScrollBar(LegacyGuiGraphics graphics, int total, int rows, int mouseX, int mouseY) {
        if (total <= rows) {
            return;
        }
        int height = listBottom - listY;
        int x = contentX + listFrameWidth - SCROLL_GUTTER - 1;
        boolean hovered = draggingListScrollbar || (!overlayController.isModalOpen() && isWithin(mouseX, mouseY, x - 2, listY, 8, height));
        overlayRenderer.renderScrollBarAt(graphics, x, listY, height, rows, total, mainScroll, hovered);
    }

    private void renderSplitInspector(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        hoveredDetailTrait = null;
        hoveredDetailItem = ItemStack.EMPTY;
        hoveredJumpTarget = null;
        hoveredPinButton = false;
        overlayController.setDetailBounds(detailX, detailY, detailWidth, detailHeight);

        if (!snapshot.fullyLoaded() || !overlayController.hasDetail()) {
            CatalogSkin.draw(graphics, CatalogSkin.DETAIL_PANEL, detailX, detailY, detailWidth, detailHeight);
            if (snapshot.fullyLoaded()) {
                List<String> hintLines = CatalogOverlayContent.wrapLines(font,
                    List.of(Component.translatable("screen.tinkers_construct_filter.select_entry_hint").getString()),
                    detailWidth - 16);
                int hy = detailY + (detailHeight - hintLines.size() * 12) / 2;
                for (String line : hintLines) {
                    graphics.drawCenteredString(font, line, detailX + detailWidth / 2, hy, CatalogSkin.TEXT_DIM);
                    hy += 12;
                }
            }
            return;
        }

        CatalogOverlayContent content = buildDetailContent(overlayController.inspectedEntry(), detailWidth - 20);
        boolean interactive = !overlayController.isModalOpen();
        CatalogOverlayContent.HoverResult hover = overlayRenderer.renderInspector(graphics,
            detailX, detailY, detailWidth, detailHeight, overlayController, font, content, mouseX, mouseY, interactive, false);
        if (interactive) {
            hoveredDetailItem = hover.item();
            hoveredDetailTrait = hover.trait();
            hoveredJumpTarget = hover.jumpTarget();
            hoveredPinButton = hover.pinHovered();
        }
    }

    private void renderFloatingInspector(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        hoveredDetailTrait = null;
        hoveredDetailItem = ItemStack.EMPTY;
        hoveredJumpTarget = null;
        hoveredPinButton = false;
        CatalogEntry target = overlayController.inspectedEntry();
        int entryIndex = visibleEntries.indexOf(target);
        if (target == null || entryIndex < mainScroll || entryIndex >= Math.min(visibleEntries.size(), mainScroll + visibleRows())) {
            clearDetailSelection();
            return;
        }
        int floatW = Math.min(280, Math.max(160, contentWidth - 10));
        CatalogOverlayContent content = buildDetailContent(target, floatW - 20);
        int desiredH = CatalogOverlayRenderer.HERO_HEIGHT + 12 + content.contentHeight(font, floatW - 20);
        int maxH = Math.max(80, listBottom - listY - 4);
        int floatH = Math.min(desiredH, maxH);
        int floatX = contentX + contentWidth - floatW - 6;
        int rowY = listY + (entryIndex - mainScroll) * ROW_HEIGHT;
        int belowY = rowY + ROW_HEIGHT + 2;
        int aboveY = rowY - floatH - 2;
        int floatY = clamp(belowY + floatH <= listBottom ? belowY : aboveY, listY, Math.max(listY, listBottom - floatH));
        overlayController.setDetailBounds(floatX, floatY, floatW, floatH);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z - 10);
        graphics.addItemZOffset(POPUP_Z - 10);
        CatalogOverlayContent.HoverResult hover;
        try {
            hover = overlayRenderer.renderInspector(graphics,
                floatX, floatY, floatW, floatH, overlayController, font, content, mouseX, mouseY, true, true);
        } finally {
            graphics.addItemZOffset(-(POPUP_Z - 10));
            graphics.pose().popPose();
        }
        hoveredDetailItem = hover.item();
        hoveredDetailTrait = hover.trait();
        hoveredJumpTarget = hover.jumpTarget();
        hoveredPinButton = hover.pinHovered();
    }

    private void renderActiveTooltips(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredPinButton) {
            Component tip = Component.translatable(overlayController.isDetailPinned()
                ? "screen.tinkers_construct_filter.unpin_detail"
                : "screen.tinkers_construct_filter.pin_detail");
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 5);
            graphics.renderTooltip(font, tip, mouseX, mouseY);
            graphics.pose().popPose();
            return;
        }
        if (hoveredDetailTrait != null) {
            renderTraitTooltip(graphics, hoveredDetailTrait, mouseX, mouseY);
            return;
        }
        if (!hoveredDetailItem.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 5);
            renderItemTooltip(graphics, hoveredDetailItem, mouseX, mouseY);
            graphics.pose().popPose();
            return;
        }
        if (hoveredJumpTarget != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 5);
            graphics.renderTooltip(font, Component.translatable("screen.tinkers_construct_filter.click_to_jump_entry")
                .withStyle(style -> style.withColor(CatalogSkin.ACCENT & 0xFFFFFF)), mouseX, mouseY);
            graphics.pose().popPose();
            return;
        }
        if (!searchQuery.isEmpty() && isOverSearchClear(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("screen.tinkers_construct_filter.clear_search"), mouseX, mouseY);
            return;
        }
        CatalogEntry slotEntry = entryAtRowSlot(mouseX, mouseY);
        if (slotEntry != null) {
            ItemStack stack = slotEntry.getDisplayStack();
            if (!stack.isEmpty()) {
                graphics.renderTooltip(font, reorderTraitTooltip(slotEntry, stack), stack.getTooltipImage(), stack, mouseX, mouseY);
            }
        }
    }

    private void selectDetailEntry(CatalogEntry entry, boolean pin) {
        if (entry == null) {
            clearDetailSelection();
            return;
        }
        boolean changed = overlayController.inspectedEntry() != entry;
        overlayController.inspect(entry, pin);
        if (changed) {
            refreshInspectedCache(entry);
        }
    }

    private void refreshInspectedCache(CatalogEntry entry) {
        materialInfoParts = List.of();
        materialInfoItem = ItemStack.EMPTY;
        recipeOverlayTools = List.of();
        recipeOverlayRecipes = List.of();
        if (entry instanceof CatalogApi.MaterialView material) {
            materialInfoParts = selectedMaterialParts(material);
        } else if (entry instanceof CatalogApi.PartView part) {
            materialInfoParts = List.of(part);
            for (CatalogEntry candidate : snapshot.materials()) {
                if (candidate instanceof CatalogApi.MaterialView material && candidate.getId().equals(part.getMaterialId())) {
                    materialInfoItem = material.getMaterialDisplayItem();
                    break;
                }
            }
        } else if (entry instanceof CatalogApi.ModifierView modifier) {
            recipeOverlayTools = modifier.getRecipeTools();
            recipeOverlayRecipes = modifier.getRecipeVariants();
        }
    }

    private void clearDetailSelection() {
        overlayController.clearDetail();
        materialInfoParts = List.of();
        materialInfoItem = ItemStack.EMPTY;
        recipeOverlayTools = List.of();
        recipeOverlayRecipes = List.of();
        hoveredDetailItem = ItemStack.EMPTY;
        hoveredDetailTrait = null;
        hoveredJumpTarget = null;
        hoveredPinButton = false;
    }

    private CatalogOverlayContent buildDetailContent(CatalogEntry entry, int wrapWidth) {
        if (entry instanceof CatalogApi.MaterialView material) {
            return buildMaterialDetailContent(entry, material, wrapWidth);
        }
        if (entry instanceof CatalogApi.PartView part) {
            return buildPartDetailContent(entry, part, wrapWidth);
        }
        if (entry instanceof CatalogApi.ModifierView modifier) {
            return buildModifierDetailContent(entry, modifier, wrapWidth);
        }
        CatalogOverlayContent.HeroHeader hero = new CatalogOverlayContent.HeroHeader(
            entry.getDisplayStack(), entry.getName(), "", CatalogSkin.TEXT_MUTED, "");
        return new CatalogOverlayContent(hero, List.of());
    }

    private CatalogOverlayContent buildMaterialDetailContent(CatalogEntry entry, CatalogApi.MaterialView material, int wrapWidth) {
        String badge = Component.translatable("screen.tinkers_construct_filter.tier_badge", entry.getMaterialLevel()).getString();
        String subtitle = formatProductionMethods(material.getProductionMethods());
        CatalogOverlayContent.HeroHeader hero = new CatalogOverlayContent.HeroHeader(
            entry.getDisplayStack(), entry.getName(), badge, CatalogSkin.tierColor(entry.getMaterialLevel()), subtitle);

        List<CatalogOverlayContent.Card> cards = new ArrayList<>();
        List<CatalogOverlayContent.CardBlock> overviewBlocks = new ArrayList<>();
        if (!material.getMaterialItems().isEmpty()) {
            List<CatalogOverlayContent.ItemSlot> itemSlots = material.getMaterialItems().stream()
                .map(CatalogOverlayContent.ItemSlot::single)
                .toList();
            overviewBlocks.add(new CatalogOverlayContent.ItemGridBlock(
                Component.translatable("screen.tinkers_construct_filter.material_items"),
                0xFFFFD86B,
                itemSlots
            ));
        }
        if (!material.getCastingFluids().isEmpty()) {
            overviewBlocks.add(new CatalogOverlayContent.TitleBlock(
                Component.translatable("screen.tinkers_construct_filter.casting_fluids"),
                0xFFFF9E5E
            ));
            overviewBlocks.add(new CatalogOverlayContent.DescriptionBlock(
                material.getCastingFluids(),
                CatalogSkin.TEXT_BRIGHT
            ));
        }
        if (!overviewBlocks.isEmpty()) {
            cards.add(new CatalogOverlayContent.Card(overviewBlocks));
        }

        List<CatalogApi.PartView> displayCards = new ArrayList<>(materialInfoParts);
        displayCards.addAll(material.getExtraStatCards());

        if (displayCards.isEmpty()) {
            List<CatalogOverlayContent.CardBlock> fallbackBlocks = new ArrayList<>();
            if (!material.getGeneralDescription().isBlank()) {
                fallbackBlocks.add(new CatalogOverlayContent.TitleBlock(
                    Component.translatable("screen.tinkers_construct_filter.material_description"),
                    CatalogSkin.ACCENT
                ));
                fallbackBlocks.add(new CatalogOverlayContent.DescriptionBlock(
                    List.of(material.getGeneralDescription()),
                    CatalogSkin.TEXT_MUTED
                ));
            }
            if (!entry.getTraitTooltips().isEmpty()) {
                fallbackBlocks.add(new CatalogOverlayContent.TraitPillsBlock(
                    Component.translatable("screen.tinkers_construct_filter.default_traits"),
                    entry.getTraitTooltips(),
                    true
                ));
            }
            fallbackBlocks.add(new CatalogOverlayContent.DescriptionBlock(
                List.of(Component.translatable("screen.tinkers_construct_filter.no_matching_parts").getString()),
                CatalogSkin.TEXT_DIM
            ));
            cards.add(new CatalogOverlayContent.Card(fallbackBlocks));
            return new CatalogOverlayContent(hero, cards);
        }

        for (int index = 0; index < displayCards.size(); index++) {
            CatalogApi.PartView part = displayCards.get(index);
            cards.add(buildPartStatCard(part, importantPartColor(index), wrapWidth));
        }
        return new CatalogOverlayContent(hero, cards);
    }

    private CatalogOverlayContent buildPartDetailContent(CatalogEntry entry, CatalogApi.PartView part, int wrapWidth) {
        String badge = Component.translatable("screen.tinkers_construct_filter.tier_badge", entry.getMaterialLevel()).getString();
        String prod = formatProductionMethods(part.getProductionMethods());
        String typeName = part.getPartTypeName();
        String subtitle = !typeName.isEmpty() && !prod.isEmpty() ? typeName + " · " + prod : (!typeName.isEmpty() ? typeName : prod);
        CatalogOverlayContent.HeroHeader hero = new CatalogOverlayContent.HeroHeader(
            entry.getDisplayStack(), entry.getName(), badge, CatalogSkin.tierColor(entry.getMaterialLevel()), subtitle);

        List<CatalogOverlayContent.Card> cards = new ArrayList<>();
        cards.add(buildPartStatCard(part, CatalogSkin.ACCENT, wrapWidth));

        List<CatalogOverlayContent.CardBlock> sourceBlocks = new ArrayList<>();
        Component materialTitle = Component.translatable("screen.tinkers_construct_filter.part_material", part.getMaterialName());
        CatalogOverlayContent.JumpTarget materialJump = new CatalogOverlayContent.JumpTarget("materials", part.getMaterialId());
        List<CatalogOverlayContent.ItemSlot> matSlots = materialInfoItem.isEmpty()
            ? List.of()
            : List.of(CatalogOverlayContent.ItemSlot.single(materialInfoItem, materialJump));
        sourceBlocks.add(new CatalogOverlayContent.ItemGridBlock(materialTitle, 0xFF70C8FF, matSlots, materialInfoItem, materialJump));
        if (!part.getToolCategories().isEmpty()) {
            String toolNames = String.join(" · ", part.getToolCategories().values());
            sourceBlocks.add(new CatalogOverlayContent.TitleBlock(Component.translatable("filter.tinkers_construct_filter.part_tool_category"), RECIPE_TOOLS_COLOR));
            sourceBlocks.add(new CatalogOverlayContent.DescriptionBlock(
                List.of(toolNames), CatalogSkin.TEXT));
        }
        cards.add(new CatalogOverlayContent.Card(sourceBlocks));
        return new CatalogOverlayContent(hero, cards);
    }

    private CatalogOverlayContent.Card buildPartStatCard(CatalogApi.PartView part, int titleColor, int wrapWidth) {
        List<CatalogOverlayContent.CardBlock> blocks = new ArrayList<>();
        String partTitle = part.getPartTypeName().isEmpty() ? part.getPartType() : part.getPartTypeName();
        if (!partTitle.isEmpty()) {
            blocks.add(new CatalogOverlayContent.TitleBlock(Component.literal(partTitle), titleColor));
        }
        String materialDescription = part.getMaterialDescription();
        if (!materialDescription.isBlank()) {
            blocks.add(new CatalogOverlayContent.DescriptionBlock(
                List.of(materialDescription), CatalogSkin.TEXT_MUTED));
        }
        if (part.getAttributeTexts().isEmpty()) {
            blocks.add(new CatalogOverlayContent.DescriptionBlock(
                List.of(Component.translatable("screen.tinkers_construct_filter.no_attributes").getString()), CatalogSkin.TEXT_DIM));
        } else {
            List<CatalogOverlayContent.StatRow> statRows = new ArrayList<>();
            for (Map.Entry<String, String> attribute : part.getAttributeTexts().entrySet()) {
                statRows.add(new CatalogOverlayContent.StatRow(
                    CatalogAttributes.shortTitle(attribute.getKey()), attribute.getValue(), CatalogSkin.TEXT_BRIGHT));
            }
            blocks.add(new CatalogOverlayContent.StatGridBlock(statRows));
        }
        if (part instanceof CatalogEntry partEntry && !partEntry.getTraitTooltips().isEmpty()) {
            blocks.add(new CatalogOverlayContent.TraitPillsBlock(
                Component.translatable("screen.tinkers_construct_filter.traits"), partEntry.getTraitTooltips(), true));
        }
        return new CatalogOverlayContent.Card(blocks);
    }

    private CatalogOverlayContent buildModifierDetailContent(CatalogEntry entry, CatalogApi.ModifierView modifier, int wrapWidth) {
        String subtitle = page == Page.TRAITS
            ? String.join(" · ", CatalogExtensions.traitCategories(entry.getId()).stream()
                .map(c -> Component.translatable("filter.tinkers_construct_filter.trait_" + c.id()).getString()).toList())
            : (!modifier.getSlotRequirementSummary().isEmpty()
                ? modifier.getSlotRequirementSummary()
                : String.join(" · ", modifier.getSlotCategories().values()));
        String badge = modifier.getLevelSummary();
        CatalogOverlayContent.HeroHeader hero = new CatalogOverlayContent.HeroHeader(
            entry.getDisplayStack(), entry.getName(), badge, CatalogSkin.ACCENT, subtitle);

        List<CatalogOverlayContent.Card> cards = new ArrayList<>();
        List<CatalogOverlayContent.CardBlock> descBlocks = new ArrayList<>();
        CatalogEntry.TraitTooltip selfTrait = new CatalogEntry.TraitTooltip(entry.getId(), modifier.getName(), modifier.getModifierDescriptions());
        descBlocks.add(new CatalogOverlayContent.TraitPillsBlock(null, List.of(selfTrait), false));
        if (!modifier.getModifierDescriptions().isEmpty()) {
            descBlocks.add(new CatalogOverlayContent.DescriptionBlock(
                modifier.getModifierDescriptions(), CatalogSkin.TEXT));
        }
        CatalogApi.ModifierRecipeView activeVariant = currentRecipeVariant();
        String slotText = activeVariant != null && !activeVariant.slotSummary().isEmpty()
            ? activeVariant.slotSummary()
            : modifier.getSlotRequirementSummary();
        String levelText = activeVariant != null && !activeVariant.levelSummary().isEmpty()
            ? activeVariant.levelSummary()
            : modifier.getLevelSummary();
        if (modifier.hasModifierRecipe() && (!slotText.isEmpty() || !levelText.isEmpty())) {
            List<CatalogOverlayContent.StatRow> specRows = new ArrayList<>();
            if (!slotText.isEmpty()) {
                specRows.add(new CatalogOverlayContent.StatRow(
                    Component.translatable("screen.tinkers_construct_filter.slot_requirement").getString(),
                    slotText,
                    CatalogSkin.ACCENT
                ));
            }
            if (!levelText.isEmpty()) {
                specRows.add(new CatalogOverlayContent.StatRow(
                    Component.translatable("screen.tinkers_construct_filter.level_cap").getString(),
                    levelText,
                    CatalogSkin.TEXT_BRIGHT
                ));
            }
            descBlocks.add(new CatalogOverlayContent.StatGridBlock(specRows));
        }
        cards.add(new CatalogOverlayContent.Card(descBlocks));

        if (page == Page.TRAITS) {
            if (selectedTraitSources.contains("materials") && !modifier.getSourceMaterials().isEmpty()) {
                cards.add(new CatalogOverlayContent.Card(List.of(
                    new CatalogOverlayContent.ItemGridBlock(
                        Component.translatable("button.tinkers_construct_filter.materials"),
                        0xFFFFD86B,
                        modifier.getSourceMaterials().stream()
                            .map(e -> CatalogOverlayContent.ItemSlot.single(
                                e.getDisplayStack(),
                                new CatalogOverlayContent.JumpTarget("materials", e.getId())
                            )).toList()))));
            }
            if (selectedTraitSources.contains("parts") && !modifier.getSourceParts().isEmpty()) {
                cards.add(new CatalogOverlayContent.Card(List.of(
                    new CatalogOverlayContent.ItemGridBlock(
                        Component.translatable("button.tinkers_construct_filter.parts"),
                        0xFF66CCFF,
                        modifier.getSourceParts().stream()
                            .map(e -> CatalogOverlayContent.ItemSlot.single(
                                e.getDisplayStack(),
                                new CatalogOverlayContent.JumpTarget("parts", e.getId())
                            )).toList()))));
            }
            if (selectedTraitSources.contains("tools") && !modifier.getSourceTools().isEmpty()) {
                cards.add(new CatalogOverlayContent.Card(List.of(
                    new CatalogOverlayContent.ItemGridBlock(
                        Component.translatable("screen.tinkers_construct_filter.innate_tool_traits"),
                        0xFF98E080,
                        modifier.getSourceTools().stream()
                            .map(CatalogOverlayContent.ItemSlot::single).toList()))));
            }
        }

        boolean showRecipe = page != Page.TRAITS || selectedTraitSources.contains("modifiers");
        if (showRecipe && modifier.hasModifierRecipe()) {
            List<CatalogOverlayContent.ItemSlot> inputItems = (activeVariant == null ? List.<List<ItemStack>>of() : activeVariant.inputSlots()).stream()
                .map(CatalogOverlayContent.ItemSlot::new).toList();
            List<CatalogOverlayContent.ItemSlot> toolItems = recipeOverlayTools.stream()
                .map(CatalogOverlayContent.ItemSlot::single).toList();
            cards.add(new CatalogOverlayContent.Card(List.of(
                new CatalogOverlayContent.ItemGridBlock(
                    Component.translatable(page == Page.TRAITS
                        ? "screen.tinkers_construct_filter.trait_recipe_inputs"
                        : "screen.tinkers_construct_filter.recipe_inputs"),
                    RECIPE_INPUTS_COLOR, inputItems),
                new CatalogOverlayContent.ItemGridBlock(
                    Component.translatable("screen.tinkers_construct_filter.recipe_tools"),
                    RECIPE_TOOLS_COLOR, toolItems)
            )));
        } else if (showRecipe && !modifier.hasModifierRecipe()) {
            cards.add(new CatalogOverlayContent.Card(List.of(
                new CatalogOverlayContent.DescriptionBlock(
                    List.of(Component.translatable("screen.tinkers_construct_filter.no_modifier_recipe").getString()),
                    MODIFIER_DESCRIPTION_COLOR))));
        }

        return new CatalogOverlayContent(hero, cards);
    }

    private String formatProductionMethods(Set<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (String key : List.of("part_builder", "casting", "composite_casting")) {
            if (methods.contains(key)) {
                names.add(Component.translatable("filter.tinkers_construct_filter.production_" + key).getString());
            }
        }
        if (names.isEmpty() && methods.contains("unknown")) {
            names.add(Component.translatable("filter.tinkers_construct_filter.production_unknown").getString());
        }
        return String.join(" · ", names);
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
            if (importantSelection().contains(option.id()) && available.containsKey(option.id())) {
                result.add(available.get(option.id()));
            }
        }
        return List.copyOf(result);
    }

    private int importantPartColor(int index) {
        return switch (index % 6) {
            case 0 -> 0xFFFFB85C;
            case 1 -> 0xFF7CD67C;
            case 2 -> 0xFF70C8FF;
            case 3 -> 0xFFFF85CC;
            case 4 -> 0xFFD9A6FF;
            default -> 0xFFFFDC73;
        };
    }

    private CatalogApi.ModifierRecipeView currentRecipeVariant() {
        if (recipeOverlayRecipes.isEmpty()) {
            return null;
        }
        int index = (int) ((System.currentTimeMillis() / RECIPE_VARIANT_ANIMATION_MILLIS) % recipeOverlayRecipes.size());
        return recipeOverlayRecipes.get(index);
    }

    private void renderTraitTooltip(LegacyGuiGraphics graphics, CatalogEntry.TraitTooltip trait, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.literal(trait.name()).withStyle(style -> style.withColor(CatalogSkin.ACCENT & 0xFFFFFF).withUnderlined(true)), 300));
        for (String description : trait.descriptions()) {
            lines.addAll(font.split(Component.literal(description), 300));
        }
        if (hoveredJumpTarget != null) {
            lines.addAll(font.split(Component.translatable("screen.tinkers_construct_filter.click_to_jump")
                .withStyle(style -> style.withColor(CatalogSkin.COPPER & 0xFFFFFF)), 300));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z + 5);
        graphics.renderTooltip(font, lines, mouseX, mouseY);
        graphics.pose().popPose();
    }

    private void renderItemTooltip(LegacyGuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> lines = new ArrayList<>(stack.getTooltipLines(minecraft.player, flag));
        if (hoveredJumpTarget != null) {
            lines.add(Component.translatable("screen.tinkers_construct_filter.click_to_jump_entry")
                .withStyle(style -> style.withColor(CatalogSkin.ACCENT & 0xFFFFFF)));
        }
        graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
    }

    private List<Component> reorderTraitTooltip(CatalogEntry entry, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> source = stack.getTooltipLines(minecraft.player, flag);
        List<CatalogEntry.TraitTooltip> traits = entry.getTraitTooltips();
        if (traits.isEmpty() || source.size() <= 1) {
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

    private void renderHistory(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<String> history = ClientConfig.getSearchHistory();
        if (history.isEmpty()) {
            overlayController.closeModal();
            return;
        }
        int rows = Math.min(history.size(), historyRows());
        int x = historyX();
        int y = historyY();
        int width = historyWidth();
        int height = POPUP_HEADER_HEIGHT + rows * POPUP_ROW_HEIGHT + 3;
        overlayController.setModalBounds(x, y, width, height);
        overlayController.setModalMetrics(history.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height);
        CatalogSkin.icon(graphics, CatalogSkin.ICON_HISTORY, x + 6, y + 5, CatalogSkin.ACCENT);
        graphics.drawString(font, Component.translatable("screen.tinkers_construct_filter.history"), x + 19, y + 6, CatalogSkin.TEXT_MUTED, false);
        CatalogSkin.copperDivider(graphics, x + 4, y + POPUP_HEADER_HEIGHT - 2, width - 8);
        for (int row = 0; row < rows; row++) {
            int rowY = y + POPUP_HEADER_HEIGHT + row * POPUP_ROW_HEIGHT;
            int index = overlayController.modalScroll() + row;
            String value = index < history.size() ? history.get(index) : "";
            boolean hovered = !value.isEmpty() && isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
            if (hovered) {
                CatalogSkin.draw(graphics, CatalogSkin.ROW_HOVER, x + 4, rowY, width - 8, POPUP_ROW_HEIGHT);
            }
            graphics.drawString(font, clip(value, width - HISTORY_DELETE_WIDTH - 16), x + 8, rowY + 5,
                hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT, false);
            if (!value.isEmpty()) {
                boolean deleteHovered = isHistoryDeleteHovered(mouseX, mouseY, x, rowY, width);
                CatalogSkin.icon(graphics, CatalogSkin.ICON_REMOVE, x + width - HISTORY_DELETE_WIDTH + 3, rowY + 4,
                    deleteHovered ? CatalogSkin.DANGER : CatalogSkin.TEXT_DIM);
            }
        }
        overlayRenderer.renderScrollBar(graphics, x, y + POPUP_HEADER_HEIGHT, width, rows, history.size(),
            overlayController.modalScroll(), rows * POPUP_ROW_HEIGHT);
    }

    private void renderImportantPopup(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<ImportantPartOption> options = importantPartOptions();
        syncImportantSelection();
        importantOptionCount = options.size();
        int rows = importantRows(options.size());
        int x = importantPopupX();
        int y = importantPopupY();
        int width = importantPopupWidth();
        int height = importantPopupHeight(options.size());
        overlayController.setModalBounds(x, y, width, height);
        overlayController.setModalMetrics(options.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height);
        String selectAll = Component.translatable("screen.tinkers_construct_filter.select_all").getString();
        String clear = Component.translatable("screen.tinkers_construct_filter.clear").getString();
        graphics.drawString(font, clip(Component.translatable(page == Page.TRAITS
            ? "screen.tinkers_construct_filter.trait_sources" : "screen.tinkers_construct_filter.important_options").getString(),
            importantSelectAllX() - x - 12), x + 6, y + 6, CatalogSkin.TEXT_MUTED, false);
        renderTextLink(graphics, selectAll, importantSelectAllX(), y + 6, mouseX, mouseY);
        renderTextLink(graphics, clear, importantClearX(), y + 6, mouseX, mouseY);
        CatalogSkin.copperDivider(graphics, x + 4, y + POPUP_HEADER_HEIGHT - 2, width - 8);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_important_options").getString(), width - 12),
                x + 6, y + 25, CatalogSkin.TEXT_DIM, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = overlayController.modalScroll() + row;
            if (index >= options.size()) {
                break;
            }
            int rowY = importantOptionsY() + row * POPUP_ROW_HEIGHT;
            ImportantPartOption option = options.get(index);
            renderOptionRow(graphics, x, rowY, width, option.title(), importantSelection().contains(option.id()), false, mouseX, mouseY);
        }
        overlayRenderer.renderScrollBar(graphics, x, importantOptionsY(), width, rows, options.size(),
            overlayController.modalScroll(), rows * POPUP_ROW_HEIGHT);
    }

    private void renderOptionRow(LegacyGuiGraphics graphics, int x, int rowY, int width, String title, boolean selected, boolean radio, int mouseX, int mouseY) {
        boolean hovered = isWithin(mouseX, mouseY, x, rowY, width, POPUP_ROW_HEIGHT);
        if (hovered) {
            CatalogSkin.draw(graphics, CatalogSkin.ROW_HOVER, x + 4, rowY, width - 8, POPUP_ROW_HEIGHT);
        }
        CatalogSkin.Sprite box = radio
            ? (selected ? CatalogSkin.RADIO_ON : CatalogSkin.RADIO_OFF)
            : (selected ? CatalogSkin.CHECK_ON : CatalogSkin.CHECK_OFF);
        CatalogSkin.icon(graphics, box, x + 8, rowY + 4, 0xFFFFFFFF);
        int color = selected ? CatalogSkin.ACCENT : hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT;
        graphics.drawString(font, clip(title, width - 34), x + 22, rowY + 5, color, false);
    }

    private void renderTextLink(LegacyGuiGraphics graphics, String text, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isWithin(mouseX, mouseY, x - 2, y - 3, font.width(text) + 4, 14);
        graphics.drawString(font,
            hovered ? Component.literal(text).withStyle(style -> style.withUnderlined(true)) : Component.literal(text),
            x, y, hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.ACCENT, false);
    }

    private void renderCategoryTab(LegacyGuiGraphics graphics, String title, int x, int y, int width, int height, boolean selected, boolean hovered) {
        if (hovered && !selected) {
            CatalogSkin.draw(graphics, CatalogSkin.TAB_HOVER, x, y, width, height);
        }
        int color = selected ? CatalogSkin.ACCENT : hovered ? CatalogSkin.TEXT_BRIGHT : CatalogSkin.TEXT_MUTED;
        String text = clip(title, width - 6);
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, y + (height - 8) / 2, color, false);
        if (selected) {
            graphics.fill(x + 3, y + height - 2, x + width - 3, y + height, CatalogSkin.ACCENT);
        }
    }

    private void renderFilterPopup(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<FilterCategory> categories = filterCategories();
        FilterCategory activeCategory = activeFilterCategory(categories);
        List<FilterOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int rows = filterRows(options.size());
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = filterPopupHeight(rows);
        overlayController.setModalBounds(x, y, width, height);
        overlayController.setModalMetrics(options.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height);
        renderFilterCategoryBar(graphics, categories, activeCategory, mouseX, mouseY);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_material_filters").getString(), width - 12),
                x + 6, filterOptionsY() + 7, CatalogSkin.TEXT_DIM, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = overlayController.modalScroll() + row;
            if (index >= options.size()) {
                break;
            }
            int rowY = filterOptionsY() + row * POPUP_ROW_HEIGHT;
            FilterOption option = options.get(index);
            renderOptionRow(graphics, x, rowY, width, option.title(), selectedFilters.get(page).contains(option.id()), false, mouseX, mouseY);
        }
        overlayRenderer.renderScrollBar(graphics, x, filterOptionsY(), width, rows, options.size(),
            overlayController.modalScroll(), rows * POPUP_ROW_HEIGHT);
    }

    private void renderFilterCategoryBar(LegacyGuiGraphics graphics, List<FilterCategory> categories, FilterCategory activeCategory, int mouseX, int mouseY) {
        int x = filterCategoryAreaX();
        int y = popupY() + 2;
        int width = filterCategoryAreaWidth();
        int height = CATEGORY_BAR_HEIGHT - 4;
        CatalogSkin.copperDivider(graphics, popupX() + 4, popupY() + CATEGORY_BAR_HEIGHT - 2, popupWidth() - 8);
        clampFilterCategoryScroll(categories);
        graphics.enableScissor(x, y, x + width, y + height);
        int buttonX = x - filterCategoryScroll;
        for (FilterCategory category : categories) {
            int buttonWidth = filterCategoryButtonWidth(category);
            boolean selected = activeCategory != null && activeCategory.id().equals(category.id());
            boolean hovered = isWithin(mouseX, mouseY, buttonX, y, buttonWidth, height) && isWithin(mouseX, mouseY, x, y, width, height);
            renderCategoryTab(graphics, category.title(), buttonX, y, buttonWidth, height, selected, hovered);
            buttonX += buttonWidth + CATEGORY_GAP;
        }
        graphics.disableScissor();
        renderTextLink(graphics, Component.translatable("screen.tinkers_construct_filter.clear").getString(), clearFilterX() + 6, popupY() + 6, mouseX, mouseY);
    }

    private void renderSortPopup(LegacyGuiGraphics graphics, int mouseX, int mouseY) {
        List<SortCategory> categories = sortCategories();
        SortCategory activeCategory = activeSortCategory(categories);
        List<SortOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int rows = sortRows(options.size());
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = sortPopupHeight(rows);
        overlayController.setModalBounds(x, y, width, height);
        overlayController.setModalMetrics(options.size(), rows);
        overlayRenderer.renderPanelFrame(graphics, x, y, width, height);
        renderSortCategoryBar(graphics, categories, activeCategory, mouseX, mouseY);
        if (options.isEmpty()) {
            graphics.drawString(font, clip(Component.translatable("screen.tinkers_construct_filter.no_sort_options").getString(), width - 12),
                x + 6, sortOptionsY() + 7, CatalogSkin.TEXT_DIM, false);
            return;
        }
        for (int row = 0; row < rows; row++) {
            int index = overlayController.modalScroll() + row;
            if (index >= options.size()) {
                break;
            }
            int rowY = sortOptionsY() + row * POPUP_ROW_HEIGHT;
            SortOption option = options.get(index);
            renderOptionRow(graphics, x, rowY, width, option.title().getString(), selectedSorts.get(page).equals(option.id()), true, mouseX, mouseY);
        }
        overlayRenderer.renderScrollBar(graphics, x, sortOptionsY(), width, rows, options.size(),
            overlayController.modalScroll(), rows * POPUP_ROW_HEIGHT);
    }

    private void renderSortCategoryBar(LegacyGuiGraphics graphics, List<SortCategory> categories, SortCategory activeCategory, int mouseX, int mouseY) {
        int x = sortCategoryAreaX();
        int y = popupY() + 2;
        int width = sortCategoryAreaWidth();
        int height = CATEGORY_BAR_HEIGHT - 4;
        CatalogSkin.copperDivider(graphics, popupX() + 4, popupY() + CATEGORY_BAR_HEIGHT - 2, popupWidth() - 8);
        clampSortCategoryScroll(categories);
        graphics.enableScissor(x, y, x + width, y + height);
        int buttonX = x - sortCategoryScroll;
        for (SortCategory category : categories) {
            int buttonWidth = sortCategoryButtonWidth(category);
            boolean selected = activeCategory != null && activeCategory.id().equals(category.id());
            boolean hovered = isWithin(mouseX, mouseY, buttonX, y, buttonWidth, height) && isWithin(mouseX, mouseY, x, y, width, height);
            renderCategoryTab(graphics, category.title(), buttonX, y, buttonWidth, height, selected, hovered);
            buttonX += buttonWidth + CATEGORY_GAP;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overlayController.isModalOpen()) {
            if (overlayController.modalType() == CatalogOverlayController.ModalType.IMPORTANT
                && importantButton != null
                && isWithin(mouseX, mouseY, importantButton.getX(), importantButton.getY(), importantButton.getWidth(), importantButton.getHeight())) {
                toggleImportantPopup();
                return true;
            }
            if (overlayController.modalType() == CatalogOverlayController.ModalType.FILTER
                && filterButton != null
                && isWithin(mouseX, mouseY, filterButton.getX(), filterButton.getY(), filterButton.getWidth(), filterButton.getHeight())) {
                toggleFilterPopup();
                return true;
            }
            if (overlayController.modalType() == CatalogOverlayController.ModalType.SORT
                && sortButton != null
                && isWithin(mouseX, mouseY, sortButton.getX(), sortButton.getY(), sortButton.getWidth(), sortButton.getHeight())) {
                toggleSortPopup();
                return true;
            }
            if (overlayController.isOverModal(mouseX, mouseY)) {
                switch (overlayController.modalType()) {
                    case HISTORY -> handleHistoryClick(mouseX, mouseY);
                    case FILTER -> handleFilterClick(mouseX, mouseY);
                    case SORT -> handleSortClick(mouseX, mouseY);
                    case IMPORTANT -> handleImportantClick(mouseX, mouseY);
                    default -> {
                    }
                }
                return true;
            }
            overlayController.closeModal();
            if (!isWithin(mouseX, mouseY, contentX, contentY, searchFrameWidth, TOOLBAR_HEIGHT)) {
                commitSearchHistoryIfNeeded();
                if (searchBox != null && searchBox.isFocused()) {
                    searchBox.setFocus(false);
                    if (getFocused() == searchBox) {
                        setFocused(null);
                    }
                }
                return true;
            }
        }

        if (searchBox != null && searchBox.isFocused() && !isWithin(mouseX, mouseY, contentX, contentY, searchFrameWidth, TOOLBAR_HEIGHT)) {
            commitSearchHistoryIfNeeded();
            searchBox.setFocus(false);
            if (getFocused() == searchBox) {
                setFocused(null);
            }
        }

        if (button == 0) {
            if (!searchQuery.isEmpty() && isOverSearchClear(mouseX, mouseY)) {
                searchBox.setValue("");
                setFocused(searchBox);
                return true;
            }

            if (isOverListScrollbar(mouseX, mouseY)) {
                draggingListScrollbar = true;
                updateListScrollFromMouse(mouseY);
                return true;
            }

            if (overlayController.hasDetail() && isOverDetailScrollbar(mouseX, mouseY)) {
                draggingDetailScrollbar = true;
                updateDetailScrollFromMouse(mouseY);
                return true;
            }

            if (overlayController.isOverPinButton(mouseX, mouseY)) {
                overlayController.togglePin();
                return true;
            }

            if (overlayController.isOverDetail(mouseX, mouseY)) {
                if (hoveredJumpTarget != null && jumpToEntry(hoveredJumpTarget)) {
                    return true;
                }
                if (!overlayController.isDetailPinned()) {
                    overlayController.setDetailPinned(true);
                }
                return true;
            }

            CatalogEntry rowEntry = entryAtRow(mouseX, mouseY);
            if (rowEntry != null) {
                commitSearchHistoryIfNeeded();
                if (overlayController.inspectedEntry() == rowEntry && overlayController.isDetailPinned()) {
                    overlayController.setDetailPinned(false);
                } else {
                    selectDetailEntry(rowEntry, true);
                }
                return true;
            }

            FilterChip chip = chipAt(mouseX, mouseY);
            if (chip != null) {
                selectedFilters.get(page).remove(chip.id());
                rebuildVisibleEntries();
                return true;
            }

            if (searchBox != null && isWithin(mouseX, mouseY, contentX, contentY, searchFrameWidth, TOOLBAR_HEIGHT)) {
                if (!ClientConfig.getSearchHistory().isEmpty()) {
                    overlayController.openModal(CatalogOverlayController.ModalType.HISTORY);
                }
                if (!isWithin(mouseX, mouseY, searchBox.x, searchBox.y, searchBox.getWidth(), searchBox.getHeight())) {
                    setFocused(searchBox);
                    searchBox.moveCursorToEnd();
                    return true;
                }
            } else if (!splitView) {
                clearDetailSelection();
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingListScrollbar) {
            updateListScrollFromMouse(mouseY);
            return true;
        }
        if (button == 0 && draggingDetailScrollbar) {
            updateDetailScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingListScrollbar = false;
        draggingDetailScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverListScrollbar(double mouseX, double mouseY) {
        int rows = visibleRows();
        if (visibleEntries.size() <= rows) {
            return false;
        }
        int barX = contentX + listFrameWidth - SCROLL_GUTTER - 2;
        return isWithin(mouseX, mouseY, barX, listY, 8, listBottom - listY);
    }

    private void updateListScrollFromMouse(double mouseY) {
        int rows = visibleRows();
        int maxScroll = Math.max(0, visibleEntries.size() - rows);
        if (maxScroll <= 0) {
            mainScroll = 0;
            return;
        }
        int trackH = Math.max(1, listBottom - listY);
        int thumbH = Math.max(12, trackH * rows / visibleEntries.size());
        double ratio = (mouseY - listY - thumbH / 2.0) / Math.max(1.0, trackH - thumbH);
        mainScroll = clamp((int) Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private boolean isOverDetailScrollbar(double mouseX, double mouseY) {
        if (overlayController.detailMaximumScroll() <= 0) {
            return false;
        }
        int barX = detailX + detailWidth - 11;
        int top = detailY + 35;
        int bottom = detailY + detailHeight - 6;
        return splitView && isWithin(mouseX, mouseY, barX, top, 8, Math.max(0, bottom - top));
    }

    private void updateDetailScrollFromMouse(double mouseY) {
        int maxScroll = overlayController.detailMaximumScroll();
        if (maxScroll <= 0) {
            return;
        }
        int top = detailY + 35;
        int trackH = Math.max(1, detailY + detailHeight - 6 - top);
        int thumbH = Math.max(12, trackH * overlayController.detailViewportHeight() / Math.max(1, overlayController.detailContentHeight()));
        double ratio = (mouseY - top - thumbH / 2.0) / Math.max(1.0, trackH - thumbH);
        overlayController.setDetailScroll((int) Math.round(ratio * maxScroll));
    }

    private void handleHistoryClick(double mouseX, double mouseY) {
        int row = (int) ((mouseY - historyY() - POPUP_HEADER_HEIGHT) / POPUP_ROW_HEIGHT);
        if (row < 0 || row >= historyRows()) {
            return;
        }
        List<String> history = ClientConfig.getSearchHistory();
        int index = overlayController.modalScroll() + row;
        if (index >= 0 && index < history.size()) {
            String value = history.get(index);
            if (isHistoryDeleteHovered(mouseX, mouseY, historyX(), historyY() + POPUP_HEADER_HEIGHT + row * POPUP_ROW_HEIGHT, historyWidth())) {
                ClientConfig.removeSearch(value);
                if (ClientConfig.getSearchHistory().isEmpty()) {
                    overlayController.closeModal();
                }
                return;
            }
            searchBox.setValue(value);
            ClientConfig.rememberSearch(value);
            lastCommittedSearch = value.trim();
            overlayController.closeModal();
        }
    }

    private boolean isHistoryDeleteHovered(double mouseX, double mouseY, int x, int rowY, int width) {
        return isWithin(mouseX, mouseY, x + width - HISTORY_DELETE_WIDTH, rowY, HISTORY_DELETE_WIDTH, POPUP_ROW_HEIGHT);
    }

    private void handleImportantClick(double mouseX, double mouseY) {
        List<ImportantPartOption> options = importantPartOptions();
        if (isWithinImportantHeader(mouseX, mouseY)) {
            if (mouseX >= importantSelectAllX() && mouseX < importantSelectAllX() + importantSelectAllWidth()) {
                importantSelection().clear();
                for (ImportantPartOption option : options) {
                    importantSelection().add(option.id());
                }
                if (overlayController.inspectedEntry() != null) {
                    refreshInspectedCache(overlayController.inspectedEntry());
                }
            } else if (mouseX >= importantClearX() && mouseX < importantClearX() + importantClearWidth()) {
                importantSelection().clear();
                if (overlayController.inspectedEntry() != null) {
                    refreshInspectedCache(overlayController.inspectedEntry());
                }
            }
            return;
        }
        int row = (int) ((mouseY - importantOptionsY()) / POPUP_ROW_HEIGHT);
        int index = overlayController.modalScroll() + row;
        if (row >= 0 && row < importantRows(options.size()) && index < options.size()) {
            String id = options.get(index).id();
            if (!importantSelection().add(id)) {
                importantSelection().remove(id);
            }
            if (overlayController.inspectedEntry() != null) {
                refreshInspectedCache(overlayController.inspectedEntry());
            }
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
                    overlayController.setModalScroll(0, 0);
                }
            }
            return;
        }
        FilterCategory activeCategory = activeFilterCategory(categories);
        List<FilterOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int row = (int) ((mouseY - filterOptionsY()) / POPUP_ROW_HEIGHT);
        int index = overlayController.modalScroll() + row;
        if (row >= 0 && row < filterRows(options.size()) && index < options.size()) {
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
                overlayController.setModalScroll(0, 0);
            }
            return;
        }
        SortCategory activeCategory = activeSortCategory(categories);
        List<SortOption> options = activeCategory == null ? List.of() : activeCategory.options();
        int row = (int) ((mouseY - sortOptionsY()) / POPUP_ROW_HEIGHT);
        int index = overlayController.modalScroll() + row;
        if (row >= 0 && row < sortRows(options.size()) && index < options.size()) {
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
            if (overlayController.isOverModal(mouseX, mouseY)) {
                switch (overlayController.modalType()) {
                    case HISTORY, IMPORTANT -> overlayController.scrollModalBy(direction);
                    case FILTER -> {
                        List<FilterCategory> categories = filterCategories();
                        if (isWithinFilterCategoryBar(mouseX, mouseY)) {
                            if (mouseX < clearFilterX()) {
                                int maximum = Math.max(0, filterCategoryContentWidth(categories) - filterCategoryAreaWidth());
                                filterCategoryScroll = clamp(filterCategoryScroll + direction * 18, 0, maximum);
                            }
                        } else {
                            overlayController.scrollModalBy(direction);
                        }
                    }
                    case SORT -> {
                        List<SortCategory> categories = sortCategories();
                        if (isWithinSortCategoryBar(mouseX, mouseY)) {
                            int maximum = Math.max(0, sortCategoryContentWidth(categories) - sortCategoryAreaWidth());
                            sortCategoryScroll = clamp(sortCategoryScroll + direction * 18, 0, maximum);
                        } else {
                            overlayController.scrollModalBy(direction);
                        }
                    }
                    default -> {
                    }
                }
            }
            return true;
        }

        if (overlayController.hasDetail() && overlayController.isOverDetail(mouseX, mouseY)) {
            overlayController.scrollDetailBy(direction * CatalogOverlayContent.SCROLL_STEP);
            return true;
        }

        if (isWithin(mouseX, mouseY, contentX, listY, listFrameWidth, listBottom - listY)) {
            if (!splitView) {
                clearDetailSelection();
            }
            mainScroll = clamp(mainScroll + direction, 0, Math.max(0, visibleEntries.size() - visibleRows()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commitSearchHistoryIfNeeded();
            overlayController.closeModal();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (overlayController.isModalOpen()) {
                overlayController.closeModal();
                return true;
            }
            if (overlayController.isDetailPinned()) {
                overlayController.setDetailPinned(false);
                if (!splitView) {
                    clearDetailSelection();
                }
                return true;
            }
        }
        if (!(searchBox != null && searchBox.isFocused())) {
            ItemStack hoveredStack = hoveredItemForJei();
            if (!hoveredStack.isEmpty() && JeiCompat.handleKeyPressed(hoveredStack, keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        commitSearchHistoryIfNeeded();
        super.removed();
    }

    @Override
    public void onClose() {
        commitSearchHistoryIfNeeded();
        Minecraft.getInstance().setScreen(parent);
    }

    private void commitSearchHistoryIfNeeded() {
        String trimmed = searchQuery == null ? "" : searchQuery.trim();
        if (!trimmed.isEmpty() && !trimmed.equals(lastCommittedSearch)) {
            ClientConfig.rememberSearch(trimmed);
            lastCommittedSearch = trimmed;
        }
    }

    private boolean jumpToEntry(CatalogOverlayContent.JumpTarget target) {
        if (target == null || target.entryId() == null || target.entryId().isEmpty()) {
            return false;
        }
        Page targetPage = switch (target.targetPage()) {
            case "materials" -> Page.MATERIALS;
            case "parts" -> Page.PARTS;
            case "modifiers" -> Page.MODIFIERS;
            case "traits" -> Page.TRAITS;
            default -> null;
        };
        if (targetPage == null) {
            return false;
        }
        List<CatalogEntry> pool = switch (targetPage) {
            case MATERIALS -> snapshot.materials();
            case PARTS -> snapshot.parts();
            case MODIFIERS -> snapshot.modifiers();
            case TRAITS -> snapshot.traits();
        };
        CatalogEntry found = null;
        for (CatalogEntry candidate : pool) {
            if (target.entryId().equals(candidate.getId())) {
                found = candidate;
                break;
            }
        }
        if (found == null) {
            return false;
        }
        commitSearchHistoryIfNeeded();
        if (page != targetPage) {
            page = targetPage;
            filterCategoryScroll = 0;
            sortCategoryScroll = 0;
            overlayController.closeModal();
            clearDetailSelection();
            updateNavigationButtons();
            updateOrderButton();
            rebuildVisibleEntries();
        }
        if (!visibleEntries.contains(found)) {
            selectedFilters.get(targetPage).clear();
            if (searchBox != null && !searchQuery.isEmpty()) {
                searchBox.setValue("");
            } else {
                searchQuery = "";
                rebuildVisibleEntries();
            }
        }
        int index = visibleEntries.indexOf(found);
        if (index >= 0) {
            int centerOffset = Math.max(0, visibleRows() / 2 - 1);
            mainScroll = clamp(index - centerOffset, 0, Math.max(0, visibleEntries.size() - visibleRows()));
        }
        selectDetailEntry(found, true);
        return true;
    }

    private ItemStack hoveredItemForJei() {
        if (overlayController.isModalOpen()) {
            return ItemStack.EMPTY;
        }
        if (!hoveredDetailItem.isEmpty()) {
            return hoveredDetailItem;
        }
        if (overlayController.hasDetail() && overlayController.inspectedEntry() != null
            && isWithin(lastMouseX, lastMouseY, overlayController.detailX() + 9, overlayController.detailY() + 6, 22, 22)) {
            return overlayController.inspectedEntry().getDisplayStack();
        }
        CatalogEntry rowEntry = entryAtRow(lastMouseX, lastMouseY);
        return rowEntry != null ? rowEntry.getDisplayStack() : ItemStack.EMPTY;
    }

    private int searchClearX() {
        return contentX + searchFrameWidth - 15;
    }

    private boolean isOverSearchClear(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, searchClearX(), contentY + 3, 13, 14);
    }

    private CatalogEntry entryAtRowSlot(double mouseX, double mouseY) {
        CatalogEntry entry = entryAtRow(mouseX, mouseY);
        if (entry == null) {
            return null;
        }
        int rowOffset = (int) ((mouseY - listY) % ROW_HEIGHT);
        if (mouseX >= rowX + 5 && mouseX < rowX + 23 && rowOffset >= 5 && rowOffset < 23) {
            return entry;
        }
        return null;
    }

    private CatalogEntry entryAtRow(double mouseX, double mouseY) {
        if (!snapshot.fullyLoaded() || (!splitView && overlayController.isOverDetail(mouseX, mouseY))
            || !isWithin(mouseX, mouseY, rowX, listY, rowWidth, listBottom - listY)) {
            return null;
        }
        int index = mainScroll + (int) ((mouseY - listY) / ROW_HEIGHT);
        return index >= 0 && index < visibleEntries.size() ? visibleEntries.get(index) : null;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listY) / ROW_HEIGHT);
    }

    private LinkedHashSet<String> importantSelection() {
        return page == Page.TRAITS ? selectedTraitSources : selectedImportantPartTypes;
    }

    private List<ImportantPartOption> importantPartOptions() {
        if (page == Page.TRAITS) {
            return List.of(
                new ImportantPartOption("materials", Component.translatable("button.tinkers_construct_filter.materials").getString()),
                new ImportantPartOption("parts", Component.translatable("button.tinkers_construct_filter.parts").getString()),
                new ImportantPartOption("modifiers", Component.translatable("button.tinkers_construct_filter.modifiers").getString()),
                new ImportantPartOption("tools", Component.translatable("screen.tinkers_construct_filter.innate_tool_traits").getString()));
        }
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
        importantSelection().retainAll(available);
    }

    private int importantPopupX() {
        return contentX;
    }

    private int importantPopupY() {
        int bottom = importantButton.getY() + importantButton.getHeight();
        return Math.max(contentY, bottom - importantPopupHeight(importantOptionCount));
    }

    private int importantPopupWidth() {
        return Math.min(260, Math.max(IMPORTANT_POPUP_MIN_WIDTH, contentWidth - 8));
    }

    private int importantRows(int optionCount) {
        int availableRows = Math.max(3, (listBottom - contentY - 8 - POPUP_HEADER_HEIGHT) / POPUP_ROW_HEIGHT);
        return Math.max(3, Math.min(14, Math.min(availableRows, Math.max(3, optionCount))));
    }

    private int importantPopupHeight(int optionCount) {
        return POPUP_HEADER_HEIGHT + importantRows(optionCount) * POPUP_ROW_HEIGHT + 3;
    }

    private int importantOptionsY() {
        return importantPopupY() + POPUP_HEADER_HEIGHT;
    }

    private int importantSelectAllX() {
        return importantClearX() - importantSelectAllWidth() - 8;
    }

    private int importantSelectAllWidth() {
        return font.width(Component.translatable("screen.tinkers_construct_filter.select_all"));
    }

    private int importantClearX() {
        return importantPopupX() + importantPopupWidth() - importantClearWidth() - 6;
    }

    private int importantClearWidth() {
        return font.width(Component.translatable("screen.tinkers_construct_filter.clear"));
    }

    private boolean isWithinImportantHeader(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, importantPopupX(), importantPopupY(), importantPopupWidth(), POPUP_HEADER_HEIGHT);
    }

    private int popupX() {
        return contentX + Math.max(0, contentWidth - popupWidth());
    }

    private int popupY() {
        return contentY + TOOLBAR_HEIGHT + 3;
    }

    private int popupWidth() {
        return Math.min(300, contentWidth);
    }

    private int filterRows(int optionCount) {
        int maximum = Math.max(4, Math.min(12, (panelY + panelHeight - popupY() - 10 - CATEGORY_BAR_HEIGHT) / POPUP_ROW_HEIGHT));
        return Math.max(2, Math.min(maximum, optionCount));
    }

    private int filterPopupHeight(int rows) {
        return CATEGORY_BAR_HEIGHT + rows * POPUP_ROW_HEIGHT + 3;
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

    private int sortRows(int optionCount) {
        int maximum = Math.max(4, Math.min(12, (panelY + panelHeight - popupY() - 10 - CATEGORY_BAR_HEIGHT) / POPUP_ROW_HEIGHT));
        return Math.max(2, Math.min(maximum, optionCount));
    }

    private int sortPopupHeight(int rows) {
        return CATEGORY_BAR_HEIGHT + rows * POPUP_ROW_HEIGHT + 3;
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
        return contentX;
    }

    private int historyY() {
        return contentY + TOOLBAR_HEIGHT + 3;
    }

    private int historyWidth() {
        return searchFrameWidth;
    }

    private int historyRows() {
        int availableRows = Math.max(1, (height - historyY() - 24) / POPUP_ROW_HEIGHT);
        return Math.min(ClientConfig.getSearchHistoryVisibleRows(), availableRows);
    }

    private String clip(String value, int maxWidth) {
        if (maxWidth <= 0 || value == null || value.isEmpty() || font.width(value) <= maxWidth) {
            return maxWidth <= 0 || value == null ? "" : value;
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
        MODIFIERS,
        TRAITS
    }

    private record FilterOption(String id, String title, Predicate<CatalogEntry> predicate) {
    }

    private record FilterCategory(String id, String title, List<FilterOption> options, FilterMatchMode mode) {
        private FilterCategory(String id, String title, List<FilterOption> options) {
            this(id, title, options, FilterMatchMode.ALL);
        }
    }

    private record SortCategory(String id, String title, List<SortOption> options) {
    }

    private record ImportantPartOption(String id, String title) {
    }

    private record FilterChip(String id, String title) {
    }

    private record SortOption(String id, Component title, Boolean numeric, String attributeId, Function<CatalogEntry, Object> valueGetter, boolean defaultOrder) {
    }
}
