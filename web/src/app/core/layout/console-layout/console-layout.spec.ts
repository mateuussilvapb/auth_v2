import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';

import { ConsoleLayout } from './console-layout';
import { ConsoleAuthService } from '../../services/console-auth.service';
import { TenantContextService } from '../../services/tenant-context.service';
import { ThemeService } from '../../services/theme.service';

@Component({ selector: 'app-blank', template: '' })
class Blank {}

describe('ConsoleLayout', () => {
  let fixture: ComponentFixture<ConsoleLayout>;
  let router: Router;
  let themeServiceStub: { dark: ReturnType<typeof signal<boolean>>; toggle: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    document.documentElement.classList.remove('app-dark', 'app-light');
    themeServiceStub = { dark: signal(false), toggle: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ConsoleLayout],
      providers: [
        provideRouter([
          { path: 'console', component: Blank },
          { path: 'console/selecionar-tenant', component: Blank, data: { hideSidebar: true } },
        ]),
        MessageService,
        ConfirmationService,
        { provide: ConsoleAuthService, useValue: { logout: vi.fn() } },
        { provide: TenantContextService, useValue: { selectedTenant: () => null, clear: vi.fn() } },
        { provide: ThemeService, useValue: themeServiceStub },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  afterEach(() => {
    document.documentElement.classList.remove('app-dark', 'app-light');
  });

  it('renderiza topbar, sidebar e o router-outlet', async () => {
    await router.navigateByUrl('/console');
    fixture = TestBed.createComponent(ConsoleLayout);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-topbar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-sidebar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.layout-main-container')).not.toBeNull();
  });

  it('oculta a sidebar na tela de seleção de tenant', async () => {
    await router.navigateByUrl('/console/selecionar-tenant');
    fixture = TestBed.createComponent(ConsoleLayout);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-sidebar')).toBeNull();
    expect(fixture.nativeElement.querySelector('.layout-main-container--no-sidebar')).not.toBeNull();
  });

  it('aplica .app-light por padrão (tema claro) e troca para .app-dark reagindo a mudanças', async () => {
    await router.navigateByUrl('/console');
    fixture = TestBed.createComponent(ConsoleLayout);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('app-dark')).toBe(false);
    expect(document.documentElement.classList.contains('app-light')).toBe(true);

    themeServiceStub.dark.set(true);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('app-dark')).toBe(true);
    expect(document.documentElement.classList.contains('app-light')).toBe(false);
  });

  it('remove .app-dark e .app-light de <html> ao destruir o shell (telas públicas não herdam a escolha)', async () => {
    await router.navigateByUrl('/console');
    fixture = TestBed.createComponent(ConsoleLayout);
    themeServiceStub.dark.set(true);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('app-dark')).toBe(true);

    fixture.destroy();

    expect(document.documentElement.classList.contains('app-dark')).toBe(false);
    expect(document.documentElement.classList.contains('app-light')).toBe(false);
  });
});
