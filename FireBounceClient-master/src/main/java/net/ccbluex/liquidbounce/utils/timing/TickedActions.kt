/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.async.waitUntil
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.updateClientWindow
import net.minecraft.item.ItemStack
import java.util.concurrent.ConcurrentLinkedQueue

object TickedActions : Listenable {
    private class Action(
        val owner: Module,
        val id: Int,
        val action: Runnable
    )

    private val actions = ConcurrentLinkedQueue<Action>()
    private val calledThisTick = LinkedHashSet<Action>()

    var setIcon = false
    private var tickCounter = 0
    private const val TITLE_UPDATE_INTERVAL = 20 // 每20个tick更新一次（约1秒）

    private val onTick = handler<GameTickEvent>(priority = 1) {
        // 更新计数器
        tickCounter++

        // 定期更新窗口标题以显示游玩时间
        if (tickCounter % TITLE_UPDATE_INTERVAL == 0) {
            updateClientWindow()
        }

        // Prevent new scheduled ids from getting marked as duplicates even if they are going to be called next tick
        actions.toCollection(calledThisTick)

        calledThisTick.forEach {
            it.action.run()
            if (actions.isNotEmpty()) {
                actions.remove()
            }
        }

        calledThisTick.clear()
    }

    private val onWorld = handler<WorldEvent> {
        actions.clear()
    }
    private val onStart = handler<StartupEvent> {
        setIcon = true
    }
    private val onSecond = handler<SecondTickEvent> {
        updateClientWindow()
    }

    /**
     * Perform window click with given parameters at next tick and run callback with click result.
     */
    inline fun Module.clickNextTick(
        slot: Int, button: Int, mode: Int,
        allowDuplicates: Boolean = false, windowId: Int = mc.thePlayer.openContainer.windowId,
        crossinline callback: (ItemStack?) -> Unit = {}
    ) = nextTick(slot, allowDuplicates) {
        val newStack = mc.playerController?.windowClick(windowId, slot, button, mode, mc.thePlayer)
        callback.invoke(newStack)
    }

    fun Module.nextTick(id: Int = -1, allowDuplicates: Boolean = true, action: Runnable) =
        schedule(id, this, allowDuplicates, action)

    suspend fun Module.awaitTicked() {
        waitUntil { hasNoTicked() }
    }

    fun Module.isTicked(id: Int) = isScheduled(id, this)

    fun Module.clearTicked() = clear(this)

    fun Module.countTicked() = size(this)

    fun Module.hasNoTicked() = isEmpty(this)

    // 私有辅助方法
    private fun schedule(id: Int, module: Module, allowDuplicates: Boolean = false, action: Runnable) =
        if (allowDuplicates || !isScheduled(id, module)) {
            actions += Action(module, id, action)
            true
        } else false

    private fun isScheduled(id: Int, module: Module) =
        actions.any { it.owner == module && it.id == id && it !in calledThisTick }

    private fun clear(module: Module) = actions.removeIf { it.owner == module }

    private fun size(module: Module) = actions.count { it.owner == module }

    private fun isEmpty(module: Module) = size(module) == 0
}
