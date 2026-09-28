export interface Announcement {
  id: number;
  title: string;
  message: string;
  createdByName: string;
  imageUrl: string | null;
  status: string;
  createdAt: string;
}

export interface AnnouncementCreateRequest {
  title: string;
  message: string;
  imageUrl?: string;
}
