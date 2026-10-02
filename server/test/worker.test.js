import { test, describe } from 'node:test';
import assert from 'node:assert';
import worker from '../worker.js';

const req = (path) => worker.fetch(new Request(`http://localhost${path}`));

describe('Cloudflare Worker Sector & Quote API Tests', () => {
  test('GET / returns API info with version 1.4.0', async () => {
    const res = await req('/');
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.status, 'online');
    assert.strictEqual(body.version, '1.4.0');
    assert.ok(body.endpoints);
  });

  test('GET /api/health returns ok', async () => {
    const res = await req('/api/health');
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.status, 'ok');
  });

  test('GET /api/quote?symbol=600519.SS returns valid stock quote', async () => {
    const res = await req('/api/quote?symbol=600519.SS');
    assert.strictEqual(res.status, 200);
    const quote = await res.json();
    assert.strictEqual(quote.symbol, '600519.SS');
    assert.ok(typeof quote.price === 'number');
    assert.ok(quote.price > 0);
  });

  test('GET /api/sector/constituents?bk=BK1638 returns popular multi-board stocks', async () => {
    const res = await req('/api/sector/constituents?bk=BK1638');
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.rc, 0);
    assert.ok(body.data);
    assert.ok(Array.isArray(body.data.diff));
    assert.ok(body.data.diff.length >= 25, `Expected >= 25 items, got ${body.data.diff.length}`);
    const first = body.data.diff[0];
    assert.ok(first.f12, 'f12 code must be present');
    assert.ok(first.f14, 'f14 name must be present');
    assert.ok(typeof first.f2 === 'number', 'f2 price must be number');
    assert.ok(typeof first.f3 === 'number', 'f3 changePercent must be number');
  });

  test('GET /api/sector/constituents?bk=BK1036 returns semiconductor industry constituents', async () => {
    const res = await req('/api/sector/constituents?bk=BK1036');
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.rc, 0);
    assert.ok(body.data);
    assert.ok(body.data.diff.length >= 20, `Expected >= 20 items, got ${body.data.diff.length}`);
  });

  test('GET /api/sector/quote?bk=BK1638 returns sector index quote', async () => {
    const res = await req('/api/sector/quote?bk=BK1638');
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.rc, 0);
    assert.ok(body.data);
    assert.ok(Array.isArray(body.data.diff));
    assert.ok(body.data.diff.length > 0);
    const quote = body.data.diff[0];
    assert.strictEqual(quote.f12, 'BK1638');
    assert.strictEqual(quote.f14, '最近多板');
    assert.ok(quote.f2 > 0);
  });
});
