import type { InputHTMLAttributes, ReactNode } from 'react'

export const cls = (v: number) => (v >= 0 ? 'pos' : 'neg')

export function Card({ children, className = '', ...rest }: { children: ReactNode; className?: string } & React.HTMLAttributes<HTMLDivElement>) {
  return <div className={`card ${className}`.trim()} {...rest}>{children}</div>
}

/** `fit`: a página ocupa exatamente a altura da janela (sem rolagem); o conteúdo se ajusta. */
export function Page({ title, subtitle, aside, fit, children }: {
  title: string; subtitle?: string; aside?: ReactNode; fit?: boolean; children: ReactNode
}) {
  return (
    <section className={`page${fit ? ' fit' : ''}`}>
      <div className="phead">
        <div>
          <h1>{title}</h1>
          {subtitle && <p className="sub">{subtitle}</p>}
        </div>
        {aside}
      </div>
      {children}
    </section>
  )
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="f">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  )
}

export function Switch({ checked, onChange, title, hint }: {
  checked: boolean; onChange: (v: boolean) => void; title: string; hint?: string
}) {
  return (
    <div className="tg">
      <div><b>{title}</b>{hint && <small>{hint}</small>}</div>
      <span className="sw">
        <input type="checkbox" role="switch" aria-label={title} checked={checked} onChange={(e) => onChange(e.target.checked)} />
        <i />
      </span>
    </div>
  )
}

export function Segmented<T extends string>({ name, value, options, onChange }: {
  name: string; value: T; options: { value: T; label: string }[]; onChange: (v: T) => void
}) {
  return (
    <div className="seg" role="radiogroup">
      {options.map((o) => (
        <label key={o.value}>
          <input type="radio" name={name} value={o.value} checked={value === o.value} onChange={() => onChange(o.value)} />
          {o.label}
        </label>
      ))}
    </div>
  )
}

/** Input numérico que devolve number (vazio vira 0). */
export function NumberInput({ value, onChange, ...rest }: {
  value: number; onChange: (v: number) => void
} & Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type'>) {
  return <input type="number" value={value} onChange={(e) => onChange(Number(e.target.value) || 0)} {...rest} />
}
