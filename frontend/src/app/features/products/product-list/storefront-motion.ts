import { AfterViewInit, DestroyRef, Directive, ElementRef, inject, NgZone } from '@angular/core';

interface MotionEngine {
  destroy(): void;
  layout(): void;
}
declare global {
  interface Window {
    ScrollCraft?: { mount(root: HTMLElement): MotionEngine };
  }
}

/** Shares Neo4flix's ScrollCraft runtime and releases it when this route closes. */
@Directive({ selector: '[storefrontMotion]' })
export class StorefrontMotion implements AfterViewInit {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      if (typeof matchMedia !== 'function') return;
      const reduced = matchMedia('(prefers-reduced-motion: reduce)');
      const compact = matchMedia('(max-width: 800px)');
      const host = this.element.nativeElement;
      let engine: MotionEngine | undefined;
      let frame = 0;
      let layoutFrame = 0;
      let disposed = false;
      const sync = () => {
        engine?.destroy();
        engine = undefined;
        host.classList.remove('motion-ready');
        const runtime = (globalThis as typeof globalThis & Window).ScrollCraft;
        if (disposed || reduced.matches || compact.matches || !runtime) return;
        try {
          host.classList.add('motion-ready');
          engine = runtime.mount(host);
        } catch {
          host.classList.remove('motion-ready');
        }
      };
      frame = requestAnimationFrame(sync);
      globalThis.addEventListener('load', sync, { once: true });
      reduced.addEventListener?.('change', sync);
      compact.addEventListener?.('change', sync);
      // API results and image sizes change the position of later pinned sections.
      // Re-measure the existing scene without replaying entrances or remounting it.
      const resize =
        typeof ResizeObserver === 'undefined'
          ? undefined
          : new ResizeObserver(() => {
              if (disposed || !engine) return;
              cancelAnimationFrame(layoutFrame);
              layoutFrame = requestAnimationFrame(() => engine?.layout());
            });
      resize?.observe(host);
      this.destroyRef.onDestroy(() => {
        disposed = true;
        cancelAnimationFrame(frame);
        cancelAnimationFrame(layoutFrame);
        resize?.disconnect();
        globalThis.removeEventListener('load', sync);
        reduced.removeEventListener?.('change', sync);
        compact.removeEventListener?.('change', sync);
        engine?.destroy();
      });
    });
  }
}
