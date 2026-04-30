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
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.item.ItemAxe
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C09PacketHeldItemChange

object ArmorBreaker : Module("ArmorBreaker", Category.COMBAT, subjective = true) {

    // 设置项
    private val switchBackDelay by int("SwitchBackDelay", 100, 0..1000)
    private val spoof by boolean("SpoofItem", true)
    private val spoofTicks by int("SpoofTicks", 10, 1..20) { spoof }
    private val onlyOnKillAura by boolean("OnlyOnKillAura", false)

    // 内部变量
    private var attackEnemy = false
    private var axeSlot = -1
    private var originalSlot = -1
    private val switchTimer = MSTimer()
    private var shouldSwitchBack = false

    val onUpdate = handler<UpdateEvent> {
        if (onlyOnKillAura && !KillAura.state) return@handler

        // 检查是否需要切换回原武器
        if (shouldSwitchBack && axeSlot != -1 && originalSlot != -1 && switchTimer.hasTimePassed(switchBackDelay.toLong())) {
            if (spoof) {
                SilentHotbar.selectSlotSilently(
                    this,
                    originalSlot,
                    spoofTicks,
                    immediate = true,
                    render = false,
                    resetManually = true
                )
            } else {
                mc.thePlayer?.inventory?.currentItem = originalSlot
                SilentHotbar.resetSlot(this)
            }

            axeSlot = -1
            originalSlot = -1
            shouldSwitchBack = false
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

            // 在快捷栏中寻找斧头
            val axeSlotPair = (0..8)
                .map { it to player.inventory.getStackInSlot(it) }
                .firstOrNull {
                    val item = it.second ?: return@firstOrNull false
                    item.item is ItemAxe
                } ?: return@handler

            val foundAxeSlot = axeSlotPair.first

            // 如果已经拿着斧头,不需要切换
            if (foundAxeSlot == player.inventory.currentItem) {
                return@handler
            }

            // 保存原始槽位
            originalSlot = player.inventory.currentItem
            axeSlot = foundAxeSlot

            // 发送切换到斧头的包
            sendPacket(C09PacketHeldItemChange(axeSlot))

            // 发送攻击包
            sendPacket(event.packet)

            // 立即切换回原武器
            sendPacket(C09PacketHeldItemChange(originalSlot))

            // 取消原始攻击包
            event.cancelEvent()

            // 标记需要在延迟后切换回原武器(用于客户端显示)
            shouldSwitchBack = true
            switchTimer.reset()

            // 如果使用Spoof模式,静默切换
            if (spoof) {
                SilentHotbar.selectSlotSilently(
                    this,
                    axeSlot,
                    spoofTicks,
                    immediate = true,
                    render = false,
                    resetManually = true
                )
            }
        }
    }

    override fun onDisable() {
        if (originalSlot != -1) {
            mc.thePlayer?.inventory?.currentItem = originalSlot
        }
        SilentHotbar.resetSlot(this)
        axeSlot = -1
        originalSlot = -1
        shouldSwitchBack = false
        attackEnemy = false
    }
}
