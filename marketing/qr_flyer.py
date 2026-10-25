"""QR del flyer: salida/flyer/qr.svg (vectorial, para imprenta). Después, node flyer.mjs.

El enlace lleva UTM para ver en Google Analytics cuánta gente llega escaneando el flyer.
Requiere segno (pip install segno).
"""
from pathlib import Path

import segno

URL = "https://pedidai.es/?utm_source=flyer&utm_medium=papel&utm_campaign=bares-2026"

salida = Path(__file__).parent / "salida" / "flyer"
salida.mkdir(parents=True, exist_ok=True)

# Corrección de errores M (15 %): aguanta pequeñas manchas o arrugas del papel.
# Margen de 2 módulos: el recuadro blanco del flyer añade el resto de la zona de silencio.
qr = segno.make(URL, error="m")
qr.save(salida / "qr.svg", scale=10, border=2, dark="#0f172a", light="#ffffff")
print(f"QR versión {qr.version} ({qr.symbol_size(border=0)[0]} módulos) -> {salida / 'qr.svg'}")
