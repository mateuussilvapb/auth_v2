import { TestBed } from '@angular/core/testing';

import { consoleAuthGuard } from './console-auth.guard';
import { ConsoleAuthService } from '../services/console-auth.service';

describe('consoleAuthGuard', () => {
  it('deve permitir ativação quando já autenticado', async () => {
    TestBed.configureTestingModule({
      providers: [{ provide: ConsoleAuthService, useValue: { isAuthenticated: () => true, login: async () => {} } }],
    });

    const result = await TestBed.runInInjectionContext(() => consoleAuthGuard({} as never, {} as never));
    expect(result).toBe(true);
  });

  it('deve disparar login e bloquear ativação quando não autenticado', async () => {
    let loginCalled = false;
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConsoleAuthService,
          useValue: {
            isAuthenticated: () => false,
            login: async () => {
              loginCalled = true;
            },
          },
        },
      ],
    });

    const result = await TestBed.runInInjectionContext(() => consoleAuthGuard({} as never, {} as never));
    expect(result).toBe(false);
    expect(loginCalled).toBe(true);
  });
});
