import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConsoleAuthService } from './console-auth.service';
import {
  BindProfileRequest,
  BindSystemRequest,
  CreateSystemProfileRequest,
  CreateSystemRequest,
  CreateTenantRequest,
  CreateUserRequest,
  Page,
  RedirectUriRequest,
  SystemProfileResponse,
  SystemResponse,
  TenantResponse,
  UpdateStatusRequest,
  UpdateSystemProfileRequest,
  UpdateSystemRequest,
  UpdateTenantRequest,
  UpdateUserRequest,
  UserResponse,
  UserSystemProfileResponse,
  UserSystemResponse,
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

  listSystemsByTenant(tenantId: number, page: number, size: number): Observable<Page<SystemResponse>> {
    return this.http.get<Page<SystemResponse>>(`${this.baseUrl}/tenants/${tenantId}/systems`, {
      headers: this.authHeaders(),
      params: { page, size },
    });
  }

  createSystem(tenantId: number, request: CreateSystemRequest): Observable<SystemResponse> {
    return this.http.post<SystemResponse>(`${this.baseUrl}/tenants/${tenantId}/systems`, request, {
      headers: this.authHeaders(),
    });
  }

  updateSystem(id: number, request: UpdateSystemRequest): Observable<SystemResponse> {
    return this.http.put<SystemResponse>(`${this.baseUrl}/systems/${id}`, request, { headers: this.authHeaders() });
  }

  updateSystemStatus(id: number, request: UpdateStatusRequest): Observable<SystemResponse> {
    return this.http.patch<SystemResponse>(`${this.baseUrl}/systems/${id}/status`, request, {
      headers: this.authHeaders(),
    });
  }

  addRedirectUri(id: number, request: RedirectUriRequest): Observable<SystemResponse> {
    return this.http.post<SystemResponse>(`${this.baseUrl}/systems/${id}/redirect-uris`, request, {
      headers: this.authHeaders(),
    });
  }

  listProfiles(systemId: number): Observable<SystemProfileResponse[]> {
    return this.http.get<SystemProfileResponse[]>(`${this.baseUrl}/systems/${systemId}/profiles`, {
      headers: this.authHeaders(),
    });
  }

  createProfile(systemId: number, request: CreateSystemProfileRequest): Observable<SystemProfileResponse> {
    return this.http.post<SystemProfileResponse>(`${this.baseUrl}/systems/${systemId}/profiles`, request, {
      headers: this.authHeaders(),
    });
  }

  updateProfile(
    systemId: number,
    id: number,
    request: UpdateSystemProfileRequest,
  ): Observable<SystemProfileResponse> {
    return this.http.put<SystemProfileResponse>(`${this.baseUrl}/systems/${systemId}/profiles/${id}`, request, {
      headers: this.authHeaders(),
    });
  }

  updateProfileStatus(systemId: number, id: number, request: UpdateStatusRequest): Observable<SystemProfileResponse> {
    return this.http.patch<SystemProfileResponse>(
      `${this.baseUrl}/systems/${systemId}/profiles/${id}/status`,
      request,
      { headers: this.authHeaders() },
    );
  }

  listUsers(tenantId: number, page: number, size: number): Observable<Page<UserResponse>> {
    return this.http.get<Page<UserResponse>>(`${this.baseUrl}/tenants/${tenantId}/users`, {
      headers: this.authHeaders(),
      params: { page, size },
    });
  }

  getUser(tenantId: number, id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/tenants/${tenantId}/users/${id}`, {
      headers: this.authHeaders(),
    });
  }

  createUser(tenantId: number, request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/tenants/${tenantId}/users`, request, {
      headers: this.authHeaders(),
    });
  }

  updateUser(tenantId: number, id: number, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/tenants/${tenantId}/users/${id}`, request, {
      headers: this.authHeaders(),
    });
  }

  updateUserStatus(tenantId: number, id: number, request: UpdateStatusRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/tenants/${tenantId}/users/${id}/status`, request, {
      headers: this.authHeaders(),
    });
  }

  resetUserPassword(tenantId: number, id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/tenants/${tenantId}/users/${id}/reset-password`, null, {
      headers: this.authHeaders(),
    });
  }

  listUserSystems(tenantId: number, userId: number, page: number, size: number): Observable<Page<UserSystemResponse>> {
    return this.http.get<Page<UserSystemResponse>>(`${this.baseUrl}/tenants/${tenantId}/users/${userId}/systems`, {
      headers: this.authHeaders(),
      params: { page, size },
    });
  }

  bindUserToSystem(tenantId: number, userId: number, request: BindSystemRequest): Observable<UserSystemResponse> {
    return this.http.post<UserSystemResponse>(`${this.baseUrl}/tenants/${tenantId}/users/${userId}/systems`, request, {
      headers: this.authHeaders(),
    });
  }

  updateUserSystemStatus(
    tenantId: number,
    id: number,
    request: UpdateStatusRequest,
  ): Observable<UserSystemResponse> {
    return this.http.patch<UserSystemResponse>(`${this.baseUrl}/tenants/${tenantId}/user-systems/${id}/status`, request, {
      headers: this.authHeaders(),
    });
  }

  listUserSystemProfiles(tenantId: number, userSystemId: number): Observable<UserSystemProfileResponse[]> {
    return this.http.get<UserSystemProfileResponse[]>(
      `${this.baseUrl}/tenants/${tenantId}/user-systems/${userSystemId}/profiles`,
      { headers: this.authHeaders() },
    );
  }

  bindProfileToUserSystem(
    tenantId: number,
    userSystemId: number,
    request: BindProfileRequest,
  ): Observable<UserSystemProfileResponse> {
    return this.http.post<UserSystemProfileResponse>(
      `${this.baseUrl}/tenants/${tenantId}/user-systems/${userSystemId}/profiles`,
      request,
      { headers: this.authHeaders() },
    );
  }

  updateUserSystemProfileStatus(
    tenantId: number,
    userSystemId: number,
    id: number,
    request: UpdateStatusRequest,
  ): Observable<UserSystemProfileResponse> {
    return this.http.patch<UserSystemProfileResponse>(
      `${this.baseUrl}/tenants/${tenantId}/user-systems/${userSystemId}/profiles/${id}/status`,
      request,
      { headers: this.authHeaders() },
    );
  }
}
