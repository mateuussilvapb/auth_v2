import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { ConsoleCallback } from './console-callback';
import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { TenantContextService } from '../../../../../core/services/tenant-context.service';

describe('ConsoleCallback', () => {
  let fixture: ComponentFixture<ConsoleCallback>;
  let router: Router;
  let tenantContext: { clear: ReturnType<typeof vi.fn> };

  async function setup(consoleAuthStub: Partial<ConsoleAuthService>): Promise<void> {
    tenantContext = { clear: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ConsoleCallback],
      providers: [
        provideRouter([]),
        { provide: ConsoleAuthService, useValue: consoleAuthStub },
        { provide: TenantContextService, useValue: tenantContext },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(ConsoleCallback);
  }

  it('deve navegar para /console e limpar o contexto de tenant quando a troca de código sucede', async () => {
    await setup({
      completeLoginFlow: async () => {},
      isAuthenticated: () => true,
    });
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(navigateSpy).toHaveBeenCalledWith(['/console']);
    expect(tenantContext.clear).toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage()).toBeNull();
  });

  it('deve mostrar mensagem de erro quando a troca de código falha', async () => {
    await setup({
      completeLoginFlow: async () => {
        throw new Error('falhou');
      },
      isAuthenticated: () => false,
    });

    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });
});
