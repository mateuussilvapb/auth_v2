import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApiService } from '../services/admin-api.service';
import { ConsoleAuthService } from '../services/console-auth.service';
import { TenantContextService } from '../services/tenant-context.service';
import { decodeJwtPayload } from '../util/jwt';

/**
 * Exige um tenant selecionado (decisão de produto 2026-08-29) em toda rota do console que
 * não seja gestão de tenants nem a própria tela de seleção — aplica-se só a platform admins
 * (claim `platform_admin` do JWT, emitida por `JwtTokenCustomizer` no backend). Sem tenant
 * algum cadastrado, força a criação do primeiro antes de liberar qualquer outra tela; com
 * tenants existentes mas nenhum selecionado (localStorage vazio ou não decifrável — inclui o
 * caso pós-login, seção "Em aberto" do plano), força a tela de seleção.
 */
export const tenantContextGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const consoleAuth = inject(ConsoleAuthService);
  const tenantContext = inject(TenantContextService);
  const adminApi = inject(AdminApiService);
  const router = inject(Router);

  const accessToken = consoleAuth.getAccessToken();
  const claims = decodeJwtPayload(accessToken);
  if (claims['platform_admin'] !== true) {
    return true;
  }

  const selected = await tenantContext.restore(accessToken);
  if (selected) {
    return true;
  }

  const page = await firstValueFrom(adminApi.listTenants(0, 1));
  if (page.content.length === 0) {
    return router.createUrlTree(['/console/tenants/novo']);
  }

  return router.createUrlTree(['/console/selecionar-tenant']);
};
