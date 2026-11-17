package com.billkevin91.martypalabras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/** Sacudida corta cada vez que cambia [key] (como .shake en el HTML). */
@Composable
fun Modifier.shake(key: Int): Modifier {
    val a = remember { Animatable(1f) }
    LaunchedEffect(key) {
        if (key > 0) {
            a.snapTo(0f)
            a.animateTo(1f, tween(400))
        }
    }
    return graphicsLayer { rotationZ = -8f * sin(2f * PI.toFloat() * a.value) }
}

@Composable
fun PicCircle(text: String) {
    Box(
        Modifier.size(132.dp).clip(CircleShape).background(C.space2).border(4.dp, C.sun, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = TextStyle(fontSize = 70.sp, color = C.muted, fontFamily = Baloo)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CenterFlow(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

