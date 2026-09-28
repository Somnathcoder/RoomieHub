import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Welcome back</h1>
        <p class="subtitle">Log in to manage your room.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-group">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" placeholder="you@example.com" />
            @if (form.controls.email.invalid && form.controls.email.touched) {
              <div class="field-error">Enter a valid email address.</div>
            }
          </div>
          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" placeholder="••••••••" />
            @if (form.controls.password.invalid && form.controls.password.touched) {
              <div class="field-error">Password is required.</div>
            }
            <div class="mt-1" style="text-align:right;"><a routerLink="/forgot-password" style="font-size:12.5px;">Forgot password?</a></div>
          </div>

          @if (errorMessage()) {
            <div class="field-error mb-2">{{ errorMessage() }}</div>
          }

          <button type="submit" class="btn btn-primary w-full" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Logging in...' : 'Log in' }}
          </button>
        </form>

        <p class="mt-2 text-muted" style="text-align:center; font-size: 13.5px;">
          Don't have an account? <a routerLink="/register">Create one</a>
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.toast.success('Welcome back, ' + res.data.fullName.split(' ')[0] + '!');
        this.router.navigate([res.data.roomId ? '/dashboard' : '/room/setup']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Login failed. Please try again.');
      }
    });
  }
}
