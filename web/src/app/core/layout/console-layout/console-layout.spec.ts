import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';

import { ConsoleLayout } from './console-layout';
import { ConsoleAuthService } from '../../services/console-auth.service';
import { TenantContextService } from '../../services/tenant-context.service';

@Component({ selector: 'app-blank', template: '' })
class Blank {}

describe('ConsoleLayout', () => {
  let fixture: ComponentFixture<ConsoleLayout>;
  let router: Router;

  beforeEach(async () => {
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
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
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
});
