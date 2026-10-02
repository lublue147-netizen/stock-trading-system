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


const SECTOR_NAMES = {
  'BK1638': '最近多板',
  'BK1050': '昨日涨停-含一字',
  'BK1715': '趋势股',
  'BK1675': '历史新高',
  'BK1036': '半导体',
  'BK0473': '证券',
  'BK0896': '白酒',
  'BK1033': '电池',
  'BK1029': '汽车整车',
  'BK1166': '低空经济',
  'BK0459': '电子元件',
  'BK0737': '软件开发',
  'BK0735': '计算机设备',
  'BK0448': '通信设备',
  'BK0475': '银行',
  'BK0474': '保险',
  'BK1031': '光伏设备',
  'BK0545': '通用设备',
  'BK0733': '工程机械',
  'BK0465': '化学制药',
  'BK1040': '中药',
  'BK0438': '食品饮料',
  'BK0428': '电力行业',
  'BK0478': '有色金属',
  'BK1037': '消费电子',
  'BK1038': '光学光电子',
  'BK1184': '人形机器人',
  'BK0854': '华为概念'
};

const SECTOR_STOCKS = {
  'BK1638': [
    'sz000536', 'sz002583', 'sh603268', 'sh603106', 'sz002094', 'sh600292', 'sz000958', 'sh603656',
    'sz001696', 'sz000062', 'sh600611', 'sz300085', 'sz000158', 'sz300339', 'sz002261', 'sz002456',
    'sz002423', 'sh600839', 'sh601727', 'sz002085', 'sz000099', 'sz301628', 'sh688656', 'sh603038',
    'sz002272', 'sh603887', 'sz002725', 'sh600653', 'sh603117', 'sh600811', 'sz300377', 'sz300061',
    'sz300380', 'sz300561', 'sz300046', 'sz301297', 'sz300489'
  ],
  'BK1050': [
    'sz300085', 'sz000158', 'sz300339', 'sz002261', 'sz001696', 'sz002456', 'sh600839', 'sh601727',
    'sz002583', 'sz000536', 'sh603268', 'sz002094', 'sh600292', 'sz000958', 'sh603106', 'sz300046',
    'sz301297', 'sz300489', 'sz300757', 'sz300476', 'sh688256', 'sz002085', 'sz000099', 'sh603038',
    'sz002272', 'sz300442', 'sz301236', 'sz300598', 'sh600187', 'sz300061', 'sz300377', 'sz300380',
    'sz300561', 'sz002786', 'sz300537'
  ],
  'BK1715': [
    'sz300750', 'sz002594', 'sh601127', 'sz300308', 'sz300502', 'sh601138', 'sz002463', 'sz300476',
    'sh688256', 'sz000977', 'sh603019', 'sz300124', 'sz002371', 'sh688012', 'sh688981', 'sh688041',
    'sh688008', 'sz002049', 'sh603986', 'sh603501', 'sz300274', 'sz300014', 'sh601689', 'sz002050',
    'sz000625', 'sh601633', 'sz002475', 'sz002241', 'sz300433', 'sz000063', 'sz300394', 'sh601899',
    'sh603993', 'sh601600', 'sh600150'
  ],
  'BK1675': [
    'sh688256', 'sz300476', 'sz002463', 'sz002130', 'sz002851', 'sz300757', 'sh601138', 'sz300502',
    'sz300308', 'sz300394', 'sz001696', 'sz300339', 'sz000158', 'sz301236', 'sz002261', 'sz300442',
    'sh688041', 'sh688047', 'sh688008', 'sh688692', 'sh688012', 'sz002371', 'sz300661', 'sh688536',
    'sh601127', 'sh600418', 'sz002594', 'sz300750', 'sh688205', 'sh688498', 'sh688183', 'sh688126',
    'sz301269', 'sh688072', 'sh688037'
  ],
  'BK1036': [
    'sh688981', 'sz002371', 'sh688012', 'sh688041', 'sh688256', 'sh603501', 'sh688008', 'sh603986',
    'sh600584', 'sz002049', 'sh688072', 'sh688396', 'sz002156', 'sh600460', 'sz300782', 'sz300661',
    'sh688126', 'sz301269', 'sh688047', 'sz002185', 'sh688536', 'sh688037', 'sh688180', 'sh688099',
    'sh688521', 'sh688123', 'sh688018', 'sh688052', 'sz300373', 'sz300671'
  ],
  'BK0473': [
    'sz300059', 'sh600030', 'sh601211', 'sh601688', 'sh600999', 'sz000776', 'sz000166', 'sh601881',
    'sh601066', 'sh601788', 'sh600958', 'sh601878', 'sz000750', 'sz002736', 'sh600837', 'sz000783',
    'sh601377', 'sh601456', 'sh601108', 'sh601901', 'sz002673', 'sh600369', 'sz002926', 'sz000686',
    'sh601990', 'sh601099', 'sh601236', 'sh601198', 'sh600906', 'sh601908'
  ],
  'BK0896': [
    'sh600519', 'sz000858', 'sz000568', 'sh600809', 'sz002304', 'sz000596', 'sh603369', 'sh600779',
    'sh600702', 'sz000799', 'sh603198', 'sh603589', 'sh603919', 'sz000860', 'sz000995', 'sh600199',
    'sz000729', 'sh600559'
  ],
  'BK1033': [
    'sz300750', 'sz300014', 'sz002074', 'sz300207', 'sz300769', 'sh688005', 'sz300073', 'sz002812',
    'sz300568', 'sz002709', 'sz300037', 'sh603659', 'sz300035', 'sh600884', 'bj835185', 'sh688063',
    'sz300438', 'sh688772', 'sh688567', 'sz300894', 'sz002850', 'sz001301', 'sz301358', 'sh688778',
    'sz301152', 'sh688275', 'sh688148', 'sh603799', 'sz002460', 'sz002466'
  ],
  'BK1029': [
    'sz002594', 'sh601127', 'sh600104', 'sz000625', 'sh601633', 'sh601238', 'sh600418', 'sh600733',
    'sh600166', 'sh600066', 'sz000957', 'sz000951', 'sz000800', 'sh601777', 'sz000982', 'sh600006',
    'sz000868', 'sz000550'
  ],
  'BK1166': [
    'sz002085', 'sz000099', 'sz001696', 'sh600580', 'sh688631', 'sh688070', 'sz300900', 'sz002389',
    'sh600038', 'sh600316', 'sh600990', 'sz002253', 'sz300411', 'sz300107', 'sz300719', 'sz000677',
    'sz300476', 'sh688017', 'sz300627', 'sz300878', 'sh688522', 'sz300484', 'sz300887', 'sh600843',
    'sz301091', 'sz300732', 'sz300284', 'sz301305', 'sh603018', 'sh603105'
  ],
  'BK0459': [
    'sz002579', 'sz002463', 'sh600183', 'sz300476', 'sz002916', 'sz002384', 'sz002475', 'sz002938',
    'sz002815', 'sz002138', 'sz300739', 'sh603328', 'sh603920', 'sz002913', 'sz002888', 'sz002859',
    'sh688183', 'sz300131', 'sz002635', 'sh605358', 'sz300969', 'sz300963', 'sh600171', 'sz002130'
  ],
  'BK0737': [
    'sz300033', 'sh600570', 'sh688111', 'sz002230', 'sh600588', 'sh601360', 'sz300339', 'sz000158',
    'sz301236', 'sz002261', 'sz300598', 'sh600536', 'sh600845', 'sh603039', 'sz300253', 'sz300674',
    'sz300663', 'sz300348', 'sz300468', 'sz300768', 'sh603223', 'sz300377', 'sz300380', 'sz300561'
  ],
  'BK0735': [
    'sz000977', 'sh603019', 'sz000938', 'sz000066', 'sh601138', 'sz002415', 'sz002236', 'sz002180',
    'sz002152', 'sh603106', 'sz300531', 'sz000997', 'sz002376', 'sz002177', 'sz002197', 'sz002362',
    'sh688036', 'sh600850', 'sz002841', 'sz300857'
  ],
  'BK0448': [
    'sz000063', 'sz300308', 'sz300502', 'sz300394', 'sh600498', 'sh600487', 'sh600522', 'sh603083',
    'sz000988', 'sz002281', 'sh688205', 'sh688498', 'sz300548', 'sz300570', 'sz002902', 'sz300780',
    'sz300638', 'sz002467', 'sz300806', 'sz002881', 'sz002792', 'sz002130'
  ],
  'BK0475': [
    'sh600036', 'sz000001', 'sh601398', 'sh601939', 'sh601288', 'sh601988', 'sh601328', 'sh601658',
    'sh601166', 'sh600016', 'sh600000', 'sh601818', 'sh601998', 'sh601229', 'sh600919', 'sh601009',
    'sz002142', 'sh600926', 'sh601860', 'sz002807'
  ],
  'BK0474': [
    'sh601318', 'sh601628', 'sh601601', 'sh601319', 'sh601336'
  ],
  'BK1031': [
    'sh601012', 'sh600438', 'sz300274', 'sh688599', 'sh688223', 'sz002129', 'sh600089', 'sh601877',
    'sz002459', 'sh600732', 'sz300763', 'sz002506', 'sz002056', 'sh688303', 'sh688472', 'sz300118'
  ],
  'BK0545': [
    'sz002050', 'sz002085', 'sz002096', 'sh688017', 'sz002472', 'sz002595', 'sz002444', 'sh603131',
    'sz002833', 'sz300803', 'sz300580', 'sz300450', 'sz002837', 'sh603095', 'sh688328'
  ],
  'BK0733': [
    'sh600031', 'sz000157', 'sz000425', 'sh601100', 'sh600761', 'sh600984', 'sz000680', 'sz000528',
    'sh603298', 'sh603338', 'sh603638', 'sz000996', 'sz002097', 'sz000821'
  ],
  'BK0465': [
    'sh600276', 'sh603259', 'sz000963', 'sh600079', 'sz002422', 'sh600426', 'sz002773', 'sz002294',
    'sh600521', 'sh600867', 'sh600518', 'sz002370', 'sz002332', 'sz000513', 'sh600789'
  ],
  'BK1040': [
    'sh600436', 'sz000538', 'sh600085', 'sh600993', 'sh600329', 'sz000999', 'sz000423', 'sh600572',
    'sz002603', 'sh600285', 'sh600771', 'sh600535', 'sz000989', 'sh600080', 'sz002412'
  ],
  'BK0438': [
    'sh603288', 'sh600887', 'sh600298', 'sz000895', 'sh603345', 'sz002557', 'sz002507', 'sh600305',
    'sz002847', 'sz002714', 'sz002216', 'sh603777', 'sh603886', 'sz300783'
  ],
  'BK0428': [
    'sh600900', 'sh601985', 'sh600011', 'sh600027', 'sh600023', 'sh601991', 'sh600886', 'sh600795',
    'sh600863', 'sh600674', 'sh600163', 'sh600509', 'sz000543', 'sz000037', 'sh600025'
  ],
  'BK0478': [
    'sh601899', 'sh603993', 'sh601600', 'sh600362', 'sh600489', 'sh600547', 'sh600988', 'sz000878',
    'sz000630', 'sh601958', 'sh600111', 'sz002460', 'sz002466', 'sh600392', 'sh601168'
  ],
  'BK1037': [
    'sz002475', 'sz002241', 'sz002600', 'sz300433', 'sz300136', 'sh688036', 'sz002384', 'sz002938',
    'sz002841', 'sz002045', 'sz300476', 'sz002456', 'sz000062', 'sz300693', 'sz300328'
  ],
  'BK1038': [
    'sz000725', 'sz000536', 'sz002456', 'sz002036', 'sh600584', 'sh600703', 'sz002217', 'sz002449',
    'sz300327', 'sz300686', 'sz300331', 'sz002876', 'sh603505', 'sh688001'
  ],
  'BK1184': [
    'sz300124', 'sh688017', 'sz002050', 'sz002472', 'sh600580', 'sz002896', 'sz002595', 'sz300803',
    'sz300450', 'sh603131', 'sz002747', 'sh603095', 'sz300607', 'sz300161', 'sz300747'
  ],
  'BK0854': [
    'sh601127', 'sz000158', 'sz300339', 'sz002261', 'sz002456', 'sz000536', 'sz002583', 'sh600839',
    'sh688256', 'sz300476', 'sz002463', 'sz000977', 'sh603019', 'sh688041', 'sz301236', 'sh600588'
  ]
};

async function fetchSinaTopGainers() {
  try {
    const url = 'http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=1&num=40&sort=changepercent&asc=0&node=hs_a';
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 2000);
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal: controller.signal });
    clearTimeout(timeout);
    if (res.ok) {
      const buffer = await res.arrayBuffer();
      const text = new TextDecoder('gbk').decode(buffer);
      const list = JSON.parse(text);
      if (Array.isArray(list)) {
        return list
          .filter(item => (parseFloat(item.changepercent) || 0) >= 9.5)
          .map(item => item.symbol.toLowerCase());
      }
    }
  } catch (_) {}
  return [];
}

async function fetchTencentBatchQuotes(tCodes) {
  if (!tCodes || tCodes.length === 0) return [];
  try {
    const url = `https://qt.gtimg.cn/q=${tCodes.join(',')}`;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 3500);
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal: controller.signal });
    clearTimeout(timeout);
    if (res.ok) {
      const buffer = await res.arrayBuffer();
      const text = new TextDecoder('gbk').decode(buffer);
      const lines = text.split(';').map(l => l.trim()).filter(Boolean);
      const items = [];
      for (const line of lines) {
        const parts = line.split('~');
        if (parts.length > 38) {
          const code = parts[2];
          const name = parts[1];
          const price = parseFloat(parts[3]) || 0;
          const prevClose = parseFloat(parts[4]) || price;
          const open = parseFloat(parts[5]) || price;
          const vol = (parseInt(parts[6], 10) || 0) * 100;
          const chg = parseFloat(parts[31]) || (Math.round((price - prevClose) * 100) / 100);
          const chgPct = parseFloat(parts[32]) || (prevClose > 0 ? Math.round(((price - prevClose) / prevClose) * 10000) / 100 : 0);
          const high = parseFloat(parts[33]) || price;
          const low = parseFloat(parts[34]) || price;
          const turnover = (parseFloat(parts[37]) || 0) * 10000;
          const turnoverRate = parseFloat(parts[38]) || 0;
          items.push({
            f12: code,
            f14: name,
            f2: price,
            f3: chgPct,
            f4: chg,
            f5: vol,
            f6: turnover,
            f7: turnoverRate,
            f15: high,
            f16: low,
            f17: open,
            f18: prevClose
          });
        }
      }
      return items;
    }
  } catch (_) {}
  return [];
}

async function getSectorConstituentsFallback(cleanBk, pn = 1, pz = 100) {
  let symbols = SECTOR_STOCKS[cleanBk] ? [...SECTOR_STOCKS[cleanBk]] : [];

  if (cleanBk === 'BK1638') {
    const liveLimitUp = await fetchSinaTopGainers();
    if (liveLimitUp.length > 0) {
      const set = new Set([...liveLimitUp, ...symbols]);
      symbols = Array.from(set);
    }
  }

  if (symbols.length === 0) {
    symbols = SECTOR_STOCKS['BK1715'] || [];
  }

  const uniqueSymbols = Array.from(new Set(symbols));
  const quotes = await fetchTencentBatchQuotes(uniqueSymbols);
  const sectorName = SECTOR_NAMES[cleanBk] || cleanBk;

  const items = quotes.map(q => ({
    ...q,
    f100: sectorName
  }));

  // Sort by change percent descending (same as EastMoney fid=f3&po=1)
  items.sort((a, b) => b.f3 - a.f3);

  const pageNum = parseInt(pn, 10) || 1;
  const pageSize = parseInt(pz, 10) || 100;
  const start = (pageNum - 1) * pageSize;
  const paged = items.slice(start, start + pageSize);

  return {
    rc: 0,
    rt: 17,
    svr: 2887138014,
    lt: 2,
    full: 0,
    dlmkts: '',
    data: {
      total: items.length,
      diff: paged
    }
  };
}

async function getSectorQuoteFallback(cleanBk) {
  const constituentsResult = await getSectorConstituentsFallback(cleanBk, 1, 100);
  const items = (constituentsResult && constituentsResult.data && constituentsResult.data.diff) || [];
  const sectorName = SECTOR_NAMES[cleanBk] || cleanBk;

  const validItems = items.filter(it => it.f2 > 0);
  const avgChgPct = validItems.length > 0
    ? Math.round((validItems.reduce((acc, it) => acc + it.f3, 0) / validItems.length) * 100) / 100
    : 0.0;
  const basePrice = 1000.0;
  const currentPrice = Math.round(basePrice * (1.0 + avgChgPct / 100.0) * 100) / 100;
  const change = Math.round((currentPrice - basePrice) * 100) / 100;
  const sumTurnover = items.reduce((acc, it) => acc + (it.f6 || 0), 0);
  const sumVol = items.reduce((acc, it) => acc + (it.f5 || 0), 0);
  const nonZeroTr = items.filter(it => it.f7 > 0);
  const avgTurnoverRate = nonZeroTr.length > 0
    ? Math.round((nonZeroTr.reduce((acc, it) => acc + it.f7, 0) / nonZeroTr.length) * 100) / 100
    : 0.0;

  const maxChg = validItems.length > 0 ? Math.max(...validItems.map(it => it.f3)) : avgChgPct;
  const minChg = validItems.length > 0 ? Math.min(...validItems.map(it => it.f3)) : avgChgPct;
  const highPrice = Math.round(basePrice * (1.0 + Math.max(maxChg, avgChgPct, 0.0) / 100.0) * 100) / 100;
  const lowPrice = Math.round(basePrice * (1.0 + Math.min(minChg, avgChgPct, 0.0) / 100.0) * 100) / 100;

  return {
    rc: 0,
    rt: 17,
    data: {
      diff: [
        {
          f12: cleanBk,
          f14: sectorName,
          f2: currentPrice,
          f3: avgChgPct,
          f4: change,
          f5: sumVol,
          f6: sumTurnover,
          f7: avgTurnoverRate,
          f15: highPrice,
          f16: lowPrice,
          f17: basePrice,
          f18: basePrice
        }
      ]
    }
  };
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
        version: '1.4.0',
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

        // Proxy: East Money sector constituent list (multi-node failover with Sina & Tencent fallback)
    if (path === '/api/sector/constituents') {
      const bk = url.searchParams.get('bk');
      const pn = url.searchParams.get('pn') || '1';
      const pz = url.searchParams.get('pz') || '100';
      if (!bk) return jsonRes({ error: 'Query parameter "bk" is required' }, 400);
      const clean = bk.trim().toUpperCase();
      const candidates = [
        `https://29.push2.eastmoney.com/api/qt/clist/get?pn=${pn}&pz=${pz}&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f3&fs=b:${clean}+f:!50&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f100`,
        `https://79.push2.eastmoney.com/api/qt/clist/get?pn=${pn}&pz=${pz}&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f3&fs=b:${clean}+f:!50&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f100`,
        `https://pushguest.eastmoney.com/api/qt/clist/get?pn=${pn}&pz=${pz}&po=1&np=1&ut=fa5fd1943c7b386f172d6893dbfba10b&fltt=2&invt=2&fid=f3&fs=b:${clean}+f:!50&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f100`
      ];

      for (const targetUrl of candidates) {
        try {
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), 1500);
          const res = await fetch(targetUrl, {
            headers: {
              'User-Agent': USER_AGENT,
              'Referer': 'https://quote.eastmoney.com/',
              'Accept': '*/*',
            },
            signal: controller.signal
          });
          clearTimeout(timeout);
          if (res.ok) {
            const data = await res.json();
            if (data && data.data && Array.isArray(data.data.diff) && data.data.diff.length > 0) {
              return new Response(JSON.stringify(data), {
                status: 200,
                headers: {
                  'Content-Type': 'application/json',
                  'Cache-Control': 'public, max-age=12, s-maxage=15',
                  ...CORS_HEADERS,
                },
              });
            }
          }
        } catch (_) {}
      }

      // Upstream failed or empty: use ultra-fast resilient fallback
      try {
        const fallbackData = await getSectorConstituentsFallback(clean, pn, pz);
        if (fallbackData && fallbackData.data && fallbackData.data.diff.length > 0) {
          return new Response(JSON.stringify(fallbackData), {
            status: 200,
            headers: {
              'Content-Type': 'application/json',
              'Cache-Control': 'public, max-age=10, s-maxage=12',
              ...CORS_HEADERS,
            },
          });
        }
      } catch (err) {
        return jsonRes({ error: 'fallback failed: ' + String(err) }, 502);
      }

      return jsonRes({ error: 'all upstream nodes and fallback failed' }, 502);
    }

    // Proxy: East Money sector index quote
    if (path === '/api/sector/quote') {
      const bk = url.searchParams.get('bk');
      if (!bk) return jsonRes({ error: 'Query parameter "bk" is required' }, 400);
      const clean = bk.trim().toUpperCase();
      const quoteCandidates = [
        `https://29.push2.eastmoney.com/api/qt/ulist.np/get?secids=90.${clean}&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f20,f21,f113,f114,f115,f116&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2`,
        `https://79.push2.eastmoney.com/api/qt/ulist.np/get?secids=90.${clean}&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f20,f21,f113,f114,f115,f116&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2`
      ];
      for (const targetUrl of quoteCandidates) {
        try {
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), 1500);
          const res = await fetch(targetUrl, {
            headers: {
              'User-Agent': USER_AGENT,
              'Referer': 'https://quote.eastmoney.com/',
              'Accept': '*/*',
            },
            signal: controller.signal
          });
          clearTimeout(timeout);
          if (res.ok) {
            const data = await res.json();
            if (data && data.data && Array.isArray(data.data.diff) && data.data.diff.length > 0) {
              return new Response(JSON.stringify(data), {
                status: 200,
                headers: {
                  'Content-Type': 'application/json',
                  'Cache-Control': 'public, max-age=10, s-maxage=12',
                  ...CORS_HEADERS,
                },
              });
            }
          }
        } catch (_) {}
      }

      // Upstream quote failed: synthesize sector quote from constituents
      try {
        const fallbackQuote = await getSectorQuoteFallback(clean);
        return new Response(JSON.stringify(fallbackQuote), {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
            'Cache-Control': 'public, max-age=10, s-maxage=12',
            ...CORS_HEADERS,
          },
        });
      } catch (err) {
        return jsonRes({ error: 'upstream quote and fallback failed: ' + String(err) }, 502);
      }
    }

    return jsonRes({ error: 'Not Found' }, 404);
  },
};
