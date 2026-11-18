package com.billkevin91.martypalabras

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.sin

/** Efectos de sonido sintetizados (no hacen falta archivos de audio). */
enum class Sfx { POP, BOING, WHOOSH, CLICK }

/** Pitidos y efectos del juego, generados al momento con ondas simples. */
class Sound {
    private val worker = Executors.newCachedThreadPool()
    private val rate = 22050

    /** Notas de onda triangular que se apagan solas (una tras otra). */
    fun tone(vararg freqs: Int) = render(((freqs.size - 1) * 0.12 + 0.26)) { pcm ->
        freqs.forEachIndexed { i, f ->
            val start = (i * 0.12 * rate).toInt()
            for (j in 0 until (0.26 * rate).toInt()) {
                val t = j.toDouble() / rate
                mix(pcm, start + j, 2 / PI * asin(sin(2 * PI * f * t)) * 0.15 * exp(-t * 18))
            }
        }
    }

    fun sfx(s: Sfx) = when (s) {
        // burbuja que revienta: tono que cae muy rápido
        Sfx.POP -> render(0.12) { pcm ->
            var ph = 0.0
            for (j in pcm.indices) {
                val t = j.toDouble() / rate
                ph += 2 * PI * (300 + 1100 * exp(-t * 45)) / rate
                mix(pcm, j, sin(ph) * 0.3 * exp(-t * 38))
            }
        }
        // salto con resorte: tono que sube y vibra
        Sfx.BOING -> render(0.38) { pcm ->
            var ph = 0.0
            for (j in pcm.indices) {
                val t = j.toDouble() / rate
                ph += 2 * PI * (220 + 520 * (1 - exp(-t * 9)) + 30 * sin(t * 2 * PI * 18)) / rate
                mix(pcm, j, sin(ph) * 0.22 * exp(-t * 6))
            }
        }
        // viento (barrilete que vuela): ruido suave que sube y baja
        Sfx.WHOOSH -> render(0.5) { pcm ->
            val rnd = java.util.Random(7)
            var lp = 0.0
            for (j in pcm.indices) {
                val p = j.toDouble() / pcm.size
                lp += (rnd.nextGaussian() - lp) * (0.05 + 0.25 * p)
                mix(pcm, j, lp * 0.5 * sin(PI * p))
            }
        }
        // tecla: clic corto
        Sfx.CLICK -> render(0.03) { pcm ->
            val rnd = java.util.Random(3)
            for (j in pcm.indices) mix(pcm, j, (rnd.nextDouble() * 2 - 1) * 0.18 * exp(-j.toDouble() / rate * 220))
        }
    }

    private fun mix(pcm: ShortArray, k: Int, v: Double) {
        if (k in pcm.indices) pcm[k] = (pcm[k] + v * 32767).toInt().coerceIn(-32768, 32767).toShort()
    }

    /** Genera [seconds] de audio con [build] y lo reproduce en segundo plano. */
    private fun render(seconds: Double, build: (ShortArray) -> Unit) {
        worker.execute {
            try {
                val pcm = ShortArray((seconds * rate).toInt() + 1)
                build(pcm)
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

/** Vibración corta para acompañar aciertos y errores. */
class Haptics(context: Context) {
    private val v: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } catch (_: Exception) {
        null
    }

    private fun play(timings: LongArray, amps: IntArray) {
        val vib = v ?: return
        try {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 26) vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
            else vib.vibrate(timings, -1)
        } catch (_: Exception) {
        }
    }

    /** Error: un zumbido corto. */
    fun wrong() = play(longArrayOf(0, 70), intArrayOf(0, 140))

    /** Acierto: dos toquecitos suaves. */
    fun right() = play(longArrayOf(0, 25, 60, 25), intArrayOf(0, 90, 0, 90))
}

/** Una voz en español disponible en el teléfono, con un nombre amable para mostrar. */
data class VoiceOption(val id: String, val label: String, val detail: String)

/**
 * Voz para dictar las palabras.
 *
 * 1. Si existe una grabación en assets/voz/ (por ejemplo assets/voz/arbol.mp3), se usa esa:
 *    es la forma de tener una voz humana de verdad.
 * 2. Si no, se usa el motor de voz del teléfono, eligiendo la voz en español de mejor calidad
 *    (preferimos acento latinoamericano) o la que se haya elegido en la pantalla "Voz de Marty".
 */
class Speech(context: Context, private val save: Save) {
    private val app = context.applicationContext

    /** Estado observable: la interfaz se actualiza sola cuando la voz termina de cargar. */
    var ready by mutableStateOf(false)
        private set
    var options by mutableStateOf<List<VoiceOption>>(emptyList())
        private set
    var current by mutableStateOf("")
        private set

    private var player: MediaPlayer? = null
    private val recordings: Set<String> = try {
        app.assets.list("voz")?.toSet() ?: emptySet()
    } catch (_: Exception) {
        emptySet()
    }

    private val tts: TextToSpeech = TextToSpeech(app) { status ->
        if (status == TextToSpeech.SUCCESS) {
            setup()
            if (ready) waiting?.let { waiting = null; speakLine(it) }
        }
    }

    private fun setup() {
        val spanish = try {
            tts.voices.orEmpty().filter {
                it.locale.language == "es" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
            }
        } catch (_: Exception) {
            emptyList()
        }
        val net = online()
        val ranked = spanish.sortedByDescending { score(it, net) }
        options = ranked.mapIndexed { i, v -> VoiceOption(v.name, "Voz ${i + 1}", describe(v)) }
        val chosen = ranked.firstOrNull { it.name == save.voice } ?: ranked.firstOrNull()
        if (chosen != null && tts.setVoice(chosen) == TextToSpeech.SUCCESS) {
            current = chosen.name
            ready = true
            return
        }
        // Teléfonos sin lista de voces: probar idiomas.
        for (loc in listOf(Locale("es", "US"), Locale("es", "MX"), Locale("es", "ES"), Locale("es"))) {
            val r = tts.setLanguage(loc)
            if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) {
                ready = true
                return
            }
        }
    }

    /** ¿Hay internet ahora? (Las voces "en línea" suenan mucho más naturales, pero lo necesitan.) */
    private fun online(): Boolean = try {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (_: Exception) {
        false
    }

    /**
     * Puntos para ordenar voces: calidad y acento latinoamericano. Con internet ganan las voces en línea
     * (las más naturales); sin internet quedan al final porque no funcionarían.
     */
    private fun score(v: Voice, net: Boolean): Int {
        var s = v.quality
        val needsNet = v.isNetworkConnectionRequired || v.name.contains("network")
        s += when {
            needsNet && net -> 120
            needsNet -> -400
            else -> 40
        }
        if (v.locale.country in LATAM) s += 60
        if (v.locale.country == "ES") s += 20
        return s
    }

    private fun describe(v: Voice): String {
        val where = when (v.locale.country) {
            "ES" -> "España"
            "MX" -> "México"
            "US" -> "Latinoamérica (EE. UU.)"
            "" -> "Español"
            in LATAM -> "Latinoamérica"
            else -> v.locale.displayCountry
        }
        val quality = when {
            v.quality >= Voice.QUALITY_VERY_HIGH -> "calidad muy alta"
            v.quality >= Voice.QUALITY_HIGH -> "calidad alta"
            else -> "calidad normal"
        }
        val net = if (v.isNetworkConnectionRequired || v.name.contains("network")) " · en línea (más natural)" else ""
        return "$where · $quality$net"
    }

    fun choose(id: String) {
        val v = try { tts.voices.orEmpty().firstOrNull { it.name == id } } catch (_: Exception) { null } ?: return
        if (tts.setVoice(v) == TextToSpeech.SUCCESS) {
            current = id
            save.voice = id
        }
    }

    /** Frases grabadas disponibles (assets/voz/f/<crc32>.m4a). */
    private val phraseClips: Set<String> = try {
        app.assets.list("voz/f")?.toSet() ?: emptySet()
    } catch (_: Exception) {
        emptySet()
    }

    /** Archivo grabado de una palabra, si existe (assets/voz/arbol.m4a). */
    private fun recordingFor(text: String): String? {
        val key = palabraClave(text)
        return listOf("ogg", "mp3", "m4a", "wav").map { "$key.$it" }.firstOrNull { it in recordings }?.let { "voz/$it" }
    }

    /** ¿Marty y Borrón leen en voz alta lo que dicen? (Se puede apagar en "Voz de Marty".) */
    var hostOn by mutableStateOf(save.hostVoice)
        private set

    fun setHost(on: Boolean) {
        hostOn = on
        save.hostVoice = on
        if (!on) stop()
    }

    /* ---------- Cadena de grabaciones ---------- */

    /** Archivos que faltan por sonar en la cadena actual, y la palabra que espera a que termine. */
    private val chain = ArrayDeque<String>()
    private var pendingWord: String? = null

    private fun playChain(files: List<String>) {
        stopPlayer()
        chain.clear()
        chain.addAll(files)
        playNext()
    }

    private fun playNext() {
        val file = chain.removeFirstOrNull()
        if (file == null) {
            player = null
            pendingWord?.let { pendingWord = null; speak(it, flush = true) }
            return
        }
        try {
            val fd = app.assets.openFd(file)
            player = MediaPlayer().apply {
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                setOnCompletionListener { mp ->
                    mp.release()
                    if (player === mp) playNext()
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            playNext()
        }
    }

    /** Frase que no se pudo decir porque la voz del teléfono aún no estaba lista. */
    private var waiting: Line? = null

    /**
     * Dice una frase de Marty o Borrón: con las grabaciones si están todas sus partes,
     * o con la voz del teléfono (Marty agudo y alegre, Borrón grave) si falta alguna.
     */
    fun speakLine(l: Line) {
        if (!hostOn) return
        val files = l.parts.map { p ->
            when (p) {
                is Parte.Frase -> fraseArchivo(p.texto).takeIf { it in phraseClips }?.let { "voz/f/$it" }
                is Parte.Palabra -> recordingFor(p.texto)
            }
        }
        if (l.parts.isNotEmpty() && files.all { it != null }) {
            if (ready) tts.stop()
            pendingWord = null
            playChain(files.filterNotNull())
            return
        }
        if (!ready) {
            waiting = l
            return
        }
        speakHost(l.text, l.who == Who.BORRON)
    }

    private fun speakHost(text: String, borron: Boolean) {
        stopPlayer()
        val clean = text.replace("\"", "").replace("…", ",").trim()
        if (clean.isEmpty()) return
        tts.setSpeechRate(if (borron) save.rate * .95f else save.rate * 1.08f)
        tts.setPitch(if (borron) .72f else 1.22f)
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "host")
    }

    /**
     * Dicta una palabra. [flush] = false la pone en cola: se dice cuando Marty termine de hablar
     * (al empezar una pregunta). El botón "Escuchar" usa flush = true para oírla de inmediato.
     */
    fun speak(text: String, flush: Boolean = true) {
        if (flush) stop()
        else if (player != null) {
            pendingWord = text
            return
        }
        recordingFor(text)?.let { file ->
            playChain(listOf(file))
            return
        }
        if (!ready) return
        tts.setSpeechRate(save.rate)
        tts.setPitch(1.05f)
        // El punto final da la entonación natural de una frase y evita que la palabra suene cortada.
        tts.speak(text.trim().replaceFirstChar { it.uppercase() } + ".", if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "w")
    }

    private fun stopPlayer() {
        chain.clear()
        player?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
        player = null
    }

    fun stop() {
        pendingWord = null
        stopPlayer()
        if (ready) tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    private companion object {
        val LATAM = setOf("US", "MX", "419", "GT", "CO", "AR", "CL", "PE", "VE", "EC", "BO", "UY", "PY", "CR", "PA", "SV", "HN", "NI", "DO", "CU", "PR")
    }
}
