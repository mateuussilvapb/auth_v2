
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ConsoleAuthService } from '../../../../../core/services/console-auth.service';
import { decodeJwtPayload } from '../../../../../core/util/jwt';

/**
 * Landing page do console administrativo, protegida por {@code consoleAuthGuard}. Ponto de
 * entrada para o CRUD administrativo (Fase 9) — hoje só linka para /console/tenants;
 * sistemas/perfis/usuários/vínculos são os próximos itens do checklist.
 */
@Component({
  selector: 'app-console-dashboard',
  imports: [RouterLink],
  templateUrl: './console-dashboard.html',
})
export class ConsoleDashboard implements OnInit {
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
