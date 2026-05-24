import { Component, input } from '@angular/core';

@Component({
  selector: 'app-icon',
  standalone: true,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      [style.display]="'block'"
    >
      <use [attr.href]="'#nx-' + name()" />
    </svg>
  `,
  styles: [`:host { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }`],
})
export class AppIconComponent {
  name = input.required<string>();
  size = input<number>(18);
}
