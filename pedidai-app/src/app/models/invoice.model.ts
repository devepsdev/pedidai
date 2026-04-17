export type InvoiceProductAction = 'CREATED' | 'UPDATED' | 'SKIPPED';

export interface InvoiceProductDTO {
  name: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  ivaPercent: number;
  subtotal: number;
  action: InvoiceProductAction;
  matchedProductUuid?: string;
  previousPrice?: number;
}

export interface InvoiceScanResultDTO {
  success: boolean;
  message: string;
  detectedSupplierName?: string;
  detectedSupplierCif?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  products: InvoiceProductDTO[];
  totalAmount?: number;
  totalIva?: number;
  productsCreated: number;
  productsUpdated: number;
  productsSkipped: number;
  matchedSupplierUuid?: string;
  supplierAutoMatched: boolean;
}

export interface InvoiceProductConfirmDTO {
  name: string;
  matchedProductUuid?: string;
  unitPrice: number;
  action: InvoiceProductAction;
}

export interface InvoiceConfirmRequestDTO {
  supplierUuid: string;
  products: InvoiceProductConfirmDTO[];
}
