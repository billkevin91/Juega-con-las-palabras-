# Marty y las Palabras

Juego educativo de ortografía en español. Marty, un presentador alienígena,
guía a los niños por planetas donde practican tildes, ñ, diéresis y dictado.

## Cómo se juega

- **Dificultad** (en el menú): *Fácil* usa palabras cortas y da más ayuda; *Difícil* usa palabras largas,
  con tildes, y da menos ayuda (menos vidas, más opciones engañosas, menos tiempo).
- **Desafío Galáctico**: recorre los 6 planetas (Lectura, Letra perdida, Anagrama, Burbujeo, Pantanal y
  Telaraña) y entrega un diploma.
- **Astroaventura**: juega libremente el planeta que quieras.
- **Ataque de las palabras**: elige la palabra bien escrita antes de que Borrón lo ensucie todo.

## La voz

La app usa la voz del teléfono y elige la mejor voz en español que encuentre (prefiere acento
latinoamericano y "calidad alta"). En **Menú → Voz de Marty** se puede probar y elegir otra voz, cambiar
la velocidad e instalar voces mejores. En la mayoría de teléfonos, las voces de Google
("Speech Services by Google") son las que suenan más naturales.

### Voz humana grabada (opcional)

Para que suene una persona de verdad, se pueden grabar las palabras y ponerlas en
`android/app/src/main/assets/voz/`. El nombre del archivo es la palabra en minúsculas, sin tildes, y con
la ñ escrita como `ny`: `arbol.mp3`, `pinguino.mp3`, `ninyo.mp3`, `montanya.mp3`. Se aceptan `.mp3`,
`.ogg`, `.m4a` y `.wav`. Si una palabra tiene grabación, se usa la grabación; si no, la voz del teléfono.

## App Android nativa (`android/`)

Escrita en **Kotlin con Jetpack Compose**: todo se dibuja con el Canvas de Android,
sin HTML ni WebView.

| Archivo | Contenido |
| --- | --- |
| `MainActivity.kt` | Arranque, botón atrás y estructura de la pantalla |
| `Game.kt` | Estado y reglas del juego (planetas, aciertos, estrellas, diploma, progreso guardado) |
| `Words.kt` | Palabras, dibujos, errores comunes y planetas |
| `Audio.kt` | Voz del teléfono (TextToSpeech) y pitidos |
| `Characters.kt` | Marty, Borrón, el globo-nave y la portada |
| `Menus.kt` | Menú, selector de planetas, nombre, resultados y diploma |
| `Pantano.kt` | Pantanal: ranas y nenúfares (adivinar la palabra) |
| `Burbuja.kt` | Burbujeo: océano y burbujas con letras |
| `Tela.kt` | Telaraña: cueva, arañas con lentes y teclado de la terminal |
| `Anagrama.kt` | Anagrama: pradera, islas flotantes y conejitos |
| `Falta.kt` | Letra perdida: el tren de las letras |
| `Lee.kt` | Lectura: festival de barriletes |
| `Simple.kt` | Piezas comunes (sacudida, botones de opciones) |
| `Attack.kt` | Ataque de las palabras (Borrón y sus manchas) |
| `Draw.kt`, `Art.kt`, `Ui.kt` | Ayudas de dibujo, piezas de escena, colores y botones |

### Trabajar con Android Studio

1. Clona el repositorio (o descárgalo como ZIP desde GitHub).
2. En Android Studio: **File → Open** y elige la carpeta **`android/`** (no la raíz).
3. Espera a que termine "Gradle sync" la primera vez (descarga las dependencias).
4. Conecta tu teléfono con la depuración USB activada, o crea un emulador, y pulsa **Run ▶**.

Requisitos: Android Studio reciente (Ladybug o posterior), JDK 17. La app funciona desde Android 7.0.

### Obtener la APK sin Android Studio

Cada vez que se suben cambios a GitHub, la acción **APK Android** compila la app:

1. En GitHub, pestaña **Actions** → última ejecución de **APK Android**.
2. En **Artifacts**, descarga `marty-y-las-palabras-apk` (es un .zip) y descomprímelo.
3. Pasa `app-debug.apk` al teléfono y ábrelo para instalarlo
   (Android pedirá permitir "instalar apps de origen desconocido").

## Versión web (`index.html`)

La versión original del juego en un solo archivo HTML. Se abre en cualquier navegador
y usa las fuentes de `fonts/`.
