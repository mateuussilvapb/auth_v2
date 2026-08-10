import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';

import { PublicLayout } from './public-layout';

describe('PublicLayout', () => {
  let fixture: ComponentFixture<PublicLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicLayout],
      providers: [provideRouter([]), MessageService],
    }).compileComponents();

    fixture = TestBed.createComponent(PublicLayout);
  });

  it('renderiza o card centralizado com o router-outlet, sem topbar/sidebar', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.public-layout-card')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-topbar')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-sidebar')).toBeNull();
  });
});
