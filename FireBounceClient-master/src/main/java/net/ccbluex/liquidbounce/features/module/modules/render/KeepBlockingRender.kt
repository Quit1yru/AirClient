package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.minecraft.item.ItemSword

object KeepBlockingRender : Module("KeepBlockingRender", Category.RENDER) {
    @JvmField
    var shouldShowRender = false
    val onUpdate = handler<UpdateEvent> {
        shouldShowRender = mc.thePlayer?.heldItem?.item is ItemSword || SilentHotbar.getVisualSlotItemType() is ItemSword
    }
}