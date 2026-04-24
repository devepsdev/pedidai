"""
Analiza la imagen para detectar automaticamente las regiones no-negras
y guardar cada variante del logo como recorte de diagnostico.
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
print(f"Imagen: {w} x {h}")

# --- Crear mapa de luminosidad (pixel por pixel, downsampled para velocidad) ---
SCALE = 4   # analizar 1 de cada 4 pixeles
lum_map = {}

pixels = img.load()
for y in range(0, h, SCALE):
    for x in range(0, w, SCALE):
        r, g, b = pixels[x, y]
        lum = 0.299*r + 0.587*g + 0.114*b
        if lum > 30:   # pixel NO negro
            lum_map[(x, y)] = lum

# --- Agrupar en regiones usando grid de celdas de 40px ---
CELL = 40
occupied = set()
for (x, y) in lum_map:
    occupied.add((x // CELL, y // CELL))

# --- BFS para conectar celdas adyacentes en regiones ---
def neighbors(cx, cy):
    for dx in [-1, 0, 1]:
        for dy in [-1, 0, 1]:
            yield (cx+dx, cy+dy)

remaining = set(occupied)
regions = []
while remaining:
    seed = next(iter(remaining))
    region = set()
    queue = [seed]
    while queue:
        cell = queue.pop()
        if cell in remaining:
            remaining.remove(cell)
            region.add(cell)
            for nb in neighbors(*cell):
                if nb in remaining:
                    queue.append(nb)
    regions.append(region)

# --- Convertir celdas a bounding boxes en pixeles ---
print(f"\nRegiones detectadas: {len(regions)}")
boxes = []
for i, region in enumerate(sorted(regions, key=lambda r: min(c[1] for c in r)*10000 + min(c[0] for c in r))):
    min_cx = min(c[0] for c in region)
    max_cx = max(c[0] for c in region)
    min_cy = min(c[1] for c in region)
    max_cy = max(c[1] for c in region)
    # Convertir a pixels con margen
    margin = CELL
    x1 = max(0, min_cx * CELL - margin)
    y1 = max(0, min_cy * CELL - margin)
    x2 = min(w, (max_cx + 1) * CELL + margin)
    y2 = min(h, (max_cy + 1) * CELL + margin)
    area = (x2-x1) * (y2-y1)
    boxes.append((x1, y1, x2, y2, area))
    print(f"  Region {i+1}: x={x1}-{x2}, y={y1}-{y2}  size={x2-x1}x{y2-y1}  area={area}")

# --- Guardar recortes de diagnostico ---
print(f"\nGuardando recortes en: {OUT}")
for i, (x1, y1, x2, y2, area) in enumerate(boxes):
    crop = img.crop((x1, y1, x2, y2))
    path = os.path.join(OUT, f"region_{i+1}_x{x1}-{x2}_y{y1}-{y2}.png")
    crop.save(path)
    print(f"  Guardado: region_{i+1}")

print("\nAbre la carpeta debug_crops para ver los recortes y verificar cual es cada logo.")
