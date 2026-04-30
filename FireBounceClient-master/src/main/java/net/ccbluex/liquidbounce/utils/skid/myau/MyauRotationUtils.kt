package net.ccbluex.liquidbounce.utils.skid.myau

import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.mc
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.*

// 自定义 RotationUtils
object MyauRotationUtils {
    private var currentRotation: Rotation? = null
    private var rotationTimer = MSTimer()
    private var currentPriority = 0
    private var keepLength = 0L
    private var rotationType = RotationType.SILENT

    enum class RotationType {
        LEGIT,      // 直接设置玩家视角
        SILENT,     // 静默转头，不直接设置玩家视角
        CLIENT      // 客户端转头，直接设置玩家视角但不发送数据包
    }

    /**
     * 设置目标转头
     * @param rotation 目标角度
     * @param priority 优先级 (越高越优先)
     * @param keepLength 保持时间 (毫秒)
     * @param rotationType 转头类型
     */
    fun setTargetRotation(
        yaw: Float,
        pitch: Float,
        priority: Int = 1,
        keepLength: Long = 0,
        rotationType: RotationType = RotationType.SILENT
    ) {
        setTargetRotation(Rotation(yaw, pitch), priority, keepLength, rotationType)
    }

    /**
     * 设置目标转头（原有方法）
     */
    fun setTargetRotation(
        rotation: Rotation,
        priority: Int = 1,
        keepLength: Long = 0,
        rotationType: RotationType = RotationType.SILENT
    ) {
        // 检查优先级，只有更高优先级的转头才能覆盖当前转头
        if (priority < currentPriority && currentRotation != null) {
            return
        }

        currentRotation = rotation.fixedSensitivity()
        currentPriority = priority
        this.keepLength = keepLength
        this.rotationType = rotationType
        rotationTimer.reset()

        // 如果是 LEGIT 或 CLIENT 类型，立即应用转头
        when (rotationType) {
            RotationType.LEGIT, RotationType.CLIENT -> {
                mc.thePlayer.rotationYaw = rotation.yaw
                mc.thePlayer.rotationPitch = rotation.pitch
            }
            RotationType.SILENT -> {
                // 静默转头不立即设置玩家视角
            }
        }
    }

    /**
     * 获取当前目标转头
     */
    fun getTargetRotation(): Rotation? {
        return currentRotation
    }

    /**
     * 获取当前转头类型
     */
    fun getRotationType(): RotationType {
        return rotationType
    }

    /**
     * 检查是否有活跃的转头
     */
    fun isRotating(): Boolean {
        return currentRotation != null && !rotationTimer.hasTimePassed(keepLength)
    }

    /**
     * 强制设置转头（忽略优先级）
     */
    fun forceTargetRotation(rotation: Rotation, rotationType: RotationType = RotationType.SILENT) {
        currentRotation = rotation.fixedSensitivity()
        currentPriority = Int.MAX_VALUE
        this.keepLength = 0L
        this.rotationType = rotationType
        rotationTimer.reset()

        when (rotationType) {
            RotationType.LEGIT, RotationType.CLIENT -> {
                mc.thePlayer.rotationYaw = rotation.yaw
                mc.thePlayer.rotationPitch = rotation.pitch
            }
            RotationType.SILENT -> {
                // 静默转头不立即设置玩家视角
            }
        }
    }

    /**
     * 更新转头（在客户端Tick事件中调用）
     */
    fun updateRotations() {
        currentRotation?.let { rotation ->
            // 检查转头是否过期
            if (rotationTimer.hasTimePassed(keepLength)) {
                currentRotation = null
                currentPriority = 0
                return
            }

            // 根据转头类型处理
            when (rotationType) {
                RotationType.SILENT -> {
                    // 静默转头：不直接设置玩家视角，但可以通过其他方式处理
                    // 这里可以添加静默转头的逻辑，比如在特定时机应用
                }
                RotationType.CLIENT -> {
                    // 客户端转头：直接设置玩家视角但不发送数据包
                    mc.thePlayer.rotationYaw = rotation.yaw
                    mc.thePlayer.rotationPitch = rotation.pitch
                }
                RotationType.LEGIT -> {
                    // 合法转头：平滑过渡到目标角度
                    val currentYaw = mc.thePlayer.rotationYaw
                    val currentPitch = mc.thePlayer.rotationPitch

                    val yawDiff = MathHelper.wrapAngleTo180_float(rotation.yaw - currentYaw)
                    val pitchDiff = MathHelper.wrapAngleTo180_float(rotation.pitch - currentPitch)

                    // 平滑过渡
                    val smoothFactor = 0.3f
                    val newYaw = currentYaw + yawDiff * smoothFactor
                    val newPitch = currentPitch + pitchDiff * smoothFactor

                    mc.thePlayer.rotationYaw = newYaw
                    mc.thePlayer.rotationPitch = newPitch
                }
            }
        }
    }

    /**
     * 应用静默转头到数据包（在发送数据包时调用）
     */
    fun applySilentRotation(yaw: Float, pitch: Float): FloatArray {
        currentRotation?.let { rotation ->
            if (rotationType == RotationType.SILENT && isRotating()) {
                return floatArrayOf(rotation.yaw, rotation.pitch)
            }
        }
        return floatArrayOf(yaw, pitch)
    }

    /**
     * 重置转头系统
     */
    fun reset() {
        currentRotation = null
        currentPriority = 0
        keepLength = 0L
        rotationType = RotationType.SILENT
        rotationTimer.reset()
    }

    /**
     * 重置转头但保留优先级
     */
    fun softReset() {
        currentRotation = null
        keepLength = 0L
        rotationTimer.reset()
    }

    // 原有的工具函数保持不变
    fun getRotationDifference(entity: Entity): Float {
        val rotations = getRotationsToEntity(entity)
        val currentYaw = mc.thePlayer.rotationYaw
        val currentPitch = mc.thePlayer.rotationPitch
        val yawDiff = abs(MathHelper.wrapAngleTo180_float(rotations.yaw - currentYaw))
        val pitchDiff = abs(rotations.pitch - currentPitch)
        return sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff)
    }

    fun getRotationDifference(yaw1: Float, pitch1: Float, yaw2: Float, pitch2: Float): Float {
        val yawDiff = abs(MathHelper.wrapAngleTo180_float(yaw1 - yaw2))
        val pitchDiff = abs(pitch1 - pitch2)
        return sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff)
    }

    fun getRotationsToEntity(entity: Entity): Rotation {
        val posX = entity.posX
        val posY = entity.posY + entity.eyeHeight
        val posZ = entity.posZ
        return getRotationsToPos(posX, posY, posZ)
    }

    fun getRotationsToPos(posX: Double, posY: Double, posZ: Double): Rotation {
        val x = posX - mc.thePlayer.posX
        val y = posY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight())
        val z = posZ - mc.thePlayer.posZ

        val dist = sqrt(x * x + z * z).toFloat()
        var yaw = (atan2(z, x) * 180.0 / Math.PI).toFloat() - 90.0f
        var pitch = (-(atan2(y, dist.toDouble()) * 180.0 / Math.PI)).toFloat()

        yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw)
        pitch = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)

        return Rotation(yaw, pitch.coerceIn(-90f, 90f))
    }

    fun getRotationsToBox(box: AxisAlignedBB, currentYaw: Float, currentPitch: Float, step: Float, smoothing: Float): Rotation {
        val centerX = (box.minX + box.maxX) / 2.0
        val centerY = (box.minY + box.maxY) / 2.0
        val centerZ = (box.minZ + box.maxZ) / 2.0

        var yaw = getRotationYaw(centerX, centerZ)
        var pitch = getRotationPitch(centerX, centerY, centerZ)

        // Apply step
        if (step > 0) {
            val yawSteps = (abs(yaw - currentYaw) / step).toInt()
            val pitchSteps = (abs(pitch - currentPitch) / step).toInt()

            yaw = if (yaw > currentYaw) currentYaw + min(yaw - currentYaw, step * yawSteps)
            else currentYaw - min(currentYaw - yaw, step * yawSteps)

            pitch = if (pitch > currentPitch) currentPitch + min(pitch - currentPitch, step * pitchSteps)
            else currentPitch - min(currentPitch - pitch, step * pitchSteps)
        }

        // Apply smoothing
        if (smoothing > 0) {
            yaw = currentYaw + (yaw - currentYaw) * smoothing
            pitch = currentPitch + (pitch - currentPitch) * smoothing
        }

        return Rotation(yaw, pitch.coerceIn(-90f, 90f))
    }

    private fun getRotationYaw(posX: Double, posZ: Double): Float {
        val x = posX - mc.thePlayer.posX
        val z = posZ - mc.thePlayer.posZ
        return (atan2(z, x) * 180.0 / Math.PI).toFloat() - 90.0f
    }

    private fun getRotationPitch(posX: Double, posY: Double, posZ: Double): Float {
        val x = posX - mc.thePlayer.posX
        val y = posY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight())
        val z = posZ - mc.thePlayer.posZ
        val dist = sqrt(x * x + z * z).toFloat()
        return (-(atan2(y, dist.toDouble()) * 180.0 / Math.PI)).toFloat()
    }
}

data class Rotation(var yaw: Float, var pitch: Float) {
    fun fixedSensitivity(): Rotation {
        return Rotation(yaw, pitch.coerceIn(-90f, 90f))
    }

    fun toDirectionVector(): Vec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val x = -sin(yawRad) * cos(pitchRad)
        val y = -sin(pitchRad)
        val z = cos(yawRad) * cos(pitchRad)
        return Vec3(x, y, z).normalize()
    }

    fun applySmoothing(current: Rotation, smoothing: Float): Rotation {
        if (smoothing <= 0) return this
        val smoothFactor = smoothing / 100f
        val newYaw = current.yaw + (yaw - current.yaw) * smoothFactor
        val newPitch = current.pitch + (pitch - current.pitch) * smoothFactor
        return Rotation(newYaw, newPitch).fixedSensitivity()
    }

    fun applyStep(current: Rotation, step: Float): Rotation {
        if (step <= 0) return this

        val yawDiff = MathHelper.wrapAngleTo180_float(yaw - current.yaw)
        val pitchDiff = pitch - current.pitch

        val maxYawStep = min(abs(yawDiff), step)
        val maxPitchStep = min(abs(pitchDiff), step)

        val newYaw = current.yaw + (if (yawDiff > 0) maxYawStep else -maxYawStep)
        val newPitch = current.pitch + (if (pitchDiff > 0) maxPitchStep else -maxPitchStep)

        return Rotation(newYaw, newPitch).fixedSensitivity()
    }
}