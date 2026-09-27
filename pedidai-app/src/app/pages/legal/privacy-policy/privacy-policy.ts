import { Component, computed, inject } from '@angular/core';
import { LanguageService } from '../../../services/language.service';
import { Bilingual, LegalDoc, LegalDocument } from '../legal-doc';

const OWNER: [string, string][] = [
  ['Titular', 'Enrique Pérez Sánchez'],
  ['NIF', '43444166H'],
  ['Email', 'hola@pedidai.es'],
  ['Web', 'https://pedidai.es'],
];

const CONTENT: Bilingual<LegalDocument> = {
  es: {
    title: 'Política de privacidad',
    updated: 'Última actualización: septiembre de 2026',
    intro: 'Explicamos qué datos tratamos en PedidAI, para qué, con quién los compartimos y cómo puedes ejercer tus derechos.',
    sections: [
      { title: 'Responsable del tratamiento', facts: OWNER },
      {
        title: 'Qué datos tratamos',
        list: [
          'Datos de la cuenta: nombre, email, contraseña (guardada cifrada), nombre del negocio e idioma. Opcionalmente, teléfono, dirección y CIF/NIF.',
          'Datos de tu actividad: proveedores, productos, precios, albaranes y facturas que subes, pedidos y los mensajes que escribes en el chat de pedidos.',
          'Datos de tus proveedores que introduces (nombre, persona de contacto, email y teléfono).',
          'Datos técnicos: dirección IP y datos de navegación necesarios para la seguridad del servicio, y las cookies descritas en la política de cookies.',
        ],
      },
      {
        title: 'Para qué los usamos',
        list: [
          'Prestar el servicio: leer tus albaranes, comparar precios entre tus proveedores, avisarte de subidas y preparar pedidos.',
          'Enviar a tus proveedores los pedidos que tú decidas enviar.',
          'Enviarte emails del servicio: verificación de la cuenta, cambio de contraseña y avisos sobre tu prueba o tu plan.',
          'Proteger la plataforma: prevenir accesos indebidos y abusos (por ejemplo, límites de uso).',
          'Solo si lo aceptas en el aviso de cookies: medir el uso de la web (Google Analytics) y la eficacia de nuestros anuncios (Google Ads).',
        ],
      },
      {
        title: 'Base legal',
        list: [
          'Ejecución del contrato (incluida la prueba gratuita): para prestar el servicio que solicitas.',
          'Consentimiento: para las cookies de analítica y publicidad. Puedes retirarlo en cualquier momento desde «Configurar cookies», en el pie de página.',
          'Interés legítimo: para la seguridad de la plataforma y la prevención del fraude.',
        ],
      },
      {
        title: 'Con quién compartimos los datos',
        paragraphs: ['Solo con los proveedores de servicios necesarios para que PedidAI funcione, que tratan los datos siguiendo nuestras instrucciones:'],
        table: {
          head: ['Proveedor', 'Para qué', 'Dónde'],
          rows: [
            ['OVHcloud', 'Alojamiento del servidor y de la base de datos.', 'Unión Europea (Francia)'],
            ['DeepSeek (Hangzhou DeepSeek Artificial Intelligence Co., Ltd.)', 'Inteligencia artificial: (1) estructurar el texto de tus albaranes y facturas, que extraemos antes en nuestro propio servidor (la imagen no se envía); (2) preparar los pedidos que pides por chat, para lo que recibe tu mensaje y los datos de proveedores, productos y precios necesarios.', 'China (transferencia internacional; ver punto 6)'],
            ['Google (Gmail)', 'Envío de los emails de la plataforma y de los pedidos a tus proveedores.', 'UE y EE. UU. (Marco de Privacidad de Datos UE-EE. UU.)'],
            ['Google Analytics y Google Ads (Google Ireland Ltd.)', 'Analítica de la web y medición de campañas. Solo con tu consentimiento.', 'UE y EE. UU. (Marco de Privacidad de Datos UE-EE. UU.)'],
            ['Cloudflare', 'Servicio de DNS del dominio (traduce pedidai.es a la dirección del servidor; el tráfico de la web no pasa por sus servidores).', 'Global'],
          ],
        },
        list: [
          'Tus proveedores: cuando envías un pedido, el proveedor recibe el pedido, el nombre de tu negocio y tus datos de contacto para poder responderte.',
          'No vendemos datos ni compartimos la información de una empresa con otras empresas usuarias de PedidAI.',
        ],
      },
      {
        title: 'Transferencias internacionales (DeepSeek)',
        paragraphs: [
          'DeepSeek presta su servicio desde China, un país que no cuenta con una decisión de adecuación de la Comisión Europea. Por eso enviamos solo la información imprescindible: nunca imágenes, contraseñas ni datos de tu cuenta, sino el texto de los documentos y los datos de catálogo necesarios para cada tarea.',
          'La transferencia es necesaria para prestar las funciones de lectura de albaranes y pedidos por chat que solicitas. Te recomendamos no escribir datos personales de terceros en el chat. Si no quieres que tus datos se procesen con DeepSeek, puedes no usar esas funciones o escribirnos.',
        ],
      },
      {
        title: 'Cuánto tiempo los conservamos',
        list: [
          'Mientras tu cuenta esté activa.',
          'Si la prueba gratuita termina y no contratas, conservamos los datos 30 días por si quieres continuar y después los eliminamos.',
          'Si das de baja la cuenta, eliminamos los datos en un plazo de 30 días, salvo los que debamos conservar por obligaciones legales (por ejemplo, facturación, durante el plazo legal).',
        ],
      },
      {
        title: 'Seguridad',
        paragraphs: ['Usamos conexiones cifradas (HTTPS), guardamos las contraseñas cifradas, separamos los datos de cada empresa y limitamos los intentos de acceso. Ningún sistema es infalible; si detectamos una brecha que afecte a tus datos, te lo comunicaremos según la ley.'],
      },
      {
        title: 'Tus derechos',
        paragraphs: [
          'Puedes solicitar el acceso, la rectificación, la supresión, la portabilidad, la limitación y la oposición al tratamiento de tus datos, así como retirar tu consentimiento, escribiendo a hola@pedidai.es. Te responderemos en un plazo máximo de un mes.',
          'Si consideras que no hemos atendido tus derechos, puedes reclamar ante la Agencia Española de Protección de Datos (www.aepd.es).',
        ],
      },
      {
        title: 'Cambios en esta política',
        paragraphs: ['Si hacemos cambios importantes te avisaremos por email o en la propia aplicación.'],
      },
    ],
  },
  ca: {
    title: 'Política de privacitat',
    updated: 'Darrera actualització: setembre de 2026',
    intro: 'T’expliquem quines dades tractem a PedidAI, per a què, amb qui les compartim i com pots exercir els teus drets.',
    sections: [
      { title: 'Responsable del tractament', facts: OWNER },
      {
        title: 'Quines dades tractem',
        list: [
          'Dades del compte: nom, email, contrasenya (desada xifrada), nom del negoci i idioma. Opcionalment, telèfon, adreça i CIF/NIF.',
          'Dades de la teva activitat: proveïdors, productes, preus, albarans i factures que puges, comandes i els missatges que escrius al xat de comandes.',
          'Dades dels teus proveïdors que hi introdueixes (nom, persona de contacte, email i telèfon).',
          'Dades tècniques: adreça IP i dades de navegació necessàries per a la seguretat del servei, i les galetes descrites a la política de galetes.',
        ],
      },
      {
        title: 'Per a què les fem servir',
        list: [
          'Prestar el servei: llegir els teus albarans, comparar preus entre els teus proveïdors, avisar-te de pujades i preparar comandes.',
          'Enviar als teus proveïdors les comandes que tu decideixis enviar.',
          'Enviar-te correus del servei: verificació del compte, canvi de contrasenya i avisos sobre la teva prova o el teu pla.',
          'Protegir la plataforma: prevenir accessos indeguts i abusos (per exemple, límits d’ús).',
          'Només si ho acceptes a l’avís de galetes: mesurar l’ús del web (Google Analytics) i l’eficàcia dels nostres anuncis (Google Ads).',
        ],
      },
      {
        title: 'Base legal',
        list: [
          'Execució del contracte (inclosa la prova gratuïta): per prestar el servei que sol·licites.',
          'Consentiment: per a les galetes d’analítica i publicitat. El pots retirar en qualsevol moment des de «Configurar galetes», al peu de pàgina.',
          'Interès legítim: per a la seguretat de la plataforma i la prevenció del frau.',
        ],
      },
      {
        title: 'Amb qui compartim les dades',
        paragraphs: ['Només amb els proveïdors de serveis necessaris perquè PedidAI funcioni, que tracten les dades seguint les nostres instruccions:'],
        table: {
          head: ['Proveïdor', 'Per a què', 'On'],
          rows: [
            ['OVHcloud', 'Allotjament del servidor i de la base de dades.', 'Unió Europea (França)'],
            ['DeepSeek (Hangzhou DeepSeek Artificial Intelligence Co., Ltd.)', 'Intel·ligència artificial: (1) estructurar el text dels teus albarans i factures, que extraiem abans al nostre propi servidor (la imatge no s’envia); (2) preparar les comandes que demanes per xat, per a la qual cosa rep el teu missatge i les dades de proveïdors, productes i preus necessàries.', 'Xina (transferència internacional; vegeu el punt 6)'],
            ['Google (Gmail)', 'Enviament dels correus de la plataforma i de les comandes als teus proveïdors.', 'UE i EUA (Marc de Privacitat de Dades UE-EUA)'],
            ['Google Analytics i Google Ads (Google Ireland Ltd.)', 'Analítica del web i mesura de campanyes. Només amb el teu consentiment.', 'UE i EUA (Marc de Privacitat de Dades UE-EUA)'],
            ['Cloudflare', 'Servei de DNS del domini (tradueix pedidai.es a l’adreça del servidor; el trànsit del web no passa pels seus servidors).', 'Global'],
          ],
        },
        list: [
          'Els teus proveïdors: quan envies una comanda, el proveïdor rep la comanda, el nom del teu negoci i les teves dades de contacte per poder respondre’t.',
          'No venem dades ni compartim la informació d’una empresa amb altres empreses usuàries de PedidAI.',
        ],
      },
      {
        title: 'Transferències internacionals (DeepSeek)',
        paragraphs: [
          'DeepSeek presta el seu servei des de la Xina, un país que no té una decisió d’adequació de la Comissió Europea. Per això enviem només la informació imprescindible: mai imatges, contrasenyes ni dades del teu compte, sinó el text dels documents i les dades de catàleg necessàries per a cada tasca.',
          'La transferència és necessària per prestar les funcions de lectura d’albarans i comandes per xat que sol·licites. Et recomanem no escriure dades personals de tercers al xat. Si no vols que les teves dades es processin amb DeepSeek, pots no fer servir aquestes funcions o escriure’ns.',
        ],
      },
      {
        title: 'Quant de temps les conservem',
        list: [
          'Mentre el teu compte estigui actiu.',
          'Si la prova gratuïta s’acaba i no contractes, conservem les dades 30 dies per si vols continuar i després les eliminem.',
          'Si dones de baixa el compte, eliminem les dades en un termini de 30 dies, excepte les que hàgim de conservar per obligacions legals (per exemple, facturació, durant el termini legal).',
        ],
      },
      {
        title: 'Seguretat',
        paragraphs: ['Fem servir connexions xifrades (HTTPS), desem les contrasenyes xifrades, separem les dades de cada empresa i limitem els intents d’accés. Cap sistema és infal·lible; si detectem una bretxa que afecti les teves dades, t’ho comunicarem segons la llei.'],
      },
      {
        title: 'Els teus drets',
        paragraphs: [
          'Pots sol·licitar l’accés, la rectificació, la supressió, la portabilitat, la limitació i l’oposició al tractament de les teves dades, així com retirar el teu consentiment, escrivint a hola@pedidai.es. Et respondrem en un termini màxim d’un mes.',
          'Si consideres que no hem atès els teus drets, pots reclamar davant l’Agència Espanyola de Protecció de Dades (www.aepd.es).',
        ],
      },
      {
        title: 'Canvis en aquesta política',
        paragraphs: ['Si fem canvis importants t’avisarem per email o a la mateixa aplicació.'],
      },
    ],
  },
};

@Component({
  selector: 'app-privacy-policy',
  imports: [LegalDoc],
  template: `<app-legal-doc [doc]="doc()" />`,
})
export class PrivacyPolicy {
  private language = inject(LanguageService);
  doc = computed(() => CONTENT[this.language.current()]);
}
