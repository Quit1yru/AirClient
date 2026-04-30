// MusicPlayer.kt - 支持 MP3 和 FLAC
package net.ccbluex.liquidbounce.utils.music

import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.Player
import org.jflac.sound.spi.FlacAudioFileReader
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class MusicPlayer {
    var isPlaying = false
    var isPaused = false
    var currentTrack: MusicFile? = null
    var progress: Float = 0f
    var currentTime: String = "0:00"
    var totalTime: String = "0:00"

    private val playlist = mutableListOf<MusicFile>()
    private var currentIndex = 0
    private var progressThread: Thread? = null
    private var audioPlayer: Any? = null
    private var playerThread: Thread? = null
    private var clip: Clip? = null

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        playlist.clear()
        playlist.addAll(MusicManager.musicFiles)

        if (playlist.isNotEmpty() && currentTrack == null) {
            currentTrack = playlist.first()
            totalTime = currentTrack?.durationFormatted ?: "0:00"
        }
    }

    fun play(trackName: String? = null) {
        println("MusicPlayer: play() called")

        if (trackName != null) {
            val track = MusicManager.getMusicFileByName(trackName)
            if (track != null) {
                currentTrack = track
                currentIndex = playlist.indexOfFirst { it.name == trackName }
            }
        }

        if (currentTrack == null && playlist.isNotEmpty()) {
            currentTrack = playlist.first()
            currentIndex = 0
        }

        if (currentTrack != null) {
            println("MusicPlayer: Playing track: ${currentTrack!!.name}")

            stopAudio()
            loadAndPlayAudio(currentTrack!!.file)
            isPlaying = true
            isPaused = false
            progress = 0f
            startProgressTracking()
        } else {
            println("MusicPlayer: No track available to play")
        }
    }

    fun pause() {
        println("MusicPlayer: pause() called")
        stopAudio()
        isPlaying = false
        isPaused = true
        stopProgressTracking()
    }

    fun stop() {
        println("MusicPlayer: stop() called")
        stopAudio()
        isPlaying = false
        isPaused = false
        progress = 0f
        currentTime = "0:00"
        stopProgressTracking()
    }

    fun next() {
        if (playlist.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % playlist.size
            currentTrack = playlist[currentIndex]
            totalTime = currentTrack?.durationFormatted ?: "0:00"
            stop()
            play()
        }
    }

    fun previous() {
        if (playlist.isNotEmpty()) {
            currentIndex = (currentIndex - 1 + playlist.size) % playlist.size
            currentTrack = playlist[currentIndex]
            totalTime = currentTrack?.durationFormatted ?: "0:00"
            stop()
            play()
        }
    }

    private fun loadAndPlayAudio(file: File) {
        try {
            val fileExtension = getFileExtension(file).lowercase()
            println("MusicPlayer: Loading file: ${file.name}, extension: $fileExtension")

            playerThread = Thread {
                try {
                    when (fileExtension) {
                        "mp3" -> playMp3(file)
                        "flac" -> playFlac(file)
                        else -> {
                            println("MusicPlayer: Unsupported format: $fileExtension")
                            // 尝试使用系统播放器
                            try {
                                playWithSystem(file)
                            } catch (e: Exception) {
                                println("System playback also failed: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Audio playback error: ${e.message}")
                    e.printStackTrace()
                }
            }

            playerThread?.isDaemon = true
            playerThread?.start()

        } catch (e: Exception) {
            println("Failed to load audio: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun playMp3(file: File) {
        try {
            val fileStream = FileInputStream(file)
            val bufferedStream = BufferedInputStream(fileStream)

            val player = Player(bufferedStream)
            audioPlayer = player

            println("MusicPlayer: Starting MP3 playback: ${file.name}")
            player.play()

            if (isPlaying) {
                next()
            }
        } catch (e: JavaLayerException) {
            println("MP3 playback error: ${e.message}")
            throw e
        }
    }

    private fun playFlac(file: File) {
        try {
            println("MusicPlayer: Attempting FLAC playback: ${file.name}")

            val audioInputStream = FlacAudioFileReader().getAudioInputStream(file)
            val clip = AudioSystem.getClip()
            this.clip = clip

            clip.open(audioInputStream)
            audioPlayer = clip

            println("MusicPlayer: Starting FLAC playback: ${file.name}")
            clip.start()

            // 监听播放结束
            clip.addLineListener { event ->
                if (event.type.toString().endsWith("STOP") && isPlaying) {
                    next()
                }
            }

        } catch (e: Exception) {
            println("FLAC playback error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun playWithSystem(file: File) {
        try {
            val audioInputStream = AudioSystem.getAudioInputStream(file)
            val clip = AudioSystem.getClip()
            this.clip = clip

            clip.open(audioInputStream)
            audioPlayer = clip

            println("MusicPlayer: Starting system playback: ${file.name}")
            clip.start()

            clip.addLineListener { event ->
                if (event.type.toString().endsWith("STOP") && isPlaying) {
                    next()
                }
            }

        } catch (e: Exception) {
            println("System playback error: ${e.message}")
            throw e
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
        clip = null
        playerThread?.interrupt()
        playerThread = null
    }

    private fun startProgressTracking() {
        stopProgressTracking()

        progressThread = Thread {
            while (isPlaying && !Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(1000)

                    if (isPlaying && currentTrack != null) {
                        updateProgress()
                        updateTimeDisplay()
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("Progress tracking error: ${e.message}")
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun updateProgress() {
        when (val player = audioPlayer) {
            is Clip -> {
                if (player.isRunning && player.frameLength > 0) {
                    progress = player.framePosition.toFloat() / player.frameLength
                }
            }
            else -> {
                // MP3 使用模拟进度
                val duration = currentTrack?.duration?.toFloat() ?: 1f
                if (duration > 0) {
                    progress += 1f / duration
                    if (progress >= 1f) {
                        progress = 0f
                    }
                }
            }
        }
    }

    private fun stopProgressTracking() {
        progressThread?.interrupt()
        progressThread = null
    }

    private fun updateTimeDisplay() {
        val totalSeconds = (progress * (currentTrack?.duration?.toFloat() ?: 0f)).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        currentTime = "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun getFileExtension(file: File): String {
        return file.name.substringAfterLast('.', "")
    }

    fun getPlaylist(): List<MusicFile> = playlist.toList()

    fun hasMusic(): Boolean = playlist.isNotEmpty()
}