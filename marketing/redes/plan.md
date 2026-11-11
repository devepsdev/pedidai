# Redes de PedidAI · plan semanal

Un tema por semana, adaptado a cada red (no cinco publicaciones distintas). Claude prepara textos, imagen o vídeo y la
frase para el perfil personal de Enrique; Enrique lo revisa todo y, con su «sí», Claude lo publica desde Chrome de una
en una. Fichas de los perfiles y textos anteriores en `../textos-redes.md`.

## Dónde y cómo

| Red | Cuenta | Semanas con imagen | Semanas con vídeo |
|---|---|---|---|
| LinkedIn | página `linkedin.com/company/pedidai` + compartir en el perfil de Enrique | Imagen + texto | Vídeo nativo + texto |
| Facebook | página «Pedidai · Compras hostelería» (comprobar que Chrome actúa como la página) | Imagen + texto más cercano | Reel |
| Instagram | `@pedidai.es` (colaborador `devepsdev`) | Imagen 1080×1350 + texto corto | Reel |
| YouTube | `@pedidai` | — | Short |
| TikTok | `@pedidai.es` | — | El mismo vídeo, sin trabajo extra |

- Imágenes: `semana-NN.html` → `node redes/imagenes.mjs` → `salida/redes/semana-NN.jpg` (1080×1350).
- Vídeos: los de `../videos.mjs` (producto) o uno corto de 15–20 s hecho a partir de la imagen con la voz de `mi-voz`.
- Facebook es la red que mejor ha funcionado; YouTube conserva los vídeos en búsquedas; Instagram y TikTok tienen poco
  alcance (no se hace contenido especial para ellas).

## Normas

- Una idea por publicación. La primera línea tiene que enganchar. Texto corto.
- Nada inventado: ni clientes, ni ahorros, ni cifras que no tengamos.
- Sin territorio, sin ubicación en las publicaciones y sin destacar el catalán.
- Como mucho un enlace (en LinkedIn, mejor en el primer comentario; en Instagram y TikTok, en la biografía).
- Una publicación por semana como mucho.
- Público de LinkedIn: gerentes de grupos de restauración, jefes de cocina, consultores y proveedores. Facebook e
  Instagram: dueños de bares y restaurantes.

## Rotación (4 semanas)

| Semana | Tipo | Idea |
|---|---|---|
| 1 | Consejo práctico | Antes de firmar el albarán, mira 3 cosas |
| 2 | Sector | Los productos que más cambian de precio (aceite, pescado, fruta) y cómo notarlo a tiempo |
| 3 | Detrás de PedidAI | Por qué hice PedidAI (historia de Enrique, en primera persona) |
| 4 | Producto | Vídeo de una función: comparar un albarán con el pedido (`salida/pedido/`) |
| 5 | Consejo práctico | Tarifas de proveedores: por qué guardarlas y compararlas cada mes |
| 6 | Sector | Qué es el escandallo y por qué depende de los precios de compra |
| 7 | Detrás de PedidAI | Lo que he aprendido leyendo albaranes reales (formatos, unidades, errores típicos) |
| 8 | Producto | Vídeo: pedir por chat al proveedor más barato (`salida/chat/`) |

## Registro

| Fecha | Semana | Tema | LinkedIn | Facebook | Instagram | YouTube | TikTok |
|---|---|---|---|---|---|---|---|
| 2026-11-11 | 1 | Antes de firmar el albarán, mira 3 cosas | Página + perfil («Un consejo sencillo que se olvida con las prisas del reparto. ¿Cómo lo hacéis en vuestro bar o restaurante?») | Publicada | Publicada (colaborador `devepsdev`) | — | — |

## Semana 1 · Antes de firmar el albarán, mira 3 cosas

Imagen: `salida/redes/semana-01.jpg`.

```
Firmar un albarán sin mirarlo es firmar que todo está bien.

Y casi nunca hay tiempo: el repartidor tiene prisa, la cocina está en marcha y el papel acaba en un cajón.

Basta con un minuto. Antes de firmar, mira 3 cosas:

1️⃣ Cantidades y unidades. ¿Pediste 10 kg y pone 10 cajas? ¿El pescado pesa lo que dice?
2️⃣ Precios. ¿Es el de la última vez? Las subidas pequeñas son las que menos se ven.
3️⃣ Lo que falta o cambia. Productos que no llegan, otra marca u otro calibre.

Si algo no cuadra, anótalo en el albarán antes de firmar. Después es tu palabra contra la suya.

¿Tú revisas los albaranes al recibirlos o a final de mes?

#hostelería #restauración #bares #restaurantes #proveedores
```

Facebook (página):

```
¿Firmas el albarán sin mirarlo? 🙋

Con el repartidor esperando y la cocina en marcha, es lo normal. Pero basta con un minuto para mirar 3 cosas:

1️⃣ Cantidades y unidades: ¿pediste kilos y pone cajas?
2️⃣ Precios: ¿es el de la última vez?
3️⃣ Lo que falta o cambia: otra marca, otro calibre o algo que no ha llegado.

Si algo no cuadra, anótalo en el albarán antes de firmar.

¿Cómo lo hacéis vosotros? 👇
```

Instagram (`@pedidai.es`, colaborador `devepsdev`, sin ubicación):

```
Antes de firmar el albarán, mira 3 cosas 👀

1️⃣ Cantidades y unidades
2️⃣ Precios
3️⃣ Lo que falta o cambia

Si algo no cuadra, anótalo antes de firmar. Guárdalo para el próximo reparto 📌

#hostelería #bares #restaurantes #proveedores #albaranes
```
