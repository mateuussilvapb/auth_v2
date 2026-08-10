import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoadingOverlay } from './loading-overlay';
import { LoadingOverlayService } from '../../services/loading-overlay.service';

describe('LoadingOverlay', () => {
  let fixture: ComponentFixture<LoadingOverlay>;
  let service: LoadingOverlayService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingOverlay],
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingOverlay);
    service = TestBed.inject(LoadingOverlayService);
  });

  it('não renderiza nada quando o serviço está inativo', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.loading-overlay')).toBeNull();
  });

  it('renderiza o overlay quando o serviço sinaliza carregamento', () => {
    service.show();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.loading-overlay')).not.toBeNull();
  });
});
