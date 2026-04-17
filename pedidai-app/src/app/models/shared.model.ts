export interface PageableInfo {
  page: number;
  size: number;
  sort: string;
  totalPages: number;
  totalElements: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  pageable: PageableInfo;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
