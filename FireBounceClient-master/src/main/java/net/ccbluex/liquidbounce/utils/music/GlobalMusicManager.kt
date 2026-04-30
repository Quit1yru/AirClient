package net.ccbluex.liquidbounce.utils.music

object GlobalMusicManager {
    val musicPlayer = MusicPlayer()

    init {
        println("GlobalMusicManager initialized")
    }

    val isPlaying get() = musicPlayer.isPlaying
    val currentTrack get() = musicPlayer.currentTrack

    fun play() = musicPlayer.play()
    fun pause() = musicPlayer.pause()
    fun stop() = musicPlayer.stop()
    fun next() = musicPlayer.next()
    fun previous() = musicPlayer.previous()
}