import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LayoutBasePages } from './layout-base-pages';

describe('LayoutBasePages', () => {
  let fixture: ComponentFixture<LayoutBasePages>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LayoutBasePages],
    }).compileComponents();

    fixture = TestBed.createComponent(LayoutBasePages);
  });

  it('exibe título e subtítulo', () => {
    fixture.componentRef.setInput('title', 'Tenants');
    fixture.componentRef.setInput('subtitle', 'Gerencie os tenants da plataforma');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Tenants');
    expect(text).toContain('Gerencie os tenants da plataforma');
  });

  it('não renderiza botão de ação sem buttonActionLabel', () => {
    fixture.componentRef.setInput('title', 'Tenants');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('p-button')).toBeNull();
  });

  it('emite actionButtonClick ao clicar no botão de ação', () => {
    fixture.componentRef.setInput('title', 'Tenants');
    fixture.componentRef.setInput('buttonActionLabel', 'Novo tenant');
    fixture.detectChanges();

    let emitted = false;
    fixture.componentInstance.actionButtonClick.subscribe(() => (emitted = true));

    fixture.componentInstance.onButtonActionClick();

    expect(emitted).toBe(true);
  });
});
