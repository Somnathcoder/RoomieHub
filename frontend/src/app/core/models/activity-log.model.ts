export interface ActivityLog {
  id: number;
  userName: string;
  action: string;
  module: string;
  referenceId: number | null;
  description: string | null;
  createdAt: string;
}
