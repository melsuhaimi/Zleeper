package com.zleeper.sleepapp.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameAudioController @Inject constructor(@ApplicationContext private val context: Context) {
    private var ambience: MediaPlayer? = null

    fun playLoop(assetPath: String, volume: Float) {
        stop()
        val player = MediaPlayer()
        runCatching {
            context.assets.openFd("game/$assetPath").use { descriptor ->
                player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            player.isLooping = true
            player.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            player.setOnPreparedListener { it.start() }
            player.setOnErrorListener { failed, _, _ -> failed.release(); if (ambience === failed) ambience = null; true }
            ambience = player
            player.prepareAsync()
        }.onFailure {
            player.release()
            if (ambience === player) ambience = null
        }
    }

    fun setVolume(volume: Float) { ambience?.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f)) }
    fun stop() {
        ambience?.let { player -> runCatching { player.stop() }; player.release() }
        ambience = null
    }
}
