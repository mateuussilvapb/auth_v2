import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('sem preferência salva, segue prefers-color-scheme do sistema', () => {
    window.matchMedia = vi.fn().mockReturnValue({ matches: true } as MediaQueryList);

    const service = TestBed.inject(ThemeService);

    expect(service.dark()).toBe(true);
  });

  it('com preferência salva, ignora prefers-color-scheme', () => {
    localStorage.setItem('console.theme', 'light');
    window.matchMedia = vi.fn().mockReturnValue({ matches: true } as MediaQueryList);

    const service = TestBed.inject(ThemeService);

    expect(service.dark()).toBe(false);
  });

  it('toggle() inverte o estado e persiste em localStorage', () => {
    window.matchMedia = vi.fn().mockReturnValue({ matches: false } as MediaQueryList);
    const service = TestBed.inject(ThemeService);

    service.toggle();

    expect(service.dark()).toBe(true);
    expect(localStorage.getItem('console.theme')).toBe('dark');

    service.toggle();

    expect(service.dark()).toBe(false);
    expect(localStorage.getItem('console.theme')).toBe('light');
  });
});
