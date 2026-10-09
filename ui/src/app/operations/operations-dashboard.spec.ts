import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OperationsDashboard } from '@/app/operations/operations-dashboard';

describe('OperationsDashboard', () => {
  let component: OperationsDashboard;
  let fixture: ComponentFixture<OperationsDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OperationsDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(OperationsDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
