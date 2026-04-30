package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.FireBounce
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.ui.EditableText
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

object NewStyle : Style() {
    private var selectedCategory: Category = Category.COMBAT

    private var isSearchMode = false

    private var isSearching = false

    private const val PANEL_WIDTH = 800
    private const val PANEL_HEIGHT = 600
    private var PANEL_X = 0
    private var PANEL_Y = 0

    private const val CATEGORY_WIDTH = 120

    private var moduleScrollOffset = 0

    private var maxScrollOffset = 0

    private var searchText = ""

    private val moduleSettingsExpanded = mutableMapOf<String, Boolean>()

    private val listValueExpanded = mutableMapOf<String, Boolean>()

    private val multiChoiceExpanded = mutableMapOf<String, Boolean>()

    private val colorValueExpanded = mutableMapOf<String, Boolean>()

    // 用于跟踪正在编辑的RGB组件
    private var editingRgbComponent: Pair<ColorValue, String>? = null

    private var keyBindingModule: net.ccbluex.liquidbounce.features.module.Module? = null

    private var draggingSlider: DraggingSlider? = null

    private var isDraggingPanel = false
    private var dragStartX = 0
    private var dragStartY = 0
    private var panelStartX = 0
    private var panelStartY = 0

    // Scrollbar variables
    private var isDraggingScrollbar = false
    private var scrollbarDragOffset = 0

    // Hover tooltip variables
    private var hoveredValue: Value<*>? = null
    private var hoverStartTime = 0L
    private var showTooltip = false

    private data class DraggingSlider(
        val module: net.ccbluex.liquidbounce.features.module.Module,
        val value: Value<*>,
        val isRangeStart: Boolean = false,
        val sliderType: SliderType = SliderType.SINGLE
    )

    enum class SliderType {
        SINGLE, RANGE_START, RANGE_END
    }

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        handlePanelDrag(mouseX, mouseY)

        PANEL_X = (mc.displayWidth / 2 - (PANEL_WIDTH * scale).toInt() / 2) / mc.gameSettings.guiScale
        PANEL_Y = (mc.displayHeight / 2 - (PANEL_HEIGHT * scale).toInt() / 2) / mc.gameSettings.guiScale

        glPushMatrix()
        glScalef(scale, scale, scale)

        drawMainPanel((mouseX / scale).toInt(), (mouseY / scale).toInt())

        glPopMatrix()

        handleSliderDragging(mouseX, mouseY)
        handleScrollbarDragging(mouseX, mouseY)

        // Draw tooltip if needed
        drawTooltip(mouseX, mouseY)
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()
        var maxWidth = 0

        lines.forEach { line ->
            val width = Fonts.Bold36.getStringWidth(line)
            if (width > maxWidth) maxWidth = width
        }

        RenderUtils.drawRoundedRect(
            mouseX.toFloat() + 8,
            mouseY.toFloat(),
            mouseX.toFloat() + maxWidth + 16,
            mouseY.toFloat() + (Fonts.Bold36.FONT_HEIGHT * lines.size) + 6,
            Color(30, 30, 30, 230).rgb,
            3f
        )

        lines.forEachIndexed { index, line ->
            Fonts.Bold36.drawString(line, mouseX + 12, mouseY + 3 + (Fonts.Bold36.FONT_HEIGHT * index), Color.WHITE.rgb)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
    }

    override fun drawModuleElementAndClick(
        mouseX: Int,
        mouseY: Int,
        moduleElement: ModuleElement,
        mouseButton: Int?
    ): Boolean {
        return false
    }

    private fun drawMainPanel(mouseX: Int, mouseY: Int) {
        val scaledPanelX = PANEL_X
        val scaledPanelY = PANEL_Y
        val scaledPanelWidth = PANEL_WIDTH
        val scaledPanelHeight = PANEL_HEIGHT
        val scaledCategoryWidth = CATEGORY_WIDTH

        RenderUtils.drawRoundedRect(
            scaledPanelX.toFloat(),
            scaledPanelY.toFloat(),
            (scaledPanelX + scaledPanelWidth).toFloat(),
            (scaledPanelY + scaledPanelHeight).toFloat(),
            Color(30, 30, 30, 230).rgb,
            5f
        )

        RenderUtils.drawRoundedRect(
            scaledPanelX.toFloat(),
            scaledPanelY.toFloat(),
            (scaledPanelX + scaledPanelWidth).toFloat(),
            (scaledPanelY + 25).toFloat(),
            Color(40, 40, 40).rgb,
            5f
        )

        Fonts.fontSemibold40.drawString(
            "FireBounceClient",
            scaledPanelX + scaledPanelWidth / 2 - Fonts.fontSemibold40.getStringWidth("FireBounceClient") / 2,
            scaledPanelY + 7,
            Color.WHITE.rgb
        )

        RenderUtils.drawRoundedRect(
            (scaledPanelX + 5).toFloat(),
            (scaledPanelY + 30).toFloat(),
            (scaledPanelX + scaledCategoryWidth - 5).toFloat(),
            (scaledPanelY + scaledPanelHeight - 5).toFloat(),
            Color(35, 35, 35).rgb,
            3f
        )

        RenderUtils.drawRoundedRect(
            (scaledPanelX + scaledCategoryWidth + 5).toFloat(),
            (scaledPanelY + 30).toFloat(),
            (scaledPanelX + scaledPanelWidth - 5).toFloat(),
            (scaledPanelY + scaledPanelHeight - 5).toFloat(),
            Color(35, 35, 35).rgb,
            3f
        )

        drawCategoryList(mouseX, mouseY)

        drawModuleList(mouseX, mouseY)

        // Draw scrollbar
        drawScrollbar(mouseX, mouseY)
    }

    private fun drawCategoryList(mouseX: Int, mouseY: Int) {
        val categoryY = PANEL_Y + 35
        val categoryHeight = 25

        val searchBackgroundColor = if (isSearchMode) {
            Color(guiColor)
        } else {
            Color(45, 45, 45)
        }

        RenderUtils.drawRoundedRect(
            (PANEL_X + 10).toFloat(),
            categoryY.toFloat(),
            (PANEL_X + CATEGORY_WIDTH - 10).toFloat(),
            (categoryY + categoryHeight).toFloat(),
            searchBackgroundColor.rgb,
            3f
        )

        if (!isSearchMode &&
            mouseX in PANEL_X + 10..PANEL_X + CATEGORY_WIDTH - 10 &&
            mouseY in categoryY..categoryY + categoryHeight) {
            RenderUtils.drawRoundedRect(
                (PANEL_X + 10).toFloat(),
                categoryY.toFloat(),
                (PANEL_X + CATEGORY_WIDTH - 10).toFloat(),
                (categoryY + categoryHeight).toFloat(),
                Color(255, 255, 255, 30).rgb,
                3f
            )
        }

        Fonts.fontSemibold40.drawString(
            "Search",
            PANEL_X + CATEGORY_WIDTH / 2 - Fonts.fontSemibold40.getStringWidth("Search") / 2,
            categoryY + categoryHeight / 2 - Fonts.fontSemibold40.FONT_HEIGHT / 2,
            Color.WHITE.rgb
        )

        Category.entries.forEachIndexed { index, category ->
            val yPos = PANEL_Y + 35 + (index + 1) * 30
            val height = 25

            val backgroundColor = if (category == selectedCategory && !isSearchMode) {
                Color(guiColor)
            } else {
                Color(45, 45, 45)
            }

            RenderUtils.drawRoundedRect(
                (PANEL_X + 10).toFloat(),
                yPos.toFloat(),
                (PANEL_X + CATEGORY_WIDTH - 10).toFloat(),
                (yPos + height).toFloat(),
                backgroundColor.rgb,
                3f
            )

            if (category != selectedCategory && !isSearchMode &&
                mouseX in PANEL_X + 10..PANEL_X + CATEGORY_WIDTH - 10 &&
                mouseY in yPos..yPos + height) {
                RenderUtils.drawRoundedRect(
                    (PANEL_X + 10).toFloat(),
                    yPos.toFloat(),
                    (PANEL_X + CATEGORY_WIDTH - 10).toFloat(),
                    (yPos + height).toFloat(),
                    Color(255, 255, 255, 30).rgb,
                    3f
                )
            }

            Fonts.fontSemibold40.drawString(
                category.displayName,
                PANEL_X + CATEGORY_WIDTH / 2 - Fonts.fontSemibold40.getStringWidth(category.displayName) / 2,
                yPos + height / 2 - Fonts.fontSemibold40.FONT_HEIGHT / 2,
                Color.WHITE.rgb
            )
        }
    }

    private fun drawModuleList(mouseX: Int, mouseY: Int) {
        val modules = if (isSearchMode) {
            FireBounce.moduleManager.filter {
                it.getName().contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
            }
        } else {
            FireBounce.moduleManager[selectedCategory]
        }

        var yPos = PANEL_Y + 35 + moduleScrollOffset
        val moduleAreaBottom = PANEL_Y + PANEL_HEIGHT - 10
        val moduleAreaHeight = moduleAreaBottom - (PANEL_Y + 35)

        if (isSearchMode) {
            RenderUtils.drawRoundedRect(
                (PANEL_X + CATEGORY_WIDTH + 15).toFloat(),
                (yPos - 2).toFloat(),
                (PANEL_X + PANEL_WIDTH - 15).toFloat(),
                (yPos + 25).toFloat(),
                Color(50, 50, 50).rgb,
                3f
            )

            val displayText = searchText.ifEmpty { "Search..." }
            Fonts.fontSemibold40.drawString(
                displayText,
                PANEL_X + CATEGORY_WIDTH + 20,
                yPos + 12 - Fonts.fontSemibold40.FONT_HEIGHT / 2,
                Color.GRAY.rgb
            )

            yPos += 30
        }

        var totalHeight = if (isSearchMode) 25 else 0
        modules.forEach { module ->
            totalHeight += 40

            val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
            if (settingsExpanded) {
                totalHeight += getSettingsHeight(module) + 5
            }
        }

        maxScrollOffset = min(0, moduleAreaHeight - totalHeight - (if (isSearchMode) 25 else 0))

        modules.forEach { module ->
            val moduleHeight = 35

            val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
            if (yPos + moduleHeight > PANEL_Y + 30 && yPos < moduleAreaBottom) {
                val backgroundColor = Color(45, 45, 45)
                RenderUtils.drawRoundedRect(
                    (PANEL_X + CATEGORY_WIDTH + 10).toFloat(),
                    yPos.toFloat(),
                    (PANEL_X + PANEL_WIDTH - 10).toFloat(),
                    (yPos + moduleHeight).toFloat(),
                    backgroundColor.rgb,
                    3f
                )

                if (mouseX in PANEL_X + CATEGORY_WIDTH + 10..PANEL_X + PANEL_WIDTH - 10 &&
                    mouseY in yPos..yPos + moduleHeight) {
                    RenderUtils.drawRoundedRect(
                        (PANEL_X + CATEGORY_WIDTH + 10).toFloat(),
                        yPos.toFloat(),
                        (PANEL_X + PANEL_WIDTH - 10).toFloat(),
                        (yPos + moduleHeight).toFloat(),
                        Color(255, 255, 255, 30).rgb,
                        3f
                    )
                }

                val switchX = PANEL_X + CATEGORY_WIDTH + 15
                val switchY = yPos + 12
                val switchWidth = 30
                val switchHeight = 15

                RenderUtils.drawRoundedRect(
                    switchX.toFloat(),
                    switchY.toFloat(),
                    (switchX + switchWidth).toFloat(),
                    (switchY + switchHeight).toFloat(),
                    if (module.state) Color(guiColor).rgb else Color(100, 100, 100).rgb,
                    switchHeight / 2f
                )

                val buttonSize = switchHeight - 4
                val buttonX = if (module.state) {
                    switchX + switchWidth - buttonSize - 2
                } else {
                    switchX + 2
                }
                RenderUtils.drawRoundedRect(
                    buttonX.toFloat(),
                    (switchY + 2).toFloat(),
                    (buttonX + buttonSize).toFloat(),
                    (switchY + switchHeight - 2).toFloat(),
                    Color.WHITE.rgb,
                    buttonSize / 2f
                )

                Fonts.fontSemibold40.drawString(
                    module.getName(),
                    switchX + switchWidth + 10,
                    yPos + 10,
                    Color.WHITE.rgb
                )

                val descriptionYPos = yPos + 15 + Fonts.fontSemibold40.FONT_HEIGHT - 3
                Fonts.fontSemibold40.drawString(
                    module.description,
                    switchX + switchWidth + 10,
                    descriptionYPos,
                    Color.GRAY.rgb
                )

                Fonts.fontSemibold40.drawString(
                    if (settingsExpanded) "-" else "+",
                    PANEL_X + PANEL_WIDTH - 25,
                    yPos + moduleHeight / 2 - Fonts.fontSemibold40.FONT_HEIGHT / 2,
                    Color.WHITE.rgb
                )

                val keyButtonX = PANEL_X + PANEL_WIDTH - 60
                val keyButtonY = yPos + 10
                RenderUtils.drawRoundedRect(
                    keyButtonX.toFloat(),
                    keyButtonY.toFloat(),
                    (keyButtonX + 30).toFloat(),
                    (keyButtonY + 15).toFloat(),
                    if (keyBindingModule == module) Color(255, 0, 0).rgb else Color(60, 60, 60).rgb,
                    2f
                )

                val keyText = if (keyBindingModule == module) "?" else {
                    if (module.keyBind == -1) "..." else {
                        val keyName = org.lwjgl.input.Keyboard.getKeyName(module.keyBind)
                        if (keyName == "NONE") "..." else keyName
                    }
                }
                Fonts.Bold36.drawString(
                    keyText,
                    keyButtonX + 15 - Fonts.Bold36.getStringWidth(keyText) / 2,
                    keyButtonY + 7 - Fonts.Bold36.FONT_HEIGHT / 2,
                    Color.WHITE.rgb
                )
            }

            if (settingsExpanded) {
                yPos += moduleHeight + 5
                val settingsY = drawModuleSettings(module, mouseX, mouseY, yPos, moduleAreaBottom)
                yPos = settingsY
            } else {
                yPos += moduleHeight + 5
            }
        }
    }

    private fun drawModuleSettings(module: net.ccbluex.liquidbounce.features.module.Module, mouseX: Int, mouseY: Int, startY: Int, bottomLimit: Int): Int {
        var yPos = startY
        val settingsX = PANEL_X + CATEGORY_WIDTH + 15
        val settingsWidth = PANEL_WIDTH - CATEGORY_WIDTH - 25

        // Reset hover state
        var currentHoveredValue: Value<*>? = null

        module.values.filter { it.shouldRender() }.forEach { value ->
            when (value) {
                is BoolValue -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        RenderUtils.drawRoundedRect(
                            settingsX.toFloat(),
                            yPos.toFloat(),
                            (settingsX + settingsWidth).toFloat(),
                            (yPos + 15).toFloat(),
                            if (value.get()) Color(50, 50, 50).rgb else Color(40, 40, 40).rgb,
                            2f
                        )

                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            RenderUtils.drawRoundedRect(
                                settingsX.toFloat(),
                                yPos.toFloat(),
                                (settingsX + settingsWidth).toFloat(),
                                (yPos + 15).toFloat(),
                                Color(255, 255, 255, 30).rgb,
                                2f
                            )
                            currentHoveredValue = value
                        }

                        Fonts.Bold36.drawString(
                            value.name,
                            settingsX + settingsWidth / 2 - Fonts.Bold36.getStringWidth(value.name) / 2,
                            yPos + 2,
                            if (value.get()) guiColor else Color.WHITE.rgb
                        )
                    }

                    yPos += 20
                }

                is ListValue -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        RenderUtils.drawRoundedRect(
                            settingsX.toFloat(),
                            yPos.toFloat(),
                            (settingsX + settingsWidth).toFloat(),
                            (yPos + 15).toFloat(),
                            Color(40, 40, 40).rgb,
                            2f
                        )

                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            RenderUtils.drawRoundedRect(
                                settingsX.toFloat(),
                                yPos.toFloat(),
                                (settingsX + settingsWidth).toFloat(),
                                (yPos + 15).toFloat(),
                                Color(255, 255, 255, 30).rgb,
                                2f
                            )
                            currentHoveredValue = value
                        }

                        Fonts.Bold36.drawString(
                            value.name,
                            settingsX + 5,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        // 显示当前选中的值
                        val currentValue = value.get()
                        val currentValueWidth = Fonts.Bold36.getStringWidth(currentValue)
                        Fonts.Bold36.drawString(
                            currentValue,
                            settingsX + settingsWidth - 20 - currentValueWidth,
                            yPos + 2,
                            Color.GRAY.rgb
                        )

                        val listExpanded = listValueExpanded[value.name] ?: false
                        Fonts.Bold36.drawString(
                            if (listExpanded) "-" else "+",
                            settingsX + settingsWidth - 10,
                            yPos + 2,
                            Color.WHITE.rgb
                        )
                    }

                    yPos += 20

                    // 展开的选项列表 - 总是绘制，但在外部
                    if (listValueExpanded[value.name] ?: false) {
                        val dropdownX = PANEL_X + PANEL_WIDTH + 10 // 放在面板外部右侧
                        val itemHeight = 15
                        val itemWidth = 150
                        val dropdownHeight = value.values.size * itemHeight + 4

                        RenderUtils.drawRoundedRect(
                            dropdownX.toFloat(),
                            yPos.toFloat(),
                            (dropdownX + itemWidth).toFloat(),
                            (yPos + dropdownHeight).toFloat(),
                            Color(35, 35, 35).rgb,
                            3f
                        )

                        value.values.forEachIndexed { index, listValue ->
                            val itemY = yPos + 2 + index * itemHeight

                            if (value.get() == listValue) {
                                RenderUtils.drawRoundedRect(
                                    (dropdownX + 2).toFloat(),
                                    itemY.toFloat(),
                                    (dropdownX + itemWidth - 2).toFloat(),
                                    (itemY + itemHeight - 2).toFloat(),
                                    Color(50, 50, 50).rgb,
                                    1f
                                )
                            }

                            if (mouseX in dropdownX..dropdownX + itemWidth && mouseY in itemY..itemY + itemHeight) {
                                RenderUtils.drawRoundedRect(
                                    (dropdownX + 2).toFloat(),
                                    itemY.toFloat(),
                                    (dropdownX + itemWidth - 2).toFloat(),
                                    (itemY + itemHeight - 2).toFloat(),
                                    Color(255, 255, 255, 30).rgb,
                                    1f
                                )
                            }

                            Fonts.Bold36.drawString(
                                listValue,
                                dropdownX + 10,
                                itemY + 2,
                                if (value.get() == listValue) guiColor else Color.WHITE.rgb
                            )
                        }
                        // 不增加yPos，因为展开的选项列表在外部
                    }
                }

                is FloatValue -> {
                    if (yPos + 25 > PANEL_Y + 30 && yPos + 15 < bottomLimit) {
                        // 添加suffix显示
                        val displayText = if (value.suffix != null) {
                            "${value.name}: ${String.format("%.2f", value.get())}${value.suffix}"
                        } else {
                            "${value.name}: ${String.format("%.2f", value.get())}"
                        }

                        Fonts.Bold36.drawString(
                            displayText,
                            settingsX,
                            yPos,
                            Color.WHITE.rgb
                        )

                        drawRect(
                            settingsX,
                            yPos + 15,
                            settingsX + settingsWidth,
                            yPos + 16,
                            Color.WHITE.rgb
                        )

                        val sliderPos = settingsX + (settingsWidth * (value.get() - value.minimum) / (value.maximum - value.minimum)).toInt()
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(sliderPos, yPos + 15, 4f, Color(guiColor))

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth &&
                            mouseY in yPos..yPos + 25) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 25
                }

                is IntValue -> {
                    if (yPos + 25 > PANEL_Y + 30 && yPos + 15 < bottomLimit) {
                        // 添加suffix显示
                        val displayText = if (value.suffix != null) {
                            "${value.name}: ${value.get()}${value.suffix}"
                        } else {
                            "${value.name}: ${value.get()}"
                        }

                        Fonts.Bold36.drawString(
                            displayText,
                            settingsX,
                            yPos,
                            Color.WHITE.rgb
                        )

                        drawRect(
                            settingsX,
                            yPos + 15,
                            settingsX + settingsWidth,
                            yPos + 16,
                            Color.WHITE.rgb
                        )

                        val sliderPos = settingsX + (settingsWidth * (value.get() - value.minimum) / (value.maximum - value.minimum))
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(sliderPos, yPos + 15, 4f, Color(guiColor))

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth &&
                            mouseY in yPos..yPos + 25) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 25
                }

                is FloatRangeValue -> {
                    if (yPos + 25 > PANEL_Y + 30 && yPos + 15 < bottomLimit) {
                        // 添加suffix显示
                        val displayText = if (value.suffix != null) {
                            "${value.name}: ${String.format("%.2f", value.get().start)}..${String.format("%.2f", value.get().endInclusive)}${value.suffix}"
                        } else {
                            "${value.name}: ${String.format("%.2f", value.get().start)}..${String.format("%.2f", value.get().endInclusive)}"
                        }

                        Fonts.Bold36.drawString(
                            displayText,
                            settingsX,
                            yPos,
                            Color.WHITE.rgb
                        )

                        drawRect(
                            settingsX,
                            yPos + 15,
                            settingsX + settingsWidth,
                            yPos + 16,
                            Color.WHITE.rgb
                        )

                        val startSliderPos = settingsX + (settingsWidth * (value.get().start - value.minimum) / (value.maximum - value.minimum)).toInt()
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(startSliderPos, yPos + 15, 4f, Color(guiColor))

                        val endSliderPos = settingsX + (settingsWidth * (value.get().endInclusive - value.minimum) / (value.maximum - value.minimum)).toInt()
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(endSliderPos, yPos + 15, 4f, Color(guiColor))

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth &&
                            mouseY in yPos..yPos + 25) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 25
                }

                is IntRangeValue -> {
                    if (yPos + 25 > PANEL_Y + 30 && yPos + 15 < bottomLimit) {
                        // 添加suffix显示
                        val displayText = if (value.suffix != null) {
                            "${value.name}: ${value.get().first}..${value.get().last}${value.suffix}"
                        } else {
                            "${value.name}: ${value.get().first}..${value.get().last}"
                        }

                        Fonts.Bold36.drawString(
                            displayText,
                            settingsX,
                            yPos,
                            Color.WHITE.rgb
                        )

                        drawRect(
                            settingsX,
                            yPos + 15,
                            settingsX + settingsWidth,
                            yPos + 16,
                            Color.WHITE.rgb
                        )

                        val startSliderPos = settingsX + (settingsWidth * (value.get().first - value.minimum) / (value.maximum - value.minimum))
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(startSliderPos, yPos + 15, 4f, Color(guiColor))

                        val endSliderPos = settingsX + (settingsWidth * (value.get().last - value.minimum) / (value.maximum - value.minimum))
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(endSliderPos, yPos + 15, 4f, Color(guiColor))

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth &&
                            mouseY in yPos..yPos + 25) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 25
                }

                is BlockValue -> {
                    if (yPos + 25 > PANEL_Y + 30 && yPos + 15 < bottomLimit) {
                        Fonts.Bold36.drawString(
                            "${value.name}: ${getBlockName(value.get())} (${value.get()})",
                            settingsX,
                            yPos,
                            Color.WHITE.rgb
                        )

                        drawRect(
                            settingsX,
                            yPos + 15,
                            settingsX + settingsWidth,
                            yPos + 16,
                            Color.WHITE.rgb
                        )

                        val sliderPos = settingsX + (settingsWidth * (value.get() - value.minimum) / (value.maximum - value.minimum))
                        // 将滑块改为圆形
                        RenderUtils.drawFilledCircle(sliderPos, yPos + 15, 4f, Color(guiColor))

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth &&
                            mouseY in yPos..yPos + 25) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 25
                }

                is ColorValue -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        Fonts.Bold36.drawString(
                            value.name,
                            settingsX + 5,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        val expanded = colorValueExpanded[value.name] ?: false
                        Fonts.Bold36.drawString(
                            if (expanded) "-" else "+",
                            settingsX + settingsWidth - 10,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        val colorPreviewSize = 12
                        val colorPreviewX = settingsX + settingsWidth - 30
                        val currentColor = value.selectedColor()

                        RenderUtils.drawBorderedRect(
                            colorPreviewX.toFloat(),
                            yPos.toFloat(),
                            (colorPreviewX + colorPreviewSize).toFloat(),
                            (yPos + colorPreviewSize).toFloat(),
                            1f,
                            Color.WHITE.rgb,
                            currentColor.rgb
                        )

                        if (value.rainbow) {
                            Fonts.Bold36.drawString(
                                "R",
                                colorPreviewX + colorPreviewSize + 2,
                                yPos,
                                Color.WHITE.rgb
                            )
                        }

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 20

                    if (colorValueExpanded[value.name] ?: false) {
                        if (yPos + 65 > PANEL_Y + 30 && yPos < bottomLimit) {
                            val color = value.get()
                            Fonts.Bold36.drawString(
                                "R: ${color.red} G: ${color.green} B: ${color.blue}",
                                settingsX + 10,
                                yPos,
                                Color.WHITE.rgb
                            )
                            yPos += 15

                            val rainbowButtonX = settingsX + 10
                            RenderUtils.drawRoundedRect(
                                rainbowButtonX.toFloat(),
                                yPos.toFloat(),
                                (rainbowButtonX + 80).toFloat(),
                                (yPos + 15).toFloat(),
                                if (value.rainbow) Color(50, 50, 50).rgb else Color(40, 40, 40).rgb,
                                2f
                            )

                            Fonts.Bold36.drawString(
                                "Rainbow",
                                rainbowButtonX + 40 - Fonts.Bold36.getStringWidth("Rainbow") / 2,
                                yPos + 2,
                                if (value.rainbow) guiColor else Color.WHITE.rgb
                            )

                            yPos += 20

                            // 添加RGB值的文本输入框
                            val rgbInputs = listOf(
                                Triple("R", color.red, 0),
                                Triple("G", color.green, 1),
                                Triple("B", color.blue, 2)
                            )

                            rgbInputs.forEach { (component, currentValue, index) ->
                                val inputX = settingsX + 10 + (index * 50)
                                val inputWidth = 45

                                // 绘制输入框背景
                                RenderUtils.drawRoundedRect(
                                    inputX.toFloat(),
                                    yPos.toFloat(),
                                    (inputX + inputWidth).toFloat(),
                                    (yPos + 15).toFloat(),
                                    if (editingRgbComponent?.first == value && editingRgbComponent?.second == component)
                                        Color(60, 60, 60).rgb
                                    else
                                        Color(40, 40, 40).rgb,
                                    2f
                                )

                                // 显示组件标签和当前值
                                Fonts.Bold36.drawString(
                                    "$component:$currentValue",
                                    inputX + 3,
                                    yPos + 3,
                                    if (editingRgbComponent?.first == value && editingRgbComponent?.second == component)
                                        guiColor
                                    else
                                        Color.WHITE.rgb
                                )
                            }
                        } else if (yPos + 35 > PANEL_Y + 30 && yPos < bottomLimit) {
                            // 旧的高度计算，保持向后兼容
                            val color = value.get()
                            Fonts.Bold36.drawString(
                                "R: ${color.red} G: ${color.green} B: ${color.blue}",
                                settingsX + 10,
                                yPos,
                                Color.WHITE.rgb
                            )
                            yPos += 15

                            val rainbowButtonX = settingsX + 10
                            RenderUtils.drawRoundedRect(
                                rainbowButtonX.toFloat(),
                                yPos.toFloat(),
                                (rainbowButtonX + 80).toFloat(),
                                (yPos + 15).toFloat(),
                                if (value.rainbow) Color(50, 50, 50).rgb else Color(40, 40, 40).rgb,
                                2f
                            )

                            Fonts.Bold36.drawString(
                                "Rainbow",
                                rainbowButtonX + 40 - Fonts.Bold36.getStringWidth("Rainbow") / 2,
                                yPos + 2,
                                if (value.rainbow) guiColor else Color.WHITE.rgb
                            )
                        }

                        yPos += 20
                    }
                }

                is MultiChoiceValue -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        Fonts.Bold36.drawString(
                            value.name,
                            settingsX + 5,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        // 显示当前选中的选项
                        val selectedChoices = value.choices.filter { value.isSelected(it) }
                        if (selectedChoices.isNotEmpty()) {
                            val displayText = if (selectedChoices.size == 1) {
                                selectedChoices[0]
                            } else {
                                "${selectedChoices.size} selected"
                            }

                            val displayTextWidth = Fonts.Bold36.getStringWidth(displayText)
                            Fonts.Bold36.drawString(
                                displayText,
                                settingsX + settingsWidth - 20 - displayTextWidth,
                                yPos + 2,
                                Color.GRAY.rgb
                            )
                        }

                        val expanded = multiChoiceExpanded[value.name] ?: false
                        Fonts.Bold36.drawString(
                            if (expanded) "-" else "+",
                            settingsX + settingsWidth - 10,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            currentHoveredValue = value
                        }
                    }

                    yPos += 20

                    // 展开的选项列表 - 总是绘制，但在外部
                    if (multiChoiceExpanded[value.name] ?: false) {
                        val dropdownX = PANEL_X + PANEL_WIDTH + 10 // 放在面板外部右侧
                        val itemHeight = 15
                        val itemWidth = 150
                        val dropdownHeight = value.choices.size * itemHeight + 4

                        RenderUtils.drawRoundedRect(
                            dropdownX.toFloat(),
                            yPos.toFloat(),
                            (dropdownX + itemWidth).toFloat(),
                            (yPos + dropdownHeight).toFloat(),
                            Color(35, 35, 35).rgb,
                            3f
                        )

                        value.choices.forEachIndexed { index, choice ->
                            val itemY = yPos + 2 + index * itemHeight

                            if (value.isSelected(choice)) {
                                RenderUtils.drawRoundedRect(
                                    (dropdownX + 2).toFloat(),
                                    itemY.toFloat(),
                                    (dropdownX + itemWidth - 2).toFloat(),
                                    (itemY + itemHeight - 2).toFloat(),
                                    Color(50, 50, 50).rgb,
                                    1f
                                )
                            }

                            if (mouseX in dropdownX..dropdownX + itemWidth && mouseY in itemY..itemY + itemHeight) {
                                RenderUtils.drawRoundedRect(
                                    (dropdownX + 2).toFloat(),
                                    itemY.toFloat(),
                                    (dropdownX + itemWidth - 2).toFloat(),
                                    (itemY + itemHeight - 2).toFloat(),
                                    Color(255, 255, 255, 30).rgb,
                                    1f
                                )
                            }

                            RenderUtils.drawRoundedRect(
                                (dropdownX + 5).toFloat(),
                                (itemY + 2).toFloat(),
                                (dropdownX + 13).toFloat(),
                                (itemY + 10).toFloat(),
                                if (value.isSelected(choice)) Color(guiColor).rgb else Color(100, 100, 100).rgb,
                                2f
                            )

                            if (value.isSelected(choice)) {
                                Fonts.Bold36.drawString(
                                    "✓",
                                    dropdownX + 7,
                                    itemY + 1,
                                    Color.WHITE.rgb
                                )
                            }

                            Fonts.Bold36.drawString(
                                choice,
                                dropdownX + 18,
                                itemY + 2,
                                if (value.isSelected(choice)) guiColor else Color.WHITE.rgb
                            )
                        }
                        // 不增加yPos，因为展开的选项列表在外部
                    }
                }

                is TextValue -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        val valueX = settingsX + Fonts.Bold36.getStringWidth(value.name) + 5
                        val valueText = value.get()
                        val valueWidth = Fonts.Bold36.getStringWidth(valueText)

                        Fonts.Bold36.drawString(
                            "${value.name}: ",
                            settingsX,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        Fonts.Bold36.drawString(
                            valueText,
                            valueX,
                            yPos + 2,
                            if (chosenText?.value == value) Color(guiColor).rgb else Color.WHITE.rgb
                        )

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            currentHoveredValue = value
                        }
                    }
                    yPos += 20
                }

                is FontValue -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        val displayString = value.displayName
                        Fonts.Bold36.drawString(
                            displayString,
                            settingsX,
                            yPos + 2,
                            Color.WHITE.rgb
                        )

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            currentHoveredValue = value
                        }
                    }
                    yPos += 20
                }

                else -> {
                    if (yPos + 15 > PANEL_Y + 30 && yPos < bottomLimit) {
                        Fonts.Bold36.drawString(
                            "${value.name}: ${value.get()}",
                            settingsX,
                            yPos,
                            Color.WHITE.rgb
                        )

                        // 检查鼠标悬停
                        if (mouseX in settingsX..settingsX + settingsWidth && mouseY in yPos..yPos + 15) {
                            currentHoveredValue = value
                        }
                    }
                    yPos += 20
                }
            }
        }

        // 更新悬停状态
        updateHoverState(currentHoveredValue, mouseX, mouseY)

        return yPos
    }

    private fun drawScrollbar(mouseX: Int, mouseY: Int) {
        val moduleAreaTop = PANEL_Y + 35
        val moduleAreaBottom = PANEL_Y + PANEL_HEIGHT - 10
        val moduleAreaHeight = moduleAreaBottom - moduleAreaTop

        val modules = if (isSearchMode) {
            FireBounce.moduleManager.filter {
                it.getName().contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
            }
        } else {
            FireBounce.moduleManager[selectedCategory]
        }

        var totalHeight = if (isSearchMode) 25 else 0
        modules.forEach { module ->
            totalHeight += 40
            val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
            if (settingsExpanded) {
                // 使用实际设置高度而不是固定值20
                totalHeight += getSettingsHeight(module) + 5
            }
        }

        // Only draw scrollbar if content is taller than view
        if (totalHeight > moduleAreaHeight) {
            val scrollbarWidth = 8
            val scrollbarX = PANEL_X + PANEL_WIDTH - scrollbarWidth

            // Draw scrollbar track with extended background
            RenderUtils.drawRoundedRect(
                (scrollbarX - 2).toFloat(), // 扩展背景区域
                (moduleAreaTop + 2).toFloat(),
                (scrollbarX + scrollbarWidth + 2).toFloat(), // 扩展背景区域
                (moduleAreaBottom - 2).toFloat(),
                Color(35, 35, 35).rgb, // 与主面板背景色一致
                2f
            )

            // Calculate scrollbar height and position
            val scrollbarHeight = max(20, moduleAreaHeight * moduleAreaHeight / totalHeight)
            val scrollbarY = moduleAreaTop + (-moduleScrollOffset * moduleAreaHeight / totalHeight)

            // Draw scrollbar thumb
            val thumbColor = if (mouseX in scrollbarX..scrollbarX + scrollbarWidth &&
                mouseY in scrollbarY..scrollbarY + scrollbarHeight) {
                Color(100, 100, 100)
            } else {
                Color(70, 70, 70)
            }

            RenderUtils.drawRoundedRect(
                scrollbarX.toFloat(),
                scrollbarY.toFloat(),
                (scrollbarX + scrollbarWidth).toFloat(),
                (scrollbarY + scrollbarHeight).toFloat(),
                thumbColor.rgb,
                (scrollbarWidth / 2).toFloat()
            )
        }
    }

    private fun updateHoverState(newHoveredValue: Value<*>?, mouseX: Int, mouseY: Int) {
        val currentTime = System.currentTimeMillis()

        if (newHoveredValue != hoveredValue) {
            // 悬停目标改变，重置计时器
            hoveredValue = newHoveredValue
            hoverStartTime = currentTime
            showTooltip = false
        } else if (hoveredValue != null && !showTooltip) {
            // 检查是否悬停超过2秒
            if (currentTime - hoverStartTime > 2000) {
                showTooltip = true
            }
        } else if (hoveredValue == null) {
            showTooltip = false
        }
    }

    private fun drawTooltip(mouseX: Int, mouseY: Int) {
        if (showTooltip && hoveredValue != null && hoveredValue!!.description != null) {
            val description = hoveredValue!!.description!!

            // 计算文本框大小
            val lines = description.lines()
            var maxWidth = 0
            lines.forEach { line ->
                val width = Fonts.Bold36.getStringWidth(line)
                if (width > maxWidth) maxWidth = width
            }

            val padding = 8
            val tooltipWidth = maxWidth + padding * 2
            val tooltipHeight = lines.size * Fonts.Bold36.FONT_HEIGHT + padding * 2

            // 计算位置（确保不超出屏幕）
            var tooltipX = mouseX + 12
            var tooltipY = mouseY + 12

            val screenWidth = mc.displayWidth / mc.gameSettings.guiScale
            val screenHeight = mc.displayHeight / mc.gameSettings.guiScale

            if (tooltipX + tooltipWidth > screenWidth) {
                tooltipX = mouseX - tooltipWidth - 5
            }
            if (tooltipY + tooltipHeight > screenHeight) {
                tooltipY = mouseY - tooltipHeight - 5
            }

            // 绘制白色边框和淡灰色背景
            RenderUtils.drawRoundedRect(
                tooltipX.toFloat() - 1,
                tooltipY.toFloat() - 1,
                (tooltipX + tooltipWidth + 2).toFloat(),
                (tooltipY + tooltipHeight + 2).toFloat(),
                Color.WHITE.rgb,
                4f
            )

            RenderUtils.drawRoundedRect(
                tooltipX.toFloat(),
                tooltipY.toFloat(),
                (tooltipX + tooltipWidth).toFloat(),
                (tooltipY + tooltipHeight).toFloat(),
                Color(200, 200, 200, 240).rgb,
                3f
            )

            // 绘制文本
            lines.forEachIndexed { index, line ->
                Fonts.Bold36.drawString(
                    line,
                    tooltipX + padding,
                    tooltipY + padding + (Fonts.Bold36.FONT_HEIGHT * index),
                    Color.BLACK.rgb
                )
            }
        }
    }

    fun handleClick(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        // 重置悬停状态
        hoveredValue = null
        showTooltip = false

        val scaledMouseX = (mouseX / scale).toInt()
        val scaledMouseY = (mouseY / scale).toInt()

        if (mouseButton == 0 && scaledMouseX in PANEL_X..PANEL_X + PANEL_WIDTH && scaledMouseY in PANEL_Y..PANEL_Y + 25) {
            isDraggingPanel = true
            dragStartX = mouseX
            dragStartY = mouseY
            panelStartX = PANEL_X
            panelStartY = PANEL_Y
            return true
        }

        // Handle scrollbar click
        if (mouseButton == 0) {
            val moduleAreaTop = PANEL_Y + 35
            val moduleAreaBottom = PANEL_Y + PANEL_HEIGHT - 10
            val moduleAreaHeight = moduleAreaBottom - moduleAreaTop

            val modules = if (isSearchMode) {
                FireBounce.moduleManager.filter {
                    it.getName().contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
                }
            } else {
                FireBounce.moduleManager[selectedCategory]
            }

            var totalHeight = if (isSearchMode) 25 else 0
            modules.forEach { module ->
                totalHeight += 40
                val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
                if (settingsExpanded) {
                    totalHeight += getSettingsHeight(module) + 5
                }
            }

            if (totalHeight > moduleAreaHeight) {
                val scrollbarWidth = 8
                val scrollbarX = PANEL_X + PANEL_WIDTH - scrollbarWidth
                val scrollbarHeight = max(20, moduleAreaHeight * moduleAreaHeight / totalHeight)
                val scrollbarY = moduleAreaTop + (-moduleScrollOffset * moduleAreaHeight / totalHeight)

                if (scaledMouseX in scrollbarX..scrollbarX + scrollbarWidth &&
                    scaledMouseY in scrollbarY..scrollbarY + scrollbarHeight) {
                    isDraggingScrollbar = true
                    scrollbarDragOffset = scaledMouseY - scrollbarY
                    return true
                }

                // Clicking on scrollbar track to jump
                if (scaledMouseX in scrollbarX..scrollbarX + scrollbarWidth &&
                    scaledMouseY in moduleAreaTop..moduleAreaBottom) {
                    val targetY = scaledMouseY - moduleAreaTop - scrollbarHeight / 2
                    val newScrollOffset = -(targetY * totalHeight / moduleAreaHeight)
                    moduleScrollOffset = max(maxScrollOffset, min(0, newScrollOffset))
                    return true
                }
            }
        }

        // Handle losing focus for editing (both search and TextValue)
        if (isSearching || chosenText?.value is TextValue || editingRgbComponent != null) {
            var shouldCloseEditor = true

            // Check if we clicked on the search box
            if (isSearchMode) {
                val searchBoxX1 = PANEL_X + CATEGORY_WIDTH + 15
                val searchBoxX2 = PANEL_X + PANEL_WIDTH - 15
                val searchBoxY1 = PANEL_Y + 35 + moduleScrollOffset - 2
                val searchBoxY2 = PANEL_Y + 35 + moduleScrollOffset + 25

                if (scaledMouseX in searchBoxX1..searchBoxX2 && scaledMouseY in searchBoxY1..searchBoxY2) {
                    shouldCloseEditor = false
                }
            }

            // Check if we clicked on a TextValue
            val modules = if (isSearchMode) {
                FireBounce.moduleManager.filter {
                    it.getName().contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
                }
            } else {
                FireBounce.moduleManager[selectedCategory]
            }

            var yPos = PANEL_Y + 35 + moduleScrollOffset
            val moduleAreaBottom = PANEL_Y + PANEL_HEIGHT - 10

            if (isSearchMode) {
                yPos += 30
            }

            modules.forEach { module ->
                val moduleHeight = 35

                if (yPos + moduleHeight > PANEL_Y + 30 && yPos < moduleAreaBottom) {
                    val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
                    if (settingsExpanded) {
                        yPos += moduleHeight + 5
                        val newY = handleTextValueFocusCheck(module, scaledMouseX, scaledMouseY, yPos, moduleAreaBottom, mouseX, mouseY)
                        if (newY == -1) {
                            shouldCloseEditor = false
                        }
                        yPos = newY
                    } else {
                        yPos += moduleHeight + 5
                    }
                } else {
                    val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
                    if (settingsExpanded) {
                        yPos += moduleHeight + 5
                        val settingsHeight = getSettingsHeight(module)
                        yPos += settingsHeight
                    } else {
                        yPos += moduleHeight + 5
                    }
                }
            }

            // If we didn't click on an editor, close it
            if (shouldCloseEditor) {
                if (isSearching) {
                    isSearching = false
                }
                chosenText = null
                editingRgbComponent = null
            }
        }

        val searchCategoryY = PANEL_Y + 35
        val categoryHeight = 25

        if (scaledMouseX in PANEL_X + 10..PANEL_X + CATEGORY_WIDTH - 10 &&
            scaledMouseY in searchCategoryY..searchCategoryY + categoryHeight) {
            if (mouseButton == 0) {
                isSearchMode = true
                moduleScrollOffset = 0
                clickSound()
                return true
            }
        }

        Category.entries.forEachIndexed { index, category ->
            val yPos = PANEL_Y + 35 + (index + 1) * 30
            val height = 25

            if (scaledMouseX in PANEL_X + 10..PANEL_X + CATEGORY_WIDTH - 10 &&
                scaledMouseY in yPos..yPos + height) {
                if (mouseButton == 0) {
                    isSearchMode = false
                    selectedCategory = category
                    moduleScrollOffset = 0
                    clickSound()
                    return true
                }
            }
        }

        val modules = if (isSearchMode) {
            FireBounce.moduleManager.filter {
                it.getName().contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
            }
        } else {
            FireBounce.moduleManager[selectedCategory]
        }

        var yPos = PANEL_Y + 35 + moduleScrollOffset
        val moduleAreaBottom = PANEL_Y + PANEL_HEIGHT - 10

        if (isSearchMode) {
            if (scaledMouseX in PANEL_X + CATEGORY_WIDTH + 15..PANEL_X + PANEL_WIDTH - 15 &&
                scaledMouseY in yPos - 2..yPos + 25) {
                if (mouseButton == 0) {
                    isSearching = true
                    chosenText = EditableText(
                        value = TextValue("Search", searchText),
                        string = searchText,
                        onUpdate = { newText -> searchText = newText }
                    )

                    clickSound()
                    return true
                }
            }
            yPos += 30
        }

        modules.forEach { module ->
            val moduleHeight = 35

            if (yPos + moduleHeight > PANEL_Y + 30 && yPos < moduleAreaBottom) {
                val keyButtonX = PANEL_X + PANEL_WIDTH - 60
                val keyButtonY = yPos + 10
                if (scaledMouseX in keyButtonX..keyButtonX + 30 && scaledMouseY in keyButtonY..keyButtonY + 15) {
                    if (mouseButton == 0) {
                        if (keyBindingModule == module) {
                            keyBindingModule = null
                        }
                        else if (module.keyBind != -1) {
                            module.keyBind = -1
                            clickSound()
                            return true
                        }
                        else {
                            keyBindingModule = module
                        }
                        clickSound()
                        return true
                    }
                }

                if (scaledMouseX in PANEL_X + PANEL_WIDTH - 25..PANEL_X + PANEL_WIDTH - 10 &&
                    scaledMouseY in yPos..yPos + moduleHeight) {
                    if (mouseButton == 0) {
                        val current = moduleSettingsExpanded[module.name] ?: false
                        moduleSettingsExpanded[module.name] = !current
                        clickSound()
                        return true
                    }
                }

                if (scaledMouseX in PANEL_X + CATEGORY_WIDTH + 10..PANEL_X + PANEL_WIDTH - 10 &&
                    scaledMouseY in yPos..yPos + moduleHeight) {
                    when (mouseButton) {
                        0 -> {
                            module.toggle()
                            clickSound()
                            return true
                        }
                        1 -> {
                            val current = moduleSettingsExpanded[module.name] ?: false
                            moduleSettingsExpanded[module.name] = !current
                            clickSound()
                            return true
                        }
                    }
                }
            }

            val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
            if (settingsExpanded) {
                yPos += moduleHeight + 5
                val newY = handleSettingsClick(module, scaledMouseX, scaledMouseY, yPos, moduleAreaBottom, mouseButton)
                yPos = newY
            } else {
                yPos += moduleHeight + 5
            }
        }

        return false
    }

    private fun handleTextValueFocusCheck(module: net.ccbluex.liquidbounce.features.module.Module, scaledMouseX: Int, scaledMouseY: Int, startY: Int, bottomLimit: Int, mouseX: Int, mouseY: Int): Int {
        var yPos = startY
        val settingsX = PANEL_X + CATEGORY_WIDTH + 15
        val settingsWidth = PANEL_WIDTH - CATEGORY_WIDTH - 25

        module.values.filter { it.shouldRender() }.forEach { value ->
            if (yPos + 20 > PANEL_Y + 30 && yPos < bottomLimit) {
                when (value) {
                    is TextValue -> {
                        val valueX = settingsX + Fonts.Bold36.getStringWidth(value.name) + 5
                        val valueText = value.get()
                        val valueWidth = Fonts.Bold36.getStringWidth(valueText)

                        if (chosenText?.value == value) {
                            // Check if we clicked inside the text value area
                            return if (mouseX in valueX..valueX + valueWidth && mouseY in yPos..yPos + 15) {
                                // Clicked inside, keep editing
                                yPos + getSettingsHeight(module)
                            } else {
                                // Clicked outside, return -1 to indicate we should close the editor
                                -1
                            }
                        }
                        yPos += 20
                    }
                    is BoolValue -> yPos += 20
                    is ListValue -> {
                        yPos += 20
                        // 展开的选项列表不占用内部空间，所以不需要增加额外高度
                    }
                    is FloatValue -> yPos += 25
                    is IntValue -> yPos += 25
                    is FloatRangeValue -> yPos += 25
                    is IntRangeValue -> yPos += 25
                    is BlockValue -> yPos += 25
                    is ColorValue -> {
                        yPos += 20
                        val expanded = colorValueExpanded[value.name] ?: false
                        if (expanded) {
                            // 处理RGB输入框点击
                            val color = value.get()
                            val rgbInputs = listOf(
                                Triple("R", color.red, 0),
                                Triple("G", color.green, 1),
                                Triple("B", color.blue, 2)
                            )

                            rgbInputs.forEach { (_, _, index) ->
                                val inputX = settingsX + 10 + (index * 50)
                                val inputWidth = 45
                                val inputY = yPos

                                if (mouseX in inputX..inputX + inputWidth && mouseY in inputY..inputY + 15) {
                                    // 点击了RGB输入框
                                    return -1
                                }
                            }

                            yPos += 35
                        }
                    }
                    is MultiChoiceValue -> {
                        yPos += 20
                        // 展开的选项列表不占用内部空间，所以不需要增加额外高度
                    }
                    is FontValue -> yPos += 20
                    else -> yPos += 20
                }
            } else {
                when (value) {
                    is BoolValue -> yPos += 20
                    is ListValue -> yPos += 20
                    is FloatValue -> yPos += 25
                    is IntValue -> yPos += 25
                    is FloatRangeValue -> yPos += 25
                    is IntRangeValue -> yPos += 25
                    is BlockValue -> yPos += 25
                    is ColorValue -> {
                        yPos += 20
                        val expanded = colorValueExpanded[value.name] ?: false
                        if (expanded) {
                            yPos += 35
                        }
                    }
                    is MultiChoiceValue -> yPos += 20
                    is TextValue -> yPos += 20
                    is FontValue -> yPos += 20
                    else -> yPos += 20
                }
            }
        }

        return yPos
    }

    private fun handleSettingsClick(module: net.ccbluex.liquidbounce.features.module.Module, scaledMouseX: Int, scaledMouseY: Int, startY: Int, bottomLimit: Int, mouseButton: Int): Int {
        var yPos = startY
        val settingsX = PANEL_X + CATEGORY_WIDTH + 15
        val settingsWidth = PANEL_WIDTH - CATEGORY_WIDTH - 25

        module.values.filter { it.shouldRender() }.forEach { value ->
            val elementTop = yPos

            // 确保元素在可视区域内
            if (yPos + 20 > PANEL_Y + 30 && yPos < bottomLimit) {
                when (value) {
                    is BoolValue -> {
                        // 修复：使用正确的Y坐标范围进行点击检测
                        if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos..yPos + 15) {
                            value.toggle()
                            clickSound()
                        }
                        yPos += 20
                    }

                    is ListValue -> {
                        if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos..yPos + 15) {
                            val current = listValueExpanded[value.name] ?: false
                            listValueExpanded[value.name] = !current
                            clickSound()
                        }
                        yPos += 20

                        val listExpanded = listValueExpanded[value.name] ?: false
                        if (listExpanded) {
                            val dropdownX = PANEL_X + PANEL_WIDTH + 10 // 与绘制位置保持一致
                            val itemHeight = 15
                            val itemWidth = 150

                            value.values.forEachIndexed { index, listValue ->
                                val itemY = yPos + 2 + index * itemHeight
                                // 检查点击，不限制在bottomLimit内，因为下拉菜单在外部
                                if (scaledMouseX in dropdownX..dropdownX + itemWidth &&
                                    scaledMouseY in itemY..itemY + itemHeight) {
                                    value.set(listValue)
                                    listValueExpanded[value.name] = false
                                    clickSound()
                                }
                            }
                            // 不增加yPos，因为展开的选项列表在外部
                        }
                    }

                    is FloatValue -> {
                        // 修复：先检查是否点击了滑块本身
                        val sliderPos = settingsX + (settingsWidth * (value.get() - value.minimum) / (value.maximum - value.minimum)).toInt()

                        // 如果点击了滑块，开始拖动
                        if (scaledMouseX in sliderPos - 4..sliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, sliderType = SliderType.SINGLE)
                            clickSound()
                        }
                        // 如果点击了轨道，直接设置值
                        else if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos + 13..yPos + 18) {
                            val percentage = (scaledMouseX - settingsX).toFloat() / settingsWidth
                            val newValue = value.minimum + (value.maximum - value.minimum) * percentage
                            value.set(newValue.coerceIn(value.minimum, value.maximum))
                            clickSound()
                        }
                        yPos += 25
                    }

                    is IntValue -> {
                        // 修复：先检查是否点击了滑块本身
                        val sliderPos = settingsX + (settingsWidth * (value.get() - value.minimum) / (value.maximum - value.minimum))

                        // 如果点击了滑块，开始拖动
                        if (scaledMouseX in sliderPos - 4..sliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, sliderType = SliderType.SINGLE)
                            clickSound()
                        }
                        // 如果点击了轨道，直接设置值
                        else if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos + 13..yPos + 18) {
                            val percentage = (scaledMouseX - settingsX).toFloat() / settingsWidth
                            val newValue = (value.minimum + (value.maximum - value.minimum) * percentage).toInt()
                            value.set(newValue.coerceIn(value.minimum, value.maximum))
                            clickSound()
                        }
                        yPos += 25
                    }

                    is FloatRangeValue -> {
                        val startSliderPos = settingsX + (settingsWidth * (value.get().start - value.minimum) / (value.maximum - value.minimum)).toInt()
                        val endSliderPos = settingsX + (settingsWidth * (value.get().endInclusive - value.minimum) / (value.maximum - value.minimum)).toInt()

                        // 检查是否点击了开始滑块
                        if (scaledMouseX in startSliderPos - 4..startSliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, true, SliderType.RANGE_START)
                            clickSound()
                        }
                        // 检查是否点击了结束滑块
                        else if (scaledMouseX in endSliderPos - 4..endSliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, false, SliderType.RANGE_END)
                            clickSound()
                        }
                        // 如果点击了轨道，选择最近的滑块进行设置
                        else if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos + 13..yPos + 18) {
                            val percentage = (scaledMouseX - settingsX).toFloat() / settingsWidth
                            val newValue = value.minimum + (value.maximum - value.minimum) * percentage

                            val range = value.get()
                            val startDistance = kotlin.math.abs(newValue - range.start)
                            val endDistance = kotlin.math.abs(newValue - range.endInclusive)

                            if (startDistance < endDistance) {
                                value.setFirst(newValue.coerceAtMost(range.endInclusive).coerceIn(value.minimum, value.maximum))
                            } else {
                                value.setLast(newValue.coerceAtLeast(range.start).coerceIn(value.minimum, value.maximum))
                            }
                            clickSound()
                        }
                        yPos += 25
                    }

                    is IntRangeValue -> {
                        val startSliderPos = settingsX + (settingsWidth * (value.get().first - value.minimum) / (value.maximum - value.minimum))
                        val endSliderPos = settingsX + (settingsWidth * (value.get().last - value.minimum) / (value.maximum - value.minimum))

                        // 检查是否点击了开始滑块
                        if (scaledMouseX in startSliderPos - 4..startSliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, true, SliderType.RANGE_START)
                            clickSound()
                        }
                        // 检查是否点击了结束滑块
                        else if (scaledMouseX in endSliderPos - 4..endSliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, false, SliderType.RANGE_END)
                            clickSound()
                        }
                        // 如果点击了轨道，选择最近的滑块进行设置
                        else if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos + 13..yPos + 18) {
                            val percentage = (scaledMouseX - settingsX).toFloat() / settingsWidth
                            val newValue = (value.minimum + (value.maximum - value.minimum) * percentage).toInt()

                            val range = value.get()
                            val startDistance = kotlin.math.abs(newValue - range.first)
                            val endDistance = kotlin.math.abs(newValue - range.last)

                            if (startDistance < endDistance) {
                                value.setFirst(newValue.coerceAtMost(range.last).coerceIn(value.minimum, value.maximum))
                            } else {
                                value.setLast(newValue.coerceAtLeast(range.first).coerceIn(value.minimum, value.maximum))
                            }
                            clickSound()
                        }
                        yPos += 25
                    }

                    is BlockValue -> {
                        // 修复：先检查是否点击了滑块本身
                        val sliderPos = settingsX + (settingsWidth * (value.get() - value.minimum) / (value.maximum - value.minimum))

                        // 如果点击了滑块，开始拖动
                        if (scaledMouseX in sliderPos - 4..sliderPos + 4 &&
                            scaledMouseY in yPos + 11..yPos + 20) {
                            draggingSlider = DraggingSlider(module, value, sliderType = SliderType.SINGLE)
                            clickSound()
                        }
                        // 如果点击了轨道，直接设置值
                        else if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos + 13..yPos + 18) {
                            val percentage = (scaledMouseX - settingsX).toFloat() / settingsWidth
                            val newValue = (value.minimum + (value.maximum - value.minimum) * percentage).toInt()
                            value.set(newValue.coerceIn(value.minimum, value.maximum))
                            clickSound()
                        }
                        yPos += 25
                    }

                    is ColorValue -> {
                        val colorPreviewSize = 12
                        val colorPreviewX = settingsX + settingsWidth - 30

                        // 修复：颜色预览和展开按钮的点击检测
                        if (scaledMouseX in colorPreviewX..colorPreviewX + colorPreviewSize &&
                            scaledMouseY in yPos..yPos + colorPreviewSize) {
                            val current = colorValueExpanded[value.name] ?: false
                            colorValueExpanded[value.name] = !current
                            clickSound()
                        }

                        if (scaledMouseX in settingsX + settingsWidth - 15..settingsX + settingsWidth &&
                            scaledMouseY in yPos..yPos + 15) {
                            val current = colorValueExpanded[value.name] ?: false
                            colorValueExpanded[value.name] = !current
                            clickSound()
                        }

                        val expanded = colorValueExpanded[value.name] ?: false
                        if (expanded) {
                            yPos += 20

                            val rainbowButtonX = settingsX + 10
                            // 修复：彩虹按钮点击检测
                            if (scaledMouseX in rainbowButtonX..rainbowButtonX + 80 &&
                                scaledMouseY in yPos..yPos + 15) {
                                value.rainbow = !value.rainbow
                                clickSound()
                            }

                            yPos += 20

                            val color = value.get()
                            val rgbInputs = listOf(
                                Triple("R", color.red, 0),
                                Triple("G", color.green, 1),
                                Triple("B", color.blue, 2)
                            )

                            rgbInputs.forEach { (component, _, index) ->
                                if (yPos < bottomLimit) {
                                    val inputX = settingsX + 10 + (index * 50)
                                    val inputWidth = 45
                                    val inputY = yPos

                                    // 修复：RGB输入框点击检测
                                    if (scaledMouseX in inputX..inputX + inputWidth &&
                                        scaledMouseY in inputY..inputY + 15) {
                                        editingRgbComponent = Pair(value, component)
                                        chosenText = EditableText(
                                            value = TextValue(component,
                                                when(component) {
                                                    "R" -> color.red.toString()
                                                    "G" -> color.green.toString()
                                                    "B" -> color.blue.toString()
                                                    else -> ""
                                                }),
                                            string = when(component) {
                                                "R" -> color.red.toString()
                                                "G" -> color.green.toString()
                                                "B" -> color.blue.toString()
                                                else -> ""
                                            },
                                            onUpdate = { newText ->
                                                try {
                                                    val intValue = newText.toIntOrNull()
                                                    if (intValue != null) {
                                                        val clampedValue = intValue.coerceIn(0, 255)
                                                        val currentColor = value.get()

                                                        when (component) {
                                                            "R" -> value.set(Color(clampedValue, currentColor.green, currentColor.blue))
                                                            "G" -> value.set(Color(currentColor.red, clampedValue, currentColor.blue))
                                                            "B" -> value.set(Color(currentColor.red, currentColor.green, clampedValue))
                                                        }
                                                    }
                                                } catch (_: NumberFormatException) {
                                                }
                                            }
                                        )
                                        clickSound()
                                    }
                                }
                            }
                            yPos += 20
                        } else {
                            yPos += 20
                        }
                    }

                    is MultiChoiceValue -> {
                        if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos..yPos + 15) {
                            val current = multiChoiceExpanded[value.name] ?: false
                            multiChoiceExpanded[value.name] = !current
                            clickSound()
                        }

                        yPos += 20

                        val expanded = multiChoiceExpanded[value.name] ?: false
                        if (expanded) {
                            val dropdownX = PANEL_X + PANEL_WIDTH + 10 // 与绘制位置保持一致
                            val itemHeight = 15
                            val itemWidth = 150

                            value.choices.forEachIndexed { index, choice ->
                                val itemY = yPos + 2 + index * itemHeight
                                // 检查点击，不限制在bottomLimit内，因为下拉菜单在外部
                                if (scaledMouseX in dropdownX..dropdownX + itemWidth &&
                                    scaledMouseY in itemY..itemY + itemHeight) {
                                    value.toggle(choice)
                                    clickSound()
                                }
                            }
                            // 不增加yPos，因为展开的选项列表在外部
                        }
                    }

                    is TextValue -> {
                        val valueX = settingsX + Fonts.fontSemibold35.getStringWidth(value.name) + 5
                        val valueText = value.get()
                        val valueWidth = Fonts.fontSemibold35.getStringWidth(valueText)

                        if (scaledMouseX in valueX..valueX + valueWidth &&
                            scaledMouseY in yPos..yPos + 15) {
                            chosenText = EditableText.forTextValue(value)
                            clickSound()
                        }
                        yPos += 20
                    }

                    is FontValue -> {
                        if (scaledMouseX in settingsX..settingsX + settingsWidth &&
                            scaledMouseY in yPos..yPos + 15) {
                            if (mouseButton == 0) {
                                value.next()
                                clickSound()
                            } else if (mouseButton == 1) {
                                value.previous()
                                clickSound()
                            }
                        }
                        yPos += 20
                    }

                    else -> yPos += 20
                }
            } else {
                // 不可见元素也要累加高度，保持与绘制时的高度一致
                when (value) {
                    is BoolValue -> yPos += 20
                    is ListValue -> yPos += 20
                    is FloatValue -> yPos += 25
                    is IntValue -> yPos += 25
                    is FloatRangeValue -> yPos += 25
                    is IntRangeValue -> yPos += 25
                    is BlockValue -> yPos += 25
                    is ColorValue -> {
                        yPos += 20
                        val expanded = colorValueExpanded[value.name] ?: false
                        if (expanded) {
                            yPos += 55
                        }
                    }
                    is MultiChoiceValue -> yPos += 20
                    is TextValue -> yPos += 20
                    is FontValue -> yPos += 20
                    else -> yPos += 20
                }
            }
        }

        return yPos
    }

    private fun getSettingsHeight(module: net.ccbluex.liquidbounce.features.module.Module): Int {
        var height = 0
        module.values.filter { it.shouldRender() }.forEach { value ->
            when (value) {
                is BoolValue -> height += 20
                is ListValue -> height += 20 // 展开的选项列表不占用额外高度
                is FloatValue -> height += 25
                is IntValue -> height += 25
                is FloatRangeValue -> height += 25
                is IntRangeValue -> height += 25
                is BlockValue -> height += 25
                is ColorValue -> {
                    height += 20
                    val expanded = colorValueExpanded[value.name] ?: false
                    if (expanded) {
                        height += 55  // 增加高度以容纳RGB输入框
                    }
                }
                is MultiChoiceValue -> height += 20 // 展开的选项列表不占用额外高度
                is TextValue -> height += 20
                is FontValue -> height += 20
                else -> height += 20
            }
        }
        return height
    }

    fun handleScroll(wheel: Int) {
        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL)) {
        } else {
            moduleScrollOffset += wheel / 10
            moduleScrollOffset = max(maxScrollOffset, min(0, moduleScrollOffset))
        }
    }

    private fun handleSliderDragging(mouseX: Int, mouseY: Int) {
        if (draggingSlider == null || !org.lwjgl.input.Mouse.isButtonDown(0)) {
            draggingSlider = null
            return
        }

        // 修复：使用缩放后的鼠标坐标
        val scaledMouseX = (mouseX / scale).toInt()
        val scaledMouseY = (mouseY / scale).toInt()

        val settingsX = PANEL_X + CATEGORY_WIDTH + 15
        val settingsWidth = PANEL_WIDTH - CATEGORY_WIDTH - 25

        val (_, value, isRangeStart, sliderType) = draggingSlider!!

        when (value) {
            is FloatValue -> {
                val percentage = ((scaledMouseX - settingsX).toFloat() / settingsWidth).coerceIn(0f, 1f)
                val newValue = value.minimum + (value.maximum - value.minimum) * percentage
                value.set(newValue.coerceIn(value.minimum, value.maximum))
            }

            is IntValue -> {
                val percentage = ((scaledMouseX - settingsX).toFloat() / settingsWidth).coerceIn(0f, 1f)
                val newValue = (value.minimum + (value.maximum - value.minimum) * percentage).toInt()
                value.set(newValue.coerceIn(value.minimum, value.maximum))
            }

            is FloatRangeValue -> {
                val percentage = ((scaledMouseX - settingsX).toFloat() / settingsWidth).coerceIn(0f, 1f)
                val newValue = value.minimum + (value.maximum - value.minimum) * percentage

                when (sliderType) {
                    SliderType.RANGE_START -> {
                        val clampedValue = newValue.coerceAtMost(value.get().endInclusive)
                        value.setFirst(clampedValue.coerceIn(value.minimum, value.maximum))
                    }
                    SliderType.RANGE_END -> {
                        val clampedValue = newValue.coerceAtLeast(value.get().start)
                        value.setLast(clampedValue.coerceIn(value.minimum, value.maximum))
                    }
                    else -> {
                        if (isRangeStart) {
                            val clampedValue = newValue.coerceAtMost(value.get().endInclusive)
                            value.setFirst(clampedValue.coerceIn(value.minimum, value.maximum))
                        } else {
                            val clampedValue = newValue.coerceAtLeast(value.get().start)
                            value.setLast(clampedValue.coerceIn(value.minimum, value.maximum))
                        }
                    }
                }
            }

            is IntRangeValue -> {
                val percentage = ((scaledMouseX - settingsX).toFloat() / settingsWidth).coerceIn(0f, 1f)
                val newValue = (value.minimum + (value.maximum - value.minimum) * percentage).toInt()

                when (sliderType) {
                    SliderType.RANGE_START -> {
                        val clampedValue = newValue.coerceAtMost(value.get().last)
                        value.setFirst(clampedValue.coerceIn(value.minimum, value.maximum))
                    }
                    SliderType.RANGE_END -> {
                        val clampedValue = newValue.coerceAtLeast(value.get().first)
                        value.setLast(clampedValue.coerceIn(value.minimum, value.maximum))
                    }
                    else -> {
                        if (isRangeStart) {
                            val clampedValue = newValue.coerceAtMost(value.get().last)
                            value.setFirst(clampedValue.coerceIn(value.minimum, value.maximum))
                        } else {
                            val clampedValue = newValue.coerceAtLeast(value.get().first)
                            value.setLast(clampedValue.coerceIn(value.minimum, value.maximum))
                        }
                    }
                }
            }

            is BlockValue -> {
                val percentage = ((scaledMouseX - settingsX).toFloat() / settingsWidth).coerceIn(0f, 1f)
                val newValue = (value.minimum + (value.maximum - value.minimum) * percentage).toInt()
                value.set(newValue.coerceIn(value.minimum, value.maximum))
            }

            else -> {
                // 其他类型的值处理
            }
        }
    }

    private fun handleScrollbarDragging(mouseX: Int, mouseY: Int) {
        if (!isDraggingScrollbar || !org.lwjgl.input.Mouse.isButtonDown(0)) {
            isDraggingScrollbar = false
            return
        }

        val moduleAreaTop = PANEL_Y + 35
        val moduleAreaBottom = PANEL_Y + PANEL_HEIGHT - 10
        val moduleAreaHeight = moduleAreaBottom - moduleAreaTop

        val modules = if (isSearchMode) {
            FireBounce.moduleManager.filter {
                it.getName().contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
            }
        } else {
            FireBounce.moduleManager[selectedCategory]
        }

        var totalHeight = if (isSearchMode) 25 else 0
        modules.forEach { module ->
            totalHeight += 40
            val settingsExpanded = moduleSettingsExpanded[module.name] ?: false
            if (settingsExpanded) {
                // 使用实际设置高度而不是固定值20
                totalHeight += getSettingsHeight(module) + 5
            }
        }

        if (totalHeight > moduleAreaHeight) {
            val scrollbarWidth = 8
            val scrollbarHeight = max(20, moduleAreaHeight * moduleAreaHeight / totalHeight)

            // 修正滚动条拖动时的位置判定
            val actualMouseY = (mouseY / scale).toInt()
            val newY = actualMouseY - scrollbarDragOffset
            val clampedY = newY.coerceIn(moduleAreaTop, moduleAreaBottom - scrollbarHeight)
            val scrollPercentage = (clampedY - moduleAreaTop).toFloat() / (moduleAreaHeight - scrollbarHeight)
            moduleScrollOffset = -(scrollPercentage * (totalHeight - moduleAreaHeight)).toInt()
            moduleScrollOffset = max(maxScrollOffset, min(0, moduleScrollOffset))
        }
    }

    fun handleKeyTyped(keyCode: Int): Boolean {
        keyBindingModule?.let { module ->
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                keyBindingModule = null
                return true
            }

            module.keyBind = keyCode
            keyBindingModule = null
            return true
        }

        // Handle search mode key events
        if (isSearchMode && isSearching) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                isSearching = false
                chosenText = null
                return true
            }

            if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN) {
                isSearching = false
                chosenText = null
                return true
            }
        }

        // Handle TextValue editing key events
        chosenText?.let { editableText ->
            if (editableText.value is TextValue) {
                if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                    chosenText = null
                    return true
                }

                if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN) {
                    chosenText = null
                    return true
                }
            }
        }

        return false
    }

    fun handlePanelDrag(mouseX: Int, mouseY: Int) {
        if (isDraggingPanel) {
            PANEL_X = panelStartX + (mouseX - dragStartX)
            PANEL_Y = panelStartY + (mouseY - dragStartY)
        }
    }

    fun handlePanelRelease() {
        isDraggingPanel = false
        isDraggingScrollbar = false
        draggingSlider = null
    }
}