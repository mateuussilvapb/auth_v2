//Angular
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

//Aplicação
import { ConsoleAuthService } from '../../services/console-auth.service';

//Externos
import { ButtonModule } from 'primeng/button';

/**
 * Topbar mínima do console (guia de estilo, seção 7.1) — marca + sair. Usuário e
 * alternador de tema entram na Fase 7, junto do resto do shell.
 */
@Component({
  selector: 'app-topbar',
  imports: [RouterLink, ButtonModule],
  templateUrl: './topbar.html',
})
export class Topbar {
  private readonly consoleAuth = inject(ConsoleAuthService);

  logout(): void {
    this.consoleAuth.logout();
  }
}
