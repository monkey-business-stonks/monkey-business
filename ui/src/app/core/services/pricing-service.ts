import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Candle, Quote } from '../models';

@Injectable({
	providedIn: 'root'
})
export class PricingService {
	constructor(private http: HttpClient) {}

	getQuote(ticker: string): Observable<Quote> {
		return this.http.get<Quote>(`${window.location.origin}/api/market/price/${ticker}`);
	}

	getPrice(ticker: string): Observable<number> {
		return this.getQuote(ticker).pipe(map(quote => quote.data.price));
	}

	getCandles(ticker: string, from?: string, to?: string): Observable<Candle> {
		let params = new HttpParams();
		if (from) {
			params = params.set('from', from);
		}
		if (to) {
			params = params.set('to', to);
		}

		return this.http.get<Candle>(
			`${window.location.origin}/api/market/candles/${encodeURIComponent(ticker)}`,
			{ params }
		);
	}

	getPrices(symbols: string): Observable<Quote[]> {
		const params = new HttpParams().set('symbols', symbols);
		return this.http
			.get<Record<string, Quote['data']>>(`${window.location.origin}/api/market/quotes`, { params })
			.pipe(
				map(quotes => Object.entries(quotes).map(([symbol, data]) => ({
					data: { ...data, symbol },
				})))
			);
	}
}
