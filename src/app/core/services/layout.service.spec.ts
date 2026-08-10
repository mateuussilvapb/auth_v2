import { TestBed } from '@angular/core/testing';

import { LayoutService } from './layout.service';

describe('LayoutService', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
    TestBed.configureTestingModule({});
  });

  it('menu principal inicia visível em telas desktop (> 991px)', () => {
    const service = TestBed.inject(LayoutService);

    expect(service.isDesktop()).toBe(true);
    expect(service.mainMenuVisible()).toBe(true);
  });

  it('menu principal inicia oculto em telas estreitas (<= 991px)', () => {
    Object.defineProperty(window, 'innerWidth', { value: 800, configurable: true });
    const service = TestBed.inject(LayoutService);

    expect(service.isDesktop()).toBe(false);
    expect(service.mainMenuVisible()).toBe(false);
  });

  it('onMenuToggle() inverte a visibilidade do menu', () => {
    const service = TestBed.inject(LayoutService);

    service.onMenuToggle();
    expect(service.mainMenuVisible()).toBe(false);

    service.onMenuToggle();
    expect(service.mainMenuVisible()).toBe(true);
  });
});
