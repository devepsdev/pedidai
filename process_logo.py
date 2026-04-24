"""
PedidAI -- Procesa variantes de logo desde ChatGPT_Image_24_abr_2026__10_57_26.png
"""
import sys
sys.stdout.reconfigure(encoding='utf-8')

from PIL import Image
import os

SRC = r"C:\Users\devep\Downloads\ChatGPT Image 24 abr 2026, 10_57_26.png"
BASE = r"C:\Users\devep\Desktop\Proyectos\pedidai\pedidai-app\src"

# ── Destinos ──────────────────────────────────────────────────────────────────
OUTPUTS = {
    "favicon":        os.path.join(BASE, "favicon.png"),
    "fav16":          os.path.join(BASE, "assets", "icons", "favicon-16.png"),
    "apple":          os.path.join(BASE, "assets", "icons", "apple-touch-icon.png"),
    "icon192":        os.path.join(BASE, "assets", "icons", "icon-192.png"),
    "icon512":        os.path.join(BASE, "assets", "icons", "icon-512.png"),
    "logo_navbar":    os.path.join(BASE, "assets", "images", "logo-navbar.png"),
    "logo_hero":      os.path.join(BASE, "assets", "images", "logo-hero.png"),
    "logo_email":     os.path.join(BASE, "assets", "images", "logo-email.png"),
}

# ── Bounding boxes (x1, y1, x2, y2) ─────────────────────────────────────────
BOXES = {
    "hero":    (208, 262, 681, 521),
    "navbar":  (806, 312, 1093, 521),
    "small":   (246, 502, 638, 642),
    "appicon": (758, 502, 1166, 807),
}


def remove_black(img: Image.Image, threshold: int = 35) -> Image.Image:
    """Elimina el fondo negro convirtiendo a RGBA con canal alpha."""
    img = img.convert("RGBA")
    pixels = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum < threshold:
                pixels[x, y] = (0, 0, 0, 0)
    return img


def resize_fit(img: Image.Image, width: int | None = None,
               height: int | None = None) -> Image.Image:
    """Redimensiona manteniendo ratio, ajustando al mayor lado."""
    w, h = img.size
    if width and not height:
        scale = width / w
        return img.resize((int(w * scale), int(h * scale)), Image.LANCZOS)
    elif height and not width:
        scale = height / h
        return img.resize((int(w * scale), int(h * scale)), Image.LANCZOS)
    elif width and height:
        img.thumbnail((width, height), Image.LANCZOS)
        return img
    return img


def resize_canvas(img: Image.Image, w: int, h: int) -> Image.Image:
    """Pega la imagen centrada sobre un canvas transparente w×h."""
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    img.thumbnail((w, h), Image.LANCZOS)
    x = (w - img.width) // 2
    y = (h - img.height) // 2
    canvas.paste(img, (x, y), img)
    return canvas


# ── Main ─────────────────────────────────────────────────────────────────────
print(f"Abriendo {SRC}")
raw = Image.open(SRC)
print(f"Tamaño original: {raw.size}")

# Extraer y limpiar cada variante
variants = {}
for name, box in BOXES.items():
    crop = raw.crop(box)
    clean = remove_black(crop)
    variants[name] = clean
    print(f"  [{name}]  {crop.size} -> RGBA fondo eliminado")

# ── App Icon → square canvas ─────────────────────────────────────────────────
icon_src = variants["appicon"]

raw_icon = icon_src.copy()

for key, size in [("favicon", 32), ("fav16", 16), ("apple", 180),
                  ("icon192", 192), ("icon512", 512)]:
    out = resize_canvas(raw_icon.copy(), size, size)
    out.save(OUTPUTS[key], "PNG")
    print(f"  Saved  {OUTPUTS[key]}  ({size}×{size})")

# ── Navbar logo → height 60 ──────────────────────────────────────────────────
logo_nav = resize_fit(variants["navbar"].copy(), height=60)
logo_nav.save(OUTPUTS["logo_navbar"], "PNG")
print(f"  Saved  {OUTPUTS['logo_navbar']}  ({logo_nav.size})")

# ── Hero logo → width 500 ────────────────────────────────────────────────────
logo_hero = resize_fit(variants["hero"].copy(), width=500)
logo_hero.save(OUTPUTS["logo_hero"], "PNG")
print(f"  Saved  {OUTPUTS['logo_hero']}  ({logo_hero.size})")

# ── Email logo → canvas 200×80 ───────────────────────────────────────────────
logo_email = resize_canvas(variants["navbar"].copy(), 200, 80)
logo_email.save(OUTPUTS["logo_email"], "PNG")
print(f"  Saved  {OUTPUTS['logo_email']}  (200×80)")

print("\nOK - Todos los assets generados correctamente.")
