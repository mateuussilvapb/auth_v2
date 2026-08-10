//Angular
import { Component } from '@angular/core';

//Externos
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-toast',
  imports: [ToastModule],
  template: `
    <p-toast
      position="top-right"
      [showTransformOptions]="'translateX(100%)'"
      [breakpoints]="{ '920px': { width: '100%', right: '0', left: '0' } }"
    ></p-toast>
  `,
})
export class Toast {}
