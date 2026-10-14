import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Quote } from '../models';

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
}
