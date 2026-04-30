package net.ccbluex.liquidbounce.features.module.modules.dev

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager
import java.io.File
import java.io.FileWriter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object AimDataCollector : Module(
    "AimDataCollector",
    Category.DEV,
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dataFile = File(FileManager.dir, "aim_path_data.jsonl")

    // 记录完整的转头路径
    private val pathData = mutableListOf<PathPoint>()
    private var isRecording = false
    private var startTime = 0L

    // 路径点数据类
    data class PathPoint(
        val timestamp: Long,          // 相对时间（毫秒）
        val currentYaw: Float,        // 当前Yaw角度
        val currentPitch: Float,      // 当前Pitch角度
        val targetYaw: Float,         // 目标Yaw角度
        val targetPitch: Float,       // 目标Pitch角度
        val yawDelta: Float,          // Yaw角度差
        val pitchDelta: Float,        // Pitch角度差
        val dx: Float,                // X轴距离
        val dy: Float,                // Y轴距离
        val dz: Float,                // Z轴距离
        val dist: Float               // 水平距离
    )

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val target = mc.pointedEntity ?: return@handler

        // 计算玩家和目标之间的相对位置和距离
        val dx = target.posX - player.posX
        val dy = target.posY + target.eyeHeight - (player.posY + player.eyeHeight)
        val dz = target.posZ - player.posZ
        val dist = sqrt(dx * dx + dz * dz)

        // 计算目标的目标Yaw和Pitch
        val targetYaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f
        val targetPitch = -Math.toDegrees(atan2(dy, dist)).toFloat()

        val currentYaw = player.rotationYaw
        val currentPitch = player.rotationPitch

        val yawDelta = targetYaw - currentYaw
        val pitchDelta = targetPitch - currentPitch

        // 规范化角度差（处理360度边界）
        val normalizedYawDelta = normalizeAngle(yawDelta)
        val normalizedPitchDelta = normalizeAngle(pitchDelta)

        // 开始记录条件：角度偏差较大时开始记录路径
        if (!isRecording && (abs(normalizedYawDelta) > 5f || abs(normalizedPitchDelta) > 2f)) {
            isRecording = true
            startTime = System.currentTimeMillis()
            pathData.clear()
            // 记录第一个点（开始点）
            recordPathPoint(
                currentYaw, currentPitch, targetYaw, targetPitch,
                normalizedYawDelta, normalizedPitchDelta,
                dx, dy, dz, dist
            )
        }

        // 如果正在记录，继续添加路径点
        if (isRecording) {
            recordPathPoint(
                currentYaw, currentPitch, targetYaw, targetPitch,
                normalizedYawDelta, normalizedPitchDelta,
                dx, dy, dz, dist
            )

            // 结束记录条件：接近目标或超时（5秒）
            val relativeTime = System.currentTimeMillis() - startTime
            if ((abs(normalizedYawDelta) < 1f && abs(normalizedPitchDelta) < 0.5f) || relativeTime > 5000L) {
                savePathData()
                isRecording = false
            }
        }
    }

    /**
     * 记录单个路径点
     */
    private fun recordPathPoint(
        currentYaw: Float, currentPitch: Float,
        targetYaw: Float, targetPitch: Float,
        yawDelta: Float, pitchDelta: Float,
        dx: Double, dy: Double, dz: Double, dist: Double
    ) {
        val relativeTime = System.currentTimeMillis() - startTime

        pathData.add(PathPoint(
            timestamp = relativeTime,
            currentYaw = currentYaw,
            currentPitch = currentPitch,
            targetYaw = targetYaw,
            targetPitch = targetPitch,
            yawDelta = yawDelta,
            pitchDelta = pitchDelta,
            dx = dx.toFloat(),
            dy = dy.toFloat(),
            dz = dz.toFloat(),
            dist = dist.toFloat()
        ))
    }

    /**
     * 保存路径数据到文件
     */
    private fun savePathData() {
        if (pathData.isEmpty()) return

        val pathRecord = mapOf(
            "path_id" to System.currentTimeMillis(),
            "start_time" to startTime,
            "duration" to pathData.last().timestamp,
            "point_count" to pathData.size,
            "points" to pathData
        )

        val jsonLine = gson.toJson(pathRecord)

        try {
            FileWriter(dataFile, true).use { fw ->
                fw.write(jsonLine + "\n")
                fw.flush()
            }
            // 可选：在聊天栏显示记录信息
            // mc.thePlayer?.addChatMessage("§a记录转头路径完成：${pathData.size}个点，时长${pathData.last().timestamp}ms")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pathData.clear()
    }

    /**
     * 规范化角度到 -180 到 180 度范围内
     */
    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        return normalized
    }

    /**
     * 模块禁用时保存未完成的数据
     */
    override fun onDisable() {
        if (isRecording && pathData.isNotEmpty()) {
            savePathData()
        }
        isRecording = false
        super.onDisable()
    }
}