import { test, describe } from 'node:test';
import assert from 'node:assert';
import worker from '../src/index';

const req = (path: string) => worker.fetch(new Request(`http://localhost${path}`));

describe('Stock Trading API Tests', () => {
  test('GET / returns API info', async () => {
    const res = await req('/');
    assert.strictEqual(res.status, 200);
    const body = (await res.json()) as any;
    assert.strictEqual(body.status, 'online');
    assert.ok(body.endpoints);
  });

  test('GET /api/health returns ok', async () => {
    const res = await req('/api/health');
    assert.strictEqual(res.status, 200);
    const body = (await res.json()) as any;
    assert.strictEqual(body.status, 'ok');
  });

  test('GET /api/quote?symbol=AAPL returns stock quote', async () => {
    const res = await req('/api/quote?symbol=AAPL');
    assert.strictEqual(res.status, 200);
    const quote = (await res.json()) as any;
    assert.strictEqual(quote.symbol, 'AAPL');
    assert.ok(typeof quote.price === 'number');
    assert.ok(typeof quote.change === 'number');
    assert.ok(typeof quote.changePercent === 'number');
  });

  test('GET /api/history?symbol=AAPL&range=1mo returns candles', async () => {
    const res = await req('/api/history?symbol=AAPL&range=1mo');
    assert.strictEqual(res.status, 200);
    const history = (await res.json()) as any;
    assert.strictEqual(history.symbol, 'AAPL');
    assert.ok(Array.isArray(history.candles));
    assert.ok(history.candles.length > 0);
    const firstCandle = history.candles[0];
    assert.ok(firstCandle.timestamp);
    assert.ok(typeof firstCandle.open === 'number');
    assert.ok(typeof firstCandle.close === 'number');
    assert.ok(typeof firstCandle.high === 'number');
    assert.ok(typeof firstCandle.low === 'number');
  });

  test('GET /api/search?q=Tesla returns search results', async () => {
    const res = await req('/api/search?q=Tesla');
    assert.strictEqual(res.status, 200);
    const results = (await res.json()) as any[];
    assert.ok(Array.isArray(results));
    assert.ok(results.length > 0);
    assert.ok(results.some((r) => r.symbol === 'TSLA'));
  });

  test('GET /api/market/indices returns market indices', async () => {
    const res = await req('/api/market/indices');
    assert.strictEqual(res.status, 200);
    const indices = (await res.json()) as any[];
    assert.ok(Array.isArray(indices));
    assert.ok(indices.length > 0);
  });
});
