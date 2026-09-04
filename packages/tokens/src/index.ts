export const color = {
  primary900: '#12372B',
  primary700: '#174A3A',
  primary600: '#255F49',
  primary100: '#DDEBE3',
  sand50: '#FBF9F4',
  sand100: '#F5F1E8',
  accentGold: '#C89B5A',
  neutral900: '#1E2522',
  neutral600: '#64706A',
  neutral300: '#D8DED9',
  success: '#2E7D32',
  warning: '#B7791F',
  error: '#C2413B',
  info: '#2F6B9A',
} as const;

export const semantic = {
  color: {
    text: 'var(--hc-color-text)',
    textMuted: 'var(--hc-color-text-muted)',
    background: 'var(--hc-color-bg)',
    surface: 'var(--hc-color-surface)',
    border: 'var(--hc-color-border)',
    brand: 'var(--hc-color-brand)',
    brandHover: 'var(--hc-color-brand-hover)',
    brandContrast: 'var(--hc-color-brand-contrast)',
    accent: 'var(--hc-color-accent)',
    success: 'var(--hc-color-success)',
    warning: 'var(--hc-color-warning)',
    error: 'var(--hc-color-error)',
    info: 'var(--hc-color-info)',
    focus: 'var(--hc-color-focus)',
  },
} as const;

export const spacing = {
  0: 0,
  1: 4,
  2: 8,
  3: 12,
  4: 16,
  5: 20,
  6: 24,
  8: 32,
  10: 40,
  12: 48,
} as const;

export const radius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
} as const;

export const elevation = {
  none: 'none',
  low: '0 1px 2px rgba(18, 55, 43, 0.08)',
  medium: '0 8px 24px rgba(18, 55, 43, 0.12)',
} as const;

export const typography = {
  fontArabic: '"IBM Plex Sans Arabic", "Noto Sans Arabic", system-ui, sans-serif',
  fontLatin: 'Inter, system-ui, sans-serif',
  scale: {
    display: { size: 40, line: 48 },
    h1: { size: 32, line: 40 },
    h2: { size: 24, line: 32 },
    h3: { size: 20, line: 28 },
    body: { size: 16, line: 24 },
    small: { size: 14, line: 20 },
    caption: { size: 12, line: 16 },
  },
} as const;

export const motion = {
  fast: '120ms ease-out',
  base: '180ms ease-out',
  slow: '280ms ease-out',
} as const;

export const breakpoints = {
  mobile: 390,
  tablet: 768,
  desktop: 1200,
  canvas: 1440,
  contentMax: 1280,
} as const;

export const control = {
  minHeight: 44,
  preferredHeight: 48,
  maxHeight: 52,
  minTouchTarget: 44,
} as const;

export const border = {
  width: 1,
} as const;
