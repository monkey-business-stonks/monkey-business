import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SelectInput } from './select-input';

describe('SelectInput', () => {
  let component: SelectInput;
  let fixture: ComponentFixture<SelectInput>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectInput],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectInput);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
