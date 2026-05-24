import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  show(message: string, type: ToastType = 'info', duration = 4000): void {
    const id = Math.random().toString(36).slice(2, 9);
    this._toasts.update((t) => [...t.slice(-3), { id, type, message, duration }]);
    setTimeout(() => this.dismiss(id), duration);
  }

  success(message: string, duration = 3000): void { this.show(message, 'success', duration); }
  error(message: string, duration = 5000): void   { this.show(message, 'error', duration);   }
  warning(message: string, duration = 4000): void { this.show(message, 'warning', duration); }
  info(message: string, duration = 4000): void    { this.show(message, 'info', duration);    }

  dismiss(id: string): void {
    this._toasts.update((t) => t.filter((x) => x.id !== id));
  }
}
