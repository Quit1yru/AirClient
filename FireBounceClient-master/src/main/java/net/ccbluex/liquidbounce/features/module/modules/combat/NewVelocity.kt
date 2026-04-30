/**
 * @author FireFly
 * 由于FireFly实在看不惯自己写的大神Velocity于是写了一个新的
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.bps
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.bpt
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.calculateAngleDifference
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeSprint
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isHurting
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceY
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.roundToPlacesIfNeeded
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.runAttack
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.setBPSTo
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.setMotion
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickSkipManager
import net.minecraft.block.BlockSoulSand
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.WorldSettings
import javax.vecmath.Vector2d
import kotlin.math.*

object NewVelocity : Module("NewVelocity", Category.COMBAT) {

    private val tagMode by choices("TagMode", arrayOf("Normal", "Custom", "None"), "Normal")
    private val customText by text("CustomText", "") { tagMode == "Custom" }
    private val mode by choices(
        "Mode", arrayOf(
            // Normal Mode
            "AttackReduce",
            "Intave", // TestedInHype-mc
            "Intave2", "IntaveSafe", "OldIntave",
            "Matrix",
            "PolarJump",
            "Delay",
            "LegitClick",
            "LegitClick2",
            "OldGrim",

            // Jump
            "JumpReset",
            "AirJumpReset",
            "FakeJump",


            // Specific Server
            "MineBerryNew",
            "MineMenClub",


            // Packet Mode
            "NoC0F",
            "GrimExempt117",

            // Test Mode
            "Prediction",
            "Tatako0.9.6.1To0.9.7.3-a2",

            // Funny Packet Mode
            "XZSwitch", // Switch X-side knockback and Z-side knockback
        ), "JumpReset"
    )

    // Prediction
    private val blinkTicks by int("BlinkTicks",5,1..10) {mode == "Prediction"}
    private var preBlinking = false
    private var preShouldBlink = false
    private var preShouldAttack = false

    // MineBerryNew
    private val mineBerryMinWorkHurtTime by int("MinWorkHurtTime",1,1..10) {mode == "MineBerryNew"}
    private var mineBerryFirstReduce = false

    // OldGrim
    private val grimrange by float("OldGrimWorkRange", 3.5f, 0f..6f) { mode == "OldGrim" }
    private val attackCountValue by int("Attack Counts", 12, 1..16) { mode == "OldGrim" }
    private val fireCheckValue by boolean("FireCheck", false) { mode == "OldGrim" }
    private val waterCheckValue by boolean("WaterCheck", false) { mode == "OldGrim" }
    private val fallCheckValue by boolean("FallCheck", false) { mode == "OldGrim" }
    private val consumecheck by boolean("ConsumableCheck", false) { mode == "OldGrim" }
    private val raycastValue by boolean("Ray cast", false) { mode == "OldGrim" }
    var entity: Entity? = null
    var velX = 0
    var velY = 0
    var velZ = 0

    // Intave
    private val blinkWorkMaxDistance by float("BlinkWorkMaxDistance",3.5f,0.0f..6.0f) {mode == "Intave"}
    private val maxBlinkTicks by int("MaxBlinkTicks",10,0..10) {mode == "Intave"}
    private val intaveJumpReset by boolean("IntaveJumpReset", true) { mode == "Intave" }
    private val intaveJumpResetSprint by boolean("ForceSprintJump", false) { mode == "Intave" && intaveJumpReset }
    private val intaveJumpResetNeedForward by boolean("ForceSprintJumpNeedForward", true) { mode == "Intave" && intaveJumpReset && intaveJumpResetSprint}
    private val extraC0APerReduce by boolean("ExtraC0APerReduce", false) { mode == "Intave" }
    private val extraPacketCount by int("ExtraC0APacketCount", 1, 1..5) { mode == "Intave" && extraC0APerReduce }
    private val moreReduce by boolean("MoreReduce",false) {mode == "Intave"}
    private val maxMoreReduce by int("MaxMoreReduceCount",3,1..6) {moreReduce && mode == "Intave"}
    private val onlyWhenNeed by boolean("OnlyWhenNeed",true) { mode == "Intave"}
    private val intaveSafe by boolean("IntaveSafe",true){mode == "Intave"}
    private var hasReceivedVelocity = false
    enum class IntavePhase {
        PHASE_1, PHASE_2, PHASE_3, PHASE_4, PHASE_5, PHASE_6
    }
    val triggeredPhases = mutableSetOf<IntavePhase>()
    private var previousTimerState = 0
    var intaveReversed = false
    private var timerState = 0
    private var boosting = true
    private var slowing = false
    private var intaveClickTimes = 0
    var moreReduceTimes = 0
    private var canOutPutMessage = false
    internal var shouldBlink = false
    internal var lastBlinkState = false
    var intaveReduceTimes = 0

    @JvmField
    var canCancelHitSlow = false

    // Intave2
    private val minFactor by float("MinFactor",0.4f,0f..1f, description = "The smallest reachable factor") {mode == "Intave2"}
    private var intave2ReduceCounter = 0

    // Matrix
    private var matrixBoost by boolean("BoostAfterReduce", false) { mode == "Matrix" }
    private var matrixBoostFactor by float("BoostFactor", 0.33f, 0.0f..5.0f) { mode == "Matrix" && matrixBoost }
    private var matrixBoostDelay by int("BoostCooldown", 0, 0..2000, "ms") { mode == "Matrix" && matrixBoost }
    private var matrixBoostTimer = MSTimer()
    private var matrixMotionYReduce = false

    // LegitClick
    private val clicks by intRange("Clicks", 1..2, 1..20) { mode == "LegitClick" }
    private val durationHurtTime by int("DurationHurtTimes", 1, 1..9) { mode == "LegitClick" }
    private val clickDelayTicks by int("ClickCooldownTicks",0,0..10) {mode == "LegitClick"}
    private val clickChancePerClick by float("ClickChancePerClick", 1.0f, 0.0f..1.0f) { mode == "LegitClick" }
    private val whenFacingEnemyOnly by boolean("WhenFacingEnemyOnly", true) { mode == "LegitClick" }
    private val ignoreBlocking by boolean("IgnoreBlocking", false) { mode == "LegitClick" }
    private val clickRange by float("ClickRange", 3f, 1f..6f) { mode == "LegitClick" }
    private val swingMode by choices("SwingMode", arrayOf("Off", "Normal", "Packet"), "Normal") { mode == "LegitClick" }
    private val modifyMotionWhenClick by boolean("ModifyMotionWhenClick", false) { mode == "LegitClick" }
    private val makeVanillaAttackNotStopSprint by boolean(
        "MakeVanillaAttackNotStopSprint",
        false
    ) { modifyMotionWhenClick && mode == "LegitClick" }
    private val modifyMotionFactor by float(
        "XZFactor",
        0.6f,
        -1.0f..1.0f
    ) { modifyMotionWhenClick && mode == "LegitClick" }
    private var attackStartHurtTime = 0
    private var clickDelayTick = 0

    private val click2MaxTimes by int("LegitClick2MaxClickTimes",3,1..20) {
        mode == "LegitClick2"
    }
    private val addClicksPerUserClick by int("AddClicksPerUserClick",1,1..20) {
        mode == "LegitClick2"
    }
    private var legitClick2Times = 0

    // MinemenClub
    private var mineMenClubDelay by int("PacketCancelDelay", 20, 0..20) { mode == "MineMenClub" }
    private var minemenClubCounter = 0

    // Delay
    private val delayTicks by int("DelayTicks", 3, 1..20) { mode == "Delay" }
    private val delayChance by int("DelayChance", 100, 0..100) { mode == "Delay" }
    private val delayHorizontal by float("DelayHorizontal", 0F, -1F..1F) { mode == "Delay" }
    private val delayVertical by float("DelayVertical", 0F, -1F..1F) { mode == "Delay" }
    private val delayAttackReduce by boolean("DelayAttackReduce",false) {mode == "Delay"}
    private val delayFakeCheck by boolean("DelayFakeCheck", true) { mode == "Delay" }
    private var delayChanceCounter = 0
    private var delayActive = false
    private var delayReverseFlag = false
    private var delayPendingExplosion = false
    private var delayAllowNext = true
    private val delayedPackets = LinkedHashMap<Packet<*>, Long>()
    private val delayTimer = MSTimer()
    private var delayTickCounter = 0

    // AttackReduce
    private val attackReduceFactor by float("AttackXZFactor",0.6f,0.0f..1.0f) {mode == "AttackReduce"}
    private val attackHurtTime by intRange("AttackHurtTime",9..9,1..10) {mode == "AttackReduce"}


    // JumpReset (General)
    private val jumpReset by boolean("JumpReset", false) {
        mode !in arrayOf(
            "Intave",
            "JumpReset",
            "AirJumpReset",
            "PolarJump",
            "IntaveSafe"
        )
    }

    // JumpReset (Proprietary)
    private val jumpResetChance by int(
        "JumpChance",
        100,
        0..100,
        "%"
    ) { displayJumpResetChoices() }
    private val jumpCooldownMode by multiChoices(
        "JumpCooldownMode",
        arrayOf("Tick", "ReceivedHit"),
        arrayOf("ReceivedHit")
    ) { displayJumpResetChoices() }
    private val jumpCooldownTick by int(
        "JumpCooldownTicks",
        4,
        0..20
    ) { displayJumpResetChoices() && "Tick" in jumpCooldownMode}
    private val jumpCooldownReceivedHit by int(
        "JumpCooldownReceivedHit",
        1,
        0..5
    ) { displayJumpResetChoices() && "ReceivedHit" in jumpCooldownMode}
    private val checkUserSprint by boolean(
        "CheckUserIsSprinting",
        true
    ) { displayJumpResetChoices() }
    private val matrixJumpTest by boolean("MatrixJumpReset",false) { displayJumpResetChoices() }
    private var jumpCooldownTickCounter = 0
    private var jumpCooldownReceivedHitCounter = 0

    // PolarJump
    private var polarHurtTime = nextInt(7, 10)

    // pauseOnExplosion
    private val pauseOnExplosion by boolean("PauseOnExplosion",false)
    private val pauseTicks by int("PauseTicks",20,0..100) {pauseOnExplosion}
    private var pausedTicks = 0

    // WorkTime
    private val allowWorkWhen by multiChoices("AllowWorkWhen",arrayOf("OnGround","InAir","NeedSprinting","NeedKillAura","NeedMoving","NeedSneaking"),arrayOf("OnGround","InAir"))

    val debugMessage by boolean("DebugMessage",false) {
        mode in arrayOf(
            "Intave","IntaveSafe","Intave2",
            "LegitClick",
            "JumpReset",
            "AirJumpReset",
            "OldGrim",
            "Delay",
            "MineBerryNew",
            "FakeJump"
        )
    }
    private val smartJumpReset by boolean("SmartJumpReset",false)
    private var shouldCancelAttack = false
    private var shouldAttackCount = 0

    private var hasJumpReset = false

    var packetMotionX = 0.0
    private var packetMotionY = 0.0
    var packetMotionZ = 0.0

    var globalTarget: EntityLivingBase? = null

    var sprintTimer = MSTimer()


    val doNotNeedReduce: Boolean
        get() = mc.thePlayer.hurtTime == 0 || knockBackIsNegated(packetMotionX,packetMotionZ)
    override val tag: String?
        get() = when (tagMode) {
            "Normal" -> mode
            "None" -> null
            else -> customText
        }
    val onAttack = handler<AttackEvent> { event ->
        globalTarget = event.targetEntity as EntityLivingBase?
        if (!canWorkNow()) return@handler
        when (mode) {
            "Intave" -> {
                when (mc.thePlayer.hurtTime) {
                    10 -> intaveReduce(0,intaveSafe)
                    9, 6, 3 -> intaveReduce(1,intaveSafe)
                    8, 5, 2 -> intaveReduce(2,intaveSafe)
                    7, 4, 1 -> intaveReduce(3,intaveSafe)
                }
            }
            "MineBerryNew" -> {
                if (mc.thePlayer.hurtTime >= mineBerryMinWorkHurtTime) {
                    if (CPSCounter.getCPS(CPSCounter.MouseButton.LEFT) > 22) return@handler
                    val attackCount = if (mineBerryFirstReduce) 3 else 1
                    sendPacket(C0APacketAnimation(),true)
                    runAttack(maxDistance = 3.0f, attackCount = attackCount, silentAttack = true, debugMessage = debugMessage, ignoreBlocking = true)
                    if (mineBerryFirstReduce) mineBerryFirstReduce = false
                    sendPacket(C0APacketAnimation(),true)
                }
            }
            "AttackReduce" -> {
                if (mc.thePlayer.hurtTime in attackHurtTime) {
                    reduceXZ(attackReduceFactor.toDouble())
                }
            }
            "Intave2" -> {
                val reduceFactor = max(1-(intave2ReduceCounter*0.1),minFactor.toDouble())
                reduceXZ(reduceFactor)
                debugMessage("Intave2Reduce")
                intave2ReduceCounter++
            }
            "OldIntave" -> {
                if (mc.thePlayer.hurtTime in 2..10) reduceXZ(0.75)
                if (mc.thePlayer.hurtTime in 1..4) {
                    if (mc.thePlayer.motionY > 0) reduceY(0.9) else reduceY(1.1)
                }
            }

            "IntaveSafe" -> {
                when (mc.thePlayer.hurtTime) {
                    9 -> {
                        reduceXZ(0.6)
                        debugMessage("IntaveSafeReduce")
                    }
                    8 -> {
                        reduceXZ(0.8)
                        debugMessage("IntaveSafeReduce")
                    }
                }
            }
            "LegitClick2" -> {
                extraJumpReset()
                if (mc.thePlayer.isHurting()) {
                    if (legitClick2Times < click2MaxTimes) {
                        sendPacket(C02PacketUseEntity(mc.pointedEntity, C02PacketUseEntity.Action.ATTACK),false)
                        mc.thePlayer.swingItem()
                        legitClick2Times = legitClick2Times + addClicksPerUserClick
                    }
                }
            }
            "OldGrim" -> {
                mc.netHandler.networkManager.sendPacket(C0APacketAnimation())
                mc.netHandler.networkManager.sendPacket(
                    C02PacketUseEntity(
                        event.targetEntity,
                        C02PacketUseEntity.Action.ATTACK
                    )
                )
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer.hurtTime == 0 && hasReceivedVelocity) hasReceivedVelocity = false

        if (clickDelayTick != 0) clickDelayTick--
        if (!canWorkNow()) return@handler
        val player = mc.thePlayer ?: return@handler

        shouldCancelAttack = shouldJumpReset(false)
        updateJumpResetCooldown()

        if (!shouldCancelAttack && shouldAttackCount != 0 && hasJumpReset) {
            runAttack(false, fakeSwing = true, attackCount = shouldAttackCount, silentAttack = true)
            shouldAttackCount = 0
        }

        when (mode) {
            "Intave" -> {

                val shouldStop = (mc.thePlayer.hurtTime == 10 - maxBlinkTicks) || maxBlinkTicks == 0
                if (shouldStop && lastBlinkState) {
                    stopIntaveBlink()
                }
                val checkSprint = !intaveJumpResetSprint
                if (intaveJumpReset) {
                    if (shouldJumpReset(checkSprint = checkSprint, checkOnGround = true, checkMoving = intaveJumpResetNeedForward ,needReceivedS12 = true, needForward = intaveJumpResetNeedForward)
                    ) {
                        if (intaveJumpResetSprint) changeSprint(setState = true, forceChange = true, sendPacketToServer = true)
                        player.jump()

                        debugMessage("Jump | ${mc.thePlayer.hurtTime}")
                    }
                }

                if (packetMotionValid() && triggeredPhases.contains(IntavePhase.PHASE_1) &&triggeredPhases.contains(IntavePhase.PHASE_2) && triggeredPhases.contains(IntavePhase.PHASE_3) && mc.thePlayer.hurtTime != 0 && canOutPutMessage) {
                    debugMessage("$packetMotionX $packetMotionZ -> ${playerNowMotionOutPut(
                        x = true,
                        y = false,
                        z = true
                    )}")
                    canOutPutMessage = false
                }
            }
            "FakeJump" -> {
                val e = mc.thePlayer.motionY
                if (mc.thePlayer.hurtTime == 9 && !mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.motionY != 0.42) {
                    mc.thePlayer.jump()
                    debugMessage("Jump")
                    if (e != mc.thePlayer.motionY) mc.thePlayer.motionY = e
                }
            }
            "Prediction" -> {
                preShouldBlink = mc.thePlayer.hurtTime in (10-blinkTicks)..10 && hasReceivedVelocity
            }
            "IntaveSafe" -> {
                if (shouldJumpReset(true) && mc.thePlayer.ticksExisted % 2 == 0) player.tryJump()
                debugMessage("Jump")
            }

            "JumpReset" -> {
                if (shouldJumpReset(checkUserSprint, checkMoving = false)
                ) {
                    player.jump()
                    if (matrixJumpTest) mc.thePlayer.motionY = packetMotionY
                    debugMessage("Jump")
                    hasReceivedVelocity = false
                    resetJumpCooldownCounter()
                }
            }

            "AirJumpReset" -> {
                if (shouldJumpReset(checkSprint = false, checkOnGround = false)
                ) {
                    player.jump()
                    debugMessage("Jump")
                    hasReceivedVelocity = false
                    resetJumpCooldownCounter()
                }
            }

            "Matrix","LegitClick","LegitClick2", "MineMenClub","NoC0F","GrimExempt117","XZSwitch","OldIntave","AttackReduce","MineBerryNew" -> {
                extraJumpReset()
                when (mode) {
                    "LegitClick" -> handleLegitClick()
                    "MineMenClub" -> minemenClubCounter++
                }
            }
            "Delay" -> {
                if (delayReverseFlag && (
                            canDelay() ||
                                    isInLiquidOrWeb() ||
                                    delayTickCounter >= delayTicks
                            )
                ) {

                    applyDelayedVelocity()
                    delayReverseFlag = false
                    delayTickCounter = 0
                    delayTimer.reset()
                }

                if (delayReverseFlag) {
                    delayTickCounter++
                }

                if (delayActive) {
                    val speed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
                    if (speed > 0.1) {
                        val yaw = Math.toDegrees(atan2(player.motionZ, player.motionX)).toFloat() - 90.0f
                        player.motionX = -sin(Math.toRadians(yaw.toDouble())) * speed
                        player.motionZ = cos(Math.toRadians(yaw.toDouble())) * speed
                    }
                    delayActive = false
                } else extraJumpReset()
            }

            "PolarJump" -> {
                if (shouldJumpReset(checkSprint = true, checkOnGround = true,checkMoving = true,needReceivedS12 = true,polarMode = true)) {
                    player.tryJump()
                    polarHurtTime = nextInt(7, 10)
                    if (mc.thePlayer.hurtTime == 0) hasReceivedVelocity = false
                }
            }
            "OldGrim" -> {
                if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.onGround) {
                    mc.thePlayer.addVelocity(-1.3E-10, -1.3E-10, -1.3E-10)
                    mc.thePlayer.isSprinting = false
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (!canWorkNow()) return@handler
        if (event.isCancelled) return@handler
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet
        if (packet.isAttackPacketAndSwingPacket &&
            shouldCancelAttack &&
            smartJumpReset &&
            !hasJumpReset
        ) {
            event.cancelEvent()
            if (packet.isAttackPacket) shouldAttackCount++
        }
        if (packet is S27PacketExplosion && pauseOnExplosion) {
            pausedTicks = pauseTicks
            return@handler
        }
        when {
            mode == "NoC0F" && (packet is C0FPacketConfirmTransaction && mc.thePlayer.isHurting() ||
                    (packet is S12PacketEntityVelocity && isValidS12Packet(packet)) || packet is S27PacketExplosion) -> {
                event.cancelEvent()
                return@handler
            }
        }

        // OtherMode
        when {
            packet is S12PacketEntityVelocity && isValidS12Packet(packet) -> {
                packetMotionX = roundToPlacesIfNeeded(packet.realMotionX)
                packetMotionY = roundToPlacesIfNeeded(packet.realMotionY)
                packetMotionZ = roundToPlacesIfNeeded(packet.realMotionZ)
                sprintTimer.reset()
                when (mode) {
                    "PolarJump", "JumpReset", "AirJumpReset", "LegitClick" -> {
                        hasReceivedVelocity = true
                    }
                    "MineBerryNew" -> mineBerryFirstReduce = true
                    "OldGrim" -> {
                        if (mc.thePlayer.isDead) return@handler
                        if (mc.currentScreen is GuiGameOver) return@handler
                        if (mc.playerController.currentGameType === WorldSettings.GameType.SPECTATOR) return@handler
                        if (mc.thePlayer.isOnLadder) return@handler
                        if (mc.thePlayer.isBurning && fireCheckValue) return@handler
                        if (mc.thePlayer.isInWater && waterCheckValue) return@handler
                        if (mc.thePlayer.fallDistance > 1.5 && fallCheckValue) return@handler
                        if (mc.thePlayer.isEating && consumecheck) return@handler
                        if (soulSandCheck()) return@handler
                        if (packet.entityID == mc.thePlayer.entityId) {
                            //            chat("触发反击退但还没攻击")
                            val s12 = event.packet
                            val horizontalStrength =
                                Vector2d(s12.getMotionX().toDouble(), s12.getMotionZ().toDouble()).length()
                            if (horizontalStrength <= 1000) return@handler
                            val mouse = mc.objectMouseOver
                            var entity: Entity? = null

                            if (mouse.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mouse.entityHit is EntityLivingBase && mc.thePlayer.getDistanceToEntityBox(
                                    mouse.entityHit
                                ) <= KillAura.range
                            ) {
                                entity = mouse.entityHit
                            }

                            if (entity == null && !raycastValue) {
                                val target: Entity? = KillAura.target
                                if (target != null && mc.thePlayer.getDistanceToEntityBox(target) <= grimrange) {
                                    entity = KillAura.target
                                }
                            }

                            val state = mc.thePlayer.serverSprintState
                            if (entity != null) {
                                if (!state) {
                                    sendPackets(C0BPacketEntityAction(mc.thePlayer,
                                        C0BPacketEntityAction.Action.START_SPRINTING
                                    ))
                                }
                                repeat(attackCountValue) {
                                    mc.netHandler.networkManager.apply {
                                        sendPacket(C0APacketAnimation())
                                        sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
                                    }
                                }
                                if (!state) {
                                    sendPackets(C0BPacketEntityAction(mc.thePlayer,
                                        C0BPacketEntityAction.Action.STOP_SPRINTING
                                    ))
                                }
                                velX = event.packet.motionX
                                velY = event.packet.motionY
                                velZ = event.packet.motionZ
                                event.cancelEvent()
                            }
                        }
                    }
                    "LegitClick2" -> {
                        hasReceivedVelocity = true
                        legitClick2Times = 0
                    }

                    "Intave" -> {
                        hasReceivedVelocity = true
                        triggeredPhases.clear()
                        intaveClickTimes = 0
                        moreReduceTimes = 0
                        canOutPutMessage = true
                        shouldBlink = true
                        intaveReversed = false
                        timerState = 0
                        boosting = false
                        slowing = false
                        intaveReduceTimes = 0
                    }

                    "Intave2" -> intave2ReduceCounter = 0
                    "XZSwitch" -> {
                        event.cancelEvent()
                        setMotion(packet.realMotionZ, packet.realMotionY, packet.realMotionX)
                    }


                    "GrimExempt117" -> {
                        sendPacket(
                            C06PacketPlayerPosLook(
                                player.posX, player.posY, player.posZ,
                                player.rotationYaw, player.rotationPitch, player.onGround
                            ), false
                        )
                        sendPacket(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                player.position, EnumFacing.DOWN
                            )
                        )
                        event.cancelEvent()
                    }

                    "Matrix" -> handleMatrixVelocity(packet, event)

                    "Delay" -> handleDelayVelocity(packet, event)

                    "MineMenClub" -> {
                        if (minemenClubCounter > mineMenClubDelay) {
                            event.cancelEvent()
                            minemenClubCounter = 0
                        } else {
                            hasReceivedVelocity = true
                        }
                    }
                }
            }

            packet is S27PacketExplosion -> {
                when (mode) {
                    "Delay" -> handleExplosionDelay(packet, event)
                    "MineMenClub" -> {
                        if (minemenClubCounter > mineMenClubDelay) {
                            event.cancelEvent()
                            minemenClubCounter = 0
                        }
                    }
                }
            }
        }
        when (mode) {
            "Intave" -> {
                val blinkDistanceCheck = when {
                    blinkWorkMaxDistance == 0.0f -> true
                    KillAura.target == null -> false
                    else -> getDistance(KillAura.target!!) <= blinkWorkMaxDistance
                }
                if (shouldBlink && mc.thePlayer.hurtTime > 0 && blinkDistanceCheck && maxBlinkTicks > 0) {
                    BlinkUtils.blink(packet,event,true, receive = true, {p -> p is S12PacketEntityVelocity || p is C03PacketPlayer || p is C02PacketUseEntity || p is C08PacketPlayerBlockPlacement || p is C07PacketPlayerDigging})

                    if (!lastBlinkState) debugMessage("Blinking")
                    lastBlinkState = true
                } else if (!blinkDistanceCheck && BlinkUtils.isBlinking) {
                    BlinkUtils.unblink()
                    if (lastBlinkState) debugMessage("Out of range,stop blink")
                    lastBlinkState = false
                }
            }
            "Prediction" -> {
                if (packet is S12PacketEntityVelocity && packet.isSelfVelocityVelocity && isValidS12Packet(packet)) {
                    preShouldAttack = true
                    hasReceivedVelocity = true
                }
                if (packet.isSelfVelocityVelocity && mc.thePlayer.onGround && mc.thePlayer.isSprinting && !mc.gameSettings.keyBindJump.isKeyDown) mc.thePlayer.jump()
                if (preShouldBlink) {
                    BlinkUtils.blink(packet,event, receive = false)
                    preBlinking = true
                    if (preShouldAttack) {
                        runAttack(attackCount = blinkTicks, silentAttack = true, fakeSwing = true)
                        preShouldAttack = false
                    }
                }
                if (!preShouldBlink && preBlinking) BlinkUtils.unblink()
            }
        }
    }
    val onMotion = handler<MotionEvent> { event ->
        when(mode){
            "Tatako0.9.6.1To0.9.7.3-a2" -> {
                if (mc.thePlayer.hurtTime >= 6) {
                    //fake click
                    sendPackets(
                        C0APacketAnimation(),
                        C08PacketPlayerBlockPlacement(
                            mc.thePlayer.position.down(),
                            EnumFacing.UP.index,
                            mc.thePlayer.inventory.getCurrentItem(),
                            nextFloat(), nextFloat(), nextFloat() //Bypass scaffold
                        )
                    )
                    event.y -= event.y%64
                    //force tatako to lag u to the ground
                    event.onGround = true
                }
            }
        }
    }
    val onTick = handler<GameTickEvent> { event ->
        if (pausedTicks > 0) {
            pausedTicks--
            return@handler
        }
        if (!canWorkNow()) return@handler
        val player = mc.thePlayer ?: return@handler
        when (mode) {
            "Delay" -> {
                if (delayReverseFlag && delayTimer.hasTimePassed(50L * delayTicks)) {
                    applyDelayedVelocity()
                    delayReverseFlag = false
                    delayTickCounter = 0
                    delayTimer.reset()
                }

                if (player.hurtTime == 0) {
                    delayPendingExplosion = false
                    delayAllowNext = true
                }
            }
        }
    }
    val onJump = handler<JumpEvent> { event ->
        hasJumpReset = mc.thePlayer.hurtTime == 9 && mc.thePlayer.isSprinting
    }

    val onWorld = handler<WorldEvent> {
        pausedTicks = 0
    }

    override fun onDisable() {
        hasReceivedVelocity = false
        matrixBoostTimer.reset()
        jumpCooldownTickCounter = 0
        jumpCooldownReceivedHitCounter = 0
        if (mode == "Delay") {
            resetDelayState()
        }
        delayedPackets.clear()
    }
    override fun onEnable() {
        hasReceivedVelocity = false
        pausedTicks = 0

        resetDelayState()
        triggeredPhases.clear()
        intaveClickTimes = 0
        moreReduceTimes = 0
        timerState = 0
        boosting = false
        slowing = false
        previousTimerState = 0

        // OtherModeReset
        matrixBoostTimer.reset()
        minemenClubCounter = 0
        legitClick2Times = 0
        attackStartHurtTime = 0
        polarHurtTime = nextInt(7, 10)

        // JumpResetCooldown
        jumpCooldownTickCounter = 0
        jumpCooldownReceivedHitCounter = 0

        // PacketQueueReset
        delayedPackets.clear()
    }
    /**
     * Functions
     */

    /**
     * 通用的JumpReset功能
     */
    private fun extraJumpReset() {
        if (jumpReset && shouldJumpReset(checkUserSprint,true, checkMoving = false, needReceivedS12 = false) &&
            nextInt(endExclusive = 100) <= jumpResetChance && passedJumpCooldown()
        ) {
            if (matrixJumpTest) changeSprint(setState = true, forceChange = true, sendPacketToServer = false)
            mc.thePlayer.jump()
            if (matrixJumpTest) mc.thePlayer.motionY = packetMotionY
            if (hasReceivedVelocity) hasReceivedVelocity = false
            resetJumpCooldownCounter()
        }
    }

    private fun handleLegitClick() {
        val player = mc.thePlayer ?: return

        if (player.hurtTime == 0) {
            attackStartHurtTime = 0
            return
        }

        if (ignoreBlocking && (player.isBlocking || KillAura.blockStatus)) {
            return
        }

        if (attackStartHurtTime == 0 && player.hurtTime > 0) {
            attackStartHurtTime = player.hurtTime
        }

        val currentHurtTimeOffset = attackStartHurtTime - player.hurtTime
        if (currentHurtTimeOffset >= durationHurtTime) {
            return
        }

        // Check if we're still in cooldown from previous attack
        if (clickDelayTick > 0) {
            return
        }

        var entity = mc.objectMouseOver?.entityHit

        if (entity == null) {
            if (whenFacingEnemyOnly) {
                var result: Entity? = null

                runWithModifiedRaycastResult(
                    currentRotation ?: player.rotation,
                    clickRange.toDouble(),
                    0.0
                ) {
                    result = it.entityHit?.takeIf { it -> isSelected(it, true) }
                }

                entity = result
            } else {
                entity = getNearestEntityInRange(clickRange)?.takeIf { isSelected(it, true) }
            }
        }

        entity ?: return

        val hits = clicks.random()
        val keepSprint = modifyMotionWhenClick && makeVanillaAttackNotStopSprint

        // 使用新的attackChance参数传递点击概率
        runAttack(keepSprint, clickRange, hits, entity, ignoreBlocking = true, fakeSwing = true, debugMessage = debugMessage, swingMode = swingMode, attackChance = clickChancePerClick)

        // Set cooldown after attack
        if (clickDelayTicks > 0) {
            clickDelayTick = clickDelayTicks
        }

        if (modifyMotionWhenClick) reduceXZ(modifyMotionFactor.toDouble())
    }

    /**
     * 更新JumpReset冷却计时器
     */
    private fun updateJumpResetCooldown() {
        if (jumpCooldownTickCounter > 0) jumpCooldownTickCounter--
        if ("ReceivedHit" in jumpCooldownMode &&
            ((mode == "JumpReset") || (mode != "Intave" && jumpReset)) &&
            jumpCooldownReceivedHitCounter < jumpCooldownReceivedHit &&
            mc.thePlayer.hurtTime > 0
        ) {
            jumpCooldownReceivedHitCounter++
        }
    }

    private fun shouldJumpReset(checkSprint: Boolean, checkOnGround: Boolean? = true, checkMoving: Boolean? = true,needReceivedS12: Boolean? = true,needForward: Boolean? = true,polarMode: Boolean? = false): Boolean {
        val jumpHurtTime = if (polarMode?: false) nextInt(7,10) else 9
        return mc.thePlayer.hurtTime == jumpHurtTime &&
                (!(needReceivedS12?: true) || hasReceivedVelocity) &&
                (!checkSprint || mc.thePlayer.isSprinting) &&
                (!(checkOnGround ?: true) || mc.thePlayer.onGround) &&
                (!(checkMoving ?: true) || mc.thePlayer.isMoving) &&
                (!(needForward ?: true) || mc.thePlayer.moveForward > 0.707f) &&
                !mc.gameSettings.keyBindJump.isKeyDown
    }

    private fun passedJumpCooldown(): Boolean {
        var tickPassed = true
        var receivedHitPassed = true

        if ("Tick" in jumpCooldownMode) {
            tickPassed = jumpCooldownTickCounter == 0
        }
        if ("ReceivedHit" in jumpCooldownMode) {
            receivedHitPassed = jumpCooldownReceivedHitCounter >= jumpCooldownReceivedHit
        }

        return tickPassed && receivedHitPassed
    }
    private fun resetJumpCooldownCounter() {
        if ("Tick" in jumpCooldownMode) {
            jumpCooldownTickCounter = jumpCooldownTick
        }
        if ("ReceivedHit" in jumpCooldownMode) {
            jumpCooldownReceivedHitCounter = 0
        }
    }

    private fun getNearestEntityInRange(range: Float = 3.0f): Entity? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld.loadedEntityList.filter {
            isSelected(it, true) && player.getDistanceToEntityBox(it) <= range
        }.minByOrNull { player.getDistanceToEntityBox(it) }
    }


    private fun applyDelayedVelocity() {
        var shouldPerformSpecialJumpReset = false

        delayedPackets.entries.removeAll { (packet, _) ->
            if (packet is S12PacketEntityVelocity) {
                applyVelocityReduction(packet)
                shouldPerformSpecialJumpReset = true
                true
            } else {
                false
            }
        }

        if (shouldPerformSpecialJumpReset &&
            mc.thePlayer.onGround &&
            mc.thePlayer.isSprinting &&
            jumpReset &&
            shouldJumpReset(checkUserSprint, true, checkMoving = false, needReceivedS12 = false) &&
            nextInt(endExclusive = 100) <= jumpResetChance &&
            passedJumpCooldown()) {

            mc.thePlayer.jump()
            if (matrixJumpTest) mc.thePlayer.motionY = packetMotionY

            if (hasReceivedVelocity) hasReceivedVelocity = false
            resetJumpCooldownCounter()
            debugMessage("Special Jump Reset triggered after delayed velocity")
        }
        if (delayAttackReduce) runAttack(attackCount = 5, debugMessage = debugMessage, debugMessageString = "DelayAttackReduced")
    }

    private fun applyVelocityReduction(packet: S12PacketEntityVelocity) {
        val thePlayer = mc.thePlayer ?: return

        var motionX = packet.realMotionX
        var motionZ = packet.realMotionZ
        var motionY = packet.realMotionY

        if (delayHorizontal != 0f) {
            motionX *= delayHorizontal
            motionZ *= delayHorizontal
        }

        if (delayVertical != 0f) {
            motionY *= delayVertical
        }

        thePlayer.motionX = motionX
        thePlayer.motionZ = motionZ
        thePlayer.motionY = motionY
    }

    /**
     * 处理Matrix模式的速度包
     */
    private fun handleMatrixVelocity(packet: S12PacketEntityVelocity, event: PacketEvent) {
        hasReceivedVelocity = true
        event.cancelEvent()

        if (abs(packet.realMotionY) >= 0.1f) {
            mc.thePlayer.motionY = packet.realMotionY

            matrixMotionYReduce = true

            if (!mc.thePlayer.isMoving) {
                val reducedSpeed = max(packet.bpt * 0.1, mc.thePlayer.bpt)
                if (packet.bpt > 0) {
                    mc.thePlayer.motionX = packet.realMotionX / packet.bpt * reducedSpeed
                    mc.thePlayer.motionZ = packet.realMotionZ / packet.bpt * reducedSpeed
                }
            } else if (matrixBoost && matrixBoostTimer.hasTimePassed(matrixBoostDelay)) {
                reduceXZ(matrixBoostFactor.toDouble() + 1)
                matrixBoostTimer.reset()
            }
        }
    }

    /**
     * 处理Delay模式的速度包
     */
    private fun handleDelayVelocity(packet: S12PacketEntityVelocity, event: PacketEvent) {
        when {
            !delayReverseFlag && !canDelay() && !isInLiquidOrWeb() &&
                    !delayPendingExplosion && (!delayAllowNext || !delayFakeCheck) -> {

                delayChanceCounter = delayChanceCounter % 100 + delayChance
                if (delayChanceCounter >= 100) {
                    delayedPackets[packet] = System.currentTimeMillis()
                    event.cancelEvent()
                    delayReverseFlag = true
                    delayActive = true
                    delayTimer.reset()
                    return
                }
            }
        }

        applyVelocityReduction(packet)
        event.cancelEvent()
    }

    /**
     * 处理Delay模式的爆炸包
     */
    private fun handleExplosionDelay(packet: S27PacketExplosion, event: PacketEvent) {
        delayPendingExplosion = true
        if (delayHorizontal == 0f || delayVertical == 0f) {
            event.cancelEvent()
        } else {
            packet.field_149152_f *= delayHorizontal
            packet.field_149153_g *= delayVertical
            packet.field_149159_h *= delayHorizontal
        }
    }

    private fun resetDelayState() {
        delayChanceCounter = 0
        delayActive = false
        delayReverseFlag = false
        delayPendingExplosion = false
        delayAllowNext = true
        delayTickCounter = 0
        delayTimer.reset()
    }

    private fun canDelay(): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return thePlayer.onGround && (!KillAura.state || !KillAura.blockStatus)
    }

    private fun isInLiquidOrWeb(): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb
    }
    private fun isValidS12Packet(packet: S12PacketEntityVelocity): Boolean {
        return (packet.realMotionX != 0.0 &&
                packet.realMotionY != 0.0 &&
                packet.realMotionZ != 0.0 &&
                packet.entityID == mc.thePlayer.entityId
                )
    }
    /**
     * 检查是否允许在当前状态下工作
     */
    private fun canWorkNow(): Boolean {
        val player = mc.thePlayer ?: return false
        val onGround = player.onGround
        val inAir = !onGround

        return when {
            "OnlySprinting" in allowWorkWhen && !mc.thePlayer.isSprinting -> false
            pausedTicks > 0 -> false
            "OnGround" in allowWorkWhen && "InAir" in allowWorkWhen -> true
            "OnGround" in allowWorkWhen -> onGround
            "InAir" in allowWorkWhen -> inAir
            else -> true
        }
    }
    @Suppress("unused")
    private fun skipTicks(count: Int) {
        TickSkipManager.skipTicks(count)
    }

    private fun displayJumpResetChoices(): Boolean {
        return mode == "JumpReset" || (mode !in arrayOf("Intave", "IntaveSafe", "PolarJump", "AirJumpReset","Prediction") && jumpReset)
    }
    fun soulSandCheck(): Boolean {
        val par1AxisAlignedBB = Minecraft.getMinecraft().thePlayer.entityBoundingBox.contract(
            0.001, 0.001,
            0.001
        )
        val var4 = MathHelper.floor_double(par1AxisAlignedBB.minX)
        val var5 = MathHelper.floor_double(par1AxisAlignedBB.maxX + 1.0)
        val var6 = MathHelper.floor_double(par1AxisAlignedBB.minY)
        val var7 = MathHelper.floor_double(par1AxisAlignedBB.maxY + 1.0)
        val var8 = MathHelper.floor_double(par1AxisAlignedBB.minZ)
        val var9 = MathHelper.floor_double(par1AxisAlignedBB.maxZ + 1.0)
        for (var11 in var4 until var5) {
            for (var12 in var6 until var7) {
                for (var13 in var8 until var9) {
                    val pos = BlockPos(var11, var12, var13)
                    val var14 = Minecraft.getMinecraft().theWorld.getBlockState(pos).block
                    if (var14 is BlockSoulSand) {
                        return true
                    }
                }
            }
        }
        return false
    }
    internal fun debugMessage(message: Any? = null) {
        if (!debugMessage) return
        if (message == null) return
        chat(message.toString())
    }
    private fun packetMotionValid(): Boolean {
        return packetMotionX != 0.0 &&
                packetMotionZ != 0.0 &&
                packetMotionY != 0.0
    }
    @Suppress("SameParameterValue")
    private fun playerNowMotionOutPut(x: Boolean, y: Boolean, z: Boolean): String {
        val motions = mutableListOf<String>()

        val mx = roundToPlacesIfNeeded(mc.thePlayer.motionX)
        val my = roundToPlacesIfNeeded(mc.thePlayer.motionY)
        val mz = roundToPlacesIfNeeded(mc.thePlayer.motionZ)
        if (x) motions.add(mx.toString())
        if (y) motions.add(my.toString())
        if (z) motions.add(mz.toString())

        return motions.joinToString(" ")
    }
    fun knockBackIsNegated(xMotion: Double, zMotion: Double): Boolean {
        val motionX = mc.thePlayer.motionX
        val motionZ = mc.thePlayer.motionZ

        val isXNegated = sign(motionX) != sign(xMotion)
        val isZNegated = sign(motionZ) != sign(zMotion)

        return isXNegated && isZNegated
    }
    internal fun stopIntaveBlink() {
        if (shouldBlink && BlinkUtils.isBlinking) {
            runAttack(silentAttack = true, fakeSwing = true)
            BlinkUtils.unblink()
            shouldBlink = false
            lastBlinkState = false
            debugMessage("Unblink | ${mc.thePlayer.hurtTime}")
        }
    }
    private fun getDistance(target: EntityLivingBase): Double {
        return mc.thePlayer.getDistanceToEntityBox(target)
    }
    private fun intaveReduce(phase: Int,safe: Boolean) {
        fun intaveReduceTrigger(phase: Int) {
            when (phase) {
                1-> triggeredPhases.add(IntavePhase.PHASE_1)
                2-> triggeredPhases.add(IntavePhase.PHASE_2)
                3-> triggeredPhases.add(IntavePhase.PHASE_3)
                4-> triggeredPhases.add(IntavePhase.PHASE_4)
                5-> triggeredPhases.add(IntavePhase.PHASE_5)
                6-> triggeredPhases.add(IntavePhase.PHASE_6)
            }
        }
        if (safe && globalTarget !is EntityPlayer) return
        stopIntaveBlink()
        if (knockBackIsNegated(packetMotionX,packetMotionZ) && onlyWhenNeed) {
            if (intaveReversed && intaveReduceTimes in 2..4) {
                canCancelHitSlow = false
                reduceXZ(0.6)
                return
            }
            if (
                (!KeepSprint.shouldKeepSprint) &&
                (!KillAura.shouldKeepSprint)
            ) {
                canCancelHitSlow = true
                return
            }
            return
        } else canCancelHitSlow = false
        if (!intaveReversed && intaveReduceTimes == 1 && mc.thePlayer.hurtTime in 1..9) {
            if (knockBackIsNegated(packetMotionX,packetMotionZ)) return
            if (mc.thePlayer.bps > 2.805) {
                if (calculateAngleDifference() > 15.0) mc.thePlayer.setBPSTo(-min(5.612, mc.thePlayer.bps * 0.6))
                intaveReversed = true
                debugMessage("IntaveReverse | ${mc.thePlayer.hurtTime}")
            }
        } else if (mc.thePlayer.hurtTime != 10) intaveReduceTimes++ else intaveReduceTimes == 1
        if (intaveReduceTimes > 5 && intaveReversed) return
        when (phase) {
            0 -> {
                runAttack(silentAttack = true)
                debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
            }
            1 -> {
                when {
                    !getTriggeredPhase(1) -> {
                        if (extraC0APerReduce) repeat(extraPacketCount) { sendPacket(C0APacketAnimation()) }
                        reduceXZ(0.6)

                        debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
                        intaveReduceTrigger(1)
                        return
                    }
                    !getTriggeredPhase(4) -> {
                        if (extraC0APerReduce) repeat(extraPacketCount) { sendPacket(C0APacketAnimation()) }
                        if (calculateAngleDifference() < 15.0)
                            reduceXZ(1.5)
                        else reduceXZ(0.6)
                        debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
                        intaveReduceTrigger(4)
                        return
                    }
                }
                if (moreReduceTimes < maxMoreReduce && moreReduce) {
                    moreReduceTimes++
                    reduceXZ(getMoreReduceFactor(moreReduceTimes))
                }
            }
            2 -> {
                when {
                    !getTriggeredPhase(2) -> {
                        if (extraC0APerReduce) repeat(extraPacketCount) { sendPacket(C0APacketAnimation()) }
                        reduceXZ(0.36)
                        debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
                        intaveReduceTrigger(2)
                        return
                    }
                    !getTriggeredPhase(5) -> {
                        if (extraC0APerReduce) repeat(extraPacketCount) { sendPacket(C0APacketAnimation()) }
                        if (calculateAngleDifference() < 15.0)
                            reduceXZ(1.5)
                        else reduceXZ(0.6)
                        debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
                        intaveReduceTrigger(5)
                        return
                    }
                }
                if (moreReduceTimes < maxMoreReduce && moreReduce) {
                    moreReduceTimes++
                    reduceXZ(getMoreReduceFactor(moreReduceTimes))
                }
            }
            3 -> {
                when {
                    !getTriggeredPhase(3) -> {
                        if (extraC0APerReduce) repeat(extraPacketCount) { sendPacket(C0APacketAnimation()) }
                        reduceXZ(0.216)
                        debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
                        intaveReduceTrigger(3)
                        return
                    }
                    !getTriggeredPhase(6) -> {
                        if (extraC0APerReduce) repeat(extraPacketCount) { sendPacket(C0APacketAnimation()) }
                        if (calculateAngleDifference() < 15.0)
                            reduceXZ(1.5)
                        else reduceXZ(0.6)
                        debugMessage("IntaveReduce | ${mc.thePlayer.hurtTime}")
                        intaveReduceTrigger(6)
                        return
                    }
                }
                if (moreReduceTimes < maxMoreReduce && moreReduce) {
                    moreReduceTimes++
                    reduceXZ(getMoreReduceFactor(moreReduceTimes))
                }
            }
        }

    }
    private fun getMoreReduceFactor(reduceCount: Int): Double {
        return when (reduceCount) {
            1 -> 0.5/0.6
            2 -> 0.75
            else -> {
                val baseFactor = 0.7
                val reduction = (reduceCount - 3) * 0.05
                max(0.0, baseFactor - reduction)
            }
        }
    }
    private fun getTriggeredPhase(phase: Int): Boolean {
        return when (phase) {
            1 -> triggeredPhases.contains(IntavePhase.PHASE_1)
            2 -> triggeredPhases.contains(IntavePhase.PHASE_2)
            3 -> triggeredPhases.contains(IntavePhase.PHASE_3)
            4 -> triggeredPhases.contains(IntavePhase.PHASE_4)
            5 -> triggeredPhases.contains(IntavePhase.PHASE_5)
            6 -> triggeredPhases.contains(IntavePhase.PHASE_6)
            else -> false
        }
    }

}