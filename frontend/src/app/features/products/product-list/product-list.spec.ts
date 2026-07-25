import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { ProductService } from '../../../core/services/product.service';
import { ProductList } from './product-list';

describe('ProductList', () => {
  async function createPage(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        provideRouter([]),
        {
          provide: ProductService,
          useValue: {
            list: () => of([])
          }
        },
        {
          provide: AuthService,
          useValue: {
            currentUser: () => null,
            isAuthenticated: () => false,
            isSeller: () => false,
            isAdmin: () => false
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ProductList);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('smoothly scrolls every section link to a real destination', async () => {
    const page = await createPage();
    const sectionLinks = Array.from(
      page.querySelectorAll<HTMLAnchorElement>('a[href^="#"]')
    );

    expect(sectionLinks.length).toBeGreaterThanOrEqual(9);

    for (const link of sectionLinks) {
      const targetId = link.getAttribute('href')?.slice(1);
      const target = targetId ? document.getElementById(targetId) : null;
      expect(target, `Missing section for ${link.textContent?.trim()}`).toBeTruthy();

      const scrollIntoView = vi.fn();
      target!.scrollIntoView = scrollIntoView;
      link.dispatchEvent(
        new MouseEvent('click', { bubbles: true, cancelable: true })
      );

      expect(scrollIntoView).toHaveBeenCalledWith({
        behavior: 'smooth',
        block: 'start'
      });
      expect(location.hash).toBe(`#${targetId}`);
    }
  });
});
