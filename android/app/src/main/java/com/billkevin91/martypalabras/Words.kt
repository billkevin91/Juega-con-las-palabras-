package com.billkevin91.martypalabras

import androidx.compose.ui.graphics.Color
import java.text.Normalizer

/** Una palabra del juego: cómo se escribe, su dibujo y errores comunes (para el Ataque). */
data class Word(val text: String, val pic: String, val wrongs: List<String>)

private fun w(text: String, pic: String, wrongs: String = "") =
    Word(text, pic, if (wrongs.isEmpty()) emptyList() else wrongs.split(","))

val WORDS: List<Word> = listOf(
    w("árbol", "🌳", "arbol,árvol,arból"), w("camión", "🚚", "camion,kamión,camíon"), w("avión", "✈️", "avion,abión,avíon"),
    w("pingüino", "🐧", "pinguino,pinjüino,pingüíno"), w("murciélago", "🦇", "murcielago,murciégalo,murziélago"),
    w("jirafa", "🦒", "girafa,jirrafa,jiraffa"), w("hormiga", "🐜", "ormiga,hormija,hormica"), w("elefante", "🐘", "helefante,elefanté,elefnate"),
    w("cebra", "🦓", "sebra,zebra,cevra"), w("abeja", "🐝", "abeha,avega,habeja"), w("pulpo", "🐙"), w("cangrejo", "🦀", "cangrejó,cangreho,canjrejo"),
    w("caballo", "🐴", "cavallo,caballó,cabayo"), w("oveja", "🐑", "obeja,ovexa,oveha"), w("rana", "🐸"), w("araña", "🕷️", "arana,arrana,arraña"),
    w("serpiente", "🐍", "cerpiente,serpiene,zerpiente"), w("búho", "🦉", "buho,vúho,búo"), w("delfín", "🐬", "delfin,delphín,delfim"),
    w("ballena", "🐳", "bayena,vallena,ballenna"), w("tiburón", "🦈", "tiburon,tivurón,tibúron"), w("manzana", "🍎", "mansana,manzaná,mazana"),
    w("fresa", "🍓"), w("zanahoria", "🥕", "sanahoria,zanaoria,zanahória"), w("sandía", "🍉", "sandia,zandía,sándia"), w("piña", "🍍", "pina,piñá,pinia"),
    w("aguacate", "🥑", "aguacáte,agüacate,aguakate"), w("galleta", "🍪", "gayeta,galeta,gallita"), w("pelota", "⚽"),
    w("guitarra", "🎸", "gitarra,guitara,guittara"), w("bicicleta", "🚲", "vicicleta,bisicleta,bicicletta"), w("cohete", "🚀", "cuete,cohéte,coete"),
    w("arcoíris", "🌈", "arcoiris,arcoíriz,arcoiriz"), w("volcán", "🌋", "volcan,bolcán,volkán"), w("isla", "🏝️", "ila,izla,hisla"),
    w("telescopio", "🔭", "telezcopio,telescopío,teliscopio"), w("corona", "👑"), w("castillo", "🏰", "castiyo,kastillo,castilo"),
    w("reloj", "🕰️", "relój,reloh,relox"), w("llave", "🔑", "yave,llabe,lave"), w("lápiz", "✏️", "lapiz,lápis,lapís"), w("libro", "📖", "livro,líbro,lybro"),
    w("loro", "🦜"), w("tortuga", "🐢", "tortuca,tortúga,tortugga"), w("mariposa", "🦋", "maripoza,mariposá,marripoza"),
    w("estrella", "⭐", "estreya,extrella,estrela"), w("luna", "🌙"), w("sol", "☀️"), w("banano", "🍌", "vanano,bananó,banáno"),
    w("niño", "👦", "nino,niñó,ninio"), w("montaña", "⛰️", "montana,montanya,móntaña"), w("cumpleaños", "🎂", "cumpleanos,cúmpleaños,cumpleaño"),
    w("corazón", "❤️", "corazon,corasón,korazón"), w("canción", "🎵", "cancion,canzión,cansión"), w("teléfono", "📞", "telefono,telefóno,teléfomo"),
    w("ratón", "🐭", "raton,ratun,rratón"), w("helado", "🍦", "elado,heládo,helato"), w("huevo", "🥚", "uevo,güevo,huebo"), w("hueso", "🦴", "ueso,huezo,güeso"),
    w("queso", "🧀", "keso,queco,qeso"), w("pájaro", "🐦", "pajaro,páharo,pájarro"), w("girasol", "🌻", "jirasol,girazol,girasól"),
    w("lluvia", "🌧️", "yuvia,lluvía,lubia"), w("nube", "☁️", "nuve,nupe,núbe"), w("barco", "🚢", "varco,barko,bárco"), w("silla", "🪑", "siya,cilla,silá"),
    w("pan", "🍞"), w("uva", "🍇", "uba,huva,úva"), w("tren", "🚂", "trem,trén,thren"), w("pato", "🦆"), w("oso", "🐻", "hoso,ozo,osó"),
    w("vaca", "🐮", "baca,vacá,bacca"), w("gato", "🐱", "gáto,gatto,cato"), w("perro", "🐶", "pero,perró,pherro"), w("casa", "🏠", "caza,cása,kasa"),
    w("flor", "🌸"), w("globo", "🎈", "glovo,globó,jlobo"), w("carro", "🚗", "karro,carró,cárro"), w("pez", "🐟", "pes,péz,pezz"),
).map { it.copy(text = Normalizer.normalize(it.text, Normalizer.Form.NFC)) }

data class PlanetDef(val key: String, val name: String, val desc: String, val a: Color, val b: Color, val ring: Boolean)

val PLANETS: List<PlanetDef> = listOf(
    PlanetDef("pantano", "Pantanal", "Adivina la palabra oculta", Color(0xFF7EE8A3), Color(0xFF1F8A55), false),
    PlanetDef("burbuja", "Burbujeo", "Atrapa las letras de la palabra que escuches", Color(0xFF8FDCFF), Color(0xFF2478C8), false),
    PlanetDef("tela", "Telaraña", "Escribe la palabra que escuches", Color(0xFFC4A8FF), Color(0xFF5A3FC0), true),
    PlanetDef("anagrama", "Anagrama", "Ordena las letras", Color(0xFFFFE07A), Color(0xFFE08A00), true),
    PlanetDef("falta", "Letra perdida", "Completa la palabra", Color(0xFFFF9DB4), Color(0xFFC23A64), false),
    PlanetDef("lee", "Lectura", "Elige la palabra del dibujo", Color(0xFFFFC58A), Color(0xFFD9631C), true),
)

fun planet(key: String): PlanetDef = PLANETS.first { it.key == key }

val RETO = listOf("pantano", "burbuja", "tela", "anagrama")

val INTRO = mapOf(
    "pantano" to "Aquí viven las ranas. Cuidado: hay tildes, eñes y diéresis escondidas.",
    "burbuja" to "Escucha la palabra y atrapa las burbujas con sus letras, en orden.",
    "tela" to "Las arañas tejen palabras. Escribe lo que escuches.",
    "anagrama" to "Las letras salieron volando. ¡Ponlas en orden!",
)

/** Preguntas por planeta. */
const val N = 5

val KEYS: List<String> = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".map { it.toString() } + "ÁÉÍÓÚÜ".map { it.toString() }

/** Quita tildes y diéresis (la ñ también pierde su virgulilla). */
fun norm(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

fun letters(s: String): List<String> = s.map { it.toString() }

fun poolFor(k: String): List<Word> = WORDS.filter {
    val l = it.text.length
    when (k) {
        "pantano" -> l >= 4
        "burbuja" -> l in 3..8
        "tela" -> l >= 3
        "anagrama" -> l in 4..8
        "falta" -> l >= 4
        else -> true
    }
}

fun starsOf(ok: Int): Int = when {
    ok >= 5 -> 3
    ok >= 4 -> 2
    ok >= 3 -> 1
    else -> 0
}
