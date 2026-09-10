import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

type MotionMode = 'system' | 'full' | 'reduced';
const storageKey = 'nexora.motion';
const isMode = (value: string | null): value is MotionMode =>
  value === 'system' || value === 'full' || value === 'reduced';

/** A site-only choice; system accessibility remains the default. */
@Injectable({ providedIn: 'root' })
export class MotionPreferenceService {
  private readonly selected = signal<MotionMode>('system');
  private readonly systemReduced = signal(false);
  readonly mode = this.selected.asReadonly();
  readonly reduced = computed(() =>
    this.mode() === 'system' ? this.systemReduced() : this.mode() === 'reduced',
  );
  readonly changes = new Subject<void>();

  constructor() {
    const media =
      typeof matchMedia === 'function' ? matchMedia('(prefers-reduced-motion: reduce)') : undefined;
    this.systemReduced.set(media?.matches ?? false);
    let stored: string | null = null;
    try {
      stored = localStorage.getItem(storageKey);
    } catch {
      // Privacy settings may disable storage; the current visit still works.
    }
    const requested = new URL(location.href).searchParams.get('motion');
    const remembered = isMode(stored) ? stored : 'system';
    this.selected.set(isMode(requested) ? requested : remembered);
    if (isMode(requested)) this.persist();
    this.apply();
    const onChange = () => {
      this.systemReduced.set(media?.matches ?? false);
      this.apply();
      this.changes.next();
    };
    media?.addEventListener?.('change', onChange);
    inject(DestroyRef).onDestroy(() => {
      media?.removeEventListener?.('change', onChange);
      this.changes.complete();
      delete document.documentElement.dataset['motion'];
    });
  }

  setMode(value: string): void {
    if (!isMode(value)) return;
    this.selected.set(value);
    this.persist();
    this.apply();
    const url = new URL(location.href);
    if (url.searchParams.has('motion')) {
      url.searchParams.set('motion', value);
      history.replaceState(history.state, '', url.pathname + url.search + url.hash);
    }
    this.changes.next();
  }

  private apply(): void {
    document.documentElement.dataset['motion'] = this.mode();
  }

  private persist(): void {
    try {
      if (this.mode() === 'system') localStorage.removeItem(storageKey);
      else localStorage.setItem(storageKey, this.mode());
    } catch {
      // Keep the in-memory choice when storage is unavailable or full.
    }
  }
}
