import { Pipe, PipeTransform } from '@angular/core';

/** 1234.5 → "1,23 km"; 850 → "850 m" */
@Pipe({ name: 'distancia' })
export class DistanciaPipe implements PipeTransform {
  transform(metros: number | null | undefined): string {
    if (metros == null) return '-';
    return metros >= 1000 ? `${(metros / 1000).toFixed(2).replace('.', ',')} km` : `${Math.round(metros)} m`;
  }
}

/** 754 → "12 min 34 s" */
@Pipe({ name: 'duracao' })
export class DuracaoPipe implements PipeTransform {
  transform(seg: number | null | undefined): string {
    if (seg == null) return '-';
    const h = Math.floor(seg / 3600), m = Math.floor((seg % 3600) / 60), s = Math.floor(seg % 60);
    return h ? `${h} h ${m} min` : m ? `${m} min ${s} s` : `${s} s`;
  }
}
