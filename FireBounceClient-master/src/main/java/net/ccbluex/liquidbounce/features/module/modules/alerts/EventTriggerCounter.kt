package net.ccbluex.liquidbounce.features.module.modules.alerts

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat


@Suppress("unused")
object EventTriggerCounter : Module("EventTriggerCounter", Category.ALERTS) {

    private val events = arrayOf(
        "AttackEvent", "BlockBBEvent", "ClickBlockEvent", "ClientShutdownEvent",
        "EntityMovementEvent", "JumpEvent", "KeyEvent", "MotionEvent",
        "SlowDownEvent", "SneakSlowDownEvent", "MovementInputEvent", "PostSprintUpdateEvent",
        "StrafeEvent", "MoveEvent", "PacketEvent", "BlockPushEvent",
        "Render2DEvent", "GameLoopEvent", "Render3DEvent", "ScreenEvent",
        "SessionUpdateEvent", "StepEvent", "StepConfirmEvent", "GameTickEvent",
        "PreTickEvent", "BlockCollideEvent", "KnockBackEvent", "TickEndEvent",
        "PlayerTickEvent", "RotationUpdateEvent", "RotationSetEvent", "CameraPositionEvent",
        "ClientSlotChangeEvent", "DelayedPacketProcessEvent", "UpdateEvent", "WorldEvent",
        "ClickWindowEvent", "StartupEvent", "QueuePacketEvent", "SecondTickEvent","HurtEvent"
    )

    private val mode by choices("Mode", arrayOf("All", "Selective"), "Selective")
    private val selectedEvent by choices("Event", events, events[0]) { mode == "Selective" }
    private val cancelEvent by boolean("CancelEvent", false) {
        mode == "Selective" && isSelectedEventCancellable(selectedEvent)
    }

    private val attackHandler = handler<AttackEvent> { event ->
        if (shouldTrigger("AttackEvent")) {
            chat("AttackEvent triggered")
            // AttackEvent 不可取消
        }
    }

    private val blockBBHandler = handler<BlockBBEvent> { event ->
        if (shouldTrigger("BlockBBEvent")) {
            chat("BlockBBEvent triggered")
            // BlockBBEvent 不可取消
        }
    }

    private val onHurt = handler<HurtEvent> { event ->
        if (shouldTrigger("HurtEvent")) {
            chat("HurtEvent Triggered | HurtTime: ${event.hurtTime} | Target: ${event.targetEntity}")
        }
    }
    private val clickBlockHandler = handler<ClickBlockEvent> { event ->
        if (shouldTrigger("ClickBlockEvent")) {
            chat("ClickBlockEvent triggered")
            // ClickBlockEvent 不可取消
        }
    }

    private val clientShutdownHandler = handler<ClientShutdownEvent> {
        if (shouldTrigger("ClientShutdownEvent")) {
            chat("ClientShutdownEvent triggered")
        }
    }

    private val entityMovementHandler = handler<EntityMovementEvent> { event ->
        if (shouldTrigger("EntityMovementEvent")) {
            chat("EntityMovementEvent triggered")
            // EntityMovementEvent 不可取消
        }
    }

    private val jumpHandler = handler<JumpEvent> { event ->
        if (shouldTrigger("JumpEvent")) {
            chat("JumpEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "JumpEvent") {
                event.cancelEvent()
            }
        }
    }

    private val keyHandler = handler<KeyEvent> {
        if (shouldTrigger("KeyEvent")) {
            chat("KeyEvent triggered")
            // KeyEvent 不可取消
        }
    }

    private val motionHandler = handler<MotionEvent> { event ->
        if (shouldTrigger("MotionEvent")) {
            chat("MotionEvent triggered")
            // MotionEvent 不可取消
        }
    }

    private val slowDownHandler = handler<SlowDownEvent> { event ->
        if (shouldTrigger("SlowDownEvent")) {
            chat("SlowDownEvent triggered")
            // SlowDownEvent 不可取消
        }
    }

    private val sneakSlowDownHandler = handler<SneakSlowDownEvent> { event ->
        if (shouldTrigger("SneakSlowDownEvent")) {
            chat("SneakSlowDownEvent triggered")
            // SneakSlowDownEvent 不可取消
        }
    }

    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (shouldTrigger("MovementInputEvent")) {
            chat("MovementInputEvent triggered")
            // MovementInputEvent 不可取消
        }
    }

    private val postSprintUpdateHandler = handler<PostSprintUpdateEvent> {
        if (shouldTrigger("PostSprintUpdateEvent")) {
            chat("PostSprintUpdateEvent triggered")
        }
    }

    private val strafeHandler = handler<StrafeEvent> { event ->
        if (shouldTrigger("StrafeEvent")) {
            chat("StrafeEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "StrafeEvent") {
                event.cancelEvent()
            }
        }
    }

    private val moveHandler = handler<MoveEvent> { event ->
        if (shouldTrigger("MoveEvent")) {
            chat("MoveEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "MoveEvent") {
                event.cancelEvent()
            }
        }
    }

    private val packetHandler = handler<PacketEvent> { event ->
        if (shouldTrigger("PacketEvent")) {
            chat("PacketEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "PacketEvent") {
                event.cancelEvent()
            }
        }
    }

    private val blockPushHandler = handler<BlockPushEvent> { event ->
        if (shouldTrigger("BlockPushEvent")) {
            chat("BlockPushEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "BlockPushEvent") {
                event.cancelEvent()
            }
        }
    }

    private val render2DHandler = handler<Render2DEvent> {
        if (shouldTrigger("Render2DEvent")) {
            chat("Render2DEvent triggered")
            // Render2DEvent 不可取消
        }
    }

    private val gameLoopHandler = handler<GameLoopEvent> {
        if (shouldTrigger("GameLoopEvent")) {
            chat("GameLoopEvent triggered")
            // GameLoopEvent 不可取消
        }
    }

    private val render3DHandler = handler<Render3DEvent> {
        if (shouldTrigger("Render3DEvent")) {
            chat("Render3DEvent triggered")
            // Render3DEvent 不可取消
        }
    }

    private val screenHandler = handler<ScreenEvent> {
        if (shouldTrigger("ScreenEvent")) {
            chat("ScreenEvent triggered")
            // ScreenEvent 不可取消
        }
    }

    private val sessionUpdateHandler = handler<SessionUpdateEvent> {
        if (shouldTrigger("SessionUpdateEvent")) {
            chat("SessionUpdateEvent triggered")
            // SessionUpdateEvent 不可取消
        }
    }

    private val stepHandler = handler<StepEvent> { event ->
        if (shouldTrigger("StepEvent")) {
            chat("StepEvent triggered")
            // StepEvent 不可取消
        }
    }

    private val stepConfirmHandler = handler<StepConfirmEvent> {
        if (shouldTrigger("StepConfirmEvent")) {
            chat("StepConfirmEvent triggered")
            // StepConfirmEvent 不可取消
        }
    }

    private val gameTickHandler = handler<GameTickEvent> {
        if (shouldTrigger("GameTickEvent")) {
            chat("GameTickEvent triggered")
            // GameTickEvent 不可取消
        }
    }

    private val preTickHandler = handler<PreTickEvent> {
        if (shouldTrigger("PreTickEvent")) {
            chat("PreTickEvent triggered")
            // PreTickEvent 不可取消
        }
    }

    private val blockCollideHandler = handler<BlockCollideEvent> { event ->
        if (shouldTrigger("BlockCollideEvent")) {
            chat("BlockCollideEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "BlockCollideEvent") {
                event.cancelEvent()
            }
        }
    }

    private val knockBackHandler = handler<KnockBackEvent> { event ->
        if (shouldTrigger("KnockBackEvent")) {
            chat("KnockBackEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "KnockBackEvent") {
                event.cancelEvent()
            }
        }
    }

    private val tickEndHandler = handler<TickEndEvent> {
        if (shouldTrigger("TickEndEvent")) {
            chat("TickEndEvent triggered")
            // TickEndEvent 不可取消
        }
    }

    private val playerTickHandler = handler<PlayerTickEvent> { event ->
        if (shouldTrigger("PlayerTickEvent")) {
            chat("PlayerTickEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "PlayerTickEvent") {
                event.cancelEvent()
            }
        }
    }

    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (shouldTrigger("RotationUpdateEvent")) {
            chat("RotationUpdateEvent triggered")
            // RotationUpdateEvent 不可取消
        }
    }

    private val rotationSetHandler = handler<RotationSetEvent> { event ->
        if (shouldTrigger("RotationSetEvent")) {
            chat("RotationSetEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "RotationSetEvent") {
                event.cancelEvent()
            }
        }
    }

    private val cameraPositionHandler = handler<CameraPositionEvent> { event ->
        if (shouldTrigger("CameraPositionEvent")) {
            chat("CameraPositionEvent triggered")
            // CameraPositionEvent 不可取消
        }
    }

    private val clientSlotChangeHandler = handler<ClientSlotChangeEvent> { event ->
        if (shouldTrigger("ClientSlotChangeEvent")) {
            chat("ClientSlotChangeEvent triggered")
            // ClientSlotChangeEvent 不可取消
        }
    }

    private val delayedPacketProcessHandler = handler<DelayedPacketProcessEvent> { event ->
        if (shouldTrigger("DelayedPacketProcessEvent")) {
            chat("DelayedPacketProcessEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "DelayedPacketProcessEvent") {
                event.cancelEvent()
            }
        }
    }

    private val updateHandler = handler<UpdateEvent> {
        if (shouldTrigger("UpdateEvent")) {
            chat("UpdateEvent triggered")
            // UpdateEvent 不可取消
        }
    }

    private val worldHandler = handler<WorldEvent> {
        if (shouldTrigger("WorldEvent")) {
            chat("WorldEvent triggered")
            // WorldEvent 不可取消
        }
    }

    private val clickWindowHandler = handler<ClickWindowEvent> { event ->
        if (shouldTrigger("ClickWindowEvent")) {
            chat("ClickWindowEvent triggered")
            if (cancelEvent && mode == "Selective" && selectedEvent == "ClickWindowEvent") {
                event.cancelEvent()
            }
        }
    }

    private val startupHandler = handler<StartupEvent> {
        if (shouldTrigger("StartupEvent")) {
            chat("StartupEvent triggered")
            // StartupEvent 不可取消
        }
    }

    private val queuePacketHandler = handler<QueuePacketEvent> { event ->
        if (shouldTrigger("QueuePacketEvent")) {
            chat("QueuePacketEvent triggered")
        }
    }

    private val secondTickHandler = handler<SecondTickEvent> {
        if (shouldTrigger("SecondTickEvent")) {
            chat("SecondTickEvent triggered")
            // SecondTickEvent 不可取消
        }
    }

    private fun shouldTrigger(eventName: String): Boolean {
        return when (mode) {
            "All" -> true
            "Selective" -> selectedEvent == eventName
            else -> false
        }
    }

    private fun isSelectedEventCancellable(eventName: String): Boolean {
        val cancellableEvents = setOf(
            "JumpEvent", "StrafeEvent", "MoveEvent", "PacketEvent",
            "BlockPushEvent", "BlockCollideEvent", "KnockBackEvent",
            "PlayerTickEvent", "RotationSetEvent",
            "DelayedPacketProcessEvent", "ClickWindowEvent",
        )
        return eventName in cancellableEvents
    }
}
