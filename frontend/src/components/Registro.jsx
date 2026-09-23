import { Panel, HttpStatus, formatTime } from './ui.jsx'

/** Últimas peticiones hechas desde este navegador, con estado y latencia. */
export default function Registro({ peticiones }) {
  return (
    <Panel eyebrow="Este navegador" title="Registro de peticiones">
      {peticiones.length === 0 && <p className="muted">Todavía no has enviado peticiones.</p>}
      <ol className="log">
        {peticiones.slice(0, 15).map((p) => (
          <li key={p.id}>
            <time>{formatTime(p.at)}</time>
            <span className="method">{p.method}</span>
            <code>{p.path}</code>
            <HttpStatus status={p.status} />
            <span className="ms">{p.ms} ms</span>
          </li>
        ))}
      </ol>
    </Panel>
  )
}
