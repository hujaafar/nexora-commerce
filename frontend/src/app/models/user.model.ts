/*
 * File purpose: Defines TypeScript contracts for user data.
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

export interface OAuthProviderOption {
  id: 'google' | 'github';
  name: string;
  enabled: boolean;
  authorizationUrl: string;
}

export interface OAuthPending {
  provider: OAuthProviderOption['id'];
  providerName: string;
  mode: 'login' | 'link' | 'register';
  email: string;
  name: string;
  returnUrl: string;
}
