package com.billkevin91.martypalabras

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Who { MARTY, BORRON }
enum class Mood { NONE, HAPPY, SAD }

/** De dónde se entró a un planeta: el Desafío Galáctico (con su número) o la visita libre. */
data class Ctx(val reto: Boolean, val pi: Int = 0)

/** Una visita a un planeta: 5 preguntas. */
class Session(val k: String, val ctx: Ctx, val q: List<Word>, val level: Level) {
    var i by mutableIntStateOf(0)
    var ok = 0
    var tried = false
    var locked by mutableStateOf(false)
    val word: Word get() = q[i]
}

sealed interface Screen {
    data object Home : Screen
    data object Picker : Screen
    data object Name : Screen
    data class Intro(val pi: Int) : Screen
    data object Play : Screen
    data class End(val k: String, val ctx: Ctx, val ok: Int, val stars: Int, val pass: Boolean) : Screen
    data object Diploma : Screen
    data object Voice : Screen
    data object Attack : Screen
    data class AttackEnd(val score: Int, val ok: Int, val record: Boolean) : Screen
}

/** Progreso guardado en el teléfono. */
class Save(context: Context) {
    private val p = context.getSharedPreferences("marty", Context.MODE_PRIVATE)
    var name: String
        get() = p.getString("name", "") ?: ""
        set(v) = p.edit().putString("name", v).apply()
    var attack: Int
        get() = p.getInt("attack", 0)
        set(v) = p.edit().putInt("attack", v).apply()

    var level: Level
        get() = Level.entries.getOrElse(p.getInt("level", 1)) { Level.NORMAL }
        set(v) = p.edit().putInt("level", v.ordinal).apply()

    /** Voz elegida (vacío = la mejor que encuentre la app) y velocidad del dictado. */
    var voice: String
        get() = p.getString("voice", "") ?: ""
        set(v) = p.edit().putString("voice", v).apply()
    var rate: Float
        get() = p.getFloat("rate", 0.9f)
        set(v) = p.edit().putFloat("rate", v).apply()

    /** Marty y Borrón leen en voz alta sus mensajes. */
    var hostVoice: Boolean
        get() = p.getBoolean("hostVoice", true)
        set(v) = p.edit().putBoolean("hostVoice", v).apply()

    fun best(k: String) = p.getInt("best_$k", 0)
    fun setBest(k: String, v: Int) = p.edit().putInt("best_$k", v).apply()
}

class Game(context: Context) {
    val save = Save(context)
    private val sound = Sound()
    private val haptics = Haptics(context)
    val speech = Speech(context, save)
    private val scope = MainScope()

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set
    var title by mutableStateOf("Marty y las Palabras")
        private set
    var text by mutableStateOf("")
        private set
    var who by mutableStateOf(Who.MARTY)
        private set
    var mood by mutableStateOf(Mood.NONE)
        private set
    var moodTick by mutableIntStateOf(0)
        private set
    var session by mutableStateOf<Session?>(null)
        private set
    private val run = mutableMapOf<String, Int>()

    /** Cada vez que sube, se lanza confeti; [partyBig] para las celebraciones grandes. */
    var party by mutableIntStateOf(0)
        private set
    var partyBig by mutableStateOf(false)
        private set

    fun celebrate(big: Boolean) {
        partyBig = big
        party++
    }

    /** Dificultad elegida; se guarda en el teléfono. */
    var level by mutableStateOf(save.level)
        private set

    fun changeLevel(l: Level) {
        level = l
        save.level = l
        tone(660)
        say(Frases.nivel(l))
    }

    val canSpeak: Boolean get() = speech.ready

    /** Muestra la frase en el globo y la dice en voz alta (grabada, o con la voz del teléfono). */
    fun say(l: Line) {
        text = l.text
        who = l.who
        speech.speakLine(l)
    }

    fun react(m: Mood) {
        mood = m
        moodTick++
    }

    fun tone(vararg f: Int) = sound.tone(*f)
    fun sfx(s: Sfx) = sound.sfx(s)
    fun speak(s: String, flush: Boolean = true) = speech.speak(s, flush)

    fun close() {
        scope.cancel()
        speech.shutdown()
    }

    /** La app pasa a segundo plano: que no siga hablando. */
    fun pause() = speech.stop()

    /* ---------- Menús ---------- */

    fun home() {
        session = null
        speech.stop()
        title = "Marty y las Palabras"
        react(Mood.NONE)
        say(if (save.name.isNotEmpty()) Frases.hola(save.name) else Frases.BIENVENIDA)
        screen = Screen.Home
    }

    fun picker() {
        session = null
        title = "Astroaventura"
        react(Mood.NONE)
        say(Frases.ELIGE_PLANETA)
        screen = Screen.Picker
    }

    /** Botón atrás de Android: devuelve false si ya estamos en el menú. */
    fun back(): Boolean {
        if (screen == Screen.Home) return false
        home()
        return true
    }

    /* ---------- Desafío Galáctico ---------- */

    fun retoStart() {
        run.clear()
        if (save.name.isNotEmpty()) return retoIntro(0)
        title = "Antes de despegar"
        react(Mood.NONE)
        say(Frases.NOMBRE)
        screen = Screen.Name
    }

    fun setName(n: String) {
        save.name = n
        retoIntro(0)
    }

    fun retoIntro(pi: Int) {
        val k = RETO[pi]
        session = null
        title = "Planeta ${pi + 1} de ${RETO.size}"
        react(Mood.NONE)
        say(Frases.llegada(k))
        screen = Screen.Intro(pi)
    }

    fun diploma() {
        title = "¡Diploma galáctico!"
        react(Mood.HAPPY)
        tone(523, 659, 784, 1046)
        say(Frases.diploma(save.name))
        celebrate(true)
        screen = Screen.Diploma
    }

    fun runStars(k: String) = run[k] ?: save.best(k)

    fun voiceSettings() {
        session = null
        title = "Voz de Marty"
        react(Mood.NONE)
        say(Frases.VOZ)
        screen = Screen.Voice
    }

    /* ---------- Un planeta ---------- */

    fun play(k: String, ctx: Ctx) {
        session = Session(k, ctx, poolFor(k, level).shuffled().take(N), level)
        startQuestion()
        screen = Screen.Play
    }

    private fun startQuestion() {
        val s = session ?: return
        s.tried = false
        s.locked = false
        react(Mood.NONE)
        title = planet(s.k).name
    }

    fun leavePlay() {
        if (session?.ctx?.reto == true) home() else picker()
    }

    fun wrong() {
        val s = session ?: return
        s.tried = true
        tone(200, 150)
        haptics.wrong()
        react(Mood.SAD)
        say(Frases.CASI)
    }

    fun win(credit: Boolean, quiet: Boolean = false) {
        val s = session ?: return
        if (s.locked) return
        s.locked = true
        if (credit) s.ok++
        react(Mood.HAPPY)
        tone(523, 659, 784)
        haptics.right()
        // Si Marty habla, ya dice la palabra en su felicitación; si no, se dicta la palabra sola.
        if (!quiet && !speech.hostOn) speak(s.word.text)
        say(Frases.acierto(s.word.text))
        celebrate(false)
        later(s, 1500)
    }

    /** Pasa a la siguiente pregunta tras una pausa, si seguimos en la misma. */
    fun later(s: Session, ms: Long) {
        val i = s.i
        scope.launch {
            delay(ms)
            if (session === s && s.i == i) nextQ()
        }
    }

    private fun nextQ() {
        val s = session ?: return
        if (s.i + 1 >= N) planetEnd() else {
            s.i++
            startQuestion()
        }
    }

    private fun planetEnd() {
        val s = session ?: return
        val k = s.k
        val st = starsOf(s.ok)
        val pass = s.ok >= 3
        val reto = s.ctx.reto
        if (!reto || pass) {
            if (st > save.best(k)) save.setBest(k, st)
        }
        if (reto && pass) run[k] = st
        react(if (pass) Mood.HAPPY else Mood.SAD)
        if (pass) {
            tone(523, 659, 784, 1046)
            celebrate(true)
        } else tone(300, 260)
        val name = planet(k).name
        say(Frases.finPlaneta(k, reto, pass, st))
        title = name
        session = null
        screen = Screen.End(k, s.ctx, s.ok, st, pass)
    }

    fun endNext(e: Screen.End) {
        when {
            e.ctx.reto && e.pass -> if (e.ctx.pi >= RETO.size - 1) diploma() else retoIntro(e.ctx.pi + 1)
            e.ctx.reto -> retoIntro(e.ctx.pi)
            else -> play(e.k, e.ctx)
        }
    }

    /* ---------- Ataque de las palabras ---------- */

    fun attack() {
        session = null
        title = "Ataque de las palabras"
        react(Mood.NONE)
        say(Frases.ATAQUE_INICIO)
        screen = Screen.Attack
    }

    fun attackEnd(score: Int, ok: Int) {
        if (screen != Screen.Attack) return
        val rec = score > save.attack
        if (rec) save.attack = score
        react(if (ok >= 7) Mood.SAD else Mood.HAPPY)
        if (ok >= 7) {
            tone(523, 659, 784)
            celebrate(true)
        } else tone(300, 260)
        say(if (ok >= 7) Frases.ATAQUE_GANASTE else Frases.ATAQUE_PERDISTE)
        screen = Screen.AttackEnd(score, ok, rec)
    }
}
