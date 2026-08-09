import { Injectable } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';

import { environment } from '../../../environments/environment';

/**
 * Cliente OAuth2 PKCE do console administrativo Angular (seção 2.2/D6 do plano, Fase 9) —
 * autentica platform admins contra o {@code RegisteredClient} estático de
 * {@code RegisteredClientRepositoryConfig} (backend), não vinculado a nenhum tenant.
 * <p>
 * {@code issuer} é a própria origem do backend (o Authorization Server expõe
 * {@code /.well-known/openid-configuration} via {@code authorizationServerConfigurer.oidc(...)},
 * seção 6 do plano) — reaproveita {@code environment.apiBaseUrl} em vez de configuração
 * separada. O redirect URI aponta para {@code /console/callback} na própria origem do
 * Angular, batendo com {@code authserver.console-client.redirect-uris}.
 * </p>
 */
@Injectable({ providedIn: 'root' })
export class ConsoleAuthService {
  private configured = false;

  constructor(private readonly oauthService: OAuthService) {}

  private configure(): void {
    if (this.configured) {
      return;
    }

    const config: AuthConfig = {
      issuer: environment.apiBaseUrl || window.location.origin,
      redirectUri: `${window.location.origin}/console/callback`,
      clientId: environment.consoleClientId,
      responseType: 'code',
      scope: 'profile',
      requireHttps: environment.production,
      strictDiscoveryDocumentValidation: false,
      // Sem "openid" no scope (seção 7.2 — o projeto não usa OIDC id_token, só o access
      // token JWT já carrega os claims necessários via JwtTokenCustomizer), então não há
      // id_token para validar/verificar nonce.
      oidc: false,
    };

    this.oauthService.configure(config);
    this.configured = true;
  }

  async ensureDiscoveryLoaded(): Promise<void> {
    this.configure();
    if (!this.oauthService.discoveryDocumentLoaded) {
      await this.oauthService.loadDiscoveryDocument();
    }
  }

  async completeLoginFlow(): Promise<void> {
    await this.ensureDiscoveryLoaded();
    await this.oauthService.tryLoginCodeFlow();
  }

  async login(): Promise<void> {
    await this.ensureDiscoveryLoaded();
    this.oauthService.initCodeFlow();
  }

  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  getAccessToken(): string {
    return this.oauthService.getAccessToken();
  }
}
