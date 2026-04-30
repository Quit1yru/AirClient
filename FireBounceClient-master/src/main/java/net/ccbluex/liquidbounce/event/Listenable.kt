/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.*
import kotlin.coroutines.resume

interface Listenable {
    fun handleEvents(): Boolean = parent?.handleEvents() ?: true

    val subListeners: Array<Listenable>
        get() = emptyArray()

    val parent: Listenable?
        get() = null
}

inline fun <reified T : Event> Listenable.handler(
    always: Boolean = false,
    priority: Byte = 0,
    noinline action: (T) -> Unit
): EventHook<T> {
    val hook = EventHook(this, always, priority, action)
    EventManager.registerEventHook(T::class.java, hook)
    return hook
}

@OptIn(DelicateCoroutinesApi::class)
inline fun <reified T : Event> Listenable.handler(
    dispatcher: CoroutineDispatcher,
    always: Boolean = false,
    priority: Byte = 0,
    crossinline action: suspend CoroutineScope.(T) -> Unit
): EventHook<T> = handler<T>(always, priority) {  // 添加返回类型
    GlobalScope.launch(dispatcher) { action(it) }
}

// 简化版本的 sequenceHandler
inline fun <reified T : Event> Listenable.sequenceHandler(
    priority: Byte = 0,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    crossinline eventHandler: suspend (T) -> Unit
): EventHook<T> = handler<T>(dispatcher = dispatcher, priority = priority) { eventHandler(it) }

// 简化版本的 tickHandler
inline fun Listenable.tickHandler(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    crossinline eventHandler: suspend CoroutineScope.() -> Unit
): EventHook<GameTickEvent> = handler<GameTickEvent>(dispatcher = dispatcher) { eventHandler() }

// 简化的 waitTicks 扩展
suspend fun Listenable.waitTicks(ticks: Int) {
    repeat(ticks) {
        suspendCancellableCoroutine { continuation ->
            var ticksRemaining = 1
            val tempListener = object : Listenable {
                override fun handleEvents() = true
            }
            lateinit var hook: EventHook<GameTickEvent>
            hook = tempListener.handler<GameTickEvent> {
                ticksRemaining--
                if (ticksRemaining <= 0) {
                    EventManager.unregisterEventHook(GameTickEvent::class.java, hook)
                    continuation.resume(Unit)
                }
            }
            continuation.invokeOnCancellation {
                EventManager.unregisterEventHook(GameTickEvent::class.java, hook)
            }
        }
    }
}

// 非挂起版本的 waitTicks - 使用回调方式
fun waitTicks(ticks: Int, onComplete: () -> Unit) {
    var ticksRemaining = ticks
    val tempListener = object : Listenable {
        override fun handleEvents() = true
    }

    lateinit var hook: EventHook<GameTickEvent>
    hook = tempListener.handler<GameTickEvent> {
        ticksRemaining--
        if (ticksRemaining <= 0) {
            EventManager.unregisterEventHook(GameTickEvent::class.java, hook)
            onComplete()
        }
    }
}

// 或者使用状态机版本（更适合 Grim 模式）
class TickWaiter {
    private var ticksRemaining = 0
    private var isWaiting = false
    private lateinit var hook: EventHook<GameTickEvent>

    fun waitTicks(ticks: Int, onComplete: () -> Unit) {
        if (isWaiting) return

        ticksRemaining = ticks
        isWaiting = true

        val tempListener = object : Listenable {
            override fun handleEvents() = true
        }

        hook = tempListener.handler<GameTickEvent> {
            if (isWaiting) {
                ticksRemaining--
                if (ticksRemaining <= 0) {
                    isWaiting = false
                    EventManager.unregisterEventHook(GameTickEvent::class.java, hook)
                    onComplete()
                }
            }
        }
    }

    fun cancel() {
        if (isWaiting) {
            isWaiting = false
            EventManager.unregisterEventHook(GameTickEvent::class.java, hook)
        }
    }
}