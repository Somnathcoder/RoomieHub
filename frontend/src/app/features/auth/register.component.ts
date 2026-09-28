import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Create your account</h1>
        <p class="subtitle">Then create a new room or wait for your admin to add you to one.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-group">
            <label for="fullName">Full name</label>
            <input id="fullName" type="text" formControlName="fullName" placeholder="Jane Doe" />
            @if (form.controls.fullName.invalid && form.controls.fullName.touched) {
              <div class="field-error">Full name is required.</div>
            }
          </div>
          <div class="form-group">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" placeholder="you@example.com" />
            @if (form.controls.email.invalid && form.controls.email.touched) {
              <div class="field-error">Enter a valid email address.</div>
            }
          </div>
          <div class="form-group">
            <label for="mobileNumber">Mobile number (optional)</label>
            <input id="mobileNumber" type="text" formControlName="mobileNumber" placeholder="9876543210" />
          </div>
          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" placeholder="At least 6 characters" />
            @if (form.controls.password.invalid && form.controls.password.touched) {
              <div class="field-error">Password must be at least 6 characters.</div>
            }
          </div>

          @if (errorMessage()) {
            <div class="field-error mb-2">{{ errorMessage() }}</div>
          }

          <button type="submit" class="btn btn-primary w-full" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Creating account...' : 'Create account' }}
          </button>
        </form>

        <p class="mt-2 text-muted" style="text-align:center; font-size: 13.5px;">
          Already have an account? <a routerLink="/login">Log in</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    mobileNumber: [''],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Account created! Let\'s set up your room.');
        this.router.navigate(['/room/setup']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Registration failed. Please try again.');
      }
    });
  }
}
