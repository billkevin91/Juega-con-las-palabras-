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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

@Composable
fun ModeButton(planetArt: @Composable () -> Unit, title: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    CandyBox(Color(0xFF3A2E92), 22.dp, modifier.fillMaxWidth(), depth = 6.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            planetArt()
            Column(Modifier.weight(1f)) {
                Text(title, style = display(21).copy(shadow = Shadow(Color(0x88000000), Offset(0f, 2f), 3f)))
                Text(desc, style = body(15, Color(0xFFD4CCFF)))
            }
            Text("›", style = display(34, C.sun))
        }
    }
}

@Composable
fun HomeScreen(g: Game) {
    val t by rememberTime()
    val tm = rememberTextMeasurer()
    val done = RETO.count { g.save.best(it) > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.scene(16f / 9f, Ink, 22.dp, 3.dp)) {
            Canvas(Modifier.fillMaxSize()) { heroScene(t, tm) }
            Sparkles(Color(0xFFFFF3B0))
        }
        StatsBar(g, Modifier.popIn(0))
        LevelPicker(g, Modifier.popIn(1))
        ModeButton({ PlanetBall(Color(0xFFFFE07A), Color(0xFFE08A00), true, 56.dp) },
            "Desafío Galáctico", "Visita los ${RETO.size} planetas, gana medallas y un diploma. Superados: $done de ${RETO.size}", Modifier.popIn(2)) { g.retoStart() }
        ModeButton({ PlanetBall(Color(0xFF8FDCFF), Color(0xFF2478C8), false, 56.dp) },
            "Astroaventura", "Visita libre: elige el planeta que quieras", Modifier.popIn(3)) { g.picker() }
        ModeButton({ PlanetBall(Color(0xFF6B57D6), Color(0xFF2A1F78), false, 56.dp) },
            "Ataque de las palabras", "Borrón quiere ensuciar el texto. ¡Detenlo en ${attackSeconds(g.level)} segundos! Récord: ${g.save.attack} puntos", Modifier.popIn(4)) { g.attack() }
        Buttons {
            Ghost("Álbum ${g.save.stickers.size}/${WORDS.size}") { g.album() }
            Ghost("Ver intro") { g.intro() }
            Ghost("Voz de Marty", icon = { SpeakerIcon() }) { g.voiceSettings() }
        }
    }
}

/** Fila de logros: racha de días, estrellas ganadas y stickers del álbum. */
@Composable
fun StatsBar(g: Game, modifier: Modifier = Modifier) {
    val streak = g.save.streak
    val stars = PLANETS.sumOf { g.save.best(it.key) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip("🔥", "$streak", if (streak == 1) "día seguido" else "días seguidos", Color(0xFFFF8A3D), Modifier.weight(1f))
        StatChip("⭐", "$stars/${PLANETS.size * 3}", "estrellas", C.sun, Modifier.weight(1f))
        StatChip("🏷️", "${g.save.stickers.size}", "stickers", C.sky, Modifier.weight(1f).tap { g.album() })
    }
}

@Composable
private fun StatChip(icon: String, value: String, label: String, col: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.panel(shape).border(2.dp, col.copy(alpha = .55f), shape).padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, style = body(18))
            Text(value, style = display(20, col))
        }
        Text(label, style = body(12, C.muted), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Tres botones para elegir la dificultad, con estrellas (1, 2 o 3). */
@Composable
fun LevelPicker(g: Game, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().panel(shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Dificultad", style = display(19))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Level.entries.forEachIndexed { i, l ->
                val on = g.level == l
                val col = listOf(C.leaf, C.sun, C.berry)[i]
                CandyBox(
                    if (on) col else Color(0xFF3F3299), 16.dp, Modifier.weight(1f).border(3.dp, if (on) Color.White else Color.Transparent, RoundedCornerShape(16.dp)),
                    padding = PaddingValues(vertical = 6.dp), onClick = { g.changeLevel(l) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            repeat(i + 1) {
                                Canvas(Modifier.size(16.dp)) { group(s = size.width / 24f) { path(STAR, Color(0xFFFFD84A), Color(0xFF8A5A00), 1.5f) } }
                            }
                        }
                        Text(l.label, style = display(18, if (on) C.onColor else C.ink))
                    }
                }
            }
        }
        Text(g.level.desc, style = body(14, C.muted), textAlign = TextAlign.Center)
    }
}

@Composable
fun PickerScreen(g: Game) {
    LevelPicker(g, Modifier.popIn(0))
    GameCard {
        PLANETS.chunked(2).forEachIndexed { ri, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { ci, p ->
                    CandyBox(
                        Color(0xFF3A2E92), 22.dp, Modifier.weight(1f).popIn(1 + ri * 2 + ci), depth = 6.dp,
                        padding = PaddingValues(horizontal = 10.dp, vertical = 12.dp), onClick = { g.play(p.key, Ctx(reto = false)) },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlanetBall(p, 76.dp)
                            Text(p.name, style = display(21).copy(shadow = Shadow(Color(0x88000000), Offset(0f, 2f), 3f)), textAlign = TextAlign.Center)
                            Text(p.desc, style = body(14, Color(0xFFD4CCFF)), textAlign = TextAlign.Center)
                            StarsRow(g.save.best(p.key), 20.dp)
                        }
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
    val t by rememberTime()
    val tm = rememberTextMeasurer()
    GameCard {
        GalaxyRoute(g, pi)
        Box(Modifier.scene(16f / 9f, Ink, 22.dp, 3.dp)) {
            Canvas(Modifier.fillMaxSize()) { landing(t, p, tm) }
            Sparkles(Color(0xFFFFF3B0), 10)
        }
        Text(p.name, style = display(28, C.sun))
        Text(p.desc, style = body(16, C.muted, FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Buttons {
            Ghost("Aterrizar", primary = true) { g.play(p.key, Ctx(reto = true, pi = pi)) }
            Ghost("Salir") { g.home() }
        }
    }
}

@Composable
fun EndScreen(g: Game, e: Screen.End) {
    val time = rememberTime()
    GameCard {
        // El planeta sobre rayos de luz que giran; más rayos y más dorados cuantas más estrellas.
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(170.dp)) {
                val c = center
                val rays = 8 + e.stars * 4
                val col = if (e.stars > 0) C.sun else C.grape
                spin(time.value * 12f, c.x, c.y) {
                    for (i in 0 until rays) {
                        val a0 = i * 2f * PI.toFloat() / rays
                        val a1 = a0 + PI.toFloat() / rays * .6f
                        val rr = size.minDimension / 2
                        drawPath(
                            androidx.compose.ui.graphics.Path().apply {
                                moveTo(c.x, c.y)
                                lineTo(c.x + cos(a0) * rr, c.y + sin(a0) * rr)
                                lineTo(c.x + cos(a1) * rr, c.y + sin(a1) * rr)
                                close()
                            },
                            col.copy(alpha = .35f),
                        )
                    }
                }
            }
            PlanetBall(planet(e.k), 110.dp)
        }
        AnimatedStars(e.stars, 64.dp) { i -> g.tone(660 + i * 220) }
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
            Text("por superar los ${RETO.size} planetas del desafío en modo ${g.level.label.lowercase()}", style = body(15, ink), textAlign = TextAlign.Center)
            RETO.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { k ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(planet(k).name, style = body(13, ink, FontWeight.ExtraBold), textAlign = TextAlign.Center)
                            StarsRow(g.runStars(k), 14.dp)
                        }
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
        AnimatedStars(if (e.ok >= 12) 3 else if (e.ok >= 7) 2 else 1, 64.dp) { i -> g.tone(660 + i * 220) }
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

/** Elegir y probar la voz del dictado, la velocidad, e instalar voces de mejor calidad. */
@Composable
fun VoiceScreen(g: Game) {
    val sp = g.speech
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var rate by remember { mutableStateOf(g.save.rate) }
    fun open(action: String) {
        try {
            ctx.startActivity(android.content.Intent(action).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            g.say(Frases.SIN_AJUSTES)
        }
    }
    GameCard {
        // Interruptor: Marty y Borrón leen en voz alta sus mensajes
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Marty habla", style = display(19))
                Text("Marty y Borrón leen en voz alta lo que dicen", style = body(14, C.muted))
            }
            CandyBox(if (sp.hostOn) C.leaf else Color(0xFF4A3BA8), 50.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), onClick = {
                sp.setHost(!sp.hostOn)
                if (sp.hostOn) g.say(Frases.SOY_MARTY)
            }) { Text(if (sp.hostOn) "Sí" else "No", style = display(18, if (sp.hostOn) C.onColor else C.ink)) }
        }
        Buttons {
            Ghost("Probar a Marty", icon = { SpeakerIcon() }) { g.say(Frases.SOY_MARTY) }
            Ghost("Probar a Borrón", icon = { SpeakerIcon() }) { g.say(Frases.SOY_BORRON) }
        }
        Text("Velocidad", style = display(19))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Lenta" to .75f, "Normal" to .9f, "Rápida" to 1.05f).forEach { (label, v) ->
                val on = kotlin.math.abs(rate - v) < .01f
                val s = RoundedCornerShape(16.dp)
                Box(
                    Modifier.weight(1f).clip(s).background(if (on) C.sun else C.space2).border(3.dp, if (on) Color.White else C.line, s)
                        .tap {
                            rate = v
                            g.save.rate = v
                            g.speak("Hola, soy Marty. Árbol, pingüino.")
                        }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = display(17, if (on) C.onColor else C.ink)) }
            }
        }
        Text("Voces en español de este teléfono", style = display(19), textAlign = TextAlign.Center)
        if (sp.options.isEmpty()) {
            Hint(if (sp.ready) "Este teléfono no muestra la lista de voces; se usa la voz en español predeterminada." else "No se encontró ninguna voz en español. Instala una con el botón de abajo.")
        }
        sp.options.forEach { v ->
            val on = v.id == sp.current
            val s = RoundedCornerShape(18.dp)
            Row(
                Modifier.fillMaxWidth().clip(s).background(if (on) C.space2 else C.card).border(if (on) 3.dp else 2.dp, if (on) C.leaf else C.line, s)
                    .tap { sp.choose(v.id); g.speak("Hola, soy Marty. Árbol, pingüino, murciélago.") }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(50)).border(3.dp, if (on) C.leaf else C.muted, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    if (on) Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(C.leaf))
                }
                Column(Modifier.weight(1f)) {
                    Text(v.label, style = display(18))
                    Text(v.detail, style = body(14, C.muted))
                }
                Ghost("Probar", icon = { SpeakerIcon() }) { sp.choose(v.id); g.speak("Hola, soy Marty. Árbol, pingüino, murciélago.") }
            }
        }
        Hint("Consejo: las voces \"en línea\" son las más naturales y la app las usa cuando hay internet. Si todas suenan robóticas, instala las voces de Google (Speech Services by Google) y descarga la voz en español.")
        Buttons {
            Ghost("Instalar voces") { open(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA) }
            Ghost("Ajustes de voz") { open("com.android.settings.TTS_SETTINGS") }
            Ghost("Volver", primary = true) { g.home() }
        }
    }
}
