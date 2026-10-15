import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import {
  CandlestickData,
  CandlestickSeries,
  ColorType,
  IChartApi,
  createChart,
  UTCTimestamp,
} from 'lightweight-charts';
import { Subscription } from 'rxjs';
import { PricingService } from '@/app/core/services/pricing-service';

@Component({
  imports: [],
  selector: 'instrument-chart',
  styleUrl: './instrument-chart.css',
  templateUrl: './instrument-chart.html',
})
export class InstrumentChart implements AfterViewInit, OnChanges, OnDestroy {
  @Input() selectedTicker: any;

  @ViewChild('chartContainer', { static: true })
  private chartContainer!: ElementRef<HTMLDivElement>;

  loading = false;
  error = false;
  hasCandles = false;

  private chart?: IChartApi;
  private candleSeries?: ReturnType<IChartApi['addSeries']>;
  private candlesSubscription?: Subscription;
  private loadedTicker?: string;
  private resizeObserver?: ResizeObserver;

  constructor(private pricingService: PricingService) {}

  ngAfterViewInit(): void {
    this.createChart();
    this.loadCandles();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedTicker'] && !changes['selectedTicker'].firstChange) {
      this.loadCandles();
    }
  }

  ngOnDestroy(): void {
    this.candlesSubscription?.unsubscribe();
    this.resizeObserver?.disconnect();
    this.chart?.remove();
  }

  private createChart(): void {
    const container = this.chartContainer.nativeElement;
    const options = {
      width: container.clientWidth,
      height: container.clientHeight,
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: '#94a3b8',
      },
      grid: {
        vertLines: { color: 'rgba(148, 163, 184, 0.12)', visible: true, style: 0 },
        horzLines: { color: 'rgba(148, 163, 184, 0.12)', visible: true, style: 0 },
      },
      rightPriceScale: { borderVisible: false },
      timeScale: { borderVisible: false },
    };

    this.chart = createChart(container, options);
    this.candleSeries = this.chart.addSeries(CandlestickSeries, {
      upColor: '#22c55e',
      downColor: '#ef4444',
      borderUpColor: '#22c55e',
      borderDownColor: '#ef4444',
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
    });

    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(([entry]) => {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          this.chart?.resize(width, height);
        }
      });
      this.resizeObserver.observe(container);
    }
  }

  private loadCandles(): void {
    const ticker = this.selectedTicker?.data?.symbol;
    if (!this.candleSeries) {
      return;
    }

    if (!ticker) {
      this.loadedTicker = undefined;
      this.candlesSubscription?.unsubscribe();
      this.candleSeries.setData([]);
      this.loading = false;
      this.error = false;
      this.hasCandles = false;
      return;
    }

    if (ticker === this.loadedTicker) {
      return;
    }

    this.candlesSubscription?.unsubscribe();
    this.loadedTicker = ticker;
    this.loading = true;
    this.error = false;
    this.hasCandles = false;
    const to = this.formatDate(new Date());
    const fromDate = new Date();
    fromDate.setDate(fromDate.getDate() - 90);
    const from = this.formatDate(fromDate);

    this.candlesSubscription = this.pricingService.getCandles(ticker, from, to).subscribe({
      next: response => {
        const candles: CandlestickData[] = response.data.candles.map(candle => ({
          time: Math.floor(new Date(`${candle.date}T00:00:00Z`).getTime() / 1000) as UTCTimestamp,
          open: candle.open,
          high: candle.high,
          low: candle.low,
          close: candle.close,
        }));
        this.candleSeries?.setData(candles);
        this.chart?.timeScale().fitContent();
        this.hasCandles = candles.length > 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
        this.hasCandles = false;
        this.candleSeries?.setData([]);
      },
    });
  }

  private formatDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
