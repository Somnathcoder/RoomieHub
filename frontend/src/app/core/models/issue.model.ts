export interface Issue {
  id: number;
  title: string;
  description: string | null;
  photoUrl: string | null;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  reportedByName: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';
  resolvedAt: string | null;
  createdAt: string;
}

export interface IssueCreateRequest {
  title: string;
  description?: string;
  priority?: string;
}
