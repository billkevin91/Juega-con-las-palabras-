#!/usr/bin/env bash
# Prueba de humo: abre la app en cada pantalla, guarda una captura y falla si la app se cierra.
set -u
PKG=com.billkevin91.martypalabras
adb install -r apk/app-debug.apk
mkdir -p shots
adb logcat -c
fail=0
for k in home pantano burbuja tela anagrama falta lee attack; do
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/.MainActivity --es demo $k
  sleep 7
  adb exec-out screencap -p > shots/$k.png
  if ! adb shell pidof $PKG > /dev/null; then
    echo "::error::La app se cerró en la pantalla '$k'"
    fail=1
  fi
done
adb logcat -d > shots/logcat.txt
if grep -n -A40 "FATAL EXCEPTION" shots/logcat.txt; then fail=1; fi
exit $fail
