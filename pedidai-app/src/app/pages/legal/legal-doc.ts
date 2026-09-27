import { Component, input } from '@angular/core';

/** Bloque de un documento legal: párrafos, lista, tabla o ficha de datos. */
export interface LegalSection {
  title: string;
  paragraphs?: string[];
  list?: string[];
  facts?: [string, string][];
  table?: { head: string[]; rows: string[][] };
  note?: string;
}

export interface LegalDocument {
  title: string;
  updated: string;
  intro?: string;
  sections: LegalSection[];
}

export type Bilingual<T> = { es: T; ca: T };

/** Presentación común de las páginas legales (el texto viene de cada página, en castellano o catalán). */
@Component({
  selector: 'app-legal-doc',
  template: `
    <div class="min-h-screen bg-white text-gray-700 pt-24 pb-20 px-4">
      <article class="max-w-3xl mx-auto">
        <header class="mb-10">
          <h1 class="text-3xl sm:text-4xl font-extrabold text-gray-900 mb-3">{{ doc().title }}</h1>
          <p class="text-gray-500 text-sm">{{ doc().updated }}</p>
          @if (doc().intro) {
            <p class="mt-4 leading-relaxed">{{ doc().intro }}</p>
          }
        </header>
        <div class="space-y-8 text-[15px] leading-relaxed">
          @for (s of doc().sections; track $index; let i = $index) {
            <section>
              <h2 class="text-lg font-bold text-gray-900 mb-3">{{ i + 1 }}. {{ s.title }}</h2>
              @if (s.facts) {
                <dl class="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-1 mb-3">
                  @for (f of s.facts; track $index) {
                    <div class="flex flex-col sm:flex-row sm:gap-2"><dt class="text-gray-500 sm:w-40 shrink-0">{{ f[0] }}</dt><dd class="text-gray-900 break-words">{{ f[1] }}</dd></div>
                  }
                </dl>
              }
              @for (p of s.paragraphs ?? []; track $index) {
                <p class="mb-3">{{ p }}</p>
              }
              @if (s.list) {
                <ul class="list-disc pl-5 space-y-2 mb-3">
                  @for (item of s.list; track $index) { <li>{{ item }}</li> }
                </ul>
              }
              @if (s.table) {
                <div class="overflow-x-auto mb-3">
                  <table class="w-full text-sm border border-slate-200 rounded-lg">
                    <thead class="bg-slate-50">
                      <tr>@for (h of s.table.head; track $index) { <th class="text-left px-3 py-2 font-semibold text-gray-800">{{ h }}</th> }</tr>
                    </thead>
                    <tbody>
                      @for (row of s.table.rows; track $index) {
                        <tr class="border-t border-slate-200">@for (c of row; track $index) { <td class="px-3 py-2 align-top">{{ c }}</td> }</tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
              @if (s.note) {
                <p class="text-sm bg-amber-50 border border-amber-200 rounded-lg p-3 text-amber-900">{{ s.note }}</p>
              }
            </section>
          }
        </div>
      </article>
    </div>
  `,
})
export class LegalDoc {
  doc = input.required<LegalDocument>();
}
