// Portadas de los Reels/TikToks con el mismo diseño (fotograma de la app oscurecido y una frase grande).
//   node portadas.mjs            (todas)   ·   node portadas.mjs chat tarifa
// El fondo sale de salida/<vídeo>/secuencia (fotogramas sin subtítulos) en el instante indicado.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const RAIZ = path.dirname(fileURLToPath(import.meta.url));
const PLANTILLA = pathToFileURL(path.join(RAIZ, 'portada.html')).href;

// titulo: *resaltado* y | salto de línea; escena + segundos dentro de ella para el fondo
const PORTADAS = {
  'foto-albaran': { titulo: '¿Cuánto te|cobran *de más*?', escena: 'lectura', seg: 3 },
  chat: { titulo: 'Haz tus pedidos|*por chat*', escena: 'respuesta', seg: 3.5 },
  tarifa: { titulo: '¿Te sale *más|barata* otra tarifa?', escena: 'comparar', seg: 3.8 },
  pedido: { titulo: '¿Te han traído|*todo* lo que pediste?', escena: 'diferencias', seg: 1.5 },
};

async function main() {
  const nombres = process.argv.slice(2).length ? process.argv.slice(2) : Object.keys(PORTADAS);
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  try {
    for (const nombre of nombres) {
      const p = PORTADAS[nombre];
      const carpeta = path.join(RAIZ, 'salida', nombre);
      const linea = JSON.parse(fs.readFileSync(path.join(carpeta, 'linea-de-tiempo.json'), 'utf8'));
      const escena = linea.escenas.find(e => e.id === p.escena);
      const n = Math.round((escena.inicio + p.seg) * 30);
      const fondo = path.join(carpeta, 'secuencia', `s${String(n).padStart(6, '0')}.jpg`);
      const url = `${PLANTILLA}?titulo=${encodeURIComponent(p.titulo)}&fondo=${encodeURIComponent(pathToFileURL(fondo).href)}`;
      await page.goto(url, { waitUntil: 'networkidle' });
      const salida = path.join(carpeta, `${nombre}-portada.jpg`);
      await page.screenshot({ path: salida, type: 'jpeg', quality: 92 });
      console.log('Portada:', salida);
    }
  } finally {
    await browser.close();
  }
}

main().catch(e => { console.error(e); process.exit(1); });
