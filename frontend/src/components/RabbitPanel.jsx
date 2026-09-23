import { useEffect, useState } from 'react'
import { get } from '../api.js'
import { usePolling } from '../usePolling.js'
import { Panel, Stat, StatusPill, Notice, formatTime } from './ui.jsx'

const COLA = 'doctor.validation'
const EXCHANGE = 'hospital.doctors'
const MUESTRAS = 30

// Traduce la respuesta HTTP de historias al resultado de la validación AMQP.
const RESULTADO_RPC = {
  200: { tone: 'ok', texto: 'FOUND' },
  201: { tone: 'ok', texto: 'FOUND' },
  400: { tone: 'warn', texto: 'NOT_FOUND' },
  503: { tone: 'down', texto: 'Sin respuesta (timeout 3 s)' },
}

/** Cola, exchange y tráfico de RabbitMQ, leídos de su API de administración. */
export default function RabbitPanel({ peticiones }) {
  const overview = usePolling(() => get('/broker/rabbitmq/overview', { log: false }), 3000)
  const cola = usePolling(() => get(`/broker/rabbitmq/queues/%2F/${COLA}`, { log: false }), 2000)

  // Serie de mensajes publicados por intervalo, para ver el tráfico llegar.
  const publicados = cola?.ok ? cola.data?.message_stats?.publish ?? 0 : null
  const [serie, setSerie] = useState({ ultimo: null, valores: [] })
  useEffect(() => {
    if (publicados === null) return
    setSerie(({ ultimo, valores }) => ({
      ultimo: publicados,
      valores: [...valores, ultimo === null ? 0 : Math.max(0, publicados - ultimo)].slice(-MUESTRAS),
    }))
  }, [cola])
  const maxSerie = Math.max(1, ...serie.valores)

  const q = cola?.ok ? cola.data : null
  const rpc = peticiones
    .filter((p) => p.path.startsWith('/api/histories') && p.method !== 'GET')
    .slice(0, 6)

  return (
    <Panel
      eyebrow="En vivo · cada 2 s"
      title="RabbitMQ"
      aside={<a className="btn btn-secondary" href="http://localhost:15672" target="_blank" rel="noreferrer">Abrir Management ↗</a>}
    >
      <div className="stat-grid">
        <Stat label="Consumidores" value={q?.consumers} hint="doctor-service" />
        <Stat label="Mensajes en cola" value={q ? q.messages_ready + q.messages_unacknowledged : null} hint="se vacía en ms" />
        <Stat label="Solicitudes publicadas" value={publicados} hint={COLA} />
        <Stat label="Versión del broker" value={overview?.ok ? overview.data.rabbitmq_version : null} />
      </div>

      {cola && !cola.ok && (
        <Notice>La cola {COLA} no existe todavía ({cola.status}): se declara cuando arrancan los servicios.</Notice>
      )}

      <h3>Topología</h3>
      <div className="topology">
        <div className="topo-node"><small>exchange · direct</small><strong>{EXCHANGE}</strong></div>
        <div className="topo-link">doctor.validation.request</div>
        <div className="topo-node highlight">
          <small>cola · durable · TTL 5 s</small><strong>{COLA}</strong>
        </div>
        <div className="topo-link">@RabbitListener</div>
        <div className="topo-node"><small>consumidor</small><strong>doctor-service</strong></div>
      </div>

      <h3>Solicitudes publicadas por intervalo</h3>
      <div className="spark" role="img" aria-label="Solicitudes de validación publicadas en los últimos sondeos">
        {Array.from({ length: MUESTRAS }, (_, i) => {
          const v = serie.valores[i - (MUESTRAS - serie.valores.length)]
          return (
            <span key={i} title={v === undefined ? '' : `${v} solicitudes`}>
              <i style={{ height: v ? `${Math.max(8, (v / maxSerie) * 100)}%` : 0 }} />
            </span>
          )
        })}
      </div>
      <p className="muted small">Cada historia creada o editada publica una solicitud de validación.</p>

      <h3>Últimas validaciones request/reply</h3>
      {rpc.length === 0 && <Notice>Crea o edita una historia clínica para ver la validación por la cola.</Notice>}
      <ul className="service-list">
        {rpc.map((p) => {
          const r = RESULTADO_RPC[p.status] ?? { tone: 'down', texto: `HTTP ${p.status}` }
          return (
            <li key={p.id}>
              <span className="service-name">
                Médico {p.body?.doctorId}
                <small>{formatTime(p.at)} · {p.ms} ms de ida y vuelta</small>
              </span>
              <StatusPill tone={r.tone}>{r.texto}</StatusPill>
            </li>
          )
        })}
      </ul>
    </Panel>
  )
}
