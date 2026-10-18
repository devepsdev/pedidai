// Grabación de PedidAI en vertical (1080×1920) con el Chrome instalado, para Instagram y TikTok.
// Captura los fotogramas con CDP (Page.startScreencast) a resolución real, quita las esperas marcadas
// (lecturas de la IA) y monta con ffmpeg el vídeo, la voz de cada escena y los subtítulos.
import { chromium } from 'playwright-core';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

export const API = 'http://localhost:8085/api';
export const WEB = 'http://localhost:4200';
const ANCHO = 405, ALTO = 720, ESCALA = 1080 / ANCHO;
const FFMPEG_DIR = path.join(process.env.LOCALAPPDATA, 'Microsoft', 'WinGet', 'Packages',
  'Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe', 'ffmpeg-9.0.2-full_build', 'bin');
const FFMPEG = path.join(FFMPEG_DIR, 'ffmpeg.exe');
export const espera = ms => new Promise(r => setTimeout(r, ms));

/** Abre Chrome en vertical con la sesión ya iniciada (sin grabar el login) y las cookies decididas. */
export async function abrir(email, password) {
  const r = await fetch(`${API}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }),
  });
  if (!r.ok) throw new Error(`Login de la demo: ${r.status}`);
  const { token, user } = (await r.json()).data;
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const ctx = await browser.newContext({
    viewport: { width: ANCHO, height: ALTO }, deviceScaleFactor: ESCALA, isMobile: true, hasTouch: true, locale: 'es-ES',
  });
  await ctx.addInitScript(({ token, user }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('lang', 'es');
    localStorage.setItem('cookieConsent', JSON.stringify({ v: 2, analytics: false, ads: false, date: new Date().toISOString() }));
    localStorage.setItem('onboarding-hidden:' + user.uuid, '1');
    // Círculo en cada toque, como en las grabaciones de pantalla del móvil
    document.addEventListener('DOMContentLoaded', () => {
      const st = document.createElement('style');
      st.textContent = `.__tap{position:fixed;width:46px;height:46px;margin:-23px 0 0 -23px;border-radius:50%;
        background:rgba(14,165,233,.30);border:3px solid rgba(14,165,233,.95);pointer-events:none;z-index:2147483647;
        animation:__tap .65s ease-out forwards}@keyframes __tap{from{transform:scale(.45);opacity:1}to{transform:scale(1.5);opacity:0}}
        ::-webkit-scrollbar{display:none}`;
      document.head.appendChild(st);
    });
    window.__tap = (x, y) => {
      const d = document.createElement('div');
      d.className = '__tap';
      d.style.left = x + 'px';
      d.style.top = y + 'px';
      document.body.appendChild(d);
      setTimeout(() => d.remove(), 800);
    };
  }, { token, user });
  const page = await ctx.newPage();
  return { browser, ctx, page };
}

/**
 * Toca un elemento: lo pone a la vista, dibuja el círculo y hace clic en su centro. El clic va por coordenadas
 * porque, con la captura CDP activa, la comprobación de Playwright cree que el elemento está tapado.
 */
export async function tocar(page, locator, { pausa = 350 } = {}) {
  await locator.waitFor({ state: 'visible' });
  await locator.scrollIntoViewIfNeeded();
  await espera(150);
  const b = await locator.boundingBox();
  const x = b.x + b.width / 2, y = b.y + b.height / 2;
  await page.evaluate(([x, y]) => window.__tap(x, y), [x, y]);
  await espera(pausa);
  await page.mouse.click(x, y);
}

/** Desplaza suavemente el contenedor que tenga scroll (la app usa uno propio dentro del layout). */
export async function desplazar(page, px, ms = 900) {
  await page.evaluate(([px, ms]) => {
    const candidatos = [document.scrollingElement, ...document.querySelectorAll('main, div')]
      .filter(e => e && e.scrollHeight > e.clientHeight + 20 && getComputedStyle(e).overflowY !== 'hidden'
        && (e === document.scrollingElement || /(auto|scroll)/.test(getComputedStyle(e).overflowY)));
    const el = candidatos.sort((a, b) => b.clientHeight - a.clientHeight)[0] || document.scrollingElement;
    const desde = el.scrollTop, t0 = performance.now();
    return new Promise(res => {
      const paso = t => {
        const k = Math.min(1, (t - t0) / ms), e = k < .5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
        el.scrollTop = desde + px * e;
        k < 1 ? requestAnimationFrame(paso) : res();
      };
      requestAnimationFrame(paso);
    });
  }, [px, ms]);
}

export class Grabacion {
  constructor(page, carpeta) {
    this.page = page;
    this.carpeta = carpeta;
    this.dirFotos = path.join(carpeta, 'fotogramas');
    fs.rmSync(this.dirFotos, { recursive: true, force: true });
    fs.mkdirSync(this.dirFotos, { recursive: true });
    this.fotos = [];
    this.marcas = [];
    this.saltos = [];
    this.pendientes = [];
  }

  ahora() { return Date.now() / 1000; }

  async empezar() {
    this.cdp = await this.page.context().newCDPSession(this.page);
    this.cdp.on('Page.screencastFrame', ({ data, metadata, sessionId }) => {
      const archivo = path.join(this.dirFotos, `f${String(this.fotos.length).padStart(5, '0')}.jpg`);
      this.fotos.push({ archivo, t: metadata.timestamp });
      this.pendientes.push(fs.promises.writeFile(archivo, Buffer.from(data, 'base64')));
      this.cdp.send('Page.screencastFrameAck', { sessionId }).catch(() => {});
    });
    await this.cdp.send('Page.startScreencast', { format: 'jpeg', quality: 92, maxWidth: 1080, maxHeight: 1920 });
    // Fuerza un primer fotograma aunque la página no cambie
    await this.page.evaluate(() => { document.body.style.outline = '0 solid transparent'; });
    await espera(300);
    this.inicio = this.ahora();
  }

  saltado(desde = 0) {
    return this.saltos.filter(([a]) => a >= desde).reduce((s, [a, b]) => s + (b - a), 0);
  }

  /** Ejecuta una espera (p. ej. la lectura de la IA) que no saldrá en el vídeo. */
  async sinGrabar(fn, margen = 0.6) {
    const a = this.ahora() + margen;
    const r = await fn();
    const b = this.ahora() - 0.2;
    if (b > a) this.saltos.push([a, b]);
    return r;
  }

  /** Una escena: sus acciones y, como mínimo, lo que dura su frase más un respiro. */
  async escena(id, duracion, acciones = async () => {}) {
    const t = this.ahora();
    this.marcas.push({ id, t, duracion });
    await acciones();
    while ((this.ahora() - t) - this.saltado(t) < duracion + 0.35) await espera(40);
  }

  async terminar() {
    await espera(200);
    // El final es ahora: guardar los fotogramas en disco puede tardar y no debe alargar el último
    this.fin = this.ahora();
    await this.cdp.send('Page.stopScreencast');
    await Promise.all(this.pendientes);
  }

  /** Tiempo en el vídeo final de un instante de la grabación (descontando las esperas quitadas). */
  final(t) {
    const quitado = this.saltos.reduce((s, [a, b]) => s + Math.max(0, Math.min(t, b) - a), 0);
    return Math.max(0, t - this.inicio - quitado);
  }

  /** Monta el MP4: imagen a 30 fps, voz de cada escena en su sitio a −14 LUFS y subtítulos quemados. */
  montar({ nombre, voces, textos }) {
    // Remuestreo a 30 fps exactos: a cada instante k/30 le toca el último fotograma capturado hasta entonces.
    // (El concat de ffmpeg alarga a 1/25 s los fotogramas más cortos y el vídeo saldría más largo que la voz.)
    const FPS = 30;
    const fotos = this.fotos.filter(f => f.t >= this.inicio - 0.5).sort((a, b) => a.t - b.t)
      .map(f => ({ archivo: f.archivo, tf: this.final(Math.max(f.t, this.inicio)) }));
    const total = this.final(this.fin);
    const dirSec = path.join(this.carpeta, 'secuencia');
    fs.rmSync(dirSec, { recursive: true, force: true });
    fs.mkdirSync(dirSec);
    let j = 0;
    for (let k = 0; k < Math.ceil(total * FPS); k++) {
      while (j + 1 < fotos.length && fotos[j + 1].tf <= k / FPS) j++;
      fs.linkSync(fotos[j].archivo, path.join(dirSec, `s${String(k).padStart(6, '0')}.jpg`));
    }
    fs.writeFileSync(path.join(this.carpeta, 'linea-de-tiempo.json'), JSON.stringify({
      escenas: this.marcas.map(m => ({ id: m.id, inicio: +this.final(m.t).toFixed(2), voz: m.duracion })),
      total: +total.toFixed(2),
      esperasQuitadas: this.saltos.map(([a, b]) => +(b - a).toFixed(2)),
      fotogramas: fotos.length,
    }, null, 2));

    const ass = path.join(this.carpeta, 'subtitulos.ass');
    fs.writeFileSync(ass, subtitulos(this.marcas.map((m, i) => ({
      inicio: this.final(m.t) + 0.05,
      fin: Math.min(this.final(m.t) + m.duracion + 0.25, i + 1 < this.marcas.length ? this.final(this.marcas[i + 1].t) : 1e9),
      texto: textos[m.id], gancho: i === 0,
    }))), 'utf8');

    const salida = path.join(this.carpeta, `${nombre}.mp4`);
    const args = ['-y', '-v', 'error', '-framerate', String(FPS), '-i', path.join(dirSec, 's%06d.jpg')];
    const mezcla = [];
    this.marcas.forEach((m, i) => {
      args.push('-i', voces[m.id]);
      mezcla.push(`[${i + 1}:a]adelay=${Math.round(this.final(m.t) * 1000)}:all=1[a${i}]`);
    });
    const etiquetas = this.marcas.map((_, i) => `[a${i}]`).join('');
    const assFiltro = ass.replace(/\\/g, '/').replace(/^([A-Za-z]):/, '$1\\:');
    const filtro = [
      `[0:v]scale=1080:1920:flags=lanczos,subtitles='${assFiltro}'[v]`,
      ...mezcla,
      `${etiquetas}amix=inputs=${this.marcas.length}:normalize=0,loudnorm=I=-14:TP=-1.5:LRA=11,apad[a]`,
    ].join(';');
    args.push('-filter_complex', filtro, '-map', '[v]', '-map', '[a]', '-shortest',
      '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-profile:v', 'high',
      '-c:a', 'aac', '-b:a', '192k', '-ar', '48000', '-movflags', '+faststart', salida);
    execFileSync(FFMPEG, args, { stdio: 'inherit' });
    return salida;
  }
}

/** Subtítulos ASS: el gancho arriba y grande; el resto abajo, por encima de los botones de TikTok. */
function subtitulos(lineas) {
  const ts = s => {
    const h = Math.floor(s / 3600), m = Math.floor(s / 60) % 60, x = (s % 60).toFixed(2).padStart(5, '0');
    return `${h}:${String(m).padStart(2, '0')}:${x}`;
  };
  const partir = (t, max) => {
    const palabras = t.split(' '), filas = [''];
    for (const p of palabras) {
      const f = filas.length - 1;
      if ((filas[f] + ' ' + p).trim().length > max && filas[f]) filas.push(p);
      else filas[f] = (filas[f] + ' ' + p).trim();
    }
    return filas.join('\\N');
  };
  return `[Script Info]
ScriptType: v4.00+
PlayResX: 1080
PlayResY: 1920
WrapStyle: 2

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Sub,Arial,60,&H00FFFFFF,&H00FFFFFF,&H00151515,&H96000000,-1,0,0,0,100,100,0,0,1,5,2,2,70,70,360,1
Style: Gancho,Arial,74,&H0000E5FF,&H00FFFFFF,&H00151515,&H96000000,-1,0,0,0,100,100,0,0,1,6,3,8,60,60,250,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
${lineas.filter(l => l.texto).map(l => `Dialogue: 0,${ts(l.inicio)},${ts(l.fin)},${l.gancho ? 'Gancho' : 'Sub'},,0,0,0,,${partir(l.texto, l.gancho ? 22 : 28)}`).join('\n')}
`;
}
