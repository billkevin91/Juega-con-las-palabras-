# Rodada Brutal

Juego 2D de física de moto para Android. Moto de dos ruedas con suspensión y un
piloto articulado, sobre terreno procedural con colinas y rampas.

Es un proyecto nuevo e **independiente**: código, arte y nombres son propios;
no reutiliza contenido de ningún juego existente.

## Estado

Esqueleto jugable mínimo:

- Moto (chasis + 2 ruedas con `WheelJoint` para suspensión + piloto con
  `RevoluteJoint` limitado).
- Terreno procedural con `ChainShape`.
- Controles multitáctiles divididos en 4 cuadrantes + teclado para pruebas
  en escritorio.
- Cámara que sigue a la moto.
- Respawn automático si se queda tumbada boca abajo.

Lo que falta: menú, niveles diseñados a mano, meta, cronómetro, estrellas,
arte real (sprites), sonido, editor.

## Stack

- Kotlin 2.0, JDK 17
- libGDX 1.12.1 + extensión Box2D
- Android Gradle Plugin 8.7, `minSdk 24`, `targetSdk 34`

## Estructura

```
rodada-brutal/
├── core/                       Lógica común (ApplicationAdapter libGDX)
│   └── src/main/kotlin/com/rodadabrutal/
│       ├── Rodada.kt           Game principal
│       ├── GameScreen.kt       Loop, cámara, step de física
│       ├── Bike.kt             Chasis, ruedas, piloto, joints
│       ├── Terrain.kt          Generación procedural
│       ├── Controls.kt         Teclado + touch
│       └── Hud.kt              Overlay
└── android/                    Launcher específico Android
    └── src/main/kotlin/com/rodadabrutal/android/AndroidLauncher.kt
```

## Compilar

```
cd rodada-brutal
./gradlew :android:assembleDebug
```

El APK sale en `android/build/outputs/apk/debug/android-debug.apk`.

## Controles

Pantalla dividida en cuatro cuadrantes:

```
┌──────────────┬──────────────┐
│ inclinar     │ inclinar     │
│ atrás        │ adelante     │
├──────────────┼──────────────┤
│ freno        │ acelerar     │
└──────────────┴──────────────┘
```

En escritorio (cuando se agregue el launcher): flechas / WASD.

## Cómo pasarlo a su propio repo

Esta carpeta está adentro del repo `Juega-con-las-palabras-` sólo porque la
integración no tenía permiso para crear uno nuevo. Para separarla:

```
# desde fuera del repo original
git clone <este-repo> tmp
cd tmp
git subtree split --prefix=rodada-brutal -b rodada-brutal-only

# crear un repo vacío en https://github.com/new llamado rodada-brutal
# luego:
git remote add nuevo git@github.com:<tu-usuario>/rodada-brutal.git
git push nuevo rodada-brutal-only:main
```

O más simple:

```
cp -r rodada-brutal ~/rodada-brutal
cd ~/rodada-brutal
git init && git add . && git commit -m "Primera versión"
git remote add origin git@github.com:<tu-usuario>/rodada-brutal.git
git push -u origin main
```
