import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConsoleAuthService } from './console-auth.service';
import {
  CreateTenantRequest,
  Page,
  TenantResponse,
  UpdateStatusRequest,
  UpdateTenantRequest,
} from '../models/admin-api.models';

/**
 * Cliente para /admin/api/v1/** (seção 9 do plano) — console administrativo, protegido por
 * token de platform admin (ConsoleAuthService/consoleAuthGuard). Diferente de
 * AuthApiService: bearer token, não cookie de sessão.
 */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/api/v1`;

  constructor(
    private readonly http: HttpClient,
    private readonly consoleAuth: ConsoleAuthService,
  ) {}

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.consoleAuth.getAccessToken()}` });
  }

  listTenants(page: number, size: number): Observable<Page<TenantResponse>> {
    return this.http.get<Page<TenantResponse>>(`${this.baseUrl}/tenants`, {
      headers: this.authHeaders(),
      params: { page, size },
    });
  }

  getTenant(id: number): Observable<TenantResponse> {
    return this.http.get<TenantResponse>(`${this.baseUrl}/tenants/${id}`, { headers: this.authHeaders() });
  }

  createTenant(request: CreateTenantRequest): Observable<TenantResponse> {
    return this.http.post<TenantResponse>(`${this.baseUrl}/tenants`, request, { headers: this.authHeaders() });
  }

  updateTenant(id: number, request: UpdateTenantRequest): Observable<TenantResponse> {
    return this.http.put<TenantResponse>(`${this.baseUrl}/tenants/${id}`, request, { headers: this.authHeaders() });
  }

  updateTenantStatus(id: number, request: UpdateStatusRequest): Observable<TenantResponse> {
    return this.http.patch<TenantResponse>(`${this.baseUrl}/tenants/${id}/status`, request, {
      headers: this.authHeaders(),
    });
  }
}
