/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.isMovePacket
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object Freeze : Module("Freeze", Category.MOVEMENT) {
    val fakeDeath by boolean("FakeDeathMode", true)
    private val keepRotationAvailable by boolean("KeepRotationStillChangeable",true)
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    override fun onEnable() {
        mc.thePlayer ?: return
        if(fakeDeath) {
            mc.thePlayer.isDead=true
            return
        }

        x = mc.thePlayer.posX
        y = mc.thePlayer.posY
        z = mc.thePlayer.posZ
        motionX = mc.thePlayer.motionX
        motionY = mc.thePlayer.motionY
        motionZ = mc.thePlayer.motionZ
    }

    val onUpdate = handler<UpdateEvent> {
        if(fakeDeath)return@handler
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionY = 0.0
        mc.thePlayer.motionZ = 0.0
        mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if(fakeDeath)return@handler

        if (packet.isMovePacket) {
            if (!keepRotationAvailable) event.cancelEvent()
            else if (packet !is C03PacketPlayer.C05PacketPlayerLook) event.cancelEvent()
        }
        if (event.packet is S08PacketPlayerPosLook) {
            x = event.packet.x
            y = event.packet.y
            z = event.packet.z
            motionX = 0.0
            motionY = 0.0
            motionZ = 0.0
        }
    }

    override fun onDisable() {
        if(fakeDeath) {
            mc.thePlayer.isDead=false
            return
        }
        mc.thePlayer.motionX = motionX
        mc.thePlayer.motionY = motionY
        mc.thePlayer.motionZ = motionZ
        mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
    }
}
