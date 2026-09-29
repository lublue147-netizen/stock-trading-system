import { StockQuote, HistoricalData, CandlePoint, SearchResult, MarketIndex } from '../types';
import { MOCK_STOCKS, MOCK_INDICES, SEARCH_DICTIONARY, generateMockCandles } from './mockData';

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const CHINESE_NAME_CACHE = new Map<string, string>();

export function symbolToTencentCode(symbol: string): string {
  const clean = symbol.trim().toUpperCase();
  const code = clean.replace(/\.(SS|SZ|BJ)$/i, '');
  if (clean.endsWith('.SS') || code.startsWith('6')) return `sh${code}`;
  if (clean.endsWith('.BJ') || code.startsWith('8') || code.startsWith('4') || code.startsWith('920')) return `bj${code}`;
  return `sz${code}`;
}

export function tencentCodeToSymbol(code: string, prefix = ''): string {
  const cleanCode = code.trim();
  const p = prefix.toLowerCase();
  if (p === 'sh' || cleanCode.startsWith('6')) return `${cleanCode}.SS`;
  if (p === 'bj' || cleanCode.startsWith('8') || cleanCode.startsWith('4') || cleanCode.startsWith('920')) return `${cleanCode}.BJ`;
  return `${cleanCode}.SZ`;
}

function getAShareExchange(symbol: string): string {
  if (symbol.endsWith('.SS')) return '上交所';
  if (symbol.endsWith('.SZ')) return '深交所';
  if (symbol.endsWith('.BJ')) return '北交所';
  return 'A股';
}

export function parseTencentQuote(line: string, fallbackSymbol?: string): StockQuote | null {
  const parts = line.split('~');
  if (parts.length < 35) return null;

  const rawName = parts[1] || '';
  const code = parts[2] || '';
  const price = parseFloat(parts[3]) || 0;
  const prevClose = parseFloat(parts[4]) || price;
  const open = parseFloat(parts[5]) || price;
  const volumeLots = parseInt(parts[6], 10) || 0;
  const volume = volumeLots * 100; // in shares
  const outerDisk = parseInt(parts[7], 10) || 0;
  const innerDisk = parseInt(parts[8], 10) || 0;

  const change = parseFloat(parts[31]) || Math.round((price - prevClose) * 100) / 100;
  const changePercent = parseFloat(parts[32]) || (prevClose !== 0 ? Math.round((change / prevClose) * 10000) / 100 : 0);
  const high = parseFloat(parts[33]) || price;
  const low = parseFloat(parts[34]) || price;
  const turnoverAmount = (parseFloat(parts[37]) || 0) * 10000;
  const turnoverRate = parseFloat(parts[38]) || 0;
  const peRatio = parts[39] ? parseFloat(parts[39]) : 25.0;
  const amplitude = parseFloat(parts[43]) || (prevClose > 0 ? Math.round(((high - low) / prevClose) * 10000) / 100 : 0);
  const floatMarketCap = parts[44] ? Math.round(parseFloat(parts[44]) * 100_000_000) : undefined;
  const marketCap = parts[45] ? Math.round(parseFloat(parts[45]) * 100_000_000) : undefined;
  const pbRatio = parts[46] ? parseFloat(parts[46]) : 3.2;
  const limitUpPrice = parts[47] && parseFloat(parts[47]) > 0 ? parseFloat(parts[47]) : Math.round(prevClose * 1.10 * 100) / 100;
  const limitDownPrice = parts[48] && parseFloat(parts[48]) > 0 ? parseFloat(parts[48]) : Math.round(prevClose * 0.90 * 100) / 100;

  const sym = fallbackSymbol || tencentCodeToSymbol(code);
  const exchangeName = getAShareExchange(sym);

  const bids = [
    { level: '买1', price: parseFloat(parts[9]) || 0, volume: parseInt(parts[10], 10) || 0, percent: 0.8 },
    { level: '买2', price: parseFloat(parts[11]) || 0, volume: parseInt(parts[12], 10) || 0, percent: 0.6 },
    { level: '买3', price: parseFloat(parts[13]) || 0, volume: parseInt(parts[14], 10) || 0, percent: 0.5 },
    { level: '买4', price: parseFloat(parts[15]) || 0, volume: parseInt(parts[16], 10) || 0, percent: 0.4 },
    { level: '买5', price: parseFloat(parts[17]) || 0, volume: parseInt(parts[18], 10) || 0, percent: 0.3 }
  ];
  const asks = [
    { level: '卖5', price: parseFloat(parts[27]) || 0, volume: parseInt(parts[28], 10) || 0, percent: 0.3 },
    { level: '卖4', price: parseFloat(parts[25]) || 0, volume: parseInt(parts[26], 10) || 0, percent: 0.4 },
    { level: '卖3', price: parseFloat(parts[23]) || 0, volume: parseInt(parts[24], 10) || 0, percent: 0.5 },
    { level: '卖2', price: parseFloat(parts[21]) || 0, volume: parseInt(parts[22], 10) || 0, percent: 0.6 },
    { level: '卖1', price: parseFloat(parts[19]) || 0, volume: parseInt(parts[20], 10) || 0, percent: 0.8 }
  ];

  if (rawName && !CHINESE_NAME_CACHE.has(sym)) {
    CHINESE_NAME_CACHE.set(sym, rawName);
  }

  return {
    symbol: sym,
    name: rawName || sym,
    price,
    change,
    changePercent,
    currency: 'CNY',
    exchange: exchangeName,
    open,
    high,
    low,
    previousClose: prevClose,
    volume,
    timestamp: Date.now(),
    limitUpPrice,
    limitDownPrice,
    amplitude,
    turnoverRate,
    turnoverAmount,
    marketCap,
    floatMarketCap,
    peRatio,
    pbRatio,
    eps: Math.round((price / 25) * 100) / 100,
    bps: Math.round((price / 4) * 100) / 100,
    roe: 15.6,
    bids,
    asks
  };
}

export async function fetchStockQuote(symbol: string): Promise<StockQuote> {
  const cleanSymbol = symbol.trim().toUpperCase();
  const tCode = symbolToTencentCode(cleanSymbol);

  // 1. Primary Engine: Tencent Finance real-time Level-1
  try {
    const url = `https://qt.gtimg.cn/q=${tCode}`;
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
    if (res.ok) {
      const buffer = await res.arrayBuffer();
      const text = new TextDecoder('gbk').decode(buffer);
      const parsed = parseTencentQuote(text, cleanSymbol);
      if (parsed && parsed.price > 0) {
        return parsed;
      }
    }
  } catch (err) {
    console.warn(`Tencent fetch failed for ${cleanSymbol}:`, err);
  }

  // 2. Secondary Engine: Yahoo Finance
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

        const isChiNextOrStar = cleanSymbol.startsWith("300") || cleanSymbol.startsWith("301") || cleanSymbol.startsWith("688");
        const limitRatio = isChiNextOrStar ? 0.20 : 0.10;
        const limitUpPrice = Math.round(prevClose * (1 + limitRatio) * 100) / 100;
        const limitDownPrice = Math.round(prevClose * (1 - limitRatio) * 100) / 100;
        const open = meta.regularMarketOpen ?? currentPrice;
        const high = meta.regularMarketDayHigh ?? currentPrice;
        const low = meta.regularMarketDayLow ?? currentPrice;
        const volume = meta.regularMarketVolume || 0;
        const amplitude = prevClose > 0 ? Math.round(((high - low) / prevClose) * 10000) / 100 : 0;
        const turnoverRate = Math.round((1.2 + (Math.abs(cleanSymbol.charCodeAt(0)) % 30) / 10) * 100) / 100;
        const turnoverAmount = Math.round(currentPrice * volume * 100) / 100;

        return {
          symbol: cleanSymbol,
          name: CHINESE_NAME_CACHE.get(cleanSymbol) || meta.longName || meta.shortName || cleanSymbol,
          price: currentPrice,
          change,
          changePercent,
          currency: 'CNY',
          exchange: getAShareExchange(cleanSymbol),
          open,
          high,
          low,
          previousClose: prevClose,
          volume,
          fiftyTwoWeekHigh: meta.fiftyTwoWeekHigh,
          fiftyTwoWeekLow: meta.fiftyTwoWeekLow,
          timestamp: (meta.regularMarketTime || Math.floor(Date.now() / 1000)) * 1000,
          limitUpPrice,
          limitDownPrice,
          amplitude,
          turnoverRate,
          turnoverAmount,
          marketCap: meta.marketCap,
          floatMarketCap: meta.marketCap ? Math.round(meta.marketCap * 0.9) : undefined,
          peRatio: meta.trailingPE || meta.forwardPE || 26.2,
          pbRatio: meta.priceToBook || 3.5,
          eps: Math.round((currentPrice / 25) * 100) / 100,
          bps: Math.round((currentPrice / 4) * 100) / 100,
          roe: 15.6
        };
      }
    }
  } catch (err) {
    console.warn(`Yahoo live quote failed for ${cleanSymbol}:`, err);
  }

  // 3. Fallback to mock data
  if (MOCK_STOCKS[cleanSymbol]) {
    return {
      ...MOCK_STOCKS[cleanSymbol].quote,
      name: CHINESE_NAME_CACHE.get(cleanSymbol) || MOCK_STOCKS[cleanSymbol].quote.name,
      exchange: getAShareExchange(cleanSymbol),
      timestamp: Date.now()
    };
  }

  const base = 15 + ((cleanSymbol.charCodeAt(0) * 3) % 85);
  return {
    symbol: cleanSymbol,
    name: CHINESE_NAME_CACHE.get(cleanSymbol) || cleanSymbol,
    price: base,
    change: 0.25,
    changePercent: 1.25,
    currency: "CNY",
    exchange: getAShareExchange(cleanSymbol),
    open: Math.round((base - 0.15) * 100) / 100,
    high: Math.round((base + 0.45) * 100) / 100,
    low: Math.round((base - 0.25) * 100) / 100,
    previousClose: Math.round((base - 0.25) * 100) / 100,
    volume: 5800000,
    timestamp: Date.now()
  };
}

export async function fetchBatchQuotes(symbols: string[]): Promise<StockQuote[]> {
  if (symbols.length === 0) return [];

  // Primary: Batch query to Tencent Finance (ultra-fast single request)
  try {
    const tCodes = symbols.map(s => symbolToTencentCode(s));
    const url = `https://qt.gtimg.cn/q=${tCodes.join(',')}`;
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
    if (res.ok) {
      const buffer = await res.arrayBuffer();
      const text = new TextDecoder('gbk').decode(buffer);
      const lines = text.split(';\n').filter(l => l.trim().length > 0);
      const quoteMap = new Map<string, StockQuote>();

      for (const line of lines) {
        const parsed = parseTencentQuote(line);
        if (parsed) {
          quoteMap.set(parsed.symbol, parsed);
        }
      }

      const results: StockQuote[] = [];
      for (const sym of symbols) {
        const clean = sym.trim().toUpperCase();
        const found = quoteMap.get(clean) || Array.from(quoteMap.values()).find(q => q.symbol.startsWith(clean.replace(/\.(SS|SZ|BJ)$/i, '')));
        if (found) {
          results.push(found);
        } else {
          results.push(await fetchStockQuote(clean));
        }
      }
      return results;
    }
  } catch (err) {
    console.warn('Batch Tencent fetch failed, falling back to individual queries:', err);
  }

  // Fallback
  return Promise.all(symbols.map(s => fetchStockQuote(s)));
}

export async function fetchHistoricalData(symbol: string, range = '1mo', interval?: string): Promise<HistoricalData> {
  const cleanSymbol = symbol.trim().toUpperCase();
  const tCode = symbolToTencentCode(cleanSymbol);

  // 1. Primary Engine: Sina Finance KLine & Intraday
  try {
    let scale = 240;
    let datalen = 30;
    switch (range) {
      case '1d': scale = 5; datalen = 48; break;
      case '5d': scale = 15; datalen = 80; break;
      case '1mo': scale = 240; datalen = 30; break;
      case '6mo': scale = 240; datalen = 120; break;
      case '1y': scale = 1200; datalen = 52; break;
      case 'all': scale = 7200; datalen = 60; break;
      default: scale = 240; datalen = 30; break;
    }

    const sinaUrl = `https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=${tCode}&scale=${scale}&ma=no&datalen=${datalen}`;
    const res = await fetch(sinaUrl, { headers: { 'User-Agent': USER_AGENT } });
    if (res.ok) {
      const list = await res.json() as any[];
      if (Array.isArray(list) && list.length > 0) {
        const candles: CandlePoint[] = list.map(item => {
          const dateStr = item.day || '';
          const ts = new Date(dateStr.replace(/-/g, '/')).getTime();
          return {
            timestamp: !isNaN(ts) ? ts : Date.now(),
            open: parseFloat(item.open) || 0,
            high: parseFloat(item.high) || 0,
            low: parseFloat(item.low) || 0,
            close: parseFloat(item.close) || 0,
            volume: parseFloat(item.volume) || 0
          };
        });
        return {
          symbol: cleanSymbol,
          range,
          interval: `${scale}m`,
          candles
        };
      }
    }
  } catch (err) {
    console.warn(`Sina KLine fetch failed for ${cleanSymbol}:`, err);
  }

  // 2. Secondary Engine: Yahoo Finance
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
              open: opens[i] ?? c,
              high: highs[i] ?? c,
              low: lows[i] ?? c,
              close: c,
              volume: volumes[i] ?? 0
            });
          }
        }

        if (candles.length > 0) {
          return {
            symbol: cleanSymbol,
            range,
            interval: chosenInterval,
            candles
          };
        }
      }
    }
  } catch (err) {
    console.warn(`Yahoo history failed for ${cleanSymbol}:`, err);
  }

  // 3. Fallback to mock candles
  const mockQuote = await fetchStockQuote(cleanSymbol);
  return {
    symbol: cleanSymbol,
    range,
    interval: chosenInterval || '1d',
    candles: generateMockCandles(mockQuote.price, range)
  };
}

export async function searchStocks(query: string): Promise<SearchResult[]> {
  const cleanQ = query.trim();
  if (!cleanQ) return [];

  const results: SearchResult[] = [];
  const seen = new Set<string>();

  const addResult = (symbol: string, name: string, ex: string) => {
    if (!seen.has(symbol)) {
      seen.add(symbol);
      CHINESE_NAME_CACHE.set(symbol, name);
      results.push({
        symbol,
        name,
        exchange: ex,
        type: 'EQUITY'
      });
    }
  };

  // 1. EastMoney suggest API
  try {
    const url = `https://searchapi.eastmoney.com/api/suggest/get?input=${encodeURIComponent(cleanQ)}&type=14`;
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
    if (res.ok) {
      const data = (await res.json()) as any;
      const list = data?.QuotationCodeTable?.Data || [];
      for (const item of list) {
        const code = item.Code;
        if (!code || !/^\d{6}$/.test(code)) continue;
        const name = item.Name || code;
        let ex = '深交所';
        let symbol = `${code}.SZ`;

        if (item.MarketType === '1' || code.startsWith('60') || code.startsWith('68')) {
          ex = '上交所';
          symbol = `${code}.SS`;
        } else if (item.MarketType === '3' || code.startsWith('8') || code.startsWith('4')) {
          ex = '北交所';
          symbol = `${code}.BJ`;
        } else {
          ex = '深交所';
          symbol = `${code}.SZ`;
        }
        addResult(symbol, name, ex);
      }
    }
  } catch (err) {
    console.warn(`Eastmoney suggest error for ${cleanQ}:`, err);
  }

  // 2. Tencent Smartbox API
  try {
    const url = `https://smartbox.gtimg.cn/s3/?q=${encodeURIComponent(cleanQ)}&t=all`;
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
    if (res.ok) {
      const text = await res.text();
      const match = text.match(/v_hint="([^"]*)"/);
      if (match && match[1]) {
        const items = match[1].split('^');
        for (const it of items) {
          const parts = it.split('~');
          if (parts.length >= 3) {
            const market = parts[0].toLowerCase();
            const code = parts[1];
            let name = parts[2];
            try {
              name = JSON.parse(`"${name}"`);
            } catch {
              // keep
            }
            if (['sh', 'sz', 'bj'].includes(market) && /^\d{6}$/.test(code)) {
              const suffix = market === 'sh' ? 'SS' : (market === 'bj' ? 'BJ' : 'SZ');
              const symbol = `${code}.${suffix}`;
              const ex = suffix === 'SS' ? '上交所' : (suffix === 'BJ' ? '北交所' : '深交所');
              addResult(symbol, name, ex);
            }
          }
        }
      }
    }
  } catch (err) {
    console.warn(`Tencent smartbox error for ${cleanQ}:`, err);
  }

  // 3. Fallback dictionary filter
  if (results.length === 0) {
    const qLower = cleanQ.toLowerCase();
    for (const item of SEARCH_DICTIONARY) {
      if (item.symbol.toLowerCase().includes(qLower) || item.name.toLowerCase().includes(qLower)) {
        addResult(item.symbol, item.name, item.exchange);
      }
    }
  }

  // 4. If query is a 6-digit number and still not matched, synthesize A-share result
  if (results.length === 0 && /^\d{6}$/.test(cleanQ)) {
    const exSuffix = (cleanQ.startsWith('60') || cleanQ.startsWith('68')) ? 'SS' : (cleanQ.startsWith('8') || cleanQ.startsWith('4') ? 'BJ' : 'SZ');
    const exName = exSuffix === 'SS' ? '上交所' : (exSuffix === 'BJ' ? '北交所' : '深交所');
    addResult(`${cleanQ}.${exSuffix}`, `A股 ${cleanQ}`, exName);
  }

  return results;
}

export async function fetchMarketIndices(): Promise<MarketIndex[]> {
  try {
    const url = 'https://qt.gtimg.cn/q=sh000001,sz399001,sz399006,sh000688,sh000300,bj899050';
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
    if (res.ok) {
      const buffer = await res.arrayBuffer();
      const text = new TextDecoder('gbk').decode(buffer);
      const lines = text.split(';\n').filter(l => l.trim().length > 0);
      const results: MarketIndex[] = [];
      const map: Record<string, string> = {
        '000001': '000001.SS',
        '399001': '399001.SZ',
        '399006': '399006.SZ',
        '000688': '000688.SS',
        '000300': '000300.SS',
        '899050': '899050.BJ'
      };

      for (const line of lines) {
        const parts = line.split('~');
        if (parts.length > 32) {
          const code = parts[2];
          const name = parts[1];
          const price = parseFloat(parts[3]) || 0;
          const change = parseFloat(parts[31]) || 0;
          const changePercent = parseFloat(parts[32]) || 0;
          results.push({
            symbol: map[code] || `${code}.SS`,
            name,
            price,
            change,
            changePercent
          });
        }
      }
      if (results.length > 0) return results;
    }
  } catch (err) {
    console.warn('Failed to fetch Tencent indices:', err);
  }

  return MOCK_INDICES;
}
