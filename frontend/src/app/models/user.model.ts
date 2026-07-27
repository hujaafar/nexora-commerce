/* BUY-01 learning header
 * File purpose: Defines TypeScript contracts for user data.
 * Learning focus: End-to-end type safety between Angular and backend DTOs.
 */
export type Role = 'CLIENT' | 'SELLER' | 'ADMIN';
export type RegistrationRole = Exclude<Role, 'ADMIN'>;

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: Role;
  avatarUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: UserProfile;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: RegistrationRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateProfileRequest {
  name: string;
  avatarUrl: string | null;
}
