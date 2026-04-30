/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.potion.Potion

object KeepSprint : Module("KeepSprint", Category.COMBAT) {
    val tagEnable by boolean("Tag",false)
    val motionAfterAttackOnGround by float("MotionAfterAttackOnGround", 0.6f, 0.0f..1f)
    val motionAfterAttackInAir by float("MotionAfterAttackInAir", 0.6f, 0.0f..1f)
    val workHurtTime by intRange("WorkHurtTime", 0..10, 0..10)
    val BurningCheck by boolean("FireCheck",true)
    val WaterCheck by boolean("WaterCheck",true)
    val LavaCheck by boolean("LavaCheck",true)
    val CobwebCheck by boolean("CobwebCheck", true)
    val SlowDownPotionCheck by boolean("SlowDownPotionCheck",false)
    val SpeedPotionCheck by boolean("SpeedPotionCheck",false)

    val motionAfterAttack
        get() = if (mc.thePlayer.onGround) motionAfterAttackOnGround else motionAfterAttackInAir
        
    private fun shouldKeepSprint(hurtTime: Int): Boolean {
        return hurtTime in workHurtTime
    }

    val shouldKeepSprint: Boolean
        get() {
            val player = mc.thePlayer ?: return false
            
            // 检查基本条件
            if (!handleEvents() || !player.isSprinting) return false
            
            // 检查燃烧状态
            if (BurningCheck && player.isBurning) return false
            
            // 检查是否在水中
            if (WaterCheck && player.isInWater) return false
            
            // 检查是否在熔岩中
            if (LavaCheck && player.isInLava) return false
            
            // 检查是否在蜘蛛网中
            if (CobwebCheck && player.isInWeb) return false
            
            // 检查缓慢药水效果
            if (SlowDownPotionCheck && player.isPotionActive(Potion.moveSlowdown)) return false
            
            // 检查速度药水效果
            if (SpeedPotionCheck && player.isPotionActive(Potion.moveSpeed)) return false
            
            // 检查受伤时间
            if (!shouldKeepSprint(player.hurtTime)) return false
            
            // 所有条件都满足
            return true
        }


    override val tag: String?
        get() = if (tagEnable) "G:${motionAfterAttackOnGround * 100}% | A:${motionAfterAttackInAir * 100}% | HT: ${workHurtTime.first}-${workHurtTime.last}" else null
}