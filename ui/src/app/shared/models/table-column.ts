export interface TableColumn<T = any> {
  key: keyof T | string;
  header: string;
  align?: 'left' | 'center' | 'right';
  paddingX?: string; // Custom padding control per column (e.g., 'var(--space-xl)')
  isMonospace?: boolean;
  className?: string;
}