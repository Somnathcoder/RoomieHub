import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Forgot password</h1>
        <p class="subtitle">Enter your email and we'll send you a reset link.</p>

        @if (submitted()) {
          <p>If an account exists for that email, a password reset link has been sent.</p>
        } @else {
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-group">
              <label for="email">Email</label>
              <input id="email" type="email" formControlName="email" placeholder="you@example.com" />
            </div>
            <button type="submit" class="btn btn-primary w-full" [disabled]="form.invalid || loading()">
              {{ loading() ? 'Sending...' : 'Send reset link' }}
            </button>
          </form>
        }

        <p class="mt-2 text-muted" style="text-align:center; font-size: 13.5px;">
          <a routerLink="/login">Back to log in</a>
        </p>
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  loading = signal(false);
  submitted = signal(false);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: () => { this.loading.set(false); this.submitted.set(true); },
      error: () => { this.loading.set(false); this.submitted.set(true); }
    });
  }
}
