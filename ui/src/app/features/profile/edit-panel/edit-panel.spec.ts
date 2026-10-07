import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditPanel } from './edit-panel';

describe('EditPanel', () => {
  let component: EditPanel;
  let fixture: ComponentFixture<EditPanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditPanel],
    }).compileComponents();

    fixture = TestBed.createComponent(EditPanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
