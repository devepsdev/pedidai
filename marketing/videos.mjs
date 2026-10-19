// Graba los vídeos promocionales sobre la copia local de PedidAI con la cuenta demo «Bar La Plaza».
//   node videos.mjs foto-albaran      (o tarifa, pedido, chat, home)
// Antes: voz generada con locutar.py y datos de la demo cargados con seed_demo.py (--pedido para «pedido»).
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { execFileSync } from 'node:child_process';
import { abrir, tocar, desplazar, resaltar, apagar, espera, exportarRedes, Grabacion, WEB } from './grabador.mjs';

const RAIZ = path.dirname(fileURLToPath(import.meta.url));
const DEMO = path.join(RAIZ, 'demo');
const OUTRO = pathToFileURL(path.join(RAIZ, 'outro.html')).href;
const acceso = fs.readFileSync(path.join(DEMO, 'acceso.txt'), 'utf8');
const EMAIL = acceso.match(/Email: (.+)/)[1].trim();
const PASSWORD = acceso.match(/Contraseña: (.+)/)[1].trim();

/** Vuelve a dejar la demo como al principio (con el pedido enviado y Huerta del Valle si hacen falta). */
function reiniciarDemo(video) {
  const opciones = [...(video.pedido ? ['--pedido'] : []), ...(video.huerta ? ['--huerta'] : [])];
  const sql = execFileSync('python', [path.join(RAIZ, 'seed_demo.py'), ...opciones], { encoding: 'utf8' });
  execFileSync('docker', ['exec', '-i', 'pedidai-mysql-local', 'sh', '-c',
    'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" pedidai_db'], { input: sql, stdio: ['pipe', 'ignore', 'ignore'] });
}

/** Toca la zona de subida y elige el archivo en el selector que se abre, como haría una persona. */
async function elegirArchivo(page, archivo) {
  const [selector] = await Promise.all([
    page.waitForEvent('filechooser'),
    tocar(page, page.getByRole('button', { name: /Arrastra aquí/ })),
  ]);
  await selector.setFiles(path.join(DEMO, archivo));
}

/** Sube un documento en la pantalla de lectura y lo lee (sin grabar la espera de la IA). */
async function subirYLeer(page, g, archivo) {
  await elegirArchivo(page, archivo);
  await espera(900);
  await tocar(page, page.getByRole('button', { name: 'Leer documento' }));
  await g.sinGrabar(() => page.getByText('Revisa y corrige').waitFor({ timeout: 180000 }), 1.4);
  await espera(400);
}

async function guardarYVerPrecios(page, g) {
  await tocar(page, page.getByRole('button', { name: /^Guardar \d+ líneas?$/ }));
  await g.sinGrabar(() => page.getByRole('link', { name: 'Ver mis precios' }).waitFor({ timeout: 30000 }), 0.9);
  await espera(700);
  await tocar(page, page.getByRole('link', { name: 'Ver mis precios' }));
  await page.getByText('Dónde comprar más barato').waitFor();
}

const VIDEOS = {
  'foto-albaran': {
    pedido: false,
    async grabar(page, g, d) {
      await g.escena('hook', d.hook, async () => {
        await espera(1600);
        await desplazar(page, 260, 1200);
      });
      await g.escena('foto', d.foto, async () => {
        await desplazar(page, -260, 600);
        await tocar(page, page.getByRole('link', { name: 'Subir albarán o tarifa' }).first());
        await page.getByRole('button', { name: /Arrastra aquí/ }).waitFor();
        await espera(500);
        await elegirArchivo(page, 'albaran-lopez.jpg');
      });
      await g.escena('lectura', d.lectura, async () => {
        await tocar(page, page.getByRole('button', { name: 'Leer documento' }));
        await g.sinGrabar(() => page.getByText('Revisa y corrige').waitFor({ timeout: 180000 }), 1.4);
        await espera(600);
        await desplazar(page, 560, 1600);
        await espera(900);
        await desplazar(page, 420, 1400);
      });
      await g.escena('comparativa', d.comparativa, async () => {
        await guardarYVerPrecios(page, g);
        await espera(600);
        await desplazar(page, 470, 1500);
        await espera(1200);
        await desplazar(page, 330, 1300);
      });
      await g.escena('cta', d.cta, async () => { await page.goto(OUTRO); });
    },
  },

  tarifa: {
    pedido: false,
    huerta: true,
    async grabar(page, g, d) {
      await g.escena('hook', d.hook, async () => {
        await espera(1500);
        await tocar(page, page.getByRole('link', { name: 'Subir albarán o tarifa' }).first());
        await page.getByRole('button', { name: /Arrastra aquí/ }).waitFor();
      });
      await g.escena('subir', d.subir, async () => {
        await subirYLeer(page, g, 'tarifa-huerta-del-valle.xlsx');
        await resaltar(page, page.getByRole('radiogroup').first());
        await espera(1300);
        await apagar(page);
        await desplazar(page, 560, 1600);
        await espera(500);
        await desplazar(page, 420, 1300);
      });
      await g.escena('comparar', d.comparar, async () => {
        await guardarYVerPrecios(page, g);
        await espera(400);
        const oferta = page.getByText(/te lo ofrece a/).first();
        await oferta.scrollIntoViewIfNeeded();
        await desplazar(page, 120, 600);
        await resaltar(page, oferta);
        await espera(2000);
        await apagar(page);
        await desplazar(page, 300, 1200);
      });
      await g.escena('cta', d.cta, async () => { await page.goto(OUTRO); });
    },
  },

  pedido: {
    pedido: true,
    async grabar(page, g, d) {
      await g.escena('hook', d.hook, async () => {
        await espera(1500);
        await tocar(page, page.getByRole('link', { name: 'Subir albarán o tarifa' }).first());
        await page.getByRole('button', { name: /Arrastra aquí/ }).waitFor();
      });
      await g.escena('subir', d.subir, async () => {
        await subirYLeer(page, g, 'albaran-lopez.jpg');
      });
      await g.escena('diferencias', d.diferencias, async () => {
        const tarjeta = page.getByText(/Comparado con tu pedido/);
        await tarjeta.scrollIntoViewIfNeeded();
        await desplazar(page, 60, 500);
        await espera(300);
        await resaltar(page, page.locator('[role=status]', { hasText: 'Comparado con tu pedido' }));
        await espera(2600);
      });
      await g.escena('cierre', d.cierre, async () => {
        await apagar(page);
        await desplazar(page, 300, 1400);
      });
      await g.escena('cta', d.cta, async () => { await page.goto(OUTRO); });
    },
  },

  chat: {
    pedido: false,
    subArriba: true,
    async grabar(page, g, d) {
      await g.escena('hook', d.hook, async () => {
        await espera(1500);
        await desplazar(page, 200, 1200);
      });
      await g.escena('escribir', d.escribir, async () => {
        await desplazar(page, -200, 400);
        await tocar(page, page.locator('main').getByText('Pedir por chat').first());
        const caja = page.getByRole('textbox');
        await caja.waitFor();
        await tocar(page, caja);
        await caja.pressSequentially('15 kg de tomates, 10 kg de patatas y 2 garrafas de aceite', { delay: 38 });
        await espera(300);
      });
      await g.escena('respuesta', d.respuesta, async () => {
        await tocar(page, page.getByRole('button', { name: 'Enviar', exact: true }));
        await espera(1200);
        await g.sinGrabar(() => page.getByText('Pedido pendiente para').first().waitFor({ timeout: 180000 }), 0.8);
        await espera(500);
        const tarjeta = page.getByText('Pedido pendiente para').first();
        await tarjeta.scrollIntoViewIfNeeded();
        await desplazar(page, 140, 700);
        await resaltar(page, tarjeta.locator('xpath=ancestor::div[contains(@class,"rounded-xl")][1]'));
        await espera(1500);
      });
      await g.escena('enviar', d.enviar, async () => {
        await apagar(page);
        const boton = page.getByRole('button', { name: 'Enviar al proveedor' }).first();
        await boton.scrollIntoViewIfNeeded();
        await espera(400);
        const b = await boton.boundingBox();
        await page.evaluate(([x, y]) => window.__tap(x, y), [b.x + b.width / 2, b.y + b.height / 2]);
        await espera(500);
        await desplazar(page, 380, 1200);
      });
      await g.escena('cta', d.cta, async () => { await page.goto(OUTRO); });
    },
  },

  // Vídeo de la home: todas las funciones en uno (albarán, pedido, precios, tarifa y chat)
  home: {
    pedido: true,
    huerta: true,
    web: true,
    async grabar(page, g, d) {
      await g.escena('hook', d.hook, async () => {
        await espera(1800);
        await desplazar(page, 260, 1300);
        await espera(900);
        await desplazar(page, -260, 700);
      });
      await g.escena('foto', d.foto, async () => {
        await tocar(page, page.getByRole('link', { name: 'Subir albarán o tarifa' }).first());
        await page.getByRole('button', { name: /Arrastra aquí/ }).waitFor();
        await espera(400);
        await elegirArchivo(page, 'albaran-lopez.jpg');
      });
      await g.escena('lectura', d.lectura, async () => {
        await tocar(page, page.getByRole('button', { name: 'Leer documento' }));
        await g.sinGrabar(() => page.getByText('Revisa y corrige').waitFor({ timeout: 180000 }), 1.4);
        await espera(500);
        await desplazar(page, 520, 1500);
      });
      await g.escena('pedido', d.pedido, async () => {
        const tarjeta = page.getByText(/Comparado con tu pedido/);
        await tarjeta.scrollIntoViewIfNeeded();
        await desplazar(page, 60, 500);
        await espera(500);
        const b = await tarjeta.boundingBox();
        await page.evaluate(([x, y]) => window.__tap(x, y), [b.x + 60, b.y + 10]);
        await espera(1600);
        await desplazar(page, 260, 1200);
      });
      await g.escena('precios', d.precios, async () => {
        await guardarYVerPrecios(page, g);
        await espera(500);
        await desplazar(page, 470, 1500);
        await espera(1100);
        await desplazar(page, 330, 1300);
      });
      await g.escena('tarifa', d.tarifa, async () => {
        await desplazar(page, -2000, 700);
        await tocar(page, page.getByRole('link', { name: 'Subir albarán o tarifa' }).first());
        await page.getByRole('button', { name: /Arrastra aquí/ }).waitFor();
        await subirYLeer(page, g, 'tarifa-huerta-del-valle.xlsx');
        await desplazar(page, 420, 1300);
        await guardarYVerPrecios(page, g);
        await espera(400);
        await desplazar(page, 440, 1400);
        await espera(1200);
      });
      await g.escena('chat', d.chat, async () => {
        await tocar(page, page.locator('app-navbar button').first());
        await espera(500);
        await tocar(page, page.locator('app-sidebar a[href="/chat"]'));
        const caja = page.getByRole('textbox');
        await caja.waitFor();
        await tocar(page, caja);
        await caja.pressSequentially('15 kg de tomates, 10 kg de patatas y 2 garrafas de aceite', { delay: 40 });
        await espera(300);
      });
      await g.escena('respuesta', d.respuesta, async () => {
        await tocar(page, page.getByRole('button', { name: 'Enviar', exact: true }));
        await espera(1200);
        await g.sinGrabar(() => page.getByText('Pedido pendiente para').first().waitFor({ timeout: 180000 }), 0.8);
        await espera(500);
        await desplazar(page, 420, 1500);
        const boton = page.getByRole('button', { name: 'Enviar al proveedor' }).first();
        await boton.scrollIntoViewIfNeeded();
        await espera(400);
        const b = await boton.boundingBox();
        await page.evaluate(([x, y]) => window.__tap(x, y), [b.x + b.width / 2, b.y + b.height / 2]);
      });
      await g.escena('cta', d.cta, async () => { await page.goto(OUTRO); });
    },
  },
};

async function main() {
  const nombre = process.argv[2];
  const video = VIDEOS[nombre];
  if (!video) throw new Error(`Vídeo desconocido: ${nombre}. Opciones: ${Object.keys(VIDEOS).join(', ')}`);
  const carpeta = path.join(RAIZ, 'salida', nombre);
  const durs = JSON.parse(fs.readFileSync(path.join(carpeta, 'voz', 'duraciones.json'), 'utf8'));
  const guion = JSON.parse(fs.readFileSync(path.join(RAIZ, 'guiones.json'), 'utf8'))[nombre];

  reiniciarDemo(video);
  const { browser, page } = await abrir(EMAIL, PASSWORD);
  try {
    await page.goto(`${WEB}/dashboard`, { waitUntil: 'networkidle' });
    await espera(1500);
    const g = new Grabacion(page, carpeta);
    await g.empezar();
    await video.grabar(page, g, durs);
    await g.terminar();
    const voces = Object.fromEntries(guion.map(e => [e.id, path.join(carpeta, 'voz', `${e.id}.wav`)]));
    const textos = Object.fromEntries(guion.map(e => [e.id, e.texto]));
    const mp4 = g.montar({ nombre, voces, textos, web: video.web, subArriba: video.subArriba });
    console.log('Vídeo:', mp4);
    if (!video.web) console.log('Para redes:', exportarRedes(mp4));
  } finally {
    await browser.close();
  }
}

main().catch(e => { console.error(e); process.exit(1); });
