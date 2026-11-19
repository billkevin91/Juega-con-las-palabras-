#!/usr/bin/env python3
"""
Genera la voz grabada de Marty y las palabras del juego con Piper (voz neural libre, sin internet una
vez descargada).

Entrada: android/app/build/voz/frases.tsv y palabras.tsv, que escribe la prueba FrasesTest
         (./gradlew testDebugUnitTest).
Salida:  <salida>/voz/<palabra>.m4a y <salida>/voz/f/<crc32>.m4a, listos para copiar a
         android/app/src/main/assets/.

Uso: python3 tools/voz/generar.py <salida>
Necesita: pip install piper-tts imageio-ffmpeg (o ffmpeg instalado).
"""
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import wave

from piper import PiperVoice, SynthesisConfig

# Voz masculina latinoamericana (México) para Marty, Borrón y el dictado.
VOICE = "es_MX-ald-medium"
URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_MX/ald/medium/"

def ffmpeg_exe():
    """ffmpeg del sistema, o el que trae el paquete imageio-ffmpeg (pip install imageio-ffmpeg)."""
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    import imageio_ffmpeg
    return imageio_ffmpeg.get_ffmpeg_exe()


ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LISTAS = os.path.join(ROOT, "android", "app", "build", "voz")


def descargar(dest):
    os.makedirs(dest, exist_ok=True)
    for ext in (".onnx", ".onnx.json"):
        path = os.path.join(dest, VOICE + ext)
        if not os.path.exists(path):
            print("Descargando", VOICE + ext)
            urllib.request.urlretrieve(URL + VOICE + ext, path)
    return os.path.join(dest, VOICE + ".onnx")


# Efectos de sonido con ffmpeg: Marty un poco más agudo y alegre; Borrón grave y con eco travieso.
def filtro(quien, sr):
    # Silencio corto al inicio y al final: algunos teléfonos tardan en arrancar el audio y
    # se comerían la primera sílaba.
    bordes = "adelay=120:all=1,apad=pad_dur=0.15"
    if quien == "marty":
        return f"asetrate={sr}*1.07,aresample={sr},atempo=0.97,{bordes}"
    if quien == "borron":
        return f"asetrate={sr}*0.80,aresample={sr},atempo=1.18,aecho=0.8:0.6:45:0.22,{bordes}"
    return f"aresample={sr},{bordes}"  # palabras: voz natural, sin cambios


def main():
    salida = sys.argv[1] if len(sys.argv) > 1 else "voz-generada"
    global FFMPEG
    FFMPEG = ffmpeg_exe()
    voice = PiperVoice.load(descargar(os.path.join(tempfile.gettempdir(), "piper-voces")))
    sr = voice.config.sample_rate
    trabajos = []
    with open(os.path.join(LISTAS, "frases.tsv"), encoding="utf-8") as f:
        for linea in f:
            if linea.strip():
                archivo, quien, texto = linea.rstrip("\n").split("\t", 2)
                trabajos.append((os.path.join(salida, "voz", "f", archivo), quien, texto, 1.0))
    with open(os.path.join(LISTAS, "palabras.tsv"), encoding="utf-8") as f:
        for linea in f:
            if linea.strip():
                archivo, palabra = linea.rstrip("\n").split("\t", 1)
                # El punto da entonación de frase; un poco más lento para dictar con claridad.
                trabajos.append((os.path.join(salida, "voz", archivo), "palabra", palabra[:1].upper() + palabra[1:] + ".", 1.12))

    with tempfile.TemporaryDirectory() as tmp:
        for n, (destino, quien, texto, lento) in enumerate(trabajos, 1):
            os.makedirs(os.path.dirname(destino), exist_ok=True)
            wav = os.path.join(tmp, "x.wav")
            with wave.open(wav, "wb") as wf:
                voice.synthesize_wav(texto, wf, syn_config=SynthesisConfig(length_scale=lento, noise_scale=0.6, noise_w_scale=0.7))
            subprocess.run(
                [FFMPEG, "-y", "-loglevel", "error", "-i", wav, "-af", filtro(quien, sr),
                 "-ac", "1", "-c:a", "aac", "-b:a", "40k", destino],
                check=True,
            )
            if n % 25 == 0:
                print(f"{n}/{len(trabajos)}")
    print(f"Listo: {len(trabajos)} grabaciones en {salida}/voz")


if __name__ == "__main__":
    main()
