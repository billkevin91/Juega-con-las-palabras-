package com.billkevin91.martypalabras

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Álbum de stickers: cada palabra acertada por primera vez se pega en el álbum. */

@Composable
fun AlbumScreen(g: Game) {
    val have = remember { g.save.stickers }
    val time = rememberTime()
    GameCard {
        Text(
            "${have.size} de ${WORDS.size} stickers",
            style = display(22, C.sun), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        // Barra de progreso del álbum.
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(C.space)) {
            Box(
                Modifier.fillMaxWidth(have.size / WORDS.size.toFloat()).height(14.dp).clip(RoundedCornerShape(7.dp))
                    .background(Brush.horizontalGradient(listOf(C.leaf, C.sun, C.berry))),
            )
        }
        Text("Toca un sticker para escuchar su palabra.", style = body(14, C.muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        WORDS.chunked(4).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { c, w ->
                    val i = r * 4 + c
                    Sticker(w, w.text in have, C.tiles[i % C.tiles.size], { wave(time.value + i * .37f, 3f) }, Modifier.weight(1f)) {
                        g.sfx(Sfx.POP)
                        g.speak(w.text)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Ghost("Volver al menú") { g.home() }
    }
}

/** Un sticker: con borde blanco troquelado y un leve vaivén; los que faltan se ven como silueta. */
@Composable
private fun Sticker(w: Word, owned: Boolean, col: Color, sway: () -> Float, modifier: Modifier, onTap: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .aspectRatio(.82f)
            .graphicsLayer { rotationZ = if (owned) sway() * 3f else 0f }
            .shadow(if (owned) 6.dp else 0.dp, shape)
            .clip(shape)
            .background(if (owned) androidx.compose.ui.graphics.lerp(col, Color.White, .55f) else C.space2)
            .border(3.dp, if (owned) Ink else C.line, shape)
            .then(if (owned) Modifier.tap(onClick = onTap) else Modifier)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (owned) {
            Text(w.pic, fontSize = 30.sp)
            Text(w.text, style = body(12, C.onColor), textAlign = TextAlign.Center, maxLines = 1)
        } else {
            Text(w.pic, fontSize = 30.sp, modifier = Modifier.graphicsLayer { alpha = .12f })
            Text("?", style = display(16, C.muted))
        }
    }
}

/** Aviso que cae desde arriba al ganar un sticker nuevo. */
@Composable
fun StickerToast(w: Word?) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = w != null,
            enter = slideInVertically(spring(Spring.DampingRatioMediumBouncy)) { -it * 2 } + scaleIn(initialScale = .6f),
            exit = slideOutVertically { -it * 2 } + fadeOut(),
        ) {
            // Se recuerda la última palabra para que no desaparezca el contenido durante la salida.
            val last = remember { arrayOfNulls<Word>(1) }
            if (w != null) last[0] = w
            val shown = last[0] ?: return@AnimatedVisibility
            val shape = RoundedCornerShape(22.dp)
            Row(
                Modifier.shadow(12.dp, shape).clip(shape)
                    .background(C.sun)
                    .border(3.dp, Ink, shape)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(shown.pic, fontSize = 34.sp)
                Column {
                    Text("¡Sticker nuevo!", style = display(20, C.onColor))
                    Text(shown.text, style = body(15, C.paperInk))
                }
            }
        }
    }
}
