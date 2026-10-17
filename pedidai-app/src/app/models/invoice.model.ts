/**
 * INVOICE: albarán o factura (precios pagados) · PRICE_LIST: tarifa o presupuesto (precios ofrecidos).
 * AUTO solo al leer: la IA decide cuál de los dos es y lo devuelve en documentKind.
 */
export type DocumentKind = 'INVOICE' | 'PRICE_LIST' | 'AUTO';

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

/** Pedido enviado al proveedor que probablemente sirve el albarán, para comparar lo servido con lo pedido. */
export interface InvoiceOrderDTO {
  uuid: string;
  name: string;
  status: string;
  date: string;
  items: { productUuid: string; productName: string; quantity?: number; unit?: string; unitPrice?: number }[];
}

export interface InvoiceScanResultDTO {
  success: boolean;
  detectedSupplierName?: string;
  detectedSupplierCif?: string;
  detectedSupplierPhone?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  documentKind?: DocumentKind;
  validUntil?: string;
  /** El documento era muy largo y solo se ha leído una parte. */
  truncated?: boolean;
  matchedOrder?: InvoiceOrderDTO;
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
  documentKind?: DocumentKind;
  validUntil?: string;
  products: InvoiceProductConfirmDTO[];
}
