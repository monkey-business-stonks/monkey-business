import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccountsPanel } from './accounts-panel';

describe('AccountsPanel', () => {
  let component: AccountsPanel;
  let fixture: ComponentFixture<AccountsPanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsPanel],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsPanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
