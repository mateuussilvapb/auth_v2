import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  let fixture: ComponentFixture<Sidebar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Sidebar);
    fixture.detectChanges();
  });

  it('exibe o link para Tenants', () => {
    const link = fixture.nativeElement.querySelector('a[href="/console/tenants"]');

    expect(link).not.toBeNull();
    expect((link.textContent as string)).toContain('Tenants');
  });
});
