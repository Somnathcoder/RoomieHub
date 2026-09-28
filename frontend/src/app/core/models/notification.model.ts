export interface AppNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  referenceId: number | null;
  isRead: boolean;
  createdAt: string;
}
