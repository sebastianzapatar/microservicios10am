import { get } from '../api.js'
import { usePolling } from '../usePolling.js'
import { Panel, StatusPill, Notice } from './ui.jsx'

const ESTADO_CIRCUITO = {
  CLOSED: { tone: 'ok', texto: 'Cerrado' },
  HALF_OPEN: { tone: 'warn', texto: 'Semiabierto' },
  OPEN: { tone: 'down', texto: 'Abierto' },
}

/** Registro de Eureka y circuit breakers del gateway, consultados cada 3 s. */
export default function Plataforma() {
  const eureka = usePolling(() => get('/registry', { log: false }), 3000)
  const circuitos = usePolling(() => get('/actuator/circuitbreakers', { log: false }), 3000)

  const apps = eureka?.ok ? [].concat(eureka.data?.applications?.application ?? []) : []
  apps.sort((a, b) => a.name.localeCompare(b.name))
  const breakers = circuitos?.ok ? Object.entries(circuitos.data?.circuitBreakers ?? {}) : []

  return (
    <Panel eyebrow="Actualiza cada 3 s" title="Estado de la plataforma">
      <h3>Registrados en Eureka</h3>
      {eureka && !eureka.ok && <Notice>Eureka todavía no responde ({eureka.status}).</Notice>}
      <ul className="service-list">
        {apps.map((app) => {
          const instancias = [].concat(app.instance ?? [])
          const arriba = instancias.filter((i) => i.status === 'UP').length
          return (
            <li key={app.name}>
              <span className="service-name">{app.name.toLowerCase()}</span>
              <StatusPill tone={arriba ? 'ok' : 'down'}>
                {arriba}/{instancias.length} UP
              </StatusPill>
            </li>
          )
        })}
        {eureka?.ok && apps.length === 0 && <li className="muted">Ningún servicio registrado aún.</li>}
      </ul>

      <h3>Circuit breakers del gateway</h3>
      {circuitos && !circuitos.ok && <Notice>El gateway todavía no responde ({circuitos.status}).</Notice>}
      <ul className="service-list">
        {breakers.map(([nombre, cb]) => {
          const estado = ESTADO_CIRCUITO[cb.state] ?? { tone: 'warn', texto: cb.state }
          return (
            <li key={nombre}>
              <span className="service-name">
                {nombre}
                <small>
                  {cb.bufferedCalls} llamadas · {cb.failedCalls} fallidas
                  {cb.notPermittedCalls > 0 && ` · ${cb.notPermittedCalls} rechazadas`}
                </small>
              </span>
              <StatusPill tone={estado.tone}>{estado.texto}</StatusPill>
            </li>
          )
        })}
      </ul>
    </Panel>
  )
}
