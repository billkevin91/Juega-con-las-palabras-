package com.billkevin91.martypalabras

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/* Temporal: pantallas en construcción. */
@Composable fun PantanoGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
@Composable fun BurbujaGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
@Composable fun TelaGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
@Composable fun AnagramaGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
@Composable fun FaltaGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
@Composable fun LeeGame(g: Game, s: Session) = QFrame(g, s) { Text(s.word.text, style = display(30)) }
@Composable fun AttackScreen(g: Game) = GameCard { Ghost("Menú") { g.home() } }
