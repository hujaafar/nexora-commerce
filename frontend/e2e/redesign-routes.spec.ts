import { test, expect, Page } from '@playwright/test';

// UI fixtures are isolated to this browser suite. marketplace.spec.ts exercises live services.
const date = '2026-09-10T00:00:00Z';
const product = {
  id: 'design-product',
  name: 'Everyday studio headphones',
  category: 'mobile accessories',
  description: 'A considered pair for your daily listening. Visual review fixture.',
  price: 79,
  quantity: 8,
  sellerId: 'design-user',
  createdAt: date,
  updatedAt: date,
  imageUrls: ['/assets/editorial-headphones.webp', '/assets/editorial-headphones.webp?second'],
};
const line = {
  productId: product.id,
  name: product.name,
  category: product.category,
  sellerId: product.sellerId,
  unitPrice: 79,
  quantity: 1,
  availableQuantity: 8,
  lineTotal: 79,
  imageUrl: product.imageUrls[0],
};
const cart = { items: [line], itemCount: 1, subtotal: 79, updatedAt: date };
const order = {
  id: 'design-order',
  orderNumber: 'NX-LOOKBOOK',
  customerId: 'design-user',
  items: [line],
  shippingAddress: {
    fullName: 'Alex Studio',
    phone: '+97333333333',
    addressLine: '15 Example Street',
    city: 'Manama',
    country: 'Bahrain',
    postalCode: '123',
  },
  paymentMethod: 'PAY_ON_DELIVERY',
  paymentStatus: 'DUE_ON_DELIVERY',
  status: 'CONFIRMED',
  subtotal: 79,
  deliveryFee: 5,
  total: 84,
  createdAt: date,
  updatedAt: date,
  cancelledAt: null,
};
const asset = {
  id: 'design-image',
  originalFilename: 'studio-headphones.webp',
  url: product.imageUrls[0],
  contentType: 'image/webp',
  size: 176804,
  ownerId: 'design-user',
  productId: product.id,
  purpose: 'PRODUCT_IMAGE',
  createdAt: date,
};

async function prepare(page: Page) {
  await page.addInitScript(() => {
    Element.prototype.requestPointerLock = () => Promise.reject(new Error('Disabled in UI tests'));
    Element.prototype.setPointerCapture = () => {};
    Element.prototype.releasePointerCapture = () => {};
  });
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    const user = await page.evaluate(
      () => JSON.parse(localStorage.getItem('nexora.session') || 'null')?.user,
    );
    const metric = { productId: product.id, name: product.name, units: 3, amount: 237 };
    const responses: Record<string, unknown> = {
      '/api/me': user,
      '/api/products/mine': [product],
      '/api/products/moderation': [product],
      '/api/products/search': {
        items: [product],
        totalItems: 1,
        totalPages: 1,
        page: 0,
        size: 12,
        categories: ['mobile accessories', 'mens shoes', 'home decoration'],
      },
      [`/api/products/${product.id}`]: product,
      '/api/cart': cart,
      '/api/wishlist': { items: [product], updatedAt: date },
      '/api/orders': { items: [order], page: 0, size: 10, totalItems: 1, totalPages: 1 },
      '/api/orders/seller': { items: [order], page: 0, size: 10, totalItems: 1, totalPages: 1 },
      [`/api/orders/${order.id}`]: order,
      [`/api/orders/seller/${order.id}`]: order,
      '/api/media/images': [asset],
      '/api/media/images/mine': [asset],
      '/api/media/images/moderation': [asset],
      '/api/admin/users': [user],
      '/api/analytics/customer': {
        totalSpent: 237,
        completedOrders: 3,
        purchasedUnits: 3,
        mostBoughtProducts: [metric],
        topCategories: [{ category: product.category, units: 3, amount: 237 }],
      },
      '/api/analytics/seller': {
        revenue: 237,
        orderCount: 3,
        unitsSold: 3,
        bestSellingProducts: [metric],
      },
    };
    if (!(path in responses) || route.request().method() !== 'GET') {
      throw new Error(`Unexpected fixture request: ${route.request().method()} ${path}`);
    }
    await route.fulfill({ json: responses[path] });
  });
}

for (const width of [1440, 390, 360]) {
  test(`redesigned account and commerce routes stay readable at ${width}px`, async ({ page }) => {
    test.setTimeout(90_000);
    await page.setViewportSize({ width, height: width === 360 ? 640 : 900 });
    await prepare(page);
    const errors: string[] = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto('/login');
    const routes = [
      ['login', null],
      ['register', null],
      [`products/${product.id}`, null],
      ['cart', 'CLIENT'],
      ['checkout', 'CLIENT'],
      ['wishlist', 'CLIENT'],
      ['orders', 'CLIENT'],
      [`orders/${order.id}`, 'CLIENT'],
      ['profile', 'CLIENT'],
      ['seller', 'SELLER'],
      ['media', 'SELLER'],
      ['profile', 'SELLER'],
      ['admin', 'ADMIN'],
    ] as const;
    for (const [path, role] of routes) {
      await page.evaluate((role) => {
        if (!role) {
          localStorage.removeItem('nexora.session');
          return;
        }
        localStorage.setItem(
          'nexora.session',
          JSON.stringify({
            accessToken: 'visual-fixture-only',
            expiresAt: new Date(Date.now() + 3600000).toISOString(),
            user: {
              id: 'design-user',
              name: 'Alex Studio',
              email: 'alex@example.com',
              role,
              avatarUrl: null,
              createdAt: '2026-09-10T00:00:00Z',
            },
          }),
        );
      }, role);
      await page.goto(`/${path}`);
      await expect(page.locator('h1').first()).toBeVisible();
      await page.evaluate(() => document.fonts.ready);
      await page.waitForTimeout(300);
      expect(
        await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
        path,
      ).toBe(true);
      expect(
        await page
          .locator('img')
          .evaluateAll((images) => images.every((img) => !img.complete || img.naturalWidth > 0)),
        path,
      ).toBe(true);
      await page.screenshot({
        path: `test-results/redesign-${width}-${path.replaceAll('/', '-')}-${role || 'guest'}.png`,
        fullPage: true,
      });
      if (path === 'login') {
        await page.getByRole('button', { name: /Enter the marketplace/ }).click();
        await expect(page.getByText('Enter a valid email address.')).toBeVisible();
      }
      if (path === 'checkout') {
        await page.getByRole('button', { name: /Review order/ }).click();
        await expect(
          page.getByText('Please complete the required delivery details.'),
        ).toBeVisible();
      }
      if (width < 800) {
        const menu = page.getByRole('button', { name: /^Menu/ });
        await menu.click();
        await expect(
          page
            .getByRole('navigation', { name: 'Primary navigation' })
            .getByRole('link', { name: 'The collection', exact: true }),
        ).toBeVisible();
        if (role)
          await expect(page.getByRole('button', { name: 'Sign out', exact: true })).toBeVisible();
        await page.keyboard.press('Escape');
        await expect(menu).toHaveAttribute('aria-expanded', 'false');
      }
    }
    expect(errors).toEqual([]);
  });
}
