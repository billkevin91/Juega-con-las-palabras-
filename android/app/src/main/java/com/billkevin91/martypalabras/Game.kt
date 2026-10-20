package com.billkevin91.martypalabras

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Who { MARTY, BORRON }
enum class Mood { NONE, HAPPY, SAD }

/** De dónde se entró a un planeta: el Desafío Galáctico (con su número) o la visita libre. */
data class Ctx(val reto: Boolean, val pi: Int = 0)

/** Una visita a un planeta: 5 preguntas. */
class Session(val k: String, val ctx: Ctx, val q: List<Word>) {
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

    fun best(k: String) = p.getInt("best_$k", 0)
    fun setBest(k: String, v: Int) = p.edit().putInt("best_$k", v).apply()
}

class Game(context: Context) {
    val save = Save(context)
    private val sound = Sound()
    private val speech = Speech(context)
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

    val canSpeak: Boolean get() = speech.ready

    fun say(t: String, w: Who = Who.MARTY) {
        text = t
        who = w
    }

    fun setMood(m: Mood) {
        mood = m
        moodTick++
    }

    fun tone(vararg f: Int) = sound.tone(*f)
    fun speak(s: String) = speech.speak(s)

    fun close() = speech.shutdown()

    /* ---------- Menús ---------- */

    fun home() {
        session = null
        speech.stop()
        title = "Marty y las Palabras"
        setMood(Mood.NONE)
        say(
            if (save.name.isNotEmpty()) "¡Hola, ${save.name}! ¿A qué planeta vamos hoy?"
            else "¡Bienvenido al concurso galáctico! ¡Soy Marty, tu presentador! ¡Vamos, vamos, vamos!"
        )
        screen = Screen.Home
    }

    fun picker() {
        session = null
        title = "Astroaventura"
        setMood(Mood.NONE)
        say("Elige un planeta. Aquí no hay prisa: practica lo que quieras.")
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
        setMood(Mood.NONE)
        say("¿Cómo te llamas, astronauta? Tu nombre irá en el diploma.")
        screen = Screen.Name
    }

    fun setName(n: String) {
        save.name = n
        retoIntro(0)
    }

    fun retoIntro(pi: Int) {
        val k = RETO[pi]
        session = null
        title = "Planeta ${pi + 1} de 4"
        setMood(Mood.NONE)
        say("Llegamos a ${planet(k).name}. ${INTRO[k]}")
        screen = Screen.Intro(pi)
    }

    fun diploma() {
        title = "¡Diploma galáctico!"
        setMood(Mood.HAPPY)
        tone(523, 659, 784, 1046)
        say("¡Lo lograste, ${save.name}! Superaste los cuatro planetas.")
        screen = Screen.Diploma
    }

    fun runStars(k: String) = run[k] ?: save.best(k)

    /* ---------- Un planeta ---------- */

    fun play(k: String, ctx: Ctx) {
        session = Session(k, ctx, poolFor(k).shuffled().take(N))
        startQuestion()
        screen = Screen.Play
    }

    private fun startQuestion() {
        val s = session ?: return
        s.tried = false
        s.locked = false
        setMood(Mood.NONE)
        title = planet(s.k).name
    }

    fun leavePlay() {
        if (session?.ctx?.reto == true) home() else picker()
    }

    fun wrong() {
        val s = session ?: return
        s.tried = true
        tone(200, 150)
        setMood(Mood.SAD)
        say("¡Casi! Prueba otra vez.")
    }

    fun win(credit: Boolean, quiet: Boolean = false) {
        val s = session ?: return
        if (s.locked) return
        s.locked = true
        if (credit) s.ok++
        setMood(Mood.HAPPY)
        tone(523, 659, 784)
        if (!quiet) speak(s.word.text)
        say(listOf("¡Muy bien!", "¡Excelente!", "¡Lo lograste!", "¡Qué listo eres!", "¡Genial!").random() + " " + s.word.text)
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
        setMood(if (pass) Mood.HAPPY else Mood.SAD)
        if (pass) tone(523, 659, 784, 1046) else tone(300, 260)
        val name = planet(k).name
        say(
            if (reto) {
                if (pass) "¡Superaste $name! Ganaste $st ${if (st == 1) "estrella" else "estrellas"}."
                else "Necesitas al menos 3 aciertos para pasar. ¡Inténtalo otra vez!"
            } else when {
                st == 3 -> "¡Eres un campeón de $name!"
                st >= 1 -> "¡Muy bien! Sigue practicando."
                else -> "¡Buen intento! Practiquemos más."
            }
        )
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
        setMood(Mood.NONE)
        say("¡Jajaja! Voy a ensuciar todas las palabras. ¡A ver si las escribes bien!", Who.BORRON)
        screen = Screen.Attack
    }

    fun attackEnd(score: Int, ok: Int) {
        if (screen != Screen.Attack) return
        val rec = score > save.attack
        if (rec) save.attack = score
        setMood(if (ok >= 7) Mood.SAD else Mood.HAPPY)
        if (ok >= 7) tone(523, 659, 784) else tone(300, 260)
        say(if (ok >= 7) "¡Me ganaste! Escribes muy bien." else "¡Jajaja! Esta vez gané yo. ¿Revancha?", Who.BORRON)
        screen = Screen.AttackEnd(score, ok, rec)
    }
}
