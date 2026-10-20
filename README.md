# Marty y las Palabras

Juego educativo de ortografía en español. Marty, un presentador alienígena,
guía a los niños por planetas donde practican tildes, ñ, diéresis y dictado.

## Estructura

- `index.html`: todo el juego (HTML, CSS y JavaScript en un solo archivo).
- `fonts/`: fuentes Baloo 2 y Nunito (licencia SIL OFL), incluidas para jugar sin internet.
- `android/`: proyecto Android nativo generado con [Capacitor](https://capacitorjs.com).
- `capacitor.config.json`: nombre (`Marty y las Palabras`) e id de la app (`com.billkevin91.martypalabras`).

## Probar en el navegador

Abre `index.html`.

## Obtener la APK

Cada vez que se suben cambios a GitHub, la acción **APK Android** compila la app:

1. En GitHub, entra en la pestaña **Actions** y abre la última ejecución de **APK Android**.
2. Al final de la página, en **Artifacts**, descarga `marty-y-las-palabras-apk` (es un .zip).
3. Descomprímelo, pasa `app-debug.apk` al teléfono y ábrelo para instalarlo
   (Android pedirá permitir "instalar apps de origen desconocido").

### Compilar en tu computadora (opcional)

Necesitas Node 22, JDK 21 y Android Studio (o el Android SDK).

```
npm install
npm run apk
```

La APK queda en `android/app/build/outputs/apk/debug/app-debug.apk`.
Para abrir el proyecto en Android Studio: `npm run sync` y luego `npx cap open android`.

## Funciones nativas

- La voz de "Escuchar" usa el motor de voz del teléfono (complemento
  `@capacitor-community/text-to-speech`), porque dentro de una app Android no
  existe la voz del navegador.
- El botón "atrás" de Android vuelve al menú; en el menú, cierra la app.
