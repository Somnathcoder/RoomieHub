import { Bill } from './bill.model';
import { Task } from './task.model';
import { Expense } from './expense.model';
import { Announcement } from './announcement.model';
import { CleaningSchedule } from './cleaning.model';
import { ActivityLog } from './activity-log.model';

export interface MemberDashboard {
  totalMonthlyExpenses: number;
  myContribution: number;
  myPendingAmount: number;
  upcomingBills: Bill[];
  myPendingTasks: Task[];
  nextCleaningDate: string | null;
  unreadMessages: number;
  unreadNotifications: number;
  recentExpenses: Expense[];
  recentAnnouncements: Announcement[];
}

export interface AdminDashboard {
  totalMembers: number;
  activeMembers: number;
  monthlyExpenses: number;
  pendingPayments: number;
  pendingExpenses: number;
  pendingTasks: number;
  upcomingBills: Bill[];
  upcomingCleaning: CleaningSchedule[];
  recentActivities: ActivityLog[];
  openIssues: number;
}

export interface NamedAmount {
  label: string;
  amount: number;
}

export interface ExpenseAnalytics {
  monthlyExpense: NamedAmount[];
  categoryWise: NamedAmount[];
  memberContribution: NamedAmount[];
  currentMonthTotal: number;
  previousMonthTotal: number;
  totalPaid: number;
  totalPending: number;
}
