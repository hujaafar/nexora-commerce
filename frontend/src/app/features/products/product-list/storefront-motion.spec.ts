import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { StorefrontMotion } from './storefront-motion';

@Component({
  template: '<section storefrontMotion>Storefront</section>',
  imports: [StorefrontMotion],
})
class MotionHost {}

describe('ScrollCraft route lifecycle', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  function setup(reducedMotion = false, compactScreen = false) {
    const reduced = new EventTarget() as EventTarget & { matches: boolean };
    const compact = new EventTarget() as EventTarget & { matches: boolean };
    reduced.matches = reducedMotion;
    compact.matches = compactScreen;
    vi.stubGlobal('matchMedia', (query: string) =>
      query.includes('reduced-motion') ? reduced : compact,
    );
    let frame: FrameRequestCallback = () => undefined;
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
      frame = callback;
      return 7;
    });
    vi.stubGlobal('cancelAnimationFrame', vi.fn());
    const engine = { destroy: vi.fn(), layout: vi.fn() };
    const mount = vi.fn(() => engine);
    vi.stubGlobal('ScrollCraft', { mount });
    TestBed.configureTestingModule({ imports: [MotionHost] });
    const fixture = TestBed.createComponent(MotionHost);
    fixture.detectChanges();
    frame(0);
    return {
      fixture,
      reduced,
      compact,
      engine,
      mount,
      host: fixture.nativeElement.querySelector('section') as HTMLElement,
    };
  }
  it('mounts on desktop, responds to accessibility changes, and releases handlers on navigation', () => {
    const state = setup();
    expect(state.mount).toHaveBeenCalledWith(state.host);
    expect(state.host.classList.contains('motion-ready')).toBe(true);
    state.reduced.matches = true;
    state.reduced.dispatchEvent(new Event('change'));
    expect(state.engine.destroy).toHaveBeenCalledTimes(1);
    expect(state.host.classList.contains('motion-ready')).toBe(false);
    state.reduced.matches = false;
    state.reduced.dispatchEvent(new Event('change'));
    expect(state.mount).toHaveBeenCalledTimes(2);
    state.fixture.destroy();
    expect(state.engine.destroy).toHaveBeenCalledTimes(2);
    expect(globalThis.cancelAnimationFrame).toHaveBeenCalledWith(7);
    state.compact.dispatchEvent(new Event('change'));
    globalThis.dispatchEvent(new Event('load'));
    expect(state.mount).toHaveBeenCalledTimes(2);
  });
  it.each([
    [true, false],
    [false, true],
  ])('keeps content readable when motion is disabled (%s, %s)', (reduced, compact) => {
    const state = setup(reduced, compact);
    expect(state.mount).not.toHaveBeenCalled();
    expect(state.host.classList.contains('motion-ready')).toBe(false);
  });
  it('falls back to static content if the runtime is unavailable or cannot mount', () => {
    const state = setup();
    vi.stubGlobal('ScrollCraft', undefined);
    state.compact.dispatchEvent(new Event('change'));
    expect(state.host.classList.contains('motion-ready')).toBe(false);
    vi.stubGlobal('ScrollCraft', {
      mount: () => {
        throw new Error('Runtime unavailable');
      },
    });
    state.compact.dispatchEvent(new Event('change'));
    expect(state.host.textContent).toContain('Storefront');
    expect(state.host.classList.contains('motion-ready')).toBe(false);
  });
});
