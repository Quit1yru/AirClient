package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase

object BlockAttack : Module("BlockAttack", Category.COMBAT) {
    private val mode by choices("Mode",arrayOf("RealAttack","OnlySwing"),"RealAttack")
    private val keyBindUseItem = mc.gameSettings.keyBindUseItem
    val onUpdate = handler<UpdateEvent> {

        if (!mc.thePlayer.isBlocking || !mc.gameSettings.keyBindAttack.isKeyDown) return@handler
        KeyBinding.setKeyBindState(keyBindUseItem.keyCode, true)
        mc.thePlayer.swingItem()
        if (mode == "OnlySwing") {
            mc.playerController.attackEntity(mc.thePlayer, null)
            return@handler
        }
        (mc.objectMouseOver?.entityHit as? EntityLivingBase)?.let { target ->
            if (!target.isDead) {
                mc.playerController.attackEntity(mc.thePlayer, target)
            }
        }
        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
    }

    override fun onDisable() {
        KeyBinding.setKeyBindState(keyBindUseItem.keyCode, false)
    }

    override val tag: String?
        get() = mode
}