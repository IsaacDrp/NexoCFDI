import { Component, inject } from '@angular/core';
import { ToastService, ToastType } from './toast.service';
import { AppIconComponent } from '../icon/icon.component';

const ICON_MAP: Record<ToastType, string> = {
  success: 'check-circle',
  error:   'x-circle',
  warning: 'alert-triangle',
  info:    'alert-circle',
};

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [AppIconComponent],
  template: `
    <div class="nx-toast-host">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="nx-toast" role="alert" aria-live="assertive">
          <div class="nx-toast-inner">
            <app-icon
              class="nx-toast-icon {{ toast.type }}"
              [name]="iconFor(toast.type)"
              [size]="18"
            />
            <span class="nx-toast-message">{{ toast.message }}</span>
            <button class="nx-toast-close" (click)="toastService.dismiss(toast.id)" aria-label="Cerrar">
              <app-icon name="x" [size]="14" />
            </button>
          </div>
          <div class="nx-toast-progress">
            <div
              class="nx-toast-progress-fill"
              [style.--nx-toast-dur]="toast.duration + 'ms'"
            ></div>
          </div>
        </div>
      }
    </div>
  `,
})
export class ToastHostComponent {
  readonly toastService = inject(ToastService);

  iconFor(type: ToastType): string {
    return ICON_MAP[type];
  }
}
