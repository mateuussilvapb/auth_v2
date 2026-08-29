import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Consent } from './consent';

describe('Consent', () => {
  let component: Consent;
  let fixture: ComponentFixture<Consent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Consent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ client_id: 'CRM_ACME', scope: 'profile', state: 'xyz' }),
            },
            queryParamMap: of(convertToParamMap({ client_id: 'CRM_ACME', scope: 'profile', state: 'xyz' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Consent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deve capturar client_id e separar scopes da query string', () => {
    expect(component.clientId).toBe('CRM_ACME');
    expect(component.scopes).toEqual(['profile']);
  });

  it('deny() sinaliza negado sem chamar POST /api/auth/consent', () => {
    component.deny();

    expect(component.denied()).toBe(true);
  });
});

describe('Consent sem client_id/scope na query string', () => {
  let component: Consent;
  let fixture: ComponentFixture<Consent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Consent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}) },
            queryParamMap: of(convertToParamMap({})),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Consent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve sinalizar erro em vez de tentar renderizar o consentimento', () => {
    expect(component.clientId).toBe('');
    expect(component.errorMessage()).toContain('inválido');
  });
});
