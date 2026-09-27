import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';
import { PriceOverview } from '../models/price.model';

@Injectable({ providedIn: 'root' })
export class PriceService {
  private api = inject(ApiService);

  /** Comparativa de proveedores, subidas de precio y sobrecoste de los últimos días. */
  overview(days = 90): Observable<PriceOverview> {
    return this.api.get<ApiResponse<PriceOverview>>('/prices/overview', { days }).pipe(map(r => r.data));
  }
}
