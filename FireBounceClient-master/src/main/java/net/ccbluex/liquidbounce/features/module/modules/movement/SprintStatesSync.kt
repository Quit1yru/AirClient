package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PostSprintUpdateEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeSprint
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C0BPacketEntityAction

object SprintStatesSync : Module("SprintStatesSync", Category.MOVEMENT) {
    private val desyncTime by choices("DesyncTime",arrayOf("OnlyWhenSprinting","OnlyWhenNotSprinting","Both"),"Both")
    private val desyncUpdateStateTime by choices("DesyncUpdateStateTime",arrayOf("onPostSprintUpdate","onUpdate","onMotion"),"onPostSprintUpdate",)
    private val desyncMode by choices("DesyncMode",arrayOf("VanillaSet","NetworkPacket","Both"),"Both")
    private val useTag by boolean("Tag",true)
    private val debugMessage by boolean("DebugMessage",false)
    val onPostSprintUpdate = handler<PostSprintUpdateEvent> {
        if (mc.thePlayer.isSprinting == mc.thePlayer.serverSprintState) return@handler
        if (desyncUpdateStateTime != "onPostSprintUpdate") return@handler
        if (mc.thePlayer.serverSprintState) {
            if (desyncTime !in arrayOf("OnlyNotSprinting")) return@handler
            when (desyncMode) {
                "VanillaSet" -> {
                    mc.thePlayer.isSprinting = true
                }
                "NetworkPacket" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                }
                "Both" -> {
                    changeSprint(true)
                }
            }
        } else {
            if (desyncTime !in arrayOf("OnlyWhenNotSprinting")) return@handler
            when (desyncMode) {
                "VanillaSet" -> {
                    mc.thePlayer.isSprinting = false
                }
                "NetworkPacket" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                }
                "Both" -> {
                    changeSprint(false)
                }
            }
        }
        if (debugMessage) chat("Desync")
    }
    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer.isSprinting == mc.thePlayer.serverSprintState) return@handler
        if (desyncUpdateStateTime != "onUpdate") return@handler
        if (mc.thePlayer.serverSprintState) {
            if (desyncTime !in arrayOf("OnlyWhenSprinting")) return@handler
            when (desyncMode) {
                "VanillaSet" -> {
                    mc.thePlayer.isSprinting = true
                }
                "NetworkPacket" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                }
                "Both" -> {
                    changeSprint(true)
                }
            }
        } else {
            if (desyncTime !in arrayOf("OnlyWhenNotSprinting")) return@handler
            when (desyncMode) {
                "VanillaSet" -> {
                    mc.thePlayer.isSprinting = false
                }
                "NetworkPacket" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                }
                "Both" -> {
                    changeSprint(false)
                }
            }
        }
        if (debugMessage) chat("Desync")
    }
    val onMotion = handler<MotionEvent> {
        if (desyncUpdateStateTime != "onMotion") return@handler
        if (mc.thePlayer.isSprinting == mc.thePlayer.serverSprintState) return@handler
        if (mc.thePlayer.serverSprintState) {
            if (desyncTime !in arrayOf("OnlyWhenSprinting")) return@handler
            when (desyncMode) {
                "VanillaSet" -> {
                    mc.thePlayer.isSprinting = true
                }
                "NetworkPacket" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                }
                "Both" -> {
                    changeSprint(true)
                }
            }
        } else {
            if (desyncTime !in arrayOf("OnlyWhenNotSprinting")) return@handler
            when (desyncMode) {
                "VanillaSet" -> {
                    mc.thePlayer.isSprinting = false
                }
                "NetworkPacket" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                }
                "Both" -> {
                    changeSprint(false)
                }
            }
        }
        if (debugMessage) chat("Desync")
    }
    override val tag: String?
        get() = if (useTag) desyncMode else null
}