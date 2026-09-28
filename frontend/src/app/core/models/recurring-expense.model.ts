export interface RecurringExpense {
  id: number;
  title: string;
  category: string;
  amount: number;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
  splitType: string;
  startDate: string;
  nextDueDate: string;
  active: boolean;
  paidByName: string;
}

export interface RecurringExpenseCreateRequest {
  title: string;
  category: string;
  amount: number;
  frequency: string;
  splitType?: string;
  startDate: string;
  paidByMemberId: number;
}
