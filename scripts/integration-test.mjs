import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const base = (process.env.API_BASE || 'http://127.0.0.1:4200/api').replace(/\/$/, '');
const run = randomUUID().slice(0, 8);
let checks = 0;
async function api(method, path, token, data, expected = 200) {
  const isForm = data instanceof FormData;
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(!isForm && data !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: data === undefined ? undefined : isForm ? data : JSON.stringify(data),
    signal: AbortSignal.timeout(20000),
  });
  assert.equal(response.status, expected, `${method} ${path}: expected ${expected}, received ${response.status}`);
  checks++;
  if (response.status === 204) return null;
  if (!response.headers.get('content-type')?.includes('json')) return response.arrayBuffer();
  const result = await response.json();
  if (expected >= 400) {
    assert.equal(typeof result.code, 'string', `${path}: error code is required`);
    assert.equal(typeof result.message, 'string', `${path}: error message is required`);
    assert.ok(result.details && typeof result.details === 'object', `${path}: error details are required`);
  }
  return result;
}
async function account(role, label) {
  return api('POST', '/auth/register', null, {
    name: `Integration ${label}`, email: `e2e-${run}-${label}@example.test`,
    password: `Test-${randomUUID()}-9`, role,
  }, 201);
}
const address = { fullName: 'Integration Customer', phone: '+97333333333', addressLine: 'Test building 1, road 2', city: 'Manama', country: 'Bahrain', postalCode: '123' };
const products = [];
let seller, buyer, outsider, mediaId;
try {
  const catalog = await api('GET', '/products/search');
  assert.ok(Array.isArray(catalog.items));
  await api('GET', '/cart', null, undefined, 401);
  seller = await account('SELLER', 'seller');
  buyer = await account('CLIENT', 'customer');
  outsider = await account('CLIENT', 'outsider');
  await api('POST', '/auth/register', null, { name: 'Denied Admin', email: `denied-${run}@example.test`, password: 'DeniedPass123!', role: 'ADMIN' }, 400);
  await api('GET', '/admin/users', buyer.accessToken, undefined, 403);
  await api('GET', '/me', seller.accessToken);
  const product = await api('POST', '/products', seller.accessToken, {
    name: `Integration product ${run}`, description: 'Created by the real API integration test.',
    category: 'Test fixtures', price: 40, quantity: 8, imageUrls: [],
  }, 201);
  products.push(product.id);
  await api('POST', '/products', buyer.accessToken, { name: 'Forbidden product', description: 'Should never exist', category: 'Test', price: 1, quantity: 1, imageUrls: [] }, 403);
  await api('DELETE', `/products/${product.id}`, outsider.accessToken, undefined, 403);

  const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7WQAAAAASUVORK5CYII=', 'base64');
  const form = new FormData();
  form.append('file', new Blob([png], { type: 'image/png' }), 'integration.png');
  const uploaded = await api('POST', '/media/images', seller.accessToken, form, 201);
  mediaId = uploaded.id;
  const downloaded = await api('GET', `/media/images/${mediaId}`);
  assert.equal(downloaded.byteLength, png.byteLength);
  const forged = new FormData();
  forged.append('file', new Blob(['not an image'], { type: 'image/png' }), 'forged.png');
  await api('POST', '/media/images', seller.accessToken, forged, 400);
  const oversized = new FormData();
  oversized.append('file', new Blob([Buffer.alloc(2 * 1024 * 1024 + 1)], { type: 'image/png' }), 'oversized.png');
  await api('POST', '/media/images', seller.accessToken, oversized, 400);

  let wishlist = await api('PUT', `/wishlist/${product.id}`, buyer.accessToken);
  assert.ok(wishlist.items.some((item) => item.id === product.id));
  wishlist = await api('DELETE', `/wishlist/${product.id}`, buyer.accessToken);
  assert.ok(!wishlist.items.some((item) => item.id === product.id));
  await api('PUT', '/cart/items', buyer.accessToken, { productId: product.id, quantity: 2 });
  const order = await api('POST', '/checkout', buyer.accessToken, { shippingAddress: address, paymentMethod: 'PAY_ON_DELIVERY' }, 201);
  assert.equal(order.subtotal, 80);
  assert.equal(order.total, 84.9);
  assert.equal(order.status, 'PLACED');
  assert.equal((await api('GET', `/products/${product.id}`)).quantity, 6);
  assert.equal((await api('GET', '/cart', buyer.accessToken)).itemCount, 0);
  await api('GET', `/orders/${order.id}`, outsider.accessToken, undefined, 404);
  await api('GET', `/orders/seller/${order.id}`, seller.accessToken);
  await api('PATCH', `/orders/${order.id}/status`, seller.accessToken, { status: 'DELIVERED' }, 409);
  const cancelled = await api('POST', `/orders/${order.id}/cancel`, buyer.accessToken);
  assert.equal(cancelled.status, 'CANCELLED');
  assert.equal((await api('GET', `/products/${product.id}`)).quantity, 8);
  await api('POST', `/orders/${order.id}/cancel`, buyer.accessToken, undefined, 409);
  const repeated = await api('POST', `/orders/${order.id}/redo`, buyer.accessToken, undefined, 201);
  for (const status of ['CONFIRMED', 'PACKING', 'SHIPPED', 'DELIVERED']) {
    const updated = await api('PATCH', `/orders/${repeated.id}/status`, seller.accessToken, { status });
    assert.equal(updated.status, status);
  }
  await api('POST', `/orders/${repeated.id}/cancel`, buyer.accessToken, undefined, 409);
  await api('GET', '/analytics/seller', seller.accessToken);
  await api('GET', '/analytics/customer', buyer.accessToken);
  await api('DELETE', `/orders/${order.id}`, buyer.accessToken, undefined, 204);
  console.log(`PASS: ${checks} real HTTP checks, including media, ownership, checkout, inventory and fulfilment.`);
  console.log('Test accounts and the fulfilled order remain as identifiable e2e fixtures; temporary products and media are removed.');
} finally {
  if (seller) {
    for (const id of products) await api('DELETE', `/products/${id}`, seller.accessToken, undefined, 204);
    if (mediaId) await api('DELETE', `/media/images/${mediaId}`, seller.accessToken, undefined, 204);
  }
}
