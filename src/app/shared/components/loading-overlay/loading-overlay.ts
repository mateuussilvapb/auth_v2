//Angular
import { Component, inject } from '@angular/core';

//Aplicação
import { LoadingOverlayService } from '../../services/loading-overlay.service';

@Component({
  selector: 'app-loading-overlay',
  imports: [],
  templateUrl: './loading-overlay.html',
  styleUrl: './loading-overlay.scss',
})
export class LoadingOverlay {
  protected readonly loadingOverlay = inject(LoadingOverlayService);
}
