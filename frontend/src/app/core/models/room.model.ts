export interface Room {
  id: number;
  roomName: string;
  address: string;
  createdByName: string;
  createdAt: string;
  status: string;
  memberCount: number;
}

export interface RoomCreateRequest {
  roomName: string;
  address: string;
}
