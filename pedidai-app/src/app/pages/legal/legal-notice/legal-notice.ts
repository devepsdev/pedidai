import { Component, computed, inject } from '@angular/core';
import { LanguageService } from '../../../services/language.service';
import { Bilingual, LegalDoc, LegalDocument } from '../legal-doc';

const CONTENT: Bilingual<LegalDocument> = {
  es: {
    title: 'Aviso legal',
    updated: 'Última actualización: septiembre de 2026',
    sections: [
      {
        title: 'Titular del sitio web',
        facts: [['Nombre', 'Enrique Pérez Sánchez'], ['NIF', '43444166H'], ['Domicilio', 'Barcelona, España'], ['Email', 'hola@pedidai.es'], ['Web', 'https://pedidai.es']],
      },
      {
        title: 'Objeto',
        paragraphs: ['PedidAI es una herramienta para bares y restaurantes que compara los precios de sus proveedores a partir de sus albaranes, avisa de las subidas de precio y permite preparar y enviar pedidos. Usa inteligencia artificial para leer documentos y preparar pedidos.'],
      },
      {
        title: 'Propiedad intelectual',
        paragraphs: ['Los textos, el diseño, el código, los logotipos y demás elementos de este sitio web pertenecen a su titular y están protegidos por la normativa de propiedad intelectual. No se permite su reproducción o distribución sin autorización previa por escrito.'],
      },
      {
        title: 'Responsabilidad',
        list: [
          'Los datos obtenidos con inteligencia artificial pueden contener errores; el usuario debe revisarlos antes de usarlos.',
          'No se garantiza la disponibilidad ininterrumpida del servicio, aunque se hará lo razonable para mantenerlo operativo.',
          'Las decisiones comerciales tomadas con la información del servicio son responsabilidad del usuario.',
          'No nos hacemos responsables del contenido de sitios web de terceros enlazados.',
        ],
      },
      {
        title: 'Ley aplicable',
        paragraphs: ['Este aviso legal se rige por la ley española. Para cualquier controversia son competentes los juzgados y tribunales de Barcelona, salvo que la ley disponga otra cosa.'],
      },
    ],
  },
  ca: {
    title: 'Avís legal',
    updated: 'Darrera actualització: setembre de 2026',
    sections: [
      {
        title: 'Titular del lloc web',
        facts: [['Nom', 'Enrique Pérez Sánchez'], ['NIF', '43444166H'], ['Domicili', 'Barcelona, Espanya'], ['Email', 'hola@pedidai.es'], ['Web', 'https://pedidai.es']],
      },
      {
        title: 'Objecte',
        paragraphs: ['PedidAI és una eina per a bars i restaurants que compara els preus dels seus proveïdors a partir dels seus albarans, avisa de les pujades de preu i permet preparar i enviar comandes. Fa servir intel·ligència artificial per llegir documents i preparar comandes.'],
      },
      {
        title: 'Propietat intel·lectual',
        paragraphs: ['Els textos, el disseny, el codi, els logotips i la resta d’elements d’aquest lloc web pertanyen al seu titular i estan protegits per la normativa de propietat intel·lectual. No se’n permet la reproducció o distribució sense autorització prèvia per escrit.'],
      },
      {
        title: 'Responsabilitat',
        list: [
          'Les dades obtingudes amb intel·ligència artificial poden contenir errors; l’usuari les ha de revisar abans de fer-les servir.',
          'No es garanteix la disponibilitat ininterrompuda del servei, tot i que es farà el que sigui raonable per mantenir-lo operatiu.',
          'Les decisions comercials preses amb la informació del servei són responsabilitat de l’usuari.',
          'No ens fem responsables del contingut de llocs web de tercers enllaçats.',
        ],
      },
      {
        title: 'Llei aplicable',
        paragraphs: ['Aquest avís legal es regeix per la llei espanyola. Per a qualsevol controvèrsia són competents els jutjats i tribunals de Barcelona, llevat que la llei disposi una altra cosa.'],
      },
    ],
  },
};

@Component({
  selector: 'app-legal-notice',
  imports: [LegalDoc],
  template: `<app-legal-doc [doc]="doc()" />`,
})
export class LegalNotice {
  private language = inject(LanguageService);
  doc = computed(() => CONTENT[this.language.current()]);
}
