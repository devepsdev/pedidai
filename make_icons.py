"""
PedidAI -- Genera iconos PWA con fondo solido de marca.
Fuente: icon-512.png existente (logo carrito con fondo transparente).
Produce:
  - icon-512.png          fondo #0F172A, para 'any maskable'
  - icon-192.png          fondo #0F172A, para 'any maskable'
  - apple-touch-icon.png  fondo #0F172A (iOS)
  - favicon.png           fondo transparente 32x32 (browser tab)
  - icons/favicon-16.png  fondo transparente 16x16 (browser tab)
"""
import sys
sys.stdout.reconfigure(encoding='utf-8')
from PIL import Image
import os

PUB = r"C:\Users\devep\Desktop\Proyectos\pedidai\pedidai-app\public"

# Color de fondo de marca (dark navy del gradiente)
BG = (15, 23, 42, 255)  # #0F172A

# Logo fuente: logo-hero.png tiene la mejor resolucion (500x390)
LOGO_SRC = os.path.join(PUB, "logo-hero.png")

logo = Image.open(LOGO_SRC).convert("RGBA")
print(f"Logo fuente: {logo.size}")


def make_icon_solid(logo, size, logo_ratio=0.65):
    """
    Icono cuadrado con fondo solido.
    logo_ratio: cuanto del lado ocupa el logo (0.65 = 65%)
    Para maskable: el safe zone es el 80% central, con 0.65 queda holgado.
    """
    canvas = Image.new("RGBA", (size, size), BG)
    max_dim = int(size * logo_ratio)
    img = logo.copy()
    img.thumbnail((max_dim, max_dim), Image.LANCZOS)
    x = (size - img.width) // 2
    y = (size - img.height) // 2
    canvas.paste(img, (x, y), img)
    return canvas


def make_icon_transparent(logo, size):
    """Icono cuadrado con fondo transparente (favicon browser)."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    img = logo.copy()
    img.thumbnail((size, size), Image.LANCZOS)
    x = (size - img.width) // 2
    y = (size - img.height) // 2
    canvas.paste(img, (x, y), img)
    return canvas


# --- PWA icons (fondo solido) ---
for size, fname in [(512, "icon-512.png"), (192, "icon-192.png"), (180, "apple-touch-icon.png")]:
    out = make_icon_solid(logo, size, logo_ratio=0.65)
    path = os.path.join(PUB, fname)
    out.save(path, "PNG")
    print(f"  Guardado {fname}  ({size}x{size})  fondo solido")

# --- Favicons (transparente, se ven sobre fondo blanco del browser) ---
os.makedirs(os.path.join(PUB, "icons"), exist_ok=True)
for size, fname in [(32, "favicon.png"), (16, os.path.join("icons", "favicon-16.png"))]:
    out = make_icon_transparent(logo, size)
    path = os.path.join(PUB, fname)
    out.save(path, "PNG")
    print(f"  Guardado {fname}  ({size}x{size})  transparente")

print("\nListo. Sube los archivos al servidor y recarga la PWA.")
