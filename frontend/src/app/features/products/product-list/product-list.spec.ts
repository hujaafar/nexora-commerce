/*
 * File purpose: Verifies product list.spec behavior.
 */
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { ProductService } from '../../../core/services/product.service';
import { ProductList } from './product-list';

describe('ProductList', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  async function createPage(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ProductService,
          useValue: {
            search: () =>
              of({
                items: [],
                page: 0,
                size: 12,
                totalItems: 0,
                totalPages: 0,
                categories: [],
              }),
          },
        },
        {
          provide: AuthService,
          useValue: {
            currentUser: () => null,
            isAuthenticated: () => false,
            isClient: () => false,
            isSeller: () => false,
            isAdmin: () => false,
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProductList);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('takes section links to their destinations within one second', async () => {
    const page = await createPage();
    const sectionLinks = Array.from(page.querySelectorAll<HTMLAnchorElement>('a[href^="#"]'));
    const frameQueue: FrameRequestCallback[] = [];
    let nextFrameId = 0;
    let currentScroll = 400;

    vi.spyOn(performance, 'now').mockReturnValue(0);
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockReturnValue({
        matches: false,
      } as MediaQueryList),
    );
    vi.spyOn(window, 'scrollY', 'get').mockImplementation(() => currentScroll);
    const requestFrame = vi
      .spyOn(window, 'requestAnimationFrame')
      .mockImplementation((callback) => {
        frameQueue.push(callback);
        nextFrameId += 1;
        return nextFrameId;
      });
    vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => undefined);
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(((
      options: ScrollToOptions,
    ) => {
      currentScroll = options.top ?? currentScroll;
    }) as typeof globalThis.scrollTo);

    expect(sectionLinks.length).toBeGreaterThanOrEqual(3);

    for (const link of sectionLinks) {
      currentScroll = 400;
      requestFrame.mockClear();
      scrollTo.mockClear();

      const targetId = link.getAttribute('href')?.slice(1);
      const target = targetId ? document.getElementById(targetId) : null;
      expect(target, `Missing section for ${link.textContent?.trim()}`).toBeTruthy();
      target!.getBoundingClientRect = () =>
        ({
          top: 1000,
        }) as DOMRect;

      link.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

      let timestamp = 0;
      let renderedFrames = 0;
      while (frameQueue.length > 0 && renderedFrames < 20) {
        timestamp += 250;
        frameQueue.shift()!(timestamp);
        renderedFrames += 1;
      }

      expect(renderedFrames).toBeGreaterThanOrEqual(2);
      expect(timestamp).toBeLessThanOrEqual(1000);
      expect(currentScroll).toBe(targetId === 'top' ? 0 : 1384);
      expect(document.documentElement.style.scrollBehavior).not.toBe('auto');
      expect(location.hash).toBe(`#${targetId}`);
    }
  });
});
