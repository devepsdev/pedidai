// Exporta flyer.html a salida/flyer/: PDF para imprenta (A6 con 3 mm de sangrado, dos caras)
// y una vista previa PNG de cada cara.
//   node flyer.mjs
import { chromium } from 'playwright-core';
import { mkdirSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join } from 'node:path';

const aqui = dirname(fileURLToPath(import.meta.url));
const salida = join(aqui, 'salida', 'flyer');
mkdirSync(salida, { recursive: true });

const browser = await chromium.launch({ channel: 'chrome', headless: true });
const page = await browser.newPage({ deviceScaleFactor: 4 });
await page.goto(pathToFileURL(join(aqui, 'flyer.html')).href, { waitUntil: 'networkidle' });
await page.waitForTimeout(300);

await page.pdf({ path: join(salida, 'flyer-pedidai-A6.pdf'), width: '111mm', height: '154mm', printBackground: true, preferCSSPageSize: true });
const caras = await page.$$('.cara');
for (const [i, cara] of caras.entries()) {
  await cara.screenshot({ path: join(salida, `flyer-${i === 0 ? 'delante' : 'detras'}.png`) });
}
await browser.close();
console.log('Flyer exportado en', salida);
