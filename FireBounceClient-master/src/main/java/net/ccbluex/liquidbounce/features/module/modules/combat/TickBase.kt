/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isHurting
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object TickBase : Module("TickBase", Category.COMBAT) {

    private val tagMode by choices("TagMode",arrayOf("ModuleMode","WorkRange","Chance","Custom"),"ModuleMode")
    private val customText by text("CustomTagText","") {tagMode == "Custom"}
    private val mode by choices("Mode", arrayOf("Past", "Future"), "Past")
    private val onlyOnKillAura by boolean("OnlyOnKillAura", true)

    // 添加 StartMode 配置
    private val startMode by choices("StartMode", arrayOf("InRange", "onAttack"), "InRange")

    private val Chance by int("Chance", 100, 0..100,"%")

    private val balanceMaxValue by int("BalanceMaxValue", 100, 1..1000)
    private val balanceRecoveryIncrement by float("BalanceRecoveryIncrement", 0.1f, 0.01f..10f)
    private val maxTicksRange by intRange("MaxTicksRange", 10..20, 1..100)

    private val rangeToAttack by floatRange("RangeToAttack", 3f..5f, 0f..10f)

    private val forceGround by boolean("ForceGround", false)
    private val pauseAfterTick by int("PauseAfterTick", 0, 0..100)
    private val cooldown by int("Cooldown", 250, 0..30000,"ms")
    private val pauseOnFlag by boolean("PauseOnFlag", true)
    private val onlyForward by boolean("OnlyForward",false)
    private val stopOnHurting by boolean("StopOnHurting",true)
    private val outPut by boolean("DebugMessage",false)


    private val line by boolean("Line", true).subjective()
    private val lineColor by color("LineColor", Color.GREEN) { line }.subjective()

    private var ticksToSkip = 0
    private var tickBalance = 0f
    private var reachedTheLimit = false
    private val tickBuffer = mutableListOf<TickData>()
    var duringTickModification = false
    private var cooldownMSTimer = MSTimer()

    // 添加 started 变量
    private var started = false
    // 添加一个标志来追踪是否在 tickSkip 期间
    private var duringTickSkip = false

    override val tag
        get() = when (tagMode) {
            "ModuleMode" -> mode
            "WorkRange" -> "${rangeToAttack.start} - ${rangeToAttack.endInclusive}"
            "Chance" -> "$Chance%"
            else -> customText
        }

    override fun onToggle(state: Boolean) {
        duringTickModification = false
        duringTickSkip = false
        started = false
        cooldownMSTimer.reset()
    }

    val onPreTick = handler<PlayerTickEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (player.ridingEntity != null || Blink.handleEvents()) {
            return@handler
        }

        if (event.state == EventState.PRE && ticksToSkip-- > 0) {
            event.cancelEvent()
            duringTickSkip = true
        } else if (ticksToSkip <= 0 && duringTickSkip) {
            // tickSkip 结束后重置 started
            if (startMode == "onAttack") {
                started = false
            }
            duringTickSkip = false
        }
    }

    private var modificationFlag = false

    val onGameTick = handler<GameTickEvent>(dispatcher = Dispatchers.Main, priority = 1) {
        val player = mc.thePlayer ?: return@handler

        if (player.ridingEntity != null || Blink.handleEvents()) return@handler

        // 检查 StartMode 条件
        if (startMode == "onAttack" && !started) {
            return@handler
        }

        if (!cooldownMSTimer.hasTimePassed(cooldown)) {
            duringTickModification = false
            return@handler
        }
        if (onlyForward && mc.thePlayer.movementInput.moveForward < 0.707) return@handler
        if (stopOnHurting && mc.thePlayer.isHurting()) return@handler

        if (duringTickModification) {
            duringTickModification = false
            return@handler
        }

        if (tickBuffer.isNotEmpty()) {
            val nearbyEnemy = getNearestEntityInRange() ?: return@handler
            val currentDistance = player.positionVector.distanceTo(nearbyEnemy.positionVector)

            val possibleTicks = tickBuffer.mapIndexedNotNull { index, tick ->
                val tickDistance = tick.position.distanceTo(nearbyEnemy.positionVector)
                (index to tick).takeIf {
                    tickDistance < currentDistance &&
                            tickDistance in rangeToAttack &&
                            !tick.isCollidedHorizontally &&
                            (!forceGround || tick.onGround)
                }
            }

            val criticalTick = possibleTicks
                .filter { (_, tick) -> tick.fallDistance > 0.0f }
                .minByOrNull { (index, _) -> index }

            val (bestTick, _) = criticalTick ?: possibleTicks.minByOrNull { (index, _) -> index } ?: return@handler

            if (bestTick == 0) return@handler

            val actualSkipTicks = bestTick + pauseAfterTick
            val maxAllowedSkip = maxTicksRange.endInclusive + pauseAfterTick
            val skipTicks = actualSkipTicks.coerceAtMost(maxAllowedSkip)

            val minRequiredSkip = maxTicksRange.start + pauseAfterTick
            if (skipTicks < minRequiredSkip) {
                ticksToSkip = 0
                return@handler
            }

            if (RandomUtils.nextInt(endExclusive = 100) > Chance ||
                (onlyOnKillAura && (!state || KillAura.target == null))
            ) {
                ticksToSkip = 0
                return@handler
            }

            cooldownMSTimer.reset()
            duringTickModification = true

            fun executeTickModification() {
                repeat(skipTicks) {
                    player.onUpdate()
                    tickBalance -= 1
                }
            }

            if (mode == "Past") {
                ticksToSkip = skipTicks
                waitTicks(skipTicks)
                executeTickModification()
                modificationFlag = true
            } else {
                executeTickModification()
                ticksToSkip = skipTicks
                waitTicks(skipTicks)
                modificationFlag = true
            }

            // 输出调试信息
            if (outPut) {
                val firstTickData = tickBuffer.first()
                val startPos = firstTickData.startPos ?: player.positionVector
                val movedDistance = player.positionVector.distanceTo(startPos)
                val stoppedTicks = skipTicks - pauseAfterTick
                chat("§b[TickBase] §7TeleportRange: §a${"%.3f".format(movedDistance)} §7SkipTick: §a$stoppedTicks")
            }
        }
    }

    val onMove = handler<MoveEvent> {
        val player = mc.thePlayer ?: return@handler

        if (player.ridingEntity != null || Blink.handleEvents()) {
            return@handler
        }

        // 检查 StartMode 条件
        if (startMode == "onAttack" && !started) {
            return@handler
        }

        // 清空旧的 Tick 数据
        tickBuffer.clear()

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)
        simulatedPlayer.rotationYaw = RotationUtils.currentRotation?.yaw ?: player.rotationYaw

        // 记录模拟开始时的玩家位置（用于后续计算移动距离）
        val startPos = player.positionVector

        // 如果 tickBalance 不足或达到限制，跳过模拟
        if (tickBalance <= 0) {
            reachedTheLimit = true
        }
        if (tickBalance > balanceMaxValue / 2) {
            reachedTheLimit = false
        }
        if (tickBalance <= balanceMaxValue) {
            tickBalance += balanceRecoveryIncrement
        }

        if (reachedTheLimit) return@handler

        // 模拟未来/过去的 Tick 数据
        repeat(minOf(tickBalance.toInt(), maxTicksRange.endInclusive * if (mode == "Past") 2 else 1)) {
            simulatedPlayer.tick()
            tickBuffer += TickData(
                position = simulatedPlayer.pos,
                fallDistance = simulatedPlayer.fallDistance,
                motionX = simulatedPlayer.motionX,
                motionY = simulatedPlayer.motionY,
                motionZ = simulatedPlayer.motionZ,
                onGround = simulatedPlayer.onGround,
                isCollidedHorizontally = simulatedPlayer.isCollidedHorizontally,
                startPos = startPos
            )
        }
    }

    // 添加 onAttack 事件处理器
    val onAttack = handler<AttackEvent> { event ->
        if (EntityUtils.isSelected(event.targetEntity, true)) {
            started = true
        }
    }

    val onDelayedPacketProcess = handler<DelayedPacketProcessEvent> {
        if (duringTickModification) {
            it.cancelEvent()
        }
    }

    val onRender3D = handler<Render3DEvent> {
        if (!line) return@handler

        if (startMode == "onAttack" && !started) {
            return@handler
        }

        synchronized(tickBuffer) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(lineColor)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (tick in tickBuffer) {
                glVertex3d(
                    tick.position.xCoord - renderPosX,
                    tick.position.yCoord - renderPosY,
                    tick.position.zCoord - renderPosZ
                )
            }

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is S08PacketPlayerPosLook && pauseOnFlag) {
            tickBalance = 0f
            cooldownMSTimer.reset()
            if (startMode == "onAttack") {
                started = false
            }
        }
    }

    private data class TickData(
        val position: Vec3,
        val fallDistance: Float,
        val motionX: Double,
        val motionY: Double,
        val motionZ: Double,
        val onGround: Boolean,
        val isCollidedHorizontally: Boolean,
        val startPos: Vec3? = null
    )

    private fun getNearestEntityInRange(): EntityLivingBase? {
        val player = mc.thePlayer ?: return null
        val entities = mc.theWorld.loadedEntityList ?: return null

        return entities.asSequence().filterIsInstance<EntityLivingBase>()
            .filter { EntityUtils.isSelected(it, true) }.minByOrNull { player.getDistanceToEntity(it) }
    }
}