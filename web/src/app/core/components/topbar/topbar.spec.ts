import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import type { Mock } from 'vitest';

import { Topbar } from './topbar';
import { ConsoleAuthService } from '../../services/console-auth.service';
import { TenantContextService, SelectedTenant } from '../../services/tenant-context.service';

@Component({ selector: 'app-blank', template: '' })
class Blank {}

describe('Topbar', () => {
  let fixture: ComponentFixture<Topbar>;
  let consoleAuthStub: { logout: Mock };
  let tenantContextStub: { selectedTenant: ReturnType<typeof signal<SelectedTenant | null>>; clear: Mock };
  let router: Router;

  async function setup(): Promise<void> {
    consoleAuthStub = { logout: vi.fn() };
    tenantContextStub = { selectedTenant: signal<SelectedTenant | null>(null), clear: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Topbar],
      providers: [
        provideRouter([
          { path: 'console', component: Blank },
          { path: 'console/tenants', component: Blank },
          { path: 'console/tenants/novo', component: Blank, data: { critical: true } },
        ]),
        ConfirmationService,
        { provide: ConsoleAuthService, useValue: consoleAuthStub },
        { provide: TenantContextService, useValue: tenantContextStub },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  }

  it('exibe a marca do console', async () => {
    await setup();
    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).toContain('Auth Server Console');
  });

  it('chama ConsoleAuthService.logout() e limpa o contexto de tenant ao sair', async () => {
    await setup();
    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();

    await fixture.componentInstance.logout();

    expect(tenantContextStub.clear).toHaveBeenCalled();
    expect(consoleAuthStub.logout).toHaveBeenCalled();
  });

  it('não exibe o tenant quando nenhum está selecionado', async () => {
    await setup();
    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.layout-topbar-tenant')).toBeNull();
  });

  it('exibe o código do tenant selecionado', async () => {
    await setup();
    tenantContextStub.selectedTenant.set({ id: 't1', code: 'acme' });
    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).toContain('acme');
  });

  it('em tela não crítica, troca de tenant direto sem confirmação', async () => {
    await setup();
    await router.navigateByUrl('/console/tenants');
    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.changeTenant();

    expect(tenantContextStub.clear).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/console/selecionar-tenant']);
  });

  it('em tela crítica, pede confirmação antes de trocar de tenant', async () => {
    await setup();
    await router.navigateByUrl('/console/tenants/novo');
    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const confirmationService = TestBed.inject(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');

    fixture.componentInstance.changeTenant();

    expect(confirmSpy).toHaveBeenCalled();
    expect(tenantContextStub.clear).not.toHaveBeenCalled();

    confirmSpy.mock.calls[0][0].accept?.();

    expect(tenantContextStub.clear).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/console/selecionar-tenant']);
  });
});
