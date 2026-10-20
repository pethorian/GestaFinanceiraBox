const PATHS = {
  chart: <><path d="M3 3v18h18" /><path d="M7 15l4-4 3 3 5-6" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0116 0" /></>,
  receipt: <><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z" /><path d="M9 8h6M9 12h6" /></>,
  gift: <><rect x="3" y="8" width="18" height="4" rx="1" /><path d="M12 8v13M5 12v9h14v-9M8 8a2.5 2.5 0 110-5c2 0 4 5 4 5s2-5 4-5a2.5 2.5 0 110 5" /></>,
  target: <><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="5" /><circle cx="12" cy="12" r="1" /></>,
  cog: <><circle cx="12" cy="12" r="3" /><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9L7 7M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1" /></>,
  up: <path d="M7 17L17 7M8 7h9v9" />,
  down: <path d="M7 7l10 10M17 8v9H8" />,
  wallet: <><path d="M3 7a2 2 0 012-2h13v4" /><path d="M3 7v11a2 2 0 002 2h15V9H5a2 2 0 01-2-2z" /><circle cx="16.5" cy="14.5" r="1" /></>,
  card: <><rect x="2.5" y="5" width="19" height="14" rx="2.5" /><path d="M2.5 10h19" /></>,
  star: <path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z" />,
  warn: <><path d="M12 3l10 18H2z" /><path d="M12 10v5M12 18v.01" /></>,
  bars: <path d="M4 19V9M10 19V5M16 19v-7M22 19H2" />,
  close: <path d="M6 6l12 12M18 6L6 18" />,
  plus: <path d="M12 5v14M5 12h14" />,
} as const

export type IconName = keyof typeof PATHS

export function Icon({ name, size, strokeWidth }: { name: IconName; size?: number; strokeWidth?: number }) {
  return (
    <svg
      className="i"
      viewBox="0 0 24 24"
      aria-hidden="true"
      style={{ ...(size ? { width: size, height: size } : null), ...(strokeWidth ? { strokeWidth } : null) }}
    >
      {PATHS[name]}
    </svg>
  )
}
