// Espelham os DTOs de com.mssousa.authserver.adapter.in.web.admin (backend, Fase 8/9).
//
// Todo campo de ID (TSID, seção 4.2 do plano) é `string`, não `number`: TSIDs regularmente
// excedem Number.MAX_SAFE_INTEGER do JavaScript (2^53-1) — como number, o JSON.parse do
// browser perde precisão ao decodificar (confirmado num teste manual: um ID virou outro ID
// ao dar round-trip pelo backend), quebrando qualquer operação subsequente que dependa do
// ID exato. O backend serializa esses campos como String pelo mesmo motivo — ver Notas de
// PROGRESS.md sobre a Fase 9.

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TenantResponse {
  id: string;
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

export interface SystemResponse {
  id: string;
  clientId: string;
  name: string;
  status: string;
  publicClient: boolean;
  redirectUris: string[];
  thirdParty: boolean;
}

export interface CreateSystemRequest {
  clientId: string;
  name: string;
  publicClient: boolean;
  clientSecret: string | null;
  initialRedirectUris: string[];
  thirdParty: boolean;
}

export interface UpdateSystemRequest {
  name: string;
}

export interface RedirectUriRequest {
  uri: string;
}

export interface RotateSecretRequest {
  newSecret: string;
}

export interface PlatformAdminResponse {
  id: string;
  username: string;
  email: string;
  name: string;
  status: string;
}

export interface CreatePlatformAdminRequest {
  username: string;
  email: string;
  password: string;
  name: string;
}

export interface SystemProfileResponse {
  id: string;
  systemId: string;
  code: string;
  description: string | null;
  status: string;
}

export interface CreateSystemProfileRequest {
  code: string;
  description: string | null;
}

export interface UpdateSystemProfileRequest {
  description: string | null;
}

export interface UserResponse {
  id: string;
  tenantId: string;
  username: string;
  email: string;
  name: string;
  status: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  name: string;
}

export interface UpdateUserRequest {
  name: string;
  email: string;
}

export interface UserSystemResponse {
  id: string;
  userId: string;
  systemId: string;
  tenantId: string;
  status: string;
}

export interface BindSystemRequest {
  systemId: string;
}

export interface UserSystemProfileResponse {
  id: string;
  userSystemId: string;
  systemProfileId: string;
  status: string;
}

export interface BindProfileRequest {
  profileId: string;
}
