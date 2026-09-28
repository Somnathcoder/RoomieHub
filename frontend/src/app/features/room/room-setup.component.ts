import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RoomService } from '../../core/services/room.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-room-setup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card" style="max-width: 480px;">
        <h1>Set up your room</h1>
        <p class="subtitle">
          Create your room to become its admin. Already been invited? Ask your room admin
          to add you with the email <strong>{{ auth.currentUser()?.email }}</strong> — you'll land
          straight on your dashboard next time you log in.
        </p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-group">
            <label for="roomName">Room / flat name</label>
            <input id="roomName" type="text" formControlName="roomName" placeholder="Sunrise PG - Flat 4B" />
          </div>
          <div class="form-group">
            <label for="address">Address</label>
            <textarea id="address" formControlName="address" placeholder="221 MG Road, Pune"></textarea>
          </div>

          <button type="submit" class="btn btn-primary w-full" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Creating room...' : 'Create room & become admin' }}
          </button>
        </form>

        <button class="btn btn-secondary w-full mt-1" (click)="auth.logout()">Log out</button>
      </div>
    </div>
  `
})
export class RoomSetupComponent {
  private fb = inject(FormBuilder);
  private roomService = inject(RoomService);
  private router = inject(Router);
  private toast = inject(ToastService);
  auth = inject(AuthService);

  loading = signal(false);

  form = this.fb.nonNullable.group({
    roomName: ['', [Validators.required]],
    address: ['', [Validators.required]]
  });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.roomService.create(this.form.getRawValue()).subscribe({
      next: (room) => {
        this.loading.set(false);
        this.auth.refreshSession({ roomId: room.id, roomName: room.roomName, role: 'ADMIN' });
        this.toast.success('Room created! Welcome to ' + room.roomName + '.');
        this.router.navigate(['/dashboard']);
      },
      error: () => this.loading.set(false)
    });
  }
}
