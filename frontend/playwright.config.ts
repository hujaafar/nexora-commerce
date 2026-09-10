import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e', timeout: 90_000, workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  outputDir: 'test-results-e2e',
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://127.0.0.1:4200',
    channel: process.env['PLAYWRIGHT_CHANNEL'] || 'chrome',
    viewport: { width: 1440, height: 1000 },
    screenshot: 'only-on-failure',
    // Login requests contain temporary credentials; do not archive network traces.
    trace: 'off',
  },
});
