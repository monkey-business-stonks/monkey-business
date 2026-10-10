export interface SelectOption {
  label: string;
  value: string | number;
}

export interface MetricCardData {
  title: string;
  value: string;
  badge?: string;
  badgeType?: 'positive' | 'negative' | 'neutral' | 'warning';
  subtext?: string;
}

export interface TableColumn {
  key: string;
  header: string;
  align?: 'left' | 'center' | 'right';
  paddingX?: string;
  className?: string;
  isMonospace?: boolean;
}