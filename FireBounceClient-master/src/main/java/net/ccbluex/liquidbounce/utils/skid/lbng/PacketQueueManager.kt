package net.ccbluex.liquidbounce.utils.skid.lbng

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.*
import net.minecraft.client.Minecraft
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C14PacketTabComplete
import net.minecraft.network.play.server.*
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import java.util.*

/**
 * Packet queue manager for LiquidBounce B100
 */
object PacketQueueManager : Listenable {

    private val mc = Minecraft.getMinecraft()
    private val packetQueue: Queue<PacketSnapshot> = Queues.newConcurrentLinkedQueue()

    // 添加防递归标志
    private var isProcessing = false
    private var isFlushing = false

    val isLagging: Boolean
        get() = packetQueue.isNotEmpty()

    val onPacket = handler<PacketEvent> { event ->
        // 防止递归调用
        if (isProcessing || event.isCancelled) {
            return@handler
        }

        isProcessing = true
        try {
            val packet = event.packet

            // B100 中通过包类型判断方向
            val origin = if (isClientBoundPacket(packet)) {
                TransferOrigin.INCOMING
            } else {
                TransferOrigin.OUTGOING
            }

            // 检查是否需要刷新队列
            if (shouldFlushQueue(packet, origin)) {
                flush(origin)
                return@handler
            }

            // 检查是否需要跳过此包
            if (shouldSkipPacket(packet, origin)) {
                return@handler
            }

            val queueAction = fireQueueEvent(packet, origin)
            when (queueAction) {
                PacketAction.QUEUE -> {
                    event.isCancelled = true
                    packetQueue.add(PacketSnapshot(packet, origin, System.currentTimeMillis()))
                }
                PacketAction.FLUSH -> {
                    flush(origin)
                }
                else -> {
                    // 默认行为：不队列化，直接发送
                }
            }
        } finally {
            isProcessing = false
        }
    }

    private fun isClientBoundPacket(packet: Packet<*>): Boolean {
        return packet is S00PacketKeepAlive ||
                packet is S01PacketJoinGame ||
                packet is S02PacketChat ||
                packet is S03PacketTimeUpdate ||
                packet is S04PacketEntityEquipment ||
                packet is S05PacketSpawnPosition ||
                packet is S06PacketUpdateHealth ||
                packet is S07PacketRespawn ||
                packet is S08PacketPlayerPosLook ||
                packet is S09PacketHeldItemChange ||
                packet is S0APacketUseBed ||
                packet is S0BPacketAnimation ||
                packet is S0CPacketSpawnPlayer ||
                packet is S0DPacketCollectItem ||
                packet is S0EPacketSpawnObject ||
                packet is S0FPacketSpawnMob ||
                packet is S10PacketSpawnPainting ||
                packet is S11PacketSpawnExperienceOrb ||
                packet is S12PacketEntityVelocity ||
                packet is S13PacketDestroyEntities ||
                packet is S14PacketEntity ||
                packet is S18PacketEntityTeleport ||
                packet is S19PacketEntityHeadLook ||
                packet is S19PacketEntityStatus ||
                packet is S1BPacketEntityAttach ||
                packet is S1CPacketEntityMetadata ||
                packet is S1DPacketEntityEffect ||
                packet is S1EPacketRemoveEntityEffect ||
                packet is S1FPacketSetExperience ||
                packet is S20PacketEntityProperties ||
                packet is S21PacketChunkData ||
                packet is S22PacketMultiBlockChange ||
                packet is S23PacketBlockChange ||
                packet is S24PacketBlockAction ||
                packet is S25PacketBlockBreakAnim ||
                packet is S26PacketMapChunkBulk ||
                packet is S27PacketExplosion ||
                packet is S28PacketEffect ||
                packet is S29PacketSoundEffect ||
                packet is S2APacketParticles ||
                packet is S2BPacketChangeGameState ||
                packet is S2CPacketSpawnGlobalEntity ||
                packet is S2DPacketOpenWindow ||
                packet is S2EPacketCloseWindow ||
                packet is S2FPacketSetSlot ||
                packet is S30PacketWindowItems ||
                packet is S31PacketWindowProperty ||
                packet is S32PacketConfirmTransaction ||
                packet is S33PacketUpdateSign ||
                packet is S34PacketMaps ||
                packet is S35PacketUpdateTileEntity ||
                packet is S36PacketSignEditorOpen ||
                packet is S37PacketStatistics ||
                packet is S38PacketPlayerListItem ||
                packet is S39PacketPlayerAbilities ||
                packet is S40PacketDisconnect ||
                packet is S41PacketServerDifficulty ||
                packet is S42PacketCombatEvent ||
                packet is S43PacketCamera ||
                packet is S44PacketWorldBorder ||
                packet is S45PacketTitle ||
                packet is S46PacketSetCompressionLevel ||
                packet is S47PacketPlayerListHeaderFooter ||
                packet is S48PacketResourcePackSend ||
                packet is S49PacketUpdateEntityNBT
    }

    private fun shouldFlushQueue(packet: Packet<*>, origin: TransferOrigin): Boolean {
        return when (packet) {
            is S08PacketPlayerPosLook, is S40PacketDisconnect -> true
            is S06PacketUpdateHealth -> packet.health <= 0
            else -> false
        }
    }

    private fun shouldSkipPacket(packet: Packet<*>, origin: TransferOrigin): Boolean {
        return when (packet) {
            is C00Handshake, is C00PacketServerQuery, is C01PacketPing -> true
            is C01PacketChatMessage, is S02PacketChat, is C14PacketTabComplete -> true
            is S29PacketSoundEffect -> packet.soundName == "game.player.hurt"
            else -> false
        }
    }

    val onWorld = handler<WorldEvent> { event ->
        if (event.worldClient == null) {
            packetQueue.clear()
        }
    }

    fun flush(origin: TransferOrigin) {
        // 防止递归调用
        if (isFlushing) {
            return
        }

        isFlushing = true
        try {
            val iterator = packetQueue.iterator()
            while (iterator.hasNext()) {
                val snapshot = iterator.next()
                if (snapshot.origin == origin) {
                    flushSnapshot(snapshot)
                    iterator.remove()
                }
            }
        } finally {
            isFlushing = false
        }
    }

    fun flush(count: Int) {
        // 防止递归调用
        if (isFlushing) {
            return
        }

        isFlushing = true
        try {
            var counter = 0
            val iterator = packetQueue.iterator()

            while (iterator.hasNext() && counter < count) {
                val snapshot = iterator.next()
                val packet = snapshot.packet

                // 使用反射访问 protected 字段
                if (packet is C03PacketPlayer) {
                    val movingField = packet.javaClass.getDeclaredField("moving")
                    movingField.isAccessible = true
                    val isMoving = movingField.getBoolean(packet)

                    if (isMoving) {
                        counter++
                    }
                }

                flushSnapshot(snapshot)
                iterator.remove()
            }
        } finally {
            isFlushing = false
        }
    }

    fun cancel() {
        // 防止递归调用
        if (isProcessing || isFlushing) {
            return
        }

        // 找到最后一个位置包并传送回去
        val lastMovePacket = packetQueue
            .filter { it.packet is C03PacketPlayer }
            .lastOrNull { snapshot ->
                try {
                    val packet = snapshot.packet as C03PacketPlayer
                    val movingField = packet.javaClass.getDeclaredField("moving")
                    movingField.isAccessible = true
                    movingField.getBoolean(packet)
                } catch (e: Exception) {
                    false
                }
            }

        lastMovePacket?.let { snapshot ->
            val posPacket = snapshot.packet as C03PacketPlayer
            mc.thePlayer?.setPosition(posPacket.x, posPacket.y, posPacket.z)
        }

        packetQueue.clear()
    }

    val onQueuePacket = handler<QueuePacketEvent> { event ->
        // 这里决定是否要队列化包
        // Grim 模式会在需要时修改 event.action
        // 默认情况下，我们不队列化任何包
    }

    fun isAboveTime(delay: Long): Boolean {
        val firstPacket = packetQueue.firstOrNull() ?: return false
        return System.currentTimeMillis() - firstPacket.timestamp >= delay
    }

    private fun flushSnapshot(snapshot: PacketSnapshot) {
        // 防止递归调用
        if (isProcessing) {
            return
        }

        when (snapshot.origin) {
            TransferOrigin.OUTGOING -> sendPacketSilently(snapshot.packet)
            TransferOrigin.INCOMING -> {
                // 在 B100 中直接调用包的处理方法
                handleIncomingPacket(snapshot.packet)
            }
        }
    }

    private fun sendPacketSilently(packet: Packet<*>) {
        // 使用无事件发送方式
        if (isProcessing) {
            return
        }

        // 临时禁用事件处理，直接发送包
        isProcessing = true
        try {
            mc.netHandler?.addToSendQueue(packet)
        } finally {
            isProcessing = false
        }
    }

    private fun handleIncomingPacket(packet: Packet<*>) {
        // 防止递归调用
        if (isProcessing) {
            return
        }

        // 手动处理接收包
        when (packet) {
            is S06PacketUpdateHealth -> {
                // 处理生命值更新
                mc.thePlayer?.health = packet.health
                mc.thePlayer?.foodStats?.let { it.foodLevel = packet.foodLevel }
            }
            is S08PacketPlayerPosLook -> {
                // 处理玩家位置更新
                mc.thePlayer?.setPositionAndRotation(packet.x, packet.y, packet.z, packet.yaw, packet.pitch)
            }
            // 其他包可以忽略，因为 B100 的事件系统会处理
        }
    }

    override fun handleEvents(): Boolean {
        return true
    }

    private fun fireEvent(packet: Packet<*>?, origin: TransferOrigin): PacketAction {
        val event = QueuePacketEvent(packet, origin)
        // 这里需要根据 B100 的事件系统来触发事件
        // 如果是 B100 的事件系统，可能是：
        // EventManager.callEvent(event)
        return event.action
    }

    fun fireQueueEvent(packet: Packet<*>?, origin: TransferOrigin): PacketAction {
        val event = QueuePacketEvent(packet, origin)
        EventManager.call(event)
        return event.action
    }
}

data class PacketSnapshot(
    val packet: Packet<*>,
    val origin: TransferOrigin,
    val timestamp: Long
)