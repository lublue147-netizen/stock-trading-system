import { StockQuote, HistoricalData, MarketIndex, SearchResult, CandlePoint } from '../types';

export const MOCK_INDICES: MarketIndex[] = [
  { symbol: "^GSPC", name: "S&P 500", price: 5864.67, change: 24.34, changePercent: 0.42 },
  { symbol: "^IXIC", name: "NASDAQ", price: 18415.21, change: 115.80, changePercent: 0.63 },
  { symbol: "^DJI", name: "Dow Jones", price: 42863.86, change: -45.12, changePercent: -0.11 },
  { symbol: "000001.SS", name: "上证指数", price: 3326.46, change: 38.20, changePercent: 1.16 },
  { symbol: "399001.SZ", name: "深证成指", price: 10611.72, change: 184.60, changePercent: 1.77 },
  { symbol: "^HSI", name: "恒生指数", price: 20638.70, change: 254.30, changePercent: 1.25 }
];

export const MOCK_STOCKS: Record<string, { quote: StockQuote; basePrice: number }> = {
  "AAPL": {
    quote: {
      symbol: "AAPL",
      name: "Apple Inc.",
      price: 231.41,
      change: 1.85,
      changePercent: 0.81,
      currency: "USD",
      exchange: "NASDAQ",
      open: 230.10,
      high: 232.50,
      low: 229.80,
      previousClose: 229.56,
      volume: 48512300,
      marketCap: 3520000000000,
      peRatio: 34.2,
      fiftyTwoWeekHigh: 237.23,
      fiftyTwoWeekLow: 164.08,
      timestamp: Date.now()
    },
    basePrice: 230.0
  },
  "TSLA": {
    quote: {
      symbol: "TSLA",
      name: "Tesla, Inc.",
      price: 260.48,
      change: 8.75,
      changePercent: 3.48,
      currency: "USD",
      exchange: "NASDAQ",
      open: 253.20,
      high: 262.00,
      low: 251.50,
      previousClose: 251.73,
      volume: 82451000,
      marketCap: 830000000000,
      peRatio: 72.5,
      fiftyTwoWeekHigh: 271.00,
      fiftyTwoWeekLow: 138.80,
      timestamp: Date.now()
    },
    basePrice: 255.0
  },
  "NVDA": {
    quote: {
      symbol: "NVDA",
      name: "NVIDIA Corporation",
      price: 138.25,
      change: 3.12,
      changePercent: 2.31,
      currency: "USD",
      exchange: "NASDAQ",
      open: 135.50,
      high: 139.10,
      low: 135.00,
      previousClose: 135.13,
      volume: 120530000,
      marketCap: 3380000000000,
      peRatio: 64.8,
      fiftyTwoWeekHigh: 140.76,
      fiftyTwoWeekLow: 39.23,
      timestamp: Date.now()
    },
    basePrice: 135.0
  },
  "MSFT": {
    quote: {
      symbol: "MSFT",
      name: "Microsoft Corporation",
      price: 428.15,
      change: -1.25,
      changePercent: -0.29,
      currency: "USD",
      exchange: "NASDAQ",
      open: 429.80,
      high: 431.20,
      low: 426.50,
      previousClose: 429.40,
      volume: 21450000,
      marketCap: 3180000000000,
      peRatio: 36.1,
      fiftyTwoWeekHigh: 468.35,
      fiftyTwoWeekLow: 309.45,
      timestamp: Date.now()
    },
    basePrice: 428.0
  },
  "0700.HK": {
    quote: {
      symbol: "0700.HK",
      name: "腾讯控股 (Tencent Holdings)",
      price: 432.80,
      change: 7.20,
      changePercent: 1.69,
      currency: "HKD",
      exchange: "HKSE",
      open: 427.00,
      high: 435.60,
      low: 426.20,
      previousClose: 425.60,
      volume: 18450000,
      marketCap: 4050000000000,
      peRatio: 24.3,
      fiftyTwoWeekHigh: 482.00,
      fiftyTwoWeekLow: 260.20,
      timestamp: Date.now()
    },
    basePrice: 430.0
  },
  "600519.SS": {
    quote: {
      symbol: "600519.SS",
      name: "贵州茅台",
      price: 1560.00,
      change: 28.00,
      changePercent: 1.83,
      currency: "CNY",
      exchange: "SSE",
      open: 1540.00,
      high: 1572.00,
      low: 1538.00,
      previousClose: 1532.00,
      volume: 4210000,
      marketCap: 1960000000000,
      floatMarketCap: 1960000000000,
      peRatio: 26.2,
      pbRatio: 7.85,
      fiftyTwoWeekHigh: 1820.00,
      fiftyTwoWeekLow: 1245.83,
      timestamp: Date.now(),
      turnoverRate: 1.82,
      turnoverAmount: 6572000000,
      amplitude: 2.22,
      limitUpPrice: 1685.20,
      limitDownPrice: 1378.80,
      eps: 58.50,
      bps: 182.30,
      roe: 32.1,
      industry: "食品饮料 / 白酒",
      mainBusiness: "茅台酒及系列酒的生产与销售，国内高档白酒绝对龙头企业。",
      conceptTags: ["白酒龙头", "沪股通", "核心资产", "MSCI中国", "高股息", "大消费"]
    },
    basePrice: 1550.0
  },
  "300750.SZ": {
    quote: {
      symbol: "300750.SZ",
      name: "宁德时代",
      price: 248.50,
      change: 5.80,
      changePercent: 2.39,
      currency: "CNY",
      exchange: "SZSE",
      open: 243.00,
      high: 251.20,
      low: 242.10,
      previousClose: 242.70,
      volume: 24500000,
      marketCap: 1090000000000,
      floatMarketCap: 950000000000,
      peRatio: 25.2,
      pbRatio: 5.62,
      fiftyTwoWeekHigh: 285.00,
      fiftyTwoWeekLow: 140.50,
      timestamp: Date.now(),
      turnoverRate: 3.45,
      turnoverAmount: 6088000000,
      amplitude: 3.75,
      limitUpPrice: 291.24,
      limitDownPrice: 194.16,
      eps: 9.85,
      bps: 44.20,
      roe: 22.3,
      industry: "电力设备 / 动力电池",
      mainBusiness: "全球新能源汽车动力电池与储能系统研发制造龙头。",
      conceptTags: ["动力电池", "储能系统", "新能源车", "创业板权重", "深股通"]
    },
    basePrice: 245.0
  },
  "002594.SZ": {
    quote: {
      symbol: "002594.SZ",
      name: "比亚迪",
      price: 285.60,
      change: 7.20,
      changePercent: 2.59,
      currency: "CNY",
      exchange: "SZSE",
      open: 280.00,
      high: 288.50,
      low: 279.00,
      previousClose: 278.40,
      volume: 18200000,
      marketCap: 831000000000,
      floatMarketCap: 720000000000,
      peRatio: 27.6,
      pbRatio: 4.88,
      fiftyTwoWeekHigh: 316.00,
      fiftyTwoWeekLow: 162.80,
      timestamp: Date.now(),
      turnoverRate: 2.78,
      turnoverAmount: 5198000000,
      amplitude: 3.41,
      limitUpPrice: 306.24,
      limitDownPrice: 250.56,
      eps: 10.35,
      bps: 58.50,
      roe: 17.7,
      industry: "汽车整车 / 新能源车",
      mainBusiness: "新能源汽车及关键零部件、刀片电池研发与出海制造领军企业。",
      conceptTags: ["新能源车", "刀片电池", "深股通", "智能座舱", "出海龙头"]
    },
    basePrice: 282.0
  },
  "300059.SZ": {
    quote: {
      symbol: "300059.SZ",
      name: "东方财富",
      price: 22.80,
      change: 1.15,
      changePercent: 5.31,
      currency: "CNY",
      exchange: "SZSE",
      open: 21.80,
      high: 23.45,
      low: 21.60,
      previousClose: 21.65,
      volume: 85200000,
      marketCap: 361000000000,
      floatMarketCap: 305000000000,
      peRatio: 35.1,
      pbRatio: 3.25,
      fiftyTwoWeekHigh: 29.00,
      fiftyTwoWeekLow: 10.50,
      timestamp: Date.now(),
      turnoverRate: 6.85,
      turnoverAmount: 1942000000,
      amplitude: 8.55,
      limitUpPrice: 25.98,
      limitDownPrice: 17.32,
      eps: 0.65,
      bps: 7.02,
      roe: 9.25,
      industry: "非银金融 / 证券互联网",
      mainBusiness: "以东方财富网为核心的互联网金融服务平台，证券、公募基金代销龙头。",
      conceptTags: ["东方财富", "互金龙头", "券商概念", "创业板50", "深股通"]
    },
    basePrice: 22.0
  }
};

export function generateMockCandles(symbol: string, range: string, basePrice: number = 100): CandlePoint[] {
  let count = 30;
  let intervalMs = 24 * 3600 * 1000; // 1 day

  switch (range) {
    case "1d":
      count = 48; // 30-min / 5-min intervals
      intervalMs = 5 * 60 * 1000;
      break;
    case "5d":
      count = 40;
      intervalMs = 30 * 60 * 1000;
      break;
    case "1mo":
      count = 30;
      intervalMs = 24 * 3600 * 1000;
      break;
    case "6mo":
      count = 120;
      intervalMs = 24 * 3600 * 1000;
      break;
    case "1y":
      count = 250;
      intervalMs = 24 * 3600 * 1000;
      break;
    case "all":
    case "5y":
      count = 300;
      intervalMs = 7 * 24 * 3600 * 1000;
      break;
    default:
      count = 30;
  }

  const candles: CandlePoint[] = [];
  const now = Date.now();
  let currentClose = basePrice * 0.85;

  // Generate deterministic synthetic walk based on symbol seed
  let seed = 0;
  for (let i = 0; i < symbol.length; i++) {
    seed += symbol.charCodeAt(i);
  }

  for (let i = 0; i < count; i++) {
    const timestamp = now - (count - i) * intervalMs;
    const randomFactor = Math.sin(seed + i * 0.3) * 0.02 + ((i / count) * 0.15);
    const stepChange = (Math.sin(i * 0.8 + seed) * 0.015) + (randomFactor * 0.01);
    
    const open = Math.round(currentClose * 100) / 100;
    const close = Math.round(Math.max(1, open * (1 + stepChange)) * 100) / 100;
    const high = Math.round(Math.max(open, close) * (1 + Math.abs(Math.sin(i * 1.5)) * 0.012) * 100) / 100;
    const low = Math.round(Math.min(open, close) * (1 - Math.abs(Math.cos(i * 1.5)) * 0.012) * 100) / 100;
    const volume = Math.floor(1000000 + Math.abs(Math.sin(i)) * 5000000);

    candles.push({
      timestamp,
      open,
      high,
      low,
      close,
      volume
    });

    currentClose = close;
  }

  return candles;
}

export const SEARCH_DICTIONARY: SearchResult[] = [
  { symbol: "AAPL", name: "Apple Inc.", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "TSLA", name: "Tesla, Inc.", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "NVDA", name: "NVIDIA Corporation", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "MSFT", name: "Microsoft Corporation", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "AMZN", name: "Amazon.com, Inc.", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "GOOGL", name: "Alphabet Inc. (Google)", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "META", name: "Meta Platforms, Inc.", exchange: "NASDAQ", type: "EQUITY" },
  { symbol: "0700.HK", name: "腾讯控股 (Tencent Holdings)", exchange: "HKSE", type: "EQUITY" },
  { symbol: "9988.HK", name: "阿里巴巴 (Alibaba Group)", exchange: "HKSE", type: "EQUITY" },
  { symbol: "3690.HK", name: "美团 (Meituan)", exchange: "HKSE", type: "EQUITY" },
  { symbol: "600519.SS", name: "贵州茅台 (Kweichow Moutai)", exchange: "SSE", type: "EQUITY" },
  { symbol: "000858.SZ", name: "五粮液 (Wuliangye)", exchange: "SZSE", type: "EQUITY" },
  { symbol: "300750.SZ", name: "宁德时代 (CATL)", exchange: "SZSE", type: "EQUITY" },
  { symbol: "002594.SZ", name: "比亚迪 (BYD Company)", exchange: "SZSE", type: "EQUITY" },
  { symbol: "BABA", name: "Alibaba Group ADR", exchange: "NYSE", type: "EQUITY" }
];
