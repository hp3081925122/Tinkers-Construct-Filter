# 图鉴界面材质

本次只改变图鉴外观，不改筛选、排序、配方识别和鼠标命中范围。两版使用相同的锻铁暖铜图集，不覆盖 Minecraft 全局按钮材质。

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

生成提示词：

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
