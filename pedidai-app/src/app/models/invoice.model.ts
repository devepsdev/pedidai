/** NEW: producto nuevo · CHANGED: precio distinto del último conocido · SAME: mismo precio. */
export type InvoiceLineStatus = 'NEW' | 'CHANGED' | 'SAME';

export interface InvoiceProductDTO {
  name: string;
  genericName?: string;
  quantity?: number;
  unit?: string;
  unitPrice?: number;
  ivaPercent?: number;
  subtotal?: number;
  status: InvoiceLineStatus;
  matchedProductUuid?: string;
  previousPrice?: number;
  priceChangePercent?: number;
}

export interface InvoiceScanResultDTO {
  success: boolean;
  detectedSupplierName?: string;
  detectedSupplierCif?: string;
  detectedSupplierPhone?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  products: InvoiceProductDTO[];
  totalAmount?: number;
  totalIva?: number;
  productsCreated: number;
  productsUpdated: number;
  productsSkipped: number;
  pricesRecorded?: number;
  priceIncreases?: number;
  matchedSupplierUuid?: string;
  supplierAutoMatched: boolean;
}

export interface InvoiceProductConfirmDTO {
  name: string;
  genericName?: string;
  quantity?: number;
  unit?: string;
  unitPrice: number;
  matchedProductUuid?: string;
}

export interface InvoiceConfirmRequestDTO {
  supplierUuid?: string;
  newSupplier?: { name: string; email?: string; phone?: string };
  invoiceNumber?: string;
  invoiceDate?: string;
  products: InvoiceProductConfirmDTO[];
}
