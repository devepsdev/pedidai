export interface DashboardResponse {
  totalComandes: number;
  despesaComandes: number;
  comandesPendents: number;
}

export interface SupplierReport {
  proveidor: string;
  numComandes: number;
  despesaTotal: number;
  percentatge: number;
}

export interface ProductReport {
  nomProducte: string;
  quantitatTotal: number;
  despesaTotal: number;
}

export interface GlobalReport {
  dataInicial: string;
  dataFinal: string;
  totalComandes: number;
  despesaTotal: number;
  comandaMitjana: number;
  despesaProveidors: SupplierReport[];
  topProductes: ProductReport[];
}
