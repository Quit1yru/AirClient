package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.GameLoopEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.calculateAngleDifference
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase

object LagRange : Module("LagRange", Category.COMBAT) {


    val delay by int("Delay", 100, 100..10000)
    val lagTime by int("LagTime", 100, 50..1000)
    val range by floatRange("Range", 3f..3.5f, 0f..8f)
    val smartCombo by boolean("SmartCombo",true)
    val notInCombat by boolean("NotInCombat", true) {!smartCombo}
    val requireKillAura by boolean("RequireKillAura",false) {!notInCombat}
    val onlyGround by boolean("OnlyGround", false)
    val debug by boolean("Debug", false)
    var lastLagTime = 0.0
    var start = false
    val timer = MSTimer()
    var target: EntityLivingBase? = null
    override fun onEnable() {
        start=false
    }


    @Suppress("UNUSED")
    val onGameLoop = handler<GameLoopEvent>{
        val player = mc.thePlayer ?: return@handler
        val range = if (KillAura.state) KillAura.rotationRange else range.endInclusive
        target = RaycastUtils.raycastEntity(range.toDouble()) { isSelected(it, true) } as EntityLivingBase?
        if(start){
            if(timer.hasTimePassed(lagTime)) {
                if(debug) Chat.print("Stop lag, skipped ${timer.getTimePassed()}ms")
                mc.timer.timerSpeed=1f
                start=false
                lastLagTime = System.currentTimeMillis().toDouble()
                for (i in 0 until timer.getTimePassed()/50) {
                    try {
                        mc.runTick()
                    }catch (e: Exception) {
                        Chat.print("Unexpected error while boosting, please report to dev(include ur MineCraft log).")
                        e.printStackTrace()
                    }
                }
            }else mc.timer.timerSpeed=0f
        }else {
            if(shouldStart()) {
                val e = mc.objectMouseOver.entityHit
                if(debug) Chat.print("Start lag with ${player.getDistanceToEntity(e)}")
                start=true
                mc.timer.timerSpeed=0f
                timer.reset()
            }
        }
    }

    fun shouldStart(): Boolean {
        if(requireKillAura && !KillAura.state || KillAura.target == null)return false

        if(target==null)return false

        return when {
            !mc.thePlayer.onGround && this.onlyGround -> false
            System.currentTimeMillis() - this.lastLagTime < this.delay -> false
            notInCombat && mc.thePlayer.hurtTime > 0 -> false
            smartCombo -> when {
                calculateAngleDifference() > 30.0 -> false
                else -> target!!.hurtResistantTime > 0
            }
            else -> mc.thePlayer.getDistanceToEntity(target) in range
        }
    }
}