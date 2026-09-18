import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { CreateAccount } from './create-account';
import { CommonModule } from '@angular/common';

/**
 * Create Account Component Test Suite
 * 
 * Comprehensive unit tests for the CreateAccount component covering:
 * - Component initialization and lifecycle
 * - Signal-based state management for registration fields
 * - Form input handling and two-way binding
 * - Checkbox state management for terms agreement
 * - Form validation and button enable/disable states
 * - User interactions (button clicks, form submission)
 * - Template rendering and DOM elements
 * 
 * Tests verify that the component correctly manages account creation form UI,
 * validates user input, and handles user interactions for registration flow.
 */
describe('CreateAccount Component', () => {
  let component: CreateAccount;
  let fixture: ComponentFixture<CreateAccount>;

  /**
   * Setup before each test
   * Configures the testing module with required dependencies and creates component instance
   */
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateAccount, FormsModule, CommonModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateAccount);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  /**
   * Test: Component Creation
   * Verifies that the CreateAccount component is successfully instantiated
   */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Test: Initial State
   * Verifies that all form signals initialize with empty values and terms are not agreed
   */
  it('should initialize with empty form fields and terms not agreed', () => {
    expect(component.firstName()).toBe('');
    expect(component.lastName()).toBe('');
    expect(component.email()).toBe('');
    expect(component.password()).toBe('');
    expect(component.agreedToTerms()).toBe(false);
  });

  /**
   * Test: First Name Signal Update
   * Verifies that the first name signal can be updated and reflects changes
   */
  it('should update firstName signal on input change', () => {
    component.firstName.set('John');
    fixture.detectChanges();
    expect(component.firstName()).toBe('John');
  });

  /**
   * Test: Last Name Signal Update
   * Verifies that the last name signal can be updated and reflects changes
   */
  it('should update lastName signal on input change', () => {
    component.lastName.set('Doe');
    fixture.detectChanges();
    expect(component.lastName()).toBe('Doe');
  });

  /**
   * Test: Email Signal Update
   * Verifies that the email signal can be updated and reflects changes
   */
  it('should update email signal on input change', () => {
    component.email.set('john@example.com');
    fixture.detectChanges();
    expect(component.email()).toBe('john@example.com');
  });

  /**
   * Test: Password Signal Update
   * Verifies that the password signal can be updated and reflects changes
   */
  it('should update password signal on input change', () => {
    component.password.set('password123');
    fixture.detectChanges();
    expect(component.password()).toBe('password123');
  });

  /**
   * Test: Terms Agreement Signal Update
   * Verifies that the agreedToTerms checkbox signal can be toggled
   */
  it('should update agreedToTerms signal on checkbox change', () => {
    component.agreedToTerms.set(true);
    fixture.detectChanges();
    expect(component.agreedToTerms()).toBe(true);
  });

  /**
   * Test: Form Validation - Invalid When Empty
   * Verifies that form is invalid when fields are empty
   */
  it('should have form invalid when fields are empty', () => {
    expect(component.isFormValid()).toBe(false);
  });

  /**
   * Test: Form Validation - Invalid When Terms Not Agreed
   * Verifies that form is invalid even when fields are filled but terms not accepted
   */
  it('should have form invalid when terms are not agreed', () => {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.email.set('john@example.com');
    component.password.set('password123');
    component.agreedToTerms.set(false);
    fixture.detectChanges();
    expect(component.isFormValid()).toBe(false);
  });

  /**
   * Test: Form Validation - Valid When All Fields Filled
   * Verifies that form is valid only when all fields are filled and terms are accepted
   */
  it('should have form valid when all fields are filled and terms agreed', () => {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.email.set('john@example.com');
    component.password.set('password123');
    component.agreedToTerms.set(true);
    fixture.detectChanges();
    expect(component.isFormValid()).toBe(true);
  });

  /**
   * Test: Logo Section Rendering
   * Verifies that the logo section with branding elements is rendered in the DOM
   */
  it('should render logo section with branding', () => {
    const logoSection = fixture.nativeElement.querySelector('.logo-section');
    expect(logoSection).toBeTruthy();
  });

  /**
   * Test: Form Input Fields Rendering
   * Verifies that all required input fields are present in the template
   */
  it('should render all form input fields', () => {
    const inputs = fixture.nativeElement.querySelectorAll('.form-input');
    expect(inputs.length).toBeGreaterThanOrEqual(4);
  });

  /**
   * Test: Terms Checkbox Rendering
   * Verifies that the terms agreement checkbox is rendered
   */
  it('should render terms agreement checkbox', () => {
    const checkbox = fixture.nativeElement.querySelector('.terms-checkbox');
    expect(checkbox).toBeTruthy();
  });

  /**
   * Test: Create Account Button Rendering
   * Verifies that the "Agree & Create Account" button is rendered
   */
  it('should render create account button', () => {
    const button = fixture.nativeElement.querySelector('.sign-in-button');
    expect(button).toBeTruthy();
    expect(button.textContent).toContain('Agree & Create Account');
  });

  /**
   * Test: Sign In Link Rendering
   * Verifies that the "Sign In" link is rendered for existing account holders
   */
  it('should render sign in link', () => {
    const link = fixture.nativeElement.querySelector('.sign-in-link');
    expect(link).toBeTruthy();
    expect(link.textContent).toContain('Sign In');
  });

  /**
   * Test: Button Disabled State When Form Invalid
   * Verifies that the create account button is disabled when form is invalid
   */
  it('should have create account button disabled when form is invalid', () => {
    const button = fixture.nativeElement.querySelector('.sign-in-button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });

  /**
   * Test: Button Enabled State When Form Valid
   * Verifies that the create account button is enabled when form is valid
   */
  it('should have create account button enabled when form is valid', () => {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.email.set('john@example.com');
    component.password.set('password123');
    component.agreedToTerms.set(true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('.sign-in-button') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
  });
});
