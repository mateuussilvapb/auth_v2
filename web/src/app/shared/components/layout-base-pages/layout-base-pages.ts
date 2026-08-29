//Angular
import { Component, input, output } from '@angular/core';

//Externos
import { ButtonModule } from 'primeng/button';

/**
 * Cabeçalho padrão de página do console (guia de estilo, seção 7.2): título, subtítulo
 * opcional e um botão de ação primária, com o conteúdo da página projetado abaixo.
 */
@Component({
  selector: 'app-layout-base-pages',
  imports: [ButtonModule],
  templateUrl: './layout-base-pages.html',
})
export class LayoutBasePages {
  title = input.required<string>();
  subtitle = input<string>();
  buttonActionLabel = input<string>();

  actionButtonClick = output();

  onButtonActionClick(): void {
    this.actionButtonClick.emit();
  }
}
