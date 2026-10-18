"""Locuta cada escena de los guiones con tu voz (programa mi-voz) y guarda su duración.

Uso, con el Python de «Mi Voz»:
    "%LOCALAPPDATA%\\Programs\\Mi Voz\\python\\Scripts\\python.exe" locutar.py foto-albaran [tarifa ...]
Crea salida/<video>/voz/<escena>.wav y salida/<video>/voz/duraciones.json. Una escena ya locutada no se repite
(bórrala para rehacerla).
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).parent
MI_VOZ = RAIZ.parent.parent / 'mi-voz'
sys.path.insert(0, str(MI_VOZ))
import voz  # noqa: E402

# Estilo «Rápido TikTok» de la app: expresividad, ritmo, pausa entre frases y entre párrafos
EXAG, CFG, PAUSA, PAUSA_PARRAFO = 0.60, 0.50, 0.10, 0.30


def main():
    guiones = json.loads((RAIZ / 'guiones.json').read_text(encoding='utf-8'))
    for video in sys.argv[1:]:
        carpeta = RAIZ / 'salida' / video / 'voz'
        carpeta.mkdir(parents=True, exist_ok=True)
        duraciones = {}
        for escena in guiones[video]:
            wav = carpeta / f"{escena['id']}.wav"
            if not wav.exists():
                audio, sr, avisos = voz.sintetizar(escena['voz'], exag=EXAG, cfg=CFG, pausa=PAUSA,
                                                   pausa_parrafo=PAUSA_PARRAFO)
                voz.guardar_wav(audio, sr, wav)
                for a in avisos:
                    print('AVISO', video, escena['id'], a)
            duraciones[escena['id']] = voz.duracion(wav)
            print(video, escena['id'], f"{duraciones[escena['id']]:.2f} s")
        (carpeta / 'duraciones.json').write_text(json.dumps(duraciones, indent=2), encoding='utf-8')


if __name__ == '__main__':
    main()
