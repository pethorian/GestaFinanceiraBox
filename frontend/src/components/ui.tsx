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

export function Segmented<T extends string>({ name, value, options, onChange, label }: {
  name: string; value: T; options: { value: T; label: string }[]; onChange: (v: T) => void; label?: string
}) {
  return (
    <div className="seg" role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <label key={o.value}>
          <input type="radio" name={name} value={o.value} checked={value === o.value} onChange={() => onChange(o.value)} />
          {o.label}
        </label>
      ))}
    </div>
  )
}

/**
 * Input numérico que devolve number (vazio vira 0). Os spinners nativos ficam escondidos (CSS global);
 * `stepper` mostra botões − / + no padrão visual do app (↑/↓ do teclado continuam funcionando) e
 * `prefix` mostra um prefixo fixo (ex.: "R$"). Sem nenhum dos dois, renderiza só o <input>.
 */
export function NumberInput({ value, onChange, stepper, prefix, style, ...rest }: {
  value: number; onChange: (v: number) => void; stepper?: boolean; prefix?: string
} & Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type'>) {
  const envolto = !!prefix || !!stepper
  const input = (
    <input type="number" value={value} style={envolto ? undefined : style}
      onChange={(e) => onChange(Number(e.target.value) || 0)} {...rest} />
  )
  if (!envolto) return input

  const step = Number(rest.step) || 1
  const min = rest.min !== undefined ? Number(rest.min) : -Infinity
  const max = rest.max !== undefined ? Number(rest.max) : Infinity
  const mudar = (dir: 1 | -1) => {
    const novo = Math.round((value + dir * step) * 100) / 100
    onChange(Math.min(max, Math.max(min, novo)))
  }
  const label = rest['aria-label']
  return (
    <span className={`nin${rest.disabled ? ' off' : ''}`} style={style}>
      {prefix && <em>{prefix}</em>}
      {input}
      {stepper && (
        <span className="stp">
          <button type="button" tabIndex={-1} disabled={rest.disabled || value <= min} aria-label={label ? `Diminuir ${label}` : 'Diminuir'} onClick={() => mudar(-1)}>−</button>
          <button type="button" tabIndex={-1} disabled={rest.disabled || value >= max} aria-label={label ? `Aumentar ${label}` : 'Aumentar'} onClick={() => mudar(1)}>+</button>
        </span>
      )}
    </span>
  )
}
