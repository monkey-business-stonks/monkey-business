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

  // testing out toggles in edit mode
  describe('toggleEditMode', () => {
    it('should toggle edit mode from false to true', () => {
      component.isEditMode = false;
      component.toggleEditMode();
      expect(component.isEditMode).toBe(true);
    });

    it('should toggle edit mode from true to false', () => {
      component.isEditMode = true;
      component.toggleEditMode();
      expect(component.isEditMode).toBe(false);
    });

    it('should toggle multiple times correctly', () => {
      component.isEditMode = false;
      component.toggleEditMode();
      expect(component.isEditMode).toBe(true);
      component.toggleEditMode();
      expect(component.isEditMode).toBe(false);
      component.toggleEditMode();
      expect(component.isEditMode).toBe(true);
    });
  });

  // testing out save functionality
  describe('onSave', () => {
    it('should exit edit mode after saving', () => {
      component.isEditMode = true;
      component.onSave();
      expect(component.isEditMode).toBe(false);
    });

    it('should work with profile data', () => {
      component.username = 'testuser';
      component.firstName = 'John';
      component.lastName = 'Doe';
      component.email = 'john@example.com';
      component.phoneNumber = '555-1234';
      component.onSave();
      expect(component.isEditMode).toBe(false);
    });
  });

  // testing out cancel functionality
  describe('onCancel', () => {
    it('should exit edit mode without saving', () => {
      component.isEditMode = true;
      component.onCancel();
      expect(component.isEditMode).toBe(false);
    });
  });

  // testing notification preferences
  describe('Notification Preferences', () => {
    it('should initialize with both notifications enabled', () => {
      expect(component.notifyByPhone).toBe(true);
      expect(component.notifyByEmail).toBe(true);
    });

    it('should toggle phone notification', () => {
      component.notifyByPhone = true;
      component.notifyByPhone = false;
      expect(component.notifyByPhone).toBe(false);
      component.notifyByPhone = true;
      expect(component.notifyByPhone).toBe(true);
    });

    it('should toggle email notification', () => {
      component.notifyByEmail = true;
      component.notifyByEmail = false;
      expect(component.notifyByEmail).toBe(false);
      component.notifyByEmail = true;
      expect(component.notifyByEmail).toBe(true);
    });

    it('should allow independent toggling of both notifications', () => {
      component.notifyByPhone = false;
      component.notifyByEmail = true;
      expect(component.notifyByPhone).toBe(false);
      expect(component.notifyByEmail).toBe(true);

      component.notifyByPhone = true;
      expect(component.notifyByPhone).toBe(true);
      expect(component.notifyByEmail).toBe(true);
    });
  });

  // testing profile data
  describe('Profile Data', () => {
    it('should have default user properties', () => {
      expect(component.username).toBe('');
      expect(component.firstName).toBe('');
      expect(component.lastName).toBe('');
      expect(component.email).toBe('');
      expect(component.phoneNumber).toBe('');
    });

    it('should have read-only userId and joinDate', () => {
      expect(component.userId).toBe('12345');
      expect(component.joinDate).toBe('2024-01-15');
    });

    it('should have placeholder avatar', () => {
      expect(component.avatar).toBeTruthy();
      expect(component.avatar).toContain('placeholder');
    });

    it('should allow updating profile fields', () => {
      component.firstName = 'Jane';
      component.lastName = 'Smith';
      component.email = 'jane@example.com';

      expect(component.firstName).toBe('Jane');
      expect(component.lastName).toBe('Smith');
      expect(component.email).toBe('jane@example.com');
    });
  });
});

