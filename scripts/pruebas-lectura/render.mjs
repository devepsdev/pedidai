// Convierte los HTML de ./docs en JPG (o PDF si esperado.json lo pide) con el Chrome instalado.
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(path.join(AQUI, '..', '..', 'marketing', 'package.json'));
const { chromium } = require('playwright-core');

const esperado = JSON.parse(fs.readFileSync(path.join(AQUI, 'esperado.json'), 'utf8'));
const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 880, height: 640 } });
  for (const archivo of Object.keys(esperado).filter(a => !a.startsWith('..'))) {
    const base = archivo.replace(/\.(jpg|pdf)$/, '');
    await page.goto(pathToFileURL(path.join(AQUI, 'docs', base + '.html')).href);
    const salida = path.join(AQUI, 'docs', archivo);
    if (archivo.endsWith('.pdf')) {
      await page.pdf({ path: salida, format: 'A4', printBackground: true });
    } else {
      await page.screenshot({ path: salida, type: 'jpeg', quality: 82, fullPage: true });
    }
    console.log(salida);
  }
} finally {
  await browser.close();
}
