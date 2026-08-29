import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  BrandingResponse,
  ConsentRequest,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
} from '../models/auth-api.models';

/**
 * Cliente para /api/auth/** (Fase 7/8 do plano) — endpoints públicos baseados em sessão,
 * consumidos pelas telas de login/consentimento/esqueci-senha. `withCredentials: true` em
 * toda chamada: o cookie de sessão HttpOnly (seção 7.4) precisa ir e voltar para que
 * GET /oauth2/authorize, na sequência, veja a mesma sessão autenticada.
 */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  constructor(private readonly http: HttpClient) {}

  branding(clientId: string): Observable<BrandingResponse> {
    return this.http.get<BrandingResponse>(`${this.baseUrl}/branding`, {
      params: { clientId },
      withCredentials: true,
    });
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request, { withCredentials: true });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/forgot-password`, request, { withCredentials: true });
  }

  resetPassword(request: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/reset-password`, request, { withCredentials: true });
  }

  consent(request: ConsentRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/consent`, request, { withCredentials: true });
  }

  /**
   * Invalida a sessão HTTP no backend — sem isso, limpar só os tokens OAuth localmente
   * (client sem `openid`/`id_token`, seção 7.2) deixa a sessão viva, e o próximo
   * `GET /oauth2/authorize` reautentica silenciosamente sem pedir login.
   */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}, { withCredentials: true });
  }
}
