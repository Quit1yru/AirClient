package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.objects.Drag
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.fdpdropdown.utils.render.Scroll
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc
import net.minecraft.client.gui.ScaledResolution
import java.util.WeakHashMap

object FDPState {
    private val categoryDrags = mutableMapOf<Category, Drag>()
    private val categoryScrolls = mutableMapOf<Category, Scroll>()
    private val moduleExpanded = WeakHashMap<Module, Boolean>()

    fun getDrag(category: Category): Drag {
        return categoryDrags.getOrPut(category) { calculatePosition(category) }
    }

    fun getScroll(category: Category): Scroll {
        return categoryScrolls.getOrPut(category) { Scroll() }
    }

    fun resetPositions() {
        Category.values().forEach { category ->
            val pos = calculatePosition(category)
            val drag = getDrag(category)
            drag.x = pos.x
            drag.y = pos.y
        }
    }

    private fun calculatePosition(targetCategory: Category): Drag {
        // Pure horizontal layout: all panels in one row
        val startX = 20f
        val startY = 20f
        val paddingX = 150f // 140 width + 10 padding

        return Drag(startX + targetCategory.ordinal * paddingX, startY)
    }

    fun isExpanded(module: Module): Boolean {
        return moduleExpanded.getOrDefault(module, false)
    }

    fun setExpanded(module: Module, expanded: Boolean) {
        moduleExpanded[module] = expanded
    }
    
    fun toggleExpanded(module: Module) {
        moduleExpanded[module] = !isExpanded(module)
    }
}
