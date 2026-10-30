// Banner del canal de YouTube (2048×1152), portada de Facebook (1640×624) y de LinkedIn (2256×382, el doble de 1128×191) con fotogramas del vídeo de la home a los lados.
//   node banner.mjs [fotograma izq] [fotograma der]   (por defecto 300 y 700)
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const RAIZ = path.dirname(fileURLToPath(import.meta.url));
const [izq = 300, der = 700] = process.argv.slice(2);
const fotograma = n => pathToFileURL(path.join(RAIZ, 'salida', 'home', 'secuencia', `s${String(n).padStart(6, '0')}.jpg`)).href;

const FORMATOS = { youtube: [2048, 1152], facebook: [1640, 624], linkedin: [2256, 382] };

const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  for (const [formato, [width, height]] of Object.entries(FORMATOS)) {
    const page = await browser.newPage({ viewport: { width, height } });
    const url = `${pathToFileURL(path.join(RAIZ, 'banner.html')).href}?formato=${formato}`
      + `&izq=${encodeURIComponent(fotograma(izq))}&der=${encodeURIComponent(fotograma(der))}`;
    await page.goto(url, { waitUntil: 'networkidle' });
    const salida = path.join(RAIZ, 'salida', `banner-${formato}.jpg`);
    await page.screenshot({ path: salida, type: 'jpeg', quality: 92 });
    console.log('Banner:', salida);
  }
} finally {
  await browser.close();
}
