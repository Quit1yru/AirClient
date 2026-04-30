/**
 * @author _0x16z
 * Hard to understand
 * But it works well :D
 */

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.potion.Potion
import net.minecraft.util.MathHelper
import kotlin.random.Random

object AdvancedJumpReset : Module("AdvancedJumpReset", Category.COMBAT) {
    private val jumpRate by choices(
        "How to calculate jump rate",
        arrayOf(
            "Tick since last velocity",
            "Simple RNG",
            "Polar Safe RNG"
        ),
        value = "Simple RNG"
    )
    private val minTickSinceLastVL by int("Min ticks since last jump reset", 3, 0..20, "ticks") { jumpRate == "Tick since last velocity" }
    private val simpleRNG by int("Simple RNG jump rate", 75, 0..100, "%") { jumpRate == "Simple RNG" }

    private val pauseWhen by choices("Pause jump reset when",
        arrayOf(
            "Server flag packet received",
            "Not in combat",
            "Both"
        ),
        value = "Server flag packet received"
    )
    private val serverLagTick by int("Min Tick since server lag packet received", 3, 0..20) { pauseWhen.contains("Server flag packet received") }
    private val notInCombatTick by int("Min Tick since not in combat", 3, 0..20) { pauseWhen.contains("Not in combat") }

    private val hurtTimeRange by intRange("Jump reset if hurt time in", 5..9, 1..10)

    private val howToJump by choices("How to jump", arrayOf("Functional", "Legitimize", "Motion"), value = "Functional")
    private val motionHeight by float("Motion height", 0.42f, 0.1f..1f) { howToJump == "Motion" }

    private val reduce by boolean("Reduce", false)
    private val reduceEvent by choices("Reduce when what happened", arrayOf("Jumped", "Hurt time updated"), value = "Hurt time updated") { reduce }
    private val reduceMode by choices("Reduce calculation", arrayOf("Linear", "Smooth"), value = "Linear") { reduce }
    private val reduceHurtTime by intRange("Reduce hurt time by", 1..3, 1..10) { reduce && reduceEvent == "Hurt time updated" }
    private val reduceFactor by float("Basic reduce factor", 0.6f, 0f..1f) { reduce }
    private val reduceFactorWhileHit by float("Reduce factor while hitting", 0.6f, 0f..1f) { reduce }
    private val reduceFactorWhileSprint by float("Reduce factor while sprinting", 0.6f, 0f..1f) { reduce }
    private val reduceFactorWhileHitSprint by float("Reduce factor while hitting and sprinting", 0.6f, 0f..1f) { reduce }

    private val activeMotion by intRange("Active motion", 700..7000, 300..32000)
    private val stopWhenBackward by boolean("Stop when S Pressed", true)
    private val stopWhenBlocking by boolean("Stop when Blocking", true)
    private val stopWhenSneaking by boolean("Stop when Sneaking", true)
    private val stopWhenFire by boolean("Stop when on fire", true)
    private val stopWhenSpeed by boolean("Stop when Speed potion", false)
    private val stopWhenJumpBoost by boolean("Stop when Jump Boost", false)
    private val stopWhenInInventory by boolean("Stop when in inventory", true)
    private val stopWhenBadSurrounding by boolean("Stop when in bad surrounding", true)
    private val stopWhenInAir by boolean("Stop when in air", true)

    private var tickSinceLastVelocity = 0
    private var tickSinceLastAttack = 0
    private var tickSinceLastFlag = 0
    private var shouldJump = false
    private var lastHurtTime = 0
    private var lastVelocitySize = 0
    private fun onHurtTimeUpdate(){
        val player = mc.thePlayer ?: return
        if(shouldReduce()&&reduceEvent=="Hurt time updated")doReduce()
        if (player.hurtTime in hurtTimeRange&&canJump(lastVelocitySize)&&shouldJump) {
            doJump()
            if(shouldReduce()&&reduceEvent=="Jumped")doReduce()
            tickSinceLastVelocity=0
            shouldJump=false
        }
    }
    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        if (player.hurtTime != lastHurtTime) {
            lastHurtTime = player.hurtTime
            onHurtTimeUpdate()
        }
    }
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if(packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            shouldJump = true
            lastVelocitySize = MathHelper.sqrt_double((packet.motionX * packet.motionX + packet.motionZ * packet.motionZ).toDouble()).toInt()
            return@handler
        }
        if(packet is S08PacketPlayerPosLook) {
            tickSinceLastFlag = 0
            return@handler
        }
    }
    val onAttack = handler<AttackEvent> {
        tickSinceLastAttack = 0
    }
    private fun doReduce() {
        if(!reduce)return
        val player = mc.thePlayer ?: return
        if (player.hurtTime in reduceHurtTime) {
            var original = reduceFactor
            if(player.isSprinting) original = reduceFactorWhileSprint
            if(tickSinceLastAttack < 3) original = reduceFactorWhileHit
            if(player.isSprinting && tickSinceLastAttack < 3) original = reduceFactorWhileHitSprint
            var amount = original
            if(reduceMode == "Smooth") amount = 1 - original
            amount = MathHelper.clamp_float(amount, 0f, 1f)
            player.motionX*= amount
            player.motionZ*= amount

        }
    }
    private fun doJump() {
        val player = mc.thePlayer ?: return
        when (howToJump) {
            "Functional" -> player.tryJump()
            "Legitimize" -> KeyBinding.onTick(mc.gameSettings.keyBindJump.keyCode)
            "Motion" -> player.motionY = motionHeight.toDouble()
        }
    }
    private fun shouldPause(): Boolean {
        when(pauseWhen){
            "Server flag packet received" -> return tickSinceLastFlag < serverLagTick
            "Not in combat" -> return tickSinceLastAttack < notInCombatTick
            "Both" -> return tickSinceLastFlag < serverLagTick || tickSinceLastAttack < notInCombatTick
        }
        return false
    }

    private fun shouldReduce(): Boolean {
        val player = mc.thePlayer ?: return false
        when(reduceEvent){
            "Jumped" -> return shouldJump
            "Hurt time updated" -> return player.hurtTime in hurtTimeRange
        }
        return false
    }

    private fun resetAll(){
        tickSinceLastVelocity = 0
        tickSinceLastAttack = 0
        tickSinceLastFlag = 0
        shouldJump = false
        lastHurtTime = 0
        lastVelocitySize = -1
    }

    override fun onEnable() {
        resetAll()
    }

    override fun onDisable() {
        resetAll()
    }

    val onWorld = handler<WorldEvent> {_ ->
        resetAll()
    }

    val onTick = handler<PlayerTickEvent> { _ ->
        tickSinceLastVelocity++
        tickSinceLastAttack++
        tickSinceLastFlag++
    }

    private fun canJump(xzAverageMotion: Int): Boolean {
        val player = mc.thePlayer ?: return false
        if(xzAverageMotion !in activeMotion) return false
        if(stopWhenBlocking && player.isBlocking) return false
        if(stopWhenBackward && player.moveForward==-1.0F) return false
        if(stopWhenSneaking && player.isSneaking) return false
        if(stopWhenFire && player.isBurning) return false
        if(stopWhenSpeed && player.isPotionActive(Potion.moveSpeed)) return false
        if(stopWhenJumpBoost && player.isPotionActive(Potion.jump)) return false
        if(stopWhenInInventory && mc.currentScreen != null) return false
        if(stopWhenBadSurrounding && (player.isCollidedHorizontally||player.isInLiquid||player.isOnLadder||player.isInWeb)) return false
        if(stopWhenInAir && !player.onGround) return false
        if(shouldPause()) return false
        when(jumpRate) {
            "Tick since last velocity" -> return tickSinceLastVelocity >= minTickSinceLastVL
            "Simple RNG" -> return Random.nextInt(0, 100) <= simpleRNG
            "Polar Safe RNG" -> return player.ticksExisted%2==0
        }
        return false
    }

}