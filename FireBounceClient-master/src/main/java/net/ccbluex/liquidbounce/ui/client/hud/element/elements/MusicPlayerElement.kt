// MusicPlayerElement.kt - 修复版本
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import javazoom.jl.player.Player
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extra.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.music.GlobalMusicManager
import net.ccbluex.liquidbounce.utils.music.MusicFile
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import java.awt.Color
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

@ElementInfo(name = "MusicPlayer")
class MusicPlayerElement : Element("MusicPlayer") {

    // 样式设置
    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..5F)
    private val borderStrength by float("Border-Strength", 2F, 1F..5F)
    private val backgroundColor by color("Background-Color", Color.BLACK.withAlpha(150))
    private val borderColor by color("Border-Color", Color.BLACK)
    private val textColor by color("TextColor", Color.WHITE)
    private val progressColor by color("ProgressColor", Color(255, 106, 0))
    private val lyricColor by color("LyricColor", Color(255, 215, 0))
    private val noLyricColor by color("NoLyricColor", Color(128, 128, 128))
    private val textShadow by boolean("TextShadow", false)

    // 显示设置
    private val showProgressBar by boolean("ShowProgressBar", true)
    private val showTime by boolean("ShowTime", true)
    private val showLyrics by boolean("ShowLyrics", true)
    private val compactMode by boolean("CompactMode", false)
    private val autoHide by boolean("AutoHide", false)

    // 状态变量
    private var width = 0f
    private var height = 0f
    private var alphaBackground = 0
    private var alphaBorder = 0
    private var alphaText = 0
    private var audioPlayer: Any? = null
    var volume: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateVolume()
        }

    // 歌词相关
    private var currentLyrics: List<LyricLine> = emptyList()
    private var currentLyricFile: String? = null

    private val isRendered get() = width > 0f || height > 0f
    private val isAlpha get() = alphaBorder > 0 || alphaBackground > 0 || alphaText > 0

    override fun drawElement(): Border {
        val musicPlayer = GlobalMusicManager.musicPlayer
        val currentTrack = musicPlayer.currentTrack
        val isPlaying = musicPlayer.isPlaying
        val progress = musicPlayer.progress

        // 歌词加载
        if (currentTrack != null && showLyrics) {
            checkAndLoadLyrics(currentTrack)
        } else {
            currentLyrics = emptyList()
            currentLyricFile = null
        }

        // 计算尺寸
        val baseWidth = if (compactMode) 140f else 180f
        var baseHeight = if (compactMode) 36f else 44f

        if (showLyrics && currentTrack != null) {
            baseHeight += if (compactMode) 12f else 16f
        }

        val shouldRender = currentTrack != null && (!autoHide || isPlaying)

        if (shouldRender) {
            width = baseWidth
            height = baseHeight
            alphaBackground = backgroundColor.alpha
            alphaBorder = borderColor.alpha
            alphaText = textColor.alpha
        } else {
            width = 0f
            height = 0f
            alphaBackground = 0
            alphaBorder = 0
            alphaText = 0
        }

        if (isRendered && isAlpha) {
            val renderWidth = width.coerceAtLeast(0F)
            val renderHeight = height.coerceAtLeast(0F)

            drawRoundedBorderRect(
                0F, 0F, renderWidth, renderHeight, borderStrength,
                backgroundColor.withAlpha(alphaBackground).rgb,
                borderColor.withAlpha(alphaBorder).rgb,
                roundedRectRadius
            )

            if (currentTrack != null) {
                drawMusicContent(currentTrack, isPlaying, progress, renderWidth, renderHeight)
            }
        }

        return Border(0F, 0F, baseWidth, baseHeight)
    }

    private fun drawMusicContent(track: MusicFile, isPlaying: Boolean, progress: Float, width: Float, height: Float) {
        val textCustomColor = textColor.withAlpha(alphaText).rgb
        val lyricCustomColor = lyricColor.withAlpha(alphaText).rgb
        val noLyricCustomColor = noLyricColor.withAlpha(alphaText).rgb

        var currentY = if (compactMode) 8f else 10f

        // 绘制歌曲名称
        val trackName = if (compactMode && track.displayName.length > 20) {
            "${track.displayName.substring(0, 18)}.."
        } else {
            track.displayName
        }

        Fonts.fontRegular40.drawString(trackName, 8f, currentY, textCustomColor, textShadow)

        // 绘制播放状态图标
        val statusIcon = if (isPlaying) "▶" else "❚❚"
        val statusX = width - 20f
        Fonts.fontRegular40.drawString(statusIcon, statusX, currentY, textCustomColor, textShadow)

        currentY += if (compactMode) 10f else 12f

        // 绘制歌词
        if (showLyrics) {
            val currentLyric = getCurrentLyric()
            val displayLyric = currentLyric.ifEmpty { "无歌词" }

            val lyricToShow = if (compactMode && displayLyric.length > 20) {
                "${displayLyric.substring(0, 18)}.."
            } else {
                displayLyric
            }

            val lyricColorToUse = if (currentLyric.isNotEmpty()) lyricCustomColor else noLyricCustomColor
            Fonts.fontRegular35.drawString(lyricToShow, 8f, currentY, lyricColorToUse, textShadow)
            currentY += if (compactMode) 8f else 10f
        }

        // 绘制时间信息
        if (showTime) {
            val timeText = "${GlobalMusicManager.musicPlayer.currentTime} / ${track.durationFormatted}"
            Fonts.fontRegular35.drawString(timeText, 8f, currentY, textCustomColor, textShadow)
            currentY += if (compactMode) 8f else 10f
        }

        // 绘制进度条
        if (showProgressBar) {
            val progressBarY = if (compactMode) height - 6f else height - 8f
            val progressBarHeight = if (compactMode) 4f else 6f
            val progressBarStart = 8f
            val progressBarTotal = width - 16f
            val currentWidth = progress.coerceIn(0F, 1F) * progressBarTotal

            drawRoundedRect(
                progressBarStart, progressBarY,
                progressBarStart + progressBarTotal, progressBarY + progressBarHeight,
                Color.BLACK.rgb, 3F
            )

            if (currentWidth > 0) {
                drawRoundedRect(
                    progressBarStart, progressBarY,
                    progressBarStart + currentWidth, progressBarY + progressBarHeight,
                    progressColor.withAlpha(alphaText).rgb, 3F
                )
            }
        }
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y)) {
            when (mouseButton) {
                0 -> { // 左键点击 - 播放/暂停
                    if (GlobalMusicManager.isPlaying) {
                        GlobalMusicManager.pause()
                    } else {
                        GlobalMusicManager.play()
                    }
                }
                1 -> { // 右键点击 - 下一曲
                    GlobalMusicManager.next()
                    currentLyrics = emptyList()
                    currentLyricFile = null
                }
            }
        }
    }

    // 歌词相关方法
    private fun checkAndLoadLyrics(track: MusicFile) {
        val baseName = track.displayName.replace(Regex("\\.[^.]+$"), "")

        if (currentLyricFile == baseName && currentLyrics.isNotEmpty()) {
            return
        }

        val lyricFile = findLyricFile(baseName)
        if (lyricFile != null) {
            currentLyrics = parseLyricFile(lyricFile)
            currentLyricFile = baseName
        } else {
            currentLyrics = emptyList()
            currentLyricFile = null
        }
    }

    private fun findLyricFile(baseName: String): File? {
        val lyricExtensions = arrayOf("lrc", "txt")
        val musicDir = File(".")

        for (extension in lyricExtensions) {
            val lyricFile = File(musicDir, "$baseName.$extension")
            if (lyricFile.exists() && lyricFile.isFile) {
                return lyricFile
            }
        }
        return null
    }

    private fun parseLyricFile(file: File): List<LyricLine> {
        val lyrics = mutableListOf<LyricLine>()
        try {
            val reader = InputStreamReader(file.inputStream(), Charset.forName("UTF-8"))
            reader.use { r ->
                r.readLines().forEach { line ->
                    parseLyricLine(line)?.let { lyricLine ->
                        lyrics.add(lyricLine)
                    }
                }
            }
            lyrics.sortBy { it.timeInSeconds }
        } catch (_: Exception) {
            // 解析失败，返回空列表
        }
        return lyrics
    }

    private fun parseLyricLine(line: String): LyricLine? {
        if (line.trim().isEmpty()) return null

        val pattern = Regex("\\[(\\d+):(\\d+)\\.(\\d+)](.+)")
        val match = pattern.find(line)

        if (match != null) {
            val (minutes, seconds, milliseconds, text) = match.destructured
            try {
                val timeInSeconds = minutes.toInt() * 60.0 + seconds.toInt() + milliseconds.toInt() / 100.0
                return LyricLine(timeInSeconds, text.trim())
            } catch (_: NumberFormatException) {
            }
        }

        val pattern2 = Regex("\\[(\\d+):(\\d+)](.+)")
        val match2 = pattern2.find(line)

        if (match2 != null) {
            val (minutes, seconds, text) = match2.destructured
            try {
                val timeInSeconds = minutes.toInt() * 60.0 + seconds.toInt()
                return LyricLine(timeInSeconds, text.trim())
            } catch (_: NumberFormatException) {
            }
        }

        return null
    }

    private fun getCurrentLyric(): String {
        if (currentLyrics.isEmpty()) return ""

        val currentTimeInSeconds = getCurrentTimeInSeconds()
        var currentLyric = ""

        for (i in currentLyrics.indices) {
            val lyric = currentLyrics[i]
            if (lyric.timeInSeconds <= currentTimeInSeconds) {
                currentLyric = lyric.text
                if (i + 1 < currentLyrics.size) {
                    val nextLyric = currentLyrics[i + 1]
                    if (nextLyric.timeInSeconds - currentTimeInSeconds < 2.0) {
                        currentLyric = nextLyric.text
                    }
                }
            } else {
                break
            }
        }

        return currentLyric
    }

    private fun getCurrentTimeInSeconds(): Float {
        return try {
            val timeStr = GlobalMusicManager.musicPlayer.currentTime
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                parts[0].toFloat() * 60 + parts[1].toFloat()
            } else {
                0f
            }
        } catch (_: Exception) {
            0f
        }
    }
    private fun updateVolume() {
        when (val player = audioPlayer) {
            is Clip -> {
                setClipVolume(player, volume)
            }
            // MP3 播放器音量控制比较复杂，需要额外处理
        }
    }
    private fun setClipVolume(clip: Clip, volume: Float) {
        try {
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val dB = (log10(volume.toDouble()) * 20.0).toFloat()
            gainControl.value = dB.coerceIn(gainControl.minimum, gainControl.maximum)
        } catch (e: Exception) {
            println("Volume control not supported: ${e.message}")
        }
    }
    private fun stopAudio() {
        when (val player = audioPlayer) {
            is Player -> {
                player.close()
            }
            is Clip -> {
                player.stop()
                player.close()
            }
        }
        audioPlayer = null

    }
    data class LyricLine(val timeInSeconds: Double, val text: String)
}