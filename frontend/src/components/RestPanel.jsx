import { get } from '../api.js'
import { usePolling } from '../usePolling.js'
import { Panel, StatusPill, Notice, formatTime } from './ui.jsx'

const TONO_EVENTO = {
  SUCCESS: 'ok',
  ERROR: 'down',
  NOT_PERMITTED: 'down',
  STATE_TRANSITION: 'warn',
  IGNORED_ERROR: 'warn',
}

/**
 * Sin broker, lo interesante es la cadena síncrona: cuánto tarda y qué pasa
 * cuando se rompe. Se muestran los eventos de Resilience4j del gateway.
 */
export default function RestPanel({ peticiones }) {
  const eventos = usePolling(() => get('/actuator/circuitbreakerevents', { log: false }), 2000)
  const lista = eventos?.ok ? [...(eventos.data?.circuitBreakerEvents ?? [])].reverse().slice(0, 10) : []
  const historias = peticiones
    .filter((p) => p.path.startsWith('/api/histories') && p.method !== 'GET')
    .slice(0, 5)

  return (
    <Panel eyebrow="En vivo · cada 2 s" title="Llamadas síncronas">
      <p className="muted">
        Crear una historia recorre <b>gateway → historias → médicos</b> en la misma petición.
        La respuesta tarda lo que tarde el servicio más lento de la cadena.
      </p>

      <h3>Validaciones por OpenFeign</h3>
      {historias.length === 0 && <Notice>Crea una historia clínica para ver la llamada encadenada.</Notice>}
      <ul className="service-list">
        {historias.map((p) => (
          <li key={p.id}>
            <span className="service-name">
              Médico {p.body?.doctorId}
              <small>{formatTime(p.at)} · {p.ms} ms en total</small>
            </span>
            <StatusPill tone={p.ok ? 'ok' : p.status === 400 ? 'warn' : 'down'}>
              {p.ok ? 'Validado' : p.status === 400 ? 'No existe' : 'Médicos caído'}
            </StatusPill>
          </li>
        ))}
      </ul>

      <h3>Eventos de los circuit breakers</h3>
      <ul className="service-list">
        {lista.map((e, i) => (
          <li key={`${e.creationTime}-${i}`}>
            <span className="service-name">
              {e.circuitBreakerName}
              <small>
                {formatTime(e.creationTime)}
                {e.durationInMs != null && ` · ${e.durationInMs} ms`}
                {e.stateTransition && ` · ${e.stateTransition}`}
              </small>
            </span>
            <StatusPill tone={TONO_EVENTO[e.type] ?? 'warn'}>{e.type}</StatusPill>
          </li>
        ))}
      </ul>

      <div className="notice">
        <b>Pruébalo:</b> <code>docker compose stop doctor-service</code> y consulta médicos varias veces.
        Tras 5 llamadas con 50 % de fallos el circuito <code>doctorGateway</code> se abre y el gateway
        responde 503 al instante. Con <code>docker compose start doctor-service</code> vuelve solo.
      </div>
    </Panel>
  )
}
