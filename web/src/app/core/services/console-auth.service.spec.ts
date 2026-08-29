import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import type { Mock } from 'vitest';

import { ConsoleAuthService } from './console-auth.service';

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

    TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: oauthServiceStub }],
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

  it('deve delegar isAuthenticated/logout/getAccessToken ao OAuthService', () => {
    service.logout();
    service.getAccessToken();
    service.isAuthenticated();

    expect(oauthServiceStub.logOut).toHaveBeenCalled();
    expect(oauthServiceStub.getAccessToken).toHaveBeenCalled();
    expect(oauthServiceStub.hasValidAccessToken).toHaveBeenCalled();
  });
});
