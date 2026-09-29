import { StockQuote, HistoricalData, MarketIndex, SearchResult, CandlePoint } from '../types';

export const MOCK_INDICES: MarketIndex[] = [
  { symbol: "000001.SS", name: "上证指数", price: 3326.46, change: 38.20, changePercent: 1.16 },
  { symbol: "399001.SZ", name: "深证成指", price: 10611.72, change: 184.60, changePercent: 1.77 },
  { symbol: "399006.SZ", name: "创业板指", price: 2215.80, change: 48.50, changePercent: 2.24 },
  { symbol: "000688.SS", name: "科创50", price: 985.40, change: 21.60, changePercent: 2.24 },
  { symbol: "000300.SS", name: "沪深300", price: 3950.25, change: 45.10, changePercent: 1.15 },
  { symbol: "899050.BJ", name: "北证50", price: 1432.18, change: 28.52, changePercent: 2.03 }
];

export const MOCK_STOCKS: Record<string, { quote: StockQuote; basePrice: number }> = {
  "002579.SZ": {
    quote: {
      symbol: "002579.SZ",
      name: "中京电子",
      price: 17.90,
      change: 0.60,
      changePercent: 3.47,
      currency: "CNY",
      exchange: "SZSE",
      open: 17.90,
      high: 18.30,
      low: 16.68,
      previousClose: 17.30,
      volume: 102635300,
      marketCap: 10960000000,
      floatMarketCap: 10440000000,
      peRatio: 26.2,
      pbRatio: 3.5,
      fiftyTwoWeekHigh: 23.40,
      fiftyTwoWeekLow: 9.98,
      timestamp: Date.now(),
      turnoverRate: 3.0,
      turnoverAmount: 1823380000,
      amplitude: 9.36,
      limitUpPrice: 19.03,
      limitDownPrice: 15.57,
      eps: 0.72,
      bps: 4.47,
      roe: 15.6,
      industry: "电子元器件 / 印制电路板(PCB)",
      mainBusiness: "专注于高密度印制电路板(PCB)与柔性印制电路板(FPC)研发生产，广泛应用于汽车电子、AI光模块与智能终端。",
      conceptTags: ["中京电子", "PCB概念", "汽车电子", "消费电子", "深股通", "折叠屏"]
    },
    basePrice: 17.5
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
  { symbol: "002579.SZ", name: "中京电子", exchange: "深交所", type: "A股" },
  { symbol: "600519.SS", name: "贵州茅台", exchange: "上交所", type: "A股" },
  { symbol: "300750.SZ", name: "宁德时代", exchange: "深交所", type: "A股" },
  { symbol: "002594.SZ", name: "比亚迪", exchange: "深交所", type: "A股" },
  { symbol: "300059.SZ", name: "东方财富", exchange: "深交所", type: "A股" },
  { symbol: "688981.SS", name: "中芯国际", exchange: "上交所", type: "科创板" },
  { symbol: "601318.SS", name: "中国平安", exchange: "上交所", type: "A股" },
  { symbol: "600036.SS", name: "招商银行", exchange: "上交所", type: "A股" },
  { symbol: "000858.SZ", name: "五粮液", exchange: "深交所", type: "A股" },
  { symbol: "000001.SZ", name: "平安银行", exchange: "深交所", type: "A股" },
  { symbol: "000001.SS", name: "上证指数", exchange: "上交所", type: "指数" },
  { symbol: "399001.SZ", name: "深证成指", exchange: "深交所", type: "指数" },
  { symbol: "399006.SZ", name: "创业板指", exchange: "深交所", type: "指数" },
  { symbol: "000688.SS", name: "科创50", exchange: "上交所", type: "指数" },
  { symbol: "000300.SS", name: "沪深300", exchange: "上交所", type: "指数" },
  { symbol: "899050.BJ", name: "北证50", exchange: "北交所", type: "指数" },
  { symbol: "301121.SZ", name: "紫建电子", exchange: "深交所", type: "A股" }
];
