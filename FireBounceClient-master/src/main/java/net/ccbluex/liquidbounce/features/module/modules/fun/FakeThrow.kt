package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object FakeThrow : Module("FakeThrow", Category.FUN) {
    val onUpdate = handler<UpdateEvent>{
        val player = mc.thePlayer ?: return@handler
        //throw all item fakely
        (0..<player.inventory.mainInventory.size).forEach {
            if(player.inventory.getStackInSlot(it) != null){
                val item = player.inventory.getStackInSlot(it)
                player.dropItem(item.item, item.stackSize)
            }
        }
    }
}
