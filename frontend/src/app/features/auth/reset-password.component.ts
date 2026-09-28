import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Reset password</h1>
        <p class="subtitle">Enter the token from your reset email and choose a new password.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-group">
            <label for="token">Reset token</label>
            <input id="token" type="text" formControlName="token" />
          </div>
          <div class="form-group">
            <label for="newPassword">New password</label>
            <input id="newPassword" type="password" formControlName="newPassword" placeholder="At least 6 characters" />
          </div>

          @if (errorMessage()) { <div class="field-error mb-2">{{ errorMessage() }}</div> }

          <button type="submit" class="btn btn-primary w-full" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Resetting...' : 'Reset password' }}
          </button>
        </form>

        <p class="mt-2 text-muted" style="text-align:center; font-size: 13.5px;">
          <a routerLink="/login">Back to log in</a>
        </p>
      </div>
    </div>
  `
})
export class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    token: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) this.form.patchValue({ token });
  }

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    const v = this.form.getRawValue();
    this.auth.resetPassword(v.token, v.newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Password reset. Please log in.');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Could not reset password. The token may be invalid or expired.');
      }
    });
  }
}
