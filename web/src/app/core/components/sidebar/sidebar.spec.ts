import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Sidebar } from './sidebar';
import { TenantContextService, SelectedTenant } from '../../services/tenant-context.service';

@Component({ selector: 'app-blank', template: '' })
class Blank {}

describe('Sidebar', () => {
  let fixture: ComponentFixture<Sidebar>;
  let tenantContextStub: { selectedTenant: ReturnType<typeof signal<SelectedTenant | null>> };

  async function setup(): Promise<void> {
    tenantContextStub = { selectedTenant: signal<SelectedTenant | null>(null) };

    await TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [
        provideRouter([{ path: 'console/tenants/:id/systems', component: Blank }]),
        { provide: TenantContextService, useValue: tenantContextStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Sidebar);
    fixture.detectChanges();
  }

  it('exibe o link para Tenants', async () => {
    await setup();

    const link = fixture.nativeElement.querySelector('a[href="/console/tenants"]');
    expect(link).not.toBeNull();
    expect((link.textContent as string)).toContain('Tenants');
  });

  it('exibe o link para Platform Admins independente de tenant selecionado', async () => {
    await setup();

    const link = fixture.nativeElement.querySelector('a[href="/console/platform-admins"]');
    expect(link).not.toBeNull();
    expect((link.textContent as string)).toContain('Platform Admins');
  });

  it('sem tenant selecionado, não exibe Sistemas nem Usuários', async () => {
    await setup();

    expect((fixture.nativeElement.textContent as string)).not.toContain('Sistemas');
    expect((fixture.nativeElement.textContent as string)).not.toContain('Usuários');
  });

  it('com tenant selecionado, exibe Sistemas e Usuários apontando para o tenant', async () => {
    await setup();
    tenantContextStub.selectedTenant.set({ id: 't1', code: 'acme' });
    fixture.detectChanges();

    const systemsLink = fixture.nativeElement.querySelector('a[href="/console/tenants/t1/systems"]');
    const usersLink = fixture.nativeElement.querySelector('a[href="/console/tenants/t1/users"]');
    expect(systemsLink).not.toBeNull();
    expect(usersLink).not.toBeNull();
  });
});
