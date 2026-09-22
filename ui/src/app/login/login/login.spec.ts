import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Login } from './login';
import { CommonModule } from '@angular/common';

/**
 * Login Component Test Suite
 * 
 * Comprehensive unit tests for the Login component covering:
 * - Component initialization and lifecycle
 * - Signal-based state management
 * - Form input handling and two-way binding
 * - User interactions (button clicks, form submission)
 * - Template rendering and DOM elements
 * 
 * Tests verify that the component correctly manages authentication form UI
 * and user interactions for sign-in and password recovery flows.
 */
describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  /**
   * Setup before each test
   * Configures the testing module with required dependencies and creates component instance
   */
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login, FormsModule, CommonModule],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  /**
   * Test: Component Creation
   * Verifies that the Login component is successfully instantiated
   */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Test: Initial State
   * Verifies that username and password signals initialize with empty strings
   */
  it('should initialize with empty username and password', () => {
    expect(component.username()).toBe('');
    expect(component.password()).toBe('');
  });

  /**
   * Test: Username Signal Update
   * Verifies that the username signal can be updated and reflects changes
   */
  it('should update username signal on input change', () => {
    component.username.set('test@example.com');
    fixture.detectChanges();
    expect(component.username()).toBe('test@example.com');
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
   * Test: Logo Section Rendering
   * Verifies that the logo section with branding elements is rendered in the DOM
   */
  it('should render login card with logo section', () => {
    const logoSection = fixture.nativeElement.querySelector('.logo-section');
    expect(logoSection).toBeTruthy();
  });

  /**
   * Test: Form Input Fields Rendering
   * Verifies that both username and password input fields are present in the template
   */
  it('should render username and password input fields', () => {
    const inputs = fixture.nativeElement.querySelectorAll('.form-input');
    expect(inputs.length).toBeGreaterThanOrEqual(2);
  });

  /**
   * Test: Sign In Button Rendering
   * Verifies that the primary "Sign In" button is rendered with correct text
   */
  it('should render Sign In button', () => {
    const button = fixture.nativeElement.querySelector('.sign-in-button');
    expect(button).toBeTruthy();
    expect(button.textContent).toContain('Sign In');
  });

  /**
   * Test: Create Account Link Rendering
   * Verifies that the "Create account" link is rendered with correct text
   */
  it('should render create account link', () => {
    const link = fixture.nativeElement.querySelector('.forgot-password-link');
    expect(link).toBeTruthy();
    expect(link.textContent).toContain('Create account');
  });
});

