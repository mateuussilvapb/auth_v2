import { TestBed } from '@angular/core/testing';

import { TenantContextService } from './tenant-context.service';

describe('TenantContextService', () => {
  let service: TenantContextService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TenantContextService);
  });

  it('inicia sem tenant selecionado', () => {
    expect(service.selectedTenant()).toBeNull();
  });

  it('select grava o tenant cifrado em localStorage e atualiza o signal', async () => {
    await service.select({ id: 't1', code: 'acme' }, 'token-abc');

    expect(service.selectedTenant()).toEqual({ id: 't1', code: 'acme' });
    const raw = localStorage.getItem('console.tenant-context');
    expect(raw).not.toBeNull();
    expect(raw).not.toContain('acme');
  });

  it('restore com o mesmo access token decifra o tenant gravado', async () => {
    await service.select({ id: 't1', code: 'acme' }, 'token-abc');

    const restored = await service.restore('token-abc');

    expect(restored).toEqual({ id: 't1', code: 'acme' });
    expect(service.selectedTenant()).toEqual({ id: 't1', code: 'acme' });
  });

  it('restore com access token diferente falha ao decifrar e limpa o storage', async () => {
    await service.select({ id: 't1', code: 'acme' }, 'token-abc');

    const restored = await service.restore('token-outro');

    expect(restored).toBeNull();
    expect(service.selectedTenant()).toBeNull();
    expect(localStorage.getItem('console.tenant-context')).toBeNull();
  });

  it('restore sem nada gravado retorna null', async () => {
    const restored = await service.restore('token-abc');

    expect(restored).toBeNull();
  });

  it('clear remove o tenant do storage e do signal', async () => {
    await service.select({ id: 't1', code: 'acme' }, 'token-abc');

    service.clear();

    expect(service.selectedTenant()).toBeNull();
    expect(localStorage.getItem('console.tenant-context')).toBeNull();
  });
});
