package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.util.Vec3
import java.util.*
import kotlin.concurrent.schedule

@Suppress("KotlinConstantConditions")
object BlinkUtils : MinecraftInstance, Listenable {

    val packets = mutableListOf<Packet<*>>()
    val packetsReceived = mutableListOf<Packet<*>>()
    private var fakePlayer: EntityOtherPlayerMP? = null
    val positions = mutableListOf<Vec3>()
    val isBlinking
        get() = packets.isNotEmpty() || packetsReceived.isNotEmpty()

    private var blinkTimeoutTask: TimerTask? = null
    private var blinkTimeoutTimer: Timer? = null
    private var isTimeoutScheduled = false

    private var filterMaxAllowCount: Int = Int.MAX_VALUE
    private var currentAllowedCount: Int = 0
    private var activeFilter: ((Packet<*>) -> Boolean)? = null

    fun blink(
        packet: Packet<*>,
        event: PacketEvent,
        sent: Boolean? = true,
        receive: Boolean? = true,
        receiveFilter: ((Packet<*>) -> Boolean)? = null,
        blinkTimes: Long? = null,
        maxAllowedPackets: Int = Int.MAX_VALUE
    ) {
        val player = mc.thePlayer ?: return

        if (event.isCancelled || player.isDead || mc.currentServerData == null) return

        when (packet) {
            is C00Handshake, is C00PacketServerQuery, is C01PacketPing,
            is S02PacketChat, is C01PacketChatMessage -> {
                return
            }

            is S29PacketSoundEffect -> {
                if (packet.soundName == "game.player.hurt") {
                    return
                }
            }
        }

        if ((!isBlinking || !isTimeoutScheduled) && blinkTimes != null && blinkTimes > 0) {
            scheduleAutoUnblink(blinkTimes)
        }

        if (isTimeoutScheduled && blinkTimeoutTask == null) {
            return
        }

        if (activeFilter != receiveFilter || filterMaxAllowCount != maxAllowedPackets) {
            activeFilter = receiveFilter
            filterMaxAllowCount = maxAllowedPackets
            currentAllowedCount = 0
        }

        var shouldReceive = false
        if (receiveFilter != null) {
            shouldReceive = receiveFilter(packet)
            if (shouldReceive && currentAllowedCount < filterMaxAllowCount) {
                currentAllowedCount++
                return
            } else if (shouldReceive) {
                shouldReceive = false
            }
        }

        if (sent == true && receive == false) {
            if (event.eventType == EventState.RECEIVE) {
                if (shouldReceive) {
                    return
                }

                synchronized(packetsReceived) {
                    PacketUtils.schedulePacketProcess(packetsReceived)
                }
                packetsReceived.clear()
            }
            if (event.eventType == EventState.SEND) {
                event.cancelEvent()
                synchronized(packets) {
                    packets += packet
                }
                if (packet is C03PacketPlayer && packet.isMoving) {
                    val packetPos = Vec3(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
            }
        }

        if (receive == true && sent == false) {
            if (event.eventType == EventState.RECEIVE && player.ticksExisted > 10) {
                if (shouldReceive) {
                    return
                }

                event.cancelEvent()

                synchronized(packetsReceived) {
                    packetsReceived += packet
                }
            }
            if (event.eventType == EventState.SEND) {
                synchronized(packets) {
                    sendPackets(*packets.toTypedArray(), triggerEvents = false)
                }
                if (packet is C03PacketPlayer && packet.isMoving) {
                    val packetPos = Vec3(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
                packets.clear()
            }
        }

        if (sent == true && receive == true) {
            if (event.eventType == EventState.RECEIVE && player.ticksExisted > 10) {
                if (shouldReceive) {
                    return
                }

                event.cancelEvent()
                synchronized(packetsReceived) {
                    packetsReceived += packet
                }

            }
            if (event.eventType == EventState.SEND) {
                event.cancelEvent()
                synchronized(packets) {
                    packets += packet
                }

                if (packet is C03PacketPlayer && packet.isMoving) {
                    val packetPos = Vec3(packet.x, packet.y, packet.z)
                    synchronized(positions) {
                        positions += packetPos
                    }
                }
            }
        }

        if (sent == false && receive == false)
            unblink()
    }

    /**
     * 拦截指定类型的数据包
     * @param event PacketEvent事件
     * @param packetClass 要拦截的数据包类型（如C03PacketPlayer::class.java）
     */
    fun <T : Packet<*>> blinkForSpecificPacket(event: PacketEvent, packetClass: Class<T>) {
        val packet = event.packet

        // 检查包类型
        if (!packetClass.isInstance(packet)) return

        // 调用原来的blink方法
        blink(packet, event)
    }
    private fun scheduleAutoUnblink(timeoutMs: Long) {
        cancelTimeoutTask()

        blinkTimeoutTask = Timer("BlinkTimeout", false).schedule(timeoutMs) {
            synchronized(this@BlinkUtils) {
                if (isBlinking) {
                    mc.addScheduledTask {
                        unblink()
                    }
                }
                isTimeoutScheduled = false
                blinkTimeoutTask = null
            }
        }

        isTimeoutScheduled = true
    }

    private fun cancelTimeoutTask() {
        synchronized(this) {
            blinkTimeoutTask?.cancel()
            blinkTimeoutTask = null
            blinkTimeoutTimer?.cancel()
            blinkTimeoutTimer = null
            isTimeoutScheduled = false
        }
    }

    val onWorld = handler<WorldEvent> { event ->
        if (event.worldClient == null) {
            clear()
        }
    }

    fun syncSent() {
        synchronized(packetsReceived) {
            PacketUtils.schedulePacketProcess(packetsReceived)
            packetsReceived.clear()
        }
        resetFilterState()
        cancelTimeoutTask()
    }

    fun syncReceived() {
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
            packets.clear()
        }
        resetFilterState()
        cancelTimeoutTask()
    }

    fun cancel() {
        val player = mc.thePlayer ?: return
        val firstPosition = positions.firstOrNull() ?: return

        player.setPositionAndUpdate(firstPosition.xCoord, firstPosition.yCoord, firstPosition.zCoord)

        synchronized(packets) {
            val iterator = packets.iterator()
            while (iterator.hasNext()) {
                val packet = iterator.next()
                if (packet is C03PacketPlayer) {
                    iterator.remove()
                } else {
                    sendPacket(packet)
                    iterator.remove()
                }
            }
        }

        synchronized(positions) {
            positions.clear()
        }

        fakePlayer?.apply {
            fakePlayer?.entityId?.let { mc.theWorld?.removeEntityFromWorld(it) }
            fakePlayer = null
        }

        resetFilterState()
        cancelTimeoutTask()
    }

    fun unblink() {
        synchronized(packetsReceived) {
            PacketUtils.schedulePacketProcess(packetsReceived)
        }
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }

        clear()
        resetFilterState()
        cancelTimeoutTask()

        fakePlayer?.apply {
            fakePlayer?.entityId?.let { mc.theWorld?.removeEntityFromWorld(it) }
            fakePlayer = null
        }
    }

    fun clear() {
        synchronized(packetsReceived) {
            packetsReceived.clear()
        }

        synchronized(packets) {
            packets.clear()
        }

        synchronized(positions) {
            positions.clear()
        }

        resetFilterState()
        cancelTimeoutTask()
    }

    private fun resetFilterState() {
        filterMaxAllowCount = Int.MAX_VALUE
        currentAllowedCount = 0
        activeFilter = null
    }

    fun addFakePlayer() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val faker = EntityOtherPlayerMP(world, player.gameProfile).apply {
            copyLocationAndAnglesFrom(player)
            rotationYaw = player.rotationYaw
            rotationPitch = player.rotationPitch
            rotationYawHead = player.rotationYawHead
            renderYawOffset = player.renderYawOffset
            inventory = player.inventory
        }

        world.addEntityToWorld(RandomUtils.nextInt(Int.MIN_VALUE, Int.MAX_VALUE), faker)

        fakePlayer = faker
    }
}