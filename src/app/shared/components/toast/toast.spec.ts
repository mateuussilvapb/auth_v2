import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';

import { Toast } from './toast';

describe('Toast', () => {
  let fixture: ComponentFixture<Toast>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Toast],
      providers: [MessageService],
    }).compileComponents();

    fixture = TestBed.createComponent(Toast);
  });

  it('renderiza o p-toast no canto superior direito', () => {
    fixture.detectChanges();

    const toastEl = fixture.nativeElement.querySelector('p-toast');
    expect(toastEl).not.toBeNull();
  });
});
