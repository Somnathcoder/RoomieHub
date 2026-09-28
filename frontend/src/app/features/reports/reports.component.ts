import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../core/services/dashboard.service';
import { ReportService, ReportType } from '../../core/services/report.service';
import { ToastService } from '../../core/services/toast.service';
import { ExpenseAnalytics } from '../../core/models/dashboard.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';

interface ReportOption {
  type: ReportType;
  label: string;
  description: string;
}

const REPORTS: ReportOption[] = [
  {
    type: 'monthly-expense',
    label: 'Monthly expense report',
    description: 'All approved expenses grouped by month.'
  },
  {
    type: 'member-contribution',
    label: 'Member contribution report',
    description: 'Who paid vs. who owes, per member.'
  },
  {
    type: 'pending-settlement',
    label: 'Pending settlements report',
    description: 'Every settlement still awaiting payment.'
  },
  {
    type: 'bill-report',
    label: 'Bill report',
    description: 'All bills, their status and amounts.'
  }
];

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, LoadingSpinnerComponent],
  template: `
    <div class="page">

      <div class="page-header">
        <div>
          <h1>Reports &amp; analytics</h1>
          <div class="page-subtitle">
            Download data or review expense trends.
          </div>
        </div>
      </div>

      @if (loading()) {

        <app-loading-spinner label="Loading analytics..." />

      } @else {

        @if (analytics(); as a) {

          <!-- Summary Cards -->
          <div class="grid grid-cols-4 mb-2">

            <div class="stat-card">
              <div class="label">This month</div>
              <div class="value">
                ₹{{ a.currentMonthTotal.toFixed(2) }}
              </div>
            </div>

            <div class="stat-card">
              <div class="label">Last month</div>
              <div class="value">
                ₹{{ a.previousMonthTotal.toFixed(2) }}
              </div>
            </div>

            <div class="stat-card">
              <div class="label">Total paid</div>
              <div class="value positive">
                ₹{{ a.totalPaid.toFixed(2) }}
              </div>
            </div>

            <div class="stat-card">
              <div class="label">Total pending</div>
              <div class="value negative">
                ₹{{ a.totalPending.toFixed(2) }}
              </div>
            </div>

          </div>


          <!-- Analytics -->
          <div class="grid grid-cols-2 mb-2">

            <!-- By Category -->
            <div class="card">

              <div class="card-title">
                By category
              </div>

              @for (c of a.categoryWise; track c.label) {

                <div
                  class="flex-between"
                  style="padding:6px 0; border-bottom:1px solid var(--color-border);"
                >
                  <span>{{ c.label }}</span>

                  <strong>
                    ₹{{ c.amount.toFixed(2) }}
                  </strong>
                </div>

              }

            </div>


            <!-- By Member -->
            <div class="card">

              <div class="card-title">
                By member
              </div>

              @for (c of a.memberContribution; track c.label) {

                <div
                  class="flex-between"
                  style="padding:6px 0; border-bottom:1px solid var(--color-border);"
                >
                  <span>{{ c.label }}</span>

                  <strong>
                    ₹{{ c.amount.toFixed(2) }}
                  </strong>
                </div>

              }

            </div>


            <!-- Last 6 Months -->
            <div
              class="card"
              style="grid-column: 1 / -1;"
            >

              <div class="card-title">
                Last 6 months
              </div>

              @for (c of a.monthlyExpense; track c.label) {

                <div
                  class="flex-between"
                  style="padding:6px 0; border-bottom:1px solid var(--color-border);"
                >
                  <span>{{ c.label }}</span>

                  <strong>
                    ₹{{ c.amount.toFixed(2) }}
                  </strong>
                </div>

              }

            </div>

          </div>

        }

      }


      <!-- Reports Download Section -->
      <div class="grid grid-cols-2">

        @for (r of reports; track r.type) {

          <div class="card">

            <div class="card-title">
              {{ r.label }}
            </div>

            <p class="text-muted">
              {{ r.description }}
            </p>

            <div class="flex-gap">

              <button
                class="btn btn-secondary btn-sm"
                (click)="download(r, 'csv')"
              >
                Download CSV
              </button>

              <button
                class="btn btn-secondary btn-sm"
                (click)="download(r, 'pdf')"
              >
                Download PDF
              </button>

            </div>

          </div>

        }

      </div>

    </div>
  `
})
export class ReportsComponent {

  private dashboardService = inject(DashboardService);

  private reportService = inject(ReportService);

  private toast = inject(ToastService);


  reports = REPORTS;

  analytics = signal<ExpenseAnalytics | null>(null);

  loading = signal(true);


  constructor() {

    this.dashboardService.getAnalytics().subscribe({

      next: (a) => {

        this.analytics.set(a);

        this.loading.set(false);

      },

      error: () => {

        this.loading.set(false);

        this.toast.error('Could not load analytics.');

      }

    });

  }


  download(
    r: ReportOption,
    format: 'csv' | 'pdf'
  ) {

    this.reportService.download(r.type, format).subscribe({

      next: (blob) => {

        this.reportService.triggerDownload(
          blob,
          `${r.type}.${format}`
        );

      },

      error: () => {

        this.toast.error(
          'Could not generate report.'
        );

      }

    });

  }

}