//Angular
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Directive, inject, signal } from '@angular/core';

//Aplicação
import { MessageService } from '../../services/message.service';

//Externo
import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

/**
 * Base para telas de listagem (guia de estilo, seção 7.2) — injeta o que toda listagem
 * precisa (router, form de filtro, diálogos, feedback) sem forçar herança de comportamento.
 */
@Directive({})
export class ListBase {
  protected readonly router = inject(Router);
  protected readonly fb = inject(FormBuilder);
  protected readonly dialogService = inject(DialogService);
  protected readonly messageService = inject(MessageService);
  protected readonly confirmationService = inject(ConfirmationService);

  form: FormGroup = this.fb.group({});

  submitting = signal<boolean>(false);

  getContentFieldOrDefault(data: unknown): unknown {
    if (data) {
      if (typeof data === 'string') {
        return data !== '' ? data : '-';
      }
      return data;
    }
    return '-';
  }
}
