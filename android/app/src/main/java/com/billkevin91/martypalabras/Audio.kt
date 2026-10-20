package com.billkevin91.martypalabras

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.sin

/** Pitidos del juego: notas de onda triangular que se apagan solas. */
class Sound {
    private val worker = Executors.newCachedThreadPool()
    private val rate = 22050

    fun tone(vararg freqs: Int) {
        worker.execute {
            try {
                val step = 0.12
                val len = 0.26
                val total = ((freqs.size - 1) * step + len) * rate
                val pcm = ShortArray(total.toInt() + 1)
                freqs.forEachIndexed { i, f ->
                    val start = (i * step * rate).toInt()
                    val n = (len * rate).toInt()
                    for (j in 0 until n) {
                        val t = j.toDouble() / rate
                        val tri = 2 / PI * asin(sin(2 * PI * f * t))
                        val g = 0.15 * exp(-t * 18)
                        val k = start + j
                        if (k < pcm.size) pcm[k] = (pcm[k] + tri * g * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    }
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((pcm.size * 1000L / rate) + 60)
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}

/** Voz del teléfono para dictar las palabras. */
class Speech(context: Context) {
    var ready = false
        private set
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) setup()
    }

    private fun setup() {
        val options = listOf(Locale("es", "ES"), Locale("es", "MX"), Locale("es", "US"), Locale("es"))
        for (loc in options) {
            val r = tts.setLanguage(loc)
            if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setSpeechRate(0.85f)
                ready = true
                return
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "w")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}
