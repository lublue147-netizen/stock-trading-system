export interface OrderBookEntry {
  level: string;
  price: number;
  volume: number;
  percent: number;
}

export interface StockQuote {
  symbol: string;
  name: string;
  price: number;
  change: number;
  changePercent: number;
  currency: string;
  exchange: string;
  open: number;
  high: number;
  low: number;
  previousClose: number;
  volume: number;
  marketCap?: number;
  peRatio?: number;
  fiftyTwoWeekHigh?: number;
  fiftyTwoWeekLow?: number;
  timestamp: number;

  // East Money fields
  turnoverRate?: number;
  turnoverAmount?: number;
  amplitude?: number;
  pbRatio?: number;
  floatMarketCap?: number;
  limitUpPrice?: number;
  limitDownPrice?: number;
  eps?: number;
  bps?: number;
  roe?: number;
  industry?: string;
  mainBusiness?: string;
  conceptTags?: string[];
  weibi?: number;
  weicha?: number;
  bids?: OrderBookEntry[];
  asks?: OrderBookEntry[];
}

export interface CandlePoint {
  timestamp: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface HistoricalData {
  symbol: string;
  range: string;
  interval: string;
  candles: CandlePoint[];
  meta: {
    currency: string;
    previousClose: number;
    high: number;
    low: number;
  };
}

export interface SearchResult {
  symbol: string;
  name: string;
  exchange: string;
  type: string;
}

export interface MarketIndex {
  symbol: string;
  name: string;
  price: number;
  change: number;
  changePercent: number;
}
