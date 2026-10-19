import { Pipe, PipeTransform } from '@angular/core';

export interface TextSegment {
  text: string;
  bold: boolean;
}

/**
 * Parte un texto en tramos normales y en negrita según los «**…**» que escribe el asistente,
 * para pintarlos con <strong> sin usar innerHTML.
 */
@Pipe({ name: 'bold' })
export class BoldPipe implements PipeTransform {
  transform(value: string | null | undefined): TextSegment[] {
    return (value ?? '').split(/\*\*(.+?)\*\*/s).map((text, i) => ({ text, bold: i % 2 === 1 })).filter(s => s.text);
  }
}
