//Angular
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

//Aplicação
import { TenantContextService } from '../../services/tenant-context.service';

/**
 * Sidebar do console (guia de estilo, seção 7.1). Tenants e Platform Admins são os únicos
 * itens independentes de contexto; Sistemas e Usuários usam o tenant selecionado
 * ({@link TenantContextService}, decisão de produto 2026-08-25) para montar o link — sem
 * tenant selecionado o `tenantContextGuard` intercepta a navegação e redireciona à seleção.
 * <p>
 * Perfis e Vínculos não têm item próprio na sidebar — dependem de um sistema/usuário já
 * escolhido (chegam por link nas telas de Sistemas/Usuários), não fazem sentido como
 * destino direto do menu.
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
