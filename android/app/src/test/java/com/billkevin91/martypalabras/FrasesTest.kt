package com.billkevin91.martypalabras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Comprueba las frases de Marty y Borrón y escribe la lista de grabaciones necesarias en
 * build/voz/ (frases.tsv y palabras.tsv), que usa el generador de voz tools/voz/generar.py.
 */
class FrasesTest {

    @Test
    fun cadaFraseTieneUnArchivoDistinto() {
        val frases = Frases.grabadas()
        val porArchivo = frases.groupBy { fraseArchivo(it.texto) }.filter { it.value.size > 1 }
        assertTrue("Dos frases comparten archivo: $porArchivo", porArchivo.isEmpty())
    }

    @Test
    fun cadaPalabraTieneUnArchivoDistinto() {
        val dup = WORDS.groupBy { palabraClave(it.text) }.filter { it.value.size > 1 }.mapValues { e -> e.value.map { it.text } }
        assertTrue("Dos palabras comparten archivo: $dup", dup.isEmpty())
    }

    @Test
    fun lasFrasesConPalabraSeArmanPorPartes() {
        val l = Frases.seEscribe("árbol")
        assertEquals("Se escribe árbol. ¡Sigamos!", l.text)
        assertEquals(3, l.parts.size)
        assertEquals(Parte.Palabra("árbol"), l.parts[1])
        val a = Frases.acierto("pingüino")
        assertTrue(a.text.endsWith(" pingüino"))
        assertEquals(Parte.Palabra("pingüino"), a.parts.last())
        assertEquals(Who.BORRON, Frases.pantanoPerdiste("luna").who)
    }

    @Test
    fun todosLosFinalesDePlanetaTienenFrase() {
        for (p in PLANETS) for (st in 0..3) for (reto in listOf(true, false)) {
            val l = Frases.finPlaneta(p.key, reto, st >= 1, st)
            assertTrue(l.parts.isNotEmpty())
        }
        for (w in WORDS) assertTrue("Falta frase de ${w.text.length} letras", Frases.telaLetras(w.text.length).parts.isNotEmpty())
    }

    @Test
    fun nuncaRepiteLaMismaFraseDosVecesSeguidas() {
        var antes = ""
        repeat(200) {
            val l = Frases.casi()
            assertTrue("Se repitió: ${l.text}", l.text != antes)
            antes = l.text
        }
        var felicita = ""
        repeat(200) {
            val l = Frases.acierto("sol")
            assertTrue("Se repitió: ${l.text}", l.text != felicita)
            felicita = l.text
        }
    }

    @Test
    fun laUltimaPreguntaTieneSuFrase() {
        val s = Session("lee", Ctx(reto = false), WORDS.take(N), Level.NORMAL)
        s.i = N - 1
        assertEquals("¡La última es la más divertida!", Frases.siguiente(s).text)
        s.i = 1
        assertTrue(Frases.siguiente(s).text != "¡La última es la más divertida!")
    }

    @Test
    fun escribeLaListaDeGrabaciones() {
        val dir = File("build/voz").apply { mkdirs() }
        File(dir, "frases.tsv").writeText(
            Frases.grabadas().joinToString("\n") { "${fraseArchivo(it.texto)}\t${if (it.borron) "borron" else "marty"}\t${it.texto}" } + "\n"
        )
        File(dir, "palabras.tsv").writeText(WORDS.joinToString("\n") { "${palabraClave(it.text)}.m4a\t${it.text}" } + "\n")
        assertTrue(File(dir, "frases.tsv").length() > 0)
    }
}
