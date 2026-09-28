export interface Split {
  roomMemberId: number;
  memberName: string;
  shareAmount: number;
}

export interface Expense {
  id: number;
  title: string;
  description: string | null;
  totalAmount: number;
  category: string;
  paidByMemberId: number;
  paidByName: string;
  expenseDate: string;
  receiptPhotoUrl: string | null;
  splitType: 'EQUAL' | 'CUSTOM';
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdByName: string;
  approvedByName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  splits: Split[];
}

export interface SplitItemRequest {
  roomMemberId: number;
  amount: number;
}

export interface ExpenseCreateRequest {
  title: string;
  description?: string;
  totalAmount: number;
  category: string;
  paidByMemberId: number;
  expenseDate: string;
  splitType: 'EQUAL' | 'CUSTOM';
  customSplits?: SplitItemRequest[];
  equalSplitMemberIds?: number[];
}

export const EXPENSE_CATEGORIES = ['GROCERY', 'ELECTRICITY', 'WIFI', 'GAS', 'WATER', 'MAINTENANCE', 'FOOD', 'OTHER'];
