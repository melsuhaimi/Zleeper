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
        val descriptor = context.assets.openFd("game/$assetPath")
        ambience = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            descriptor.close()
            isLooping = true
            setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            prepare()
            start()
        }
    }

    fun setVolume(volume: Float) { ambience?.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f)) }
    fun stop() { ambience?.run { if (isPlaying) stop(); release() }; ambience = null }
}
