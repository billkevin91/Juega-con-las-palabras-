package com.billkevin91.martypalabras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ModeButton(planetArt: @Composable () -> Unit, title: String, desc: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(C.card).border(2.dp, C.line, shape).tap(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        planetArt()
        Column {
            Text(title, style = display(21))
            Text(desc, style = body(15, C.muted))
        }
    }
}

@Composable
fun HomeScreen(g: Game) {
    val t by rememberTime()
    val tm = rememberTextMeasurer()
    val done = RETO.count { g.save.best(it) > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(22.dp)).border(3.dp, Ink, RoundedCornerShape(22.dp))
        ) { heroScene(t, tm) }
        ModeButton({ PlanetBall(Color(0xFFFFE07A), Color(0xFFE08A00), true, 56.dp) },
            "Desafío Galáctico", "Visita 4 planetas, gana medallas y un diploma. Superados: $done de 4") { g.retoStart() }
        ModeButton({ PlanetBall(Color(0xFF8FDCFF), Color(0xFF2478C8), false, 56.dp) },
            "Astroaventura", "Visita libre: elige el planeta que quieras") { g.picker() }
        ModeButton({ PlanetBall(Color(0xFF6B57D6), Color(0xFF2A1F78), false, 56.dp) },
            "Ataque de las palabras", "Borrón quiere ensuciar el texto. ¡Detenlo en 60 segundos! Récord: ${g.save.attack} puntos") { g.attack() }
    }
}

@Composable
fun PickerScreen(g: Game) {
    GameCard {
        PLANETS.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { p ->
                    val shape = RoundedCornerShape(22.dp)
                    Column(
                        Modifier.weight(1f).clip(shape).background(C.card).border(2.dp, C.line, shape)
                            .tap { g.play(p.key, Ctx(reto = false)) }.padding(horizontal = 10.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlanetBall(p, 64.dp)
                        Text(p.name, style = display(21), textAlign = TextAlign.Center)
                        Text(p.desc, style = body(14, C.muted), textAlign = TextAlign.Center)
                        StarsRow(g.save.best(p.key), 18.dp)
                    }
                }
            }
        }
        Ghost("Volver al menú") { g.home() }
    }
}

@Composable
fun NameScreen(g: Game) {
    var v by remember { mutableStateOf("") }
    val submit = { if (v.isNotBlank()) g.setName(v.trim()) }
    GameCard {
        val shape = RoundedCornerShape(16.dp)
        BasicTextField(
            value = v,
            onValueChange = { if (it.length <= 24) v = it },
            singleLine = true,
            textStyle = display(26).copy(textAlign = TextAlign.Center),
            cursorBrush = SolidColor(C.ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth().clip(shape).background(C.space).border(3.dp, C.line, shape).padding(12.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (v.isEmpty()) Text("Tu nombre", style = display(26, C.muted))
                    inner()
                }
            },
        )
        Buttons {
            Ghost("Despegar", primary = true) { submit() }
            Ghost("Volver") { g.home() }
        }
    }
}

@Composable
fun IntroScreen(g: Game, pi: Int) {
    val p = planet(RETO[pi])
    GameCard {
        PlanetBall(p, 110.dp)
        Text("${p.name}\n${p.desc}", style = body(16, C.muted, FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Buttons {
            Ghost("Aterrizar", primary = true) { g.play(p.key, Ctx(reto = true, pi = pi)) }
            Ghost("Salir") { g.home() }
        }
    }
}

@Composable
fun EndScreen(g: Game, e: Screen.End) {
    GameCard {
        PlanetBall(planet(e.k), 90.dp)
        StarsRow(e.stars, 60.dp)
        Text("${e.ok} de $N a la primera", style = body(16, C.muted, FontWeight.ExtraBold))
        Buttons {
            val label = when {
                e.ctx.reto && e.pass -> if (e.ctx.pi >= RETO.size - 1) "Ver mi diploma" else "Siguiente planeta"
                e.ctx.reto -> "Intentar otra vez"
                else -> "Jugar otra vez"
            }
            Ghost(label, primary = true) { g.endNext(e) }
            Ghost(if (e.ctx.reto) "Salir" else "Elegir otro planeta") { if (e.ctx.reto) g.home() else g.picker() }
        }
    }
}

@Composable
fun DiplomaScreen(g: Game) {
    val date = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date()) }
    GameCard {
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(C.paper).border(6.dp, C.gold, shape).padding(6.dp)
                .border(2.dp, C.gold, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val ink = C.paperInk
            Text("Concurso Galáctico de Palabras", style = body(15, ink))
            Text("Diploma de Astronauta de la Ortografía", style = display(30, ink), textAlign = TextAlign.Center)
            Text("Se otorga a", style = body(15, ink))
            Text(g.save.name, style = display(36, ink), textAlign = TextAlign.Center)
            Box(Modifier.width(200.dp).height(3.dp).background(C.gold))
            Text("por superar los cuatro planetas del desafío", style = body(15, ink), textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                RETO.forEach { k ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(planet(k).name, style = body(13, ink, FontWeight.ExtraBold))
                        StarsRow(g.runStars(k), 14.dp)
                    }
                }
            }
            Text("$date · Marty, presentador", style = body(14, ink), textAlign = TextAlign.Center)
        }
        Hint("Haz una captura de pantalla para guardar tu diploma.")
        Buttons {
            Ghost("Volver a jugar", primary = true) { g.retoStart() }
            Ghost("Menú") { g.home() }
        }
    }
}

@Composable
fun AttackEndScreen(g: Game, e: Screen.AttackEnd) {
    GameCard {
        StarsRow(if (e.ok >= 12) 3 else if (e.ok >= 7) 2 else 1, 60.dp)
        Text(
            "${e.score} puntos · ${e.ok} palabras bien" + if (e.record) "\n¡Nuevo récord!" else "",
            style = body(16, C.muted, FontWeight.ExtraBold), textAlign = TextAlign.Center,
        )
        Buttons {
            Ghost("Revancha", primary = true) { g.attack() }
            Ghost("Menú") { g.home() }
        }
    }
}

/** Marco común de cada pregunta: progreso, contenido y botones (pista, escuchar, salir). */
@Composable
fun QFrame(
    g: Game, s: Session, hear: Boolean = false, clue: Boolean? = null, onClue: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    GameCard {
        ProgressDots(s.i)
        content()
        Buttons {
            if (clue == false) Ghost("Ver pista") { onClue() }
            if (hear) Ghost("Escuchar", icon = { SpeakerIcon() }) { g.speak(s.word.text) }
            Ghost("Salir") { g.leavePlay() }
        }
    }
}
