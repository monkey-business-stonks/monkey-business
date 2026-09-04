import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SmallButton } from './small-button';

describe('SmallButton', () => {
  let component: SmallButton;
  let fixture: ComponentFixture<SmallButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SmallButton],
    }).compileComponents();

    fixture = TestBed.createComponent(SmallButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
