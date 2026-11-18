package com.billkevin91.martypalabras

import java.text.Normalizer
import java.util.zip.CRC32

/* Todo lo que dicen Marty y Borrón, en un solo lugar.
 *
 * Cada frase sabe cómo se arma su audio: las frases fijas tienen su propia grabación y las que llevan
 * una palabra del juego se arman por partes ("¡Muy bien!" + "árbol"). La prueba FrasesTest vuelca la
 * lista completa de grabaciones necesarias, y el generador de voz (tools/voz) las crea con una voz
 * neural natural. Si falta alguna grabación, se usa la voz del teléfono.
 */

/** Un trozo de audio: una frase grabada (con la voz de Marty o la de Borrón) o una palabra del juego. */
sealed interface Parte {
    data class Frase(val texto: String, val borron: Boolean) : Parte
    data class Palabra(val texto: String) : Parte
}

/** Lo que se muestra en el globo, quién lo dice y cómo suena. */
data class Line(val text: String, val who: Who, val parts: List<Parte>)

/** Nombre del archivo de una frase grabada: assets/voz/f/<crc32 del texto>.m4a */
fun fraseArchivo(texto: String): String {
    val crc = CRC32()
    crc.update(Normalizer.normalize(texto, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8))
    return "%08x.m4a".format(crc.value)
}

/** Nombre base del archivo de una palabra: sin tildes, minúsculas, la ñ como "ny" (niño → ninyo). */
fun palabraClave(texto: String): String =
    norm(texto.lowercase().replace("ñ", "ny")).replace(Regex("[^a-z]"), "")

object Frases {
    private val fijas = mutableListOf<Parte.Frase>()

    private fun reg(texto: String, borron: Boolean = false): Parte.Frase = Parte.Frase(texto, borron).also { fijas += it }

    private fun marty(texto: String) = reg(texto).let { Line(texto, Who.MARTY, listOf(it)) }
    private fun borron(texto: String) = reg(texto, true).let { Line(texto, Who.BORRON, listOf(it)) }

    /* ---------- Menús ---------- */
    val BIENVENIDA = marty("¡Bienvenido al concurso galáctico! ¡Soy Marty, tu presentador! ¡Vamos, vamos, vamos!")
    private val HOLA_DE_NUEVO = reg("¡Hola! ¿A qué planeta vamos hoy?")
    fun hola(nombre: String) = Line("¡Hola, $nombre! ¿A qué planeta vamos hoy?", Who.MARTY, listOf(HOLA_DE_NUEVO))
    val ELIGE_PLANETA = marty("Elige un planeta. Aquí no hay prisa: practica lo que quieras.")
    val NOMBRE = marty("¿Cómo te llamas, astronauta? Tu nombre irá en el diploma.")
    val VOZ = marty("Elige la voz que mejor se entienda. Toca \"Probar\" para escucharla.")
    val SOY_MARTY = marty("¡Hola! Soy Marty, tu presentador. ¡Vamos a jugar con las palabras!")
    val SOY_BORRON = borron("¡Jajaja! Soy Borrón y voy a ensuciar todas tus palabras.")
    val SIN_AJUSTES = marty("No pude abrir los ajustes de voz de este teléfono. Búscalos en Ajustes, Accesibilidad, Texto a voz.")

    fun nivel(l: Level) = when (l) {
        Level.FACIL -> NIVEL_FACIL
        Level.NORMAL -> NIVEL_NORMAL
        Level.DIFICIL -> NIVEL_DIFICIL
    }
    private val NIVEL_FACIL = marty("¡Modo fácil! Palabras cortas y te ayudo más.")
    private val NIVEL_NORMAL = marty("¡Modo normal! Palabras de todo tipo.")
    private val NIVEL_DIFICIL = marty("¡Modo difícil! Palabras largas y con tildes. ¡Tú puedes!")

    /* ---------- Desafío ---------- */
    private val LLEGADA = PLANETS.associate { p -> p.key to marty("Llegamos a ${p.name}. ${INTRO[p.key] ?: p.desc}") }
    fun llegada(k: String) = LLEGADA.getValue(k)

    private val DIPLOMA = reg("¡Lo lograste! Superaste los ${RETO.size} planetas.")
    fun diploma(nombre: String) = Line("¡Lo lograste, $nombre! Superaste los ${RETO.size} planetas.", Who.MARTY, listOf(DIPLOMA))

    private val SUPERASTE = PLANETS.associate { p ->
        p.key to (1..3).associate { st -> st to marty("¡Superaste ${p.name}! Ganaste $st ${if (st == 1) "estrella" else "estrellas"}.") }
    }
    private val CAMPEON = PLANETS.associate { p -> p.key to marty("¡Eres un campeón de ${p.name}!") }
    val NECESITAS_3 = marty("Necesitas al menos 3 aciertos para pasar. ¡Inténtalo otra vez!")
    val SIGUE = marty("¡Muy bien! Sigue practicando.")
    val BUEN_INTENTO = marty("¡Buen intento! Practiquemos más.")

    fun finPlaneta(k: String, reto: Boolean, pass: Boolean, st: Int): Line = when {
        reto && pass -> SUPERASTE.getValue(k).getValue(st.coerceIn(1, 3))
        reto -> NECESITAS_3
        st == 3 -> CAMPEON.getValue(k)
        st >= 1 -> SIGUE
        else -> BUEN_INTENTO
    }

    /* ---------- Durante una pregunta ---------- */
    val CASI = marty("¡Casi! Prueba otra vez.")
    private val FELICITA = listOf("¡Muy bien!", "¡Excelente!", "¡Lo lograste!", "¡Qué listo eres!", "¡Genial!", "¡Fantástico!", "¡Eres una estrella!").map { reg(it) }
    fun acierto(palabra: String): Line = FELICITA.random().let { Line("${it.texto} $palabra", Who.MARTY, listOf(it, Parte.Palabra(palabra))) }

    private val SE_ESCRIBE = reg("Se escribe")
    private val SIGAMOS = reg("¡Sigamos!")
    fun seEscribe(palabra: String) = Line("Se escribe $palabra. ¡Sigamos!", Who.MARTY, listOf(SE_ESCRIBE, Parte.Palabra(palabra), SIGAMOS))

    // Pantanal
    val PANTANO_INICIO = marty("Toca los nenúfares para descubrir la palabra. ¡Cuidado con las tildes!")
    val PANTANO_TILDE = marty("¡Cuidado! Esa letra sí está, pero lleva tilde, diéresis o es otra letra.")
    val PANTANO_NO_ESTA = marty("Esa letra no está.")
    private val JAJAJA_ERA = reg("¡Jajaja! Era", true)
    fun pantanoPerdiste(palabra: String) = Line("¡Jajaja! Era $palabra.", Who.BORRON, listOf(JAJAJA_ERA, Parte.Palabra(palabra)))

    // Burbujeo
    val BURBUJA_INICIO = marty("Escucha la palabra y atrapa las burbujas con sus letras, en orden.")
    val BURBUJA_SIN_VOZ = marty("Tu teléfono no puede hablar. Mira el dibujo y atrapa las letras en orden.")
    val BURBUJA_ESA_ES = marty("¡Esa es! Busca la siguiente letra.")
    val BURBUJA_TILDE = marty("¡Casi! Es otra letra: fíjate en la tilde.")
    val BURBUJA_NO_ES = marty("Esa letra no es. Busca la siguiente en la palabra.")

    // Telaraña
    val TELA_INICIO = marty("Escucha la palabra y escríbela en el teclado. ¡Las arañas te traerán cada letra!")
    val TELA_SIN_VOZ = marty("Tu teléfono no puede hablar. Mira la pantalla y escribe la palabra en el teclado.")
    val TELA_TILDES = marty("¡Casi! Revisa las tildes, la ñ y la ü.")
    val TELA_OTRA_VEZ = marty("Todavía no. Escucha otra vez.")
    private val TELA_LETRAS = (1..14).associateWith { n -> marty("Casi. La palabra tiene $n letras.") }
    fun telaLetras(n: Int) = TELA_LETRAS[n] ?: Line("Casi. La palabra tiene $n letras.", Who.MARTY, emptyList())

    // Anagrama
    val ANAGRAMA_INICIO = marty("Toca los conejitos en orden para formar la palabra.")
    val ANAGRAMA_NO_VA = marty("¡Ese conejito no va ahí! Busca la letra que sigue.")
    val ANAGRAMA_SALTA = marty("¡Salta, conejito! Busca la siguiente letra.")

    // Letra perdida y Lectura
    val FALTA_INICIO = marty("¡Al tren le falta una letra! Toca la caja correcta. Fíjate bien en las tildes.")
    val LEE_INICIO = marty("¡Festival de barriletes! Lee y toca el barrilete con la palabra del dibujo.")

    /* ---------- Ataque de las palabras ---------- */
    val ATAQUE_INICIO = borron("¡Jajaja! Voy a ensuciar todas las palabras. ¡A ver si las escribes bien!")
    val ATAQUE_GANASTE = borron("¡Me ganaste! Escribes muy bien.")
    val ATAQUE_PERDISTE = borron("¡Jajaja! Esta vez gané yo. ¿Revancha?")
    private val BIEN = reg("¡Bien!")
    fun ataqueBien(palabra: String) = Line("¡Bien! $palabra", Who.MARTY, listOf(BIEN, Parte.Palabra(palabra)))
    private val JAJAJA_SE_ESCRIBE = reg("¡Jajaja! Se escribe", true)
    private val UNA_MANCHA = reg("¡Una mancha más!", true)
    fun ataqueMancha(palabra: String) =
        Line("¡Jajaja! Se escribe $palabra. ¡Una mancha más!", Who.BORRON, listOf(JAJAJA_SE_ESCRIBE, Parte.Palabra(palabra), UNA_MANCHA))

    /** Todas las frases que hay que grabar (sin repetir). */
    fun grabadas(): List<Parte.Frase> = fijas.distinctBy { it.texto to it.borron }
}
