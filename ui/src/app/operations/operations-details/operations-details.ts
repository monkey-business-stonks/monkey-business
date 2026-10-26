import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BannerMessageComponent } from '@/app/shared/components/banner-message/banner-message';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { AnalyticsService } from '@/app/core/services/analytics.service';
import { OperationsOrderDetailResponse } from '@/app/core/models';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-operations-details',
  standalone: true,
  imports: [
    CommonModule, 
    BannerMessageComponent,
    ButtonComponent,
    RouterLink
  ],
  templateUrl: './operations-details.html',
  styleUrls: [
    '../operations-dashboard.css',
    './operations-details.css'
  ]
})
export class OperationsDetailsComponent implements OnInit {
  detail: OperationsOrderDetailResponse | null = null;
  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private analyticsService: AnalyticsService,
    private changeDetector: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('orderId');
    if (!orderId) {
      this.loading = false;
      this.error = 'No order was selected.';
      this.changeDetector.detectChanges();
      return;
    }

    this.analyticsService.getOperationsOrderDetail(orderId)
      .pipe(finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      }))
      .subscribe({
        next: (detail) => {
          this.detail = detail;
          this.changeDetector.detectChanges();
        },
        error: (err) => {
          this.error = err?.error?.detail || err?.error?.message || 'Unable to load order details.';
          this.changeDetector.detectChanges();
        },
      });
  }

  formatMoney(value: number | null): string {
    return value === null ? 'n/a' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }

  formatNumber(value: number | null): string {
    return value === null ? 'n/a' : new Intl.NumberFormat('en-US', { maximumFractionDigits: 6 }).format(value);
  }

  formatUtc(value: string | null): string {
    if (!value) return 'n/a';
    return new Date(value).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'medium', timeZone: 'UTC' }) + ' UTC';
  }
}