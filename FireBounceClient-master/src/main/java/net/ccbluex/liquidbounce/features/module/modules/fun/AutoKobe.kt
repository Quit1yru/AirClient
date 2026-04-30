package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object AutoKobe : Module("AutoKobe", Category.FUN) {
    val zhuijiMode by choices("FallMode",arrayOf("SetMotionY","SetPosition"),"SetMotionY")
    val zhuijisudu by int("FallSpeed",999,100..10000) {zhuijiMode == "SetMotionY"}
    override fun onEnable() {
        super.onEnable()
        when (zhuijiMode) {
            "SetMotionY" -> {
                mc.thePlayer.motionY = -zhuijisudu.toDouble() / 100.0
            }
            "SetPosition" -> {
                // 使用更可靠的方式发送数据包
                mc.netHandler.networkManager.sendPacket(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY - 999,
                        mc.thePlayer.posZ,
                        true
                    )
                )
            }
        }

        chat("主播主播你怎么坠机了?")
        state = false
    }
}