import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TableComponent } from './table';

describe('Table', () => {
  let component: TableComponent<{ id: string }>;
  let fixture: ComponentFixture<TableComponent<{ id: string }>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TableComponent<{ id: string }>);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render one row per item and move selection on click', () => {
    component.data = [{ id: 'order-1' }, { id: 'order-2' }];
    component.columns = [{ key: 'id', header: 'Order' }];
    component.rowKey = 'id';
    component.selectedRowKey = 'order-1';
    component.rowSelected.subscribe(row => component.selectedRowKey = row.id);
    fixture.detectChanges();

    const rows: NodeListOf<HTMLTableRowElement> = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].classList.contains('selected')).toBe(true);
    rows[1].click();
    fixture.detectChanges();
    expect(rows[0].classList.contains('selected')).toBe(false);
    expect(rows[1].getAttribute('aria-selected')).toBe('true');

    component.data = component.data.map(row => ({ ...row }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.selected')?.textContent).toContain('order-2');
  });

  it('should allow keyboard selection and clearing the highlight', () => {
    component.data = [{ id: 'order-1' }];
    component.rowKey = 'id';
    component.rowSelected.subscribe(row => component.selectedRowKey = row.id);
    fixture.detectChanges();

    const row: HTMLTableRowElement = fixture.nativeElement.querySelector('tbody tr');
    row.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();
    expect(row.classList.contains('selected')).toBe(true);
    component.selectedRowKey = null;
    fixture.detectChanges();
    expect(row.classList.contains('selected')).toBe(false);
    row.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));
    fixture.detectChanges();
    expect(row.classList.contains('selected')).toBe(true);
  });

  it('should leave tables without a selection key unchanged', () => {
    component.data = [{ id: 'order-1' }];
    fixture.detectChanges();
    const row: HTMLTableRowElement = fixture.nativeElement.querySelector('tbody tr');
    expect(row.classList.contains('selectable')).toBe(false);
    expect(row.hasAttribute('tabindex')).toBe(false);
    expect(row.hasAttribute('aria-selected')).toBe(false);
  });
});
