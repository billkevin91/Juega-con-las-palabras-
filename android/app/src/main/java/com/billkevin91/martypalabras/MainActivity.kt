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
            else -> if (PLANETS.any { it.key == demo }) game.play(demo, Ctx(reto = false))
        }
        setContent {
            BackHandler(enabled = game.screen != Screen.Home) { game.back() }
            App(game)
        }
    }

    override fun onDestroy() {
        game.close()
        super.onDestroy()
    }
}

private val STARS = listOf(
    Triple(.08f, .12f, 1.6f), Triple(.22f, .64f, 1.2f), Triple(.41f, .28f, 1.8f), Triple(.63f, .82f, 1.2f),
    Triple(.78f, .18f, 1.6f), Triple(.91f, .55f, 1.2f), Triple(.54f, .46f, 1.4f), Triple(.15f, .9f, 1.3f),
    Triple(.86f, .93f, 1.5f), Triple(.33f, .05f, 1.1f),
)

@Composable
fun App(g: Game) {
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(listOf(C.space, C.space2)))
            STARS.forEach { (x, y, r) -> drawCircle(Color.White.copy(alpha = .7f), r * density, Offset(size.width * x, size.height * y)) }
        }
    ) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HostBar(g)
                Text(g.title, style = display(34))
                Screens(g)
            }
        }
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
