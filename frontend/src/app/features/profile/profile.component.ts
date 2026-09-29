import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FileUploadService } from '../../core/services/file-upload.service';
import { Member } from '../../core/models/member.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="page" style="max-width: 640px;">

      <!-- Page Header -->
      <div class="page-header">
        <div>
          <h1>My profile</h1>

          <div class="page-subtitle">
            {{ auth.currentUser()?.roomName }}
            ·
            {{ auth.currentUser()?.role }}
          </div>
        </div>
      </div>


      <!-- Loading -->
      @if (loading()) {

        <app-loading-spinner
          label="Loading profile..."
        />

      } @else {

        <!-- Profile exists -->
        @if (me(); as m) {

          <!-- Profile Card -->
          <div class="card mb-2">

            <!-- Profile Photo -->
            <div class="flex-gap mb-2">

              <span
                class="avatar"
                style="width:56px; height:56px; font-size:18px;"
              >

                @if (m.profilePhotoUrl) {

                  <img
                    [src]="resolveUrl(m.profilePhotoUrl)"
                    alt=""
                  />

                } @else {

                  {{ initials(m.fullName) }}

                }

              </span>


              <!-- Change Photo -->
              <div>

                <label
                  class="btn btn-secondary btn-sm"
                  style="cursor:pointer;"
                >

                  Change photo

                  <input
                    type="file"
                    accept="image/*"
                    hidden
                    (change)="uploadPhoto($event)"
                  />

                </label>

              </div>

            </div>


            <!-- Profile Form -->
            <form
              [formGroup]="form"
              (ngSubmit)="submit()"
            >

              <!-- Full Name + Mobile -->
              <div class="form-row">

                <div class="form-group">

                  <label>
                    Full name
                  </label>

                  <input
                    type="text"
                    formControlName="fullName"
                  />

                </div>


                <div class="form-group">

                  <label>
                    Mobile number
                  </label>

                  <input
                    type="text"
                    formControlName="mobileNumber"
                  />

                </div>

              </div>


              <!-- Room + Bed -->
              <div class="form-row">

                <div class="form-group">

                  <label>
                    Room number
                  </label>

                  <input
                    type="text"
                    formControlName="roomNumber"
                  />

                </div>


                <div class="form-group">

                  <label>
                    Bed number
                  </label>

                  <input
                    type="text"
                    formControlName="bedNumber"
                  />

                </div>

              </div>


              <!-- Email -->
              <div class="form-group">

                <label>
                  Email
                </label>

                <input
                  type="email"
                  [value]="m.email"
                  disabled
                />

              </div>


              <!-- Save -->
              <button
                type="submit"
                class="btn btn-primary"
                [disabled]="form.invalid || saving()"
              >

                {{
                  saving()
                    ? 'Saving...'
                    : 'Save changes'
                }}

              </button>

            </form>

          </div>


          <!-- ID Proof Card -->
          <div class="card">

            <div class="card-title">
              ID proof
            </div>


            <p class="text-muted">
              Your ID proof is only visible to you and the room admin.
            </p>


            <!-- Existing ID Proof -->
            @if (m.hasIdProof) {

              <button
                type="button"
                class="btn-link"
                (click)="viewIdProof(m)"
              >
                View uploaded ID proof
              </button>

            }


            <!-- Upload ID Proof -->
            <div class="mt-1">

              <label
                class="btn btn-secondary btn-sm"
                style="cursor:pointer;"
              >

                {{
                  m.hasIdProof
                    ? 'Replace ID proof'
                    : 'Upload ID proof'
                }}

                <input
                  type="file"
                  accept="image/*,.pdf"
                  hidden
                  (change)="uploadIdProof($event)"
                />

              </label>

            </div>

          </div>

        }

      }

    </div>
  `
})
export class ProfileComponent {

  private fb = inject(FormBuilder);

  memberService = inject(MemberService);

  auth = inject(AuthService);

  private toast = inject(ToastService);

  private fileUpload = inject(FileUploadService);


  // Current logged-in member
  me = signal<Member | null>(null);

  // Loading state
  loading = signal(true);

  // Saving state
  saving = signal(false);


  // Profile form
  form = this.fb.nonNullable.group({

    fullName: [
      '',
      Validators.required
    ],

    mobileNumber: [
      ''
    ],

    roomNumber: [
      ''
    ],

    bedNumber: [
      ''
    ]

  });


  constructor() {
    this.refresh();
  }


  // Resolve uploaded file URL
  resolveUrl(path: string | null) {
    return this.fileUpload.resolveUrl(path);
  }


  // Generate initials from full name
  initials(name: string) {

    return name
      .split(' ')
      .map(p => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();

  }


  // Load current member
  refresh() {

    this.loading.set(true);

    this.memberService.list().subscribe({

      next: (list) => {

        const self =
          list.find(
            m =>
              m.userId ===
              this.auth.currentUser()?.userId
          ) ?? null;


        this.me.set(self);


        if (self) {

          this.form.reset({

            fullName:
              self.fullName,

            mobileNumber:
              self.mobileNumber ?? '',

            roomNumber:
              self.roomNumber ?? '',

            bedNumber:
              self.bedNumber ?? ''

          });

        }


        this.loading.set(false);

      },


      error: () => {

        this.loading.set(false);

      }

    });

  }


  // Save profile
  submit() {

    const m = this.me();


    if (!m || this.form.invalid) {
      return;
    }


    this.saving.set(true);


    const v =
      this.form.getRawValue();


    this.memberService
      .update(

        m.roomMemberId,

        {
          fullName:
            v.fullName,

          mobileNumber:
            v.mobileNumber || undefined,

          roomNumber:
            v.roomNumber || undefined,

          bedNumber:
            v.bedNumber || undefined
        }

      )
      .subscribe({

        next: () => {

          this.saving.set(false);


          this.auth.refreshSession({
            fullName: v.fullName
          });


          this.toast.success(
            'Profile updated.'
          );


          this.refresh();

        },


        error: (err) => {

          this.saving.set(false);


          this.toast.error(
            err.error?.message ??
            'Could not update profile.'
          );

        }

      });

  }


  // Upload profile photo
  uploadPhoto(event: Event) {

    const m = this.me();


    const file =
      (event.target as HTMLInputElement)
        .files?.[0];


    if (!m || !file) {
      return;
    }


    this.memberService
      .uploadPhoto(
        m.roomMemberId,
        file
      )
      .subscribe({

        next: () => {

          this.toast.success(
            'Photo updated.'
          );


          this.refresh();

        },


        error: (err) => {

          this.toast.error(
            err.error?.message ??
            'Upload failed.'
          );

        }

      });

  }


  // Upload ID proof
  uploadIdProof(event: Event) {

    const m = this.me();


    const file =
      (event.target as HTMLInputElement)
        .files?.[0];


    if (!m || !file) {
      return;
    }


    this.memberService
      .uploadIdProof(
        m.roomMemberId,
        file
      )
      .subscribe({

        next: () => {

          this.toast.success(
            'ID proof uploaded.'
          );


          this.refresh();

        },


        error: (err) => {

          this.toast.error(
            err.error?.message ??
            'Upload failed.'
          );

        }

      });

  }


  // View ID proof (requires auth - fetched as a blob rather than a plain link)
  viewIdProof(m: Member) {

    const tab = window.open('', '_blank');

    this.memberService
      .viewIdProof(m.roomMemberId)
      .subscribe({

        next: (blob) => {
          if (tab) tab.location.href = URL.createObjectURL(blob);
        },

        error: (err) => {
          if (tab) tab.close();
          this.toast.error(
            err.error?.message ??
            'Could not load ID proof.'
          );
        }

      });

  }

}