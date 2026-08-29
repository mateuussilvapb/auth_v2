//Angular
import { Component, computed, input } from '@angular/core';

//Externos
import { TagModule } from 'primeng/tag';

type TagSeverity = 'success' | 'secondary' | 'danger' | 'warn' | 'info';

/**
 * Mapeamento único de status do backend → severidade/rótulo (guia de estilo, seção 2.3).
 * Nenhum template decide cor de status por conta própria — sempre passar pelo StatusTag.
 */
const STATUS_MAP: Record<string, { severity: TagSeverity; label: string }> = {
  ACTIVE: { severity: 'success', label: 'Ativo' },
  INACTIVE: { severity: 'secondary', label: 'Inativo' },
  BLOCKED: { severity: 'danger', label: 'Bloqueado' },
  DISABLED: { severity: 'secondary', label: 'Desabilitado' },
};

@Component({
  selector: 'app-status-tag',
  imports: [TagModule],
  template: `<p-tag [severity]="severity()" [value]="label()" />`,
})
export class StatusTag {
  status = input.required<string>();

  private readonly mapped = computed(() => STATUS_MAP[this.status()] ?? { severity: 'secondary' as const, label: this.status() });

  severity = computed(() => this.mapped().severity);
  label = computed(() => this.mapped().label);
}
