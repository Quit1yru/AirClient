package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity.clicks
import net.ccbluex.liquidbounce.features.module.modules.exploit.Disabler
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeSprint
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeTimer
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isInBadEnvironment
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.keepingSprint
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceY
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.isOnGround
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.BlockAir
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SNEAKING
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing.DOWN
import net.minecraft.util.Vec3
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

private fun Float.normalizeAngle(): Float {
    return ((this % 360) + 360) % 360
}

object Velocity : Module("Velocity", Category.COMBAT) {

    private val tagMode by choices("TagMode",arrayOf("Normal","Custom","None"),"Normal")
    private val CustomTag by text("CustomTag", "") {tagMode == "Custom"}
    /**
     * OPTIONS
     */
    var mode by choices(
        "Mode", arrayOf(
            "Custom",
            "Simple", "Cancel", "AAC", "AACPush", "AACZero", "AACv4", "AAC5",
            "AttackReduce",
            "Reverse", "SmoothReverse", "Jump", "Glitch", "Legit",
            "GhostBlock", "Vulcan", "S32Packet",
            "MatrixReduce","MatrixReduce2","MatrixReduce3",
            "LiquidBounceDelay", "GrimC03", "Hypixel", "HypixelAir", "HypixelMoving", "Delay","BufferAbuse",
            "LegitClick", "GrimVertical", "OldGrim",
            "PolarJump", "OldPolar", "BuzzReverse",
            "Intave14","Intave13.0.6", "Intave14.3.3", "Intave14.1.2", "IntaveReduce", "IntaveTimer","IntaveFlag", "IntaveStrong",
            "Karhu",
            "Kazer","UniversoCraftOld","BlocksMC", "Hylex", "Dexland"
        ), "Simple"
    )

    private val horizontal by float("Horizontal", 0F, -1F..1F) { mode in arrayOf("Simple", "AAC", "Legit") }
    private val vertical by float("Vertical", 0F, -1F..1F) { mode in arrayOf("Simple", "Legit") }


    // Custom
    private val SprintControl by choices("SprintControl",arrayOf("KeepSprint", "StopSprint", "NoControl"),"NoControl") {mode in arrayOf("Custom")}
    private val TryForward by boolean("TryForward",false) {mode in arrayOf("Custom")}
    private val TryingForwardTime by float("TryForwardTime",50f,0f..1000f,"ms") {mode in arrayOf("Custom") && TryForward}
    private val DisableStrafeInput by boolean("DisableStrafeInput",false) {mode in arrayOf("Custom")}
    private val DisableStrafeInputTime by float("DisableStrafeInputTime",50f,0f..1000f,"ms") {mode in arrayOf("Custom") && DisableStrafeInput}
    private val CustomAttackReduce by boolean("Custom-AttackReduce", false) { mode in arrayOf("Custom") }
    private val AttackReduceOnlyWhenBackward by boolean("AttackReduce-OnlyWhenBackward", false) { mode in arrayOf("Custom") && CustomAttackReduce }
    private val AttackHelper by boolean("Attack/TriggerHelper",false) {mode in arrayOf("Custom") && CustomAttackReduce}
    private val ActiveRange by float("AttackHelp-ActiveRange",3.0F,0f..6f) {mode in arrayOf("Custom") && CustomAttackReduce && AttackHelper}
    private val AllowHurtTime by intRange("AllowAttackHurtTime",0..10,0..10) {mode in arrayOf("Custom") && CustomAttackReduce && AttackHelper}
    private val CheckRotation by boolean("CheckRotation",true) {mode in arrayOf("Custom") && CustomAttackReduce && AttackHelper}
    private val attackreducefactor by float("AttackReduceFactor", 0.6F, -1F..1F) { mode in arrayOf("Custom") && CustomAttackReduce }
    private val attackreduceYfactor by float("AttackReduceYFactor", 1.0F, -1F..1F) { mode in arrayOf("Custom") && CustomAttackReduce }
    private val attackreducemaxhurttime by int("AttackReduceMaxHurtTime", 9, 1..10) { mode in arrayOf("Custom") && CustomAttackReduce }
    private val attackreduceminhurttime by int("AttackReduceMinHurtTime", 9, 1..10) { mode in arrayOf("Custom") && CustomAttackReduce }
    private val DoubleReduceWhenFirstReduce by boolean("ExtraReduceWhenFirstReduce",false) {mode in arrayOf("Custom") && CustomAttackReduce}
    private val DoubleReduceFactor by float("ExtraReduceFactor", 0.6F, -2F..2F) {mode in arrayOf("Custom") && CustomAttackReduce && DoubleReduceWhenFirstReduce}
    private val SpecialReduce by boolean("ApplySpecialModifyWhenSpecificHurtTime",false) {mode in arrayOf("Custom") && CustomAttackReduce}
    private val SpecialMultiReduce by boolean("SpecialReduce-MultiReduce",false) {mode in arrayOf("Custom") && CustomAttackReduce && SpecialReduce}
    private val SpecialReduceFactor by float("SpecialReduceFactor",0.6F,-2F..2F) {mode in arrayOf("Custom") && CustomAttackReduce && SpecialReduce}
    private val SpecialReduceHurtTime by int("SpecialReduceHurtTime",10,1..10) {mode in arrayOf("Custom") && CustomAttackReduce && SpecialReduce}
    private val MultiReduce by boolean("AttackReduce-MultiReduce", false) { mode in arrayOf("Custom") && CustomAttackReduce }
    private val MaxTriggerTimes by int("MultiReduce-MaxTriggerTimes",3,1..10) { mode in arrayOf("Custom") && CustomAttackReduce && MultiReduce }
    private val ProgressiveFactor by boolean("MultiReduce-ProgressiveFactor", false) { mode in arrayOf("Custom") && CustomAttackReduce && MultiReduce }
    private val ProgressiveMode by choices("MultiReduce-ProgressiveMode",arrayOf("Increases","Decrease"),"Decrease") { mode in arrayOf("Custom") && CustomAttackReduce && MultiReduce }
    private val ProgressiveStep by float("MultiReduce-ProgressiveStep", 0.05F, 0F..5F) { mode in arrayOf("Custom") && CustomAttackReduce && MultiReduce && ProgressiveFactor }
    private val MaxProgressiveFactor by float("MaxProgressiveFactor",1F,-5f..5f) { mode in arrayOf("Custom") && CustomAttackReduce && MultiReduce && ProgressiveFactor }
    private val MinProgressiveFactor by float("MinProgressiveFactor",0F,-5F..5F) { mode in arrayOf("Custom") && CustomAttackReduce && MultiReduce && ProgressiveFactor }
    private val randomizeXZ by boolean("AttackReduce-RandomizeXZ", false) { mode == "Custom" && CustomAttackReduce }
    private val minXZReduce by float("RandomFactor-MinXZReduce", 0.5f, 0f..1f) { mode == "Custom" && randomizeXZ && CustomAttackReduce }
    private val maxXZReduce by float("RandomFactor-MaxXZReduce", 0.8f, 0f..1f) { mode == "Custom" && randomizeXZ && CustomAttackReduce }
    private val randomizeY by boolean("AttackReduce-RandomizeY", false) { mode == "Custom" && CustomAttackReduce }
    private val minYReduce by float("RandomFactor-MinYReduce", 0.9f, 0f..1f) { mode == "Custom" && randomizeY && CustomAttackReduce }
    private val maxYReduce by float("RandomFactor-MaxYReduce", 1.1f, 0f..2f) { mode == "Custom" && randomizeY && CustomAttackReduce }
    private val CustomJumpReset by boolean("Custom-JumpReset", false) { mode in arrayOf("Custom") }
    private val CustomJumpResetSafe by boolean("CustomJumpResetCheckEnvironment", true) { mode in arrayOf("Custom") && CustomJumpReset }
    private val CustomChance by float("CustomJumpResetChance", 100F, 0F..100F) { mode in arrayOf("Custom") && CustomJumpReset }
    private val JumpResetMaxHurtTime by int("JumpResetMaxHurtTime", 9, 1..10) { mode in arrayOf("Custom") && CustomJumpReset }
    private val JumpResetMinHurtTime by int("JumpResetMinHurtTime", 9, 1..10) { mode in arrayOf("Custom") && CustomJumpReset }
    private val AfterJumpSprintControl by choices("AfterJumpSprintControl", arrayOf("None", "Sprint", "Stop"), "None") { mode in arrayOf("Custom") && CustomJumpReset }
    private val JumpResetOnlyOnSwing by boolean("JumpResetOnlyOnSwing",false) {mode == "Custom" && CustomJumpReset}
    private val CustomTimer by boolean("Custom-Timer",false) {mode in arrayOf("Custom")}
    private val CustomTimerLowTimer by float("CustomTimer-LowTimer",0.789f,0.0001f..10f) {mode in arrayOf("Custom") && CustomTimer}
    private val CustomTimerMaxTimer by float("CustomTimer-MaxTimer",1.321f,0.0001f..200f) {mode in arrayOf("Custom") && CustomTimer}
    private val CustomTimerTimeMode by choices("CustomTimer-TimeMode",arrayOf("HurtTime","MSTimer"),"HurtTime") {mode in arrayOf("Custom") && CustomTimer}
    private val CustomTimerLowMinHurtTime by int("CustomTimer-LowTimerMinHurtTime",9,0..10) {mode in arrayOf("Custom") && CustomTimer && CustomTimerTimeMode == "HurtTime"}
    private val CustomTimerMinWorkHurtTime by int("CustomTimer-MinWorkHurtTime",8,0..10) {mode in arrayOf("Custom") && CustomTimer && CustomTimerTimeMode == "HurtTime"}
    private val CustomTimerLowMSTimer by float("CustomTimer-LowTimerMSTimerTime",50f,0f..1000f,"ms") {mode in arrayOf("Custom") && CustomTimer && CustomTimerTimeMode == "MSTimer"}
    private val CustomTimerMinWorkMSTimer by float("CustomTimer-MinWorkMSTimerTime",100f,0f..1000f,"ms") {mode in arrayOf("Custom") && CustomTimer && CustomTimerTimeMode == "MSTimer"}
    private val CustomTimerOnlyWhenReceivedVelocity by boolean("CustomTimer-OnlyWhenReceivedVelocity",true) {mode in arrayOf("Custom") && CustomTimer}
    private val CustomTimerC03 by boolean("Custom-CancelC03",false) {mode in arrayOf("Custom") && CustomTimer}

    private val Debugger by boolean("Debugger",false) { mode in arrayOf("Custom")}

    // AttackReduce
    private val attackVelocityFactor by float("OnHitFactor",0.6f,-1.0f..1.0f) {mode == "AttackReduce"}
    private val attackVelocitySprintFactor by float("SprintOnHitFactor",0.6f,-1.0f..1.0f) {mode == "AttackReduce"}
    private val attackVelocityHurtTime by intRange("ReduceHurtTime", 9..9, 1..10) {mode == "AttackReduce"}

    // Delay
    private val delayTicks by int("DelayTicks", 3, 1..20) { mode == "Delay" }
    private val delayChance by int("DelayChance", 100, 0..100) { mode == "Delay" }
    private val delayHorizontal by float("DelayHorizontal", 0F, -1F..1F) { mode == "Delay" }
    private val delayVertical by float("DelayVertical", 0F, -1F..1F) { mode == "Delay" }
    private val delayFakeCheck by boolean("DelayFakeCheck", true) { mode == "Delay" }
    private var delayChanceCounter = 0
    private var delayActive = false
    private var delayReverseFlag = false
    private var delayPendingExplosion = false
    private var delayAllowNext = true
    private val delayedPackets = LinkedHashMap<Packet<*>, Long>()
    private val delayTimer = MSTimer()
    private var delayTickCounter = 0
    // BufferAbuse
    private val bufferPacket by int("BufferPacket",3,1..5) {mode == "BufferAbuse"}
    private val bufferHorizontal by float("BufferHorizontal",1.0f,0.0f..1.0f){mode == "BufferAbuse"}
    private val bufferVertical by float("BufferVertical",1.0f,0.0f..1.0f){mode == "BufferAbuse"}
    private val bufferDebugger by boolean("BufferModeDebugger",false){mode == "BufferAbuse"}
    private var bufferAmount = 0

    // MatrixReduce3
    private var matrixReduce3Boost by boolean("BoostAfterReduce",false) {mode == "MatrixReduce3"}
    private var matrixReduce3BoostFactor by float("BoostFactor", 0.33f, 0.0f..5.0f) {mode == "MatrixReduce3" && matrixReduce3Boost}
    private var matrixReduce3BoostDelay by int("BoostCooldown",0,0..2000,"ms") {mode == "MatrixReduce3" && matrixReduce3Boost}

    // Intave14
    private val TriggerTimes by int("MaxTriggerTimes",2,1..3) { mode == "Intave14" }
    private val applyDiffFactorOnGroundOrInAir by boolean("ApplyDiffFactorOnGround/InAir",true) {mode == "Intave14"}
    private val firstReduce by int("FirstReduceHurtTime",9,1..10) { mode == "Intave14" && TriggerTimes >= 1 }.onChange { _, new -> new.coerceAtLeast(secondReduce) }
    private val secondReduce by int("SecondReduceHurtTime",8,1..10) { mode == "Intave14" && TriggerTimes >= 2 }.onChange { _, new -> new.coerceAtLeast(thirdReduce) }
    private val thirdReduce by int("ThirdReduceHurtTime",6,1..10) { mode == "Intave14" && TriggerTimes >= 3 }
    private val intaveTimerTest by boolean("IntaveTimer-Test",false) {mode == "Intave14"}
    private val yReduceTest by boolean("YReduce-Test",false) {mode == "Intave14"}
    private val yReduceCount by float("motionYReduceCount",0.1f,0.001f..1.0f) {mode == "Intave14" && yReduceTest}
    private val yReduceMaxTimes by int("YReduceMaxTimes",1,0..3) {mode == "Intave14" && yReduceTest }.onChange { _, new ->
        new.coerceAtMost(TriggerTimes)
    }
    private val intaveMoreReduce by boolean("Intave14MoreReduce-Test",false) {mode == "Intave14"}
    private val intaveMoreReduceFactor by float("Intave14MoreReduceFactor",0.8f,0.0f..1.0f) {mode == "Intave14" && intaveMoreReduce}
    private val intaveMoreReduceMaxTimes by int("Intave14MoreReduceMaxTimes",9,1..9) {mode == "Intave14" && intaveMoreReduce}
    private val intaveMoreReduceExtraReduce by boolean("MoreReduceWillUseAnotherFactorWhenNormalReduceNotWorked",false) {mode == "Intave14" && intaveMoreReduce}
    private val intaveMoreReduceAnotherFactor by float("AnotherFactor",0.6f,0.0f..1.0f) {mode == "Intave14" && intaveMoreReduce && intaveMoreReduceExtraReduce}
    private val finalReverse by boolean("FinalReverse",false) {mode == "Intave14"}
    private val finalReverseTriggerMode by choices("finalReverseTriggerMode",arrayOf("NeedAttack","AutoTrigger"),"NeedAttack") {mode == "Intave14" && finalReverse}
    private val finalReverseStrict by boolean("StrictReverse",true) {mode == "Intave14" && finalReverse && TriggerTimes >= 2}
    private val finalReverseFactor by float("ReverseFactor",1.0f,0.0f..5.0f) {mode == "Intave14" && finalReverse}
    private val OnlyWhenBackward by boolean("ReduceOnlyWhenBackward",true) {mode == "Intave14"}
    private val intave14Debugger by boolean("Intave14Debugger",false) {mode == "Intave14"}

    // BuzzReverse
    private val needAttack by boolean("NeedAttack",false) {mode in arrayOf("BuzzReverse")}

    // OldGrim
    private val oldGrimLegit by boolean("OldGrim-Legit", false) { mode in arrayOf("OldGrim") }
    private val oldGrimRayCast by boolean("OldGrim-RayCast", false) { mode in arrayOf("OldGrim") }
    private val oldGrimAttackReduce by float("OldGrim-ReduceFactor", 0.07776f, 0f..1f) { mode in arrayOf("OldGrim") }
    private val webValue by boolean("CheckWeb",true) { mode in arrayOf("OldGrim") }
    private val liquidValue by boolean("CheckLiquid",true) { mode in arrayOf("OldGrim") }

    // IntaveFlag
    private var intaFlag = false

    // Cancel
//    private val cancelHorizon by boolean("CancelHorizonVelocity",true) {mode == "Cancel"}
//    private val cancelVertical by boolean("CancelVerticalVelocity",true) {mode == "Cancel"}
    private val cancelVelocity by multiChoices("CancelVelocity", arrayOf("Horizontal", "Vertical")) {mode == "Cancel"}
    private val cancelVerticalOnlyInAir by boolean("CancelVerticalVelocityOnlyInAir", false) { mode == "Cancel" && "Vertical" in cancelVelocity }

    // PolarJump
    private val forceChangehurtTime by boolean("ForceChangeJumpHurtTime",true) { mode in arrayOf("PolarJump") }
    private val forceChangehurtTimeCount by int("ForceChangehurtTime-HurtCount",5,1..10) { mode in arrayOf("PolarJump") && forceChangehurtTime }
    private val polarJumpDebugger by boolean("Debugger",false) { mode in arrayOf("PolarJump") }

    // Reverse
    private val reverseStrength by float("ReverseStrength", 1F, 0.1F..1F) { mode == "Reverse" }
    private val reverse2Strength by float("SmoothReverseStrength", 0.05F, 0.02F..0.1F) { mode == "SmoothReverse" }

    private val onLook by boolean("onLook", false) { mode in arrayOf("Reverse", "SmoothReverse") }
    private val range by float("Range", 3.0F, 1F..5.0F) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }
    private val maxAngleDifference by float("MaxAngleDifference", 45.0f, 5.0f..90f) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }

    // AAC Push
    private val aacPushXZReducer by float("AACPushXZReducer", 2F, 1F..3F) { mode == "AACPush" }
    private val aacPushYReducer by boolean("AACPushYReducer", true) { mode == "AACPush" }

    // AAC v4
    private val aacv4MotionReducer by float("AACv4MotionReducer", 0.62F, 0F..1F) { mode == "AACv4" }

    // Legit
    private val legitDisableInAir by boolean("DisableInAir", true) { mode == "Legit" }

    // Chance
    private val chance by int("Chance", 100, 0..100) { mode == "Jump" || mode == "Legit" }

    // Jump
    private val jumpCooldownMode by choices("JumpCooldownMode", arrayOf("Ticks", "ReceivedHits"), "Ticks")
    { (mode == "Jump") }
    private val ticksUntilJump by int("TicksUntilJump", 4, 0..20)
    { jumpCooldownMode == "Ticks" && (mode == "Jump") }
    private val hitsUntilJump by int("ReceivedHitsUntilJump", 2, 0..5)
    { jumpCooldownMode == "ReceivedHits" && (mode == "Jump") }
    private val JumpResetOnlyOnSwingForJumpVlc by boolean("JumpResetOnlyOnSwing",false) {mode == "Jump"}

    // Ghost Block
    private val hurtTimeRange by intRange("HurtTime", 1..9, 1..10) {
        mode == "GhostBlock"
    }

    // Delay
    private val spoofDelay by int("SpoofDelay", 500, 0..5000) { mode == "LiquidBounceDelay" }
    var delayMode = false

    // IntaveReduce
    private val reduceFactor by float("IntaveReduceFactor", 0.6f, 0.6f..1f) { mode == "IntaveReduce" }
    private val hurtTime by intRange("IntaveReduceHurtTime", 9..10, 1..10) { mode == "IntaveReduce" }

    // Global Var
    var globalJumpCheckTarget: Entity? = null

    // TODO: Could this be useful in other modes? (Jump?)
    // Limits
    private val limitMaxMotionValue = boolean("LimitMaxMotion", false) { mode == "Simple" }
    private val maxXZMotion by float("MaxXZMotion", 0.4f, 0f..1.9f) { limitMaxMotionValue.isActive() }
    private val maxYMotion by float("MaxYMotion", 0.36f, 0f..0.46f) { limitMaxMotionValue.isActive() }
    //0.00075 is added silently

    // Vanilla XZ limits
    // Non-KB: 0.4 (no sprint), 0.9 (sprint)
    // KB 1: 0.9 (no sprint), 1.4 (sprint)
    // KB 2: 1.4 (no sprint), 1.9 (sprint)
    // Vanilla Y limits
    // 0.36075 (no sprint), 0.46075 (sprint)

    private val clicks by intRange("Clicks", 1..2, 1..20) { mode == "LegitClick" }
    private val durationHurtTime by int("DurationHurtTimes",1,1..9) {mode == "LegitClick"}
    private val whenFacingEnemyOnly by boolean("WhenFacingEnemyOnly", true) { mode == "LegitClick" }
    private val ignoreBlocking by boolean("IgnoreBlocking", false) { mode == "LegitClick" }
    private val clickRange by float("ClickRange", 3f, 1f..6f) { mode == "LegitClick" }
    private val swingMode by choices("SwingMode", arrayOf("Off", "Normal", "Packet"), "Normal") { mode == "LegitClick" }

    // Dexland
    private val hReduce by float("HReduce", 0.3f, 0f..1f) {mode == "Dexland"}
    private val times by int("AttacksToWork", 4, 1..10) {mode == "Dexland"}

    // Grim
    private val grimVerticalMode by choices("GrimVerticalMode", arrayOf("Reduce", "1.17", "Vertical"), "Reduce") { mode == "GrimVertical" }
    private val smartVelo by boolean("SmartVelo", true) { mode == "GrimVertical" && grimVerticalMode == "Vertical" }
    private val sendC0FValue by boolean("C0F", false) { mode == "GrimVertical" && grimVerticalMode == "Vertical" }
    private val c0fPacketAmount by int("C0FPacketAmount", 0, 1..40) { mode == "GrimVertical" && grimVerticalMode == "Vertical" && sendC0FValue }
    private val callEvent by boolean("CallEvent", true) { mode == "GrimVertical" && grimVerticalMode == "Vertical" }
    private val via by boolean("Via", true) { mode == "GrimVertical" && (grimVerticalMode == "Vertical" || grimVerticalMode == "Reduce") }


    // Global
    private var tagIncludeGlobalMode by boolean("TagIncludeGlobalMode",false) {globalJumpReset || globalSprintReset && mode != "Custom"}
    private val globalSprintReset by boolean("GlobalSprintReset",false)
    private val globalSprintResetOnlyGround by boolean("SprintResetOnlyWhenGround",true) { globalSprintReset }
    val globalJumpReset by boolean("GlobalJumpReset",false) {mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump")}
    private val jumpResetChance by int("JumpChance",100,0..100,"%") {mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") && globalJumpReset}
    private val rangeLimit by boolean("RangeLimit", false) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump")}
    private val maxDistanceToTarget by float("maxDistanceToTarget",3.0f,0.0f..6.0f){globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") && rangeLimit}
    private val globalCheckBadEnvironment by boolean("CheckBadEnvironment",true) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") }
    private val fastFallAfterJump by boolean("FastFallAfterJump",false) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") }
    private val fastFallSpeed by float("FastFallSpeed",1.0f,1.0f..10.0f) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") && fastFallAfterJump}
    private val forceSprintBeforeJump by boolean("ForceSprintBeforeJump",false) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump")}
    private val globalCheckSprinting by boolean("CheckSprinting",true) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump")}
    private val globalIgnoreSprintingWhenBlocking by boolean("JumpResetIgnoreSprintingCheckWhenBlocking",false) {globalJumpReset && mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") && globalCheckSprinting}
    private val globalActionDebugger by boolean("globalActionDebugger",false) {globalSprintReset || globalJumpReset}
    private val pauseOnExplosion by boolean("PauseOnExplosion", true)
    private val ticksToPause by int("TicksToPause", 20, 1..50) { pauseOnExplosion }
    private val cancelSpecialVelocity by boolean("CancelSpecialVelocity",false) { mode != "MatrixReduce3" }
    private val cancelSpecialVelocityKeepMotionY by boolean("KeepMotionYVelocity",true) {mode == "MatrixReduce3" || cancelSpecialVelocity}

    /**
     * VALUES
     */
    private val velocityTimer = MSTimer()
    private var hasReceivedVelocity = false


    // SmoothReverse
    private var reverseHurt = false

    // AACPush
    private var jump = false

    // Jump
    private var limitUntilJump = 0

    // IntaveReduce
    private var intaveTick = 0
    private var lastAttackTime = 0L
    private var intaveDamageTick = 0

    // Delay
    private val packets = LinkedHashMap<Packet<*>, Long>()

    // Grim
    private var timerTicks = 0

    // Southside OldGrim
    private var oldGrimAttacked = false
    private var oldGrimVelocity = false
    private var oldGrimVelocityPacket: S12PacketEntityVelocity? = null

    // Vulcan
    private var transaction = false

    // Hypixel
    private var absorbedVelocity = false

    // Pause On Explosion
    private var pauseTicks = 0

    // Dexland
    var count = 0

    // GrimVertical Variables
    private var attack = false
    private var motionXZ = 0.0
    private var velocityInput = false
    private var canCancel = false
    private var canSpoof = false

    // MatrixReduce3
    private var matrixReduce3BoostTimer = MSTimer()

    // LegitClick
    private var attackStartHurtTime = 0

    // PolarJump
    private var polarhurtTime = 0
    private var polarhurtCount = 0

    // Intave14
    private var notTriggered1 = false
    private var notTriggered2 = false
    private var notTriggered3 = false
    private var notTriggeredA = false
    private var onGroundTri = false
    private var reduceCondition = "OnGround"
    private var intave14ReduceFactorText = "0.0"
    private var wTapTimer = MSTimer()
    private var yReduceTriggeredTimes = 0
    private var finalReverseHurtTime = 0
    private var finalReverseCondition = 0
    private var intaveLowTimering = false
    private var intaveHighTimering = false
    private var intaveMoreReduceTimes = 0
    private var finalReverseTriggered = false

    // Custom
    private var triggerTimes = 0
    private var progressiveXZFactor = 0F
    private var progressivestepfactor = ProgressiveStep
    private var progressivemode = ProgressiveMode
    private var DoubleReduce = false
    private var triggerTimesSpecial = false
    private var hasReceivedVelocity2 = false
    private var hasReceivedVelocity3 = false
    private var TimerChangeTime = MSTimer()
    private var StrafeControlTime = MSTimer()
    private var ForwardTime = MSTimer()
    private var DisablingStrafeInput = false
    private var TryingForward = false

    // GlobalSprintReset
    private var sprintResetReceivedKB = false
    private var sprintResetIsTrackingMaxMotionY = false
    private var sprintResetMotionYLimit = 0.0
    private var sprintResetCurrentMotionY = 0.0
    private var sprintResetLastMotionY = 0.0

    // GlobalTag
    var globalTag = ""

    override val tag
        get() = when (tagMode) {

            "Normal" -> when (mode) {
                "Simple" -> {
                    val horizontalPercentage = (horizontal * 100).toInt()
                    val verticalPercentage = (vertical * 100).toInt()

                    if (!tagIncludeGlobalMode) {
                        "$horizontalPercentage% $verticalPercentage%"
                    } else "$horizontalPercentage% $verticalPercentage% | $globalTag"
                }
                "Custom" -> {
                    CustomTag
                }
                else -> if (!tagIncludeGlobalMode) {
                    mode
                } else "$mode | $globalTag"
            }
            "Custom" -> CustomTag
            else -> ""
        }

    override fun onDisable() {
        pauseTicks = 0
        mc.thePlayer?.speedInAir = 0.02F
        timerTicks = 0
        reset()
        StrafeControlTime.reset()
        ForwardTime.reset()
        polarhurtTime = nextInt(startInclusive = 7, endExclusive = 10)
        polarhurtCount = 0
        matrixReduce3BoostTimer.reset()
        if (mode == "Delay") {
            resetDelayState()
        }
        delayedPackets.clear()
    }

    override fun onEnable() {
        StrafeControlTime.reset()
        ForwardTime.reset()
        polarhurtTime = nextInt(startInclusive = 7, endExclusive = 10)
        polarhurtCount = 0
        wTapTimer.reset()
        matrixReduce3BoostTimer.reset()
    }

    val onUpdate = handler<UpdateEvent> {
        globalTag = buildGlobalTag()

        val thePlayer = mc.thePlayer ?: return@handler
        finalReverseHurtTime = if (TriggerTimes == 3) (thirdReduce - 1) else if (TriggerTimes == 2) (secondReduce - 1) else (firstReduce - 1)

        if (thePlayer.isInLiquid || thePlayer.isInWeb || thePlayer.isDead)
            return@handler
        sprintResetCurrentMotionY = mc.thePlayer.motionY
        if (pauseTicks > 0 && pauseOnExplosion) {
            pauseTicks--
            return@handler
        }
        globalJumpCheckTarget = RaycastUtils.raycastEntity(maxDistanceToTarget.toDouble()) { isSelected(it, true) }
        if (globalSprintReset) {
            if (mc.thePlayer.hurtTime == 0) {
                sprintResetReceivedKB = false
                sprintResetIsTrackingMaxMotionY = false
                sprintResetMotionYLimit = -1000.0
                return@handler
            }
            if (sprintResetCurrentMotionY - sprintResetLastMotionY > 0.08 && sprintResetReceivedKB && !sprintResetIsTrackingMaxMotionY) {
                sprintResetIsTrackingMaxMotionY = true
                sprintResetMotionYLimit = -1000.0
                if (globalActionDebugger) chat("Calculating MaxMotionY")
            }
            if (sprintResetIsTrackingMaxMotionY && sprintResetReceivedKB && globalSprintReset) {
                if (sprintResetMotionYLimit <= sprintResetCurrentMotionY) {
                    sprintResetMotionYLimit = sprintResetCurrentMotionY
                }
                if (sprintResetMotionYLimit >= sprintResetCurrentMotionY && sprintResetReceivedKB && sprintResetIsTrackingMaxMotionY) {
                    sprintResetReceivedKB = false
                    sprintResetIsTrackingMaxMotionY = false
                    sprintResetMotionYLimit = -1000.0
                    if (mc.thePlayer.hurtTime != 0) {
                        if (!mc.gameSettings.keyBindForward.isKeyDown) return@handler
                        if (!mc.thePlayer.onGround && globalSprintResetOnlyGround) return@handler
                        wTapTimer.reset()
                        sprintReset()
                        if (globalActionDebugger) chat("SprintReset")
                    }
                }
            }
            if (mc.thePlayer.hurtTime == 0 && sprintResetIsTrackingMaxMotionY || sprintResetReceivedKB) {
                sprintResetReceivedKB = false
                sprintResetIsTrackingMaxMotionY = false
            }
            sprintResetLastMotionY = sprintResetCurrentMotionY
        }

        when (mode.lowercase()) {
            "glitch" -> {
                thePlayer.noClip = hasReceivedVelocity

                if (thePlayer.hurtTime == 7)
                    thePlayer.motionY = 0.4

                hasReceivedVelocity = false
            }

            "reverse" -> {
                val nearbyEntity = getNearestEntityInRange()

                if (!hasReceivedVelocity)
                    return@handler

                if (nearbyEntity != null) {
                    if (!thePlayer.onGround) {
                        if (onLook && !isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                            return@handler
                        }

                        speed *= reverseStrength
                    } else if (velocityTimer.hasTimePassed(80))
                        hasReceivedVelocity = false
                }
            }
            "smoothreverse" -> {
                val nearbyEntity = getNearestEntityInRange()

                if (hasReceivedVelocity) {
                    if (nearbyEntity == null) {
                        thePlayer.speedInAir = 0.02F
                        reverseHurt = false
                    } else {
                        if (onLook && !isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                            hasReceivedVelocity = false
                            thePlayer.speedInAir = 0.02F
                            reverseHurt = false
                        } else {
                            if (thePlayer.hurtTime > 0) {
                                reverseHurt = true
                            }

                            if (!thePlayer.onGround) {
                                thePlayer.speedInAir = if (reverseHurt) reverse2Strength else 0.02F
                            } else if (velocityTimer.hasTimePassed(80)) {
                                hasReceivedVelocity = false
                                thePlayer.speedInAir = 0.02F
                                reverseHurt = false
                            }
                        }
                    }
                }
            }

            "aac" -> if (hasReceivedVelocity && velocityTimer.hasTimePassed(80)) {
                reduceXZ(horizontal.toDouble())
                //mc.thePlayer.motionY *= vertical ?
                hasReceivedVelocity = false
            }

            "aacv4" ->
                if (thePlayer.hurtTime > 0 && !thePlayer.onGround) {
                    val reduce = aacv4MotionReducer
                    reduceXZ(reduce.toDouble())
                }
            "delay" -> {
                // 检查延迟结束条件
                if (delayReverseFlag && (
                            canDelay() ||
                                    isInLiquidOrWeb() ||
                                    delayTickCounter >= delayTicks
                            )) {
                    // 应用延迟的速度包
                    applyDelayedVelocity()
                    delayReverseFlag = false
                    delayTickCounter = 0
                    delayTimer.reset()
                }

                // 更新延迟计时器
                if (delayReverseFlag) {
                    delayTickCounter++
                }

                // 处理延迟的移动
                if (delayActive) {
                    // 使用 LiquidBounce 的移动工具类保持移动
                    val speed = sqrt(thePlayer.motionX * thePlayer.motionX + thePlayer.motionZ * thePlayer.motionZ)
                    if (speed > 0.1) {
                        val yaw = Math.toDegrees(atan2(thePlayer.motionZ, thePlayer.motionX)).toFloat() - 90.0f
                        thePlayer.motionX = -sin(Math.toRadians(yaw.toDouble())) * speed
                        thePlayer.motionZ = cos(Math.toRadians(yaw.toDouble())) * speed
                    }
                    delayActive = false
                }
            }

            "aacpush" -> {
                if (jump) {
                    if (thePlayer.onGround)
                        jump = false
                } else {
                    // Strafe
                    if (thePlayer.hurtTime > 0 && thePlayer.motionX != 0.0 && thePlayer.motionZ != 0.0)
                        thePlayer.onGround = true

                    // Reduce Y
                    if (thePlayer.hurtResistantTime > 0 && aacPushYReducer && !Speed.handleEvents())
                        thePlayer.motionY -= 0.014999993
                }

                // Reduce XZ
                if (thePlayer.hurtResistantTime >= 19) {
                    val reduce = aacPushXZReducer

                    thePlayer.motionX /= reduce
                    thePlayer.motionZ /= reduce
                }
            }

            "aaczero" ->
                if (thePlayer.hurtTime > 0) {
                    if (!hasReceivedVelocity || thePlayer.onGround || thePlayer.fallDistance > 2F)
                        return@handler

                    thePlayer.motionY -= 1.0
                    thePlayer.isAirBorne = true
                    thePlayer.onGround = true
                } else
                    hasReceivedVelocity = false
            "aac5" -> {
                if (mc.thePlayer.hurtTime > 1) {
                    reduceXZ(0.81)
                }
            }

            "legit" -> {
                if (legitDisableInAir && !isOnGround(0.5))
                    return@handler

                if (mc.thePlayer.maxHurtResistantTime != mc.thePlayer.hurtResistantTime || mc.thePlayer.maxHurtResistantTime == 0)
                    return@handler

                if (nextInt(endExclusive = 100) < chance) {
                    val horizontal = horizontal / 100f
                    val vertical = vertical / 100f

                    reduceXZ(horizontal.toDouble())
                    reduceY(vertical.toDouble())
                }
            }

            "intavereduce" -> {
                if (!hasReceivedVelocity) return@handler
                intaveTick++

                if (mc.thePlayer.hurtTime == 2) {
                    intaveDamageTick++
                    if (thePlayer.onGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                        thePlayer.tryJump()
                        intaveTick = 0
                    }
                    hasReceivedVelocity = false
                }
            }
            "intavetimer" -> {
                if (mc.thePlayer.hurtTime >= 8) {
                    mc.timer.timerSpeed = 0.3f
                } else if (mc.thePlayer.hurtTime > 2) {
                    mc.timer.timerSpeed = 5.0f
                } else if (mc.thePlayer.hurtTime == 2) {
                    mc.timer.timerSpeed = 1.0f
                }
            }

            "hypixel" -> {
                if (hasReceivedVelocity && thePlayer.onGround) {
                    absorbedVelocity = false
                }
            }

            "hypixelair" -> {
                if (hasReceivedVelocity) {
                    if (thePlayer.onGround) {
                        thePlayer.tryJump()
                    }
                    hasReceivedVelocity = false
                }
            }
            "custom" -> {
                progressivemode = ProgressiveMode
                progressivestepfactor = ProgressiveStep
                if (progressiveXZFactor > MaxProgressiveFactor) {
                    progressiveXZFactor = MaxProgressiveFactor
                } else if (progressiveXZFactor < MinProgressiveFactor) {
                    progressiveXZFactor = MinProgressiveFactor
                }
                if (hasReceivedVelocity2 && (AllowHurtTime.last >= thePlayer.hurtTime && AllowHurtTime.first <= thePlayer.hurtTime) && AttackHelper && CustomAttackReduce && ((mc.thePlayer.hurtTime == SpecialReduceHurtTime && SpecialReduce) || (mc.thePlayer.hurtTime == attackreducemaxhurttime && CustomAttackReduce))) {
                    var entity = mc.objectMouseOver?.entityHit

                    if (entity == null) {
                        if (CheckRotation) {
                            var result: Entity? = null

                            runWithModifiedRaycastResult(
                                currentRotation ?: thePlayer.rotation,
                                ActiveRange.toDouble(),
                                0.0
                            ) {
                                result = it.entityHit?.takeIf { it -> isSelected(it, true) }
                            }

                            entity = result
                        } else {
                            entity = getNearestEntityInRange(ActiveRange)?.takeIf { isSelected(it, true) }
                        }
                    }

                    entity ?: return@handler

                    val swingHand = {mc.thePlayer.swingItem()}
                    thePlayer.attackEntityWithModifiedSprint(entity, true) { swingHand() }
                    if (Debugger) {
                        chat("AttackHelper | Attacked")
                    }
                }
                if (thePlayer.hurtTime == 0 && AttackHelper) {
                    hasReceivedVelocity2 = false
                }
                if (thePlayer.hurtTime == 0 && CustomTimer) {
                    hasReceivedVelocity3 = false
                }
                if (CustomTimer) {
                    if (CustomTimerOnlyWhenReceivedVelocity) {
                        when (CustomTimerTimeMode) {
                            "HurtTime" -> {
                                if (mc.thePlayer.hurtTime >= CustomTimerLowMinHurtTime && hasReceivedVelocity3) {
                                    changeTimer(CustomTimerLowTimer)
                                } else if (!mc.thePlayer.onGround && mc.thePlayer.hurtTime >= CustomTimerMinWorkHurtTime) {
                                    changeTimer(CustomTimerMaxTimer)
                                } else changeTimer(1f)
                            }
                            "MSTimer" -> {
                                if (hasReceivedVelocity3) {
                                    val elapsedTime = System.currentTimeMillis() - TimerChangeTime.time

                                    when {
                                        elapsedTime <= CustomTimerLowMSTimer && mc.thePlayer.hurtTime != 0 -> {
                                            changeTimer(CustomTimerLowTimer)
                                        }
                                        elapsedTime >= CustomTimerLowMSTimer && elapsedTime <= CustomTimerMinWorkMSTimer && mc.thePlayer.hurtTime != 0 -> {
                                            changeTimer(CustomTimerMaxTimer)
                                        }
                                        else -> {
                                            changeTimer(1f)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        when (CustomTimerTimeMode) {
                            "HurtTime" -> {
                                if (mc.thePlayer.hurtTime >= CustomTimerLowMinHurtTime) {
                                    changeTimer(CustomTimerLowTimer)
                                } else if (!mc.thePlayer.onGround && mc.thePlayer.hurtTime >= CustomTimerMinWorkHurtTime) {
                                    changeTimer(CustomTimerMaxTimer)
                                } else changeTimer(1f)
                            }
                            "MSTimer" -> {
                                    val elapsedTime = System.currentTimeMillis() - TimerChangeTime.time

                                when {
                                    elapsedTime <= CustomTimerLowMSTimer && mc.thePlayer.hurtTime != 0 -> {
                                        changeTimer(CustomTimerLowTimer)
                                    }
                                    elapsedTime >= CustomTimerLowMSTimer && elapsedTime <= CustomTimerMinWorkMSTimer && mc.thePlayer.hurtTime != 0 -> {
                                        changeTimer(CustomTimerMaxTimer)
                                    }
                                    else -> {
                                        changeTimer(1f)
                                    }
                                }
                            }
                        }
                    }
                    val elapsedTime = System.currentTimeMillis() - ForwardTime.time
                    val elapsedTime2 = System.currentTimeMillis() - StrafeControlTime.time
                    if (TryForward && TryingForward && mc.thePlayer.hurtTime != 0) {
                        val elapsedTime = System.currentTimeMillis() - ForwardTime.time
                        if (elapsedTime <= TryingForwardTime) {
                            mc.thePlayer.movementInput.moveForward = 1.0f
                            when (SprintControl) {
                                "KeepSprint" -> changeSprint(true)
                                "StopSprint" -> changeSprint(false)
                            }
                        }
                    } else if (mc.thePlayer.hurtTime == 0 || elapsedTime >= TryingForwardTime) {
                        TryingForward = false
                    }

                    // DisableStrafeInput 逻辑
                    if (DisableStrafeInput && DisablingStrafeInput && mc.thePlayer.hurtTime != 0) {
                        val elapsedTime = System.currentTimeMillis() - StrafeControlTime.time
                        if (elapsedTime <= DisableStrafeInputTime) {
                            mc.thePlayer.movementInput.moveStrafe = 0.0f
                        }
                    } else if (mc.thePlayer.hurtTime == 0 || elapsedTime2 >= DisableStrafeInputTime) {
                        DisablingStrafeInput = false
                    }
                }
            }
            //Skid By NewFDPClient
            "grimvertical" -> {
                when(grimVerticalMode.lowercase()){
                    "1.17" -> {
                        if (canSpoof) {
                            sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.rotationYaw, thePlayer.rotationPitch, thePlayer.onGround))
                            sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, BlockPos(thePlayer).down(), DOWN))
                            canSpoof = false
                        }
                    }
                    "vertical" -> {
                        if (attack) {
                            val entity = mc.thePlayer.entityId

                            if (via) {
                                sendPacket(C02PacketUseEntity(mc.theWorld.getEntityByID(entity), C02PacketUseEntity.Action.ATTACK))
                                if (callEvent)
                                    sendPacket(C0APacketAnimation())
                            }
                            else {
                                if (callEvent)
                                    sendPacket(C0APacketAnimation())
                                sendPacket(C02PacketUseEntity(mc.theWorld.getEntityByID(entity), C02PacketUseEntity.Action.ATTACK))
                            }


                            if (smartVelo && thePlayer.onGround) {
                                reduceXZ(motionXZ)
                            } else {
                                reduceXZ(0.077760000)
                            }
                            velocityInput = false
                            attack = false
                        }
                    }
                }
            }
            "matrixreduce2" -> {
                if (hasReceivedVelocity && mc.thePlayer.hurtTime >= 9) {
                    if (mc.thePlayer.isMoving && !(mc.thePlayer.isBlocking || mc.thePlayer.isSneaking || mc.thePlayer.isEating || !mc.thePlayer.onGround)) {
                        reduceXZ(0.0)
                    } else if (!mc.thePlayer.isMoving || (mc.thePlayer.isBlocking || mc.thePlayer.isSneaking || mc.thePlayer.isEating || !mc.thePlayer.onGround)) {
                        reduceXZ(0.2)
                    }
                    hasReceivedVelocity = false
                }
            }
            "polarjump" -> {
                if (polarhurtTime == mc.thePlayer.hurtTime && mc.thePlayer.onGround) {
                    mc.thePlayer.tryJump()
                    if (polarJumpDebugger) {
                        chat("[PolarJump] Jumped")
                    }
                    polarhurtTime = nextInt(startInclusive = 7, endExclusive = 10)
                    if (polarJumpDebugger) {
                        chat("[PolarJump] NextJumpHurtTime: $polarhurtTime")
                    }
                }
                if (polarhurtCount >= forceChangehurtTimeCount) {
                    polarhurtCount = 0
                    polarhurtTime = nextInt(startInclusive = 7, endExclusive = 10)
                    if (polarJumpDebugger) {
                        chat("[PolarJump] ForceChangeJumpHurtTime-NextJumpHurtTime: $polarhurtTime")
                    }
                }
            }
            "intave14" -> {
                if (mc.thePlayer.hurtTime >= 9 && hasReceivedVelocity) {
                    onGroundTri = mc.thePlayer.onGround
                }
                if (mc.thePlayer.hurtTime == 0 && hasReceivedVelocity) {
                    hasReceivedVelocity = false
                }
                if (finalReverse && finalReverseTriggerMode == "AutoTrigger") {
                    if (mc.thePlayer.hurtTime == finalReverseHurtTime) {
                        if (!isMovingBackwards()) return@handler
                        if (!hasReceivedVelocity) return@handler
                        if (finalReverseStrict && TriggerTimes == 2) {
                            if (finalReverseCondition < 2) return@handler
                        } else if (finalReverseStrict && TriggerTimes == 3 && finalReverseCondition < 3) return@handler
                        reduceXZ(-finalReverseFactor.toDouble())
                        if (intave14Debugger) chat("FinalReversed [$finalReverseCondition/$TriggerTimes]")
                        finalReverseTriggered = true
                    }
                }
                if (intaveTimerTest) {
                    if (mc.thePlayer.hurtTime >= 8) {
                        mc.timer.timerSpeed = 0.3f
                    } else if (mc.thePlayer.hurtTime > 2) {
                        mc.timer.timerSpeed = 5.0f
                    } else if (mc.thePlayer.hurtTime == 2) {
                        mc.timer.timerSpeed = 1.0f
                    }
                }
            }
            "buzzreverse" -> {
                if (mc.thePlayer.hurtTime == 7 && hasReceivedVelocity) {
                    if (needAttack) return@handler
                    reduceXZ(-1.0)
                    hasReceivedVelocity = false
                }
                if (mc.thePlayer.hurtTime == 0 && hasReceivedVelocity) hasReceivedVelocity = false
            }
            "intave14.3.3" -> {
                if (mc.thePlayer.hurtTime == 10) {
                    reduceXZ(-1.0)
                } else if (mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround) {
                    reduceXZ(0.9)
                }
            }
            "intave13.0.6" -> {
                if (mc.thePlayer.hurtTime == 0) return@handler
                if (mc.thePlayer.isSprinting && hasReceivedVelocity) {
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionZ = 0.0
                    false
                }
            }
            "intave14.1.2" -> {
                if (mc.thePlayer.isSwingInProgress &&
                    (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f) &&
                    mc.thePlayer.onGround &&
                    mc.thePlayer.isSprinting &&
                    hasReceivedVelocity
                ) {
                    val yawRad = mc.thePlayer.rotationYaw * Math.PI / 180.0
                    mc.thePlayer.addVelocity(
                        -sin(yawRad) * 0.5,
                        0.1,
                        cos(yawRad) * 0.5
                    )
                }
            }
            "oldpolar" -> {
                if (mc.thePlayer.hurtTime > 0 && hasReceivedVelocity) {
                    reduceXZ(0.44999998807907104)
                    changeSprint(setState = false, sendPacketToServer = true)
                    hasReceivedVelocity = false
                }
            }
        }
    }


    private fun getMotionNoXZ(packetEntityVelocity: S12PacketEntityVelocity): Double {
        val vec = Vec3(
            packetEntityVelocity.motionX.toDouble(),
            packetEntityVelocity.motionY.toDouble(),
            packetEntityVelocity.motionZ.toDouble()
        )

        val strength = vec.lengthVector()
        val motionNoXZ: Double = if (strength >= 20000.0) {
            if (mc.thePlayer.onGround) {
                0.06425
            } else {
                0.075
            }
        } else if (strength >= 5000.0) {
            if (mc.thePlayer.onGround) {
                0.02625
            } else {
                0.0552
            }
        } else {
            0.0175
        }

        return motionNoXZ
    }

    /**
     * @see net.minecraft.entity.player.EntityPlayer.attackTargetEntityWithCurrentItem
     * Lines 1035 and 1058
     *
     * Minecraft only applies motion slow-down when you are sprinting and attacking, once per tick.
     * An example scenario: If you perform a mouse double-click on an entity, the game will only accept the first attack.
     *
     * This is where we come in clutch by making the player always sprint before dropping
     *
     * [clicks] amount of hits on the target [entity]
     *
     * We also explicitly-cast the player as an [Entity] to avoid triggering any other things caused from setting new sprint status.
     *
     * @see net.minecraft.client.entity.EntityPlayerSP.setSprinting
     * @see net.minecraft.entity.EntityLivingBase.setSprinting
     */
    val onGameTick = handler<GameTickEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (pauseTicks > 0 && pauseOnExplosion) return@handler
        when (mode) {
            "LegitClick" -> {
                if (thePlayer.hurtTime == 0) {
                    attackStartHurtTime = 0 // 重置
                    return@handler
                }

                if (ignoreBlocking && (thePlayer.isBlocking || KillAura.blockStatus)) {
                    return@handler
                }

                if (attackStartHurtTime == 0 && thePlayer.hurtTime > 0) {
                    attackStartHurtTime = thePlayer.hurtTime
                }

                val currentHurtTimeOffset = attackStartHurtTime - thePlayer.hurtTime
                if (currentHurtTimeOffset >= durationHurtTime) {
                    return@handler
                }

                var entity = mc.objectMouseOver?.entityHit

                if (entity == null) {
                    if (whenFacingEnemyOnly) {
                        var result: Entity? = null

                        runWithModifiedRaycastResult(
                            currentRotation ?: thePlayer.rotation,
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

                entity ?: return@handler

                val swingHand = {
                    when (swingMode.lowercase()) {
                        "normal" -> thePlayer.swingItem()
                        "packet" -> sendPacket(C0APacketAnimation())
                    }
                }

                val hits = clicks.random()
                repeat(hits) {
                    thePlayer.attackEntityWithModifiedSprint(entity, true) { swingHand() }
                }
            }
        }
    }

    val onAttack = handler<AttackEvent> {
        val player = mc.thePlayer ?: return@handler
        if (player.hurtTime <= attackreducemaxhurttime && player.hurtTime >= attackreduceminhurttime) {
            ForwardTime.reset()
            StrafeControlTime.reset()
        }

        if (AttackReduceOnlyWhenBackward && CustomAttackReduce && !isMovingBackwards() && mode == "Custom") {
            return@handler
        }

        if (mode == "AttackReduce" && mc.thePlayer.hurtTime in attackVelocityHurtTime) {
            if (keepingSprint()) reduceXZ(attackVelocitySprintFactor.toDouble()) else reduceXZ(attackVelocityFactor.toDouble())
        }
        if (mode == "IntaveStrong") {
            if (mc.thePlayer.hurtTime > 0) {
                reduceXZ(0.6)
            }
        }
        if (player.hurtTime in hurtTime && System.currentTimeMillis() - lastAttackTime <= 8000 && mode == "IntaveReduce" && hasReceivedVelocity) {
            reduceXZ(reduceFactor.toDouble())
        }
        if (mode == "Custom") {
            if (player.hurtTime == SpecialReduceHurtTime && SpecialReduce && CustomAttackReduce && !triggerTimesSpecial) {
                val finalXZFactor = if (randomizeXZ) {
                    val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                    SpecialReduceFactor * randomFactor
                } else {
                    SpecialReduceFactor
                }

                reduceXZ(finalXZFactor.toDouble())

                if (!SpecialMultiReduce) {
                    triggerTimesSpecial = true
                } else if (SpecialMultiReduce) {
                    triggerTimesSpecial = false
                }

                if (Debugger) {
                    val debugMsg = if (randomizeXZ) {
                        "[SpecialReduce] XZ: Base=${SpecialReduceFactor} RandomXZ=${finalXZFactor / SpecialReduceFactor} Final=$finalXZFactor | HurtTime=${player.hurtTime}"
                    } else {
                        "[SpecialReduce] XZ: Base=${SpecialReduceFactor} | HurtTime=${player.hurtTime}"
                    }
                    chat(debugMsg)
                }

                if (DisableStrafeInput) {
                    DisablingStrafeInput = true
                    StrafeControlTime.reset()
                }
                if (TryForward) {
                    TryingForward = true
                    ForwardTime.reset()
                }
            }

            if (player.hurtTime <= attackreducemaxhurttime && player.hurtTime >= attackreduceminhurttime && hasReceivedVelocity && CustomAttackReduce) {
                if (MultiReduce && triggerTimes < MaxTriggerTimes && !ProgressiveFactor) {
                    val finalXZFactor = if (randomizeXZ) {
                        val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                        attackreducefactor * randomFactor
                    } else {
                        attackreducefactor
                    }

                    val finalYFactor = if (randomizeY) {
                        val randomFactor = nextFloat(minYReduce, maxYReduce)
                        attackreduceYfactor * randomFactor
                    } else {
                        attackreduceYfactor
                    }

                    reduceXZ(finalXZFactor.toDouble())
                    reduceY(finalYFactor.toDouble())
                    triggerTimes++

                    if (Debugger) {
                        val xzDebug = if (randomizeXZ) {
                            "XZ: Base=$attackreducefactor RandomXZ=${finalXZFactor / attackreducefactor} Final=$finalXZFactor | "
                        } else {
                            "XZ: Base=$attackreducefactor | "
                        }

                        val yDebug = if (randomizeY) {
                            "Y: Base=$attackreduceYfactor RandomY=${finalYFactor / attackreduceYfactor} Final=$finalYFactor | "
                        } else {
                            "Y: Base=$attackreduceYfactor | "
                        }

                        chat("[AttackReduce] $xzDebug$yDebug HurtTime=${player.hurtTime} Triggers=$triggerTimes/$MaxTriggerTimes | HurtTime: ${mc.thePlayer.hurtTime}")
                    }

                    if (DoubleReduceWhenFirstReduce && DoubleReduce) {
                        val doubleFinalXZFactor = if (randomizeXZ) {
                            val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                            DoubleReduceFactor * randomFactor
                        } else {
                            DoubleReduceFactor
                        }

                        reduceXZ(finalXZFactor.toDouble())
                        DoubleReduce = false

                        if (Debugger) {
                            val debugMsg = if (randomizeXZ) {
                                "[DoubleReduce] XZ: Base=$DoubleReduceFactor RandomXZ=${doubleFinalXZFactor / DoubleReduceFactor} Final=$doubleFinalXZFactor | HurtTime: ${mc.thePlayer.hurtTime}"
                            } else {
                                "[DoubleReduce] XZ: Base=$DoubleReduceFactor | HurtTime: ${mc.thePlayer.hurtTime}"
                            }
                            chat(debugMsg)
                        }
                    }

                    if (DisableStrafeInput) {
                        DisablingStrafeInput = true
                        StrafeControlTime.reset()
                    }
                    if (TryForward) {
                        TryingForward = true
                        ForwardTime.reset()
                    }

                    if (player.hurtTime < attackreduceminhurttime) {
                        hasReceivedVelocity = false
                        triggerTimes = 0
                    }
                } else if (!MultiReduce) {
                    val finalXZFactor = if (randomizeXZ) {
                        val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                        attackreducefactor * randomFactor
                    } else {
                        attackreducefactor
                    }

                    val finalYFactor = if (randomizeY) {
                        val randomFactor = nextFloat(minYReduce, maxYReduce)
                        attackreduceYfactor * randomFactor
                    } else {
                        attackreduceYfactor
                    }

                    reduceXZ(finalXZFactor.toDouble())
                    reduceY(finalYFactor.toDouble())

                    if (Debugger) {
                        val xzDebug = if (randomizeXZ) {
                            "XZ: Base=$attackreducefactor RandomXZ=${finalXZFactor / attackreducefactor} Final=$finalXZFactor | "
                        } else {
                            "XZ: Base=$attackreducefactor | "
                        }

                        val yDebug = if (randomizeY) {
                            "Y: Base=$attackreduceYfactor RandomY=${finalYFactor / attackreduceYfactor} Final=$finalYFactor | "
                        } else {
                            "Y: Base=$attackreduceYfactor | "
                        }

                        chat("[AttackReduce] $xzDebug$yDebug | HurtTime: ${mc.thePlayer.hurtTime}")
                    }

                    if (DoubleReduceWhenFirstReduce && DoubleReduce) {
                        val doubleFinalXZFactor = if (randomizeXZ) {
                            val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                            DoubleReduceFactor * randomFactor
                        } else {
                            DoubleReduceFactor
                        }

                        reduceXZ(finalXZFactor.toDouble())
                        DoubleReduce = false

                        if (Debugger) {
                            val debugMsg = if (randomizeXZ) {
                                "[DoubleReduce] XZ: Base=$DoubleReduceFactor RandomXZ=${doubleFinalXZFactor / DoubleReduceFactor} Final=$doubleFinalXZFactor | HurtTime: ${mc.thePlayer.hurtTime}"
                            } else {
                                "[DoubleReduce] XZ: Base=$DoubleReduceFactor | HurtTime: ${mc.thePlayer.hurtTime}"
                            }
                            chat(debugMsg)
                        }
                    }

                    if (DisableStrafeInput) {
                        DisablingStrafeInput = true
                        StrafeControlTime.reset()
                    }
                    if (TryForward) {
                        TryingForward = true
                        ForwardTime.reset()
                    }
                    hasReceivedVelocity = false
                } else if (ProgressiveFactor && MultiReduce && triggerTimes < MaxTriggerTimes) {
                    if (triggerTimes == 0) {
                        progressiveXZFactor = BigDecimal(attackreducefactor.toString()).setScale(5, RoundingMode.DOWN).toFloat()
                    }

                    val finalProgressiveXZFactor = if (randomizeXZ) {
                        val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                        progressiveXZFactor * randomFactor
                    } else {
                        progressiveXZFactor
                    }

                    val finalYFactor = if (randomizeY) {
                        val randomFactor = nextFloat(minYReduce, maxYReduce)
                        attackreduceYfactor * randomFactor
                    } else {
                        attackreduceYfactor
                    }

                    reduceXZ(finalProgressiveXZFactor.toDouble())
                    reduceY(finalYFactor.toDouble())
                    triggerTimes++

                    if (Debugger) {
                        val xzDebug = if (randomizeXZ) {
                            "XZ: Base=$progressiveXZFactor RandomXZ=${finalProgressiveXZFactor / progressiveXZFactor} Final=$finalProgressiveXZFactor | "
                        } else {
                            "XZ: Base=$progressiveXZFactor | "
                        }

                        val yDebug = if (randomizeY) {
                            "Y: Base=$attackreduceYfactor RandomY=${finalYFactor / attackreduceYfactor} Final=$finalYFactor | "
                        } else {
                            "Y: Base=$attackreduceYfactor | "
                        }

                        chat("[ProgressiveReduce] $xzDebug$yDebug HurtTime=${player.hurtTime} Triggers=$triggerTimes/$MaxTriggerTimes")
                    }

                    if (DoubleReduceWhenFirstReduce && DoubleReduce) {
                        val doubleFinalXZFactor = if (randomizeXZ) {
                            val randomFactor = nextFloat(minXZReduce, maxXZReduce)
                            DoubleReduceFactor * randomFactor
                        } else {
                            DoubleReduceFactor
                        }

                        reduceXZ(doubleFinalXZFactor.toDouble())
                        DoubleReduce = false

                        if (Debugger) {
                            val debugMsg = if (randomizeXZ) {
                                "[DoubleReduce] XZ: Base=$DoubleReduceFactor RandomXZ=${doubleFinalXZFactor / DoubleReduceFactor} Final=$doubleFinalXZFactor | HurtTime: ${mc.thePlayer.hurtTime}"
                            } else {
                                "[DoubleReduce] XZ: Base=$DoubleReduceFactor | HurtTime: ${mc.thePlayer.hurtTime}"
                            }
                            chat(debugMsg)
                        }
                    }

                    progressiveXZFactor = if (progressivemode == "Decrease") {
                        BigDecimal(progressiveXZFactor.toString()).setScale(5, RoundingMode.DOWN).toFloat() - BigDecimal(progressivestepfactor.toString()).setScale(5, RoundingMode.DOWN).toFloat()
                    } else {
                        BigDecimal(progressiveXZFactor.toString()).setScale(5, RoundingMode.DOWN).toFloat() + BigDecimal(progressivestepfactor.toString()).setScale(5, RoundingMode.DOWN).toFloat()
                    }

                    progressiveXZFactor = BigDecimal(progressiveXZFactor.toString()).setScale(5, RoundingMode.DOWN).toFloat().coerceIn(MinProgressiveFactor, MaxProgressiveFactor)

                    if (DisableStrafeInput) {
                        DisablingStrafeInput = true
                        StrafeControlTime.reset()
                    }
                    if (TryForward) {
                        TryingForward = true
                        ForwardTime.reset()
                    }
                }
            }
        }
        if (mode == "Kazer") {
            reduceXZ(0.078,9..10)
        }
        if (mode == "Hylex") {
            when (player.hurtTime) {
                9 -> {
                    reduceXZ(0.8)
                }
                8 -> {
                    reduceXZ(0.11)
                }
                7 -> {
                    reduceXZ(0.4)
                }
                4 -> {
                    reduceXZ(0.37)
                }
            }
        }
        if (mode == "BuzzReverse" && needAttack && mc.thePlayer.hurtTime == 7 && hasReceivedVelocity) {
            reduceXZ(-1.0)
            hasReceivedVelocity = false
        }
        if (mode == "Dexland" && player.hurtTime > 0 && ++count % times == 0 && System.currentTimeMillis() - lastAttackTime <= 8000) {
            reduceXZ(hReduce.toDouble())
        }
        lastAttackTime = System.currentTimeMillis()
        if (mode == "Intave14") {
            if (OnlyWhenBackward) if (!isMovingBackwards()) return@handler
            if (!hasReceivedVelocity) return@handler
            when (mc.thePlayer.hurtTime) {
                firstReduce -> if (notTriggered1 && TriggerTimes >= 1) {
                    when (onGroundTri) {
                        true -> { // OnGround
                            reduceXZ(0.6)
                            reduceCondition = "OnGround"
                            intave14ReduceFactorText = "60%"
                        }

                        false -> { // InAir
                            reduceXZ(0.6)
                            reduceCondition = "InAir"
                            intave14ReduceFactorText = "60%"
                        }
                    }
                    finalReverseCondition++
                    if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                        mc.thePlayer.motionY -= yReduceCount
                        yReduceTriggeredTimes++
                        chat("YReduced")
                    }
                    notTriggered1 = false
                    if (intave14Debugger) chat("Reduce | Phase1 | $reduceCondition | $intave14ReduceFactorText")
                    notTriggeredA = false
                }

                secondReduce -> if (notTriggered2 && TriggerTimes >= 2) {
                    when (onGroundTri) {
                        true -> { // OnGround
                            if (notTriggeredA) {
                                reduceXZ(0.6)
                                intave14ReduceFactorText = "60%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                                notTriggeredA = false
                            } else {
                                reduceXZ(0.35)
                                intave14ReduceFactorText = "35%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                            }
                            reduceCondition = "OnGround"
                        }

                        false -> { // InAir
                            if (notTriggeredA) {
                                reduceXZ(0.6)
                                notTriggeredA = false
                                intave14ReduceFactorText = "60%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                            } else {
                                reduceXZ(0.35)
                                intave14ReduceFactorText = "35%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                            }
                            reduceCondition = "InAir"
                        }
                    }
                    finalReverseCondition++
                    notTriggered2 = false
                    if (intave14Debugger) chat("Reduce | Phase2 | $reduceCondition | $intave14ReduceFactorText")
                }

                thirdReduce -> if (notTriggered3 && TriggerTimes >= 3) {
                    when (onGroundTri) {
                        true -> { // OnGround
                            if (notTriggeredA) {
                                reduceXZ(0.6)
                                intave14ReduceFactorText = "60%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                                notTriggeredA = false
                            } else {
                                reduceXZ(0.15)
                                intave14ReduceFactorText = "15%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                            }
                            reduceCondition = "OnGround"
                        }

                        false -> { // InAir
                            if (notTriggeredA) {
                                reduceXZ(0.6)
                                notTriggeredA = false
                                intave14ReduceFactorText = "60%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                            } else {
                                if (applyDiffFactorOnGroundOrInAir) {
                                    reduceXZ(0.5)
                                } else reduceXZ(0.15)
                                intave14ReduceFactorText = if (applyDiffFactorOnGroundOrInAir) {
                                    "50%"
                                } else "15%"
                                if (yReduceTest && yReduceTriggeredTimes < yReduceMaxTimes) {
                                    mc.thePlayer.motionY -= yReduceCount
                                    yReduceTriggeredTimes++
                                    chat("YReduced")
                                }
                            }
                            reduceCondition = "InAir"
                        }
                    }
                    finalReverseCondition++
                    notTriggered3 = false
                    if (intave14Debugger) chat("Reduce | Phase3 | $reduceCondition | $intave14ReduceFactorText")
                }
                finalReverseHurtTime -> {
                    if (finalReverseTriggerMode != "NeedAttack") return@handler
                    if (OnlyWhenBackward) if (!isMovingBackwards()) return@handler
                    if (!finalReverse) return@handler
                    if (finalReverseStrict && TriggerTimes == 2) {
                        if (finalReverseCondition < 2) return@handler
                    } else if (finalReverseStrict && TriggerTimes == 3 && finalReverseCondition < 3) return@handler
                    reduceXZ(-finalReverseFactor.toDouble())
                    if(intave14Debugger) chat("FinalReversed [$finalReverseCondition/$TriggerTimes]")
                    finalReverseTriggered = true
                }

            }
            if (intaveMoreReduce) {
                val moreReduceHurtTime = if (TriggerTimes == 3) (thirdReduce - 1) else if (TriggerTimes == 2) (secondReduce - 1) else (firstReduce - 1)
                if (mc.thePlayer.hurtTime <= moreReduceHurtTime && intaveMoreReduceTimes < intaveMoreReduceMaxTimes && !finalReverseTriggered) {
                    val moreReduceFactor = if (intaveMoreReduceExtraReduce && notTriggered1 && notTriggered2 && notTriggered3) {
                        intaveMoreReduceAnotherFactor.toDouble()
                    } else {
                        intaveMoreReduceFactor.toDouble()
                    }


                    reduceXZ(moreReduceFactor)
                    intaveMoreReduceTimes++
                    if (intave14Debugger) chat("IntaveMoreReduce")
                }
            }
        }
    }


    private fun checkAir(blockPos: BlockPos): Boolean {
        val world = mc.theWorld ?: return false

        if (!world.isAirBlock(blockPos)) {
            return false
        }

        timerTicks = 20

        sendPackets(
            C03PacketPlayer(true),
            C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, DOWN)
        )

        world.setBlockToAir(blockPos)

        return true
    }
    private fun isMovingBackwards(): Boolean {
        val player = mc.thePlayer ?: return false
        val motionX = player.motionX
        val motionZ = player.motionZ

        // 1. 静止或微小运动 → 视为允许触发条件（非向前运动）
        if (sqrt(motionX * motionX + motionZ * motionZ) < 0.1) return true

        // 2. 计算运动方向角度（0~360°）
        val moveAngle = Math.toDegrees(atan2(motionX, motionZ)).toFloat().normalizeAngle()

        // 3. 获取玩家面朝方向（0~360°）
        val lookAngle = player.rotationYaw.normalizeAngle()

        // 4. 计算最小夹角差（0~180°）
        val angleDiff = minOf(
            abs(moveAngle - lookAngle),
            360 - abs(moveAngle - lookAngle)
        )

        // 5. 夹角≥70° → 视为非正向运动（包括侧向和向后）
        return angleDiff >= 60
    }

    // 角度标准化扩展方法
    private fun Float.normalizeAngle(): Float {
        return ((this % 360) + 360) % 360
    }
    // TODO: Recode
    private fun getDirection(): Double {
        var moveYaw = mc.thePlayer.rotationYaw
        when {
            mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing == 0f -> {
                moveYaw += if (mc.thePlayer.moveForward > 0) 0 else 180
            }

            mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing != 0f -> {
                if (mc.thePlayer.moveForward > 0) moveYaw += if (mc.thePlayer.moveStrafing > 0) -45 else 45 else moveYaw -= if (mc.thePlayer.moveStrafing > 0) -45 else 45
                moveYaw += if (mc.thePlayer.moveForward > 0) 0 else 180
            }

            mc.thePlayer.moveStrafing != 0f && mc.thePlayer.moveForward == 0f -> {
                moveYaw += if (mc.thePlayer.moveStrafing > 0) -90 else 90
            }
        }
        return Math.floorMod(moveYaw.toInt(), 360).toDouble()
    }

    val onPacket = handler<PacketEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        val packet = event.packet
        if (pauseTicks > 0 && pauseOnExplosion) {
            return@handler
        }

        if (event.isCancelled)
            return@handler
        if (event.packet is S12PacketEntityVelocity && event.packet.entityID == mc.thePlayer.entityId && event.packet.realMotionX == 0.0 && event.packet.realMotionZ == 0.0)
            if (mode == "MatrixReduce3" || cancelSpecialVelocity) {
                if (cancelSpecialVelocityKeepMotionY) mc.thePlayer.motionY = event.packet.realMotionY
                event.cancelEvent()
            }

        if ((packet is S12PacketEntityVelocity && thePlayer.entityId == packet.entityID && packet.motionY > 0 && (packet.motionX != 0 || packet.motionZ != 0))
            || (packet is S27PacketExplosion && (thePlayer.motionY + packet.field_149153_g) != 0.0
                    && ((thePlayer.motionX + packet.field_149152_f) != 0.0 || (thePlayer.motionZ + packet.field_149159_h) != 0.0))
        ) {
            velocityTimer.reset()

            if (pauseOnExplosion && packet is S27PacketExplosion
//                (thePlayer.motionY + packet.field_149153_g) > 0.0 && ((thePlayer.motionX + packet.field_149152_f) != 0.0 || (thePlayer.motionZ + packet.field_149159_h) != 0.0)
            ) {
                pauseTicks = ticksToPause
            }
            if (globalSprintReset) sprintResetReceivedKB = true
            when (mode.lowercase()) {
                "simple" -> handleVelocity(event)

                "legitclick","aac", "reverse", "smoothreverse", "aaczero", "ghostblock", "intavereduce", "matrixreduce2", "universocraftold", "buzzreverse","oldpolar","intave14.1.2","intave13.0.6","karhu" -> hasReceivedVelocity = true
                "custom" -> {
                    triggerTimes = 0
                    progressiveXZFactor = attackreducefactor
                    hasReceivedVelocity = true
                    hasReceivedVelocity2 = true
                    hasReceivedVelocity3 = true
                    DoubleReduce = true
                    triggerTimesSpecial = false
                    TimerChangeTime.reset()
                }
                "polarjump" -> {
                    polarhurtCount++
                }
                "intave14" -> {
                    finalReverseTriggered = false
                    hasReceivedVelocity = true
                    notTriggered1 = true
                    notTriggered2 = true
                    notTriggered3 = true
                    notTriggeredA = true
                    yReduceTriggeredTimes = 0
                    finalReverseCondition = 0
                    intaveHighTimering = false
                    intaveLowTimering = false
                    intaveMoreReduceTimes = 0
                }
                "intaveflag" ->{
                    intaFlag = false
                }
                "jump" -> {
                    // TODO: Recode and make all velocity modes support velocity direction checks
                    var packetDirection = 0.0
                    when (packet) {
                        is S12PacketEntityVelocity -> {
                            if (packet.entityID != thePlayer.entityId) return@handler

                            val motionX = packet.motionX.toDouble()
                            val motionZ = packet.motionZ.toDouble()

                            packetDirection = atan2(motionX, motionZ)
                        }

                        is S27PacketExplosion -> {
                            val motionX = thePlayer.motionX + packet.field_149152_f
                            val motionZ = thePlayer.motionZ + packet.field_149159_h

                            packetDirection = atan2(motionX, motionZ)
                        }
                    }
                    val degreePlayer = getDirection()
                    val degreePacket = Math.floorMod(packetDirection.toDegrees().toInt(), 360).toDouble()
                    var angle = abs(degreePacket + degreePlayer)
                    val threshold = 120.0
                    angle = Math.floorMod(angle.toInt(), 360).toDouble()
                    val inRange = angle in 180 - threshold / 2..180 + threshold / 2
                    if (inRange)
                        hasReceivedVelocity = true
                }
                "delay" -> {
                    when {
                        packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId -> {
                            // Delay 模式的核心逻辑
                            if (!delayReverseFlag &&
                                !canDelay() &&
                                !isInLiquidOrWeb() &&
                                !delayPendingExplosion &&
                                (!delayAllowNext || !delayFakeCheck)
                            ) {

                                delayChanceCounter = delayChanceCounter % 100 + delayChance
                                if (delayChanceCounter >= 100) {
                                    // 存储包并延迟处理
                                    delayedPackets[packet] = System.currentTimeMillis()
                                    event.cancelEvent()
                                    delayReverseFlag = true
                                    delayActive = true
                                    delayTimer.reset()
                                    return@handler
                                }
                            }

                            // 直接应用速度修改（如果不延迟）
                            applyVelocityReduction(packet)
                            event.cancelEvent()
                        }

                        packet is S27PacketExplosion -> {
                            // 处理爆炸击退
                            delayPendingExplosion = true
                            if (delayHorizontal == 0f || delayVertical == 0f) {
                                event.cancelEvent()
                            } else {
                                // 修改爆炸击退强度
                                packet.field_149152_f *= delayHorizontal
                                packet.field_149153_g *= delayVertical
                                packet.field_149159_h *= delayHorizontal
                            }
                        }
                    }
                }
                "glitch" -> {
                    if (!thePlayer.onGround)
                        return@handler

                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "matrixreduce" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        packet.motionX = (packet.getMotionX() * 0.33).toInt()
                        packet.motionZ = (packet.getMotionZ() * 0.33).toInt()

                        if (thePlayer.onGround) {
                            packet.motionX = (packet.getMotionX() * 0.86).toInt()
                            packet.motionZ = (packet.getMotionZ() * 0.86).toInt()
                        }
                    }
                }
                "matrixreduce3" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        event.cancelEvent()
                        if (abs(event.packet.realMotionY) >= 0.1f) {
                            mc.thePlayer.motionY = (event.packet.motionY / 8000f).toDouble()

                            val currentSpeed = hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ)

                            // 计算击退包中的原始水平速度
                            val knockbackX = packet.getMotionX() / 8000f
                            val knockbackZ = packet.getMotionZ() / 8000f
                            val knockbackSpeed = hypot(knockbackX, knockbackZ)
                            if (!mc.thePlayer.isMoving) {
                                val reducedSpeed = max(knockbackSpeed * 0.1,currentSpeed)
                                if (knockbackSpeed > 0) {
                                    mc.thePlayer.motionX = knockbackX / knockbackSpeed * reducedSpeed
                                    mc.thePlayer.motionZ = knockbackZ / knockbackSpeed * reducedSpeed
                                }
                            } else if (matrixReduce3Boost && matrixReduce3BoostTimer.hasTimePassed(matrixReduce3BoostDelay)) {
                                reduceXZ(matrixReduce3BoostFactor.toDouble() + 1)
                                matrixReduce3BoostTimer.reset()
                            }
                        }
                    }
                }

                // Credit: @LiquidSquid / Ported from NextGen
                "blocksmc" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        hasReceivedVelocity = true
                        event.cancelEvent()

                        sendPacket(C0BPacketEntityAction(thePlayer, START_SNEAKING))
                        sendPacket(C0BPacketEntityAction(thePlayer, STOP_SNEAKING))
                    }
                }

                "grimc03" -> {
                    // Checks to prevent from getting flagged (BadPacketsE)
                    if (thePlayer.isMoving) {
                        hasReceivedVelocity = true
                        event.cancelEvent()
                    }
                }

                "hypixel" -> {
                    hasReceivedVelocity = true
                    if (!thePlayer.onGround) {
                        if (!absorbedVelocity) {
                            event.cancelEvent()
                            absorbedVelocity = true
                            return@handler
                        }
                    }

                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        packet.motionX = (thePlayer.motionX * 8000).toInt()
                        packet.motionZ = (thePlayer.motionZ * 8000).toInt()
                    }
                }
                "bufferabuse" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        if (bufferAmount < bufferPacket) {
                            event.cancelEvent()
                            bufferAmount++
                            if (bufferDebugger) {
                                chat("[BufferAbuse] Cancelled packet $bufferAmount/$bufferPacket")
                            }
                            return@handler
                        }

                        // 处理第N个包
                        packet.motionX = (packet.motionX * bufferHorizontal).toInt()
                        packet.motionY = (packet.motionY * bufferVertical).toInt()
                        packet.motionZ = (packet.motionZ * bufferHorizontal).toInt()

                        bufferAmount = 0
                        if (bufferDebugger) {
                            chat("[BufferAbuse] Applied reduction: H=$bufferHorizontal, V=$bufferVertical")
                        }
                    } else if (packet is S27PacketExplosion) {
                        if (bufferAmount < bufferPacket) {
                            event.cancelEvent()
                            bufferAmount++
                            if (bufferDebugger) {
                                chat("[BufferAbuse] Cancelled explosion $bufferAmount/$bufferPacket")
                            }
                            return@handler
                        }

                        // 处理爆炸包
                        packet.field_149152_f *= bufferHorizontal
                        packet.field_149153_g *= bufferVertical
                        packet.field_149159_h *= bufferHorizontal

                        bufferAmount = 0
                        if (bufferDebugger) {
                            chat("[BufferAbuse] Applied explosion reduction")
                        }
                    }
                }
                "hypixelair" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "vulcan" -> {
                    event.cancelEvent()
                }

                "s32packet" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }
                //skid by FDPClient
                "grimvertical" -> {
                    if (packet is S12PacketEntityVelocity) {
                        if (packet.entityID == thePlayer.entityId) {
                            when (grimVerticalMode.lowercase()) {
                                "reduce" -> {
                                    val velocityX = packet.motionX / 8000.0
                                    val velocityZ = packet.motionZ / 8000.0

                                    thePlayer.motionX = velocityX * 0.078
                                    thePlayer.motionZ = velocityZ * 0.078
                                }

                                "1.17" -> {
                                    canCancel = true
                                    canSpoof = true
                                }

                                "vertical" -> {
                                    if (packet.motionX == 0 && packet.motionZ == 0 ||
                                        mc.thePlayer == null ||
                                        mc.theWorld.getEntityByID(packet.entityID) != mc.thePlayer
                                    ) {
                                        return@handler
                                    }

                                    velocityInput = true
                                    motionXZ = getMotionNoXZ(packet)

                                    if (thePlayer.isSprinting && thePlayer.serverSprintState && thePlayer.isMoving) {
                                        for (i in 0 until c0fPacketAmount) {
                                            if (sendC0FValue) {
                                                mc.netHandler.addToSendQueue(
                                                    C0FPacketConfirmTransaction(
                                                        nextInt(102, 1000024123),
                                                        nextInt(102, 1000024123).toShort(),
                                                        true
                                                    )
                                                )
                                            }
                                        }
                                        attack = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (mode == "HypixelMoving" && (event.packet is C0FPacketConfirmTransaction || event.packet is S12PacketEntityVelocity) && mc.thePlayer.isMoving) {
            event.cancelEvent()
        }


        if (mode == "OldGrim" && event.packet is S12PacketEntityVelocity) {
            val packet = event.packet
            if (packet.entityID != mc.thePlayer?.entityId) return@handler

            if ((mc.thePlayer.isInWeb && webValue) ||
                ((mc.thePlayer.isInWater || mc.thePlayer.isInLava) && liquidValue)) {
                return@handler
            }

            val horizontalStrength = sqrt(packet.motionX.toDouble().pow(2) + packet.motionZ.toDouble().pow(2))
            if (horizontalStrength <= 1000) return@handler

            oldGrimVelocity = true
            oldGrimAttacked = false
            oldGrimVelocityPacket = packet
            event.cancelEvent()
        }



        if (mode == "Custom" && CustomTimerC03 && event.packet is C03PacketPlayer && !(event.packet is C04PacketPlayerPosition || event.packet is C03PacketPlayer.C05PacketPlayerLook || event.packet is C03PacketPlayer.C06PacketPlayerPosLook)) {
            event.cancelEvent()
        }
        if (mode == "Cancel" && packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            event.cancelEvent()

            val cancelHorizon = "Horizontal" in cancelVelocity
            val cancelVertical = "Vertical" in cancelVelocity

            // 如果完全取消所有速度，直接返回
            if (cancelHorizon && cancelVertical && !cancelVerticalOnlyInAir) return@handler

            // 处理水平速度
            if (!cancelHorizon) {
                mc.thePlayer.motionX = packet.realMotionX
                mc.thePlayer.motionZ = packet.realMotionZ
            }

            // 处理垂直速度
            val shouldCancelVertical = cancelVertical || (cancelVerticalOnlyInAir && !mc.thePlayer.onGround)
            if (!shouldCancelVertical) {
                mc.thePlayer.motionY = packet.realMotionY
            }
        }
        if (mode == "IntaveTimer" && mc.thePlayer.hurtTime != 0 && event.packet is C03PacketPlayer && !(event.packet is C04PacketPlayerPosition
                    || event.packet is C03PacketPlayer.C05PacketPlayerLook || event.packet is C03PacketPlayer.C06PacketPlayerPosLook)
        ) {
            event.cancelEvent()
        }

        if (mode == "UniversoCraftOld" && hasReceivedVelocity) {
            if (packet is S12PacketEntityVelocity) {
                event.cancelEvent()
                mc.thePlayer.motionY += Math.random() / 100
            }
            if (packet is S27PacketExplosion) {
                event.cancelEvent()
                mc.thePlayer.motionY += Math.random() / 100
            }
            if (mc.thePlayer.hurtTime == 0) {
                hasReceivedVelocity = false
            }
        }

        if (mode == "BlocksMC" && hasReceivedVelocity) {
            if (packet is C0BPacketEntityAction) {
                hasReceivedVelocity = false
                event.cancelEvent()
            }
        }

        if (mode == "Vulcan") {
            if (Disabler.handleEvents() && Disabler.verusCombat && (!Disabler.onlyCombat || Disabler.isOnCombat)) return@handler

            if (packet is S32PacketConfirmTransaction) {
                event.cancelEvent()
                sendPacket(
                    C0FPacketConfirmTransaction(
                        if (transaction) 1 else -1,
                        if (transaction) -1 else 1,
                        transaction
                    ), false
                )
                transaction = !transaction
            }
        }

        if (mode == "S32Packet" && packet is S32PacketConfirmTransaction) {

            if (!hasReceivedVelocity)
                return@handler

            event.cancelEvent()
            hasReceivedVelocity = false
        }
    }

    /**
     * Tick Event (Abuse Timer Balance)
     */
    val onTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler
        when (mode.lowercase()) {
            "delay" -> {
                // 每 tick 更新延迟逻辑
                if (delayReverseFlag && delayTimer.hasTimePassed(50L * delayTicks)) {
                    // 强制应用延迟的速度包（超时保护）
                    applyDelayedVelocity()
                    delayReverseFlag = false
                    delayTickCounter = 0
                    delayTimer.reset()
                }

                // 重置状态
                if (player.hurtTime == 0) {
                    delayPendingExplosion = false
                    delayAllowNext = true
                }
            }
            "grimc03" -> {
                // Timer Abuse (https://github.com/CCBlueX/LiquidBounce/issues/2519)
                if (timerTicks > 0 && mc.timer.timerSpeed <= 1) {
                    val timerSpeed = 0.8f + (0.2f * (20 - timerTicks) / 20)
                    changeTimer(timerSpeed.coerceAtMost(1f))
                    --timerTicks
                } else if (mc.timer.timerSpeed <= 1) {
                    changeTimer(1f)
                }

                if (hasReceivedVelocity) {
                    val pos = BlockPos(player.posX, player.posY, player.posZ)

                    if (checkAir(pos))
                        hasReceivedVelocity = false
                }
            }
        }
    }

    /**
     * Delay Mode
     */
    val onDelayPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (event.isCancelled)
            return@handler

        if (mode == "LiquidBounceDelay") {
            if (packet is S32PacketConfirmTransaction || packet is S12PacketEntityVelocity) {

                event.cancelEvent()

                // Delaying packet like PingSpoof
                synchronized(packets) {
                    packets[packet] = System.currentTimeMillis()
                }
            }
            delayMode = true
        } else {
            delayMode = false
        }
    }

    /**
     * Reset on world change
     */
    val onWorld = handler<WorldEvent> {
        packets.clear()
        if (mode == "Delay") {
            resetDelayState()
        }
        delayedPackets.clear()
    }

    val onGameLoop = handler<GameLoopEvent> {
        if (mode == "LiquidBounceDelay")
            sendPacketsByOrder(false)
    }

    private fun sendPacketsByOrder(velocity: Boolean) {
        synchronized(packets) {
            packets.entries.removeAll { (packet, timestamp) ->
                if (velocity || timestamp <= System.currentTimeMillis() - spoofDelay) {
                    PacketUtils.schedulePacketProcess(packet)
                    true
                } else false
            }
        }
    }


    private fun reset() {
        sendPacketsByOrder(true)

        packets.clear()
    }

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer

        if (thePlayer == null || thePlayer.isInLiquid || thePlayer.isInWeb)
            return@handler

        when (mode.lowercase()) {
            "aacpush" -> {
                jump = true

                if (!thePlayer.isCollidedVertically)
                    event.cancelEvent()
            }

            "aaczero" ->
                if (thePlayer.hurtTime > 0)
                    event.cancelEvent()
        }
    }

    val onStrafe = handler<StrafeEvent> {
        val player = mc.thePlayer ?: return@handler
        if (mode == "Jump" && hasReceivedVelocity) {
            if (!player.isJumping && nextInt(endExclusive = 100) <= chance && shouldJump() && player.isSprinting && player.onGround && player.hurtTime == 9) {
                if (JumpResetOnlyOnSwingForJumpVlc) {
                    if (mc.thePlayer.isSwingInProgress) {
                        player.tryJump()
                        limitUntilJump = 0
                    }
                } else if (!JumpResetOnlyOnSwingForJumpVlc) {
                    player.tryJump()
                    limitUntilJump = 0
                }
            }
            hasReceivedVelocity = false
            return@handler
        }
        when (jumpCooldownMode.lowercase()) {
            "ticks" -> limitUntilJump++
            "receivedhits" -> if (player.hurtTime == 9) limitUntilJump++
        }
        if (mode !in arrayOf("Jump","Custom","IntaveReduce","PolarJump") && globalJumpReset) {
            var jumped = false
            if (forceSprintBeforeJump && mc.thePlayer.movementInput.moveForward > 0.707 && hasReceivedVelocity && mc.thePlayer.hurtTime >= 9 && mc.thePlayer.onGround) player setSprintSafely true
            if (globalCheckSprinting) {
                val sprintAllowed = mc.thePlayer.isSprinting ||
                        (globalIgnoreSprintingWhenBlocking && mc.thePlayer.isBlocking)

                if (!sprintAllowed) {
                    return@handler
                }
            }
            if (globalCheckBadEnvironment && mc.thePlayer.isInBadEnvironment()) return@handler
            if (rangeLimit && globalJumpCheckTarget == null) return@handler
            if (mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround && nextInt(endExclusive = 100) < jumpResetChance) {
                mc.thePlayer.tryJump()
                if (globalActionDebugger) chat("Jump")
                jumped = true
            }
            if (!mc.thePlayer.onGround && jumped && mc.thePlayer.motionY < 0) {
                changeTimer(fastFallSpeed)
            }
        }
        if (mode == "Custom" && hasReceivedVelocity && CustomJumpReset) {
            if (!player.isJumping && nextInt(endExclusive = 100) < CustomChance && shouldJump() && player.onGround && player.hurtTime <= JumpResetMaxHurtTime && player.hurtTime >= JumpResetMinHurtTime) {
                if (CustomJumpResetSafe && mc.thePlayer.isInBadEnvironment()) return@handler
                if (JumpResetOnlyOnSwing && !mc.thePlayer.isSwingInProgress) return@handler
                player.tryJump()
                limitUntilJump = 0
                if (AfterJumpSprintControl == "Stop") {
                    changeSprint(false)
                } else if (AfterJumpSprintControl == "Sprint") {
                    changeSprint(true)
                }
                if (Debugger) {
                    chat("[JumpReset] Jumped | HurtTime: " + mc.thePlayer.hurtTime)
                }
            }
        }
    }

    val onMotion = handler<MotionEvent> { event ->

        if (mode == "IntaveFlag") {
            if (mc.thePlayer.hurtTime >= 9 && !intaFlag) {
                mc.netHandler.addToSendQueue(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX + 6.0,
                        mc.thePlayer.posY + 1.0,
                        mc.thePlayer.posZ + 6.0,
                        false
                    )
                )
                intaFlag = true
            }
        }

        if (mode == "Intave14.3.3") {
            if (mc.thePlayer.hurtTime == 10) {
                reduceXZ(-1.0)
            } else if (mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround) {
                reduceXZ(0.9)
            }
        }
        if (mode == "Intave13.0.6") {
            if (mc.thePlayer.hurtTime == 0) return@handler
            if (mc.thePlayer.isSprinting && hasReceivedVelocity) {
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                false
            }
        }
        if (mode == "Intave14.1.2" && hasReceivedVelocity) {
            if (mc.thePlayer.isSwingInProgress &&
                (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f) &&
                mc.thePlayer.onGround &&
                mc.thePlayer.isSprinting
            ) {
                val yawRad = mc.thePlayer.rotationYaw * Math.PI / 180.0
                mc.thePlayer.addVelocity(
                    -sin(yawRad) * 0.5,
                    0.1,
                    cos(yawRad) * 0.5
                )
            }
        }
        if (mode == "OldPolar" && mc.thePlayer.hurtTime > 0 && hasReceivedVelocity) {
            reduceXZ(0.44999998807907104)
            changeSprint(false)
            hasReceivedVelocity = false
        }



        if (mc.thePlayer == null) return@handler

        val player = mc.thePlayer!!

        if (player.hurtTime == 0) {
            oldGrimVelocity = false
            oldGrimAttacked = false
        }






        if (oldGrimVelocity && !oldGrimAttacked) {
            // 使用LiquidBounce的EntityUtils寻找目标
            val entity = if (oldGrimRayCast) {
                // 使用射线检测
                var result: Entity? = null
                runWithModifiedRaycastResult(
                    currentRotation ?: player.rotation,
                    3.2,
                    0.0
                ) { it ->
                    result = it.entityHit?.takeIf { e ->
                        isSelected(e, true) && e != player
                    }
                }
                result
            } else {
                // 使用KillAura的目标
                KillAura.target?.takeIf { e ->
                    e.getDistanceToEntityBox(player) <= 3.0 && isSelected(e, true)
                }
            }

            entity ?: return@handler

            // 执行攻击逻辑
            val state = player.serverSprintState
            if (!state) {
                sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING))
            }

            val count = if (oldGrimLegit) 1 else 6
            repeat(count) {
                sendPacket(C0APacketAnimation())
                sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
                if (!oldGrimLegit) {
                    sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
                }
            }

            if (!state) {
                sendPacket(C03PacketPlayer(player.onGround))
                sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }

            oldGrimAttacked = true
            player.motionX *= oldGrimAttackReduce.toDouble()
            player.motionZ *= oldGrimAttackReduce.toDouble()
        }
    }


    val onBlockBB = handler<BlockBBEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        if (pauseOnExplosion && pauseTicks > 0) return@handler
        if (mode == "GhostBlock") {
            if (hasReceivedVelocity) {
                if (player.hurtTime in hurtTimeRange) {
                    // Check if there is air exactly 1 level above the player's Y position
                    if (event.block is BlockAir && event.y == mc.thePlayer.posY.toInt() + 1) {
                        event.boundingBox = AxisAlignedBB(
                            event.x.toDouble(),
                            event.y.toDouble(),
                            event.z.toDouble(),
                            event.x + 1.0,
                            event.y + 1.0,
                            event.z + 1.0
                        )
                    }
                } else if (player.hurtTime == 0) {
                    hasReceivedVelocity = false
                }
            }
        }
        if (mode == "Karhu" && mc.thePlayer.hurtTime > 0 && event.block is BlockAir && event.y.toDouble() == mc.thePlayer.posY + 1 && hasReceivedVelocity) {
            event.boundingBox = AxisAlignedBB(
                event.x.toDouble(),                      // block x position
                event.y.toDouble(),                      // block y position
                event.z.toDouble(),                      // block z position
                event.x.toDouble() + 1.0,                // maxX
                event.y.toDouble() + 1.0,                // maxY
                event.z.toDouble() + 1.0                 // maxZ
            )
        } else if (player.hurtTime == 0) {
            hasReceivedVelocity = false
        }
    }


    private fun shouldJump() = when (jumpCooldownMode.lowercase()) {
        "ticks" -> limitUntilJump >= ticksUntilJump
        "receivedhits" -> limitUntilJump >= hitsUntilJump
        else -> false
    }

    private fun handleVelocity(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity) {
            // Always cancel event and handle motion from here
            event.cancelEvent()

            if (horizontal == 0f && vertical == 0f)
                return

            // Don't modify player's motionXZ when horizontal value is 0
            if (horizontal != 0f) {
                var motionX = packet.realMotionX
                var motionZ = packet.realMotionZ

                if (limitMaxMotionValue.get()) {
                    val distXZ = sqrt(motionX * motionX + motionZ * motionZ)

                    if (distXZ > maxXZMotion) {
                        val ratioXZ = maxXZMotion / distXZ

                        motionX *= ratioXZ
                        motionZ *= ratioXZ
                    }
                }

                mc.thePlayer.motionX = motionX * horizontal
                mc.thePlayer.motionZ = motionZ * horizontal
            }

            // Don't modify player's motionY when vertical value is 0
            if (vertical != 0f) {
                var motionY = packet.realMotionY

                if (limitMaxMotionValue.get())
                    motionY = motionY.coerceAtMost(maxYMotion + 0.00075)

                mc.thePlayer.motionY = motionY * vertical
            }
        } else if (packet is S27PacketExplosion) {
            // Don't cancel explosions, modify them, they could change blocks in the world
            if (horizontal != 0f && vertical != 0f) {
                packet.field_149152_f = 0f
                packet.field_149153_g = 0f
                packet.field_149159_h = 0f

                return
            }

            // Unlike with S12PacketEntityVelocity explosion packet motions get added to player motion, doesn't replace it
            // Velocity might behave a bit differently, especially LimitMaxMotion
            packet.field_149152_f *= horizontal // motionX
            packet.field_149153_g *= vertical // motionY
            packet.field_149159_h *= horizontal // motionZ

            if (limitMaxMotionValue.get()) {
                val distXZ =
                    sqrt(packet.field_149152_f * packet.field_149152_f + packet.field_149159_h * packet.field_149159_h)
                val distY = packet.field_149153_g
                val maxYMotion = maxYMotion + 0.00075f

                if (distXZ > maxXZMotion) {
                    val ratioXZ = maxXZMotion / distXZ

                    packet.field_149152_f *= ratioXZ
                    packet.field_149159_h *= ratioXZ
                }

                if (distY > maxYMotion) {
                    packet.field_149153_g *= maxYMotion / distY
                }
            }
        }
    }

    fun sprintReset() {
        if (!wTapTimer.hasTimePassed(50)) {
            mc.thePlayer setSprintSafely false
        } else {
            mc.thePlayer setSprintSafely true
            wTapTimer.reset()
            return
        }
    }
    private fun buildGlobalTag(): String {
        return buildList {
            if (globalSprintReset) add("SprintReset")
            if (globalJumpReset && mode !in arrayOf("PolarJump","Jump","IntaveReduce","Custom")) add("JumpReset")
        }.joinToString(" | ")
    }


    private fun getNearestEntityInRange(range: Float = this.range): Entity? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld.loadedEntityList.filter {
            isSelected(it, true) && player.getDistanceToEntityBox(it) <= range
        }.minByOrNull { player.getDistanceToEntityBox(it) }
    }
    private fun canDelay(): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return thePlayer.onGround && (!KillAura.state || !KillAura.blockStatus)
    }

    private fun isInLiquidOrWeb(): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb
    }

    // 应用延迟的速度包
    private fun applyDelayedVelocity() {

        delayedPackets.entries.removeAll { (packet, timestamp) ->
            if (packet is S12PacketEntityVelocity) {
                applyVelocityReduction(packet)
                true
            } else {
                false
            }
        }
    }

    // 应用速度减少
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
    private fun resetDelayState() {
        delayChanceCounter = 0
        delayActive = false
        delayReverseFlag = false
        delayPendingExplosion = false
        delayAllowNext = true
        delayTickCounter = 0
        delayTimer.reset()
    }
}
