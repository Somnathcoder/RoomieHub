export interface Bill {
  id: number;
  title: string;
  amount: number;
  amountPaid: number;
  dueDate: string;
  paidByName: string | null;
  category: string;
  billPhotoUrl: string | null;
  status: 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';
}

export interface BillCreateRequest {
  title: string;
  amount: number;
  dueDate: string;
  category: string;
  paidByMemberId?: number;
}

export const BILL_CATEGORIES = ['ELECTRICITY', 'WIFI', 'GAS', 'WATER', 'MAINTENANCE', 'OTHER'];
