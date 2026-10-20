package com.billkevin91.martypalabras

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/* Temporal: pantallas en construcción. */
@Composable fun AnagramaGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
