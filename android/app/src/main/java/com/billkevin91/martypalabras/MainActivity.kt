package com.billkevin91.martypalabras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        game = Game(this)
        game.home()
        // Para las pruebas automáticas: abrir directamente un planeta ("--es demo pantano").
        when (val demo = intent?.getStringExtra("demo")) {
            null, "home" -> Unit
            "attack" -> game.attack()
            "voz" -> game.voiceSettings()
            "picker" -> game.picker()
            "intro" -> game.retoIntro(4)
            else -> if (PLANETS.any { it.key == demo }) game.play(demo, Ctx(reto = false))
        }
        setContent {
            BackHandler(enabled = game.screen != Screen.Home) { game.back() }
            App(game)
        }
    }

    override fun onStop() {
        game.pause()
        super.onStop()
    }

    override fun onDestroy() {
        game.close()
        super.onDestroy()
    }
}

/** Estrellas del fondo: posición (0..1), tamaño y desfase del parpadeo. */
private val STARS: List<FloatArray> = kotlin.random.Random(7).let { r -> List(46) { floatArrayOf(r.nextFloat(), r.nextFloat(), .8f + r.nextFloat() * 1.4f, r.nextFloat() * 5f) } }

/** Fondo del espacio animado: nebulosas que respiran, estrellas que parpadean y alguna estrella fugaz. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.space(t: Float) {
    drawRect(Brush.verticalGradient(listOf(C.space, C.space2)))
    val w = size.width
    val h = size.height
    for ((x, y, c) in listOf(Triple(.15f, .2f, Color(0xFF7A3FC0)), Triple(.85f, .55f, Color(0xFF2E5BC0)), Triple(.3f, .85f, Color(0xFFC03F8A)))) {
        val k = lerp(.9f, 1.1f, swing(t, 9f, x))
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .22f), Color.Transparent), Offset(w * x, h * y), w * .55f * k), w * .55f * k, Offset(w * x, h * y))
    }
    STARS.forEach { (x, y, r, ph) ->
        drawCircle(Color.White.copy(alpha = lerp(.25f, .95f, swing(t + ph, 1.8f))), r * density, Offset(w * x, h * y))
    }
    // estrella fugaz cada 7 segundos
    val p = (t % 7f) / .9f
    if (p < 1f) {
        val n = (t / 7f).toInt()
        val sx = w * (.2f + (n * 37 % 60) / 100f)
        val sy = h * (.05f + (n * 53 % 30) / 100f)
        val hx = sx + w * .35f * p
        val hy = sy + w * .18f * p
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, Color.White), Offset(hx - w * .12f, hy - w * .06f), Offset(hx, hy)),
            Offset(hx - w * .12f, hy - w * .06f), Offset(hx, hy), 2.5f * density, alpha = 1f - p,
        )
    }
}

@Composable
fun App(g: Game) {
    val time = rememberTime()
    Box(
        Modifier.fillMaxSize().drawBehind { space(time.value) }
    ) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (g.screen == Screen.Play) 10.dp else 16.dp)) {
                HostBar(g)
                // Durante el juego el título es pequeño: el espacio es para la escena.
                Logo(g.title, if (g.screen == Screen.Play) 22 else 34)
                Screens(g)
            }
        }
        Confetti(g.party, g.partyBig)
    }
}

@Composable
private fun Screens(g: Game) {
    when (val s = g.screen) {
        Screen.Home -> HomeScreen(g)
        Screen.Picker -> PickerScreen(g)
        Screen.Name -> NameScreen(g)
        is Screen.Intro -> IntroScreen(g, s.pi)
        Screen.Play -> PlayScreen(g)
        is Screen.End -> EndScreen(g, s)
        Screen.Diploma -> DiplomaScreen(g)
        Screen.Voice -> VoiceScreen(g)
        Screen.Attack -> AttackScreen(g)
        is Screen.AttackEnd -> AttackEndScreen(g, s)
    }
}

@Composable
private fun PlayScreen(g: Game) {
    val s = g.session ?: return
    androidx.compose.runtime.key(s, s.i) {
        when (s.k) {
            "pantano" -> PantanoGame(g, s)
            "burbuja" -> BurbujaGame(g, s)
            "tela" -> TelaGame(g, s)
            "anagrama" -> AnagramaGame(g, s)
            "falta" -> FaltaGame(g, s)
            else -> LeeGame(g, s)
        }
    }
}

/** Título estilo videojuego: letras con degradado dorado, contorno grueso y sombra. */
@Composable
fun Logo(text: String, size: Int) {
    val base = display(size)
    val d = androidx.compose.ui.platform.LocalDensity.current.density
    Box {
        Text(text, style = base.copy(color = Ink, drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = size * .14f * d, join = androidx.compose.ui.graphics.StrokeJoin.Round)))
        Text(
            text,
            style = base.copy(
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF3B0), Color(0xFFFFC93C))),
                shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF7A5AE0), Offset(0f, size * .12f), size * .25f),
            ),
        )
    }
}
