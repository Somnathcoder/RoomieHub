export interface Message {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number | null;
  receiverName: string | null;
  content: string;
  isRead: boolean;
  type: 'PUBLIC' | 'PRIVATE';
  createdAt: string;
}
