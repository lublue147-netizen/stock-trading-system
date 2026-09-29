import { StockQuote, HistoricalData, CandlePoint, SearchResult, MarketIndex } from '../types';
import { MOCK_STOCKS, MOCK_INDICES, SEARCH_DICTIONARY, generateMockCandles } from './mockData';

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

export async function fetchStockQuote(symbol: string): Promise<StockQuote> {
  const cleanSymbol = symbol.trim().toUpperCase();
  try {
    const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(cleanSymbol)}?range=1d&interval=1m`;
    const res = await fetch(url, {
      headers: {
        'User-Agent': USER_AGENT,
        'Accept': 'application/json'
      }
    });

    if (res.ok) {
      const data = (await res.json()) as any;
      const result = data?.chart?.result?.[0];
      if (result) {
        const meta = result.meta;
        const currentPrice = meta.regularMarketPrice ?? meta.chartPreviousClose ?? 0;
        const prevClose = meta.chartPreviousClose ?? meta.previousClose ?? currentPrice;
        const change = Math.round((currentPrice - prevClose) * 100) / 100;
        const changePercent = prevClose !== 0 ? Math.round(((currentPrice - prevClose) / prevClose) * 10000) / 100 : 0;

        return {
          symbol: cleanSymbol,
          name: meta.longName || meta.shortName || cleanSymbol,
          price: currentPrice,
          change,
          changePercent,
          currency: meta.currency || 'USD',
          exchange: meta.exchangeName || 'Unknown',
          open: meta.regularMarketOpen ?? currentPrice,
          high: meta.regularMarketDayHigh ?? currentPrice,
          low: meta.regularMarketDayLow ?? currentPrice,
          previousClose: prevClose,
          volume: meta.regularMarketVolume || 0,
          fiftyTwoWeekHigh: meta.fiftyTwoWeekHigh,
          fiftyTwoWeekLow: meta.fiftyTwoWeekLow,
          timestamp: (meta.regularMarketTime || Math.floor(Date.now() / 1000)) * 1000
        };
      }
    }
  } catch (err) {
    console.warn(`Failed to fetch live quote for ${cleanSymbol}:`, err);
  }

  // Fallback to mock data or generated data
  if (MOCK_STOCKS[cleanSymbol]) {
    return {
      ...MOCK_STOCKS[cleanSymbol].quote,
      timestamp: Date.now()
    };
  }

  // Synthetic fallback
  const base = 100 + (cleanSymbol.charCodeAt(0) * 2);
  return {
    symbol: cleanSymbol,
    name: `${cleanSymbol} Equity`,
    price: base,
    change: 1.25,
    changePercent: 1.25,
    currency: "USD",
    exchange: "NYSE",
    open: base - 0.5,
    high: base + 2.0,
    low: base - 1.0,
    previousClose: base - 1.25,
    volume: 1500000,
    timestamp: Date.now()
  };
}

export async function fetchBatchQuotes(symbols: string[]): Promise<StockQuote[]> {
  const promises = symbols.map(s => fetchStockQuote(s));
  return Promise.all(promises);
}

export async function fetchHistoricalData(symbol: string, range = '1mo', interval?: string): Promise<HistoricalData> {
  const cleanSymbol = symbol.trim().toUpperCase();

  // Pick suitable interval if not given
  let chosenInterval = interval;
  if (!chosenInterval) {
    switch (range) {
      case '1d': chosenInterval = '5m'; break;
      case '5d': chosenInterval = '15m'; break;
      case '1mo': chosenInterval = '1d'; break;
      case '6mo': chosenInterval = '1d'; break;
      case '1y': chosenInterval = '1d'; break;
      case 'all':
      case '5y': chosenInterval = '1wk'; break;
      default: chosenInterval = '1d';
    }
  }

  try {
    const url = `https://query1.finance.yahoo.com/v8/finance/chart/${encodeURIComponent(cleanSymbol)}?range=${range}&interval=${chosenInterval}`;
    const res = await fetch(url, {
      headers: {
        'User-Agent': USER_AGENT,
        'Accept': 'application/json'
      }
    });

    if (res.ok) {
      const data = (await res.json()) as any;
      const result = data?.chart?.result?.[0];
      if (result) {
        const timestamps: number[] = result.timestamp || [];
        const quotes = result.indicators?.quote?.[0] || {};
        const opens = quotes.open || [];
        const highs = quotes.high || [];
        const lows = quotes.low || [];
        const closes = quotes.close || [];
        const volumes = quotes.volume || [];

        const candles: CandlePoint[] = [];
        for (let i = 0; i < timestamps.length; i++) {
          const c = closes[i];
          if (c !== null && c !== undefined && !isNaN(c)) {
            candles.push({
              timestamp: timestamps[i] * 1000,
              open: Math.round((opens[i] ?? c) * 100) / 100,
              high: Math.round((highs[i] ?? c) * 100) / 100,
              low: Math.round((lows[i] ?? c) * 100) / 100,
              close: Math.round(c * 100) / 100,
              volume: Math.round(volumes[i] || 0)
            });
          }
        }

        if (candles.length > 0) {
          const high = Math.max(...candles.map(c => c.high));
          const low = Math.min(...candles.map(c => c.low));
          return {
            symbol: cleanSymbol,
            range,
            interval: chosenInterval,
            candles,
            meta: {
              currency: result.meta?.currency || 'USD',
              previousClose: result.meta?.chartPreviousClose || candles[0].open,
              high,
              low
            }
          };
        }
      }
    }
  } catch (err) {
    console.warn(`Historical fetch failed for ${cleanSymbol}:`, err);
  }

  // Fallback to generated candles
  const basePrice = MOCK_STOCKS[cleanSymbol]?.basePrice || 120.0;
  const candles = generateMockCandles(cleanSymbol, range, basePrice);
  const high = Math.max(...candles.map(c => c.high));
  const low = Math.min(...candles.map(c => c.low));

  return {
    symbol: cleanSymbol,
    range,
    interval: chosenInterval,
    candles,
    meta: {
      currency: MOCK_STOCKS[cleanSymbol]?.quote?.currency || 'USD',
      previousClose: candles[0]?.open || basePrice,
      high,
      low
    }
  };
}

export async function searchStocks(query: string): Promise<SearchResult[]> {
  const cleanQ = query.trim();
  if (!cleanQ) return [];

  try {
    const url = `https://query2.finance.yahoo.com/v1/finance/search?q=${encodeURIComponent(cleanQ)}&quotesCount=10&newsCount=0`;
    const res = await fetch(url, {
      headers: {
        'User-Agent': USER_AGENT,
        'Accept': 'application/json'
      }
    });

    if (res.ok) {
      const data = (await res.json()) as any;
      const quotes = data?.quotes || [];
      const results: SearchResult[] = quotes
        .filter((q: any) => q.symbol && (q.quoteType === 'EQUITY' || q.quoteType === 'ETF' || q.quoteType === 'INDEX'))
        .map((q: any) => ({
          symbol: q.symbol,
          name: q.shortname || q.longname || q.symbol,
          exchange: q.exchDisp || q.exchange || '',
          type: q.quoteType || 'EQUITY'
        }));

      if (results.length > 0) {
        return results;
      }
    }
  } catch (err) {
    console.warn(`Search error for ${cleanQ}:`, err);
  }

  // Fallback search dictionary filter
  const qLower = cleanQ.toLowerCase();
  return SEARCH_DICTIONARY.filter(item =>
    item.symbol.toLowerCase().includes(qLower) || item.name.toLowerCase().includes(qLower)
  );
}

export async function fetchMarketIndices(): Promise<MarketIndex[]> {
  const results: MarketIndex[] = [];
  for (const index of MOCK_INDICES) {
    try {
      const quote = await fetchStockQuote(index.symbol);
      results.push({
        symbol: index.symbol,
        name: index.name,
        price: quote.price,
        change: quote.change,
        changePercent: quote.changePercent
      });
    } catch {
      results.push(index);
    }
  }
  return results;
}
