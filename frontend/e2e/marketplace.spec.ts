import { test, expect, APIRequestContext, Page } from '@playwright/test';
import { randomUUID } from 'node:crypto';

async function register(request: APIRequestContext, role: 'CLIENT' | 'SELLER') {
  const email = `browser-${randomUUID()}@example.test`;
  const password = `Browser-${randomUUID()}-9!`;
  const response = await request.post('/api/auth/register', {
    data: { name: `Browser ${role.toLowerCase()}`, email, password, role },
  });
  expect(response.status()).toBe(201);
  return { ...(await response.json()), email, password };
}
async function login(page: Page, user: { email: string; password: string }) {
  await page.goto('/login');
  await page.getByLabel('Email address').fill(user.email);
  await page.getByLabel('Password', { exact: true }).fill(user.password);
  await page.getByRole('button', { name: /Enter the marketplace/ }).click();
  await expect(page).not.toHaveURL(/\/login$/);
}

test('customer buys through the wizard, tracks, cancels, reorders, and removes history', async ({ page, request }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  const seller = await register(request, 'SELLER');
  const buyer = await register(request, 'CLIENT');
  const sellerHeaders = { Authorization: `Bearer ${seller.accessToken}` };
  const buyerHeaders = { Authorization: `Bearer ${buyer.accessToken}` };
  const created = await request.post('/api/products', { headers: sellerHeaders, data: {
    name: `Browser lamp ${randomUUID().slice(0, 6)}`, description: 'Browser acceptance test fixture',
    category: 'Home', price: 40, quantity: 8, imageUrls: [],
  } });
  expect(created.status()).toBe(201);
  const product = await created.json();
  try {
    await login(page, buyer);
    await page.goto(`/products/${product.id}`);
    await page.getByRole('button', { name: /Add to bag/ }).click();
    await page.goto('/cart');
    await expect(page.locator('.cart-line')).toContainText(product.name);
    await page.locator('.quantity-control').getByRole('button', { name: '+', exact: true }).click();
    await expect(page.locator('.cart-summary')).toContainText('$84.90');
    await page.getByRole('link', { name: /Continue to checkout/ }).click();
    await page.getByRole('button', { name: /Review order/ }).click();
    await expect(page.locator('.field-error')).toBeVisible();
    await page.getByLabel('Full name').fill('Browser Customer');
    await page.getByLabel('Phone').fill('+97333333333');
    await page.getByLabel('Address', { exact: true }).fill('Building 1, test road');
    await page.getByLabel('City').fill('Manama');
    await page.getByRole('button', { name: /Review order/ }).click();
    await expect(page.locator('.review-card')).toContainText('Pay on delivery');
    await expect(page.locator('.review-card')).toContainText('Nothing is charged now');
    await page.screenshot({ path: 'test-results/browser-checkout.png', fullPage: true });
    await page.getByRole('button', { name: /Confirm order/ }).click();
    await expect(page.locator('.success-card')).toContainText('Order confirmed');
    await page.getByRole('link', { name: 'Track this order' }).click();
    await expect(page.locator('.timeline')).toBeVisible();
    await expect(page.locator('.order-hero')).toContainText('Placed');
    page.on('dialog', (dialog) => dialog.accept());
    await page.getByRole('button', { name: 'Cancel order', exact: true }).click();
    await expect(page.locator('.cancelled-banner')).toBeVisible();
    const originalUrl = page.url();
    await page.getByRole('button', { name: 'Order again', exact: true }).click();
    await expect(page).not.toHaveURL(originalUrl);
    await expect(page.locator('.timeline')).toBeVisible();
    await page.getByRole('button', { name: 'Cancel order', exact: true }).click();
    await expect(page.locator('.cancelled-banner')).toBeVisible();
    await page.getByRole('button', { name: 'Remove from history' }).click();
    await expect(page).toHaveURL(/\/orders$/);
    await page.goto('/profile');
    await expect(page.getByText('Total spent', { exact: true })).toBeVisible();
    expect(errors).toEqual([]);
  } finally {
    const orders = await request.get('/api/orders', { headers: buyerHeaders });
    for (const order of (await orders.json()).items) {
      if (order.status === 'PLACED' || order.status === 'CONFIRMED') {
        await request.post(`/api/orders/${order.id}/cancel`, { headers: buyerHeaders });
      }
      await request.delete(`/api/orders/${order.id}`, { headers: buyerHeaders });
    }
    await request.delete(`/api/products/${product.id}`, { headers: sellerHeaders });
  }
});

test('seller publishes, edits, and deletes through the responsive dashboard', async ({ page, request }) => {
  const seller = await register(request, 'SELLER');
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, seller);
  await expect(page.getByRole('heading', { name: 'Manage your shop.' })).toBeVisible();
  await page.getByLabel('Product name').fill('Browser ceramic bowl');
  await page.getByLabel('Product story').fill('Handmade ceramic bowl for acceptance testing');
  await page.getByLabel(/^Category/).fill('Home');
  await page.getByLabel('Price / USD').fill('25');
  await page.getByLabel('Quantity', { exact: true }).fill('4');
  await page.getByRole('button', { name: /Launch product/ }).click();
  const inventory = page.locator('.inventory-list article').filter({ hasText: 'Browser ceramic bowl' });
  await expect(inventory).toContainText('$25.00');
  await inventory.getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Price / USD').fill('30');
  await page.getByRole('button', { name: /Save product changes/ }).click();
  await expect(inventory).toContainText('$30.00');
  await page.screenshot({ path: 'test-results/browser-seller-mobile.png', fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
  page.on('dialog', (dialog) => dialog.accept());
  await inventory.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(inventory).toHaveCount(0);
});
