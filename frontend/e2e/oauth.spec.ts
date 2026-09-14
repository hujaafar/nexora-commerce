import { test, expect, Page } from '@playwright/test';

// Provider responses are explicitly mocked; real code exchange is covered by Java protocol tests.
const providers = [
  {
    id: 'google',
    name: 'Google',
    enabled: true,
    authorizationUrl: 'http://127.0.0.1:4200/api/auth/oauth2/authorize/google',
  },
  {
    id: 'github',
    name: 'GitHub',
    enabled: true,
    authorizationUrl: 'http://127.0.0.1:4200/api/auth/oauth2/authorize/github',
  },
];
const profile = {
  id: 'oauth-browser-user',
  name: 'Social Shopper',
  email: 'shopper@example.test',
  role: 'CLIENT',
  avatarUrl: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};
const response = {
  accessToken: 'browser-fixture-token',
  tokenType: 'Bearer',
  expiresAt: '2099-01-01T00:00:00Z',
  user: profile,
};

async function prepare(
  page: Page,
  mode: 'register' | 'link' | 'login' = 'register',
  provider = 'google',
) {
  await page.route('**/api/products/search*', (route) =>
    route.fulfill({ json: { items: [], total: 0, page: 0, size: 12 } }),
  );
  await page.route('**/api/auth/oauth2/providers', (route) => route.fulfill({ json: providers }));
  await page.route('**/api/auth/oauth2/pending', (route) =>
    route.fulfill({
      json: {
        provider,
        providerName: provider === 'google' ? 'Google' : 'GitHub',
        mode,
        name: profile.name,
        email: profile.email,
        returnUrl: '/products',
      },
    }),
  );
}

test('Google and GitHub preserve the requested route and do not expose a secret', async ({
  page,
}) => {
  await prepare(page);
  await page.goto('/login?returnUrl=%2Fcheckout');
  for (const provider of providers) {
    const link = page.getByRole('link', { name: `Continue with ${provider.name}` });
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute(
      'href',
      `${provider.authorizationUrl}?returnUrl=%2Fcheckout`,
    );
  }
  expect(await page.locator('app-social-login').innerHTML()).not.toContain('client_secret');
});

test('unconfigured providers stay unavailable and email login stays usable', async ({ page }) => {
  await prepare(page);
  await page.route('**/api/auth/oauth2/providers', (route) =>
    route.fulfill({ json: providers.map((p) => ({ ...p, enabled: false })) }),
  );
  await page.goto('/login');
  await expect(page.getByText('Google sign-in is not available yet.')).toBeVisible();
  await expect(page.getByText('GitHub sign-in is not available yet.')).toBeVisible();
  await expect(page.locator('.social-button').first()).not.toHaveAttribute('href');
  await expect(page.getByRole('button', { name: /Enter the marketplace/ })).toBeEnabled();
});

test('new Google shopper completes registration on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await prepare(page);
  let submitted: unknown;
  await page.route('**/api/auth/oauth2/complete', (route) => {
    submitted = route.request().postDataJSON();
    return route.fulfill({ json: response });
  });
  await page.goto('/oauth2/complete');
  await expect(page.getByRole('heading', { name: 'Make it yours.' })).toBeVisible();
  await page.getByLabel('Display name').fill('');
  await page.getByRole('button', { name: /Continue to Nexora/ }).click();
  await expect(page.getByText('Use a name between 2 and 80 characters.')).toBeVisible();
  expect(submitted).toBeUndefined();
  await page.getByLabel('Display name').fill(profile.name);
  await page.getByLabel('Create a password').fill('Strong123!');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(
    true,
  );
  await page.getByRole('button', { name: /Continue to Nexora/ }).click();
  await expect(page).toHaveURL(/\/products$/);
  expect(submitted).toEqual({ name: profile.name, password: 'Strong123!' });
});

test('existing GitHub account needs its password and shows a rejected attempt', async ({
  page,
}) => {
  await prepare(page, 'link', 'github');
  let attempts = 0;
  await page.route('**/api/auth/oauth2/complete', (route) => {
    attempts++;
    return route.fulfill({
      status: 401,
      json: {
        code: 'Unauthorized',
        message: 'Enter your current password to connect this account.',
        details: {},
      },
    });
  });
  await page.goto('/oauth2/complete');
  await expect(page.getByText(/Enter its password once/)).toBeVisible();
  expect(attempts).toBe(0);
  await expect(page.getByLabel('Display name')).toHaveCount(0);
  await page.getByLabel('Current password').fill('Wrong123!');
  await page.getByRole('button', { name: /Continue to Nexora/ }).click();
  await expect(page.locator('.auth-card [role=alert]')).toContainText('current password');
  expect(await page.evaluate(() => localStorage.getItem('nexora.session'))).toBeNull();
});

test('returning linked member signs in without another password', async ({ page }) => {
  await prepare(page, 'login');
  await page.route('**/api/auth/oauth2/complete', (route) => route.fulfill({ json: response }));
  await page.goto('/oauth2/complete');
  await expect(page).toHaveURL(/\/products$/);
  const stored = await page.evaluate(() =>
    JSON.parse(localStorage.getItem('nexora.session') || '{}'),
  );
  expect(stored.user.id).toBe(profile.id);
});

test('expired completion and cancelled authorization offer a clean retry', async ({ page }) => {
  await prepare(page);
  await page.route('**/api/auth/oauth2/pending', (route) =>
    route.fulfill({ status: 401, json: { message: 'Expired', code: 'Unauthorized', details: {} } }),
  );
  await page.goto('/oauth2/complete');
  await expect(page.getByRole('heading', { name: 'Let’s try again.' })).toBeVisible();
  await page.goto('/login?oauthError=github');
  await expect(page.locator('.auth-card [role=alert]')).toContainText(
    'GitHub sign-in was cancelled',
  );
  expect(await page.evaluate(() => localStorage.getItem('nexora.session'))).toBeNull();
});
