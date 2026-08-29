import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { ForgotPassword } from './forgot-password';

describe('ForgotPassword', () => {
  let component: ForgotPassword;
  let fixture: ComponentFixture<ForgotPassword>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ client_id: 'CRM_ACME' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPassword);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
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

  function respondSuccess(req: TestRequest): void {
    req.flush(null);
  }

  function respondNotFound(req: TestRequest): void {
    req.flush('erro', { status: 404, statusText: 'Not Found' });
  }

  it.each([
    ['usuário existente', respondSuccess],
    ['usuário inexistente', respondNotFound],
  ])(
    'exibe a mesma mensagem de sucesso para %s — a API não diferencia, a UI também não pode (guia 6.2)',
    (_cenario, respond) => {
      component.form.setValue({ usernameOrEmail: 'alguem@example.com' });
      component.submit();

      const req = httpMock.expectOne((r) => r.url.endsWith('/api/auth/forgot-password'));
      respond(req);

      expect(component.submitted()).toBe(true);

      fixture.detectChanges();
      const text = fixture.nativeElement.textContent as string;
      expect(text).toContain('Se o e-mail estiver cadastrado, enviaremos as instruções.');
    },
  );
});
