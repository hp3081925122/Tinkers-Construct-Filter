# 1.4

## English

- **Custom Forged-Iron & Copper Pixel UI Redesign**: Ported the 256×256 pixel-art catalog UI (`catalog_atlas.png`) and three-column inspector layout to 1.19.2, complete with split/compact auto-layout, stat grids, interactive trait pills, and item grids.
- **Unified Workstation Entry Buttons**: Added 22×22 catalog entry buttons to the Tinker Station/Anvil (opens Materials), Part Builder (opens Parts), and Modifier Worktable (opens Modifiers), with JEI area exclusion and outside-click drop protection.
- **Corrected Material & Tool Stats**:
  - Fixed `handle_durability` and `grip_durability` rounding to `0`; unified Handle stats and Grip durability to signed percentages (`+15%`, `-10%`, `+0%`, "Bonus" instead of "Multiplier").
  - Fixed `grip_melee_damage` Chinese translation, added explicit signs (`+0.10`) for Limb and Grip accuracy stats, scaled Plating knockback resistance by `×10`, omitted `0` armor from Shield plating so it does not overwrite Chestplate armor, and added Skull stats plus dynamic parsing for third-party addon `IMaterialStats`.
- **Complete Material, Modifier & Trait Info**:
  - Added craftable material items, melting/casting fluid names with temperatures in `°C`, fallback descriptions and default traits for non-part materials, and extra stat cards with reverse trait indexing.
  - Added modifier slot count & type summaries (including Upgrade, Ability, Defense, Soul, and custom addon slots), level caps (`Max Lv.5`, `Single Level`, `No Level Cap`), and new sort options on the Modifiers and Traits pages.
- **Interactive Navigation, Search History & JEI**:
  - Added cross-page click-to-jump from trait pills, part materials, and trait source materials/parts.
  - Auto-committed search history on Enter, focus loss, and page switches.
  - Added optional JEI `R` / `U` keybind lookup when hovering any item in the catalog.
- **Multiplayer & Dependency Fixes**: Added `displayTest = "IGNORE_SERVER_VERSION"` for client-side multiplayer compatibility and cleaned up `mods.toml` dependency constraints.

---

## 中文

- **全新锻铁铜边像素风 UI 同步重做**：将 256×256 像素图集（`catalog_atlas.png`）与三栏详情面板完整同步至 1.19.2，支持宽屏分栏与小窗口自动适配、点线对齐属性表、可交互词条胶囊及物品网格。
- **三大工作台快捷入口**：为工匠站/工匠砧（直达材料页）、部件加工台（直达部件页）、强化调配台（直达强化页）统一添加右上角 22×22 图鉴按钮，支持 JEI 避让区域注册与防误丢物品保护。
- **材料与部件属性数值修正**：
  - 修复手柄耐久与握把耐久四舍五入变成 `0` 的问题；将手柄四项属性及握把耐久统一改为带正负号的百分比加成显示（如 `-10%`、`+15%`），将“倍率”更正为“加成”。
  - 修正“握把近战伤害”翻译，为弓臂与握把精准度补全显式正负号，将镶板击退抗性按 `×10` 换算显示，屏蔽盾牌镶板无意义的 `0` 护甲以避免覆盖胸甲护甲，并补全头颅属性与第三方附属自定义 `IMaterialStats` 动态解析。
- **材料、强化与词条信息补全**：
  - 补全材料代表物品列表、熔炼/浇筑流体及摄氏温度显示、无部件材料的通用说明与默认词条展示，以及额外统计卡片的词条反查。
  - 补全强化配方槽位消耗数量与类型（升级槽、能力槽、防御槽、灵魂槽及附属自定义槽位）与等级上限显示，并为强化页与词条页新增多维度排序选项。
- **跨页跳转、搜索历史与 JEI 联动**：
  - 支持点击词条胶囊、部件所属材料、词条来源材料/部件直接跳转定位到对应页面与条目。
  - 修复搜索框回车、失焦、切页时自动保存搜索历史。
  - 新增悬停图鉴内物品按 `R` / `U` 呼出 JEI 配方/用途的可选联动。
- **联机与依赖修复**：补充 `displayTest = "IGNORE_SERVER_VERSION"` 修复单端加装联机红叉，优化 `mods.toml` 依赖约束。

# 1.2

## English

- Added production-method filters for materials and parts, using loaded recipes without addon-specific exceptions.
- Changed trait introductions in Traits and Modifiers tooltips to underlined names with hover descriptions.
- Expanded KubeJS support across all four catalogs: custom filter categories, filters, sorting, trait-category overrides and read-only entry data. Added examples and documentation.
- Improved multi-select filtering: material tiers and part types use OR within their category, while different categories combine with AND.
- Added a catalog-only dark iron and copper UI theme, then reduced mottled noise and stretched grain for clearer panels, buttons and tooltips.
- Added bilingual Wiki documentation and catalog-extension regression checks.

## 中文

- 为匠魂材料和部件新增制作方式筛选，依据实际加载的配方识别，不添加附属模组特判。
- 匠魂词条与匠魂强化悬浮框中的词条介绍改为带下划线名称，鼠标悬停显示详情。
- 扩展四个图鉴页面的 KubeJS 支持：自定义筛选分类、筛选、排序、词条分类覆盖及只读条目数据，并提供示例和文档。
- 改进多选筛选：材料等级和部件类型在同一分类内取并集，不同分类之间取交集。
- 新增图鉴专用深灰铁质与细铜边主题，减少斑驳噪点和拉伸条纹，提高面板、按钮及悬浮框的可读性。
- 补充中英文 Wiki 文档及图鉴扩展回归检查。

# 1.1

## English

- Added a separate Traits catalog; the Modifiers catalog now only includes entries with actual modifier recipes.
- Added Attack, Defense, Resource and Other trait filters. Known multi-effect traits can appear in multiple categories; unclassified addon traits fall back to Other.
- Added acquisition-source options to trait tooltips: matching materials, materialized parts, modifier ingredients and applicable tools. Pinned tooltips retain scrolling and item details.
- Removed slot labels from the Traits page and reduced Traits/Modifiers sorting to Default Trait Order.
- Added applicable-tool associations for part filtering, based on loaded tool definitions.
- Included search-history and version-specific rendering/development-launch fixes.
- Updated Chinese and English interface text and added trait-category regression checks.

## 中文

- 新增独立的匠魂词条列表；匠魂强化仅保留存在实际强化配方的条目。
- 新增攻击、防御、资源及其他词条筛选。已归类的多效果词条可属于多个类别；未归类的附属词条默认进入其他。
- 新增词条获取来源选项，悬浮窗可展示关联材料、对应材质的部件、强化消耗材料及适用工具，保留点击固定、滚轮及物品详情功能。
- 移除匠魂词条页的槽位标签；匠魂词条与匠魂强化的排序仅保留默认词条顺序。
- 根据实际加载的工具定义，为部件筛选建立适用工具关联。
- 纳入搜索历史及各版本的渲染、开发启动修复。
- 更新中英文界面文字，新增词条分类回归检查。
