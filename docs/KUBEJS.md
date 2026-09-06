# KubeJS 图鉴扩展（第一批）

适用于本项目的 Minecraft 1.19.2 Forge 和 1.20.1 Forge，两版接口相同。
脚本放在 `kubejs/client_scripts`，事件为 `TinkersFilterEvents.register(event => { ... })`。
完整示例见 [examples/catalog_extensions.js](examples/catalog_extensions.js)。示例不自动加载。

## 筛选与排序注册

| 方法 | 参数 | 页面 |
|---|---|---|
| materialFilter | id, title, predicate；或 category, id, title, predicate | 匠魂材料 |
| partFilter | id, title, predicate；或 category, id, title, predicate | 匠魂部件 |
| modifierFilter | id, title, predicate；或 category, id, title, predicate | 匠魂强化 |
| traitFilter | id, title, predicate；或 category, id, title, predicate | 匠魂词条 |
| materialSort / partSort / modifierSort / traitSort | id, title, valueGetter | 对应页面 |
| filterCategory | page, id, title, mode | 注册独立筛选分类 |
| traitCategories | modifierId, categories | 覆盖某个词条的效果归类 |

- `page` 只接受 `materials`、`parts`、`modifiers`、`traits`。
- `mode` 为 `any`（勾选条件任意满足）或 `all`（全部满足）。
- 不同分类之间始终取交集；未勾选选项的分类不限制结果。
- 分类需要注册；分类与筛选的注册先后不影响最终结果。引用未注册分类的选项会跳过，并记录英文错误日志。
- 没有指定分类的三个参数筛选入口使用保留分类 `custom`，默认 `all`。这保留旧脚本的组合语义。
- 如主动注册 `custom` 分类，可改变默认自定义分类的标题和规则。其他分类不会覆盖内置分类。
- 分类标题按首次出现有效筛选项的顺序展示；没有有效筛选项的分类不显示。
- 筛选 ID 按页面隔离，同页面同 ID 后注册覆盖前注册（即使换了分类）；排序 ID 同理。建议使用自己的命名空间前缀。
- `title` 是脚本提供的显示文本，空标题回退 ID。动态语言键标题不属于这批功能。
- 谓词返回布尔值。排序返回有限数字、文本或 `null`；空值始终排末尾。
- 数字默认降序、文本默认升序，玩家可以切换；默认方向与多级排序配置不在本批范围。
- 筛选在列表重建时执行；排序值对每条候选条目只计算一次，不在比较器中反复调用脚本。
- 回调应保持纯计算，不扫描整个配方表、写文件、修改条目或触发目录刷新。
- 某个回调异常会记录一次日志并停用该定义；失效筛选放行条目，失效排序返回空值，不把整个目录拖垮。

## 条目数据

所有页面都可读取 `getId()`、`getName()`、`getMaterialLevel()`、
`getTraitNames()`、`getTraitDescriptions()`、`getAttributeValues()`、`getAttributeTexts()`。

### 材料与部件新增词条索引

- `getTraitIds()`：只读词条 ID 集合。
- `getTraitLevels()`：只读 `Map<String, Integer>`，完整词条 ID 对应等级。
- 部件：以匠魂按该部件属性类型返回的词条为准。有专用定义时替代默认定义，不把默认词条再混入。
- 材料：默认定义与可用部件定义的汇总；同词条取最高等级，不相加。
- 这里不是组装后的工具总等级。要精确判断某种部件，请使用部件页接口。
- 强化、词条目录条目不是已安装的工具词条，不虚构安装等级；它们的上述材料词条索引为空，自己的词条 ID 使用 `getId()`。
- 已有 `getTraitNames()` 等方法继续保留。

材料已有 `getAvailablePartCount()`、`getDefaultSortOrder()`、`getMaterialDisplayItem()`、
`getProductionMethods()`。
部件已有 `getMaterialId()`、`getMaterialName()`、`getPartId()`、`getPartType()`、
`getPartTypeName()`、`getMaterializedItem()`、`getToolCategories()`、`getProductionMethods()`。

制作方式标识为 `part_builder`、`casting`、`composite_casting`、`unknown`。
材料制作方式是所有可用部件的并集，不意味着每个部件都能用该方式制作。
部件的 `getToolCategories()` 返回可以使用该部件组装的工具 ID → 名称映射。

### 强化与词条回调

两页回调接收同一个只读 `ModifierView`，注册入口决定显示页面：

| 方法 | 含义 |
|---|---|
| hasModifierRecipe() | 是否有可展示强化配方 |
| getEffectCategories() | 脚本覆盖后生效的效果分类 ID 列表 |
| getSlotCategories() | 槽位分类标识 → 标题 |
| getToolCategories() | 适用工具分类标识 → 标题 |
| getModifierDescriptions() | 介绍文本 |
| getRecipeTools() | 强化配方适用工具展示物品 |
| getRecipeVariants() | 配方变体列表，每个变体的 inputSlots() 返回输入槽候选物品列表 |
| getSourceMaterials() | 带有该词条的材料条目 |
| getSourceParts() | 带有该词条的材料化部件条目 |
| getSourceTools() | 工具定义自身提供该词条的工具，不包括材料来源或后加强化 |

来源材料、部件条目可读取通用条目方法。物品返回副本，不能借修改展示物品改变真实配方。
这些集合是 Java 集合，示例使用 `isEmpty()`、`size()`、`contains()`、`get()`。

## 词条效果归类

`traitCategories(modifierId, categories)` 是展示归类，不修改实际效果：

- `modifierId` 必须是完整资源 ID，不限定模组命名空间。
- 分类只接受 `attack`、`defense`、`resource`、`other`。
- 多分类使用数组，例如 `['attack', 'defense']`；重复值去重。
- 明确分类与 `other` 同时提供时，删除 `other`；空数组归为 `other`。
- 脚本设置覆盖该词条的内置归类；没有脚本设置则使用对应 MC 版本的内置归类。
- 拼错分类会明确报错，不静默回退；尚未加载或不存在的词条 ID 不会创建新词条。
- 需要“远程”“采矿”等额外自定义分类时，使用 `filterCategory` 和 `traitFilter`，本接口只调整已有四个效果分类。

## 本批界面变化

- 材料等级、部件类型多选改为并集，不再因为勾选两个互斥值得到零结果。
- 制作方式、部件对应工具、词条效果类型维持并集。
- 强化槽位、强化适用工具维持原有交集。
- 旧部件脚本筛选移到独立“自定义”分类，不再混在内置部件类型中。
- 不修改消耗材料、适用工具和制作方式的真实配方识别，不添加任何附属模组特判。

## 加载与验证

定义在目录重建时清空并重新触发注册事件，不会累积重复项。
修改脚本后最稳妥的测试方式是重启客户端，再重新打开目录。
普通服务端 `/reload` 不等于客户端脚本重载，本批未新增热重载事件。
若使用客户端脚本重载机制，仍需关闭旧目录并重新打开，不能假定当前打开的页面会立即刷新。

在 Java 17 下运行 `gradlew.bat catalogExtensionTest` 可验证注册、分类规则、页面隔离、
归类覆盖、异常停用与重建恢复；不会启动游戏或执行发布打包。
测试会主动触发标注为预期的错误日志，成功标准以最终测试通过消息及进程退出码为准。

游戏内还需验证：

1. 分别打开四页，确认示例自定义分类和排序项出现。
2. 材料“发展阶段”同时勾选两个条件时，结果为两个范围的并集。
3. 部件“加工条件”同时勾选两个条件时，结果同时满足两个条件；跨分类仍取交集。
4. 强化与词条的自定义排序数值、升降序和空值位置正确。
5. 归类覆盖只影响指定词条；移除脚本并重启后恢复内置分类。
6. 切换筛选标签、滚动及悬停弹层时，确认遮挡区域不触发底层条目提示。
