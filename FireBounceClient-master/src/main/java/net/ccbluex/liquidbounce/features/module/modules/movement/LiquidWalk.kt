/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.skid.nightx.NightXMovementUtils.strafe
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import org.lwjgl.input.Keyboard

object LiquidWalk : Module("LiquidWalk", Category.MOVEMENT, Keyboard.KEY_J) {

    val mode by choices("Mode", arrayOf("Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin", "TatakoLatest", "VulcanA", "VulcanB"), "NCP")
    private val aacFly by float("AACFlyMotion", 0.5f, 0.1f..1f) { mode == "AACFly" }

    private val noJump by boolean("NoJump", false)

    private var nextTick = false
    private var wasInWater = false

    val onMotion = handler<MotionEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        if(player.isInWater) {
            when(mode){
                "TatakoLatest" -> player.motionY+=0.13
                "VulcanA" -> {
                    player.motionY = 0.5
                    strafe(0.36F)
                }
                "VulcanB" -> {
                    strafe((.3f - Math.random() / 1000f).toFloat())
                    player.motionY = 0.5
                    wasInWater=true
                }
            }
        }
        if(!player.isInWater&&wasInWater){
            wasInWater
            if(mode == "VulcanB")player.motionY=-1.0
        }
    }
    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer

        if (thePlayer == null || thePlayer.isSneaking) return@handler

        when (mode) { //toLowerCase/toUpperCase死妈
            "NCP", "Vanilla" -> if (collideBlock(thePlayer.entityBoundingBox) { it is BlockLiquid } && thePlayer.isInsideOfMaterial(
                    Material.air
                ) && !thePlayer.isSneaking) thePlayer.motionY = 0.08

            "AAC" -> {
                val blockPos = thePlayer.position.down()
                if (!thePlayer.onGround && blockPos.block == Blocks.water || thePlayer.isInWater) {
                    if (!thePlayer.isSprinting) {
                        thePlayer.motionX *= 0.99999
                        thePlayer.motionY *= 0.0
                        thePlayer.motionZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY =
                            ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        thePlayer.motionX *= 0.99999
                        thePlayer.motionY *= 0.0
                        thePlayer.motionZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY =
                            ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (thePlayer.fallDistance >= 4) thePlayer.motionY =
                        -0.004 else if (thePlayer.isInWater) thePlayer.motionY = 0.09
                }
                if (thePlayer.hurtTime != 0) thePlayer.onGround = false
            }

            "Spartan" -> if (thePlayer.isInWater) {
                if (thePlayer.isCollidedHorizontally) {
                    thePlayer.motionY += 0.15
                    return@handler
                }
                val block = BlockPos(thePlayer).up().block
                val blockUp = BlockPos(thePlayer.posX, thePlayer.posY + 1.1, thePlayer.posZ).block

                if (blockUp is BlockLiquid) {
                    thePlayer.motionY = 0.1
                } else if (block is BlockLiquid) {
                    thePlayer.motionY = 0.0
                }

                thePlayer.onGround = true
                thePlayer.motionX *= 1.085
                thePlayer.motionZ *= 1.085
            }

            "AAC3.3.11" -> if (thePlayer.isInWater) {
                thePlayer.motionX *= 1.17
                thePlayer.motionZ *= 1.17
                if (thePlayer.isCollidedHorizontally)
                    thePlayer.motionY = 0.24
                else if (BlockPos(thePlayer).up().block != Blocks.air)
                    thePlayer.motionY += 0.04
            }

            "Dolphin" -> if (thePlayer.isInWater) thePlayer.motionY += 0.03999999910593033
        }
    }

    val onMove = handler<MoveEvent> { event ->
        if ("AACFly" == mode && mc.thePlayer.isInWater) {
            event.y = aacFly.toDouble()
            mc.thePlayer.motionY = aacFly.toDouble()
        }
    }

    @Suppress("unused")
    val onBlockBB = handler<BlockBBEvent> { event ->
        if (mc.thePlayer == null)
            return@handler

        if (event.block is BlockLiquid && !collideBlock(mc.thePlayer.entityBoundingBox) { it is BlockLiquid } && !mc.thePlayer.isSneaking) {
            when (mode) {
                "NCP", "Vanilla" -> event.boundingBox = AxisAlignedBB.fromBounds(
                    event.x.toDouble(),
                    event.y.toDouble(),
                    event.z.toDouble(),
                    event.x + 1.toDouble(),
                    event.y + 1.toDouble(),
                    event.z + 1.toDouble()
                )
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val thePlayer = mc.thePlayer

        if (thePlayer == null || mode != "NCP")
            return@handler

        if (event.packet is C03PacketPlayer) {
            val packetPlayer = event.packet

            if (collideBlock(
                    AxisAlignedBB.fromBounds(
                        thePlayer.entityBoundingBox.maxX,
                        thePlayer.entityBoundingBox.maxY,
                        thePlayer.entityBoundingBox.maxZ,
                        thePlayer.entityBoundingBox.minX,
                        thePlayer.entityBoundingBox.minY - 0.01,
                        thePlayer.entityBoundingBox.minZ
                    )
                ) { it is BlockLiquid }
            ) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        val block = BlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ).block

        if (noJump && block is BlockLiquid)
            event.cancelEvent()
    }

    override val tag
        get() = mode
}