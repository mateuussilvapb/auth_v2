//Angular
import { inject, Injectable } from '@angular/core';

//Externos
import { MessageService as MessageServicePG } from 'primeng/api';

type ToastSeverity = 'success' | 'error' | 'warn' | 'info';

/**
 * Wrapper do MessageService do PrimeNG (guia de estilo, seção 5.4/7.2) — centraliza
 * summary padrão em pt-BR e o `life` de 5s para toda a aplicação.
 */
@Injectable({
  providedIn: 'root',
})
export class MessageService {
  private readonly messageService = inject(MessageServicePG);

  showSuccess(detail: string, summary: string = 'Sucesso'): void {
    this.showGeneric(detail, summary, 'success');
  }

  showError(detail: string, summary: string = 'Erro'): void {
    this.showGeneric(detail, summary, 'error');
  }

  showWarning(detail: string, summary: string = 'Atenção'): void {
    this.showGeneric(detail, summary, 'warn');
  }

  showInfo(detail: string, summary: string = 'Informação'): void {
    this.showGeneric(detail, summary, 'info');
  }

  private showGeneric(detail: string, summary: string, severity: ToastSeverity, life: number = 5000): void {
    this.messageService.add({ severity, summary, detail, life });
  }
}
