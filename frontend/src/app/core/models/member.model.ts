export interface Member {
  roomMemberId: number;
  userId: number;
  fullName: string;
  email: string;
  mobileNumber: string | null;
  profilePhotoUrl: string | null;
  hasIdProof: boolean;
  roomNumber: string | null;
  bedNumber: string | null;
  joiningDate: string;
  status: 'ACTIVE' | 'INACTIVE' | 'DEACTIVATED';
  role: 'ADMIN' | 'MEMBER' | 'MODERATOR';
  /** Only set in the response to adding a brand-new member (a new user account was created). */
  temporaryPassword?: string | null;
}

export interface MemberCreateRequest {
  fullName: string;
  email: string;
  password?: string;
  mobileNumber?: string;
  roomNumber?: string;
  bedNumber?: string;
  role: string;
  joiningDate?: string;
}

export interface MemberUpdateRequest {
  fullName?: string;
  mobileNumber?: string;
  roomNumber?: string;
  bedNumber?: string;
}

export interface PermissionItem {
  code: string;
  granted: boolean;
}

export interface Permission {
  code: string;
  description: string;
  granted: boolean;
}
