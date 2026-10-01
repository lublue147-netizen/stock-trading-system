import { fetchStockQuote, fetchBatchQuotes, fetchHistoricalData, searchStocks, fetchMarketIndices } from './services/stockService';

const corsHeaders: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  'Access-Control-Max-Age': '86400',
};

function jsonResponse(data: unknown, status = 200, maxAge = 15): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': `public, max-age=${maxAge}, s-maxage=${maxAge * 2}`,
      ...corsHeaders,
    },
  });
}

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: corsHeaders,
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    if (path === '/' || path === '') {
      return jsonResponse({
        service: 'A股行情通 API',
        status: 'online',
        version: '1.0.5',
        endpoints: {
          health: '/api/health',
          quote: '/api/quote?symbol=600519.SS',
          quotes: '/api/quotes?symbols=600519.SS,002579.SZ,300750.SZ',
          history: '/api/history?symbol=600519.SS&range=1mo&interval=1d',
          search: '/api/search?q=中京电子',
          marketIndices: '/api/market/indices',
        },
        supportedRanges: ['1d', '5d', '1mo', '6mo', '1y', 'all'],
      });
    }

    if (path === '/api/health') {
      return jsonResponse({ status: 'ok', timestamp: Date.now() });
    }

    if (path === '/api/quote') {
      const symbol = url.searchParams.get('symbol');
      if (!symbol) {
        return jsonResponse({ error: 'Query parameter "symbol" is required' }, 400);
      }
      const quote = await fetchStockQuote(symbol);
      return jsonResponse(quote, 200, 10);
    }

    if (path === '/api/quotes') {
      const symbolsParam = url.searchParams.get('symbols');
      if (!symbolsParam) {
        return jsonResponse({ error: 'Query parameter "symbols" is required, comma-separated' }, 400);
      }
      const symbols = symbolsParam.split(',').map((s) => s.trim()).filter(Boolean);
      if (symbols.length === 0) {
        return jsonResponse([]);
      }
      const quotes = await fetchBatchQuotes(symbols);
      return jsonResponse(quotes, 200, 10);
    }

    if (path === '/api/history') {
      const symbol = url.searchParams.get('symbol');
      if (!symbol) {
        return jsonResponse({ error: 'Query parameter "symbol" is required' }, 400);
      }
      const range = url.searchParams.get('range') || '1mo';
      const interval = url.searchParams.get('interval') || undefined;
      const date = url.searchParams.get('date') || undefined;
      const history = await fetchHistoricalData(symbol, range, interval, date);
      return jsonResponse(history, 200, 30);
    }

    if (path === '/api/search') {
      const q = url.searchParams.get('q');
      if (!q) {
        return jsonResponse([]);
      }
      const results = await searchStocks(q);
      return jsonResponse(results, 200, 300);
    }

    if (path === '/api/market/indices') {
      const indices = await fetchMarketIndices();
      return jsonResponse(indices, 200, 15);
    }

    return jsonResponse({ error: 'Not Found' }, 404);
  },
};
