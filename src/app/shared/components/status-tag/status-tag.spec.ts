import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatusTag } from './status-tag';

describe('StatusTag', () => {
  let fixture: ComponentFixture<StatusTag>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusTag],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusTag);
  });

  it.each([
    ['ACTIVE', 'success', 'Ativo'],
    ['INACTIVE', 'secondary', 'Inativo'],
    ['BLOCKED', 'danger', 'Bloqueado'],
    ['DISABLED', 'secondary', 'Desabilitado'],
  ])('mapeia %s para severidade %s e rótulo %s', (status, severity, label) => {
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();

    expect(fixture.componentInstance.severity()).toBe(severity);
    expect(fixture.componentInstance.label()).toBe(label);
  });

  it('status desconhecido cai em secondary com o valor cru como rótulo', () => {
    fixture.componentRef.setInput('status', 'ALGO_NOVO');
    fixture.detectChanges();

    expect(fixture.componentInstance.severity()).toBe('secondary');
    expect(fixture.componentInstance.label()).toBe('ALGO_NOVO');
  });
});
