package net.ccbluex.liquidbounce.features.module.modules.test

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.Minecraft

object TimerBalance : Module("TimerBalance", Category.TEST) {
    // 模块设置
    private val slowSpeed = float("SlowSpeed", 0.05f, 0.005f..0.5f)
    private val fastSpeed = float("FastSpeed", 20f, 5f..1000f)
    private val accumulateThreshold = int("Threshold", 32000, 1000..100000)
    private val resetOnDisable = BoolValue("ResetOnDisable", true)

    override val mc = Minecraft.getMinecraft()!!
    private var accumulatedTime = 0L
    private var isReleasing = false
    private var lastUpdateTime = System.currentTimeMillis()

    // 当模块启用时
    override fun onEnable() {
        accumulatedTime = 0L
        isReleasing = false
        lastUpdateTime = System.currentTimeMillis()
        mc.timer.timerSpeed = slowSpeed.get()
    }

    // 当模块禁用时
    override fun onDisable() {
        if (resetOnDisable.get()) {
            mc.timer.timerSpeed = 1.0f
        }
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastUpdateTime
        lastUpdateTime = currentTime

        if (isReleasing) {
            // 计算应该释放的时间量
            val timeToRelease = (deltaTime * fastSpeed.get()).toLong()
            accumulatedTime -= timeToRelease

            // 检查是否已经释放足够的时间
            if (accumulatedTime <= 0) {
                // 释放完成，直接关闭模块
                state = false
                return@handler
            }
        } else {
            // 计算实际经过的时间与游戏时间的差异
            val gameTimePassed = deltaTime * slowSpeed.get()
            val timeDifference = deltaTime - gameTimePassed

            accumulatedTime += timeDifference.toLong()

            // 检查是否达到阈值
            if (accumulatedTime >= accumulateThreshold.get()) {
                isReleasing = true
                mc.timer.timerSpeed = fastSpeed.get()
            }
        }
    }

    // 覆盖getTag方法以显示当前状态
    override val tag: String
        get() = "${if (isReleasing) "Releasing" else "Accumulating"} (${accumulatedTime}ms)"
}