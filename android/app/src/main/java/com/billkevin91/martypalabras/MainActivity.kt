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
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.togetherWith
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
            null -> if (!game.save.introSeen) game.intro()
            "home" -> Unit
            "cine_intro" -> game.intro()
            "cine_viaje" -> game.cine(Escena.VIAJE, "Viajando...", "tela") { game.home() }
            "cine_borron" -> game.cine(Escena.BORRON_LLEGA, "¡Alerta de tinta!") { game.home() }
            "cine_huye" -> game.cine(Escena.BORRON_HUYE, "¡Fuera, Borrón!") { game.home() }
            "cine_final" -> game.cine(Escena.FINAL, "¡Campeón galáctico!") { game.home() }
            "attack" -> game.attack()
            "voz" -> game.voiceSettings()
            "album" -> { WORDS.shuffled().take(40).forEach { game.save.addSticker(it.text) }; game.album() }
            "picker" -> game.picker()
            "intro" -> game.retoIntro(4)
            "fin" -> game.demoEnd()
            else -> if (PLANETS.any { it.key == demo }) game.play(demo, Ctx(reto = false))
        }
        setContent {
            BackHandler(enabled = game.screen != Screen.Home) { game.back() }
            App(game)
        }
    }

    override fun onResume() {
        super.onResume()
        Tilt.start(this)
    }

    override fun onPause() {
        Tilt.stop()
        super.onPause()
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

/** Fondo de cielo nocturno de caricatura: franjas de color, planetitas planos y estrellas que titilan. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.space(t: Float) {
    drawRect(Brush.verticalGradient(*bands(4, arrayOf(0f to C.space, 1f to C.space2))))
    val w = size.width
    val h = size.height
    // Paralaje: las estrellas grandes (cercanas) se mueven más que las pequeñas al inclinar el teléfono.
    // Las más grandes son estrellitas de 4 puntas de caricatura; las demás, puntitos.
    STARS.forEach { (x, y, r, ph) ->
        val depth = r * 9f * density
        val o = Offset(w * x - Tilt.x * depth, h * y - Tilt.y * depth)
        val a = lerp(.3f, 1f, swing(t + ph, 1.8f))
        if (r > 1.8f) {
            val s = r * density * 2.6f * lerp(.75f, 1.1f, swing(t + ph, 1.8f))
            val k = s * .22f
            val star = androidx.compose.ui.graphics.Path().apply {
                moveTo(o.x, o.y - s)
                quadraticBezierTo(o.x + k, o.y - k, o.x + s, o.y)
                quadraticBezierTo(o.x + k, o.y + k, o.x, o.y + s)
                quadraticBezierTo(o.x - k, o.y + k, o.x - s, o.y)
                quadraticBezierTo(o.x - k, o.y - k, o.x, o.y - s)
                close()
            }
            drawPath(star, Color(0xFFFFF3B0), alpha = a)
        } else drawCircle(Color.White.copy(alpha = a), r * density, o)
    }
    // estrella fugaz cada 7 segundos
    val p = (t % 7f) / .9f
    if (p < 1f) {
        val n = (t / 7f).toInt()
        val sx = w * (.2f + (n * 37 % 60) / 100f)
        val sy = h * (.05f + (n * 53 % 30) / 100f)
        val hx = sx + w * .35f * p
        val hy = sy + w * .18f * p
        // Estrella fugaz de caricatura: estela de rayitas y una estrella amarilla al frente.
        for (i in 1..3) drawLine(
            Color.White, Offset(hx - w * .04f * i, hy - w * .02f * i), Offset(hx - w * .04f * i + w * .025f, hy - w * .02f * i + w * .0125f),
            3f * density, androidx.compose.ui.graphics.StrokeCap.Round, alpha = (1f - p) * (1f - i * .25f),
        )
        drawCircle(Color(0xFFFFE36A), 4.5f * density, Offset(hx, hy), alpha = 1f - p)
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
        StickerToast(g.newSticker)
    }
}

@Composable
private fun Screens(g: Game) {
    // Transición entre pantallas: la nueva entra deslizándose y creciendo; la anterior se desvanece.
    androidx.compose.animation.AnimatedContent(
        targetState = g.screen,
        transitionSpec = {
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260, 60)) +
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(320)) { it / 10 } +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(320), initialScale = .96f)) togetherWith
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140))
        },
        label = "pantalla",
    ) { s ->
        // Algunas pantallas tienen varios bloques: van en columna para no superponerse durante la transición.
        Column(verticalArrangement = Arrangement.spacedBy(if (s == Screen.Play) 10.dp else 16.dp)) { ScreenContent(g, s) }
    }
}

@Composable
private fun ScreenContent(g: Game, s: Screen) {
    when (s) {
        Screen.Home -> HomeScreen(g)
        Screen.Picker -> PickerScreen(g)
        Screen.Name -> NameScreen(g)
        is Screen.Intro -> IntroScreen(g, s.pi)
        Screen.Play -> PlayScreen(g)
        is Screen.End -> EndScreen(g, s)
        Screen.Diploma -> DiplomaScreen(g)
        Screen.Voice -> VoiceScreen(g)
        Screen.Album -> AlbumScreen(g)
        is Screen.Cine -> CineScreen(g, s)
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
    val time = rememberTime()
    // Título de caricatura: letras amarillas planas, contorno grueso, sombra dura y un leve balanceo.
    Box(Modifier.graphicsLayer { rotationZ = 1.2f * wave(time.value, 3f) }) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = size * .16f * d, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        Text(text, style = base.copy(color = Color(0xFF0A0620), drawStyle = stroke), modifier = Modifier.offset(y = (size * .09f).dp))
        Text(text, style = base.copy(color = Ink, drawStyle = stroke))
        Text(text, style = base.copy(brush = Brush.verticalGradient(*bands(2, arrayOf(0f to Color(0xFFFFF0A0), 1f to C.sun)))))
    }
}
