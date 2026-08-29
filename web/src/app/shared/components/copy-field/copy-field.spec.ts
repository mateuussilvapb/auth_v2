import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { Mock } from 'vitest';

import { CopyField } from './copy-field';
import { MessageService } from '../../services/message.service';

describe('CopyField', () => {
  let fixture: ComponentFixture<CopyField>;
  let messageServiceStub: { showSuccess: Mock };
  let writeTextSpy: Mock;

  beforeEach(async () => {
    messageServiceStub = { showSuccess: vi.fn() };
    writeTextSpy = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal('navigator', { clipboard: { writeText: writeTextSpy } });

    await TestBed.configureTestingModule({
      imports: [CopyField],
      providers: [{ provide: MessageService, useValue: messageServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(CopyField);
    fixture.componentRef.setInput('value', 'super-secreto');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('exibe o valor em texto claro quando não mascarado', () => {
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).toContain('super-secreto');
  });

  it('mascara o valor por padrão quando masked=true, sem exibir o texto claro', () => {
    fixture.componentRef.setInput('masked', true);
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).not.toContain('super-secreto');
  });

  it('revela o valor ao alternar toggleReveal()', () => {
    fixture.componentRef.setInput('masked', true);
    fixture.detectChanges();

    fixture.componentInstance.toggleReveal();
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).toContain('super-secreto');
  });

  it('copy() escreve na área de transferência e mostra toast de sucesso', async () => {
    fixture.detectChanges();

    await fixture.componentInstance.copy();

    expect(writeTextSpy).toHaveBeenCalledWith('super-secreto');
    expect(messageServiceStub.showSuccess).toHaveBeenCalled();
  });
});
