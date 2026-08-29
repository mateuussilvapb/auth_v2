import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ConsoleDashboard } from './console-dashboard';
import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';

function fakeJwt(claims: Record<string, unknown>): string {
  const base64url = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url({ alg: 'RS256' })}.${base64url(claims)}.signature`;
}

describe('ConsoleDashboard', () => {
  let fixture: ComponentFixture<ConsoleDashboard>;

  beforeEach(async () => {
    const token = fakeJwt({ platform_admin: true, username: 'root_admin', name: 'Administrador' });

    await TestBed.configureTestingModule({
      imports: [ConsoleDashboard],
      providers: [
        provideRouter([]),
        { provide: ConsoleAuthService, useValue: { getAccessToken: () => token, logout: () => {} } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConsoleDashboard);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('deve decodificar username e name do access token', () => {
    expect(fixture.componentInstance.username()).toBe('root_admin');
    expect(fixture.componentInstance.name()).toBe('Administrador');
  });
});
