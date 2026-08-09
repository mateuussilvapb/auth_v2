import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';

import { ConsoleAuthService } from '../../core/services/console-auth.service';
import { decodeJwtPayload } from '../../core/util/jwt';

/**
 * Landing page do console administrativo, protegida por {@code consoleAuthGuard}. Só
 * confirma que o token PKCE foi emitido corretamente (claim {@code platform_admin}) — as
 * telas de CRUD de tenants/sistemas/perfis/usuários (Fase 9, itens seguintes do checklist)
 * ainda não existem.
 */
@Component({
  selector: 'app-console-dashboard',
  imports: [CommonModule],
  templateUrl: './console-dashboard.component.html',
})
export class ConsoleDashboardComponent implements OnInit {
  name = signal<string | null>(null);
  username = signal<string | null>(null);

  constructor(private readonly consoleAuth: ConsoleAuthService) {}

  ngOnInit(): void {
    const claims = decodeJwtPayload(this.consoleAuth.getAccessToken());
    this.name.set(typeof claims['name'] === 'string' ? (claims['name'] as string) : null);
    this.username.set(typeof claims['username'] === 'string' ? (claims['username'] as string) : null);
  }

  logout(): void {
    this.consoleAuth.logout();
  }
}
