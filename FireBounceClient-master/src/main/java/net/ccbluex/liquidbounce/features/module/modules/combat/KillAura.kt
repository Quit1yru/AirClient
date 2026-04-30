/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.FireBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.runWithSimulatedPosition
import net.ccbluex.liquidbounce.features.module.modules.combat.NewVelocity.doNotNeedReduce
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.Fucker
import net.ccbluex.liquidbounce.features.module.modules.world.Nuker
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Tower
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.resetLastAttackedTicks
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.extra.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.math.FastMathUtil.sqrt
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.potion.Potion
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.roundToInt

object KillAura : Module("KillAura", Category.COMBAT, Keyboard.KEY_R) {
    /**
     * OPTIONS
     */
    private val attackDelayCalculationMode by choices("AttackDelayCalculationMode", arrayOf("CPS", "Delay"), "CPS")
    private val simulateCooldown by boolean("SimulateCooldown", false) { attackDelayCalculationMode == "CPS" }
    private val simulateDoubleClicking by boolean(
        "SimulateDoubleClicking",
        false
    ) { !simulateCooldown && attackDelayCalculationMode == "CPS" }
    private val simulateDoubleClickChance by int(
        "SimulateDoubleClickChance",
        100,
        0..100,
        "%"
    ) { !simulateCooldown && simulateDoubleClicking && attackDelayCalculationMode == "CPS" }
    private val simulateDoubleClickHurtTime by intRange(
        "SimulateDoubleClickHurtTime",
        0..10,
        0..10
    ) { !simulateCooldown && simulateDoubleClicking && attackDelayCalculationMode == "CPS" }
    private val randomCPS by boolean("RandomCPS", false) {
        attackDelayCalculationMode == "CPS"
    }
    private val randomCPSRange by intRange(
        "RandomCPS-CPSRange",
        18..20,
        1..100,
        description = "TotalAttackCount In a second But More Random"
    ) { randomCPS && attackDelayCalculationMode == "CPS" }

    // CPS - Attack speed
    private val cpsMode by choices(
        "CPSMode",
        arrayOf(
            "RNG",
            "Record1",
            "Record2",
            "Record3",
            "Butterfly",
            "Jitter",
            "ConstantMiddle",
            "ConstGenerate",
            "ConstGenerate2",
            "Extra"
        ),
        "RNG"
    )

    private val cps by intRange("CPS", 5..8, 1..100, description = "TotalAttackCount In a second") {
        !simulateCooldown && !randomCPS && attackDelayCalculationMode == "CPS"
    }.onChanged {
        updateCPS()
        attackDelay = calculateAttackDelay()
    }

    private val damageBoostClick by boolean("DamageBoostClick", false) { attackDelayCalculationMode == "CPS" }
    private val damageBoostClickFactor by float(
        "DamageBoostClickFactor",
        1.5f,
        1f..100f,
        "x"
    ) { damageBoostClick && attackDelayCalculationMode == "CPS" }
    private val damageBoostClickMinHurtTime by int(
        "DamageBoostMinHurtTime",
        6,
        1..10
    ) { damageBoostClick && attackDelayCalculationMode == "CPS" }

    private val attackDelaySetting by intRange("AttackDelay", 50..100, 1..1000, "ms") {
        attackDelayCalculationMode == "Delay"
    }.onChanged {
        attackDelay = calculateAttackDelay()
        attackTimer.reset()
    }

    private var currentCPS: Int = 0
    private val cpsTimer = MSTimer()


    private val targetHurtTime by intRange("AttackTargetHurtTime", 0..10, 0..10) { !simulateCooldown }
    private val ownHurtTime by intRange("AttackOwnHurtTime", 0..10, 0..10) { !simulateCooldown }

    private val activationSlot by boolean("ActivationSlot", false)
    private val preferredSlot by int("PreferredSlot", 1, 1..9) { activationSlot }

    private val clickOnly by boolean("ClickOnly", false)

    // Range
    // TODO: Make block range independent from attack range
    val rangeCalculateMode by choices("RangeCalculateMode", arrayOf("Client-Side", "Server-Side"), "Client-Side")
    val range: Float by float("Range", 3.0f, 1f..8f).onChanged {
        blockRange = blockRange.coerceAtMost(it)
    }
    val rotationRange by float("RotationRange", 4.5f, 0f..10f).onChange { _, new ->
    new.coerceAtLeast(this@KillAura.range)
    }
    private val throughWallsRange by float("ThroughWallsRange", 3f, 0f..8f)
    private val rotationThroughWallsRange by float("RotationThroughWallsRange", 3f, 0f..8f) { options.rotationsActive }
    private val rangeSprintReduction by float("RangeSprintReduction", 0f, 0f..0.4f)

    // Modes
    private val priority by choices(
        "Priority", arrayOf(
            "Health",
            "Distance",
            "Direction",
            "LivingTime",
            "Armor",
            "HurtResistance",
            "HurtTime",
            "HealthAbsorption",
            "RegenAmplifier",
            "OnLadder",
            "InLiquid",
            "InWeb"
        ), "Distance"
    )
    private val targetMode by choices("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val lockTarget by boolean("TargetLocker", false, description = "Make you can attack specific name player")
    private val lockTargetName by text("TargetName", "") { lockTarget }
    private val TagMode by choices(
        "TagMode", arrayOf(
            "TargetMode+TargetCount",
            "TargetCount",
            "TargetMode",
            "TargetName",
            "TargetMode+BlockMode",
            "CustomText"
        ), "TargetMode+TargetCount"
    )
    private val customTagText by text("CustomTagText", "") { TagMode == "CustomText" }
    private val limitedMultiTargets by int("LimitedMultiTargets", 0, 0..50) { targetMode == "Multi" }
    private val maxSwitchFOV by float("MaxSwitchFOV", 90f, 30f..180f) { targetMode == "Switch" }

    // Delay
    private val switchDelay by int("SwitchDelay", 15, 1..1000) { targetMode == "Switch" }

    private val maxMultiRotationDifference by float("MaxRotationDifferenceEachTarget", 45f, 0f..180f) {
        targetMode == "Multi" && options.rotationsActive
    }

    // Bypass
    val swing by boolean("Swing", true)
    private val noSwingOnlyClientSide by boolean("NoSwingOnlyClientSide", false) { swing }
    private val hideSwingOnlyFakeSwing by boolean(
        "OnlyHideFakeSwing'sSwing",
        false
    ) { swing && noSwingOnlyClientSide && failSwing }

    val keepSprint by boolean("KeepSprint", true)
    private val keepSprintMode by choices(
        "KeepSprintMode", arrayOf(
            "Always",
            "Cooldown",
            "HurtTime",
            "SmartPredict",
        ), "Always"
    ) { keepSprint }
    private val keepSprintPredictTicks by int("KeepSprintPredictTicks", 1, 1..5) { "SmartPredict" in keepSprintMode && keepSprint }
    private val keepSprintCooldownMode by choices(
        "KeepSprintCooldownMode",
        arrayOf("Tick", "AttackCount"),
        "AttackCount"
    ) { "Cooldown" == keepSprintMode && keepSprint }
    private val keepSprintCooldownTick by int(
        "KeepSprintCooldownTick",
        1,
        1..20
    ) { "Cooldown" in keepSprintMode && "Tick" in keepSprintCooldownMode && keepSprint }
    private val keepSprintCooldownAttack by int(
        "KeepSprintCooldownAttack",
        1,
        1..20
    ) { "Cooldown" in keepSprintMode && "AttackCount" in keepSprintCooldownMode && keepSprint }
    private val keepSprintDurationAttack by int(
        "AllowKeepSprintAttackCount",
        3,
        1..20
    ) { "Cooldown" in keepSprintMode && keepSprint }
    private val keepSprintOwnHurtTime by intRange(
        "KeepSprintOwnHurtTime",
        0..10,
        0..10
    ) { keepSprint && "HurtTime" in keepSprintMode && keepSprint }
    private val keepSprintTargetHurtTime by intRange(
        "KeepSprintTargetHurtTime",
        0..10,
        0..10
    ) { keepSprint && "HurtTime" in keepSprintMode && keepSprint }

    val keepSprint2 by boolean("KeepSprintWhenNotHitExpectTarget", false)
    private var cooldownTicks = 0
    private var cooldownAttacks = 0
    private var allowedAttacks = 0

    val shouldKeepSprint: Boolean
        get() {
            if (keepSprint2 && realHitTarget != target && handleEvents() && realHitTarget != null) return true

            if (!keepSprint) return false

            return when (keepSprintMode) {
                "Always" -> true
                "HurtTime" -> {
                    mc.thePlayer.hurtTime in keepSprintOwnHurtTime && target?.hurtTime in keepSprintTargetHurtTime
                }

                "Cooldown" -> {
                    if (allowedAttacks > 0) {
                        return true
                    }

                    when (keepSprintCooldownMode) {
                        "Tick" -> cooldownTicks <= 0
                        "AttackCount" -> cooldownAttacks <= 0
                        else -> false
                    }
                }

                "SmartPredict" -> {
                    if (target != null) if (predictPos(keepSprintPredictTicks) >= range) true else false

                    if (mc.thePlayer.hurtTime > 0) {
                        if (doNotNeedReduce) return true else false
                    } else {
                        return true
                    }
                }

                else -> false
            }
        }

    // Settings
    private val autoF5 by boolean("AutoF5", false)
    private val onScaffold by boolean("OnScaffold", false)
    private val onDestroyBlock by boolean("OnDestroyBlock", false)

    // AutoBlock
    val autoBlock by choices("AutoBlock", arrayOf("Off", "Packet", "BlocksMC", "Fake", "Force", "Regular"), "Packet")

    private val blockMaxRange by float("BlockMaxRange", 3f, 0f..8f) { autoBlock == "Packet" || autoBlock == "Regular" }
    private val unblockMode by choices(
        "UnblockMode", arrayOf("Stop", "Switch", "Empty"), "Stop"
    ) { autoBlock == "Packet" || autoBlock == "Regular" }

    // Regular AutoBlock settings
    private val blockCooldownTick by int("BlockCooldownTick", 10, 1..40) { autoBlock == "Regular" }
    private val blockDurationTick by int("BlockDurationTick", 5, 1..20) { autoBlock == "Regular" }

    private val releaseAutoBlock by boolean(
        "ReleaseAutoBlock",
        true,
        description = "Automatically cancel blocking when an attack is needed"
    ) { autoBlock !in arrayOf("Off", "Fake", "BlocksMC", "Force", "Regular") }
    val forceBlockRender by boolean("ForceBlockRender", true) {
        (autoBlock !in arrayOf(
            "Off", "Fake", "BlocksMC", "Force"
        ) && releaseAutoBlock) || autoBlock in arrayOf("Regular")
    }
    private val ignoreTickRule by boolean("IgnoreTickRule", false) {
        autoBlock !in arrayOf(
            "Off", "Fake", "BlocksMC", "Force", "Regular"
        ) && releaseAutoBlock && !ignoreTickRule2
    }
    private val ignoreTickRule2 by boolean("IgnoreTickRule2", false) {
        autoBlock !in arrayOf(
            "Off", "Fake", "BlocksMC", "Force", "Regular"
        ) && releaseAutoBlock
    }
    private val blockRate by int("BlockRate", 100, 1..100, "%") {
        autoBlock !in arrayOf(
            "Off",
            "Fake",
            "Force",
            "Regular"
        ) && releaseAutoBlock
    }

    private val uncpAutoBlock by boolean("UpdatedNCPAutoBlock", false) {
        autoBlock !in arrayOf(
            "Off", "Fake", "BlocksMC", "Force", "Regular"
        ) && !releaseAutoBlock
    }

    private val switchStartBlock by boolean("SwitchStartBlock", false) {
        autoBlock !in arrayOf(
            "Off",
            "Fake",
            "BlocksMC",
            "Force",
            "Regular"
        )
    }

    private val interactAutoBlock by boolean("InteractAutoBlock", true) {
        autoBlock !in arrayOf(
            "Off",
            "Fake",
            "BlocksMC",
            "Force",
            "Regular"
        )
    }

    val blinkAutoBlock by boolean("BlinkAutoBlock", false) {
        autoBlock !in arrayOf(
            "Off",
            "Fake",
            "BlocksMC",
            "Force",
            "Regular"
        )
    }

    private val blinkBlockTicks by int("BlinkBlockTicks", 3, 2..5) {
        autoBlock !in arrayOf(
            "Off", "Fake", "BlocksMC", "Force", "Regular"
        ) && blinkAutoBlock
    }

    // Force Block mode settings
    private val notOnScaffolding by boolean("NotOnScaffolding", true) { autoBlock == "Force" }

    // AutoBlock conditions
    private val smartAutoBlock by boolean(
        "SmartAutoBlock",
        false,
        "Use more value to make autoblock smarter"
    ) { autoBlock == "Packet" || autoBlock == "Regular" }

    // Ignore all blocking conditions, except for block rate, when standing still
    private val forceBlock by boolean(
        "ForceBlockWhenStill",
        true,
        description = "Ignore all blocking conditions, except for block rate, when standing still"
    ) { smartAutoBlock }

    // Don't block if target isn't holding a sword or an axe
    private val checkWeapon by boolean(
        "CheckEnemyWeapon",
        true,
        description = "Don't block if target isn't holding a sword or an axe"
    ) { smartAutoBlock }

    // TODO: Make block range independent from attack range
    private var blockRange: Float by float("BlockRange", range, 1f..8f) {
        smartAutoBlock
    }.onChange { _, new ->
        new.coerceAtMost(this@KillAura.range)
    }

    // Don't block when you can't get damaged
    private val maxOwnHurtTime by intRange(
        "MaxOwnHurtTime",
        0..3,
        0..10
    ) { smartAutoBlock }
    private val smartBlockTargetHurtTimeRange by intRange(
        "SmartBlockTargetHurtTimeRange",
        0..10,
        0..10
    ) { smartAutoBlock }
    // Don't block if target isn't looking at you
    private val maxDirectionDiff by float("MaxOpponentDirectionDiff", 60f, 30f..180f) { smartAutoBlock }

    // Don't block if target is swinging an item and therefore cannot attack
    private val maxSwingProgress by int("MaxOpponentSwingProgress", 1, 0..5) { smartAutoBlock }

    // Rotations
    private val options = RotationSettings(this).withoutKeepRotation()

    // Raycast
    private val raycastValue = boolean("RayCast", true) { options.rotationsActive }
    private val raycast by raycastValue
    private val raycastIgnored by boolean(
        "RayCastIgnored", false
    ) { raycastValue.isActive() && options.rotationsActive }
    private val livingRaycast by boolean("LivingRayCast", true) { raycastValue.isActive() && options.rotationsActive }

    // Hit delay
    private val useHitDelay by boolean("UseHitDelay", false)
    private val hitDelayTicks by int("HitDelayTicks", 1, 1..5) { useHitDelay }

    private val generateClicksBasedOnDist by boolean("GenerateClicksBasedOnDistance", false)
    private val cpsMultiplier by intRange("CPS-Multiplier", 1..2, 1..10) { generateClicksBasedOnDist }
    private val distanceFactor by floatRange("DistanceFactor", 5F..10F, 1F..10F) { generateClicksBasedOnDist }

    private val generateSpotBasedOnDistance by boolean("GenerateSpotBasedOnDistance", false) { options.rotationsActive }

    private val randomization = RandomizationSettings(this) { options.rotationsActive }
    private val outBorder by boolean("OutBorder", false) { options.rotationsActive }

    private val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
        coercedPoint.displayName
    }
    private val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
        coercedPoint.displayName
    }

    private val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    private val horizontalBodySearchRange by floatRange(
        "HorizontalBodySearchRange", 0f..1f, 0f..1f
    ) { options.rotationsActive }

    private val fov by float("FOV", 180f, 0f..180f)

    // Prediction
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5)
    private val predictOnlyWhenOutOfRange by boolean(
        "PredictOnlyWhenOutOfRange", false
    ) { predictClientMovement != 0 }
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f)

    private val forceFirstHit by boolean("ForceFirstHit", false) { !respectMissCooldown && !useHitDelay }

    // Extra swing
    private val failSwing by boolean("FailSwing", true) { swing && options.rotationsActive }
    private val swingPerSecond by intRange("SwingPerSecond", 5..8, 1..50) { failSwing }.onChanged {
        swingDelay = calculateSwingDelay()
    }
    private val swingRange by floatRange("SwingRange", 0.0f..3.5f, 0f..8f) {
        swing && failSwing && options.rotationsActive
    }.onChange { _, new ->
        new.start.coerceAtMost(rotationRange)..new.endInclusive.coerceAtMost(rotationRange)
    }

    private val respectMissCooldown by boolean(
        "RespectMissCooldown", false
    ) { swing && failSwing && options.rotationsActive }
    private val swingOnlyInAir by boolean("SwingOnlyInAir", true) { swing && failSwing && options.rotationsActive }
    private val maxRotationDifferenceToSwing by float(
        "MaxRotationDifferenceToSwing", 180f, 0f..180f
    ) { swing && failSwing && options.rotationsActive }
    private val swingWhenTicksLate = boolean("SwingWhenTicksLate", false) {
        swing && failSwing && maxRotationDifferenceToSwing != 180f && options.rotationsActive
    }
    private val ticksLateToSwing by int(
        "TicksLateToSwing", 4, 0..20
    ) { swing && failSwing && swingWhenTicksLate.isActive() && options.rotationsActive }

    private val renderBoxOnSwingFail by boolean("RenderBoxOnSwingFail", false) { failSwing }
    private val renderBoxColor = ColorSettingsInteger(this, "RenderBoxColor") { renderBoxOnSwingFail }.with(Color.CYAN)
    private val renderBoxFadeSeconds by float("RenderBoxFadeSeconds", 1f, 0f..5f) { renderBoxOnSwingFail }

    // Inventory
    private val simulateClosingInventory by boolean("SimulateClosingInventory", false) { !noInventoryAttack }
    private val noInventoryAttack by boolean("NoInvAttack", false)
    private val noInventoryDelay by int("NoInvDelay", 200, 0..500) { noInventoryAttack }
    private val noConsumeAttack by choices(
        "NoConsumeAttack", arrayOf("Off", "NoHits", "NoRotation"), "Off"
    ).subjective()

    // Visuals
    private val mark by choices("Mark", arrayOf("None", "Platform", "Box", "Circle"), "Circle").subjective()
    private val tracers by boolean("Tracers", false) { mark != "None" }.subjective()
    private val tracersColor by color("TracersColor", Color.CYAN) { tracers }.subjective()
    private val tracersWidth by float("TracersWidth", 2f, 0.5f..5f) { tracers }.subjective()
    private val displayAttackRangeCircle by boolean("DisplayAttackRangeCircle", false).subjective()
    private val attackRangeCircleColor by color(
        "AttackRangeCircleColor",
        Color.GREEN
    ) { displayAttackRangeCircle }.subjective()
    private val attackRangeCircleWidth by float(
        "AttackRangeCircleWidth",
        2f,
        0.5f..5f
    ) { displayAttackRangeCircle }.subjective()
    private val attackRangeCircleSegments by int(
        "AttackRangeCircleSegments",
        90,
        30..180
    ) { displayAttackRangeCircle }.subjective()

    private val fakeSharp by boolean("FakeSharp", true).subjective()
    private val renderAimPointBox by boolean("RenderAimPointBox", false).subjective()
    private val aimPointBoxColor by color("AimPointBoxColor", Color.CYAN) { renderAimPointBox }.subjective()
    private val aimPointBoxSize by float("AimPointBoxSize", 0.1f, 0f..0.2F) { renderAimPointBox }.subjective()

    // Circle options
    private val circleStartColor by color("CircleStartColor", Color.BLUE) { mark == "Circle" }.subjective()
    private val circleEndColor by color("CircleEndColor", Color.CYAN.withAlpha(0)) { mark == "Circle" }.subjective()
    private val fillInnerCircle by boolean("FillInnerCircle", false) { mark == "Circle" }.subjective()
    private val withHeight by boolean("WithHeight", true) { mark == "Circle" }.subjective()
    private val animateHeight by boolean("AnimateHeight", false) { withHeight }.subjective()
    private val heightRange by floatRange("HeightRange", 0.0f..0.4f, -2f..2f) { withHeight }.subjective()
    private val extraWidth by float("ExtraWidth", 0F, 0F..2F) { mark == "Circle" }.subjective()
    private val animateCircleY by boolean("AnimateCircleY", true) { fillInnerCircle || withHeight }.subjective()
    private val circleYRange by floatRange("CircleYRange", 0F..0.5F, 0F..2F) { animateCircleY }.subjective()
    private val duration by float(
        "Duration", 1.5F, 0.5F..3F, suffix = "Seconds"
    ) { animateCircleY || animateHeight }.subjective()

    // Box option
    private val boxOutline by boolean("Outline", true) { mark == "Box" }.subjective()
    private val Debugger by boolean("Debugger", false)
    private val outputMessage by multiChoices(
        "OutPutDebuggerMessageWhen",
        arrayOf("RealAttack", "FakeSwing"),
        arrayOf("RealAttack")
    ) { Debugger }

    /**
     * MODULE
     */

    // Target
    var target: EntityLivingBase? = null
    var realHitTarget: EntityLivingBase? = null
    private var hittable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0
    private var clicks = 0
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()
    private var swingDelay = 0

    // Container Delay
    private var containerOpen = -1L

    // Block status
    var renderBlocking = false
    var blockStatus = false
    private var blockStopInDead = false

    // BlocksMC
    private var asw = 0 // BlocksMC state
    private var blockTick: Int = 0
    private var blinking = false
    private var attack = 0

    // Force Block mode
    private var shouldBlock = false

    // Switch Delay
    private val switchTimer = MSTimer()

    // Blink AutoBlock
    var blinked = false
    val blinkedPackets: ArrayList<Packet<*>?> = ArrayList()

    // Swing fails
    private val swingFails = mutableListOf<SwingFailData>()

    // Regular AutoBlock timers
    private var blockCooldown = 0
    private var blockDuration = 0
    private var justStoppedBlocking = false // 新增：标记是否刚刚停止格挡

    private fun reset() {
        target = null
        hittable = false
        prevTargetEntities.clear()
        attackTickTimes.clear()
        attackTimer.reset()
        clicks = 0
        if (autoBlock == "BlocksMC" && blockStatus) {
            sendPacket(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 2) % 8))
            sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        }
        // Reset all mode-specific states
        asw = 0
        blockTick = 0
        attack = 0
        shouldBlock = false
        stopBlocking(true)
        if (blinkAutoBlock && blinked) {
            BlinkUtils.unblink()
            blinked = false
        }
        blinking = false
        blinked = false
        synchronized(swingFails) {
            swingFails.clear()
        }
        if (autoBlock == "Blink") {
            BlinkUtils.unblink()
        }

        // Reset Regular mode timers
        blockCooldown = 0
        blockDuration = 0
        justStoppedBlocking = false
    }

    private fun calculateAttackDelay(): Int {
        return when (attackDelayCalculationMode) {
            "CPS" -> {
                val c = getCurrentCPSRange()
                randomClickDelay(c.first, c.last, cpsMode)
            }
            "Delay" -> attackDelaySetting.random()
            else -> 50
        }
    }
    private fun calculateSwingDelay(): Int {
        return randomClickDelay(swingPerSecond.first,swingPerSecond.last,cpsMode)
    }
    init {
        updateCPS()
    }

    /**
     * Update CPS value based on Random CPS settings
     */
    private fun updateCPS() {
        currentCPS = if (!randomCPS) {
            cps.first
        } else {
            nextInt(randomCPSRange.first, randomCPSRange.last + 1)
        }
        cpsTimer.reset()
    }

    private fun getCurrentCPSRange(): IntRange {
        if (randomCPS && cpsTimer.hasTimePassed(1000L)) {
            updateCPS()
        }

        // 基础范围
        val baseRange = if (randomCPS) {
            randomCPSRange
        } else {
            cps
        }

        // 应用伤害加成
        return if (damageBoostClick && mc.thePlayer != null &&
            mc.thePlayer.hurtTime > damageBoostClickMinHurtTime) {
            val boostedFirst = (baseRange.first * damageBoostClickFactor).roundToInt()
            val boostedLast = (baseRange.last * damageBoostClickFactor).roundToInt()
            boostedFirst..boostedLast
        } else {
            baseRange
        }
    }
    /**
     * Disable kill aura module
     */
    override fun onToggle(state: Boolean) {
        target = null
        hittable = false
        prevTargetEntities.clear()
        attackTickTimes.clear()
        attackTimer.reset()
        clicks = 0

        cooldownTicks = 0
        cooldownAttacks = 0
        allowedAttacks = 0

        if (!state && options.pidResetOnDisable.get()) {
            options.resetPID()
        }

        if (!state) {
            options.slowPursuitLastAttackTime = null
            options.slowPursuitActive = false
        }

        if (state) {
            if (attackDelayCalculationMode == "CPS") {
                updateCPS()
            }
            attackDelay = calculateAttackDelay()
            swingDelay = calculateSwingDelay()
        }
        if (blinkAutoBlock) {
            BlinkUtils.unblink()
            blinked = false
        }

        if (autoF5) mc.gameSettings.thirdPersonView = 0

        stopBlocking(true)

        synchronized(swingFails) {
            swingFails.clear()
        }
        blockCooldown = 0
        blockDuration = 0
        justStoppedBlocking = false
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        update()
    }

    fun update() {
        if (cancelRun || (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay))) return

        // Update target
        updateTarget()

        if (autoF5) {
            if (mc.gameSettings.thirdPersonView != 1 && target != null) {
                mc.gameSettings.thirdPersonView = 1
            }
        }

        if (simulateCooldown && getAttackCooldownProgress() < 1f) {
            return
        }

        if (failSwing) {
            swingDelay = calculateSwingDelay()
        }

        attackDelay = calculateAttackDelay()
    }

    /**
     * Tick event
     */
    val onTick = handler<GameTickEvent>(priority = 2) {
        realHitTarget = null
        val player = mc.thePlayer ?: return@handler

        if (keepSprint && keepSprintMode == "Cooldown") {
            if (cooldownTicks > 0) {
                cooldownTicks--
                if (cooldownTicks <= 0) {
                    allowedAttacks = keepSprintDurationAttack
                }
            }
        }

        if (autoBlock == "Regular") {
            if (justStoppedBlocking) {
                blockCooldown = blockCooldownTick
                justStoppedBlocking = false
            }

            // 更新冷却计时器
            if (blockCooldown > 0) {
                blockCooldown--
            }

            // 更新格挡持续时间计时器
            if (blockDuration > 0) {
                blockDuration--
                if (blockDuration <= 0 && blockStatus) {
                    stopBlocking()
                    justStoppedBlocking = true
                }
            }
        }

        if (autoBlock == "Force") {
            handleForceBlock()
        }

        if (blockStatus && player.heldItem?.item !is ItemSword) {
            blockStatus = false
            renderBlocking = false
            return@handler
        }

        if (shouldPrioritize()) {
            target = null
            renderBlocking = false
            return@handler
        }

        if (clickOnly && !mc.gameSettings.keyBindAttack.isKeyDown) {
            clicks = 0
            return@handler
        }

        if (blockStatus && autoBlock == "Packet" && releaseAutoBlock && !ignoreTickRule) {
            if (!ignoreTickRule2) clicks = 0
            stopBlocking()
            return@handler
        }

        if (cancelRun) {
            target = null
            hittable = false
            stopBlocking()
            return@handler
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        if (simulateCooldown && getAttackCooldownProgress() < 1f) {
            return@handler
        }

        if (target == null && !blockStopInDead) {
            blockStopInDead = true
            stopBlocking()
            return@handler
        }
        when (autoBlock) {
            "BlocksMC" -> {
                handleBlocksMC()
                return@handler
            }
        }
        if (blinkAutoBlock) {
            when (player.ticksExisted % (blinkBlockTicks + 1)) {
                0 -> {
                    if (blockStatus && !blinked && !BlinkUtils.isBlinking) {
                        blinked = true
                    }
                }

                1 -> {
                    if (blockStatus && blinked && BlinkUtils.isBlinking) {
                        stopBlocking()
                    }
                }

                blinkBlockTicks -> {
                    if (!blockStatus && blinked && BlinkUtils.isBlinking) {
                        BlinkUtils.unblink()
                        blinked = false

                        startBlocking(target!!, interactAutoBlock, autoBlock == "Fake") // block again
                    }
                }
            }
        }

        if (target != null) {
            if (player.getDistanceToEntityBox(target!!) > blockMaxRange && blockStatus) {
                stopBlocking(true)
                return@handler
            } else {
                if (autoBlock != "Off" && !releaseAutoBlock) {
                    renderBlocking = if (forceBlockRender || autoBlock == "Fake") true else blockStatus
                }
            }

            // Usually when you butterfly click, you end up clicking two (and possibly more) times in a single tick.
            // Sometimes you also do not click. The positives outweigh the negatives, however.
            val extraClicks =
                if (simulateDoubleClicking && !simulateCooldown && nextInt(100) < simulateDoubleClickChance && mc.thePlayer.hurtTime in simulateDoubleClickHurtTime) nextInt(
                    -1,
                    1
                ) else 0

            // Generate clicks based on distance from us to target.
            val generatedClicks = if (generateClicksBasedOnDist) {
                val distance = player.getDistanceToEntityBox(target!!)
                ((distance / distanceFactor.random()) * cpsMultiplier.random()).roundToInt()
            } else 0

            var maxClicks = clicks + extraClicks + generatedClicks

            val prevHittable = hittable

            updateHittable()

            if (!prevHittable && hittable && maxClicks == 0 && forceFirstHit) {
                maxClicks++
            }

            repeat(maxClicks) {
                val wasBlocking = blockStatus

                runAttack(it == 0, it + 1 == maxClicks)
                clicks--

                if (wasBlocking && !blockStatus && (releaseAutoBlock && !ignoreTickRule || autoBlock == "Off")) {
                    return@handler
                }
            }
        } else {
            renderBlocking = false
        }
    }

    /**
     * Handle Force Block mode
     */
    private fun handleForceBlock() {
        val player = mc.thePlayer ?: return

        if (player.heldItem?.item is ItemSword && state && target != null && getForceBlockScaffoldState() &&
            SilentHotbar.getSilentSlotItemType() is ItemSword
        ) {
            shouldBlock = true
            sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
            mc.gameSettings.keyBindUseItem.pressed = true
            blockStatus = true
            renderBlocking = true
        } else if (shouldBlock) {
            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            mc.gameSettings.keyBindUseItem.pressed = false
            shouldBlock = false
            blockStatus = false
            renderBlocking = false
        }
    }

    /**
     * Check scaffold state for Force Block mode
     */
    private fun getForceBlockScaffoldState(): Boolean {
        if (!notOnScaffolding) return true
        return !(Scaffold.handleEvents() || net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold2.handleEvents())
    }

    /**
     * Attack Event
     */
    val onAttack = handler<AttackEvent> { e ->
        if (handleEvents()) realHitTarget = e.targetEntity as? EntityLivingBase
    }

    /**
     * Render event
     */
    val onRender3D = handler<Render3DEvent> {
        handleFailedSwings()

        drawAimPointBox()

        if (cancelRun) {
            target = null
            hittable = false
            return@handler
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }
        if (displayAttackRangeCircle) {
            val player = mc.thePlayer ?: return@handler
            val renderManager = mc.renderManager

            val center = player.interpolatedPosition(player.prevPos) - renderManager.renderPos

            RenderUtils.drawCircle3D(
                center.xCoord, center.yCoord, center.zCoord,
                range.toDouble(), attackRangeCircleColor.rgb,
                attackRangeCircleWidth, attackRangeCircleSegments
            )
        }

        target ?: return@handler

        if (attackTimer.hasTimePassed(attackDelay)) {
            if ((attackDelayCalculationMode == "CPS" && cps.last > 0) || attackDelayCalculationMode == "Delay") {
                clicks++
            }
            attackTimer.reset()
            attackDelay = calculateAttackDelay()
        }

        val hittableColor = if (hittable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70)

        if (targetMode != "Multi") {
            when (mark.lowercase()) {
                "none" -> return@handler
                "platform" -> drawPlatform(target!!, hittableColor)
                "box" -> drawEntityBox(target!!, hittableColor, boxOutline)
                "circle" -> drawCircle(
                    target!!,
                    duration * 1000F,
                    heightRange.takeIf { animateHeight } ?: heightRange.endInclusive..heightRange.endInclusive,
                    extraWidth,
                    fillInnerCircle,
                    withHeight,
                    circleYRange.takeIf { animateCircleY },
                    circleStartColor.rgb,
                    circleEndColor.rgb
                )
            }
        }
        fun calculateAimPoint(): Vec3? {
            val player = mc.thePlayer ?: return null
            val target = this.target ?: return null

            return runWithSimulatedPosition(player, player.interpolatedPosition(player.prevPos)) {
                runWithSimulatedPosition(target, target.interpolatedPosition(target.prevPos)) {
                    val rotation =
                        serverRotation.lerpWith(currentRotation ?: player.rotation, mc.timer.renderPartialTicks)
                    val rotationVec = player.eyes + getVectorForRotation(rotation) *
                            player.getDistanceToEntityBox(target).coerceAtMost(range.toDouble())
                    rotationVec
                }
            }
        }
        if (tracers) {
            val player = mc.thePlayer ?: return@handler
            if (target == null) return@handler
            val renderManager = mc.renderManager
            // 玩家眼睛位置
            val eyePos = player.getPositionEyes(mc.timer.renderPartialTicks).subtract(
                renderManager.renderPosX,
                renderManager.renderPosY,
                renderManager.renderPosZ
            )

            // 直接使用AimBox的算法获取瞄准点
            val aimPos = calculateAimPoint()?.subtract(renderManager.renderPos) ?: return@handler

            RenderUtils.drawLine3D(
                eyePos.xCoord, eyePos.yCoord, eyePos.zCoord,
                aimPos.xCoord, aimPos.yCoord, aimPos.zCoord,
                tracersColor.rgb, tracersWidth
            )
        }
    }
    private val swingTimer = MSTimer()

    /**
     * Attack enemy
     */
    private fun runAttack(isFirstClick: Boolean, isLastClick: Boolean) {
        val currentTarget = this.target ?: return

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (noConsumeAttack == "NoHits" && isConsumingItem()) {
            return
        }

        // Settings
        val multi = targetMode == "Multi"
        val manipulateInventory = simulateClosingInventory && !noInventoryAttack && serverOpenInventory

        if (hittable && !(currentTarget.hurtTime in targetHurtTime || mc.thePlayer.hurtTime in ownHurtTime)) {
            return
        }

        if (!hittable && options.rotationsActive) {
            if (swing && failSwing) {
                val distanceToTarget = smartGetDistanceTo(currentTarget)
                if (distanceToTarget !in swingRange) {
                    return
                }
                if (swingTimer.hasTimePassed(swingDelay)) {
                    swingTimer.reset()

                    val rotation = currentRotation ?: player.rotation

                    if (rotationDifference(rotation) > maxRotationDifferenceToSwing) {
                        val shouldIgnore = swingWhenTicksLate.isActive() && ticksSinceClick() >= ticksLateToSwing

                        if (!shouldIgnore) {
                            return
                        }
                    }

                    runWithModifiedRaycastResult(rotation, range.toDouble(), throughWallsRange.toDouble()) {
                        if (swingOnlyInAir && !it.typeOfHit.isMiss) {
                            return@runWithModifiedRaycastResult
                        }

                        if (respectMissCooldown && ticksSinceClick() <= 1 && it.typeOfHit.isMiss) {
                            return@runWithModifiedRaycastResult
                        }

                        val shouldEnterBlockBreakProgress =
                            !shouldDelayClick(it.typeOfHit) || attackTickTimes.lastOrNull()?.first?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK

                        if (shouldEnterBlockBreakProgress) {
                            if (manipulateInventory && isFirstClick) serverOpenInventory = false
                        }

                        val prevCooldown = mc.leftClickCounter

                        val isAnyClientGuiActive = mc.currentScreen?.javaClass?.`package`?.name?.contains(
                            FireBounce.CLIENT_NAME, ignoreCase = true
                        ) == true

                        if (isAnyClientGuiActive) {
                            mc.leftClickCounter = 0
                        }

                        if (!shouldDelayClick(it.typeOfHit)) {
                            attackTickTimes += it to runTimeTicks

                            if (it.typeOfHit.isEntity) {
                                val entity = it.entityHit

                                if (entity is EntityLivingBase && isSelectedWithTargetLocker(entity, true)) {
                                    attackEntity(entity, isLastClick)
                                } else attackTickTimes -= it to runTimeTicks
                            } else {
                                // ========== NO SWING ONLY CLIENT SIDE IMPLEMENTATION ==========
                                if (noSwingOnlyClientSide) {
                                    // 只发送包给服务器，不在客户端显示挥动动画
                                    mc.netHandler.addToSendQueue(C0APacketAnimation())

                                    // 重置冷却计数器（如果需要）
                                    if (isAnyClientGuiActive) {
                                        mc.leftClickCounter = prevCooldown
                                    }
                                    CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
                                } else {
                                    // 正常挥动（客户端和服务器都能看到）
                                    mc.clickMouse()
                                    CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
                                }
                                // ========== END NO SWING ONLY CLIENT SIDE IMPLEMENTATION ==========
                                val leftClickCount = CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)
                                val distance = mc.thePlayer.getTrueDistanceTo(target!!)
                                val formattedDistance = BigDecimal(distance.toString())
                                    .setScale(2, RoundingMode.DOWN)
                                    .toDouble()
                                debugMessage("SwingRange: $formattedDistance | CPS: $leftClickCount", 1)

                                if (renderBoxOnSwingFail) {
                                    synchronized(swingFails) {
                                        val centerDistance = (currentTarget.hitBox.center - player.eyes).lengthVector()
                                        val spot = player.eyes + getVectorForRotation(rotation) * centerDistance

                                        swingFails += SwingFailData(spot, System.currentTimeMillis())
                                    }
                                }
                            }
                        }

                        if (shouldEnterBlockBreakProgress && isLastClick) {
                            mc.sendClickBlockToController(true)

                            nextTick {
                                mc.sendClickBlockToController(false)

                                if (!ignoreTickRule2) clicks = 0

                                if (manipulateInventory) serverOpenInventory = true
                            }
                        }

                        if (isAnyClientGuiActive) {
                            mc.leftClickCounter = prevCooldown
                        }
                    }
                }
            }

            return
        }

        // Close inventory when open
        if (manipulateInventory && isFirstClick) serverOpenInventory = false

        blockStopInDead = false

        if (!multi) {
            attackEntity(currentTarget, isLastClick)
        } else {
            var targets = 0
            val serverViewRotation = Rotation(player.rotationYaw, player.rotationPitch)  // 服务器视角

            for (entity in world.loadedEntityList) {
                val distance = player.getDistanceToEntityBox(entity)

                if (entity is EntityLivingBase && isSelectedWithTargetLocker(entity, true) && distance <= getRange(
                        entity
                    )
                ) {
                    if (maxMultiRotationDifference > 0 && options.rotationsActive &&
                        !(lockTarget && lockTargetName.isNotBlank() && entity.name == lockTargetName)
                    ) {
                        val rotationToEntity = toRotation(entity.hitBox.center, true, player)
                        val rotationDiff = rotationDifference(rotationToEntity, serverViewRotation)

                        if (rotationDiff > maxMultiRotationDifference) {
                            continue
                        }
                    }

                    attackEntity(entity, isLastClick)
                    targets += 1

                    if (limitedMultiTargets != 0 && limitedMultiTargets <= targets) break
                }
            }
        }

        if (!isLastClick) return

        val switchMode = targetMode == "Switch"

        if (!switchMode || switchTimer.hasTimePassed(switchDelay)) {
            prevTargetEntities += currentTarget.entityId

            if (switchMode) {
                switchTimer.reset()
            }
        }

        // Open inventory
        if (manipulateInventory) serverOpenInventory = true
    }

    /**
     * Update current target
     */
    /**
     * Update current target
     */
    /**
     * Update current target
     */
    private fun updateTarget() {
        if (shouldPrioritize()) return

        // Reset fixed target to null
        target = null

        val switchMode = targetMode == "Switch"

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        var bestTarget: EntityLivingBase? = null
        var bestValue: Double? = null

        // TargetLocker 功能：优先检查名称匹配的目标
        var foundLockTarget: EntityLivingBase? = null

        if (lockTarget && lockTargetName.isNotBlank()) {
            for (entity in theWorld.loadedEntityList) {
                if (entity !is EntityLivingBase || !isSelected(entity, false)) continue

                // 检查名称匹配（区分大小写）
                if (entity.name == lockTargetName) {
                    val distance =
                        Backtrack.runWithNearestTrackedDistance(entity) { thePlayer.getDistanceToEntityBox(entity) }
                    val entityFov = rotationDifference(entity)

                    // 检查距离和FOV条件
                    if (distance <= maxRange && (fov == 180F || entityFov <= fov)) {
                        if (switchMode && entity.entityId in prevTargetEntities) continue

                        if (switchMode && !isLookingOnEntities(entity, maxSwitchFOV.toDouble())) continue

                        if (Backtrack.runWithNearestTrackedDistance(entity) { updateRotations(entity) }) {
                            foundLockTarget = entity
                            break
                        }
                    }
                }
            }
        }

        // 如果找到了锁定的目标，直接设置为当前目标
        if (foundLockTarget != null) {
            target = foundLockTarget
            return
        }

        // 如果没有找到锁定的目标，或者锁定的目标不在范围内，继续按照正常逻辑搜索
        for (entity in theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isSelected(
                    entity, true
                ) || switchMode && entity.entityId in prevTargetEntities
            ) continue

            val distance = Backtrack.runWithNearestTrackedDistance(entity) { thePlayer.getDistanceToEntityBox(entity) }

            if (switchMode && distance > range && prevTargetEntities.isNotEmpty()) continue

            val entityFov = rotationDifference(entity)

            if (distance > maxRange || fov != 180F && entityFov > fov) continue

            if (switchMode && !isLookingOnEntities(entity, maxSwitchFOV.toDouble())) continue

            val currentValue = when (priority.lowercase()) {
                "distance" -> distance
                "direction" -> entityFov.toDouble()
                "health" -> entity.health.toDouble()
                "livingtime" -> -entity.ticksExisted.toDouble()
                "armor" -> entity.totalArmorValue.toDouble()
                "hurtresistance" -> entity.hurtResistantTime.toDouble()
                "hurttime" -> entity.hurtTime.toDouble()
                "healthabsorption" -> (entity.health + entity.absorptionAmount).toDouble()
                "regenamplifier" -> if (entity.isPotionActive(Potion.regeneration)) {
                    entity.getActivePotionEffect(Potion.regeneration).amplifier.toDouble()
                } else -1.0

                "inweb" -> if (entity.isInWeb) -1.0 else Double.MAX_VALUE
                "onladder" -> if (entity.isOnLadder) -1.0 else Double.MAX_VALUE
                "inliquid" -> if (entity.isInWater || entity.isInLava) -1.0 else Double.MAX_VALUE
                else -> null
            } ?: continue

            if (bestValue == null || currentValue < bestValue) {
                bestValue = currentValue
                bestTarget = entity
            }
        }

        if (bestTarget != null) {
            if (Backtrack.runWithNearestTrackedDistance(bestTarget) { updateRotations(bestTarget) }) {
                target = bestTarget
                return
            }
        }

        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase, isLastClick: Boolean) {
        val thePlayer = mc.thePlayer ?: return
        if (shouldPrioritize()) return

        options.slowPursuitLastAttackTime = System.currentTimeMillis()
        options.slowPursuitActive = true

        if (thePlayer.isBlocking && (autoBlock == "Off" && blockStatus || autoBlock == "Packet" && releaseAutoBlock)) {
            stopBlocking()
            if (!ignoreTickRule || autoBlock == "Off") {
                return
            }
        }

        if (shouldDelayClick(MovingObjectPosition.MovingObjectType.ENTITY)) {
            return
        }

        if (keepSprint && keepSprintMode == "Cooldown") {
            if (allowedAttacks > 0) {
                allowedAttacks--
            } else {
                when (keepSprintCooldownMode) {
                    "Tick" -> cooldownTicks = keepSprintCooldownTick
                    "AttackCount" -> cooldownAttacks = keepSprintCooldownAttack
                }
            }

            if ((keepSprintCooldownMode == "Tick" && cooldownTicks <= 0) ||
                (keepSprintCooldownMode == "AttackCount" && cooldownAttacks <= 0)
            ) {
                allowedAttacks = keepSprintDurationAttack
            }
        }

        if (!blinkAutoBlock || !BlinkUtils.isBlinking) {
            val affectSprint = false.takeIf { shouldKeepSprint }

            thePlayer.attackEntityWithModifiedSprint(entity, affectSprint) {
                if (swing) {
                    if (noSwingOnlyClientSide && !hideSwingOnlyFakeSwing) {
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                    } else {
                        thePlayer.swingItem()
                    }
                }
            }

            if (EnchantmentHelper.getModifierForCreature(
                    thePlayer.heldItem, entity.creatureAttribute
                ) <= 0F && fakeSharp
            ) {
                thePlayer.onEnchantmentCritical(entity)
            }

            if (Debugger && "RealAttack" in outputMessage) {
                val leftClickCount = CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)
                val distance = thePlayer.getTrueDistanceTo(entity)
                val formattedDistance = BigDecimal(distance.toString())
                    .setScale(2, RoundingMode.DOWN)
                    .toDouble()
                debugMessage("AttackRange: $formattedDistance | CPS: $leftClickCount", 2)
            }
        }

        if (autoBlock == "Regular") {
            if (blockCooldown <= 0 && canBlock && thePlayer.hurtResistantTime <= 0) {
                startBlocking(entity, interactAutoBlock, false)
                blockDuration = blockDurationTick
                justStoppedBlocking = false
            }
        } else if (autoBlock != "Off" && (thePlayer.isBlocking || canBlock) &&
            (!blinkAutoBlock && isLastClick || blinkAutoBlock && (!blinked || !BlinkUtils.isBlinking))
        ) {
            // 其他模式的自动格挡逻辑保持不变
            startBlocking(entity, interactAutoBlock, autoBlock == "Fake")
        }

        resetLastAttackedTicks()
    }

    /**
     * Update rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        if (mc.thePlayer == null) return false
        if (shouldPrioritize()) return false

        val prediction = entity.currPos.subtract(entity.prevPos).times(2 + predictEnemyPosition.toDouble())
        val boundingBox = entity.hitBox.offset(prediction)

        val rotation = RotationUtils.searchCenter(
            boundingBox,
            generateSpotBasedOnDistance,
            outBorder && !attackTimer.hasTimePassed(attackDelay / 2),
            randomization,
            predict = false,
            lookRange = rotationRange,
            attackRange = range,
            throughWallsRange = max(rotationThroughWallsRange, throughWallsRange),
            bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
            horizontalSearch = horizontalBodySearchRange
        ) ?: return false

        RotationUtils.setTargetRotation(rotation, options = options)

        return true
    }
    private fun ticksSinceClick() = runTimeTicks - (attackTickTimes.lastOrNull()?.second ?: 0)

    /**
     * Check if enemy is hittable with current rotations
     */
    private fun updateHittable() {
        val eyes = mc.thePlayer.eyes

        val currentRotation = currentRotation ?: mc.thePlayer.rotation
        val target = this.target ?: return
        val distanceToTarget = smartGetDistanceTo(target)
        if (shouldPrioritize()) return

        if (!options.rotationsActive) {
            hittable = distanceToTarget <= range
            return
        }

        var chosenEntity: Entity? = null

        if (raycast) {
            chosenEntity = raycastEntity(
                range.toDouble(), currentRotation.yaw, currentRotation.pitch
            ) { entity -> !livingRaycast || entity is EntityLivingBase && entity !is EntityArmorStand }

            if (chosenEntity != null && chosenEntity is EntityLivingBase && (NoFriends.handleEvents() || !(chosenEntity is EntityPlayer && chosenEntity.isClientFriend()))) {
                if (raycastIgnored && target != chosenEntity) {
                    this.target = chosenEntity
                }
            }

            hittable = this.target == chosenEntity
        } else {
            hittable = isRotationFaced(target, range.toDouble(), currentRotation)
        }

        var shouldExcept = false

        chosenEntity ?: this.target?.run {
            if (ForwardTrack.handleEvents()) {
                ForwardTrack.includeEntityTruePos(this) {
                    checkIfAimingAtBox(this, currentRotation, eyes, onSuccess = {
                        hittable = true

                        shouldExcept = true
                    })
                }
            }
        }

        if (!hittable || shouldExcept) {
            return
        }

        val targetToCheck = chosenEntity ?: this.target ?: return

        // If player is inside entity, automatic yes because the intercept below cannot check for that
        // Minecraft does the same, see #EntityRenderer line 353
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            return
        }

        var checkNormally = true

        if (Backtrack.handleEvents()) {
            Backtrack.loopThroughBacktrackData(targetToCheck) {
                var result = false

                checkIfAimingAtBox(targetToCheck, currentRotation, eyes, onSuccess = {
                    checkNormally = false

                    result = true
                }, onFail = {
                    result = false
                })

                return@loopThroughBacktrackData result
            }
        } else if (ForwardTrack.handleEvents()) {
            ForwardTrack.includeEntityTruePos(targetToCheck) {
                checkIfAimingAtBox(targetToCheck, currentRotation, eyes, onSuccess = { checkNormally = false })
            }
        }

        if (!checkNormally) {
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        // Is the entity box raycast vector visible? If not, check through-wall range
        hittable =
            isVisible(intercept.hitVec) || smartGetDistanceTo(targetToCheck) <= throughWallsRange
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: Entity, interact: Boolean, fake: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (blockStatus && (!uncpAutoBlock || !blinkAutoBlock) || shouldPrioritize()) return

        if (mc.thePlayer.isBlocking) {
            blockStatus = true
            renderBlocking = true
            return
        }

        if (unblockMode == "Empty" && player.inventory.firstEmptyStack !in 0..8) {
            return
        }

        if (!fake) {
            if (!(blockRate > 0 && nextInt(endExclusive = 100) <= blockRate)) return

            if (interact) {
                val positionEye = player.eyes

                val boundingBox = interactEntity.hitBox

                val (yaw, pitch) = currentRotation ?: player.rotation

                val vec = getVectorForRotation(Rotation(yaw, pitch))

                val lookAt = positionEye.add(vec * maxRange.toDouble())

                val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
                val hitVec = movingObject.hitVec

                sendPackets(
                    C02PacketUseEntity(interactEntity, hitVec - interactEntity.positionVector),
                    C02PacketUseEntity(interactEntity, INTERACT)
                )

            }
            if (switchStartBlock) {
                switchToSlot((SilentHotbar.currentSlot + 1) % 9)
            }

            sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
            blockStatus = true
        }

        renderBlocking = true

        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking(forceStop: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (!forceStop) {
            if (blockStatus && !mc.thePlayer.isBlocking) {

                when (unblockMode.lowercase()) {
                    "stop" -> {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    }

                    "switch" -> {
                        switchToSlot((SilentHotbar.currentSlot + 1) % 9)
                    }

                    "empty" -> {
                        player.inventory.firstEmptyStack.takeIf { it in 0..8 }.let {
                            if (it == null) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                                return@let
                            }

                            switchToSlot(it)
                        }
                    }
                }

                blockStatus = false
                renderBlocking = false
            }
        } else {
            if (autoBlock == "Regular") {
                justStoppedBlocking = true
            }
            if (blockStatus) {
                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            }

            blockStatus = false
            renderBlocking = false
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet
        val isClientPacket = packet.javaClass.name.startsWith("net.minecraft.network.play.client.")
        if (autoBlock == "Blink") {
            if (mc.thePlayer == null || mc.thePlayer.isDead) {
                BlinkUtils.unblink()
                return@handler
            }
            BlinkUtils.blink(packet, event)
        }

        if (blinking && target != null && isClientPacket) {
            blinkedPackets.add(packet)
            event.cancelEvent()
        }
        //here up
        if (autoBlock == "Off" || !blinkAutoBlock || !blinked) return@handler

        if (player.isDead || player.ticksExisted < 20) {
            BlinkUtils.unblink()
            return@handler
        }

        if (Blink.blinkingSend() || Blink.blinkingReceive()) {
            BlinkUtils.unblink()
            return@handler
        }

        BlinkUtils.blink(packet, event)
        if (autoBlock == "Off" || !blinkAutoBlock || !blinked) return@handler

        if (player.isDead || player.ticksExisted < 20) {
            BlinkUtils.unblink()
            return@handler
        }

        if (Blink.blinkingSend() || Blink.blinkingReceive()) {
            BlinkUtils.unblink()
            return@handler
        }

        BlinkUtils.blink(packet, event)
    }

    /**
     * Checks if raycast landed on a different object
     *
     * The game requires at least 1 tick of cool-down on raycast object type change (miss, block, entity)
     * We are doing the same thing here but allow more cool-down.
     */
    private fun shouldDelayClick(currentType: MovingObjectPosition.MovingObjectType): Boolean {
        if (!useHitDelay) {
            return false
        }

        val lastAttack = attackTickTimes.lastOrNull()

        return lastAttack != null && lastAttack.first.typeOfHit != currentType && runTimeTicks - lastAttack.second <= hitDelayTicks
    }

    fun checkIfAimingAtBox(
        targetToCheck: Entity, currentRotation: Rotation, eyes: Vec3, onSuccess: () -> Unit,
        onFail: () -> Unit = { },
    ) {
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            onSuccess()
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        if (intercept != null) {
            // Is the entity box raycast vector visible? If not, check through-wall range
            hittable =
                isVisible(intercept.hitVec) || smartGetDistanceTo(targetToCheck) <= throughWallsRange

            if (hittable) {
                onSuccess()
                return
            }
        }

        onFail()
    }

    private fun switchToSlot(slot: Int) {
        SilentHotbar.selectSlotSilently(this, slot, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }

    private fun shouldPrioritize(): Boolean = when {
        !onScaffold && (Scaffold.handleEvents() && (Scaffold.placeRotation != null || currentRotation != null) || Tower.handleEvents() && Tower.isTowering) -> true

        !onDestroyBlock && (Fucker.handleEvents() && !Fucker.noHit && Fucker.pos != null && !Fucker.isOwnBed || Nuker.handleEvents()) -> true

        activationSlot && SilentHotbar.currentSlot != preferredSlot - 1 -> true

        else -> false
    }

    private fun handleFailedSwings() {
        if (!renderBoxOnSwingFail) return

        val box = AxisAlignedBB(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        synchronized(swingFails) {
            val fadeSeconds = renderBoxFadeSeconds * 1000L
            val colorSettings = renderBoxColor

            val renderManager = mc.renderManager

            swingFails.removeAll {
                val timestamp = System.currentTimeMillis() - it.startTime
                val transparency = (0f..255f).lerpWith(1 - (timestamp / fadeSeconds).coerceAtMost(1.0F))

                val offsetBox = box.offset(it.vec3 - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offsetBox, colorSettings.color(a = transparency.roundToInt()))

                timestamp > fadeSeconds
            }
        }
    }

    private fun drawAimPointBox() {
        val player = mc.thePlayer ?: return
        val target = this.target ?: return

        if (!renderAimPointBox) {
            return
        }

        val f = aimPointBoxSize.toDouble()

        val box = AxisAlignedBB(0.0, 0.0, 0.0, f, f, f)

        val renderManager = mc.renderManager

        runWithSimulatedPosition(player, player.interpolatedPosition(player.prevPos)) {
            runWithSimulatedPosition(target, target.interpolatedPosition(target.prevPos)) {
                val rotationVec = player.eyes + getVectorForRotation(
                    serverRotation.lerpWith(currentRotation ?: player.rotation, mc.timer.renderPartialTicks)
                ) * player.getDistanceToEntityBox(target).coerceAtMost(range.toDouble())

                val offSetBox = box.offset(rotationVec - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offSetBox, aimPointBoxColor)
            }
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun
        inline get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer) || noConsumeAttack == "NoRotation" && isConsumingItem()

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            if (target != null && player.heldItem?.item is ItemSword) {
                if (autoBlock == "Regular") {
                    if (blockCooldown > 0) return false

                    if (player.hurtResistantTime > 0) return false
                }

                if (smartAutoBlock) {
                    if (player.isMoving && forceBlock) return false

                    if (checkWeapon && target?.heldItem?.item !is ItemSword && target?.heldItem?.item !is ItemAxe) return false

                    if (player.hurtTime !in maxOwnHurtTime) return false

                    if (target!!.hurtTime !in smartBlockTargetHurtTimeRange) return false

                    val rotationToPlayer = toRotation(player.hitBox.center, true, target!!)

                    if (rotationDifference(rotationToPlayer, target!!.rotation) > maxDirectionDiff) return false

                    if (target!!.swingProgressInt > maxSwingProgress) return false

                    if (target!!.getDistanceToEntityBox(player) > blockRange) return false
                }

                if (player.getDistanceToEntityBox(target!!) > blockMaxRange) return false

                return true
            }

            return false
        }

    /**
     * Range
     */
    private val maxRange
        get() = max(rotationRange, throughWallsRange)

    private fun getRange(entity: Entity) =
        (if (smartGetDistanceTo(entity) >= throughWallsRange) rotationRange else throughWallsRange) - if (mc.thePlayer.isSprinting) rangeSprintReduction else 0F


    /**
     * TargetCount
     */
    fun getTargetCount(): Int {
        if (shouldPrioritize()) return 0

        val world = mc.theWorld ?: return 0
        val player = mc.thePlayer ?: return 0

        var count = 0

        for (entity in world.loadedEntityList) {
            if (entity !is EntityLivingBase || !isSelectedWithTargetLocker(entity, true)) continue

            val distance = player.getDistanceToEntityBox(entity)
            val entityFov = rotationDifference(entity)

            if (distance > maxRange || (fov != 180F && entityFov > fov)) continue

            count++
        }

        return count
    }

    override fun onDisable() {
        reset()
        if (autoF5) {
            mc.gameSettings.thirdPersonView = 0
        }
        blinkedPackets.clear()
        blockCooldown = 0
        blockDuration = 0
        justStoppedBlocking = false
    }

    /**
     * HUD Tag
     */
    override val tag: String?
        get() = when (TagMode.lowercase()) {
            "targetmode+targetcount" -> "$targetMode-${getTargetCount()}"
            "targetmode" -> targetMode
            "targetname" -> target?.name ?: ""
            "targetcount" -> "${getTargetCount()}"
            "customtext" -> customTagText
            else -> "$targetMode->$autoBlock"
        }

    val isBlockingChestAura
        get() = handleEvents() && target != null


    @Suppress("SameParameterValue")
    private fun isSelectedWithTargetLocker(entity: EntityLivingBase, checkTeam: Boolean): Boolean {
        if (lockTarget && lockTargetName.isNotBlank()) {
            return entity.name == lockTargetName
        }

        return isSelected(entity, checkTeam)
    }

    private fun handleBlocksMC() {
        updateHittable()

        val player = mc.thePlayer ?: return
        val currentTarget = target ?: return

        asw++
        when (asw) {
            1 -> {
                val attackRangeCheck = player.getDistanceToEntityBox(currentTarget) <= (if (blinking) range else 3.0F)

                if (attackRangeCheck && hittable) {
                    attackEntityDirectly(currentTarget)
                    attack++
                } else {
                    attack = 0
                    player.swingItem()
                }

                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
                blockStatus = true
                renderBlocking = true

                blinking = false
                releaseBlinkedPackets()
            }

            2 -> {
                if (attack % 3 == 0) {
                    blinking = true
                    sendPacket(C09PacketHeldItemChange((player.inventory.currentItem + 2) % 8))
                    sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, player.heldItem, 0f, 0f, 0f))
                    asw = 0
                } else if (attack % 6 == 1) {
                    blinking = true
                    sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    blockStatus = false
                    renderBlocking = false
                    asw = 0
                }
            }

            3 -> {
                blinking = true
                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                blockStatus = false
                renderBlocking = false
                asw = 0
            }
        }
    }

    private fun attackEntityDirectly(entity: EntityLivingBase, interact: Boolean = true) {
        mc.thePlayer.swingItem()
        mc.playerController.attackEntity(mc.thePlayer, entity)
        if (interact) {
            sendPacket(C02PacketUseEntity(entity, INTERACT))
        }
        resetLastAttackedTicks()
    }

    private fun releaseBlinkedPackets() {
        if (blinkedPackets.isNotEmpty()) {
            val packetsToRelease = blinkedPackets.toList()
            blinkedPackets.clear()
            packetsToRelease.forEach { packet ->
                packet?.let { sendPacket(it) }
            }
        }
        blinking = false
    }

    private fun debugMessage(c: Any, type: Int) {
        if (!Debugger) return
        val msg = c.toString()
        when (type) {
            1 -> {
                if ("FakeSwing" !in outputMessage) return
            }

            2 -> {
                if ("RealAttack" !in outputMessage) return
            }
        }
        chat(msg)
    }

    private fun predictPos(ticks: Int): Float {
        val target = target as EntityLivingBase
        return sqrt(
            (((target.posX + target.motionX * ticks) - mc.thePlayer.posX).pow(2f) + ((target.posY + target.motionY * ticks) - mc.thePlayer.posY).pow(
                2f
            ) + ((target.posZ + target.motionZ * ticks) - mc.thePlayer.posZ).pow(2f)).toFloat()
        )
    }

    fun smartGetDistanceTo(entity: Entity): Double {
        return when (rangeCalculateMode) {
            "Client-Side" -> mc.thePlayer.getDistanceToEntityBox(entity)
            "Server-Side" -> mc.thePlayer.getTrueDistanceTo(entity)
            else -> 0.0
        }
    }
}
data class SwingFailData(val vec3: Vec3, val startTime: Long)
fun EntityPlayer.getTrueDistanceTo(entity: Entity): Double {
    return if (entity is IMixinEntity && entity.truePos) {
        getDistance(entity.trueX, entity.trueY, entity.trueZ)
    } else {
        getDistanceToEntityBox(entity)
    }
}