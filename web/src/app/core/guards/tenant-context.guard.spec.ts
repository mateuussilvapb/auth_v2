import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { tenantContextGuard } from './tenant-context.guard';
import { AdminApiService } from '../services/admin-api.service';
import { ConsoleAuthService } from '../services/console-auth.service';
import { TenantContextService } from '../services/tenant-context.service';
import { Page, TenantResponse } from '../models/admin-api.models';

function encodeClaims(claims: Record<string, unknown>): string {
  const base64url = (input: string): string =>
    btoa(input).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url('{}')}.${base64url(JSON.stringify(claims))}.signature`;
}

function page(content: TenantResponse[]): Page<TenantResponse> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 1 };
}

describe('tenantContextGuard', () => {
  let router: Router;
  let listTenants: ReturnType<typeof vi.fn>;
  let restore: ReturnType<typeof vi.fn>;

  function setup(claims: Record<string, unknown>): void {
    listTenants = vi.fn().mockReturnValue(of(page([])));
    restore = vi.fn().mockResolvedValue(null);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ConsoleAuthService, useValue: { getAccessToken: () => encodeClaims(claims) } },
        { provide: AdminApiService, useValue: { listTenants } },
        { provide: TenantContextService, useValue: { restore } },
      ],
    });

    router = TestBed.inject(Router);
  }

  async function run(): Promise<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() => tenantContextGuard({} as never, {} as never)) as Promise<
      boolean | UrlTree
    >;
  }

  it('libera direto quando o token não é de platform admin', async () => {
    setup({ username: 'joao' });

    const result = await run();

    expect(result).toBe(true);
    expect(restore).not.toHaveBeenCalled();
  });

  it('libera quando já há um tenant selecionado no storage', async () => {
    setup({ platform_admin: true });
    restore.mockResolvedValue({ id: 't1', code: 'acme' });

    const result = await run();

    expect(result).toBe(true);
  });

  it('redireciona para criação de tenant quando não existe nenhum', async () => {
    setup({ platform_admin: true });
    listTenants.mockReturnValue(of(page([])));

    const result = (await run()) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/console/tenants/novo');
  });

  it('redireciona para seleção de tenant quando existem tenants mas nenhum selecionado', async () => {
    setup({ platform_admin: true });
    listTenants.mockReturnValue(of(page([{ id: 't1', code: 'acme', name: 'Acme', status: 'ACTIVE', logoUrl: null }])));

    const result = (await run()) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/console/selecionar-tenant');
  });
});
