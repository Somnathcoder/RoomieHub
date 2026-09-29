import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Set a new password</h1>
        <p class="subtitle">
          @if (isForced()) {
            You're logging in with a temporary password. Choose a new one to continue.
          } @else {
            Enter your current password and choose a new one.
          }
        </p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-group">
            <label for="currentPassword">Current password</label>
            <input id="currentPassword" type="password" formControlName="currentPassword" placeholder="••••••••" />
          </div>
          <div class="form-group">
            <label for="newPassword">New password</label>
            <input id="newPassword" type="password" formControlName="newPassword" placeholder="At least 8 characters, with a letter and a number" />
            @if (form.controls.newPassword.invalid && form.controls.newPassword.touched) {
              <div class="field-error">Password must be at least 8 characters and include a letter and a number.</div>
            }
          </div>
          <div class="form-group">
            <label for="confirmPassword">Confirm new password</label>
            <input id="confirmPassword" type="password" formControlName="confirmPassword" placeholder="••••••••" />
            @if (form.errors?.['mismatch'] && form.controls.confirmPassword.touched) {
              <div class="field-error">Passwords do not match.</div>
            }
          </div>

          @if (errorMessage()) { <div class="field-error mb-2">{{ errorMessage() }}</div> }

          <button type="submit" class="btn btn-primary w-full" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Updating...' : 'Update password' }}
          </button>
        </form>
      </div>
    </div>
  `
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  loading = signal(false);
  errorMessage = signal<string | null>(null);
  isForced = signal(!!this.auth.currentUser()?.mustChangePassword);

  form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', Validators.required]
  }, { validators: (group) => group.get('newPassword')?.value === group.get('confirmPassword')?.value ? null : { mismatch: true } });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    const v = this.form.getRawValue();
    this.auth.changePassword(v.currentPassword, v.newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.auth.refreshSession({ mustChangePassword: false });
        this.toast.success('Password updated.');
        const user = this.auth.currentUser();
        this.router.navigate([user?.roomId ? '/dashboard' : '/room/setup']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Could not update password. Please try again.');
      }
    });
  }
}
