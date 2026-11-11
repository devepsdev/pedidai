// Exporta cada semana-*.html de esta carpeta a salida/linkedin/<nombre>.jpg (1080×1350, formato vertical de LinkedIn).
//   node linkedin/imagenes.mjs [semana-01]   (sin argumento, todas)
import { chromium } from 'playwright-core';
import { mkdirSync, readdirSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join } from 'node:path';

const aqui = dirname(fileURLToPath(import.meta.url));
const salida = join(aqui, '..', 'salida', 'linkedin');
mkdirSync(salida, { recursive: true });

const filtro = process.argv[2];
const paginas = readdirSync(aqui).filter(f => /^semana-\d+\.html$/.test(f) && (!filtro || f.startsWith(filtro)));

const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  for (const archivo of paginas) {
    const page = await browser.newPage({ viewport: { width: 1080, height: 1350 } });
    await page.goto(pathToFileURL(join(aqui, archivo)).href, { waitUntil: 'networkidle' });
    const destino = join(salida, archivo.replace('.html', '.jpg'));
    await page.screenshot({ path: destino, type: 'jpeg', quality: 92 });
    console.log('Imagen:', destino);
  }
} finally {
  await browser.close();
}
