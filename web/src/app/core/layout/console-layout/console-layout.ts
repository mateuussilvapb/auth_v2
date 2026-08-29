//Angular
import { RouterOutlet } from '@angular/router';
import { Component } from '@angular/core';

//Aplicação
import { Topbar } from '../../components/topbar/topbar';
import { Sidebar } from '../../components/sidebar/sidebar';
import { Toast } from '../../../shared/components/toast/toast';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingOverlay } from '../../../shared/components/loading-overlay/loading-overlay';

/**
 * Shell do console administrativo (guia de estilo, seção 1.1/7.1) — topbar + sidebar +
 * router-outlet. Audiência: platform admin, uso contínuo, densidade de informação.
 */
@Component({
  selector: 'app-console-layout',
  imports: [RouterOutlet, Topbar, Sidebar, Toast, ConfirmDialog, LoadingOverlay],
  templateUrl: './console-layout.html',
})
export class ConsoleLayout {}
