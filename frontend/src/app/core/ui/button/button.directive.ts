import { Directive, computed, input } from '@angular/core';
import { booleanAttribute } from '@angular/core';

@Directive({
  selector: 'button[nx-btn], a[nx-btn]',
  standalone: true,
  host: {
    'class': 'nx-btn',
    '[class.nx-btn-solid]':   "variant() === 'solid'",
    '[class.nx-btn-outline]': "variant() === 'outline'",
    '[class.nx-btn-ghost]':   "variant() === 'ghost'",
    '[class.nx-btn-icon]':    "variant() === 'icon'",
    '[class.nx-btn-primary]': "color() === 'primary'",
    '[class.nx-btn-success]': "color() === 'success'",
    '[class.nx-btn-danger]':  "color() === 'danger'",
    '[class.nx-btn-warning]': "color() === 'warning'",
    '[class.nx-btn-orange]':  "color() === 'orange'",
    '[class.nx-btn-neutral]': "color() === 'neutral'",
    '[class.nx-btn-lg]':      "size() === 'lg'",
    '[class.nx-btn-sm]':      "size() === 'sm'",
    '[attr.disabled]':        '_isDisabled() || null',
  },
})
export class AppBtnDirective {
  variant = input<'solid' | 'outline' | 'ghost' | 'icon'>('solid');
  color   = input<'primary' | 'success' | 'danger' | 'warning' | 'orange' | 'neutral'>('primary');
  size    = input<'base' | 'lg' | 'sm'>('base');
  disabled = input(false, { transform: booleanAttribute });
  loading  = input(false, { transform: booleanAttribute });

  _isDisabled = computed(() => this.disabled() || this.loading());
}
