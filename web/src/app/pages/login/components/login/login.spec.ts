import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Login } from './login';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({ client_id: 'CRM_ACME' }) },
            queryParamMap: of(convertToParamMap({ client_id: 'CRM_ACME' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url.endsWith('/api/auth/branding')).flush({ tenantName: 'Acme', logoUrl: null });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deve capturar client_id da query string', () => {
    expect(component.clientId).toBe('CRM_ACME');
  });

  it.each([
    ['usuário inexistente', 'Usuário ou senha inválidos.'],
    ['senha incorreta', 'Usuário ou senha inválidos.'],
  ])(
    'exibe a mesma mensagem genérica do backend para %s, sem diferenciar a causa (guia 6.1)',
    async (_cenario, mensagemBackend) => {
      component.form.setValue({ usernameOrEmail: 'alguem', password: 'errada' });
      await component.submit();

      const req = httpMock.expectOne((r) => r.url.endsWith('/api/auth/login'));
      req.flush({ message: mensagemBackend }, { status: 401, statusText: 'Unauthorized' });

      expect(component.errorMessage()).toBe(mensagemBackend);
    },
  );

  it('não decide foco nem ícone diferente com base no tipo de erro — só exibe o texto recebido', async () => {
    component.form.setValue({ usernameOrEmail: 'alguem', password: 'errada' });
    await component.submit();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/auth/login'));
    req.flush({ message: 'Usuário ou senha inválidos.' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('[role="alert"]');
    expect(errorEl.textContent).toContain('Usuário ou senha inválidos.');
  });
});
