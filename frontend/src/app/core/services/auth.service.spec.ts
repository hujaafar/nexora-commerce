import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { seller } from '../../testing/marketplace-fixtures';
import { AuthService } from './auth.service';

describe('Session handling', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    localStorage.clear();
  });
  it.each([
    '{invalid',
    JSON.stringify({ accessToken: 'expired', user: seller, expiresAt: '2000-01-01' }),
    JSON.stringify({ accessToken: 'invalid-date', user: seller, expiresAt: 'invalid' }),
  ])('discards invalid persisted sessions: %s', (session) => {
    localStorage.setItem('nexora.session', session);
    expect(TestBed.inject(AuthService).isAuthenticated()).toBe(false);
    expect(localStorage.getItem('nexora.session')).toBeNull();
  });
  it('stores a successful login, updates roles/profile and clears credentials on logout', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    auth.login({ email: seller.email, password: 'test-password' }).subscribe();
    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    login.flush({ accessToken: 'test-token', expiresAt: '2099-01-01T00:00:00Z', user: seller });
    expect(auth.token()).toBe('test-token');
    expect(auth.isSeller()).toBe(true);
    expect(auth.isClient()).toBe(false);
    expect(auth.isAdmin()).toBe(false);
    auth.logout();
    expect(auth.token()).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
    expect(localStorage.length).toBe(0);
  });
  it('registers a client and refreshes profile data', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    const request = {
      name: 'Test client',
      email: 'client@example.com',
      password: 'test-password',
      role: 'CLIENT' as const,
    };
    auth.register(request).subscribe();
    const registration = http.expectOne('/api/auth/register');
    expect(registration.request.body).toEqual(request);
    registration.flush({
      accessToken: 'test-token',
      expiresAt: '2099-01-01T00:00:00Z',
      user: { ...seller, role: 'CLIENT' },
    });
    expect(auth.isClient()).toBe(true);
    auth.refreshProfile().subscribe();
    http.expectOne('/api/me').flush({ ...seller, name: 'Updated client', role: 'CLIENT' });
    expect(auth.currentUser()?.name).toBe('Updated client');
  });
});
