import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { fetchStockQuote, fetchBatchQuotes, fetchHistoricalData, searchStocks, fetchMarketIndices } from './services/stockService';

const app = new Hono();

// Enable CORS for all origins
app.use('*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'Authorization'],
  exposeHeaders: ['Content-Length'],
  maxAge: 86400,
}));

// Welcome / API Documentation
app.get('/', (c) => {
  return c.json({
    service: 'Stock Trading & Quotation API',
    status: 'online',
    version: '1.0.0',
    endpoints: {
      health: '/api/health',
      quote: '/api/quote?symbol=AAPL',
      quotes: '/api/quotes?symbols=AAPL,TSLA,NVDA',
      history: '/api/history?symbol=AAPL&range=1mo&interval=1d',
      search: '/api/search?q=Apple',
      marketIndices: '/api/market/indices'
    },
    supportedRanges: ['1d', '5d', '1mo', '6mo', '1y', 'all']
  });
});

// Health check
app.get('/api/health', (c) => {
  return c.json({ status: 'ok', timestamp: Date.now() });
});

// Single stock quote
app.get('/api/quote', async (c) => {
  const symbol = c.req.query('symbol');
  if (!symbol) {
    return c.json({ error: 'Query parameter "symbol" is required' }, 400);
  }

  const quote = await fetchStockQuote(symbol);
  c.header('Cache-Control', 'public, max-age=10, s-maxage=20');
  return c.json(quote);
});

// Batch stock quotes for Watchlist
app.get('/api/quotes', async (c) => {
  const symbolsParam = c.req.query('symbols');
  if (!symbolsParam) {
    return c.json({ error: 'Query parameter "symbols" is required, comma-separated' }, 400);
  }

  const symbols = symbolsParam.split(',').map(s => s.trim()).filter(Boolean);
  if (symbols.length === 0) {
    return c.json([]);
  }

  const quotes = await fetchBatchQuotes(symbols);
  c.header('Cache-Control', 'public, max-age=10, s-maxage=20');
  return c.json(quotes);
});

// Stock historical K-line & trend data
app.get('/api/history', async (c) => {
  const symbol = c.req.query('symbol');
  if (!symbol) {
    return c.json({ error: 'Query parameter "symbol" is required' }, 400);
  }

  const range = c.req.query('range') || '1mo';
  const interval = c.req.query('interval');

  const history = await fetchHistoricalData(symbol, range, interval);
  c.header('Cache-Control', 'public, max-age=30, s-maxage=60');
  return c.json(history);
});

// Search stocks by symbol or name
app.get('/api/search', async (c) => {
  const q = c.req.query('q');
  if (!q) {
    return c.json([]);
  }

  const results = await searchStocks(q);
  c.header('Cache-Control', 'public, max-age=300, s-maxage=600');
  return c.json(results);
});

// Market indices overview
app.get('/api/market/indices', async (c) => {
  const indices = await fetchMarketIndices();
  c.header('Cache-Control', 'public, max-age=15, s-maxage=30');
  return c.json(indices);
});

export default app;
