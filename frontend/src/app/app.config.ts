import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // withFetch: cookie de sessão HttpOnly (seção 7.1) precisa ir em toda chamada às
    // rotas /api/auth/** — os serviços fazem isso explicitamente via { withCredentials: true }.
    provideHttpClient(withFetch()),
  ],
};
