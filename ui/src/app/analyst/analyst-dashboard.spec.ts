import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalystDashboard } from '@/app/analyst/analyst-dashboard';

describe('AnalystDashboard', () => {
  let component: AnalystDashboard;
  let fixture: ComponentFixture<AnalystDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalystDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalystDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
