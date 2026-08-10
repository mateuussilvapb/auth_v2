import { Injectable } from '@angular/core';
import { fromEvent, Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class ScreenSizeService {
  width$: Observable<number>;

  constructor() {
    this.width$ = fromEvent(globalThis.window, 'resize').pipe(
      map((event) => (event.target as Window).innerWidth),
      startWith(globalThis.window.innerWidth),
    );
  }
}
