import { TestBed } from '@angular/core/testing';

import { LoadingOverlayService } from './loading-overlay.service';

describe('LoadingOverlayService', () => {
  let service: LoadingOverlayService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoadingOverlayService);
  });

  it('inicia invisível', () => {
    expect(service.visible()).toBe(false);
  });

  it('fica visível enquanto houver show() sem hide() correspondente', () => {
    service.show();
    expect(service.visible()).toBe(true);

    service.show();
    service.hide();
    expect(service.visible()).toBe(true);

    service.hide();
    expect(service.visible()).toBe(false);
  });

  it('hide() nunca deixa o contador negativo', () => {
    service.hide();
    service.hide();
    expect(service.visible()).toBe(false);
  });

  it('wrap() mostra durante a task e esconde ao final, mesmo em erro', async () => {
    await service.wrap(async () => 'ok');
    expect(service.visible()).toBe(false);

    await expect(
      service.wrap(async () => {
        throw new Error('falhou');
      }),
    ).rejects.toThrow('falhou');
    expect(service.visible()).toBe(false);
  });
});
