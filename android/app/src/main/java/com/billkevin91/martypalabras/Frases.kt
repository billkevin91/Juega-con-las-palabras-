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

/**
 * Lo que se muestra en el globo, quién lo dice y cómo suena.
 * [kind]: tipo de comentario (para no repetirlo seguido ni decirlo dos veces en pocos segundos).
 * [voice]: false = solo se muestra en el globo (comentarios de cada letra, que serían repetitivos).
 */
/**
 * Una frase de Marty o Borrón. [chat] = comentario de una jugada ("¡Casi!", "¡Otra palabra!"): por defecto
 * solo aparece escrito en el globo (para que Marty no hable todo el rato); se escucha si se toca a Marty.
 */
data class Line(val text: String, val who: Who, val parts: List<Parte>, val kind: String? = null, val voice: Boolean = true, val chat: Boolean = false)

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

    /** Varias formas de decir lo mismo: se elige una distinta cada vez. */
    private fun grupo(kind: String, vararg textos: String, borron: Boolean = false, voz: Boolean = true): List<Line> =
        textos.map { t -> reg(t, borron).let { Line(t, if (borron) Who.BORRON else Who.MARTY, listOf(it), kind, voz, chat = true) } }

    private val ultima = HashMap<String, String>()

    /** Elige una variante al azar sin repetir la última que se usó de ese grupo. */
    fun <T> sinRepetir(kind: String, opciones: List<T>, texto: (T) -> String): T {
        val antes = ultima[kind]
        val elegida = opciones.filter { texto(it) != antes }.ifEmpty { opciones }.random()
        ultima[kind] = texto(elegida)
        return elegida
    }

    private fun pick(g: List<Line>) = sinRepetir(g.first().kind ?: "", g) { it.text }

    /* ---------- Menús ---------- */
    val BIENVENIDA = marty("¡Bienvenido al concurso galáctico! ¡Soy Marty, tu presentador! ¡Vamos, vamos, vamos!")
    private val HOLA_DE_NUEVO = reg("¡Hola! ¿A qué planeta vamos hoy?")
    fun hola(nombre: String) = Line("¡Hola, $nombre! ¿A qué planeta vamos hoy?", Who.MARTY, listOf(HOLA_DE_NUEVO), chat = true)
    val ELIGE_PLANETA = marty("Elige un planeta. Aquí no hay prisa: practica lo que quieras.").copy(chat = true)

    /** Lo que cuenta Marty cuando lo tocas: datos curiosos de ortografía, chistes y ánimos. */
    private val CHARLA = grupo(
        "charla",
        "¿Sabías que la eñe es una letra que casi solo existe en español?",
        "¡Ji, ji! ¡Me haces cosquillas!",
        "Un truco: si la palabra termina en ene, ese o vocal y suena fuerte al final, lleva tilde. Como camión.",
        "¿Sabes por qué las letras nunca se pierden? ¡Porque siempre siguen el abecedario!",
        "La diéresis son los dos puntitos sobre la u. Hacen que suene, como en pingüino.",
        "¡Eres el mejor concursante de toda la galaxia!",
        "En mi planeta las palabras flotan. ¡Hay que atraparlas con red!",
        "¿Qué le dijo la be a la uve? ¡Suenas igualito que yo!",
        "Leer un ratito cada día te hace un campeón de las palabras.",
        "¡Equivocarse está bien! Así es como aprende el cerebro.",
        "La hache es muy tímida: está en las palabras, pero no hace ningún sonido.",
        "¡Vamos, vamos, vamos! ¡A jugar!",
    )
    fun charla() = pick(CHARLA)
    val NOMBRE = marty("¿Cómo te llamas, astronauta? Tu nombre irá en el diploma.")
    val VOZ = marty("Elige la voz que mejor se entienda. Toca \"Probar\" para escucharla.")
    val SOY_MARTY = marty("¡Hola! Soy Marty, tu presentador. ¡Vamos a jugar con las palabras!")
    val SOY_BORRON = borron("¡Jajaja! Soy Borrón y voy a ensuciar todas tus palabras.")
    private val ALBUM_VACIO = marty("Tu álbum está vacío. ¡Acierta palabras para ganar stickers!")
    private val ALBUM = marty("¡Mira tu álbum! Cada palabra que aciertas es un sticker nuevo.")
    private val ALBUM_LLENO = marty("¡Completaste el álbum! ¡Eres una leyenda galáctica!")
    fun album(n: Int, total: Int) = when {
        n == 0 -> ALBUM_VACIO
        n >= total -> ALBUM_LLENO
        else -> ALBUM
    }
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

    /* ---------- Escenas animadas ---------- */
    val CINE_INTRO_1 = marty("¡Atención, atención! Desde el otro lado de la galaxia...")
    val CINE_INTRO_2 = marty("¡Llega el concurso más divertido del universo!")
    val CINE_INTRO_3 = marty("¡Soy Marty! ¡Y hoy vamos a jugar con las palabras!")
    private val RUMBO = PLANETS.associate { p -> p.key to marty("¡Rumbo a ${p.name}! ¡Agárrate fuerte!") }
    fun rumbo(k: String) = RUMBO.getValue(k)
    val CINE_BORRON_LLEGA = borron("¡Jajaja! ¡Sorpresa! ¡Vengo a manchar todas las palabras!")
    val CINE_HUYE_1 = marty("¡Alto ahí, Borrón! ¡Deja en paz las palabras!")
    val CINE_HUYE_2 = borron("¡Aaay! ¡Un borrador gigante! ¡Me voy, me voy!")
    val CINE_HUYE_3 = marty("¡Gracias a ti, las palabras están limpias otra vez!")
    val CINE_FINAL = marty("¡Increíble! ¡Superaste los seis planetas! ¡Aquí está tu diploma!")

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
    private val CASI = grupo(
        "casi", "¡Casi! Prueba otra vez.", "¡Uy, esa no! Inténtalo de nuevo.", "¡No te rindas! Otra vez.",
        "Mmm, no es esa. ¡Tú puedes!", "¡Casi casi! Mira bien.", "¡Ups! Prueba con otra.",
    )
    fun casi() = pick(CASI)

    /** Al empezar la segunda pregunta en adelante (las instrucciones solo se dicen en la primera). */
    private val SIGUIENTE = grupo(
        "siguiente", "¡Vamos con la siguiente!", "¡Otra palabra!", "¡A ver esta!", "¡Siguiente! ¡Tú puedes!",
        "¡Aquí viene otra!", "¡Seguimos!", "¡La última es la más divertida!",
    )
    fun siguiente(s: Session): Line = if (s.i == N - 1) SIGUIENTE.last() else pick(SIGUIENTE.dropLast(1))

    private val FELICITA = listOf(
        "¡Muy bien!", "¡Excelente!", "¡Lo lograste!", "¡Qué listo eres!", "¡Genial!", "¡Fantástico!", "¡Eres una estrella!",
        "¡Perfecto!", "¡Increíble!", "¡Así se hace!",
    ).map { reg(it) }
    fun acierto(palabra: String): Line =
        sinRepetir("felicita", FELICITA) { it.texto }.let { Line("${it.texto} $palabra", Who.MARTY, listOf(it, Parte.Palabra(palabra)), chat = true) }

    private val SE_ESCRIBE = reg("Se escribe")
    private val SIGAMOS = reg("¡Sigamos!")
    fun seEscribe(palabra: String) = Line("Se escribe $palabra. ¡Sigamos!", Who.MARTY, listOf(SE_ESCRIBE, Parte.Palabra(palabra), SIGAMOS))

    // Pantanal
    val PANTANO_INICIO = marty("Toca los nenúfares para descubrir la palabra. ¡Cuidado con las tildes!")
    val PANTANO_TILDE = marty("¡Cuidado! Esa letra sí está, pero lleva tilde, diéresis o es otra letra.")
    private val PANTANO_NO_ESTA = grupo("no-esta", "Esa letra no está.", "¡Esa no está en la palabra!", "No hay esa letra. ¡Prueba otra!", "¡Splash! Esa no va.")
    fun pantanoNoEsta() = pick(PANTANO_NO_ESTA)
    private val JAJAJA_ERA = reg("¡Jajaja! Era", true)
    fun pantanoPerdiste(palabra: String) = Line("¡Jajaja! Era $palabra.", Who.BORRON, listOf(JAJAJA_ERA, Parte.Palabra(palabra)))

    // Burbujeo
    val BURBUJA_INICIO = marty("Escucha la palabra y atrapa las burbujas con sus letras, en orden.")
    val BURBUJA_SIN_VOZ = marty("Tu teléfono no puede hablar. Mira el dibujo y atrapa las letras en orden.")
    private val BURBUJA_ESA_ES = grupo("esa-es", "¡Esa es! Busca la siguiente letra.", "¡Bien atrapada! ¿Cuál sigue?", "¡Sí! Ahora la siguiente.", "¡Pop! Vamos por otra.", voz = false)
    fun burbujaEsaEs() = pick(BURBUJA_ESA_ES)
    val BURBUJA_TILDE = marty("¡Casi! Es otra letra: fíjate en la tilde.")
    private val BURBUJA_NO_ES = grupo("no-es", "Esa letra no es. Busca la siguiente en la palabra.", "¡Esa no va ahora! Busca la que sigue.", "¡Ups! Esa burbuja no era.")
    fun burbujaNoEs() = pick(BURBUJA_NO_ES)

    // Telaraña
    val TELA_INICIO = marty("Escucha la palabra y escríbela en el teclado. ¡Las arañas te traerán cada letra!")
    val TELA_SIN_VOZ = marty("Tu teléfono no puede hablar. Mira la pantalla y escribe la palabra en el teclado.")
    val TELA_TILDES = marty("¡Casi! Revisa las tildes, la ñ y la ü.")
    private val TELA_OTRA_VEZ = grupo("otra-vez", "Todavía no. Escucha otra vez.", "Casi. Toca Escuchar y revisa letra por letra.", "¡Sigue intentando! Escucha con atención.")
    fun telaOtraVez() = pick(TELA_OTRA_VEZ)
    private val TELA_LETRAS = (1..14).associateWith { n -> marty("Casi. La palabra tiene $n letras.") }
    fun telaLetras(n: Int) = TELA_LETRAS[n] ?: Line("Casi. La palabra tiene $n letras.", Who.MARTY, emptyList())

    // Anagrama
    val ANAGRAMA_INICIO = marty("Toca los conejitos en orden para formar la palabra.")
    private val ANAGRAMA_NO_VA = grupo("no-va", "¡Ese conejito no va ahí! Busca la letra que sigue.", "¡Ese no! ¿Qué letra sigue?", "¡Uy! Ese conejito espera su turno.")
    fun anagramaNoVa() = pick(ANAGRAMA_NO_VA)
    private val ANAGRAMA_SALTA = grupo("salta", "¡Salta, conejito! Busca la siguiente letra.", "¡Boing! ¿Quién sigue?", "¡Muy bien! Otro conejito.", "¡Eso! Sigue así.", voz = false)
    fun anagramaSalta() = pick(ANAGRAMA_SALTA)

    // Letra perdida y Lectura
    val FALTA_INICIO = marty("¡Al tren le falta una letra! Toca la caja correcta. Fíjate bien en las tildes.")
    val LEE_INICIO = marty("¡Festival de barriletes! Lee y toca el barrilete con la palabra del dibujo.")

    /* ---------- Ataque de las palabras ---------- */
    val ATAQUE_INICIO = borron("¡Jajaja! Voy a ensuciar todas las palabras. ¡A ver si las escribes bien!")
    val ATAQUE_GANASTE = borron("¡Me ganaste! Escribes muy bien.")
    val ATAQUE_PERDISTE = borron("¡Jajaja! Esta vez gané yo. ¿Revancha?")
    private val BIEN = listOf("¡Bien!", "¡Correcto!", "¡Eso es!", "¡Así se escribe!").map { reg(it) }
    fun ataqueBien(palabra: String) =
        sinRepetir("bien", BIEN) { it.texto }.let { Line("${it.texto} $palabra", Who.MARTY, listOf(it, Parte.Palabra(palabra)), "bien") }
    private val MANCHA_ANTES = listOf("¡Jajaja! Se escribe", "¡Te manché! Se escribe", "¡Splash! Se escribe").map { reg(it, true) }
    private val MANCHA_DESPUES = listOf("¡Una mancha más!", "¡Jijiji!", "¡Qué sucio quedó!").map { reg(it, true) }
    fun ataqueMancha(palabra: String): Line {
        val a = sinRepetir("mancha-a", MANCHA_ANTES) { it.texto }
        val b = sinRepetir("mancha-b", MANCHA_DESPUES) { it.texto }
        return Line("${a.texto} $palabra. ${b.texto}", Who.BORRON, listOf(a, Parte.Palabra(palabra), b), "mancha")
    }

    /** Todas las frases que hay que grabar (sin repetir). */
    fun grabadas(): List<Parte.Frase> = fijas.distinctBy { it.texto to it.borron }
}
