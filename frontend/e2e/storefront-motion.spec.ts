import { test, expect, Page } from '@playwright/test';

test.setTimeout(30_000);

// Deterministic visual fixtures only. marketplace.spec.ts tests the real API.
const fixtureProduct = {
  id: 'motion-fixture',
  name: 'Motion gallery fixture',
  category: 'Studio',
  description: 'A deterministic fixture for visual and keyboard checks.',
  price: 40,
  quantity: 3,
  sellerId: 'motion-seller',
  createdAt: '2026-09-10T00:00:00Z',
  updatedAt: '2026-09-10T00:00:00Z',
  imageUrls: ['/assets/nexora-hero.png', '/assets/nexora-hero.png?view=second'],
};

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    Element.prototype.requestPointerLock = () =>
      Promise.reject(new Error('Disabled in visual tests'));
    Element.prototype.setPointerCapture = () => {};
    Element.prototype.releasePointerCapture = () => {};
  });
  await page.route('**/api/products/**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/search')) {
      const items = url.searchParams.get('q') === 'missing' ? [] : [fixtureProduct];
      await route.fulfill({
        json: {
          items,
          totalItems: items.length,
          totalPages: 1,
          page: 0,
          size: 12,
          categories: ['Studio'],
        },
      });
    } else {
      await route.fulfill({ json: fixtureProduct });
    }
  });
});

async function sampleAct(page: Page, selector: string, progress: number) {
  await page.locator(selector).evaluate((element, p) => {
    const box = element.getBoundingClientRect();
    window.scrollTo({
      top: window.scrollY + box.top + Math.max(0, box.height - innerHeight) * p,
      behavior: 'instant',
    });
  }, progress);
  await page.waitForTimeout(220);
}

test('desktop scroll and pointer move the rendered collection, and route navigation releases the engine', async ({
  page,
}) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/products');
  await expect(page.locator('.storefront')).toHaveClass(/motion-ready/);
  await expect(page.locator('.product-card')).toHaveCount(1);
  await page.evaluate(() => document.fonts.ready);
  await sampleAct(page, '#top', 0);
  const start = await page.locator('.hero-scene').evaluate((el) => getComputedStyle(el).transform);
  await page.screenshot({ path: 'test-results/browser-motion-opening.png' });
  await sampleAct(page, '#top', 0.55);
  const middle = await page.locator('.hero-scene').evaluate((el) => getComputedStyle(el).transform);
  expect(middle).not.toBe(start);
  await page.screenshot({ path: 'test-results/browser-motion-middle.png' });
  await sampleAct(page, '#top', 1);
  await page.screenshot({ path: 'test-results/browser-motion-settled.png' });
  await sampleAct(page, '#top', 0.35);
  await page.mouse.move(890, 260);
  await page.waitForTimeout(300);
  const pointerA = await page.locator('.scene-window').screenshot();
  await page.mouse.move(1290, 650);
  await page.waitForTimeout(300);
  const pointerB = await page.locator('.scene-window').screenshot();
  expect(pointerA.equals(pointerB)).toBe(false);
  await page.screenshot({ path: 'test-results/browser-motion-pointer.png' });
  await sampleAct(page, '#about', 0.5);
  await page.screenshot({ path: 'test-results/browser-motion-story.png' });
  await page.getByLabel('Search products', { exact: true }).fill('missing');
  await expect(page.getByRole('heading', { name: 'No products match yet.' })).toBeVisible();
  await page.getByRole('button', { name: 'Reset discovery' }).click();
  await expect(page.locator('.product-card')).toHaveCount(1);
  await page
    .locator('.product-card')
    .getByRole('link', { name: /View details/ })
    .click();
  await expect(page).toHaveURL(/products\/motion-fixture$/);
  expect(await page.evaluate(() => (window as any).ScrollCraft.instances.length)).toBe(0);
  expect(errors).toEqual([]);
});

for (const mode of [
  { name: 'phone', width: 390, height: 844, reduced: false },
  { name: 'compact-phone', width: 360, height: 640, reduced: false },
  { name: 'reduced-motion', width: 1440, height: 900, reduced: true },
]) {
  test(`${mode.name} keeps all content and controls visible without extra pinned scrolling`, async ({
    page,
  }) => {
    await page.setViewportSize({ width: mode.width, height: mode.height });
    await page.emulateMedia({ reducedMotion: mode.reduced ? 'reduce' : 'no-preference' });
    await page.goto('/products');
    await expect(page.locator('.product-card')).toHaveCount(1);
    await expect(page.locator('.storefront')).not.toHaveClass(/motion-ready/);
    await page.evaluate(() => document.fonts.ready);
    await page.locator('.hero-stage').evaluate(async (element) => {
      await Promise.all(
        element.getAnimations({ subtree: true }).map((animation) => animation.finished),
      );
    });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
    await page.screenshot({ path: `test-results/browser-motion-${mode.name}.png`, fullPage: true });
    await page
      .locator('.hero-actions')
      .getByRole('link', { name: /Shop the collection/ })
      .click();
    await expect(page.getByLabel('Search products', { exact: true })).toBeInViewport();
    for (const link of await page.locator('.story-card a').all()) {
      await link.focus();
      await expect(link).toBeFocused();
      await expect(link).toBeInViewport();
    }
    if (mode.reduced) {
      const running = await page
        .locator('.hero-stage')
        .evaluate((el) => el.getAnimations({ subtree: true }).length);
      expect(running).toBe(0);
    }
  });
}

test('gallery thumbnails cross-slide, count correctly, and support keyboard navigation', async ({
  page,
}) => {
  await page.goto('/products/motion-fixture');
  await expect(page.getByRole('heading', { level: 1, name: fixtureProduct.name })).toBeVisible();
  const next = page.getByRole('button', { name: 'Next product image' });
  await next.click();
  await expect(page.locator('.image-index')).toContainText('02 / 02');
  await expect(page.getByRole('button', { name: 'Show image 2', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await expect(page.locator('.main-image > img')).toHaveCount(1);
  await expect(page.locator('.main-image > img')).toHaveAttribute('src', /view=second/);
  await next.press('ArrowLeft');
  await expect(page.locator('.image-index')).toContainText('01 / 02');
  await expect(page.locator('.main-image > img')).toHaveCount(1);
  await page.screenshot({ path: 'test-results/browser-motion-gallery.png' });
});
