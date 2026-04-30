/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Tower
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.renderNameTag
import net.minecraft.block.BlockAir
import net.minecraft.client.renderer.GlStateManager.resetColor
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemEnderPearl
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

object AntiVoid : Module("AntiVoid", Category.MOVEMENT) {

    private val mode by choices(
        "Mode",
        arrayOf("Blink", "TeleportBack", "FlyFlag", "OnGroundSpoof", "MotionTeleport-Flag", "GhostBlock","Freeze","OldGrim","Grim"),
        "FlyFlag"
    )
    private val maxFallDistance by int("MaxFallDistance", 10, 2..255)
    private val maxDistanceWithoutGround by float("MaxDistanceToSetback", 2.5f, 1f..30f) { mode != "Blink" && mode != "Grim" }
    private val blinkDelay by int("BlinkDelay", 10, 1..20) { mode == "Blink" }
    private val onScaffold by boolean("OnScaffold", false) { mode == "Blink" }
    private val ticksToDelay by int("TicksDelay", 5, 1..20) { mode == "Blink" && !onScaffold }
    private val indicator by boolean("Indicator", true).subjective()
    private val autoScaffold by boolean("AutoScaffold",false) {mode == "Freeze"}
    private val switchFreezeCoolDown by int("SwitchFreezeCooldown",1,0..20,"ticks") {mode == "Freeze" && autoScaffold}
    private val scaffoldCheckDistance by float("ScaffoldCheckDistance", 3f, 1f..10f) { mode == "Freeze" && autoScaffold }
    private val grimReduce by float("GrimReduce", 3f, 0f..5f) { mode == "OldGrim" || mode == "Grim" }
    private val distance by float("Distance", 5f, 0f..10f) { mode == "Grim" }
    private val toggleScaffold by boolean("ToggleScaffold", true) { mode == "Grim" }

    // Grim mode variables
    private var grimX = 0.0
    private var grimY = 0.0
    private var grimZ = 0.0
    private var shouldStuck = false
    private var setBack = false
    private var wasVoid = false
    private var wait = false
    private var position: Vec3? = null
    private var motionVec: Vec3? = null
    private var overVoidTicks = 0

    private var gotVelocity = false
    private var grimACTimer = 0
    private var detectedLocation: BlockPos? = null
    private var lastFound = 0F
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0
    private var shouldSimulateBlock = false
    private var shouldBlink = false
    private var pauseTicks = 0
    private var freezeOpened = false
    private var scaffoldOpened = false
    private var freezeCooldown = 0
    private var lastPlacedBlockPos: BlockPos? = null

    override fun onDisable() {
        prevX = 0.0
        prevY = 0.0
        prevZ = 0.0
        pauseTicks = 0
        freezeCooldown = 0
        gotVelocity = false
        grimACTimer = 0
        shouldSimulateBlock = false
        shouldBlink = false

        // Grim mode variables
        grimX = 0.0
        grimY = 0.0
        grimZ = 0.0
        shouldStuck = false
        setBack = false
        wasVoid = false
        wait = false
        position = null
        motionVec = null
        overVoidTicks = 0

        // Reset states
        resetFreezeStates()

        BlinkUtils.unblink()

        // Reset timer
        mc.timer.timerSpeed = 1.0f
    }

    val onUpdate = handler<UpdateEvent> {
        detectedLocation = null

        val thePlayer = mc.thePlayer ?: return@handler

        // Handle Grim mode separately
        if (mode == "Grim") {
            handleGrimMode(thePlayer)
            return@handler
        }

        if ((mode == "OldGrim" || mode == "Grim") && gotVelocity) {
            grimACTimer++
            if (grimACTimer <= 5) {
                val reduceFactor = 1.0 - (grimReduce / 10.0)
                mc.timer.timerSpeed = (0.4f + Math.random().toFloat() * 0.2f)
                thePlayer.motionX *= reduceFactor
                thePlayer.motionZ *= reduceFactor
                thePlayer.motionY *= 0.9
                val horizJitter = (Math.random() - 0.5) * 1.0E-6
                thePlayer.motionX += horizJitter
                thePlayer.motionZ += -horizJitter
            } else {
                mc.timer.timerSpeed = 1.0f
                gotVelocity = false
            }
        }

        if (thePlayer.onGround && gotVelocity) {
            gotVelocity = false
            grimACTimer = 0
            mc.timer.timerSpeed = 1.0f
        }
        // Update cooldown
        if (freezeCooldown > 0) {
            freezeCooldown--
        }

        // Check if player is on ground or near placed blocks
        val isSafe = checkPlayerSafety(thePlayer)

        if (isSafe) {
            prevX = thePlayer.prevPosX
            prevY = thePlayer.prevPosY
            prevZ = thePlayer.prevPosZ
            shouldSimulateBlock = false

            // Reset states when safe
            if (freezeOpened || scaffoldOpened) {
                resetFreezeStates()
            }
            return@handler
        }

        if (!thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater) {
            val fallingPlayer = FallingPlayer(thePlayer)

            detectedLocation = fallingPlayer.findCollision(60)?.pos

            if (detectedLocation != null && abs(thePlayer.posY - detectedLocation!!.y) +
                thePlayer.fallDistance <= maxFallDistance
            ) {
                lastFound = thePlayer.fallDistance
            }

            if (thePlayer.fallDistance - lastFound > maxDistanceWithoutGround) {
                when (mode.lowercase()) {
                    "oldgrim" -> {
                        if (gotVelocity) {
                            // 如果正在减少击退，同时提供防虚空保护
                            if (thePlayer.fallDistance > maxDistanceWithoutGround) {
                                thePlayer.motionY = 0.1
                                thePlayer.fallDistance = max(thePlayer.fallDistance - 1f, 0f)
                            }
                        } else {
                            // Grim 模式防虚空逻辑
                            when {
                                // 快速下落且下方是虚空
                                thePlayer.motionY < -0.3 && detectedLocation == null -> {
                                    thePlayer.motionY = -0.1  // 减缓下落速度
                                    thePlayer.fallDistance = max(thePlayer.fallDistance - 1f, 0f)
                                }
                                // 检测到碰撞点但fallDistance过大
                                detectedLocation != null && thePlayer.fallDistance > maxDistanceWithoutGround -> {
                                    val targetY = detectedLocation!!.y + 1.0
                                    val currentY = thePlayer.posY

                                    if (currentY - targetY > 2) {
                                        // 向上推动避免摔落伤害
                                        thePlayer.motionY = 0.2
                                        thePlayer.fallDistance = 0f
                                    }
                                }
                                // 普通防虚空 - 向上推动
                                thePlayer.fallDistance > maxDistanceWithoutGround -> {
                                    thePlayer.motionY = 0.15
                                    thePlayer.fallDistance = 0f
                                }
                            }
                        }
                    }
                    "freeze" -> {
                        if (freezeCooldown > 0) return@handler

                        if (autoScaffold) {
                            if (!scaffoldOpened) {
                                // Enable scaffold first
                                Scaffold.state = true
                                scaffoldOpened = true
                                freezeCooldown = switchFreezeCoolDown
                            } else {
                                // Check if scaffold has placed blocks near player
                                if (checkScaffoldSuccess(thePlayer)) {
                                    // Scaffold successfully placed blocks, disable everything
                                    resetFreezeStates()
                                    freezeCooldown = switchFreezeCoolDown
                                } else {
                                    // Keep freeze enabled while scaffold is working
                                    Freeze.state = true
                                    freezeOpened = true
                                }
                            }
                        } else {
                            // Simple freeze mode
                            Freeze.state = true
                            freezeOpened = true
                        }
                    }
                    "teleportback" -> {
                        thePlayer.setPositionAndUpdate(prevX, prevY, prevZ)
                        thePlayer.fallDistance = 0F
                        thePlayer.motionY = 0.0
                    }
                    "flyflag" -> {
                        thePlayer.motionY += 0.1
                        thePlayer.fallDistance = 0F
                    }
                    "ongroundspoof" -> sendPacket(C03PacketPlayer(true))
                    "motionteleport-flag" -> {
                        thePlayer.setPositionAndUpdate(thePlayer.posX, thePlayer.posY + 1f, thePlayer.posZ)
                        sendPacket(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                        thePlayer.motionY = 0.1
                        strafe()
                        thePlayer.fallDistance = 0f
                    }
                    "ghostblock" -> shouldSimulateBlock = true
                }
            } else if (mode == "Freeze" && (freezeOpened || scaffoldOpened) && freezeCooldown <= 0) {
                // Reset states when no longer in void danger
                resetFreezeStates()
            }
        }
    }

    /**
     * Handle Grim mode logic
     */
    private fun handleGrimMode(player: net.minecraft.entity.player.EntityPlayer) {
        if (player.heldItem == null) mc.timer.timerSpeed = 1.0f
        if (player.heldItem != null && player.heldItem.item is ItemEnderPearl) wait = true

        if (gotVelocity) {
            grimACTimer++
            if (grimACTimer <= 5) {
                val reduceFactor = 1.0 - (grimReduce / 10.0)
                mc.timer.timerSpeed = (0.4f + Math.random().toFloat() * 0.2f)
                player.motionX *= reduceFactor
                player.motionZ *= reduceFactor
                player.motionY *= 0.9
                val horizJitter = (Math.random() - 0.5) * 1.0E-6
                player.motionX += horizJitter
                player.motionZ += -horizJitter
            } else {
                mc.timer.timerSpeed = 1.0f
                gotVelocity = false
            }
        }

        if (player.onGround && gotVelocity) {
            gotVelocity = false
            grimACTimer = 0
            mc.timer.timerSpeed = 1.0f
        }

        val overVoid = !player.onGround && !isBlockUnder(30.0)
        if (!overVoid) {
            shouldStuck = false
            grimX = player.posX
            grimY = player.posY
            grimZ = player.posZ
            mc.timer.timerSpeed = 1.0f
        }
        if (overVoid) overVoidTicks++ else if (player.onGround) overVoidTicks = 0

        if (overVoid && position != null && motionVec != null && overVoidTicks < 30 + distance * 20) {
            if (!setBack) {
                wasVoid = true
                if (player.fallDistance > distance || setBack) {
                    player.fallDistance = 0f
                    setBack = true
                    shouldStuck = true
                    grimX = player.posX
                    grimY = player.posY
                    grimZ = player.posZ
                }
            }
        } else {
            if (shouldStuck) toggle()
            shouldStuck = false
            mc.timer.timerSpeed = 1.0f
            setBack = false
            if (wasVoid) wasVoid = false
            motionVec = Vec3(player.motionX, player.motionY, player.motionZ)
            position = Vec3(player.posX, player.posY, player.posZ)
        }

        if (shouldStuck && !player.onGround) {
            val microY = 1.0E-7
            val jitterX = (Math.random() - 0.5) * 1.0E-6
            val jitterZ = (Math.random() - 0.5) * 1.0E-6
            player.motionX = 0.0
            player.motionY = 0.0
            player.motionZ = 0.0
            player.setPositionAndRotation(grimX + jitterX, grimY + microY, grimZ + jitterZ, player.rotationYaw, player.rotationPitch)
        }
    }

    /**
     * Check if player is safe (on ground or near placed blocks)
     */
    private fun checkPlayerSafety(player: net.minecraft.entity.player.EntityPlayer): Boolean {
        // Check if directly on ground
        if (player.onGround && BlockPos(player).down().block !is BlockAir) {
            return true
        }

        // Check for nearby placed blocks in auto-scaffold mode
        if (mode == "Freeze" && autoScaffold && scaffoldOpened) {
            val playerPos = BlockPos(player)

            // Check blocks around player within scaffoldCheckDistance
            for (x in -2..2) {
                for (y in -1..1) {
                    for (z in -2..2) {
                        val checkPos = playerPos.add(x, y, z)
                        val distance = getDistanceBetween(playerPos, checkPos)
                        if (distance <= scaffoldCheckDistance) {
                            if (checkPos.block !is BlockAir) {
                                return true
                            }
                        }
                    }
                }
            }
        }

        return false
    }

    /**
     * Calculate distance between two BlockPos
     */
    private fun getDistanceBetween(pos1: BlockPos, pos2: BlockPos): Double {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        val dz = pos1.z - pos2.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    /**
     * Check if scaffold has successfully placed blocks near player
     */
    private fun checkScaffoldSuccess(player: net.minecraft.entity.player.EntityPlayer): Boolean {
        val playerPos = BlockPos(player)

        // Check if any non-air blocks are below player
        for (x in -1..1) {
            for (z in -1..1) {
                for (y in -3..0) { // Check below player
                    val checkPos = playerPos.add(x, y, z)
                    if (checkPos.block !is BlockAir) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Reset freeze related states
     */
    private fun resetFreezeStates() {
        if (freezeOpened) {
            Freeze.state = false
            freezeOpened = false
        }
        if (scaffoldOpened) {
            Scaffold.state = false
            scaffoldOpened = false
        }
        lastPlacedBlockPos = null
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        if (mode == "GhostBlock" && shouldSimulateBlock) {
            if (event.y < mc.thePlayer.posY.toInt()) {
                event.boundingBox = AxisAlignedBB(
                    event.x.toDouble(),
                    event.y.toDouble(),
                    event.z.toDouble(),
                    event.x + 1.0,
                    event.y + 1.0,
                    event.z + 1.0
                )
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        // Handle velocity packets for Grim modes
        if (packet is S12PacketEntityVelocity &&
            packet.entityID == player.entityId &&
            (mode == "OldGrim" || mode == "Grim")
        ) {
            gotVelocity = true
            grimACTimer = 0
        }

        // Handle position look packets for Grim mode
        if (packet is S08PacketPlayerPosLook && mode == "Grim") {
            grimX = packet.x
            grimY = packet.y
            grimZ = packet.z
            mc.timer.timerSpeed = 0.2f
        }

        // Grim mode packet handling
        if (mode == "Grim") {
            if (!player.onGround && shouldStuck && packet is C03PacketPlayer &&
                packet !is C03PacketPlayer.C05PacketPlayerLook &&
                packet !is C03PacketPlayer.C06PacketPlayerPosLook
            ) {
                event.cancelEvent()
            }

            if (packet is C08PacketPlayerBlockPlacement && wait) {
                shouldStuck = false
                mc.timer.timerSpeed = 0.2f
                wait = false
            }
        }

        // Track block placements in auto-scaffold mode
        if (mode == "Freeze" && autoScaffold && packet is C08PacketPlayerBlockPlacement) {
            if (packet.stack?.item is ItemBlock) {
                lastPlacedBlockPos = BlockPos(packet.position)
            }
        }

        // Stop considering non colliding blocks as collidable ones on setback.
        if (packet is S08PacketPlayerPosLook) {
            shouldSimulateBlock = false
        }

        if (!onScaffold && mode == "Blink" && pauseTicks > 0) {
            pauseTicks--
            return@handler
        }

        if (!onScaffold && mode == "Blink") {
            // Check for block placement
            if (packet is C08PacketPlayerBlockPlacement) {
                if (packet.stack?.item is ItemBlock) {

                    if (BlinkUtils.isBlinking && player.fallDistance < 1.5f) BlinkUtils.unblink()
                    if (pauseTicks < ticksToDelay) pauseTicks = ticksToDelay
                }
            }

            // Check for scaffold
            if ((Scaffold.handleEvents() || Tower.handleEvents()) && Scaffold.placeRotation != null) {
                if (BlinkUtils.isBlinking && player.fallDistance < 1.5f) BlinkUtils.unblink()
                if (pauseTicks < ticksToDelay) pauseTicks = ticksToDelay
            }
        }

        if (mode != "Blink" || !shouldBlink) return@handler

        if (player.isDead || player.ticksExisted < 20) {
            BlinkUtils.unblink()
            return@handler
        }

        if (Blink.blinkingSend() || Blink.blinkingReceive()) {
            BlinkUtils.unblink()
            return@handler
        }

        BlinkUtils.blink(packet, event, sent = true, receive = false)
    }

    val onRender3D = handler<Render3DEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (detectedLocation == null || !indicator ||
            thePlayer.fallDistance + (thePlayer.posY - (detectedLocation!!.y + 1)) < 3
        ) return@handler

        val (x, y, z) = detectedLocation ?: return@handler

        val renderManager = mc.renderManager

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glLineWidth(2f)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        glColor(Color(255, 0, 0, 90))
        drawFilledBox(
            AxisAlignedBB.fromBounds(
                x - renderManager.renderPosX,
                y + 1 - renderManager.renderPosY,
                z - renderManager.renderPosZ,
                x - renderManager.renderPosX + 1.0,
                y + 1.2 - renderManager.renderPosY,
                z - renderManager.renderPosZ + 1.0
            )
        )

        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)

        val fallDist = floor(thePlayer.fallDistance + (thePlayer.posY - (y + 0.5))).toInt()

        renderNameTag("${fallDist}m (~${max(0, fallDist - 3)} damage)", x + 0.5, y + 1.7, z + 0.5)

        resetColor()
    }
    /**
     * Check if there is a block under the player
     */
    private fun isBlockUnder(distance: Double = 30.0): Boolean {
        val player = mc.thePlayer ?: return false

        for (i in 1..distance.toInt()) {
            val pos = BlockPos(player.posX, player.posY - i, player.posZ)
            val block = pos.block

            if (block !is BlockAir) {
                return true
            }
        }
        return false
    }

    override val tag: String?
        get() = if ((mode == "Grim" || mode == "OldGrim") && gotVelocity) "ReducingKB" else mode
}