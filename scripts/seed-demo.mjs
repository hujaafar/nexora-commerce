import { readFile } from 'node:fs/promises';
import { setTimeout } from 'node:timers/promises';

const base = (process.argv[2] || 'http://127.0.0.1:8080').replace(/\/$/, '');
const records = JSON.parse(await readFile(new URL('./demo-products.json', import.meta.url), 'utf8'));
let token;
async function api(path, options = {}) {
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    signal: AbortSignal.timeout(15000),
  });
  if (!response.ok) throw new Error(`${path} returned HTTP ${response.status}`);
  return response.status === 204 ? null : response.json();
}

// Eureka routing can settle after the individual process health checks pass.
for (let attempt = 0; attempt < 45; attempt++) {
  try {
    const session = await api('/auth/login', { method: 'POST', body: JSON.stringify({ email: 'seller@nexora.local', password: 'Seller123!' }) });
    token = session.accessToken;
    break;
  } catch (error) {
    if (attempt === 44) throw error;
    await setTimeout(2000);
  }
}
const existing = await api('/products/mine');
let created = 0;
for (const product of records) {
  if (existing.some((item) => item.name === product.name)) continue;
  await api('/products', { method: 'POST', body: JSON.stringify(product) });
  created++;
}
console.log(`Demo catalog ready: ${created} products added; existing products preserved.`);
