// 放入 kubejs/client_scripts；仅改变图鉴筛选、排序和分类，不修改配方。
TinkersFilterEvents.register(event => {
  // 材料发展阶段：同分类选择多个等级时取并集。
  event.filterCategory('materials', 'pack:stage', '发展阶段', 'any')
  event.materialFilter('pack:stage', 'pack:early', '材料等级 0–2', view => view.getMaterialLevel() <= 2)
  event.materialFilter('pack:stage', 'pack:late', '材料等级 3 及以上', view => view.getMaterialLevel() >= 3)

  // 部件制作方式读取已有真实配方索引，与内置制作方式使用相同数据。
  event.filterCategory('parts', 'pack:crafting', '加工条件', 'all')
  event.partFilter('pack:crafting', 'pack:part_builder', '可在部件加工台制作',
    view => view.getProductionMethods().contains('part_builder'))
  event.partFilter('pack:crafting', 'pack:has_traits', '具有材料词条',
    view => !view.getTraitIds().isEmpty())

  // 部件等级排序示范读取稳定词条 ID；没有该词条时返回空值，排在末尾。
  event.partSort('pack:jagged_level', '锯齿等级',
    view => view.getTraitLevels().get('tconstruct:jagged'))

  // 强化页可按来源筛选，不能把工具自带词条与可强化的工具混为一谈。
  event.filterCategory('modifiers', 'pack:source', '词条来源', 'any')
  event.modifierFilter('pack:source', 'pack:from_materials', '也来自材料',
    view => !view.getSourceMaterials().isEmpty())
  event.modifierFilter('pack:source', 'pack:from_tools', '也来自工具固有词条',
    view => !view.getSourceTools().isEmpty())
  event.modifierSort('pack:tool_count', '适用工具种类',
    view => view.getToolCategories().size())

  // 全词条页按有无强化配方筛选，同一分类选择两个选项时恢复全部词条。
  event.filterCategory('traits', 'pack:recipe', '强化配方', 'any')
  event.traitFilter('pack:recipe', 'pack:with_recipe', '有强化配方',
    view => view.hasModifierRecipe())
  event.traitFilter('pack:recipe', 'pack:without_recipe', '无强化配方',
    view => !view.hasModifierRecipe())
  event.traitSort('pack:name', '按词条名称', view => view.getName())

  // 归类接口接收完整词条 ID，可用于任何命名空间；此示例保留锋利的攻击分类。
  event.traitCategories('tconstruct:sharpness', ['attack'])

  // 旧的三个参数入口仍有效，默认进入“自定义”分类并使用全部满足规则。
  event.materialFilter('pack:has_parts', '至少可制作一个部件',
    view => view.getAvailablePartCount() > 0)
  event.materialSort('pack:part_count', '可用部件数量',
    view => view.getAvailablePartCount())
})
