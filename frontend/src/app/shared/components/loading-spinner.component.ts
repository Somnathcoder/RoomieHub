import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `
    <div class="loading-state">
      <div class="spinner"></div>
      <span>{{ label }}</span>
    </div>
  `
})
export class LoadingSpinnerComponent {
  @Input() label = 'Loading...';
}
