// Piezas visuales compartidas por todos los paneles.

export function Panel({ eyebrow, title, aside, children, className = '' }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-accent" />
      <div className="panel-body">
        {(eyebrow || title || aside) && (
          <div className="panel-head">
            <div>
              {eyebrow && <span className="eyebrow">{eyebrow}</span>}
              {title && <h2>{title}</h2>}
            </div>
            {aside}
          </div>
        )}
        {children}
      </div>
    </section>
  )
}

export function Stat({ label, value, hint }) {
  return (
    <div className="stat">
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value ?? '—'}</strong>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  )
}

/** Estado con punto y texto: el color nunca va solo. */
export function StatusPill({ tone, children }) {
  return <span className={`pill pill-${tone}`}><i />{children}</span>
}

export function HttpStatus({ status }) {
  const tone = status === 0 ? 'down' : status < 300 ? 'ok' : status < 500 ? 'warn' : 'down'
  return <span className={`http http-${tone}`}>{status === 0 ? 'sin red' : status}</span>
}

export function Notice({ children }) {
  return <p className="notice">{children}</p>
}

export function formatTime(value) {
  if (!value) return '—'
  // Resilience4j serializa ZonedDateTime como "...Z[Etc/UTC]"; Date no acepta la zona.
  const date = new Date(typeof value === 'string' ? value.replace(/\[.*\]$/, '') : value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleTimeString('es-CO', { hour12: false })
}
