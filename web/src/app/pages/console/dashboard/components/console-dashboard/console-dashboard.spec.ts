import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import type { Mock } from 'vitest';

import { ConsoleDashboard } from './console-dashboard';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { TenantContextService } from '../../../../../core/services/tenant-context.service';
import { Page, TenantResponse, SystemResponse, UserResponse } from '../../../../../core/models/admin-api.models';

function fakeJwt(claims: Record<string, unknown>): string {
  const base64url = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url({ alg: 'RS256' })}.${base64url(claims)}.signature`;
}

function pageOf<T>(totalElements: number): Page<T> {
  return { content: [], totalElements, totalPages: 1, number: 0, size: 1 };
}

describe('ConsoleDashboard', () => {
  let fixture: ComponentFixture<ConsoleDashboard>;
  let isAuthenticated: Mock;
  let listSystemsByTenant: Mock;
  let listUsers: Mock;

  const token = fakeJwt({ platform_admin: true, username: 'root_admin', name: 'Administrador' });
  const tenant = { id: '123', code: 'acme' };

  function setup(overrides: { authenticated?: boolean } = {}): void {
    isAuthenticated = vi.fn().mockReturnValue(overrides.authenticated ?? true);
    listSystemsByTenant = vi.fn().mockReturnValue(of(pageOf<SystemResponse>(4)));
    listUsers = vi.fn().mockReturnValue(of(pageOf<UserResponse>(9)));

    TestBed.configureTestingModule({
      imports: [ConsoleDashboard],
      providers: [
        provideRouter([]),
        {
          provide: ConsoleAuthService,
          useValue: { getAccessToken: () => token, isAuthenticated, login: vi.fn() },
        },
        { provide: TenantContextService, useValue: { selectedTenant: signal(tenant) } },
        {
          provide: AdminApiService,
          useValue: {
            listTenants: vi.fn().mockReturnValue(of(pageOf<TenantResponse>(2))),
            listSystemsByTenant,
            listUsers,
          },
        },
      ],
    });
  }

  it('deve decodificar username e name do access token e carregar as contagens', () => {
    setup();
    fixture = TestBed.createComponent(ConsoleDashboard);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.username()).toBe('root_admin');
    expect(component.name()).toBe('Administrador');
    expect(component.tenantsTotal()).toBe(2);
    expect(component.systemsTotal()).toBe(4);
    expect(component.usersTotal()).toBe(9);
    expect(component.sessionExpired()).toBe(false);
    expect(listSystemsByTenant).toHaveBeenCalledWith('123', 0, 1);
    expect(listUsers).toHaveBeenCalledWith('123', 0, 1);
  });

  it('deve exibir estado de sessão expirada em vez das claims do token quando expires_at já passou', () => {
    setup({ authenticated: false });
    fixture = TestBed.createComponent(ConsoleDashboard);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.sessionExpired()).toBe(true);
    expect(component.name()).toBeNull();
    expect(component.username()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Sessão expirada');
  });

  it('deve marcar erro por contagem quando a chamada correspondente falha, sem afetar as demais', () => {
    setup();
    TestBed.overrideProvider(AdminApiService, {
      useValue: {
        listTenants: vi.fn().mockReturnValue(throwError(() => new Error('falhou'))),
        listSystemsByTenant,
        listUsers,
      },
    });
    fixture = TestBed.createComponent(ConsoleDashboard);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.tenantsError()).toBe(true);
    expect(component.tenantsTotal()).toBeNull();
    expect(component.systemsTotal()).toBe(4);
    expect(component.usersTotal()).toBe(9);
  });
});
