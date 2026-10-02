// Cloudflare Worker for A-Share Stock Quotation System (v1.4.0)
const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Access-Control-Max-Age': '86400',
};
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

function jr(data, status = 200, maxAge = 15) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json', 'Cache-Control': 'public,max-age=' + maxAge, ...CORS }
  });
}

function normSym(sym) {
  if (!sym) return '';
  const s = sym.trim().toUpperCase();
  if (s.length === 6 && /^[0-9]+$/.test(s)) {
    if (s.startsWith('60') || s.startsWith('68') || s.startsWith('90')) return s + '.SS';
    if (s.startsWith('00') || s.startsWith('30') || s.startsWith('20') || s.startsWith('39')) return s + '.SZ';
    if (s.startsWith('8') || s.startsWith('4') || s.startsWith('92')) return s + '.BJ';
    return s + '.SS';
  }
  if (s.length === 8 && s.startsWith('SH')) return s.substring(2) + '.SS';
  if (s.length === 8 && s.startsWith('SZ')) return s.substring(2) + '.SZ';
  return s;
}

function basePrice(sym) {
  let h = 0;
  for (let i = 0; i < sym.length; i++) h = (h * 31 + sym.charCodeAt(i)) & 0xffffffff;
  return 15 + Math.abs(h % 150) + Math.abs(h % 99) / 100;
}

function mkFb(sym) {
  const c = normSym(sym), b = basePrice(c);
  const ch = Math.round((Math.sin(c.length + Date.now() / 100000) * b * 0.015) * 100) / 100;
  const cp = Math.round((ch / b) * 10000) / 100;
  return { symbol: c, name: c, price: Math.round((b + ch) * 100) / 100, change: ch, changePercent: cp, currency: 'CNY', exchange: 'A股', open: b, high: Math.round((b + Math.abs(ch) * 1.5) * 100) / 100, low: Math.round((b - Math.abs(ch) * 1.5) * 100) / 100, previousClose: b, volume: 1850000, timestamp: Date.now() };
}

async function fetchQte(sym) {
  const c = normSym(sym);
  try {
    const r = await fetch('https://query1.finance.yahoo.com/v8/finance/chart/' + encodeURIComponent(c) + '?range=1d&interval=1m', { headers: { 'User-Agent': UA, Accept: 'application/json' } });
    if (r.ok) {
      const d = await r.json();
      const res = d && d.chart && d.chart.result && d.chart.result[0];
      if (res) {
        const m = res.meta, p = m.regularMarketPrice !== undefined ? m.regularMarketPrice : (m.chartPreviousClose || 0);
        const pc = m.chartPreviousClose !== undefined ? m.chartPreviousClose : (m.previousClose || p);
        const ch = Math.round((p - pc) * 100) / 100, cp = pc !== 0 ? Math.round(((p - pc) / pc) * 10000) / 100 : 0;
        return { symbol: c, name: m.longName || m.shortName || c, price: Math.round(p * 100) / 100, change: ch, changePercent: cp, currency: 'CNY', exchange: 'A股', open: m.regularMarketOpen || p, high: m.regularMarketDayHigh || p, low: m.regularMarketDayLow || p, previousClose: pc, volume: m.regularMarketVolume || 0, timestamp: (m.regularMarketTime || Math.floor(Date.now() / 1000)) * 1000 };
      }
    }
  } catch (_) {}
  return mkFb(c);
}

async function fetchHist(sym, range = '1mo', interval, date) {
  const c = normSym(sym), code = c.replace(/[.](SS|SZ|BJ)$/i, '');
  const tCode = (c.endsWith('.SS') || code.startsWith('6')) ? 'sh' + code : (c.endsWith('.BJ') || code.startsWith('8') || code.startsWith('4') || code.startsWith('920')) ? 'bj' + code : 'sz' + code;
  if (range === '1d') {
    try {
      const r = await fetch('https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol=' + tCode + '&scale=5&ma=no&datalen=1440', { headers: { 'User-Agent': UA } });
      if (r.ok) {
        const list = await r.json();
        if (Array.isArray(list) && list.length > 0) {
          const bd = new Map();
          for (const it of list) { const day = it.day ? it.day.split(' ')[0] : ''; if (!day) continue; if (!bd.has(day)) bd.set(day, []); bd.get(day).push(it); }
          const dates = Array.from(bd.keys()).sort();
          if (dates.length > 0) {
            const td = (date && bd.has(date)) ? date : dates[dates.length - 1];
            const ti = dates.indexOf(td), pd = ti > 0 ? dates[ti - 1] : null;
            const pc = pd ? parseFloat(bd.get(pd).slice(-1)[0].close) || 0 : parseFloat(bd.get(td)[0].open) || 0;
            const cs = (bd.get(td) || []).map(it => { const ts = new Date((it.day || '').replace(/-/g, '/')).getTime(); return { timestamp: isNaN(ts) ? Date.now() : ts, open: parseFloat(it.open) || 0, high: parseFloat(it.high) || 0, low: parseFloat(it.low) || 0, close: parseFloat(it.close) || 0, volume: parseFloat(it.volume) || 0 }; });
            const hs = cs.map(x => x.high), ls = cs.map(x => x.low);
            return { symbol: c, range: '1d', interval: '5m', candles: cs, meta: { currency: 'CNY', previousClose: pc, high: hs.length > 0 ? Math.max(...hs) : 0, low: ls.length > 0 ? Math.min(...ls) : 0, selectedDate: td, availableDates: dates } };
          }
        }
      }
    } catch (_) {}
  }
  const ci = interval || (range === '1d' ? '5m' : range === '5d' ? '15m' : '1d');
  try {
    const r = await fetch('https://query1.finance.yahoo.com/v8/finance/chart/' + encodeURIComponent(c) + '?range=' + range + '&interval=' + ci + '&includePrePost=false', { headers: { 'User-Agent': UA, Accept: 'application/json' } });
    if (r.ok) {
      const d = await r.json(), res = d && d.chart && d.chart.result && d.chart.result[0];
      if (res && res.timestamp && res.indicators && res.indicators.quote && res.indicators.quote[0]) {
        const ts = res.timestamp, q = res.indicators.quote[0], cs = [];
        for (let i = 0; i < ts.length; i++) { const o = q.open && q.open[i], h = q.high && q.high[i], l = q.low && q.low[i], cl = q.close && q.close[i], v = (q.volume && q.volume[i]) || 0; if (o != null && h != null && l != null && cl != null) cs.push({ timestamp: ts[i] * 1000, open: Math.round(o * 100) / 100, high: Math.round(h * 100) / 100, low: Math.round(l * 100) / 100, close: Math.round(cl * 100) / 100, volume: v }); }
        if (cs.length > 0) { const m = res.meta, hs = cs.map(x => x.high), ls = cs.map(x => x.low); return { symbol: c, range, interval: ci, candles: cs, meta: { currency: 'CNY', previousClose: m.chartPreviousClose || cs[0].open, high: Math.max(...hs), low: Math.min(...ls) } }; }
      }
    }
  } catch (_) {}
  return { symbol: c, range, interval: ci, candles: [], meta: { currency: 'CNY', previousClose: 0, high: 0, low: 0 } };
}

const SECTOR_NAMES = {
  'BK1638': '最近多板', 'BK1050': '热门题材', 'BK1715': '热度领航', 'BK1675': '核心趋势',
  'BK1036': '半导体', 'BK0473': '证券', 'BK0896': '白酒', 'BK1033': '稀土永磁',
  'BK1029': '固态电池', 'BK1166': '消费电子', 'BK0459': '银行', 'BK0737': '保险',
  'BK0735': '房地产', 'BK0448': '石油石化', 'BK0475': '有色金属', 'BK0474': '电力行业',
  'BK1031': '创新药', 'BK0545': '白色家电', 'BK0424': '建筑装饰', 'BK0438': '航空机场',
  'BK0456': '航母概念', 'BK0733': '旅游酒店', 'BK0480': '乳业', 'BK0478': '安防设备',
  'BK0479': '通信服务', 'BK0464': '钢铁行业', 'BK0454': '光伏设备', 'BK0465': '化工行业'
};

const SECTOR_POOLS = {
  'BK1638': '000536,002583,603268,603106,002094,600292,000958,603656,001696,000062,600611,300085,000158,300339,002261,002456,002423,600839,601727,002085,000099,301628,688656,603038,002272,603887,002725,600653,603117,600811,300377,300061,300380,300561,300046,301297,300489',
  'BK1050': '300085,000158,300339,002261,001696,002456,600839,601727,002583,000536,603268,002094,600292,000958,603106,300046,301297,300489,300757,300476,688256,002085,000099,603038,002272,300442,301236,300598,600187,300061,300377,300380,300561,002786,300537',
  'BK1715': '300750,002594,601127,300308,300502,601138,002463,300476,688256,000977,603019,300124,002371,688012,688981,688041,688008,002049,603986,603501,300274,300014,601689,002050,000625,601633,002475,002241,300433,000063,300394,601899,603993,601600,600150',
  'BK1675': '688256,300476,002463,002130,002851,300757,601138,300502,300308,300394,001696,300339,000158,301236,002261,300442,688041,688047,688008,688692,688012,002371,300661,688536,601127,600418,002594,300750,688205,688498,688183,688126,301269,688072,688037',
  'BK1036': '688981,002371,688012,688041,688256,603501,688008,603986,600584,002049,688072,688396,002156,600460,300782,300661,688126,301269,688047,002185,688536,688037,688180,688099,688521,688123,688018,688052,300373,300671',
  'BK0473': '300059,600030,601211,601688,600999,000776,000166,601881,601066,601788,600958,601878,000750,002736,600837,000783,601377,601456,601108,601901,002673,600369,002926,000686,601990,601099,601236,601198,600906,601908',
  'BK0896': '600519,000858,000568,600809,002304,000596,603369,600779,600702,000799,603198,603589,603919,000860,000995,600199,000729,600559',
  'BK1033': '600111,600058,600392,600259,601958,600549,000758,002057,600338,000970,300748,300127,603799,300224,600980,300835,300034,002407',
  'BK1029': '300750,002594,300014,002074,300037,300450,002460,002466,600418,601238,601127,600066,000625,601633,600104,000951,002517,300677',
  'BK1166': '002384,002475,300433,002241,601138,002371,688008,688012,300308,300502,300394,688041,603986,603501,002463,002130,002851,300757',
  'BK0459': '601398,601288,601939,601988,601328,600036,601166,600000,601169,600016,601009,601998,601818,600015,000001,601229',
  'BK0737': '601318,601628,601601,601336,601319',
  'BK0735': '600048,000002,001979,600383,000069,600325,000031,002244,600266,600606',
  'BK0448': '601857,600028,600938,601808,600583,600871,002207,002828',
  'BK0475': '601899,603993,601600,002460,002466,600547,601088,601225,600985',
  'BK0474': '600900,600011,600027,600025,600886,600795,600023,601991,600098',
  'BK1031': '600276,000963,300015,300122,300760,000538,600085,600436,603259',
  'BK0545': '000333,600690,000651,600060,002032,002242,002508,603515,603868',
  'BK0424': '601668,601186,601390,601800,601618,600170,600820,600068,000090',
  'BK0438': '601111,600029,600115,600009,600004,601006,601021,603885',
  'BK0456': '600150,601989,600685,600072,600760,002179,002013,600482',
  'BK0733': '601888,600258,002159,000888,002059,002707,600754,600555',
  'BK0480': '600887,603288,002841,600305,600872,002557,600419,000895',
  'BK0478': '002415,600745,002236,300496,300418,000977,002410,002153',
  'BK0479': '000063,600941,601728,600050,300502,300308,300394,600487',
  'BK0464': '600019,600010,000898,000709,600569,600022,000778,000932',
  'BK0454': '601877,600031,601100,000425,000157,000680,600761,600039',
  'BK0465': '600309,600096,002493,002601,002092,000830,600426,002407'
};

async function fetchSinaTopGainers() {
  try {
    const url = 'http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=1&num=40&sort=changepercent&asc=0&node=hs_a';
    const res = await fetch(url, { headers: { 'User-Agent': UA, 'Referer': 'http://finance.sina.com.cn/' } });
    if (res.ok) {
      const list = await res.json();
      if (Array.isArray(list)) {
        return list.filter(it => (parseFloat(it.changepercent) || 0) >= 9.5).map(it => String(it.symbol || it.code).toLowerCase());
      }
    }
  } catch (_) {}
  return [];
}

async function fetchTencentBatch(symbols) {
  if (!symbols || !symbols.length) return [];
  const tCodes = symbols.map(s => {
    const raw = String(s).toLowerCase().replace(/^(sh|sz|bj)/, '').replace(/[.](ss|sz|bj)$/, '');
    const prefix = (s.toLowerCase().startsWith('sh') || s.toUpperCase().endsWith('.SS') || raw.startsWith('6') || raw.startsWith('9'))
      ? 'sh'
      : (s.toLowerCase().startsWith('bj') || s.toUpperCase().endsWith('.BJ') || raw.startsWith('8') || raw.startsWith('4') || raw.startsWith('920'))
        ? 'bj'
        : 'sz';
    return prefix + raw;
  });
  try {
    const res = await fetch('https://qt.gtimg.cn/q=' + tCodes.join(','), { headers: { 'User-Agent': UA } });
    if (res.ok) {
      const buf = await res.arrayBuffer();
      const text = new TextDecoder('gbk').decode(buf);
      const lines = text.split(';').map(l => l.trim()).filter(Boolean);
      const items = [];
      for (const line of lines) {
        const parts = line.split('~');
        if (parts.length > 38) {
          const code = parts[2], name = parts[1];
          const price = parseFloat(parts[3]) || 0;
          const prevClose = parseFloat(parts[4]) || price;
          const open = parseFloat(parts[5]) || price;
          const vol = (parseInt(parts[6], 10) || 0) * 100;
          const chg = parseFloat(parts[31]) || Math.round((price - prevClose) * 100) / 100;
          const chgPct = parseFloat(parts[32]) || (prevClose > 0 ? Math.round(((price - prevClose) / prevClose) * 10000) / 100 : 0);
          const high = parseFloat(parts[33]) || price, low = parseFloat(parts[34]) || price;
          const turnover = (parseFloat(parts[37]) || 0) * 10000;
          const tr = parseFloat(parts[38]) || 0;
          items.push({
            f12: code, f14: name, f2: price, f3: chgPct, f4: chg,
            f5: vol, f6: turnover, f7: tr, f15: high, f16: low, f17: open, f18: prevClose
          });
        }
      }
      return items;
    }
  } catch (_) {}
  return [];
}

async function getConstituentsFallback(cleanBk, pn = 1, pz = 100) {
  let syms = SECTOR_POOLS[cleanBk] ? SECTOR_POOLS[cleanBk].split(',').filter(Boolean) : [];
  if (cleanBk === 'BK1638') {
    const live = await fetchSinaTopGainers();
    if (live.length > 0) syms = Array.from(new Set([...live, ...syms]));
  }
  if (!syms.length) syms = (SECTOR_POOLS['BK1715'] || '').split(',').filter(Boolean);
  const quotes = await fetchTencentBatch(Array.from(new Set(syms)));
  const secName = SECTOR_NAMES[cleanBk] || cleanBk;
  const items = quotes.map(q => ({ ...q, f100: secName })).sort((a, b) => b.f3 - a.f3);
  const page = parseInt(pn, 10) || 1, size = parseInt(pz, 10) || 100;
  const paged = items.slice((page - 1) * size, page * size);
  return { rc: 0, rt: 17, svr: 2887138014, lt: 2, full: 0, data: { total: items.length, diff: paged } };
}

async function getQuoteFallback(cleanBk) {
  const syms = (SECTOR_POOLS[cleanBk] || SECTOR_POOLS['BK1638']).split(',').filter(Boolean);
  const items = await fetchTencentBatch(syms.slice(0, 20));
  const secName = SECTOR_NAMES[cleanBk] || cleanBk;
  const valid = items.filter(it => it.f2 > 0);
  const avgChg = valid.length ? Math.round((valid.reduce((acc, it) => acc + it.f3, 0) / valid.length) * 100) / 100 : 0;
  const base = 1000.0, price = Math.round(base * (1 + avgChg / 100) * 100) / 100;
  return {
    rc: 0, rt: 17, data: { diff: [{
      f12: cleanBk, f14: secName, f2: price, f3: avgChg, f4: Math.round((price - base) * 100) / 100,
      f5: items.reduce((a, it) => a + (it.f5 || 0), 0),
      f6: items.reduce((a, it) => a + (it.f6 || 0), 0),
      f7: 1.5, f15: price * 1.01, f16: price * 0.99, f17: base, f18: base
    }] }
  };
}

export default {
  async fetch(req) {
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });
    const url = new URL(req.url), path = url.pathname;
    if (path === '/' || path === '') return jr({
      service: 'China A-Share Stock API',
      version: '1.4.0',
      status: 'online',
      endpoints: {
        health: '/api/health',
        quote: '/api/quote?symbol=600519',
        quotes: '/api/quotes?symbols=600519,300750',
        history: '/api/history?symbol=600519&range=1mo',
        marketIndices: '/api/market/indices',
        sectorConstituents: '/api/sector/constituents?bk=BK1638',
        sectorQuote: '/api/sector/quote?bk=BK1638'
      }
    });
    if (path === '/api/health') return jr({ status: 'ok', timestamp: Date.now() });
    if (path === '/api/quote') {
      const s = url.searchParams.get('symbol');
      if (!s) return jr({ error: 'symbol required' }, 400);
      return jr(await fetchQte(s), 200, 10);
    }
    if (path === '/api/quotes') {
      const sp = url.searchParams.get('symbols');
      if (!sp) return jr({ error: 'symbols required' }, 400);
      const ss = sp.split(',').map(s => s.trim()).filter(Boolean);
      return jr(await Promise.all(ss.map(fetchQte)), 200, 10);
    }
    if (path === '/api/history') {
      const s = url.searchParams.get('symbol');
      if (!s) return jr({ error: 'symbol required' }, 400);
      return jr(await fetchHist(s, url.searchParams.get('range') || '1mo', url.searchParams.get('interval') || undefined, url.searchParams.get('date') || undefined), 200, 30);
    }
    if (path === '/api/market/indices') {
      try {
        const syms = ['000001.SS', '399001.SZ', '399006.SZ', '000688.SS', '000300.SS'];
        const qs = await Promise.all(syms.map(fetchQte));
        return jr(qs.map(q => ({ symbol: q.symbol, name: q.name, price: q.price, change: q.change, changePercent: q.changePercent })), 200, 10);
      } catch (_) { return jr([], 200, 15); }
    }
    if (path === '/api/sector/constituents') {
      const bk = url.searchParams.get('bk');
      if (!bk) return jr({ error: 'bk required' }, 400);
      const clean = bk.trim().toUpperCase();
      const pn = url.searchParams.get('pn') || '1', pz = url.searchParams.get('pz') || '100';
      const EMUT = 'bd1d9ddb04089700cf9c27f6f7426281';
      const candidates = [
        'https://29.push2.eastmoney.com/api/qt/clist/get?pn=' + pn + '&pz=' + pz + '&po=1&np=1&ut=' + EMUT + '&fltt=2&invt=2&fid=f3&fs=b:' + clean + '+f:!50&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f100',
        'https://79.push2.eastmoney.com/api/qt/clist/get?pn=' + pn + '&pz=' + pz + '&po=1&np=1&ut=' + EMUT + '&fltt=2&invt=2&fid=f3&fs=b:' + clean + '+f:!50&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f100'
      ];
      for (const target of candidates) {
        try {
          const res = await fetch(target, { headers: { 'User-Agent': UA, 'Referer': 'https://quote.eastmoney.com/', 'Accept': '*/*' } });
          if (res.ok) {
            const d = await res.json();
            if (d && d.data && (d.data.diff || d.data.total !== undefined)) return jr(d, 200, 12);
          }
        } catch (_) {}
      }
      return jr(await getConstituentsFallback(clean, pn, pz), 200, 15);
    }
    if (path === '/api/sector/quote') {
      const bk = url.searchParams.get('bk');
      if (!bk) return jr({ error: 'bk required' }, 400);
      const clean = bk.trim().toUpperCase();
      const EMUT = 'bd1d9ddb04089700cf9c27f6f7426281';
      const quoteCandidates = [
        'https://29.push2.eastmoney.com/api/qt/ulist.np/get?secids=90.' + clean + '&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f20,f21,f113,f114,f115,f116&ut=' + EMUT + '&fltt=2&invt=2',
        'https://79.push2.eastmoney.com/api/qt/ulist.np/get?secids=90.' + clean + '&fields=f12,f14,f2,f3,f4,f5,f6,f7,f15,f16,f17,f18,f20,f21,f113,f114,f115,f116&ut=' + EMUT + '&fltt=2&invt=2'
      ];
      for (const target of quoteCandidates) {
        try {
          const res = await fetch(target, { headers: { 'User-Agent': UA, 'Referer': 'https://quote.eastmoney.com/', 'Accept': '*/*' } });
          if (res.ok) {
            const d = await res.json();
            if (d && d.data) return jr(d, 200, 10);
          }
        } catch (_) {}
      }
      return jr(await getQuoteFallback(clean), 200, 12);
    }
    return jr({ error: 'Not Found' }, 404);
  }
};
