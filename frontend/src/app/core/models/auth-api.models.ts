// Espelham os DTOs de com.mssousa.authserver.adapter.in.web.auth (backend, Fase 7/8).

export interface BrandingResponse {
  tenantName: string;
  logoUrl: string | null;
}

export interface LoginRequest {
  clientId: string;
  usernameOrEmail: string;
  password: string;
}

export interface LoginResponse {
  username: string;
  name: string;
}

export interface ForgotPasswordRequest {
  clientId: string;
  usernameOrEmail: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ConsentRequest {
  clientId: string;
  scopes: string[];
}

export interface ApiErrorResponse {
  message: string;
  fieldErrors?: Record<string, string> | null;
}
