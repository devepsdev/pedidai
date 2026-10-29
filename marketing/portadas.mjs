// Portadas de los Reels/TikToks con el mismo diseño (fotograma de la app oscurecido y una frase grande).
//   node portadas.mjs            (todas)   ·   node portadas.mjs chat tarifa
// El fondo sale de salida/<vídeo>/secuencia (fotogramas sin subtítulos) en el instante indicado.
// Los horizontales (tutorial de YouTube) usan miniatura.html a 1280×720.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const RAIZ = path.dirname(fileURLToPath(import.meta.url));
const PLANTILLA = pathToFileURL(path.join(RAIZ, 'portada.html')).href;
const MINIATURA = pathToFileURL(path.join(RAIZ, 'miniatura.html')).href;

// titulo: *resaltado* y | salto de línea; escena + segundos dentro de ella para el fondo
const PORTADAS = {
  'foto-albaran': { titulo: '¿Cuánto te|cobran *de más*?', escena: 'lectura', seg: 3 },
  chat: { titulo: 'Haz tus pedidos|*por chat*', escena: 'respuesta', seg: 3.5 },
  tarifa: { titulo: '¿Te sale *más|barata* otra tarifa?', escena: 'comparar', seg: 3.8 },
  pedido: { titulo: '¿Te han traído|*todo* lo que pediste?', escena: 'diferencias', seg: 1.5 },
  home: { titulo: 'Controla lo que|*pagas* a tus|proveedores', escena: 'precios', seg: 2.5 },
  tutorial: { titulo: '¿Pagas *de más*|a tus|proveedores?', linea: 'Tutorial en 2 minutos', escena: 'precios', seg: 3.5, horizontal: true },
};

async function main() {
  const nombres = process.argv.slice(2).length ? process.argv.slice(2) : Object.keys(PORTADAS);
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    for (const nombre of nombres) {
      const p = PORTADAS[nombre];
      const page = await browser.newPage({ viewport: p.horizontal ? { width: 1280, height: 720 } : { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
      const carpeta = path.join(RAIZ, 'salida', nombre);
      const linea = JSON.parse(fs.readFileSync(path.join(carpeta, 'linea-de-tiempo.json'), 'utf8'));
      const escena = linea.escenas.find(e => e.id === p.escena);
      const n = Math.round((escena.inicio + p.seg) * 30);
      const fondo = path.join(carpeta, 'secuencia', `s${String(n).padStart(6, '0')}.jpg`);
      const url = `${p.horizontal ? MINIATURA : PLANTILLA}?titulo=${encodeURIComponent(p.titulo)}&linea=${encodeURIComponent(p.linea || '')}`
        + `&fondo=${encodeURIComponent(pathToFileURL(fondo).href)}`;
      await page.goto(url, { waitUntil: 'networkidle' });
      const salida = path.join(carpeta, `${nombre}-portada.jpg`);
      await page.screenshot({ path: salida, type: 'jpeg', quality: 92 });
      console.log('Portada:', salida);
      await page.close();
    }
  } finally {
    await browser.close();
  }
}

main().catch(e => { console.error(e); process.exit(1); });
