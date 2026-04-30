/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.withY
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.*

val mc = MinecraftInstance.mc
/**
 * Called when player attacks other entity
 *
 * @param targetEntity Attacked entity
 */
class AttackEvent(val targetEntity: Entity?) : Event()

/**
 * Called when minecraft get bounding box of block
 *
 * @param blockPos block position of block
 * @param block block itself
 * @param boundingBox vanilla bounding box
 */
class BlockBBEvent(blockPos: BlockPos, val block: Block, var boundingBox: AxisAlignedBB?) : Event() {
    val x = blockPos.x
    val y = blockPos.y
    val z = blockPos.z
}
/**
 * Called every second after game startup
 */
object SecondTickEvent : Event()

/**
 * Called when player clicks a block
 */
class ClickBlockEvent(val clickedBlock: BlockPos?, val enumFacing: EnumFacing?) : Event()

/**
 * Called when client is shutting down
 */
object ClientShutdownEvent : Event()

/**
 * Called when another entity moves
 */
data class EntityMovementEvent(val movedEntity: Entity) : Event()

/**
 * Called when player jumps
 *
 * @param motion jump motion (y motion)
 */
class JumpEvent(var motion: Float, val eventState: EventState) : CancellableEvent()

/**
 * Called when user press a key once
 *
 * @param key Pressed key
 */
class KeyEvent(val key: Int) : Event()

/**
 * Called in "onUpdateWalkingPlayer"
 *
 * @param eventState PRE or POST
 */
class MotionEvent(var x: Double, var y: Double, var z: Double, var onGround: Boolean, val eventState: EventState) :
    Event()

/**
 * Called in "onLivingUpdate" when the player is using a use item.
 *
 * @param strafe the applied strafe slow down
 * @param forward the applied forward slow down
 */
class SlowDownEvent(var strafe: Float, var forward: Float) : Event()

/**
 * Called in "onLivingUpdate" when the player is sneaking.
 *
 * @param strafe the applied strafe slow down
 * @param forward the applied forward slow down
 */
class SneakSlowDownEvent(var strafe: Float, var forward: Float) : Event()

/**
 * Called in "onLivingUpdate" after the movement input update.
 *
 * @param originalInput the movement input after the update
 */
class MovementInputEvent(var originalInput: MovementInput) : Event()

/**
 * Called in "onLivingUpdate" after when the player's sprint states are updated
 */
object PostSprintUpdateEvent : Event()

/**
 * Called in "moveFlying"
 */
class StrafeEvent(val strafe: Float, val forward: Float, val friction: Float) : CancellableEvent()

/**
 * Called when player moves
 *
 * @param x motion
 * @param y motion
 * @param z motion
 */
class MoveEvent(var x: Double, var y: Double, var z: Double) : CancellableEvent() {
    var isSafeWalk = false

    fun zero() {
        x = 0.0
        y = 0.0
        z = 0.0
    }

    fun zeroXZ() {
        x = 0.0
        z = 0.0
    }
}

/**
 * Called when receive or send a packet
 */
class PacketEvent(val packet: Packet<*>, val eventType: EventState) : CancellableEvent()
val Packet<*>.isAttackPacket: Boolean
    get() = this is C02PacketUseEntity && this.action == C02PacketUseEntity.Action.ATTACK
val Packet<*>.isAttackPacketAndSwingPacket: Boolean
    get() = this.isAttackPacket || this.isSwingPacket
val Packet<*>.isSwingPacket: Boolean
    get() = this is C0APacketAnimation
val Packet<*>.isSelfVelocityVelocity: Boolean
    get() = this is S12PacketEntityVelocity && this.entityID == mc.thePlayer.entityId
val Packet<*>.isMovePacket: Boolean
    get() = this is C03PacketPlayer
val Packet<*>.isRotationPacket: Boolean
    get() = this is C03PacketPlayer.C05PacketPlayerLook
val Packet<*>.isServerLagPacket: Boolean
    get() = this is S08PacketPlayerPosLook
val Packet<*>.isPlaceBlockPacket: Boolean
    get() = this is C08PacketPlayerBlockPlacement
/**
 * Called when a block tries to push you
 */
class BlockPushEvent : CancellableEvent()

/**
 * Called when screen is going to be rendered
 */
class Render2DEvent(val partialTicks: Float) : Event()

/**
 * Called when packets sent to client are processed
 */
object GameLoopEvent : Event()

/**
 * Called when world is going to be rendered
 */
class Render3DEvent(val partialTicks: Float) : Event()

/**
 * Called when the screen changes
 */
class ScreenEvent(val guiScreen: GuiScreen?) : Event()

/**
 * Called when the session changes
 */
object SessionUpdateEvent : Event()

/**
 * Called when player is going to step
 */
class StepEvent(var stepHeight: Float) : Event()

/**
 * Called when player step is confirmed
 */
object StepConfirmEvent : Event()

/**
 * tick... tack... tick... tack
 */
object GameTickEvent : Event()

object PreTickEvent : Event()

class BlockCollideEvent(val blockPos: BlockPos, val blockState: IBlockState) : CancellableEvent() {
    operator fun component1() = blockPos.x
    operator fun component2() = blockPos.y
    operator fun component3() = blockPos.z
}
/**
 * Called when player receives velocity/knockback from S12PacketEntityVelocity
 *
 * @param packet 速度包实例
 * @param target 被击退的目标实体
 * @param motionX X轴运动速度
 * @param motionY Y轴运动速度
 * @param motionZ Z轴运动速度
 */
/**
 * Called when player receives velocity/knockback from S12PacketEntityVelocity
 *
 * @param packet 速度包实例
 * @param target 被击退的目标实体
 * @param motionX X轴运动速度
 * @param motionY Y轴运动速度
 * @param motionZ Z轴运动速度
 */
class KnockBackEvent(
    val packet: S12PacketEntityVelocity,
    val target: Entity,
    var motionX: Int,
    var motionY: Int,
    var motionZ: Int
) : CancellableEvent() {

    /**
     * 获取真实的运动值（包中的值是乘以8000的）
     */
    fun getRealMotionX(): Double = motionX / 8000.0
    fun getRealMotionY(): Double = motionY / 8000.0
    fun getRealMotionZ(): Double = motionZ / 8000.0

    /**
     * 设置真实的运动值（会自动乘以8000）
     */
    fun setRealMotionX(motion: Double) {
        motionX = (motion * 8000.0).toInt()
    }

    fun setRealMotionY(motion: Double) {
        motionY = (motion * 8000.0).toInt()
    }

    fun setRealMotionZ(motion: Double) {
        motionZ = (motion * 8000.0).toInt()
    }

    /**
     * 将修改后的运动值更新到原始数据包中
     */
    fun updatePacket() {
        packet.motionX = motionX
        packet.motionY = motionY
        packet.motionZ = motionZ
    }
}



object TickEndEvent : Event()

/**
 * tick tack for player
 */
class PlayerTickEvent(val state: EventState) : CancellableEvent()

object RotationUpdateEvent : Event()

class RotationSetEvent(var yawDiff: Float, var pitchDiff: Float) : CancellableEvent()

class CameraPositionEvent(
    val currPos: Vec3, val prevPos: Vec3, val lastTickPos: Vec3,
    var result: FreeCam.PositionPair? = null,
) : Event() {
    fun withY(value: Double) {
        result = FreeCam.PositionPair(currPos.withY(value), prevPos.withY(value), lastTickPos.withY(value))
    }

    // 添加修改X和Z的方法
    fun withX(value: Double) {
        result = FreeCam.PositionPair(
            Vec3(value, currPos.yCoord, currPos.zCoord),
            Vec3(value, prevPos.yCoord, prevPos.zCoord),
            Vec3(value, lastTickPos.yCoord, lastTickPos.zCoord)
        )
    }

    fun withZ(value: Double) {
        result = FreeCam.PositionPair(
            Vec3(currPos.xCoord, currPos.yCoord, value),
            Vec3(prevPos.xCoord, prevPos.yCoord, value),
            Vec3(lastTickPos.xCoord, lastTickPos.yCoord, value)
        )
    }

    fun withXYZ(x: Double, y: Double, z: Double) {
        result = FreeCam.PositionPair(
            Vec3(x, y, z),
            Vec3(x, prevPos.yCoord, prevPos.zCoord),  // 注意：prevPos和lastTickPos可能需要不同的处理
            Vec3(x, lastTickPos.yCoord, lastTickPos.zCoord)
        )
    }

    fun setAllPositions(curr: Vec3, prev: Vec3, lastTick: Vec3) {
        result = FreeCam.PositionPair(curr, prev, lastTick)
    }
}

class ClientSlotChangeEvent(var supposedSlot: Int, var modifiedSlot: Int) : Event()

class DelayedPacketProcessEvent : CancellableEvent()

/**
 * Called when minecraft player will be updated
 */
object UpdateEvent : Event()

/**
 * Called when the world changes
 */
class WorldEvent(val worldClient: WorldClient?, val eventState: Any) : Event()

/**
 * Called when window clicked
 */
class ClickWindowEvent(val windowId: Int, val slotId: Int, val mouseButtonClicked: Int, val mode: Int) :
    CancellableEvent()

/**
 * Called when Client finishes starting up
 */
object StartupEvent : Event()

class HurtEvent(
    val targetEntity: Entity,
    val hurtTime: Int = 0
) : Event()

class QueuePacketEvent(
    val packet: Packet<*>?,
    val origin: TransferOrigin
) : Event() {

    var action: PacketAction = PacketAction.FLUSH
        set(value) {
            if (field == value || field.priority >= value.priority) {
                return
            }

            field = value
        }

}

enum class TransferOrigin(val choiceName: String) {
    INCOMING("Incoming"),
    OUTGOING("Outgoing");
}

enum class PacketAction(val priority: Int) {
    FLUSH(0),
    PASS(1),
    QUEUE(2)
}
internal val ALL_EVENT_CLASSES = arrayOf(
    PlayerTickEvent::class.java,
    StepConfirmEvent::class.java,
    SessionUpdateEvent::class.java,
    MovementInputEvent::class.java,
    GameLoopEvent::class.java,
    Render2DEvent::class.java,
    ClickWindowEvent::class.java,
    StartupEvent::class.java,
    SneakSlowDownEvent::class.java,
    PostSprintUpdateEvent::class.java,
    KeyEvent::class.java,
    SlowDownEvent::class.java,
    TickEndEvent::class.java,
    JumpEvent::class.java,
    MoveEvent::class.java,
    ClientShutdownEvent::class.java,
    GameTickEvent::class.java,
    StepEvent::class.java,
    BlockBBEvent::class.java,
    ClickBlockEvent::class.java,
    UpdateEvent::class.java,
    RotationSetEvent::class.java,
    EntityMovementEvent::class.java,
    ClientSlotChangeEvent::class.java,
    PacketEvent::class.java,
    CameraPositionEvent::class.java,
    RotationUpdateEvent::class.java,
    StrafeEvent::class.java,
    ScreenEvent::class.java,
    AttackEvent::class.java,
    BlockPushEvent::class.java,
    Render3DEvent::class.java,
    MotionEvent::class.java,
    WorldEvent::class.java,
    DelayedPacketProcessEvent::class.java,
    PreTickEvent::class.java,
    BlockCollideEvent::class.java,
    KnockBackEvent::class.java,
    QueuePacketEvent::class.java,
    SecondTickEvent::class.java,
    HurtEvent::class.java,
)
