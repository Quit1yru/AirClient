package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C02PacketUseEntity
import java.util.*

object ValidCPSDisplay : Module("ValidCPSDisplay", Category.ALERTS) {
    private val alertCPSWhenExceededLimit by boolean("AlertWhenCPSExceededLimit",false)
    private val alertCPS by int("AlertCPS",21,0..100) {alertCPSWhenExceededLimit}
    private val validAttackTimestamps = LinkedList<Long>()

    private var validCPS = 0

    override val tag: String?
        get() = validCPS.toString()

    val onPacket = handler<PacketEvent> { e->
        val p = e.packet
        if (p is C02PacketUseEntity && p.action == C02PacketUseEntity.Action.ATTACK && p.getEntityFromWorld(mc.theWorld) != null) {
            val currentTime = System.currentTimeMillis()
            validAttackTimestamps.add(currentTime)

            updateValidCPS(currentTime)
        }
    }

    /**
     * 更新有效CPS值
     */
    private fun updateValidCPS(currentTime: Long) {
        val iterator = validAttackTimestamps.iterator()
        while (iterator.hasNext()) {
            val timestamp = iterator.next()
            if (currentTime - timestamp > 1000L) { // 1000ms = 1秒
                iterator.remove()
            } else {
                break
            }
        }

        // 更新当前有效CPS
        validCPS = validAttackTimestamps.size
    }

    /**
     * 模块启用时初始化
     */
    override fun onEnable() {
        validAttackTimestamps.clear()
        validCPS = 0
    }

    /**
     * 模块禁用时清空数据
     */
    override fun onDisable() {
        validAttackTimestamps.clear()
        validCPS = 0
    }

    /**
     * 每tick更新一次（可选，确保CPS实时更新）
     */
    val onTick = handler<GameTickEvent> {
        updateValidCPS(System.currentTimeMillis())
    }
    val onUpdate = handler<UpdateEvent> {
        if (validCPS > alertCPS && alertCPSWhenExceededLimit) chat("Your CPS Exceeded Limit! ($validCPS/$alertCPS)")
    }
}