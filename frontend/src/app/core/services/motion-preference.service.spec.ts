import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MotionPreferenceService } from './motion-preference.service';

describe('Site motion preference', () => {
  let media: EventTarget & { matches: boolean };
  const initialUrl = location.href;

  beforeEach(() => {
    localStorage.removeItem('nexora.motion');
    history.replaceState(null, '', '/products');
    media = Object.assign(new EventTarget(), { matches: true });
    vi.stubGlobal('matchMedia', () => media);
  });
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    localStorage.removeItem('nexora.motion');
    history.replaceState(null, '', initialUrl);
  });

  it('follows device changes by default and lets a visitor opt in without changing the device', () => {
    const motion = TestBed.inject(MotionPreferenceService);
    expect(motion.reduced()).toBe(true);
    media.matches = false;
    media.dispatchEvent(new Event('change'));
    expect(motion.reduced()).toBe(false);
    media.matches = true;
    media.dispatchEvent(new Event('change'));
    motion.setMode('full');
    expect(motion.reduced()).toBe(false);
    expect(media.matches).toBe(true);
    motion.setMode('system');
    expect(motion.reduced()).toBe(true);
    expect(localStorage.getItem('nexora.motion')).toBeNull();
  });

  it('remembers an explicit preview link and preserves a later off choice on refresh', () => {
    history.replaceState(null, '', '/products?motion=full');
    const motion = TestBed.inject(MotionPreferenceService);
    expect(motion.mode()).toBe('full');
    expect(localStorage.getItem('nexora.motion')).toBe('full');
    motion.setMode('reduced');
    expect(location.search).toBe('?motion=reduced');
    TestBed.resetTestingModule();
    history.replaceState(null, '', '/products');
    const reloaded = TestBed.inject(MotionPreferenceService);
    expect(reloaded.mode()).toBe('reduced');
    expect(reloaded.reduced()).toBe(true);
  });

  it('keeps controls usable when the browser denies storage access', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('Denied');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Quota');
    });
    const motion = TestBed.inject(MotionPreferenceService);
    expect(() => motion.setMode('full')).not.toThrow();
    expect(motion.reduced()).toBe(false);
    expect(document.documentElement.dataset['motion']).toBe('full');
  });

  it('ignores unsupported preference values', () => {
    history.replaceState(null, '', '/products?motion=invalid');
    const motion = TestBed.inject(MotionPreferenceService);
    motion.setMode('invalid');
    expect(motion.mode()).toBe('system');
    expect(motion.reduced()).toBe(true);
  });
});
