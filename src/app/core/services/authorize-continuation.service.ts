import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Captura os parâmetros de {@code GET /oauth2/authorize} preservados na query string de
 * {@code /login}/{@code /consent} (seção 2.2 do plano — ver
 * `SpaLoginAuthenticationEntryPoint` no backend) e retoma o fluxo navegando de volta para
 * lá após login/consentimento. É navegação de página inteira, não roteamento Angular: o
 * cookie de sessão HttpOnly precisa ir numa requisição real ao backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthorizeContinuationService {
  constructor(private readonly route: ActivatedRoute) {}

  async captureParams(): Promise<URLSearchParams> {
    const queryParamMap = await firstValueFrom(this.route.queryParamMap);
    const params = new URLSearchParams();
    queryParamMap.keys.forEach((key) => {
      const value = queryParamMap.get(key);
      if (value !== null) {
        params.set(key, value);
      }
    });
    return params;
  }

  continueAuthorize(params: URLSearchParams): void {
    window.location.href = `${environment.apiBaseUrl}/oauth2/authorize?${params.toString()}`;
  }
}
