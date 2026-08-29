//Angular
import { Component, computed, effect, input, signal } from '@angular/core';
import { AbstractControl } from '@angular/forms';

//Externos
import { TooltipModule } from 'primeng/tooltip';
import { merge } from 'rxjs';

/**
 * Label + slot de campo + erro inline (guia de estilo, seção 5.2): erro só aparece após
 * `touched && dirty`, nunca placeholder como label.
 */
@Component({
  selector: 'app-form-label',
  imports: [TooltipModule],
  templateUrl: './form-label.html',
  styleUrl: './form-label.scss',
  host: {
    '(focusout)': 'onBlur()',
  },
})
export class FormLabel {
  readonly for = input.required<string>();
  readonly label = input.required<string>();
  readonly control = input.required<AbstractControl>();

  readonly icon = input<string>();
  readonly tooltipMessage = input<string>();

  readonly errorMessages = input<Record<string, string>>({
    required: 'Este campo é obrigatório.',
    email: 'E-mail inválido.',
  });

  private readonly state = signal({
    touched: false,
    dirty: false,
    pristine: true,
    errors: null as Record<string, unknown> | null,
  });

  constructor() {
    effect((onCleanup) => {
      const ctrl = this.control();

      if (!ctrl) {
        this.state.set({ touched: false, dirty: false, pristine: true, errors: null });
        return;
      }

      const updateState = () => {
        this.state.set({
          touched: ctrl.touched,
          dirty: ctrl.dirty,
          pristine: ctrl.pristine,
          errors: ctrl.errors,
        });
      };

      updateState();

      const subscription = merge(ctrl.valueChanges, ctrl.statusChanges).subscribe(updateState);

      onCleanup(() => subscription.unsubscribe());
    });
  }

  onBlur(): void {
    const ctrl = this.control();

    if (!ctrl) {
      return;
    }

    ctrl.markAsTouched();
    ctrl.markAsDirty();
    ctrl.updateValueAndValidity();

    this.state.set({
      touched: ctrl.touched,
      dirty: ctrl.dirty,
      pristine: ctrl.pristine,
      errors: ctrl.errors,
    });
  }

  readonly errorMessage = computed(() => {
    const state = this.state();

    if (!state.errors || (state.pristine && !state.touched)) {
      return '';
    }

    const firstError = Object.keys(state.errors)[0];
    return this.errorMessages()?.[firstError] ?? `Erro: ${firstError}`;
  });
}
