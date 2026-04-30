package net.ccbluex.liquidbounce.features.module.modules.test

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import kotlin.random.Random

object AutoBlock : Module("AutoBlock", category = Category.TEST) {
    private val blockChance by int("BlockChance", 100, 0..100, "%")
    private val blockRange by float("BlockRange", 3f, 0f..6f)
    private val swingProgress by float("SwingProgress", 0.3f, 0.0f..1.0f)
    private val rotationDiff by float("RotationDiff", 45f, 0.0f..180f)
    private val blockDuration by int("BlockDuration", 200, 50..500, "ms")

    private var blocking = false
    private var lastBlockTime = 0L
    private var blockStartTime = 0L
    private val lastSwingProgress = mutableMapOf<Entity, Float>()

    val onUpdate = handler<UpdateEvent> {
        // 检查是否可以格挡
        if (!canBlock()) {
            stopSwordBlock()
            return@handler
        }

        val target = KillAura.target ?: mc.pointedEntity ?: run {
            stopSwordBlock()
            return@handler
        }

        if (target !is EntityPlayer) {
            stopSwordBlock()
            return@handler
        }

        val currentTime = System.currentTimeMillis()

        // 如果正在格挡，检查是否需要停止
        if (blocking) {
            // 格挡时间结束或目标消失时停止
            if (currentTime - blockStartTime >= blockDuration ||
                mc.thePlayer.getDistanceToEntity(target) > blockRange + 1.0f) {
                stopSwordBlock()
            }
            return@handler
        }

        // 冷却检查（防止频繁触发）
        if (currentTime - lastBlockTime < 300) return@handler

        val currentProgress = target.swingProgress


        val rotationDifference = getFixedRotationDifference(target)

        if (mc.thePlayer.getDistanceToEntity(target) <= blockRange &&
            currentProgress <= swingProgress &&
            rotationDifference <= rotationDiff &&  // 使用修复后的检测
            Random.nextInt(100) < blockChance
        ) {
            startSwordBlock()
            lastBlockTime = currentTime
            blockStartTime = currentTime
        }
    }

    /**
     * 修复的旋转差异计算
     */
    private fun getFixedRotationDifference(target: EntityPlayer): Float {
        val player = mc.thePlayer
        val targetLook = target.getLook(1.0f)

        // 计算从目标看向玩家的向量
        val toPlayer = Vec3(
            player.posX - target.posX,
            player.posY + player.getEyeHeight() - (target.posY + target.getEyeHeight()),
            player.posZ - target.posZ
        ).normalize()

        // 计算两个向量的点积（余弦值）
        val dotProduct = targetLook.dotProduct(toPlayer)

        // 将点积转换为角度（弧度转角度）
        val angleRad = kotlin.math.acos(dotProduct.coerceIn(-1.0, 1.0))
        val angleDeg = Math.toDegrees(angleRad).toFloat()

        return angleDeg
    }

    private fun canBlock(): Boolean {
        val heldItem = mc.thePlayer.heldItem ?: return false
        return heldItem.item is net.minecraft.item.ItemSword
    }

    private fun startSwordBlock() {
        if (blocking) return

        sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        mc.gameSettings.keyBindUseItem.pressed = true
        blocking = true
    }

    private fun stopSwordBlock() {
        if (!blocking) return
        mc.gameSettings.keyBindUseItem.pressed = false
        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
        blocking = false
    }

    override fun onDisable() {
        stopSwordBlock()
        lastSwingProgress.clear()
    }
}