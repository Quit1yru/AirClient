// MusicManager.kt
package net.ccbluex.liquidbounce.utils.music

import net.ccbluex.liquidbounce.file.FileManager
import java.io.File
import javax.sound.sampled.AudioSystem

object MusicManager {

    // 支持的音频格式
    private val SUPPORTED_FORMATS = arrayOf("mp3", "wav", "ogg", "flac", "aac", "m4a")

    // 获取所有音乐文件
    val musicFiles: List<MusicFile>
        get() = scanMusicFiles()

    private fun createReadmeFile() {
        try {
            val readme = File(FileManager.musicDir, "README.txt")
            if (!readme.exists()) {
                readme.writeText("""
                    FireBounce Music Folder
                    ======================
                    
                    Supported formats: ${SUPPORTED_FORMATS.joinToString(", ")}
                    
                    How to use:
                    1. Place your music files in this folder
                    2. Supported formats: MP3, WAV, OGG, FLAC, AAC, M4A
                    3. Files will be automatically detected and added to playlist
                    4. Use the music player in main menu to control playback
                    
                    Enjoy your music!
                """.trimIndent())
            }
        } catch (e: Exception) {
            println("Failed to create music readme: ${e.message}")
        }
    }

    fun scanMusicFiles(): List<MusicFile> {
        createReadmeFile()
        return try {
            FileManager.musicDir.listFiles { file ->
                file.isFile && SUPPORTED_FORMATS.any { format ->
                    file.name.endsWith(".$format", ignoreCase = true)
                }
            }?.map { file ->
                MusicFile(
                    name = file.nameWithoutExtension,
                    file = file,
                    format = getFileFormat(file),
                    duration = getAudioDuration(file)
                )
            }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            println("Failed to scan music files: ${e.message}")
            emptyList()
        }
    }

    private fun getFileFormat(file: File): String {
        return file.extension.lowercase()
    }

    private fun getAudioDuration(file: File): Int {
        return try {
            when (file.extension.lowercase()) {
                "mp3" -> {
                    // MP3 时长估计（基于文件大小和比特率）
                    val fileSizeKB = file.length() / 1024
                    // 假设平均比特率 128 kbps
                    (fileSizeKB / (128 / 8)).toInt()
                }
                else -> {
                    // 其他格式使用原来的方法
                    val audioInputStream = AudioSystem.getAudioInputStream(file)
                    val format = audioInputStream.format
                    val frames = audioInputStream.frameLength
                    (frames / format.frameRate).toInt()
                }
            }
        } catch (_: Exception) {
            // 如果无法获取准确时长，返回估计值
            (file.length() / 1024 / 128).toInt() // 粗略估计
        }
    }

    fun getMusicFileByName(name: String): MusicFile? {
        return musicFiles.find { it.name.equals(name, ignoreCase = true) }
    }

    fun reloadMusicFiles() {
        scanMusicFiles()
    }
}

// MusicFile 数据类 - 添加到这里
data class MusicFile(
    val name: String,
    val file: File,
    val format: String,
    val duration: Int
) {
    val displayName: String
        get() = name

    val durationFormatted: String
        get() {
            val minutes = duration / 60
            val seconds = duration % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}