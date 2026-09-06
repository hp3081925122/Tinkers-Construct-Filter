# 图鉴界面材质

本次只改变图鉴外观，不改筛选、排序、配方识别和鼠标命中范围。两版使用相同的锻铁暖铜图集，不覆盖 Minecraft 全局按钮材质。

## 清爽版调整

原图中心的斑驳颗粒在九宫格拉伸后形成大块色斑和横向条纹。本次仅替换两版图集：大面板、侧栏和悬浮框采用近纯色中性深灰，按钮弱化纹理，顶部不再使用大面积棕色。保留细金属边、小铆钉与选中状态的暖铜强调，不修改绘制次数或交互逻辑。

资源处理完成后，正在运行的开发客户端可用 F3+T 重载资源；若客户端已经退出，下次启动时会读取新材质。重点检查大面板无明显色斑、长按钮无拉伸条纹，以及普通、悬停、选中和禁用状态仍可区分。

## 资源与替换

- 游戏资源：`assets/tinkers_construct_filter/textures/gui/catalog_atlas.png`。
- 使用四行四列图集，逻辑尺寸 256 × 256，每格 64 × 64。实际生成文件采用更高分辨率，以归一化 UV 读取，不要求等于逻辑尺寸。
- 各格边角从源格子的 10 个逻辑像素区域取样，绘制为 3 个界面像素；中心区域拉伸，不拉长边角铆钉。
- 资源包可覆盖同路径 PNG 与元数据，需保持四行四列布局。
- 关闭纹理模糊，使用边缘限制。图集只有一张，不在每帧读取文件或创建动态纹理。

| 行 | 从左至右 |
|---|---|
| 1 | 主窗口、侧栏、顶部工具栏、弹层 |
| 2 | 普通按钮、悬停按钮、选中按钮、禁用按钮 |
| 3 | 普通列表行、悬停列表行、普通物品槽、悬停物品槽 |
| 4 | 普通输入框、聚焦输入框、滚动轨道、滑块 |

## 实现范围

`CatalogSkin` 负责图集和九宫格绘制，`CatalogButton` 继承各版本原版按钮，保留点击、键盘和朗读。主界面与弹层渲染器调用统一材质入口。

搜索框只绘制外框，保留原版背景、文字、光标及选区。详情内的词条颜色、物品提示、1.19.2 的物品深度处理保持原有实现。物品槽和滚动条每个只绘制一个材质四边形，其余面板最多九个，避免随物品数量放大九宫格绘制开销。

## 检查

用浏览器打开 [切片预览](catalog-theme-preview.html)，可切换筛选、详情弹层。它用于检查材质比例及对比度，**不是游戏截图或实机验证**。

进入游戏后还需检查：

1. 两版分别打开图鉴，四个页面与筛选、排序、重要选项、历史弹层的材质完整。
2. 切换 GUI 缩放，检查边框、短按钮、长标题及资源包覆盖。
3. 鼠标位于弹层覆盖区域时，底层条目不得高亮、弹出提示或响应点击；移到未覆盖区域后恢复原有行为。
4. 点击固定详情并滚动，检查物品、数量、下划线词条二级提示的层级。
5. 输入、选择搜索文字，检查光标和鼠标定位未发生偏移。

## 美术来源

使用内置图像生成工具制作原始 PNG，未下载第三方游戏材质。生产资源保留生成结果，不进行脚本图像重绘。

初版生成提示词（保留来源记录）：

```text
Use case: stylized-concept.
Asset type: production game GUI nine-slice texture atlas for a Minecraft Tinkers Construct material filter catalog. Generate ONE square opaque PNG atlas, 1024x1024, a strict 4 columns by 4 rows grid of 16 square UI surface sprites. EACH CELL exactly 256x256 pixels, flush to its cell, NO GUTTERS, NO LABELS, no surrounding margin. Actual asset, NOT a screenshot, NOT a scene.
Style: crisp Minecraft-inspired pixel art, dark forged iron workshop with restrained aged copper accents. Fine low-contrast surface grain, calm dark centers so white small text remains very readable. Draw at a logical 256x256 pixel atlas upscaled by 4 using nearest-neighbor look: every logical pixel is a crisp 4x4 square, no antialiasing or blur. Opaque all the way to the canvas edges.
Each sprite has a thin uniform beveled border EXACTLY 12 output pixels (3 logical pixels) wide on each edge, with tiny square corner rivets contained WITHIN that border, no decoration farther inside. Centers MUST be quiet consistent surface texture, no icons, diagonal cracks, text, characters, bright gradients, symbols or focal objects.
ROW 1 left-to-right: outer window dark charcoal hammered iron with dull copper edge; darker sidebar iron with almost black edge; warm dark bronze header panel; darkest graphite popup panel with copper edge.
ROW 2 left-to-right: normal dark iron button; hover button lighter warm gray iron with brighter copper edge; selected button warm dark copper center and golden copper edge; inactive button subdued near-black iron and dim edges.
ROW 3 left-to-right: normal flat dark graphite list row with very subtle border; hovered list row slightly lighter graphite with copper edge; dark recessed item socket; highlighted recessed item socket with warm copper edge.
ROW 4 left-to-right: blackened recessed search field; focused search field with clear copper border; near-black scrollbar track; copper scrollbar thumb.
Keep each shape filling its exact 256 square cell and border completely inside its cell. No spacing between cells. No mockup labels, no text anywhere, no watermark. Visually consistent utilitarian Minecraft inventory UI kit, NOT ornate fantasy, not photorealistic.
```

## 清爽版编辑提示词

使用内置图像生成工具，以项目原图集为编辑目标；保留四行四列布局和切片位置，未使用脚本重绘图片。

```text
Use case: precise-object-edit.
Asset type: production Minecraft GUI 4x4 nine-slice texture atlas.
Image 1 is the EDIT TARGET. Clean up this exact atlas, preserving its square canvas, strict four equal columns and four equal rows, 16 sprite positions, border geometry, and tiny corner rivets. No gutters, no outside margin, no text.
Primary change: REMOVE ALL mottled pixel noise, stone texture, diagonal patterns, brushed streaks, scratches and grain from EVERY center and border. The large inner center of EACH cell must be an absolutely UNIFORM SOLID COLOR, no gradients, no lighting falloff, no dithering. A clean minimalist dark iron UI with subtle copper edges, not dirty or rusty.
Keep hard crisp pixel-aligned edges, simple thin bevels, small square rivets; reduce border detail and contrast. Borders remain inside the outer 15 percent of each cell, leaving central 70 percent completely flat. Copper is restrained and muted, not bright orange.
Row1 left to right: main window solid neutral charcoal #252729 with muted thin copper edge; sidebar solid #1c1e20 with iron edge; header solid neutral #2b2d2f (REMOVE brown fill) with iron edge; popup solid #202224 with muted copper edge.
Row2: normal button solid #33363a iron edge; hover button solid #41454a with muted copper edge; selected button solid #40352a with clear warm copper edge; disabled button solid #232528 dim iron edge.
Row3: list row solid #292c2f with subtle iron edge; hover row solid #34383c with muted copper edge; recessed item slot solid #17191b center iron edge; highlighted slot same flat dark center with muted copper edge.
Row4: input solid #141618 iron edge; focused input same flat center copper edge; scroll track flat near-black; scroll thumb preserve centered narrow copper rectangular bar but make it solid muted copper with flat near-black surround.
Preserve all 16 cell roles and their existing arrangement exactly. Opaque PNG, square atlas, no transparency, no added symbols, no icons, no mockup. NO NOISE ANYWHERE. Large flat color areas are essential because this atlas will be stretched behind text.
```
