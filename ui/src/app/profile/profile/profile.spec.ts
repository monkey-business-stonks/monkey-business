import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Profile } from './profile';

describe('Profile Component', () => {
  let component: Profile;
  let fixture: ComponentFixture<Profile>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profile],
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // TODO: Add more tests after layout is finalized
  // it('should save profile', () => {
  //   component.username = 'testuser';
  //   component.firstName = 'John';
  //   component.lastName = 'Doe';
  //   component.email = 'john@example.com';
  //   component.phoneNumber = '555-1234';
  //   const consoleSpy = spyOn(console, 'log');
  //   component.onSave();
  //   expect(consoleSpy).toHaveBeenCalledWith('Profile saved', {
  //     username: 'testuser',
  //     firstName: 'John',
  //     lastName: 'Doe',
  //     email: 'john@example.com',
  //     phoneNumber: '555-1234',
  //   });
  // });

  // it('should exit edit mode after saving', () => {
  //   component.isEditMode = true;
  //   component.onSave();
  //   expect(component.isEditMode).toBe(false);
  // });

  // it('should toggle edit mode', () => {
  //   component.isEditMode = false;
  //   component.toggleEditMode();
  //   expect(component.isEditMode).toBe(true);
  //   component.toggleEditMode();
  //   expect(component.isEditMode).toBe(false);
  // });
});

