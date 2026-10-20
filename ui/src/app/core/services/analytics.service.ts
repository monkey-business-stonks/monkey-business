import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalystOverviewResponse, OperationsOverviewResponse } from '@/app/core/models';

export interface AnalystOverviewQuery {
  period_from?: string;
  period_to?: string;
  compare_from?: string;
  compare_to?: string;
  instrument?: string;
  account_type?: string;
}

export interface OperationsOverviewQuery {
  period_from?: string;
  period_to?: string;
  instrument?: string;
  account_type?: string;
  search?: string;
  limit?: number;
  offset?: number;
}

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  constructor(private http: HttpClient) {}

  getAnalystOverview(query: AnalystOverviewQuery = {}): Observable<AnalystOverviewResponse> {
    return this.http.get<AnalystOverviewResponse>(`${window.location.origin}/analytics-api/v1/analyst/overview`, {
      params: this.buildParams(query),
    });
  }

  getOperationsOverview(query: OperationsOverviewQuery = {}): Observable<OperationsOverviewResponse> {
    return this.http.get<OperationsOverviewResponse>(`${window.location.origin}/analytics-api/v1/operations/overview`, {
      params: this.buildParams(query),
    });
  }

  private buildParams<T extends object>(query: T): HttpParams {
    let params = new HttpParams();
    Object.entries(query as Record<string, string | number | undefined>).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}