"""
PedidAI -- Extrae logos del PNG con fondo gris (no negro puro).
Usa eliminacion por canal de saturacion HSV en lugar de luminosidad.
Auto-recorta el resultado con getbbox() para eliminar bordes transparentes.
"""
import sys
sys.stdout.reconfigure(encoding='utf-8')
from PIL import Image
import os, colorsys

SRC = r"C:\Users\devep\Downloads\ChatGPT Image 24 abr 2026, 10_57_26.png"
PUB = r"C:\Users\devep\Desktop\Proyectos\pedidai\pedidai-app\public"
DBG = r"C:\Users\devep\Desktop\Proyectos\pedidai\debug_crops"
os.makedirs(DBG, exist_ok=True)
os.makedirs(os.path.join(PUB, "icons"), exist_ok=True)

# -----------------------------------------------------------------------
# Bounding boxes (x1, y1, x2, y2) -- generosos, se auto-recortaran
# Determinados inspeccionando los cuadrantes Q_*.png
# -----------------------------------------------------------------------
BOXES = {
    "hero":    (155, 210, 800, 660),   # logo grande, cruza la mitad vertical
    "navbar":  (755, 270, 1110, 560),  # logo mediano top-right
    "small":   (768, 580, 1018, 865),  # empieza tras las ruedas del navbar (~y575)
    "appicon": (1038, 518, 1258, 800), # separado del small
}


def remove_gray_background(img: Image.Image) -> Image.Image:
    """
    Elimina el fondo gris:
    - Un pixel es 'fondo' si su saturation HSV < 0.25 Y luminosidad < 190
    - Un pixel es 'logo' si es brillante (L>190) O tiene color (sat>0.25)
    La franja de glow azul tenue alrededor del logo se elimina si sat < 0.10
    """
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            # Normalizar a 0-1
            rf, gf, bf = r/255, g/255, b/255
            h, s, v = colorsys.rgb_to_hsv(rf, gf, bf)
            lum = 0.299*r + 0.587*g + 0.114*b

            # Eliminar si es gris oscuro (baja saturacion, oscuro)
            if s < 0.18 and lum < 185:
                px[x, y] = (0, 0, 0, 0)
            # Eliminar el glow muy tenue en los bordes
            elif s < 0.08 and lum < 210:
                px[x, y] = (0, 0, 0, 0)
    return img


def extract_and_trim(src_img, box, name):
    """Recorta, elimina fondo, auto-trim, devuelve imagen limpia."""
    crop = src_img.crop(box)
    clean = remove_gray_background(crop)
    bbox = clean.getbbox()  # bounding box del contenido no-transparente
    if bbox is None:
        print(f"  WARN [{name}]: todo transparente tras eliminacion de fondo")
        return clean
    trimmed = clean.crop(bbox)
    print(f"  [{name}] box={box} -> recorte={crop.size} -> trim={trimmed.size}")
    # Guardar debug
    trimmed.save(os.path.join(DBG, f"clean_{name}.png"))
    return trimmed


def make_square_icon(img, size):
    """Pone la imagen centrada en un canvas cuadrado transparente de 'size'."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    img_copy = img.copy()
    img_copy.thumbnail((size, size), Image.LANCZOS)
    x = (size - img_copy.width) // 2
    y = (size - img_copy.height) // 2
    canvas.paste(img_copy, (x, y), img_copy)
    return canvas


def resize_fit_height(img, target_h):
    w, h = img.size
    scale = target_h / h
    return img.resize((int(w*scale), target_h), Image.LANCZOS)


def resize_fit_width(img, target_w):
    w, h = img.size
    scale = target_w / w
    return img.resize((target_w, int(h*scale)), Image.LANCZOS)


def make_canvas(img, cw, ch):
    """Logo centrado en canvas de cw x ch."""
    canvas = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    img_copy = img.copy()
    img_copy.thumbnail((cw, ch), Image.LANCZOS)
    x = (cw - img_copy.width) // 2
    y = (ch - img_copy.height) // 2
    canvas.paste(img_copy, (x, y), img_copy)
    return canvas


# -----------------------------------------------------------------------
print(f"Abriendo: {SRC}")
raw = Image.open(SRC)
print(f"Tamano: {raw.size}")

# Extraer y limpiar variantes
variants = {name: extract_and_trim(raw, box, name) for name, box in BOXES.items()}

# -----------------------------------------------------------------------
# Exportar
# -----------------------------------------------------------------------
SAVES = []

# Icons (desde appicon)
icon_src = variants["appicon"]
for size, fname in [(512, "icon-512.png"), (192, "icon-192.png"),
                    (180, "apple-touch-icon.png"), (32, "favicon.png"),
                    (16, os.path.join("icons","favicon-16.png"))]:
    out = make_square_icon(icon_src, size)
    path = os.path.join(PUB, fname)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path, "PNG")
    SAVES.append((fname, f"{size}x{size}"))

# Navbar logo (alto 60px)
nav = resize_fit_height(variants["navbar"], 60)
nav.save(os.path.join(PUB, "logo-navbar.png"), "PNG")
SAVES.append(("logo-navbar.png", f"{nav.size}"))

# Hero logo (ancho 500px)
hero = resize_fit_width(variants["hero"], 500)
hero.save(os.path.join(PUB, "logo-hero.png"), "PNG")
SAVES.append(("logo-hero.png", f"{hero.size}"))

# Email logo (canvas 200x80)
email = make_canvas(variants["navbar"], 200, 80)
email.save(os.path.join(PUB, "logo-email.png"), "PNG")
SAVES.append(("logo-email.png", "200x80"))

print("\nAssets exportados:")
for fname, sz in SAVES:
    print(f"  public/{fname}  ({sz})")

print(f"\nRecortes de debug en: {DBG}")
print("Abre los archivos clean_*.png para verificar que cada logo es correcto.")
