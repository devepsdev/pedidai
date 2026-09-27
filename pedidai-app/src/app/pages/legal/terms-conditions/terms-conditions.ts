import { Component, computed, inject } from '@angular/core';
import { LanguageService } from '../../../services/language.service';
import { Bilingual, LegalDoc, LegalDocument } from '../legal-doc';

const CONTENT: Bilingual<LegalDocument> = {
  es: {
    title: 'Términos y condiciones',
    updated: 'Última actualización: septiembre de 2026',
    sections: [
      {
        title: 'Objeto',
        paragraphs: ['Estas condiciones regulan el uso de PedidAI (el «Servicio»), una herramienta para comparar los precios de tus proveedores a partir de tus albaranes y enviarles pedidos, ofrecida por Enrique Pérez Sánchez (NIF 43444166H). Al registrarte aceptas estas condiciones.'],
      },
      {
        title: 'Cuenta',
        list: [
          'Debes dar datos veraces y mantener tus credenciales en secreto.',
          'Cada local tiene su propia cuenta. El administrador de la cuenta es responsable de los usuarios que invite.',
          'Para enviar pedidos a proveedores debes verificar tu email.',
        ],
      },
      {
        title: 'Prueba gratuita',
        list: [
          'Al registrarte tienes 14 días de prueba gratuita con acceso a todas las funciones, sin tarjeta de crédito.',
          'Al terminar la prueba, el acceso se suspende hasta que contrates el plan. No se cobra nada automáticamente.',
          'Si no contratas, conservamos tus datos 30 días por si quieres continuar y después los eliminamos.',
        ],
      },
      {
        title: 'Plan y precio',
        list: [
          'Precio: 39 €/mes + IVA por local.',
          'Precio de lanzamiento: 29 €/mes + IVA por local para quienes contraten durante el periodo de lanzamiento, que se mantiene mientras la suscripción siga activa sin interrupciones.',
          'Facturación mensual y sin permanencia: puedes dejarlo cuando quieras con efecto al final del mes en curso.',
          'Para contratar, escríbenos a hola@pedidai.es y te indicaremos la forma de pago.',
        ],
      },
      {
        title: 'Uso razonable',
        paragraphs: ['Para proteger el Servicio y su coste, se aplican límites razonables de uso (por ejemplo, número de documentos leídos o de pedidos enviados por día). Si los necesitas ampliar, escríbenos.'],
        list: [
          'No puedes usar el Servicio para fines ilegales, para enviar comunicaciones no solicitadas ni para intentar acceder a datos de otras empresas.',
          'No puedes sobrecargar, atacar ni intentar eludir las medidas de seguridad de la plataforma.',
        ],
      },
      {
        title: 'Inteligencia artificial y pedidos',
        list: [
          'La lectura de albaranes y la preparación de pedidos se hacen con inteligencia artificial y pueden contener errores. Revisa siempre los datos antes de guardarlos y los pedidos antes de enviarlos.',
          'Ningún pedido se envía a un proveedor sin que lo confirmes. Eres responsable de los pedidos que envías.',
          'Las comparativas y estimaciones de ahorro son orientativas: dependen de los albaranes que subas y de que los productos comparados sean equivalentes.',
        ],
      },
      {
        title: 'Tus datos',
        list: [
          'Los datos que introduces son tuyos. Puedes pedirnos una copia o su eliminación en cualquier momento.',
          'No vendemos tus datos ni los compartimos con otras empresas usuarias. El tratamiento se explica en la política de privacidad.',
        ],
      },
      {
        title: 'Responsabilidad',
        list: [
          'PedidAI es una herramienta de ayuda a la compra; las decisiones comerciales son tuyas.',
          'Hacemos lo razonable para que el Servicio esté disponible, pero puede haber interrupciones por mantenimiento o causas ajenas.',
          'Salvo dolo o culpa grave, nuestra responsabilidad se limita al importe que hayas pagado en los 3 meses anteriores al hecho que la origine.',
        ],
      },
      {
        title: 'Cambios',
        paragraphs: ['Si cambiamos estas condiciones de forma relevante te avisaremos con al menos 30 días de antelación. Si no estás de acuerdo, puedes darte de baja.'],
      },
      {
        title: 'Ley aplicable',
        paragraphs: ['Estas condiciones se rigen por la ley española. Para cualquier controversia son competentes los juzgados y tribunales de Barcelona, salvo que la ley disponga otra cosa.'],
      },
    ],
  },
  ca: {
    title: 'Termes i condicions',
    updated: 'Darrera actualització: setembre de 2026',
    sections: [
      {
        title: 'Objecte',
        paragraphs: ['Aquestes condicions regulen l’ús de PedidAI (el «Servei»), una eina per comparar els preus dels teus proveïdors a partir dels teus albarans i enviar-los comandes, oferta per Enrique Pérez Sánchez (NIF 43444166H). En registrar-te acceptes aquestes condicions.'],
      },
      {
        title: 'Compte',
        list: [
          'Has de donar dades veraces i mantenir les teves credencials en secret.',
          'Cada local té el seu propi compte. L’administrador del compte és responsable dels usuaris que convidi.',
          'Per enviar comandes a proveïdors has de verificar el teu email.',
        ],
      },
      {
        title: 'Prova gratuïta',
        list: [
          'En registrar-te tens 14 dies de prova gratuïta amb accés a totes les funcions, sense targeta de crèdit.',
          'En acabar la prova, l’accés se suspèn fins que contractis el pla. No es cobra res automàticament.',
          'Si no contractes, conservem les teves dades 30 dies per si vols continuar i després les eliminem.',
        ],
      },
      {
        title: 'Pla i preu',
        list: [
          'Preu: 39 €/mes + IVA per local.',
          'Preu de llançament: 29 €/mes + IVA per local per a qui contracti durant el període de llançament, que es manté mentre la subscripció continuï activa sense interrupcions.',
          'Facturació mensual i sense permanència: ho pots deixar quan vulguis amb efecte al final del mes en curs.',
          'Per contractar, escriu-nos a hola@pedidai.es i t’indicarem la forma de pagament.',
        ],
      },
      {
        title: 'Ús raonable',
        paragraphs: ['Per protegir el Servei i el seu cost, s’apliquen límits raonables d’ús (per exemple, nombre de documents llegits o de comandes enviades per dia). Si els necessites ampliar, escriu-nos.'],
        list: [
          'No pots fer servir el Servei per a finalitats il·legals, per enviar comunicacions no sol·licitades ni per intentar accedir a dades d’altres empreses.',
          'No pots sobrecarregar, atacar ni intentar eludir les mesures de seguretat de la plataforma.',
        ],
      },
      {
        title: 'Intel·ligència artificial i comandes',
        list: [
          'La lectura d’albarans i la preparació de comandes es fan amb intel·ligència artificial i poden contenir errors. Revisa sempre les dades abans de desar-les i les comandes abans d’enviar-les.',
          'Cap comanda s’envia a un proveïdor sense que la confirmis. Ets responsable de les comandes que envies.',
          'Les comparatives i estimacions d’estalvi són orientatives: depenen dels albarans que pugis i que els productes comparats siguin equivalents.',
        ],
      },
      {
        title: 'Les teves dades',
        list: [
          'Les dades que hi introdueixes són teves. Ens pots demanar una còpia o que les eliminem en qualsevol moment.',
          'No venem les teves dades ni les compartim amb altres empreses usuàries. El tractament s’explica a la política de privacitat.',
        ],
      },
      {
        title: 'Responsabilitat',
        list: [
          'PedidAI és una eina d’ajuda a la compra; les decisions comercials són teves.',
          'Fem el que és raonable perquè el Servei estigui disponible, però hi pot haver interrupcions per manteniment o causes alienes.',
          'Llevat de dol o culpa greu, la nostra responsabilitat es limita a l’import que hagis pagat els 3 mesos anteriors al fet que l’origini.',
        ],
      },
      {
        title: 'Canvis',
        paragraphs: ['Si canviem aquestes condicions de manera rellevant t’avisarem amb almenys 30 dies d’antelació. Si no hi estàs d’acord, pots donar-te de baixa.'],
      },
      {
        title: 'Llei aplicable',
        paragraphs: ['Aquestes condicions es regeixen per la llei espanyola. Per a qualsevol controvèrsia són competents els jutjats i tribunals de Barcelona, llevat que la llei disposi una altra cosa.'],
      },
    ],
  },
};

@Component({
  selector: 'app-terms-conditions',
  imports: [LegalDoc],
  template: `<app-legal-doc [doc]="doc()" />`,
})
export class TermsConditions {
  private language = inject(LanguageService);
  doc = computed(() => CONTENT[this.language.current()]);
}
