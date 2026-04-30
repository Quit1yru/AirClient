/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.attackDamage
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK

object AutoWeapon : Module("AutoWeapon", Category.COMBAT, subjective = true) {

    // 设置项
    private val mode by choices("Mode", arrayOf("Normal", "SwitchWeapon"), "Normal")
    private val itemSelect by choices("Item", arrayOf("Sword", "Sword&Axe&Pickaxe", "Sword&Axe&EnchantedStick","All"), "Sword")
    private val useCustomWeightToCalculateWeaponLevel by boolean("UseCustomWeightToCalculateWeaponLevel", false)
    private val damageWeight by int("DamageWeight", 70, 1..100){useCustomWeightToCalculateWeaponLevel}
    private val knockbackWeight by int("KnockbackWeight", 20, 0..100){useCustomWeightToCalculateWeaponLevel}
    private val FireAspectWeight by int("FireAspectWeight", 20, 0..100){useCustomWeightToCalculateWeaponLevel}
    private val switchBackDelay by int("SwitchBackDelay", 500, 1..2000) { mode == "SwitchWeapon" }
    private val spoof by boolean("SpoofItem", false)
    private val spoofTicks by int("SpoofTicks", 10, 1..20) { spoof }
    private val cancelAttackWhenNotUsingBestWeapon by boolean("CancelAttackWhenNotUsingBestWeapon", false) {mode == "Normal"}

    private val onlyOnKillAura by boolean("OnlyOnKillAura",false)
    // 内部变量
    private var attackEnemy = false
    private var bestWeaponSlot = -1
    private var originalSlot = -1
    private val switchTimer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        if (onlyOnKillAura && !KillAura.state) return@handler
        if (mode == "SwitchWeapon" && bestWeaponSlot != -1 && originalSlot != -1 && switchTimer.hasTimePassed(switchBackDelay.toLong())) {
            if (spoof) {
                SilentHotbar.selectSlotSilently(this, originalSlot, spoofTicks, true,
                    render = false,
                    resetManually = true
                )
            } else {
                mc.thePlayer?.inventory?.currentItem = originalSlot
                SilentHotbar.resetSlot(this)
            }
            bestWeaponSlot = -1
            originalSlot = -1
        }
    }

    val onAttack = handler<AttackEvent> {
        if (onlyOnKillAura && !KillAura.state) return@handler
        attackEnemy = true
    }

    val onPacket = handler<PacketEvent> { event ->
        if (onlyOnKillAura && !KillAura.state) return@handler
        val player = mc.thePlayer ?: return@handler

        if (event.packet is C02PacketUseEntity && event.packet.action == ATTACK && attackEnemy) {
            attackEnemy = false

            // 在快捷栏中寻找最佳武器
            val bestSlotPair = (0..8)
                .map { it to player.inventory.getStackInSlot(it) }
                .filter {
                    val item = it.second ?: return@filter false

                    isWeapon(item)
                }
                .maxByOrNull { getLevelScore(it.second) } ?: return@handler
            val bestSlot = bestSlotPair.first

            val isHoldingBestWeapon = bestSlot == player.inventory.currentItem

            if (cancelAttackWhenNotUsingBestWeapon && !isHoldingBestWeapon) {
                event.cancelEvent()
            }

            if (isHoldingBestWeapon) {
                return@handler
            }

            when (mode) {
                "Normal" -> handleNormalMode(player, bestSlot)
                "SwitchWeapon" -> handleSwitchWeaponMode(player, bestSlot)
            }

            sendPacket(event.packet)
            event.cancelEvent()
        }
    }

    private fun handleNormalMode(player: net.minecraft.entity.player.EntityPlayer, slot: Int) {
        SilentHotbar.selectSlotSilently(this, slot, spoofTicks, true, !spoof, spoof)
        if (!spoof) {
            player.inventory.currentItem = slot
            SilentHotbar.resetSlot(this)
        }
    }

    private fun getLevelScore(item: ItemStack): Double {
        return if(useCustomWeightToCalculateWeaponLevel) (item.attackDamage * damageWeight +
                EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, item) * knockbackWeight +
                EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, item) * FireAspectWeight)
        else item.attackDamage
    }

    private fun handleSwitchWeaponMode(player: net.minecraft.entity.player.EntityPlayer, slot: Int) {
        bestWeaponSlot = slot
        originalSlot = player.inventory.currentItem
        switchTimer.reset()

        SilentHotbar.selectSlotSilently(this, slot, spoofTicks, true, !spoof, spoof)
        if (!spoof) {
            player.inventory.currentItem = slot
        }

        // 寻找次佳武器而不是非武器槽位
        val secondBestWeaponSlot = (0..8)
            .filter { it != slot }
            .map { it to player.inventory.getStackInSlot(it) }
            .filter { (_, stack) -> stack != null && isWeapon(stack) }
            .maxByOrNull { (_, stack) -> getLevelScore(stack) }
            ?.first ?: originalSlot // 如果没有其他武器，则回退到原始槽位

        SilentHotbar.selectSlotSilently(this, secondBestWeaponSlot, spoofTicks,
            immediate = true,
            render = false,
            resetManually = spoof
        )
    }

    private fun isWeapon(item: ItemStack): Boolean {
        return when (itemSelect){
            "Sword" -> item.item is ItemSword
            "Sword&Axe&Pickaxe" -> item.item is ItemSword || item.item is ItemTool
            "Sword&Axe&EnchantedStick" -> item.item is ItemSword || item.item is ItemTool || (EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, item)>=1&&(item.item == Items.stick || item.item == Items.blaze_rod))
            else -> item.item!=null
        }
    }
}