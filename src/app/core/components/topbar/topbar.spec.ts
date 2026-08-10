import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { Mock } from 'vitest';

import { Topbar } from './topbar';
import { ConsoleAuthService } from '../../services/console-auth.service';

describe('Topbar', () => {
  let fixture: ComponentFixture<Topbar>;
  let consoleAuthStub: { logout: Mock };

  beforeEach(async () => {
    consoleAuthStub = { logout: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Topbar],
      providers: [provideRouter([]), { provide: ConsoleAuthService, useValue: consoleAuthStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(Topbar);
    fixture.detectChanges();
  });

  it('exibe a marca do console', () => {
    expect((fixture.nativeElement.textContent as string)).toContain('Auth Server Console');
  });

  it('chama ConsoleAuthService.logout() ao clicar em Sair', () => {
    fixture.componentInstance.logout();

    expect(consoleAuthStub.logout).toHaveBeenCalled();
  });
});
