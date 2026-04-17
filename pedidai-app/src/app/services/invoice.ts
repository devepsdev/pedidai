import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api';
import { ApiResponse } from '../models/shared.model';
import { InvoiceConfirmRequestDTO, InvoiceScanResultDTO } from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private api = inject(ApiService);

  scanInvoice(image: File, supplierUuid?: string): Observable<ApiResponse<InvoiceScanResultDTO>> {
    const formData = new FormData();
    formData.append('image', image);
    if (supplierUuid) {
      formData.append('supplierUuid', supplierUuid);
    }
    return this.api.postFile<ApiResponse<InvoiceScanResultDTO>>('/invoices/scan', formData);
  }

  confirmInvoice(request: InvoiceConfirmRequestDTO): Observable<ApiResponse<unknown>> {
    return this.api.post<ApiResponse<unknown>>('/invoices/confirm', request);
  }
}
