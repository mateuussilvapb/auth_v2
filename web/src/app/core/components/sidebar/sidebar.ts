//Angular
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

//Aplicação
import { TenantContextService } from '../../services/tenant-context.service';

/**
 * Sidebar do console (guia de estilo, seção 7.1). Tenants é o único item independente de
 * contexto; Sistemas e Usuários usam o tenant selecionado ({@link TenantContextService},
 * decisão de produto 2026-08-25) para montar o link — sem tenant selecionado o
 * `tenantContextGuard` intercepta a navegação e redireciona à seleção.
 * <p>
 * Perfis, Vínculos e Platform Admins ainda não têm rota própria (Perfis depende de um
 * sistema escolhido, Vínculos de um usuário escolhido, Platform Admins nem existe como tela)
 * — entram quando os respectivos CRUDs forem migrados (Fase 7 do plano).
 */
@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
})
export class Sidebar {
  private readonly tenantContext = inject(TenantContextService);

  readonly selectedTenant = this.tenantContext.selectedTenant;
}
