import { Component, computed, inject } from '@angular/core';
import { LanguageService } from '../../../services/language.service';
import { AnalyticsService } from '../../../services/analytics.service';
import { Bilingual, LegalDoc, LegalDocument } from '../legal-doc';

const CONTENT: Bilingual<LegalDocument> = {
  es: {
    title: 'Política de cookies',
    updated: 'Última actualización: septiembre de 2026',
    intro: 'Las cookies y el almacenamiento local son pequeños datos que la web guarda en tu navegador. Usamos los técnicos imprescindibles y, solo si lo aceptas, los de analítica y publicidad de Google.',
    sections: [
      {
        title: 'Técnicos (siempre activos)',
        paragraphs: ['Necesarios para que la web funcione. No requieren consentimiento.'],
        table: {
          head: ['Nombre', 'Para qué', 'Duración'],
          rows: [
            ['token', 'Mantener tu sesión iniciada (almacenamiento local).', 'Hasta que cierras sesión o caduca'],
            ['user', 'Datos básicos de tu usuario para mostrar la aplicación (almacenamiento local).', 'Hasta que cierras sesión'],
            ['lang', 'Recordar el idioma elegido (almacenamiento local).', 'Permanente'],
            ['cookieConsent', 'Recordar tu decisión sobre las cookies (almacenamiento local).', 'Hasta que la cambies'],
          ],
        },
      },
      {
        title: 'Analítica (solo con tu consentimiento)',
        table: {
          head: ['Nombre', 'Proveedor', 'Para qué', 'Duración'],
          rows: [
            ['_ga, _ga_*', 'Google Analytics (Google Ireland Ltd.)', 'Contar visitas y saber cómo se usa la web, de forma agregada.', '2 años'],
          ],
        },
      },
      {
        title: 'Publicidad (solo con tu consentimiento)',
        table: {
          head: ['Nombre', 'Proveedor', 'Para qué', 'Duración'],
          rows: [
            ['_gcl_au, _gcl_aw', 'Google Ads (Google Ireland Ltd.)', 'Saber si alguien llega desde nuestros anuncios y se registra, para medir las campañas.', '90 días'],
          ],
        },
        paragraphs: ['Usamos el modo de consentimiento de Google: si no aceptas, no se carga ninguna etiqueta de Google.'],
      },
      {
        title: 'Cómo cambiar tu decisión',
        paragraphs: [
          'Puedes aceptar, rechazar o elegir por categorías en el aviso que aparece la primera vez, y cambiar de opinión en cualquier momento desde «Configurar cookies», en el pie de página. Si retiras el consentimiento, borramos las cookies de Google de este dominio.',
          'También puedes borrar o bloquear las cookies desde la configuración de tu navegador; si bloqueas las técnicas, la aplicación puede no funcionar.',
        ],
      },
      {
        title: 'Más información',
        paragraphs: ['Sobre cómo tratamos tus datos, consulta la política de privacidad. Para cualquier duda: hola@pedidai.es.'],
      },
    ],
  },
  ca: {
    title: 'Política de galetes',
    updated: 'Darrera actualització: setembre de 2026',
    intro: 'Les galetes i l’emmagatzematge local són petites dades que el web desa al teu navegador. Fem servir les tècniques imprescindibles i, només si ho acceptes, les d’analítica i publicitat de Google.',
    sections: [
      {
        title: 'Tècniques (sempre actives)',
        paragraphs: ['Necessàries perquè el web funcioni. No requereixen consentiment.'],
        table: {
          head: ['Nom', 'Per a què', 'Durada'],
          rows: [
            ['token', 'Mantenir la teva sessió iniciada (emmagatzematge local).', 'Fins que tanques la sessió o caduca'],
            ['user', 'Dades bàsiques del teu usuari per mostrar l’aplicació (emmagatzematge local).', 'Fins que tanques la sessió'],
            ['lang', 'Recordar l’idioma triat (emmagatzematge local).', 'Permanent'],
            ['cookieConsent', 'Recordar la teva decisió sobre les galetes (emmagatzematge local).', 'Fins que la canviïs'],
          ],
        },
      },
      {
        title: 'Analítica (només amb el teu consentiment)',
        table: {
          head: ['Nom', 'Proveïdor', 'Per a què', 'Durada'],
          rows: [
            ['_ga, _ga_*', 'Google Analytics (Google Ireland Ltd.)', 'Comptar visites i saber com s’utilitza el web, de manera agregada.', '2 anys'],
          ],
        },
      },
      {
        title: 'Publicitat (només amb el teu consentiment)',
        table: {
          head: ['Nom', 'Proveïdor', 'Per a què', 'Durada'],
          rows: [
            ['_gcl_au, _gcl_aw', 'Google Ads (Google Ireland Ltd.)', 'Saber si algú arriba des dels nostres anuncis i s’hi registra, per mesurar les campanyes.', '90 dies'],
          ],
        },
        paragraphs: ['Fem servir el mode de consentiment de Google: si no ho acceptes, no es carrega cap etiqueta de Google.'],
      },
      {
        title: 'Com canviar la teva decisió',
        paragraphs: [
          'Pots acceptar, rebutjar o triar per categories a l’avís que apareix la primera vegada, i canviar d’opinió en qualsevol moment des de «Configurar galetes», al peu de pàgina. Si retires el consentiment, esborrem les galetes de Google d’aquest domini.',
          'També pots esborrar o bloquejar les galetes des de la configuració del navegador; si bloqueges les tècniques, l’aplicació pot no funcionar.',
        ],
      },
      {
        title: 'Més informació',
        paragraphs: ['Sobre com tractem les teves dades, consulta la política de privacitat. Per a qualsevol dubte: hola@pedidai.es.'],
      },
    ],
  },
};

@Component({
  selector: 'app-cookie-policy',
  imports: [LegalDoc],
  template: `
    <app-legal-doc [doc]="doc()" />
    <div class="bg-white pb-16 -mt-10 px-4">
      <div class="max-w-3xl mx-auto">
        <button type="button" (click)="openSettings()"
          class="px-5 py-2.5 bg-slate-800 hover:bg-slate-700 text-white text-sm font-semibold rounded-lg">
          {{ language.current() === 'ca' ? 'Configurar galetes' : 'Configurar cookies' }}
        </button>
      </div>
    </div>
  `,
})
export class CookiePolicy {
  protected language = inject(LanguageService);
  private analytics = inject(AnalyticsService);
  doc = computed(() => CONTENT[this.language.current()]);

  openSettings() {
    this.analytics.openPreferences();
  }
}
