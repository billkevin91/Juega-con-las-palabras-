# Marty y las Palabras

Juego educativo de ortografía en español. Marty, un presentador alienígena,
guía a los niños por planetas donde practican tildes, ñ, diéresis y dictado.

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
| `Simple.kt` | Letra perdida y Lectura |
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
