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

export interface SystemResponse {
  id: number;
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

export interface SystemProfileResponse {
  id: number;
  systemId: number;
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
  id: number;
  tenantId: number;
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
  id: number;
  userId: number;
  systemId: number;
  tenantId: number;
  status: string;
}

export interface BindSystemRequest {
  systemId: number;
}

export interface UserSystemProfileResponse {
  id: number;
  userSystemId: number;
  systemProfileId: number;
  status: string;
}

export interface BindProfileRequest {
  profileId: number;
}
