import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { ConsoleAuthService } from './console-auth.service';
import { AuthApiService } from './auth-api.service';

describe('ConsoleAuthService', () => {
  let service: ConsoleAuthService;
  let oauthServiceStub: {
    configure: Mock;
    loadDiscoveryDocument: Mock;
    tryLoginCodeFlow: Mock;
    initCodeFlow: Mock;
    hasValidAccessToken: Mock;
    logOut: Mock;
    getAccessToken: Mock;
    discoveryDocumentLoaded: boolean;
  };
  let authApiStub: { logout: Mock };

  beforeEach(() => {
    oauthServiceStub = {
      configure: vi.fn(),
      loadDiscoveryDocument: vi.fn().mockResolvedValue(undefined),
      tryLoginCodeFlow: vi.fn().mockResolvedValue(undefined),
      initCodeFlow: vi.fn(),
      hasValidAccessToken: vi.fn().mockReturnValue(false),
      logOut: vi.fn(),
      getAccessToken: vi.fn().mockReturnValue('token'),
      discoveryDocumentLoaded: false,
    };
    authApiStub = { logout: vi.fn().mockReturnValue(of(undefined)) };

    TestBed.configureTestingModule({
      providers: [
        { provide: OAuthService, useValue: oauthServiceStub },
        { provide: AuthApiService, useValue: authApiStub },
      ],
    });

    service = TestBed.inject(ConsoleAuthService);
  });

  it('deve configurar o OAuthService apenas uma vez ao logar', async () => {
    await service.login();
    await service.login();

    expect(oauthServiceStub.configure).toHaveBeenCalledTimes(1);
    expect(oauthServiceStub.initCodeFlow).toHaveBeenCalledTimes(2);
  });

  it('deve carregar o discovery document quando ainda não carregado', async () => {
    await service.login();
    expect(oauthServiceStub.loadDiscoveryDocument).toHaveBeenCalled();
  });

  it('deve delegar isAuthenticated/getAccessToken ao OAuthService', () => {
    service.getAccessToken();
    service.isAuthenticated();

    expect(oauthServiceStub.getAccessToken).toHaveBeenCalled();
    expect(oauthServiceStub.hasValidAccessToken).toHaveBeenCalled();
  });

  it('logout invalida a sessão no backend e limpa os tokens locais sem redirecionar ao end_session_endpoint', async () => {
    await service.logout();

    expect(authApiStub.logout).toHaveBeenCalled();
    expect(oauthServiceStub.logOut).toHaveBeenCalledWith(true);
  });

  it('logout limpa os tokens locais mesmo se a chamada ao backend falhar', async () => {
    authApiStub.logout.mockReturnValue(throwError(() => new Error('rede fora')));

    await service.logout();

    expect(oauthServiceStub.logOut).toHaveBeenCalledWith(true);
  });
});
