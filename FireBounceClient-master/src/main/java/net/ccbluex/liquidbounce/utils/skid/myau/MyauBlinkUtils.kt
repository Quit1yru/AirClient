package net.ccbluex.liquidbounce.utils.skid.myau

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import java.util.concurrent.ConcurrentLinkedDeque

object MyauBlinkUtils : Listenable, MinecraftInstance {

    enum class BlinkModules {
        NONE, AUTO_BLOCK, MOVEMENT, COMBAT
    }

    private val blinkedPackets = ConcurrentLinkedDeque<net.minecraft.network.Packet<*>>()
    var blinkModule = BlinkModules.NONE
        private set
    private var pass = false

    /**
     * 提供数据包到闪烁队列
     */
    fun offerPacket(packet: net.minecraft.network.Packet<*>): Boolean {
        if (blinkModule == BlinkModules.NONE ||
            packet is C00PacketKeepAlive ||
            packet is C01PacketChatMessage) {
            return false
        } else if (blinkedPackets.isEmpty() && packet is C0FPacketConfirmTransaction) {
            return false
        } else {
            blinkedPackets.offer(packet)
            return true
        }
    }

    /**
     * 设置闪烁状态
     */
    fun setBlinkState(state: Boolean, module: BlinkModules): Boolean {
        if (module == BlinkModules.NONE) {
            return false
        }
        if (blinkModule != module) {
            return false
        }

        if (state) {
            blinkModule = module
            pass = false
        } else {
            // 释放所有积压的数据包
            if (mc.netHandler != null && blinkedPackets.isEmpty()) {
                return true
            }

            while (true) {
                val packet = blinkedPackets.poll() ?: break
                mc.netHandler.addToSendQueue(packet)
            }
            blinkedPackets.clear()
            pass = true
        }
        return true
    }

    /**
     * 启动指定模块的闪烁
     */
    fun startBlink(module: BlinkModules): Boolean {
        if (blinkModule != BlinkModules.NONE && blinkModule != module) {
            return false
        }
        blinkModule = module
        pass = false
        return true
    }

    /**
     * 停止闪烁并释放数据包
     */
    fun stopBlink(): Boolean {
        if (blinkModule == BlinkModules.NONE) {
            return false
        }

        // 释放所有积压的数据包
        if (mc.netHandler != null) {
            while (true) {
                val packet = blinkedPackets.poll() ?: break
                mc.netHandler.addToSendQueue(packet)
            }
        }
        blinkedPackets.clear()
        blinkModule = BlinkModules.NONE
        pass = true
        return true
    }

    /**
     * 获取当前闪烁的模块
     */
    fun getBlinkingModule(): BlinkModules = blinkModule

    /**
     * 计算移动数据包数量
     */
    fun countMovement(): Long = blinkedPackets.count { it is C03PacketPlayer }.toLong()

    /**
     * 是否可以发送数据包（不拦截）
     */
    fun canSendPacket(): Boolean = pass

    /**
     * 获取积压数据包数量
     */
    fun getBacklogSize(): Int = blinkedPackets.size

    /**
     * 清空所有积压数据包（不发送）
     */
    fun clearBacklog() {
        blinkedPackets.clear()
    }

    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        // 某些数据包会强制停止闪烁
        if (packet is C00Handshake ||
            packet is C00PacketLoginStart ||
            packet is C00PacketServerQuery ||
            packet is C01PacketPing ||
            packet is C01PacketEncryptionResponse) {
            stopBlink()
        }

        // 如果正在闪烁，拦截数据包
        if (blinkModule != BlinkModules.NONE && !pass) {
            if (offerPacket(packet)) {
                event.cancelEvent()
            }
        }
    }

    fun onWorld(event: WorldEvent) {
        // 世界变化时停止闪烁
        stopBlink()
    }

    override fun handleEvents(): Boolean = true
}