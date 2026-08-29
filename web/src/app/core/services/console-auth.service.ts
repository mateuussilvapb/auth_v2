import { Injectable, inject } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthApiService } from './auth-api.service';

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
  private readonly authApi = inject(AuthApiService);

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
      // 'remoteOnly' em vez de environment.production: exige HTTPS para qualquer domínio
      // real (produção sempre é TLS via nginx/Let's Encrypt, seção 11), mas permite
      // http://localhost — necessário para testar o build de produção localmente (nginx
      // servindo dist/, sem certificado) sem enfraquecer a checagem em produção de verdade.
      requireHttps: 'remoteOnly',
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

  async logout(): Promise<void> {
    // Sem isso, a sessão HTTP do backend continuaria válida e o próximo
    // GET /oauth2/authorize reautenticaria silenciosamente (bug real, encontrado em
    // 2026-08-29) — limpar só os tokens OAuth locais não desloga de verdade. Best-effort:
    // segue limpando os tokens locais mesmo se a chamada falhar (rede fora, por exemplo).
    try {
      await firstValueFrom(this.authApi.logout());
    } catch {
      // Segue para limpar o estado local de qualquer forma — ver comentário acima.
    }

    // Logout local (`true` = noRedirectToLogoutUrl): o client não usa "openid" no scope
    // (comentário da config acima), então nunca há id_token. O `end_session_endpoint` do
    // backend (RP-initiated logout) exige `id_token_hint` e responde 400 sem ele —
    // redirecionar para lá quebraria o logout.
    this.oauthService.logOut(true);
  }

  getAccessToken(): string {
    return this.oauthService.getAccessToken();
  }
}
