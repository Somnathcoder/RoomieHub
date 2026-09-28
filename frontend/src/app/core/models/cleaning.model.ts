export interface CleaningSchedule {
  id: number;
  title: string;
  cleaningDate: string;
  cleaningTime: string | null;
  assignedToMemberId: number;
  assignedToName: string;
  taskType: string;
  description: string | null;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  completedAt: string | null;
}

export interface CleaningScheduleCreateRequest {
  title: string;
  cleaningDate: string;
  cleaningTime?: string;
  assignedToMemberId: number;
  taskType: string;
  description?: string;
}

export interface CleaningRotation {
  id: number;
  taskType: string;
  memberNames: string[];
  memberIds: number[];
  intervalDays: number;
  startDate: string;
  active: boolean;
  currentIndex: number;
}

export interface CleaningRotationCreateRequest {
  taskType: string;
  memberIds: number[];
  intervalDays: number;
  startDate: string;
}

export const CLEANING_TASK_TYPES = ['KITCHEN', 'BATHROOM', 'ROOM', 'GARBAGE', 'COMMON_AREA', 'OTHER'];
