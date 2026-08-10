import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';

import { ConsoleAuthService } from './console-auth.service';

describe('ConsoleAuthService', () => {
  let service: ConsoleAuthService;
  let oauthServiceStub: {
    configure: jasmine.Spy;
    loadDiscoveryDocument: jasmine.Spy;
    tryLoginCodeFlow: jasmine.Spy;
    initCodeFlow: jasmine.Spy;
    hasValidAccessToken: jasmine.Spy;
    logOut: jasmine.Spy;
    getAccessToken: jasmine.Spy;
    discoveryDocumentLoaded: boolean;
  };

  beforeEach(() => {
    oauthServiceStub = {
      configure: jasmine.createSpy('configure'),
      loadDiscoveryDocument: jasmine.createSpy('loadDiscoveryDocument').and.resolveTo(undefined),
      tryLoginCodeFlow: jasmine.createSpy('tryLoginCodeFlow').and.resolveTo(undefined),
      initCodeFlow: jasmine.createSpy('initCodeFlow'),
      hasValidAccessToken: jasmine.createSpy('hasValidAccessToken').and.returnValue(false),
      logOut: jasmine.createSpy('logOut'),
      getAccessToken: jasmine.createSpy('getAccessToken').and.returnValue('token'),
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
