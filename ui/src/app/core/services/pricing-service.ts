import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Quote } from '../models';

@Injectable({
	providedIn: 'root'
})
export class PricingService {
	constructor(private http: HttpClient) {}

	getQuote(ticker: string): Quote {
		return this.http.get<Quote>(`${window.location.origin}/api/market/quote/${ticker}`);
	}
}
