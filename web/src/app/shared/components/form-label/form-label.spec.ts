import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';

import { FormLabel } from './form-label';

describe('FormLabel', () => {
  let fixture: ComponentFixture<FormLabel>;
  let control: FormControl;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormLabel],
    }).compileComponents();

    fixture = TestBed.createComponent(FormLabel);
    control = new FormControl('');
    fixture.componentRef.setInput('for', 'nome');
    fixture.componentRef.setInput('label', 'Nome');
    fixture.componentRef.setInput('control', control);
  });

  it('não exibe erro antes de touched/dirty, mesmo com o controle inválido', () => {
    control.setErrors({ required: true });
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe('');
  });

  it('exibe a mensagem mapeada após touched e dirty', () => {
    control.setErrors({ required: true });
    control.markAsTouched();
    control.markAsDirty();
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe('Este campo é obrigatório.');
  });

  it('usa fallback genérico para erro sem mensagem mapeada', () => {
    control.setErrors({ minlength: true });
    control.markAsTouched();
    control.markAsDirty();
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe('Erro: minlength');
  });

  it('onBlur marca o controle como touched/dirty', () => {
    expect(control.touched).toBe(false);

    fixture.componentInstance.onBlur();

    expect(control.touched).toBe(true);
    expect(control.dirty).toBe(true);
  });

  it('erro tem id "<for>-error" para servir de alvo de aria-describedby (guia, seção 8)', () => {
    control.setErrors({ required: true });
    control.markAsTouched();
    control.markAsDirty();
    fixture.detectChanges();

    const span: HTMLElement = fixture.nativeElement.querySelector('span[role="alert"]');
    expect(span.id).toBe('nome-error');
  });
});
