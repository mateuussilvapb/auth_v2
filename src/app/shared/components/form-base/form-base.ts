//Angular
import { computed, Directive, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

//Aplicação
import { ListBase } from '../list-base/list-base';

//Externo
import { DynamicDialogRef } from 'primeng/dynamicdialog';

export interface FormDialogData {
  mode?: 'cadastro' | 'edicao' | 'visualizacao';
  id?: string;
  valoresIniciais?: Record<string, unknown>;
}

/**
 * Base para telas de formulário (guia de estilo, seção 5.2/7.2). Detecta o modo
 * (cadastro/edição/visualização) tanto por rota quanto por dados de diálogo, e resolve
 * `finalizar()` fechando o diálogo ou navegando, conforme o contexto de abertura.
 */
@Directive({})
export class FormBase extends ListBase {
  protected readonly route = inject(ActivatedRoute);
  protected readonly dialogRef = inject(DynamicDialogRef, { optional: true });

  protected dialogData = computed<FormDialogData | null>(() =>
    this.dialogRef ? (this.dialogService.getInstance(this.dialogRef)?.data ?? null) : null,
  );

  isDialogMode = computed<boolean>(() => this.dialogRef !== null);

  isEditMode = computed<boolean>(
    () => this.dialogData()?.mode === 'edicao' || (!this.dialogData() && this.router.url.includes('edicao')),
  );

  isCreateMode = computed<boolean>(
    () => this.dialogData()?.mode === 'cadastro' || (!this.dialogData() && this.router.url.includes('cadastro')),
  );

  isViewMode = computed<boolean>(
    () =>
      this.dialogData()?.mode === 'visualizacao' ||
      (!this.dialogData() && this.router.url.includes('visualizacao')),
  );

  pageId = computed<string>(() => this.dialogData()?.id ?? this.route.snapshot.paramMap.get('id') ?? '');

  isInvalid(controlName: string): boolean {
    const control = this.form?.get(controlName);
    return (control?.touched && control?.dirty && control?.invalid) ?? false;
  }

  protected finalizar<T>(resultado: T | undefined, rotaFallback: string[]): void {
    if (this.dialogRef) {
      this.dialogRef.close(resultado);
      return;
    }
    this.router.navigate(rotaFallback);
  }
}
