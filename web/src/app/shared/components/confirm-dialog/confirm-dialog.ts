//Angular
import { Component } from '@angular/core';

//Externos
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'app-confirm-dialog',
  imports: [ConfirmDialogModule],
  template: `<p-confirmdialog [style]="{ width: '28rem' }" />`,
})
export class ConfirmDialog {}
