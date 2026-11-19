#!/usr/bin/env bash
# Prueba de humo: abre la app en cada pantalla, guarda una captura y falla si la app se cierra.
set -u
PKG=com.billkevin91.martypalabras
adb install -r apk/app-debug.apk
mkdir -p shots
adb logcat -c
fail=0
read SW SH < <(adb shell wm size | tr -d '\r' | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
echo "Pantalla: ${SW}x${SH}"
for k in home picker album voz intro fin pantano burbuja tela anagrama falta lee attack; do
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/.MainActivity --es demo $k
  sleep 7
  adb exec-out screencap -p > shots/$k.png
  # Toques repartidos por la zona de juego, para detectar cierres al jugar, y segunda captura.
  if [ "$k" != home ] && [ "$k" != picker ] && [ "$k" != voz ] && [ "$k" != album ] && [ "$k" != intro ] && [ "$k" != fin ]; then
    for y in 30 38 46 54 62; do
      for x in 15 32 50 68 85; do
        adb shell input tap $((SW * x / 100)) $((SH * y / 100))
      done
    done
    sleep 2
    adb exec-out screencap -p > shots/${k}_toques.png
  fi
  if ! adb shell pidof $PKG > /dev/null; then
    echo "::error::La app se cerró en la pantalla '$k'"
    fail=1
  fi
done
adb logcat -d > shots/logcat.txt
if grep -n -A40 "FATAL EXCEPTION" shots/logcat.txt; then fail=1; fi
exit $fail
