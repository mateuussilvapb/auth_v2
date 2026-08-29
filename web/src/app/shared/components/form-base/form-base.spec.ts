import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { FormBase } from './form-base';

@Component({ selector: 'app-test-blank', template: '' })
class TestBlankComponent {}

class TestFormBase extends FormBase {}

describe('FormBase (sem diálogo, modo via URL)', () => {
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', component: TestBlankComponent }]),
        ConfirmationService,
        MessageServicePG,
        DialogService,
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
      ],
    });
    router = TestBed.inject(Router);
  });

  function create(): TestFormBase {
    return TestBed.runInInjectionContext(() => new TestFormBase());
  }

  it('detecta modo cadastro pela URL', async () => {
    await router.navigateByUrl('/tenants/cadastro');
    const base = create();

    expect(base.isCreateMode()).toBe(true);
    expect(base.isEditMode()).toBe(false);
    expect(base.isDialogMode()).toBe(false);
  });

  it('detecta modo edição pela URL', async () => {
    await router.navigateByUrl('/tenants/1/edicao');
    const base = create();

    expect(base.isEditMode()).toBe(true);
    expect(base.isCreateMode()).toBe(false);
  });

  it('isInvalid só é true após touched e dirty', () => {
    const base = create();
    base.form.addControl('nome', new FormControl(''));
    const control = base.form.get('nome')!;
    control.setErrors({ required: true });

    expect(base.isInvalid('nome')).toBe(false);

    control.markAsTouched();
    control.markAsDirty();
    expect(base.isInvalid('nome')).toBe(true);
  });

  it('finalizar() navega para a rota de fallback quando não há diálogo', async () => {
    const base = create();
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    (base as unknown as { finalizar: (r: unknown, f: string[]) => void }).finalizar(undefined, ['/tenants']);

    expect(navigateSpy).toHaveBeenCalledWith(['/tenants']);
  });
});

describe('FormBase (modo diálogo)', () => {
  it('lê o modo/id dos dados do diálogo em vez da URL', () => {
    const dialogRefStub = {} as DynamicDialogRef;
    const dialogServiceStub = {
      getInstance: vi.fn().mockReturnValue({ data: { mode: 'edicao', id: '7' } }),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', component: TestBlankComponent }]),
        ConfirmationService,
        MessageServicePG,
        { provide: DialogService, useValue: dialogServiceStub },
        { provide: DynamicDialogRef, useValue: dialogRefStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
      ],
    });

    const base = TestBed.runInInjectionContext(() => new TestFormBase());

    expect(base.isDialogMode()).toBe(true);
    expect(base.isEditMode()).toBe(true);
    expect(base.pageId()).toBe('7');
  });

  it('finalizar() fecha o diálogo em vez de navegar', () => {
    const closeSpy = vi.fn();
    const dialogRefStub = { close: closeSpy } as unknown as DynamicDialogRef;
    const dialogServiceStub = { getInstance: vi.fn().mockReturnValue({ data: {} }) };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', component: TestBlankComponent }]),
        ConfirmationService,
        MessageServicePG,
        { provide: DialogService, useValue: dialogServiceStub },
        { provide: DynamicDialogRef, useValue: dialogRefStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
      ],
    });

    const base = TestBed.runInInjectionContext(() => new TestFormBase());
    (base as unknown as { finalizar: (r: unknown, f: string[]) => void }).finalizar('resultado', ['/tenants']);

    expect(closeSpy).toHaveBeenCalledWith('resultado');
  });
});
