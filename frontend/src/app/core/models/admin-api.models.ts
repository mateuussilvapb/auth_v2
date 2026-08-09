// Espelham os DTOs de com.mssousa.authserver.adapter.in.web.admin (backend, Fase 8/9).

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TenantResponse {
  id: number;
  code: string;
  name: string;
  status: string;
  logoUrl: string | null;
}

export interface CreateTenantRequest {
  code: string;
  name: string;
}

export interface UpdateTenantRequest {
  name: string;
}

export interface UpdateStatusRequest {
  status: string;
}
