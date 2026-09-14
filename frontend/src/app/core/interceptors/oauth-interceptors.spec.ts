import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { authInterceptor } from './auth.interceptor';
import { errorInterceptor } from './error.interceptor';

describe('OAuth handshake and application sessions', () => {
  const logout = vi.fn();
  const navigate = vi.fn().mockResolvedValue(true);
  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => 'old-session-token', logout } },
        { provide: Router, useValue: { navigate } },
        { provide: NotificationService, useValue: { error: vi.fn() } },
      ],
    });
  });
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it.each(['pending', 'complete'])(
    'keeps %s errors on the OAuth screen without sending an old JWT',
    (endpoint) => {
      TestBed.inject(HttpClient)
        .post(`/api/auth/oauth2/${endpoint}`, {})
        .subscribe({ error: () => undefined });
      const request = TestBed.inject(HttpTestingController).expectOne(
        `/api/auth/oauth2/${endpoint}`,
      );
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush(
        { message: 'Please retry social sign-in.' },
        { status: 401, statusText: 'Unauthorized' },
      );
      expect(logout).not.toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalled();
    },
  );

  it('still logs out an expired ordinary API session', () => {
    TestBed.inject(HttpClient)
      .get('/api/me')
      .subscribe({ error: () => undefined });
    const request = TestBed.inject(HttpTestingController).expectOne('/api/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer old-session-token');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
