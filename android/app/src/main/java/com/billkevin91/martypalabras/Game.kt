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
    data object Album : Screen
    data class Cine(val kind: Escena, val arg: String = "") : Screen
    data object Attack : Screen
    data class AttackEnd(val score: Int, val ok: Int, val record: Boolean) : Screen
}

/** Día de hoy (contado en la zona horaria del teléfono), para la racha. */
fun today(now: Long = System.currentTimeMillis()): Long = (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L

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

    /** Stickers del álbum: las palabras que el jugador ya acertó alguna vez. */
    val stickers: Set<String> get() = p.getStringSet("stickers", emptySet()) ?: emptySet()

    /** Pega un sticker; devuelve true si es nuevo. */
    fun addSticker(word: String): Boolean {
        val now = stickers
        if (word in now) return false
        p.edit().putStringSet("stickers", now + word).apply()
        return true
    }

    /** Racha: días seguidos jugando (se cuenta al acertar al menos una palabra en el día). */
    val streak: Int get() = if (p.getLong("lastDay", -9) >= today() - 1) p.getInt("streak", 0) else 0

    /** Marca el día de hoy como jugado; devuelve true si la racha acaba de crecer. */
    fun markPlayed(): Boolean {
        val d = today()
        val last = p.getLong("lastDay", -9)
        if (last == d) return false
        val s = if (last == d - 1) p.getInt("streak", 0) + 1 else 1
        p.edit().putLong("lastDay", d).putInt("streak", s).apply()
        return true
    }

    /** Ya vio la escena de introducción (se muestra sola solo la primera vez). */
    var introSeen: Boolean
        get() = p.getBoolean("introSeen", false)
        set(v) = p.edit().putBoolean("introSeen", v).apply()

    /** Marty comenta cada jugada en voz alta (si no, solo lo importante; lo demás aparece escrito). */
    var chatty: Boolean
        get() = p.getBoolean("chatty", false)
        set(v) = p.edit().putBoolean("chatty", v).apply()

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

    private val spokenAt = HashMap<String, Long>()

    /**
     * Muestra la frase en el globo y la dice en voz alta (grabada, o con la voz del teléfono).
     * Las frases de un mismo tipo no se repiten en voz alta en menos de 3,5 segundos, y las que son
     * solo para el globo (comentarios de cada letra) no se dicen: así Marty no habla sin parar.
     */
    private var lastLine: Line? = null
    private var lastVoiced = false

    /** Marty dirá en voz alta los comentarios de cada jugada (si no, solo aparecen escritos). */
    var chatty by mutableStateOf(save.chatty)
        private set

    fun changeChatty(on: Boolean) {
        chatty = on
        save.chatty = on
    }

    fun say(l: Line, force: Boolean = false) {
        text = l.text
        who = l.who
        lastLine = l
        lastVoiced = false
        if (!l.voice && !force) return
        if (l.chat && !chatty && !force) return
        val k = l.kind
        val now = android.os.SystemClock.uptimeMillis()
        if (k != null && !force) {
            if (now - (spokenAt[k] ?: 0L) < 3500L) return
            spokenAt[k] = now
        }
        lastVoiced = true
        speech.speakLine(l)
    }

    /** Al tocar a Marty: lee lo que dice su globo; si ya lo dijo, cuenta algo nuevo. */
    fun tapHost() {
        sfx(Sfx.POP)
        val l = lastLine
        if (l != null && !lastVoiced) {
            lastVoiced = true
            speech.speakLine(l)
            react(Mood.HAPPY)
            return
        }
        if (who == Who.BORRON) { react(Mood.HAPPY); return }
        react(Mood.HAPPY)
        say(Frases.charla(), force = true)
    }

    /** Instrucciones del planeta solo en la primera pregunta; después, una frase corta de ánimo. */
    fun intro(s: Session, l: Line) = say(if (s.i == 0) l else Frases.siguiente(s))

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
        cineThen = null
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

    /** Solo para las pruebas automáticas: muestra la pantalla de fin de planeta con 3 estrellas. */
    fun demoEnd() {
        title = "¡Planeta superado!"
        react(Mood.HAPPY)
        celebrate(true)
        screen = Screen.End("anagrama", Ctx(reto = false), N, 3, true)
    }

    /* ---------- Escenas animadas ---------- */

    private var cineThen: (() -> Unit)? = null

    /** Muestra una escena animada y, al terminar (o al tocar "Saltar"), hace [then]. */
    fun cine(kind: Escena, title: String, arg: String = "", then: () -> Unit) {
        session = null
        speech.stop()
        this.title = title
        cineThen = then
        screen = Screen.Cine(kind, arg)
    }

    fun cineDone() {
        val then = cineThen ?: return
        cineThen = null
        speech.stop()
        then()
    }

    fun intro() {
        save.introSeen = true
        cine(Escena.INTRO, "Marty y las Palabras") { home() }
    }

    fun runStars(k: String) = run[k] ?: save.best(k)

    fun album() {
        session = null
        speech.stop()
        title = "Álbum de stickers"
        react(Mood.HAPPY)
        val n = save.stickers.size
        say(Frases.album(n, WORDS.size))
        screen = Screen.Album
    }

    /** Sticker recién conseguido: se muestra un aviso unos segundos. */
    var newSticker by mutableStateOf<Word?>(null)
        private set

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

    /** Error: sonido, vibración y cara triste. [comment] = false si el juego dirá algo más preciso. */
    fun wrong(comment: Boolean = true) {
        val s = session ?: return
        s.tried = true
        tone(200, 150)
        haptics.wrong()
        react(Mood.SAD)
        if (comment) say(Frases.casi())
    }

    fun win(credit: Boolean, quiet: Boolean = false) {
        val s = session ?: return
        if (s.locked) return
        s.locked = true
        if (credit) s.ok++
        save.markPlayed()
        if (credit && save.addSticker(s.word.text)) {
            val w = s.word
            newSticker = w
            scope.launch {
                delay(2200)
                if (newSticker === w) newSticker = null
            }
        }
        react(Mood.HAPPY)
        tone(523, 659, 784)
        haptics.right()
        // Si Marty habla, ya dice la palabra en su felicitación; si no, se dicta la palabra sola.
        if (!quiet && !(speech.hostOn && chatty)) speak(s.word.text)
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
            e.ctx.reto && e.pass -> if (e.ctx.pi >= RETO.size - 1) cine(Escena.FINAL, "¡Campeón galáctico!") { diploma() }
            else cine(Escena.VIAJE, "Viajando...", RETO[e.ctx.pi + 1]) { retoIntro(e.ctx.pi + 1) }
            e.ctx.reto -> retoIntro(e.ctx.pi)
            else -> play(e.k, e.ctx)
        }
    }

    /* ---------- Ataque de las palabras ---------- */

    /** Antes del ataque, Borrón aparece en una escena animada. */
    fun attack() = cine(Escena.BORRON_LLEGA, "¡Alerta de tinta!") { attackStart() }

    private fun attackStart() {
        session = null
        title = "Ataque de las palabras"
        react(Mood.NONE)
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
        val end = Screen.AttackEnd(score, ok, rec)
        // Si ganaste, Marty sale a perseguir a Borrón con un borrador gigante.
        if (ok >= 7) return cine(Escena.BORRON_HUYE, "¡Fuera, Borrón!") { title = "Ataque de las palabras"; screen = end }
        say(Frases.ATAQUE_PERDISTE)
        screen = end
    }
}
