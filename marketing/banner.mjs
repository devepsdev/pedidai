// Banner del canal de YouTube (2048×1152) con fotogramas del vídeo de la home a los lados.
//   node banner.mjs [fotograma izq] [fotograma der]   (por defecto 300 y 700)
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const RAIZ = path.dirname(fileURLToPath(import.meta.url));
const [izq = 300, der = 700] = process.argv.slice(2);
const fotograma = n => pathToFileURL(path.join(RAIZ, 'salida', 'home', 'secuencia', `s${String(n).padStart(6, '0')}.jpg`)).href;

const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 2048, height: 1152 } });
  const url = `${pathToFileURL(path.join(RAIZ, 'banner.html')).href}?izq=${encodeURIComponent(fotograma(izq))}&der=${encodeURIComponent(fotograma(der))}`;
  await page.goto(url, { waitUntil: 'networkidle' });
  const salida = path.join(RAIZ, 'salida', 'banner-youtube.jpg');
  await page.screenshot({ path: salida, type: 'jpeg', quality: 92 });
  console.log('Banner:', salida);
} finally {
  await browser.close();
}
