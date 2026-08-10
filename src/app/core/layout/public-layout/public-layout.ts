//Angular
import { RouterOutlet } from '@angular/router';
import { Component } from '@angular/core';

//Aplicação
import { Toast } from '../../../shared/components/toast/toast';
import { LoadingOverlay } from '../../../shared/components/loading-overlay/loading-overlay';

/**
 * Shell das telas públicas (guia de estilo, seção 1.1/7.1) — sem topbar/sidebar, card
 * centralizado. Audiência: usuário final do tenant, zero fricção. Ainda não usado pelas
 * rotas — as telas públicas mantêm o card próprio (CSS antigo) até serem reescritas na
 * Fase 6; passam a usar este shell nessa fase (ver PROGRESS.md).
 */
@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, Toast, LoadingOverlay],
  templateUrl: './public-layout.html',
})
export class PublicLayout {}
