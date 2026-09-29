export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  fullName: string;
  email: string;
  roomId: number | null;
  roomName: string | null;
  role: string | null;
  mustChangePassword: boolean;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  mobileNumber?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
