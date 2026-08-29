//Angular
import { Component, inject, input, signal } from '@angular/core';

//Aplicação
import { MessageService } from '../../services/message.service';

//Externos
import { ButtonModule } from 'primeng/button';

/**
 * Valor mono + copiar + máscara opcional (guia de estilo, seção 5.5) — usado para
 * `clientSecret`, IDs (TSID) e redirect URIs. Nunca persiste o valor em storage nem loga.
 */
@Component({
  selector: 'app-copy-field',
  imports: [ButtonModule],
  templateUrl: './copy-field.html',
  styleUrl: './copy-field.scss',
})
export class CopyField {
  value = input.required<string>();
  masked = input(false);
  label = input<string>();

  protected readonly revealed = signal(false);

  private readonly messageService = inject(MessageService);

  toggleReveal(): void {
    this.revealed.update((valor) => !valor);
  }

  async copy(): Promise<void> {
    await navigator.clipboard.writeText(this.value());
    this.messageService.showSuccess('Valor copiado para a área de transferência.');
  }
}
