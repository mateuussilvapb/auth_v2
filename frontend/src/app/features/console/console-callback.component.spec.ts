import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { ConsoleCallbackComponent } from './console-callback.component';
import { ConsoleAuthService } from '../../core/services/console-auth.service';

describe('ConsoleCallbackComponent', () => {
  let fixture: ComponentFixture<ConsoleCallbackComponent>;
  let router: Router;

  async function setup(consoleAuthStub: Partial<ConsoleAuthService>): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ConsoleCallbackComponent],
      providers: [provideRouter([]), { provide: ConsoleAuthService, useValue: consoleAuthStub }],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(ConsoleCallbackComponent);
  }

  it('deve navegar para /console quando a troca de código sucede', async () => {
    await setup({
      completeLoginFlow: async () => {},
      isAuthenticated: () => true,
    });
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(navigateSpy).toHaveBeenCalledWith(['/console']);
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
