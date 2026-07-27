/*
 * File purpose: Centralizes auth API or UI state operations.
 */
import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
  UserProfile
} from '../../models/user.model';

interface StoredSession {
  accessToken: string;
  expiresAt: string;
  user: UserProfile;
}

const SESSION_KEY = 'buy01.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionSignal = signal<StoredSession | null>(this.readSession());

  readonly currentUser = computed(() => this.sessionSignal()?.user ?? null);
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  readonly isSeller = computed(() => this.currentUser()?.role === 'SELLER');
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((response) => this.storeSession(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, request)
      .pipe(tap((response) => this.storeSession(response)));
  }

  refreshProfile(): Observable<UserProfile> {
    return this.http
      .get<UserProfile>(`${environment.apiUrl}/me`)
      .pipe(tap((profile) => this.updateUser(profile)));
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    return this.http
      .put<UserProfile>(`${environment.apiUrl}/me`, request)
      .pipe(tap((profile) => this.updateUser(profile)));
  }

  token(): string | null {
    return this.sessionSignal()?.accessToken ?? null;
  }

  logout(): void {
    this.sessionSignal.set(null);
    localStorage.removeItem(SESSION_KEY);
  }

  private storeSession(response: AuthResponse): void {
    const session: StoredSession = {
      accessToken: response.accessToken,
      expiresAt: response.expiresAt,
      user: response.user
    };
    this.sessionSignal.set(session);
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  private updateUser(user: UserProfile): void {
    const session = this.sessionSignal();
    if (!session) {
      return;
    }
    const updated = { ...session, user };
    this.sessionSignal.set(updated);
    localStorage.setItem(SESSION_KEY, JSON.stringify(updated));
  }

  private readSession(): StoredSession | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as StoredSession;
      if (
        !session.accessToken ||
        !session.user ||
        new Date(session.expiresAt).getTime() <= Date.now()
      ) {
        localStorage.removeItem(SESSION_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
