package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.matrix

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction

object MatrixTest2 : FlyMode("MatrixTest2") {
    private var jumpDelay = 0
    override fun onUpdate() {
        if (jumpDelay <= 12)
        jumpDelay++
    }
    override fun onMove(event: MoveEvent) {
        if (!mc.thePlayer.isMoving) return
        if (isStraightMovement())
        strafe(.2805f, true, event)
        else
        strafe(.2865f, true, event)
        mc.thePlayer.motionY = -0.0784000015258789
        mc.thePlayer.onGround = true
    }

    override fun onPacket(event: PacketEvent) {
        if (!mc.thePlayer.isMoving) return
        val packet = event.packet
        if (mc.thePlayer.serverSprintState) sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING),false)
        if (packet is C03PacketPlayer) {
            event.cancelEvent()
            sendPacket(C03PacketPlayer(true),false)
        }
    }
    private fun isStraightMovement(): Boolean {
        val moveForward = mc.gameSettings.keyBindForward.isKeyDown
        val moveBackward = mc.gameSettings.keyBindBack.isKeyDown
        val moveLeft = mc.gameSettings.keyBindLeft.isKeyDown
        val moveRight = mc.gameSettings.keyBindRight.isKeyDown

        val keysPressed = listOf(moveForward, moveBackward, moveLeft, moveRight).count { it }

        return keysPressed <= 1 ||
                (moveForward && moveBackward) ||
                (moveLeft && moveRight)
    }
}