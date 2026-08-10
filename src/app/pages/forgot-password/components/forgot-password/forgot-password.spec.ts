import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
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

  it('deve marcar submitted mesmo quando a API retorna erro (evita enumeração de contas)', () => {
    component.usernameOrEmail = 'usuario_inexistente';
    component.submit();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/auth/forgot-password'));
    req.flush('erro', { status: 404, statusText: 'Not Found' });

    expect(component.submitted()).toBe(true);
  });
});
