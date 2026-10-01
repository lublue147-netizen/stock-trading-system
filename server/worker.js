// Cloudflare Worker for A-Share Stock Quotation System
// Zero-dependency native ES module

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  'Access-Control-Max-Age': '86400',
};

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const A_SHARE_NAMES = {
  '000001.SS': '上证指数',
  '399001.SZ': '深证成指',
  '399006.SZ': '创业板指',
  '000688.SS': '科创50',
  '000300.SS': '沪深300',
  '000905.SS': '中证500',
  '600519.SS': '贵州茅台',
  '300750.SZ': '宁德时代',
  '002594.SZ': '比亚迪',
  '601318.SS': '中国平安',
  '600036.SS': '招商银行',
  '300059.SZ': '东方财富',
  '000001.SZ': '平安银行',
  '000858.SZ': '五粮液',
  '688981.SS': '中芯国际',
  '601857.SS': '中国石油',
  '601288.SS': '农业银行',
  '600900.SS': '长江电力',
  '601988.SS': '中国银行',
  '600028.SS': '中国石化',
  '601628.SS': '中国人寿',
  '002475.SZ': '立讯精密',
  '000333.SZ': '美的集团',
  '603288.SS': '海天味业',
  '300124.SZ': '汇川技术',
  '688111.SS': '金山办公',
  '002415.SZ': '海康威视',
  '601012.SS': '隆基绿能',
  '600276.SS': '恒瑞医药',
  '601888.SS': '中国中免',
  '300015.SZ': '爱尔眼科',
  '0700.HK': '腾讯控股',
  '9988.HK': '阿里巴巴',
  '3690.HK': '美团',
  '1810.HK': '小米集团',
  '^HSI': '恒生指数',
  '^GSPC': '标普 500',
  '^IXIC': '纳斯达克',
  '^DJI': '道琼斯'
};

const MARKET_INDICES = [
  { symbol: '000001.SS', name: '上证指数', price: 3841.88, change: 18.26, changePercent: 0.48 },
  { symbol: '399001.SZ', name: '深证成指', price: 12939.70, change: 80.95, changePercent: 0.63 },
  { symbol: '399006.SZ', name: '创业板指', price: 3152.28, change: 12.46, changePercent: 0.40 },
  { symbol: '000688.SS', name: '科创50', price: 1575.43, change: 19.45, changePercent: 1.25 },
  { symbol: '000300.SS', name: '沪深300', price: 4357.12, change: 16.36, changePercent: 0.38 },
  { symbol: '^HSI', name: '恒生指数', price: 20590.15, change: 312.45, changePercent: 1.54 },
];

const SEARCH_DICT = [
  { symbol: '600519.SS', name: '贵州茅台', pinyin: 'GZMT', exchange: '上交所', type: 'A股' },
  { symbol: '300750.SZ', name: '宁德时代', pinyin: 'NDSD', exchange: '深交所', type: 'A股' },
  { symbol: '002594.SZ', name: '比亚迪', pinyin: 'BYD', exchange: '深交所', type: 'A股' },
  { symbol: '601318.SS', name: '中国平安', pinyin: 'ZGPA', exchange: '上交所', type: 'A股' },
  { symbol: '600036.SS', name: '招商银行', pinyin: 'ZSYH', exchange: '上交所', type: 'A股' },
  { symbol: '300059.SZ', name: '东方财富', pinyin: 'DFCF', exchange: '深交所', type: 'A股' },
  { symbol: '000001.SZ', name: '平安银行', pinyin: 'PAYH', exchange: '深交所', type: 'A股' },
  { symbol: '000858.SZ', name: '五粮液', pinyin: 'WLY', exchange: '深交所', type: 'A股' },
  { symbol: '688981.SS', name: '中芯国际', pinyin: 'ZXGJ', exchange: '上交所', type: '科创板' },
  { symbol: '601857.SS', name: '中国石油', pinyin: 'ZGSY', exchange: '上交所', type: 'A股' },
  { symbol: '601288.SS', name: '农业银行', pinyin: 'NYYH', exchange: '上交所', type: 'A股' },
  { symbol: '600900.SS', name: '长江电力', pinyin: 'CJDL', exchange: '上交所', type: 'A股' },
  { symbol: '601988.SS', name: '中国银行', pinyin: 'ZGYH', exchange: '上交所', type: 'A股' },
  { symbol: '600028.SS', name: '中国石化', pinyin: 'ZGSH', exchange: '上交所', type: 'A股' },
  { symbol: '002475.SZ', name: '立讯精密', pinyin: 'LXJM', exchange: '深交所', type: 'A股' },
  { symbol: '000333.SZ', name: '美的集团', pinyin: 'MDJT', exchange: '深交所', type: 'A股' },
  { symbol: '603288.SS', name: '海天味业', pinyin: 'HTWY', exchange: '上交所', type: 'A股' },
  { symbol: '300124.SZ', name: '汇川技术', pinyin: 'HCJS', exchange: '深交所', type: 'A股' },
  { symbol: '688111.SS', name: '金山办公', pinyin: 'JSBG', exchange: '上交所', type: '科创板' },
  { symbol: '002415.SZ', name: '海康威视', pinyin: 'HKWS', exchange: '深交所', type: 'A股' },
  { symbol: '601012.SS', name: '隆基绿能', pinyin: 'LJLN', exchange: '上交所', type: 'A股' },
  { symbol: '600276.SS', name: '恒瑞医药', pinyin: 'HRYY', exchange: '上交所', type: 'A股' },
  { symbol: '601888.SS', name: '中国中免', pinyin: 'ZGZM', exchange: '上交所', type: 'A股' },
  { symbol: '300015.SZ', name: '爱尔眼科', pinyin: 'AEYK', exchange: '深交所', type: 'A股' },
  { symbol: '000001.SS', name: '上证指数', pinyin: 'SZZS', exchange: '上交所', type: '指数' },
  { symbol: '399001.SZ', name: '深证成指', pinyin: 'SZCZ', exchange: '深交所', type: '指数' },
  { symbol: '399006.SZ', name: '创业板指', pinyin: 'CYBZ', exchange: '深交所', type: '指数' },
  { symbol: '000688.SS', name: '科创50', pinyin: 'KC50', exchange: '上交所', type: '指数' },
  { symbol: '000300.SS', name: '沪深300', pinyin: 'HS300', exchange: '上交所', type: '指数' },
  { symbol: '0700.HK', name: '腾讯控股', pinyin: 'TXKG', exchange: '港交所', type: '港股' },
  { symbol: '9988.HK', name: '阿里巴巴', pinyin: 'ELBB', exchange: '港交所', type: '港股' },
  { symbol: '3690.HK', name: '美团', pinyin: 'MT', exchange: '港交所', type: '港股' },
  { symbol: '1810.HK', name: '小米集团', pinyin: 'XMGB', exchange: '港交所', type: '港股' },
  { symbol: 'AAPL', name: '苹果公司 (Apple)', pinyin: 'PGGS', exchange: 'NASDAQ', type: '美股' },
  { symbol: 'TSLA', name: '特斯拉 (Tesla)', pinyin: 'TSL', exchange: 'NASDAQ', type: '美股' },
  { symbol: 'NVDA', name: '英伟达 (NVIDIA)', pinyin: 'YWD', exchange: 'NASDAQ', type: '美股' }
];

function normalizeSymbol(symbol) {
  if (!symbol) return '';
  const s = symbol.trim().toUpperCase();
  if (/^\d{6}$/.test(s)) {
    if (s.startsWith('60') || s.startsWith('68') || s.startsWith('90')) return s + '.SS';
    if (s.startsWith('00') || s.startsWith('30') || s.startsWith('20') || s.startsWith('39')) return s + '.SZ';
    if (s.startsWith('8') || s.startsWith('4') || s.startsWith('92')) return s + '.BJ';
    return s + '.SS';
  }
  if (/^SH\d{6}$/i.test(s)) return s.substring(2) + '.SS';
  if (/^SZ\d{6}$/i.test(s)) return s.substring(2) + '.SZ';
  return s;
}

function getExchangeLabel(symbol) {
  if (symbol.endsWith('.SS')) return '上交所';
  if (symbol.endsWith('.SZ')) return '深交所';
  if (symbol.endsWith('.BJ')) return '北交所';
  if (symbol.endsWith('.HK')) return '港交所';
  if (symbol.startsWith('^')) return '指数';
  return '美股';
}

function getCurrencyLabel(symbol) {
  if (symbol.endsWith('.SS') || symbol.endsWith('.SZ') || symbol.endsWith('.BJ')) return 'CNY';
  if (symbol.endsWith('.HK')) return 'HKD';
  if (symbol.startsWith('^')) return '点';
  return 'USD';
}

function jsonRes(data, status = 200, maxAge = 15) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': 'public, max-age=' + maxAge + ', s-maxage=' + (maxAge * 2),
      ...CORS_HEADERS,
    },
  });
}

function getBasePrice(symbol) {
  let hash = 0;
  for (let i = 0; i < symbol.length; i++) {
    hash = (hash * 31 + symbol.charCodeAt(i)) & 0xffffffff;
  }
  if (symbol.endsWith('.SS') || symbol.endsWith('.SZ')) {
    return 15 + Math.abs(hash % 150) + Math.abs(hash % 99) / 100;
  }
  return 80 + Math.abs(hash % 350) + Math.abs(hash % 99) / 100;
}

function generateMockCandles(symbol, range, base) {
  let count = 30;
  let intervalMs = 24 * 60 * 60 * 1000;
  if (range === '1d') { count = 78; intervalMs = 5 * 60 * 1000; }
  else if (range === '5d') { count = 40; intervalMs = 30 * 60 * 1000; }
  else if (range === '1mo') { count = 30; intervalMs = 24 * 60 * 60 * 1000; }
  else if (range === '6mo') { count = 120; intervalMs = 24 * 60 * 60 * 1000; }
  else if (range === '1y') { count = 250; intervalMs = 24 * 60 * 60 * 1000; }
  else if (range === 'all') { count = 300; intervalMs = 7 * 24 * 60 * 60 * 1000; }

  const candles = [];
  let cur = base * 0.95;
  const now = Date.now();
  const startTime = now - count * intervalMs;

  for (let i = 0; i < count; i++) {
    const time = startTime + i * intervalMs;
    const seed = Math.sin(time + cur) * 10000;
    const changePct = ((seed - Math.floor(seed)) - 0.48) * 0.035;
    const open = Math.round(cur * 100) / 100;
    const close = Math.round(open * (1 + changePct) * 100) / 100;
    const high = Math.round(Math.max(open, close) * (1 + Math.abs(changePct) * 0.4) * 100) / 100;
    const low = Math.round(Math.min(open, close) * (1 - Math.abs(changePct) * 0.4) * 100) / 100;
    const volume = Math.floor(500000 + Math.abs(seed % 3000000));
    candles.push({ timestamp: time, open, high, low, close, volume });
    cur = close;
  }
  return candles;
}

function fallbackQuote(symbol) {
  const clean = normalizeSymbol(symbol);
  const found = SEARCH_DICT.find(s => s.symbol.toUpperCase() === clean);
  const zhName = A_SHARE_NAMES[clean] || (found ? found.name : clean);
  const base = getBasePrice(clean);
  const change = Math.round((Math.sin(clean.length + Date.now() / 100000) * base * 0.015) * 100) / 100;
  const changePct = Math.round((change / base) * 10000) / 100;
  return {
    symbol: clean,
    name: zhName,
    price: Math.round((base + change) * 100) / 100,
    change,
    changePercent: changePct,
    currency: getCurrencyLabel(clean),
    exchange: getExchangeLabel(clean),
    open: base,
    high: Math.round((base + Math.abs(change) * 1.5) * 100) / 100,
    low: Math.round((base - Math.abs(change) * 1.5) * 100) / 100,
    previousClose: base,
    volume: 1850000,
    fiftyTwoWeekHigh: Math.round(base * 1.35 * 100) / 100,
    fiftyTwoWeekLow: Math.round(base * 0.75 * 100) / 100,
    timestamp: Date.now()
  };
}

async function fetchQuote(symbol) {
  const clean = normalizeSymbol(symbol);
  try {
    const url = 'https://query1.finance.yahoo.com/v8/finance/chart/' + encodeURIComponent(clean) + '?range=1d&interval=1m';
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT, Accept: 'application/json' } });
    if (res.ok) {
      const data = await res.json();
      const result = data && data.chart && data.chart.result && data.chart.result[0];
      if (result) {
        const meta = result.meta;
        const currentPrice = meta.regularMarketPrice !== undefined ? meta.regularMarketPrice : (meta.chartPreviousClose || 0);
        const prevClose = meta.chartPreviousClose !== undefined ? meta.chartPreviousClose : (meta.previousClose || currentPrice);
        const change = Math.round((currentPrice - prevClose) * 100) / 100;
        const changePercent = prevClose !== 0 ? Math.round(((currentPrice - prevClose) / prevClose) * 10000) / 100 : 0;
        const defaultName = meta.longName || meta.shortName || clean;
        const displayName = A_SHARE_NAMES[clean] || defaultName;

        return {
          symbol: clean,
          name: displayName,
          price: Math.round(currentPrice * 100) / 100,
          change,
          changePercent,
          currency: getCurrencyLabel(clean),
          exchange: getExchangeLabel(clean),
          open: meta.regularMarketOpen !== undefined ? meta.regularMarketOpen : currentPrice,
          high: meta.regularMarketDayHigh !== undefined ? meta.regularMarketDayHigh : currentPrice,
          low: meta.regularMarketDayLow !== undefined ? meta.regularMarketDayLow : currentPrice,
          previousClose: prevClose,
          volume: meta.regularMarketVolume || 0,
          fiftyTwoWeekHigh: meta.fiftyTwoWeekHigh,
          fiftyTwoWeekLow: meta.fiftyTwoWeekLow,
          timestamp: (meta.regularMarketTime || Math.floor(Date.now() / 1000)) * 1000
        };
      }
    }
  } catch (e) {
    // fallback
  }
  return fallbackQuote(clean);
}

async function fetchHistory(symbol, range = '1mo', interval, date) {
  const clean = normalizeSymbol(symbol);
  const code = clean.replace(/\.(SS|SZ|BJ)$/i, '');
  const tCode = clean.endsWith('.SS') || code.startsWith('6') ? `sh${code}` :
                clean.endsWith('.BJ') || code.startsWith('8') || code.startsWith('4') || code.startsWith('920') ? `bj${code}` : `sz${code}`;

  if (range === '1d') {
    try {
      const url = `https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=${tCode}&scale=5&ma=no&datalen=1440`;
      const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
      if (res.ok) {
        const list = await res.json();
        if (Array.isArray(list) && list.length > 0) {
          const byDate = new Map();
          for (const item of list) {
            const d = item.day ? item.day.split(' ')[0] : '';
            if (!d) continue;
            if (!byDate.has(d)) byDate.set(d, []);
            byDate.get(d).push(item);
          }
          const sortedDates = Array.from(byDate.keys()).sort();
          if (sortedDates.length > 0) {
            const targetDate = (date && byDate.has(date)) ? date : sortedDates[sortedDates.length - 1];
            const targetIdx = sortedDates.indexOf(targetDate);
            const prevDate = targetIdx > 0 ? sortedDates[targetIdx - 1] : null;
            const prevClose = prevDate
              ? parseFloat(byDate.get(prevDate).slice(-1)[0].close) || 0
              : parseFloat(byDate.get(targetDate)[0].open) || 0;

            const dayBars = byDate.get(targetDate) || [];
            const candles = dayBars.map((item) => {
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

            const highs = candles.map(c => c.high);
            const lows = candles.map(c => c.low);
            return {
              symbol: clean,
              range: '1d',
              interval: '5m',
              candles,
              meta: {
                currency: 'CNY',
                previousClose: prevClose,
                high: highs.length > 0 ? Math.max(...highs) : 0,
                low: lows.length > 0 ? Math.min(...lows) : 0,
                selectedDate: targetDate,
                availableDates: sortedDates
              }
            };
          }
        }
      }
    } catch (e) {
      // fallback
    }
  }

  let chosenInterval = interval;
  if (!chosenInterval) {
    if (range === '1d') chosenInterval = '5m';
    else if (range === '5d') chosenInterval = '15m';
    else if (range === '1mo') chosenInterval = '1d';
    else if (range === '6mo') chosenInterval = '1d';
    else if (range === '1y') chosenInterval = '1d';
    else chosenInterval = '1wk';
  }

  try {
    const url = 'https://query1.finance.yahoo.com/v8/finance/chart/' + encodeURIComponent(clean) + '?range=' + range + '&interval=' + chosenInterval + '&includePrePost=false';
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT, Accept: 'application/json' } });
    if (res.ok) {
      const data = await res.json();
      const result = data && data.chart && data.chart.result && data.chart.result[0];
      if (result && result.timestamp && result.indicators && result.indicators.quote && result.indicators.quote[0]) {
        const timestamps = result.timestamp;
        const quote = result.indicators.quote[0];
        const candles = [];
        for (let i = 0; i < timestamps.length; i++) {
          const o = quote.open && quote.open[i];
          const h = quote.high && quote.high[i];
          const l = quote.low && quote.low[i];
          const c = quote.close && quote.close[i];
          const v = (quote.volume && quote.volume[i]) || 0;
          if (o != null && h != null && l != null && c != null) {
            candles.push({
              timestamp: timestamps[i] * 1000,
              open: Math.round(o * 100) / 100,
              high: Math.round(h * 100) / 100,
              low: Math.round(l * 100) / 100,
              close: Math.round(c * 100) / 100,
              volume: v
            });
          }
        }
        if (candles.length > 0) {
          const meta = result.meta;
          const highs = candles.map(c => c.high);
          const lows = candles.map(c => c.low);
          return {
            symbol: clean,
            range,
            interval: chosenInterval,
            candles,
            meta: {
              currency: 'CNY',
              previousClose: meta.chartPreviousClose || candles[0].open,
              high: Math.max(...highs),
              low: Math.min(...lows)
            }
          };
        }
      }
    }
  } catch (e) {
    // fallback
  }

  const base = getBasePrice(clean);
  const candles = generateMockCandles(clean, range, base);
  const highs = candles.map(c => c.high);
  const lows = candles.map(c => c.low);
  return {
    symbol: clean,
    range,
    interval: chosenInterval,
    candles,
    meta: {
      currency: 'CNY',
      previousClose: candles[0].open,
      high: Math.max(...highs),
      low: Math.min(...lows)
    }
  };
}

async function search(q) {
  const query = q.trim().toUpperCase();
  const queryLower = q.trim().toLowerCase();

  // Search within dictionary first (symbol, Chinese name, or Pinyin initials)
  const localMatches = SEARCH_DICT.filter(s =>
    s.symbol.toUpperCase().includes(query) ||
    s.name.includes(q.trim()) ||
    s.pinyin.toUpperCase().includes(query)
  );

  // If query is 6 digits, also check if direct normalized symbol exists
  if (/^\d{6}$/.test(query)) {
    const norm = normalizeSymbol(query);
    if (!localMatches.some(s => s.symbol === norm)) {
      const zhName = A_SHARE_NAMES[norm] || norm;
      localMatches.unshift({
        symbol: norm,
        name: zhName,
        pinyin: '',
        exchange: getExchangeLabel(norm),
        type: 'A股'
      });
    }
  }

  if (localMatches.length > 0) {
    return localMatches.map(m => ({
      symbol: m.symbol,
      name: m.name,
      exchange: m.exchange,
      type: m.type
    }));
  }

  try {
    const url = 'https://query1.finance.yahoo.com/v1/finance/search?q=' + encodeURIComponent(queryLower) + '&quotesCount=10&newsCount=0';
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT, Accept: 'application/json' } });
    if (res.ok) {
      const data = await res.json();
      const quotes = (data && data.quotes) || [];
      const results = quotes
        .filter(q => q.symbol && (q.quoteType === 'EQUITY' || q.quoteType === 'INDEX' || q.quoteType === 'ETF'))
        .map(q => {
          const norm = normalizeSymbol(q.symbol);
          return {
            symbol: norm,
            name: A_SHARE_NAMES[norm] || q.longname || q.shortname || norm,
            exchange: getExchangeLabel(norm),
            type: q.quoteType || 'Equity'
          };
        });
      if (results.length > 0) return results;
    }
  } catch (e) {
    // fallback
  }

  return SEARCH_DICT.slice(0, 10).map(m => ({
    symbol: m.symbol,
    name: m.name,
    exchange: m.exchange,
    type: m.type
  }));
}

export default {
  async fetch(request) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    if (path === '/' || path === '') {
      return jsonRes({
        service: 'China A-Share Stock Quotation API',
        status: 'online',
        version: '1.2.0',
        endpoints: {
          health: '/api/health',
          quote: '/api/quote?symbol=600519',
          quotes: '/api/quotes?symbols=600519,300750,002594',
          history: '/api/history?symbol=600519&range=1mo&interval=1d',
          search: '/api/search?q=BYD',
          marketIndices: '/api/market/indices',
        },
        supportedRanges: ['1d', '5d', '1mo', '6mo', '1y', 'all'],
      });
    }

    if (path === '/api/health') {
      return jsonRes({ status: 'ok', timestamp: Date.now() });
    }

    if (path === '/api/quote') {
      const symbol = url.searchParams.get('symbol');
      if (!symbol) return jsonRes({ error: 'Query parameter "symbol" is required' }, 400);
      const quote = await fetchQuote(symbol);
      return jsonRes(quote, 200, 10);
    }

    if (path === '/api/quotes') {
      const symbolsParam = url.searchParams.get('symbols');
      if (!symbolsParam) return jsonRes({ error: 'Query parameter "symbols" is required, comma-separated' }, 400);
      const symbols = symbolsParam.split(',').map(s => s.trim()).filter(Boolean);
      if (symbols.length === 0) return jsonRes([]);
      const quotes = await Promise.all(symbols.map(s => fetchQuote(s)));
      return jsonRes(quotes, 200, 10);
    }

    if (path === '/api/history') {
      const symbol = url.searchParams.get('symbol');
      if (!symbol) return jsonRes({ error: 'Query parameter "symbol" is required' }, 400);
      const range = url.searchParams.get('range') || '1mo';
      const interval = url.searchParams.get('interval') || undefined;
      const date = url.searchParams.get('date') || undefined;
      const history = await fetchHistory(symbol, range, interval, date);
      return jsonRes(history, 200, 30);
    }

    if (path === '/api/search') {
      const q = url.searchParams.get('q');
      if (!q) return jsonRes([]);
      const results = await search(q);
      return jsonRes(results, 200, 300);
    }

    if (path === '/api/market/indices') {
      try {
        const indexSymbols = MARKET_INDICES.map(i => i.symbol);
        const quotes = await Promise.all(indexSymbols.map(s => fetchQuote(s)));
        const liveIndices = quotes.map((q, idx) => ({
          symbol: q.symbol,
          name: MARKET_INDICES[idx].name,
          price: q.price,
          change: q.change,
          changePercent: q.changePercent
        }));
        return jsonRes(liveIndices, 200, 10);
      } catch (e) {
        return jsonRes(MARKET_INDICES, 200, 15);
      }
    }

    return jsonRes({ error: 'Not Found' }, 404);
  },
};
