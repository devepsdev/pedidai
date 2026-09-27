/** Precio vigente de un producto en un proveedor. */
export interface PriceOffer {
  productUuid: string;
  productName: string;
  supplierUuid: string;
  supplierName: string;
  supplierHasEmail: boolean;
  unit: string;
  latestPrice: number;
  latestDate: string;
  previousPrice?: number;
  changePercent?: number;
  cheapest: boolean;
}

/** Un mismo producto (nombre genérico + unidad) en uno o varios proveedores. */
export interface PriceGroup {
  key: string;
  name: string;
  unit: string;
  offers: PriceOffer[];
  cheapestPrice: number;
  highestPrice: number;
  spreadPercent?: number;
}

export interface PriceAlert {
  productUuid: string;
  productName: string;
  supplierName: string;
  unit: string;
  previousPrice: number;
  previousDate: string;
  latestPrice: number;
  latestDate: string;
  changePercent: number;
}

export interface PriceOverview {
  days: number;
  observations: number;
  productsTracked: number;
  suppliersTracked: number;
  comparable: PriceGroup[];
  singleSupplier: PriceGroup[];
  alerts: PriceAlert[];
  overpaidAmount: number;
  overpaidLines: number;
}
