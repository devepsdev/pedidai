import { Pipe, PipeTransform } from '@angular/core';

/**
 * Parte un texto traducido en líneas por los saltos «\n» de los ficheros i18n.
 * Cada línea es una frase completa: en pantallas grandes se pinta en su propia línea
 * (`lg:block`) y en móvil fluye como un párrafo normal.
 */
@Pipe({ name: 'lines' })
export class LinesPipe implements PipeTransform {
  transform(value: string | null | undefined): string[] {
    return (value ?? '').split('\n').map(line => line.trim()).filter(Boolean);
  }
}
