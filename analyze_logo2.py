"""
Analiza la imagen fila a fila y columna a columna para encontrar
las franjas negras que separan los logos, y guarda slices de diagnostico.
"""
import sys
sys.stdout.reconfigure(encoding='utf-8')
from PIL import Image
import os

SRC = r"C:\Users\devep\Downloads\ChatGPT Image 24 abr 2026, 10_57_26.png"
OUT = r"C:\Users\devep\Desktop\Proyectos\pedidai\debug_crops"
os.makedirs(OUT, exist_ok=True)

img = Image.open(SRC).convert("RGB")
w, h = img.size
pixels = img.load()
THR = 25  # umbral luminosidad para "negro"

def row_has_content(y):
    """True si la fila y tiene algun pixel no-negro."""
    for x in range(0, w, 4):
        r, g, b = pixels[x, y]
        if 0.299*r + 0.587*g + 0.114*b > THR:
            return True
    return False

def col_has_content(x):
    """True si la columna x tiene algun pixel no-negro."""
    for y in range(0, h, 4):
        r, g, b = pixels[x, y]
        if 0.299*r + 0.587*g + 0.114*b > THR:
            return True
    return False

# -- Perfil horizontal (contenido por fila) --
print("Escaneando filas...")
row_content = [row_has_content(y) for y in range(h)]

# Encontrar rangos de filas con contenido
row_ranges = []
in_range = False
start = 0
for y, has in enumerate(row_content):
    if has and not in_range:
        start = y; in_range = True
    elif not has and in_range:
        row_ranges.append((start, y-1)); in_range = False
if in_range:
    row_ranges.append((start, h-1))

print(f"Franjas con contenido (Y): {row_ranges}")

# -- Para cada franja de filas, buscar columnas --
print("\nEscaneando columnas por franja...")
for ry1, ry2 in row_ranges:
    def col_has_content_in_row_range(x):
        for y in range(ry1, ry2+1, 4):
            r, g, b = pixels[x, y]
            if 0.299*r + 0.587*g + 0.114*b > THR:
                return True
        return False

    col_content = [col_has_content_in_row_range(x) for x in range(w)]
    col_ranges = []
    in_range = False
    start = 0
    for x, has in enumerate(col_content):
        if has and not in_range:
            start = x; in_range = True
        elif not has and in_range:
            col_ranges.append((start, x-1)); in_range = False
    if in_range:
        col_ranges.append((start, w-1))

    print(f"  Fila Y={ry1}-{ry2}:")
    for cx1, cx2 in col_ranges:
        print(f"    Logo candidato: x={cx1}-{cx2}, y={ry1}-{ry2}  ({cx2-cx1}x{ry2-ry1}px)")

# -- Guardar cuadrantes fijos para inspeccion visual --
print("\nGuardando cuadrantes de la imagen para inspeccion...")
cuadrantes = {
    "top_left":    (0,    0,    768,  512),
    "top_right":   (768,  0,    1536, 512),
    "bottom_left": (0,    512,  768,  1024),
    "bottom_right":(768,  512,  1536, 1024),
}
for name, box in cuadrantes.items():
    crop = img.crop(box)
    path = os.path.join(OUT, f"Q_{name}.png")
    crop.save(path)
    print(f"  Guardado: Q_{name}.png")

print("\nRevisa los archivos Q_*.png en debug_crops para ver que hay en cada cuadrante.")
