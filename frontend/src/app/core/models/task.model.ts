export interface Task {
  id: number;
  title: string;
  description: string | null;
  assignedToMemberId: number;
  assignedToName: string;
  dueDate: string | null;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  completedAt: string | null;
}

export interface TaskCreateRequest {
  title: string;
  description?: string;
  assignedToMemberId: number;
  dueDate?: string;
  priority?: string;
}
