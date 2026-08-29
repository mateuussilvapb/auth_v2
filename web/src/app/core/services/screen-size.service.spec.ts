import { TestBed } from '@angular/core/testing';
import { firstValueFrom, take } from 'rxjs';

import { ScreenSizeService } from './screen-size.service';

describe('ScreenSizeService', () => {
  let service: ScreenSizeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ScreenSizeService);
  });

  it('emite a largura atual da janela ao inscrever', async () => {
    const width = await firstValueFrom(service.width$.pipe(take(1)));

    expect(width).toBe(window.innerWidth);
  });

  it('se inscreve no evento "resize" da window ao assinar width$', () => {
    const addEventListenerSpy = vi.spyOn(window, 'addEventListener');

    const subscription = service.width$.subscribe();

    expect(addEventListenerSpy).toHaveBeenCalledWith('resize', expect.any(Function), undefined);
    subscription.unsubscribe();
  });
});
