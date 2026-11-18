package com.billkevin91.martypalabras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de las palabras y las reglas del juego (corren en la computadora, sin teléfono). */
class WordsTest {

    @Test
    fun cadaLetraDeCadaPalabraTieneSuTeclaEnElPantanal() {
        for (w in WORDS) for (c in letters(w.text.uppercase())) {
            assertTrue("La letra '$c' de \"${w.text}\" no está en el teclado del Pantanal", c in KEYS)
        }
    }

    @Test
    fun lasPalabrasNoSeRepiten() {
        val dup = WORDS.groupBy { it.text }.filter { it.value.size > 1 }.keys
        assertTrue("Palabras repetidas: $dup", dup.isEmpty())
    }

    @Test
    fun losErroresSonDistintosDeLaPalabraYEntreSi() {
        for (w in WORDS) {
            assertFalse("\"${w.text}\" aparece entre sus propios errores", w.text in w.wrongs)
            assertEquals("Errores repetidos en \"${w.text}\"", w.wrongs.size, w.wrongs.toSet().size)
            assertTrue("Error vacío en \"${w.text}\"", w.wrongs.none { it.isBlank() })
        }
    }

    @Test
    fun elAtaqueTienePalabrasSuficientes() {
        assertTrue(WORDS.count { it.wrongs.size >= 2 } >= 40)
    }

    @Test
    fun cadaPlanetaTienePalabrasEnCadaDificultad() {
        for (p in PLANETS) for (l in Level.entries) {
            val pool = poolFor(p.key, l)
            assertTrue("${p.name} en ${l.label} solo tiene ${pool.size} palabras", pool.size >= N * 2)
        }
    }

    @Test
    fun laDificultadOrdenaLasPalabras() {
        val facil = poolFor("tela", Level.FACIL).map { hardness(it.text) }.average()
        val dificil = poolFor("tela", Level.DIFICIL).map { hardness(it.text) }.average()
        assertTrue("Fácil ($facil) debería ser más sencillo que difícil ($dificil)", facil < dificil)
    }

    @Test
    fun losPlanetasRespetanSuLargoDePalabra() {
        for (l in Level.entries) {
            assertTrue(poolFor("anagrama", l).all { it.text.length in 4..8 })
            assertTrue(poolFor("burbuja", l).all { it.text.length in 3..8 })
            assertTrue(poolFor("pantano", l).all { it.text.length >= 4 })
        }
    }

    @Test
    fun normQuitaTildesYDieresis() {
        assertEquals("pinguino", norm("pingüino"))
        assertEquals("arbol", norm("árbol"))
        assertEquals("N", norm("Ñ"))
        assertEquals("A", norm("Á"))
    }

    @Test
    fun elDesafioRecorreLosSeisPlanetasUnaVez() {
        assertEquals(PLANETS.map { it.key }.toSet(), RETO.toSet())
        assertEquals(RETO.size, RETO.toSet().size)
        for (k in RETO) assertTrue("Falta la presentación de $k", INTRO.containsKey(k))
    }

    @Test
    fun lasEstrellasSeGananConAciertos() {
        assertEquals(0, starsOf(2))
        assertEquals(1, starsOf(3))
        assertEquals(2, starsOf(4))
        assertEquals(3, starsOf(5))
    }
}
