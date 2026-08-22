package com.zleeper.sleepapp.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameAudioController @Inject constructor(@ApplicationContext private val context: Context) {
    private var music: MediaPlayer? = null
    private var ambience: MediaPlayer? = null
    private val sfxPlayers = mutableSetOf<MediaPlayer>()

    fun playRegion(musicAsset: String, ambienceAsset: String, musicVolume: Float, ambienceVolume: Float) {
        music = replaceLoop(music, musicAsset, musicVolume)
        ambience = replaceLoop(ambience, ambienceAsset, ambienceVolume)
    }

    fun setMusicVolume(value: Float) = music?.setVolume(value.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    fun setAmbienceVolume(value: Float) = ambience?.setVolume(value.coerceIn(0f, 1f), value.coerceIn(0f, 1f))

    fun playSfx(assetPath: String, volume: Float) {
        val player = newPlayer(assetPath, looping = false, volume = volume) ?: return
        sfxPlayers += player
        player.setOnCompletionListener { completed -> sfxPlayers.remove(completed); completed.release() }
        player.start()
    }

    fun stopLoops() {
        music = release(music)
        ambience = release(ambience)
    }

    fun stopAll() {
        stopLoops()
        sfxPlayers.toList().forEach { release(it) }
        sfxPlayers.clear()
    }

    private fun replaceLoop(current: MediaPlayer?, assetPath: String, volume: Float): MediaPlayer? {
        release(current)
        return newPlayer(assetPath, looping = true, volume = volume)?.also { it.start() }
    }

    private fun newPlayer(assetPath: String, looping: Boolean, volume: Float): MediaPlayer? {
        val player = MediaPlayer()
        return runCatching {
            context.assets.openFd("game/$assetPath").use { descriptor ->
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            player.isLooping = looping
            val safeVolume = volume.coerceIn(0f, 1f)
            player.setVolume(safeVolume, safeVolume)
            player.prepare()
            player
        }.getOrElse { player.release(); null }
    }

    private fun release(player: MediaPlayer?): MediaPlayer? {
        if (player != null) {
            runCatching { player.stop() }
            player.release()
        }
        return null
    }
}
