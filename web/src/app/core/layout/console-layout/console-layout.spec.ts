import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';

import { ConsoleLayout } from './console-layout';
import { ConsoleAuthService } from '../../services/console-auth.service';

describe('ConsoleLayout', () => {
  let fixture: ComponentFixture<ConsoleLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsoleLayout],
      providers: [
        provideRouter([]),
        MessageService,
        ConfirmationService,
        { provide: ConsoleAuthService, useValue: { logout: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConsoleLayout);
  });

  it('renderiza topbar, sidebar e o router-outlet', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-topbar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-sidebar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.layout-main-container')).not.toBeNull();
  });
});
