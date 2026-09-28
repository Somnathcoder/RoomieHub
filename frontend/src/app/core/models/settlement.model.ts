export interface Settlement {
  id: number;
  fromMemberId: number;
  fromMemberName: string;
  toMemberId: number;
  toMemberName: string;
  amount: number;
  status: 'PENDING' | 'PAID';
  paymentDate: string | null;
  verifiedByAdmin: boolean;
  relatedExpenseTitle: string | null;
}

export interface MemberBalance {
  roomMemberId: number;
  memberName: string;
  totalPaid: number;
  totalShare: number;
  balance: number;
}

export interface SettlementSummary {
  fromMemberId: number;
  fromMemberName: string;
  toMemberId: number;
  toMemberName: string;
  netAmount: number;
}
