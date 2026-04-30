package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.skid.eclipse.EasyUtils.abs
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.EntityLivingBase

object DamageCalculator : Module("DamageCalculator", Category.ALERTS) {
    private var alertWhenDamageWasReduced by boolean("AlertWhenDamageWasReduced",false)

    private var lastHealth = 0.0f
    private var nowHealth = 0.0f
    private var changeHealthCount: Float = 0.0f
    private var trackingTarget: EntityLivingBase? = null
    private var targetArmor = 0

    val onAttack = handler<AttackEvent> { e ->
        if (e.targetEntity == null) return@handler
        val buffer = e.targetEntity as? EntityLivingBase
        if (e.targetEntity != trackingTarget && buffer?.hurtTime in 0..1) {
            trackingTarget = e.targetEntity as? EntityLivingBase
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val target = trackingTarget ?: return@handler
        targetArmor = target.totalArmorValue
        nowHealth = target.health
        if (nowHealth != lastHealth && alertWhenDamageWasReduced) {
            changeHealthCount = SomeUtil.roundToPlacesIfNeeded((nowHealth - lastHealth).abs.toDouble()).toFloat()
        }
        if (target.hurtTime > 0) {
            val rawDamage = SomeUtil.getCurrentWeaponDamage(!mc.thePlayer.onGround && mc.thePlayer.motionY < 0)
            val afterArmor = applyArmorReduction(rawDamage)
            val afterProtection = SomeUtil.roundToPlacesIfNeeded(applyProtectionReduction(target, afterArmor))
            val afterAllDamage = SomeUtil.roundToPlacesIfNeeded(afterProtection)
            chat("Expect damage:$afterAllDamage")
            if (alertWhenDamageWasReduced &&
                !(changeHealthCount == afterProtection.toFloat() ||
                  changeHealthCount == (afterProtection*1.5).toFloat() ||
                  changeHealthCount == afterProtection.toFloat() - 1.0f ||
                  changeHealthCount == 1f
                ) &&
                SomeUtil.roundToPlacesIfNeeded(changeHealthCount.toDouble()) < afterProtection
                ) {
                chat("Detected a possible DamageReduce: Expect:$afterAllDamage | Actual:$changeHealthCount")
            }

            trackingTarget = null
        }
        lastHealth = nowHealth
    }

    override fun onDisable() {
        trackingTarget = null
    }
    private fun applyArmorReduction(incomingDamage: Double): Double {
        val damageReduction = (targetArmor * 0.04).coerceAtMost(0.8)
        val reducedDamage = incomingDamage * (1 - damageReduction)
        return reducedDamage.coerceAtLeast(incomingDamage * 0.2)
    }

    private fun applyProtectionReduction(target: EntityLivingBase, damage: Double): Double {
        var reducedDamage = damage
        val totalProtectionLevel = getTotalProtectionLevel(target)

        if (totalProtectionLevel > 0) {
            val protectionReduction = totalProtectionLevel * 0.04
            reducedDamage = damage * (1 - protectionReduction.coerceAtMost(0.8))
        }

        return reducedDamage
    }

    private fun getTotalProtectionLevel(target: EntityLivingBase): Int {
        var totalLevel = 0

        for (i in 0..3) {
            val armorPiece = target.getEquipmentInSlot(i) ?: continue
            val enchantmentList = armorPiece.enchantmentTagList ?: continue

            for (j in 0 until enchantmentList.tagCount()) {
                val enchantment = enchantmentList.getCompoundTagAt(j)
                val id = enchantment.getShort("id").toInt()
                val level = enchantment.getShort("lvl").toInt()

                if (id == Enchantment.protection.effectId ||
                    id == Enchantment.fireProtection.effectId ||
                    id == Enchantment.blastProtection.effectId ||
                    id == Enchantment.projectileProtection.effectId) {
                    totalLevel += level
                }
            }
        }

        return totalLevel
    }
}