import { useEffect, useRef, useState } from 'react'
import { get } from '../api.js'
import { usePolling } from '../usePolling.js'
import { Panel, Stat, Notice, formatTime } from './ui.jsx'

const TOPICO = 'hospital.events'

/**
 * Prueba visible de que Kafka está corriendo: el tópico con sus particiones y
 * offsets (vía Kafka UI) y los eventos que audit-service ya consumió.
 */
export default function KafkaPanel() {
  const eventos = usePolling(() => get('/api/events', { log: false }), 2000)
  const topico = usePolling(
    () => get(`/broker/kafka/clusters/hospital/topics/${TOPICO}`, { log: false }),
    4000,
  )
  const [filtro, setFiltro] = useState(null)

  // Resalta los eventos que no estaban en el sondeo anterior.
  const vistos = useRef(null)
  const lista = eventos?.ok && Array.isArray(eventos.data) ? eventos.data : []
  const nuevos = new Set()
  if (vistos.current) lista.forEach((e) => !vistos.current.has(e.eventId) && nuevos.add(e.eventId))
  useEffect(() => {
    if (eventos?.ok) vistos.current = new Set(lista.map((e) => e.eventId))
  })

  const porTipo = {}
  lista.forEach((e) => (porTipo[e.eventType] = (porTipo[e.eventType] ?? 0) + 1))
  const tipos = Object.entries(porTipo).sort((a, b) => b[1] - a[1])
  const maxTipo = Math.max(1, ...tipos.map(([, n]) => n))

  const particiones = topico?.ok ? [...(topico.data?.partitions ?? [])] : []
  particiones.sort((a, b) => a.partition - b.partition)
  const offsetTotal = particiones.reduce((s, p) => s + (p.offsetMax - p.offsetMin), 0)
  const maxParticion = Math.max(1, ...particiones.map((p) => p.offsetMax - p.offsetMin))

  const visibles = filtro ? lista.filter((e) => e.eventType === filtro) : lista

  return (
    <Panel
      eyebrow="En vivo · cada 2 s"
      title="Apache Kafka"
      aside={<a className="btn btn-secondary" href="http://localhost:8090" target="_blank" rel="noreferrer">Abrir Kafka UI ↗</a>}
    >
      <div className="stat-grid">
        <Stat label="Mensajes en el tópico" value={topico?.ok ? offsetTotal : null} hint={TOPICO} />
        <Stat label="Particiones" value={topico?.ok ? particiones.length : null} hint="KRaft, sin ZooKeeper" />
        <Stat label="Consumidos por auditoría" value={eventos?.ok ? lista.length : null} hint="audit-service" />
        <Stat label="Tipos de evento" value={eventos?.ok ? tipos.length : null} />
      </div>

      {topico && !topico.ok && (
        <Notice>
          Kafka UI no responde todavía ({topico.status}). El tópico se crea con el primer evento publicado.
        </Notice>
      )}

      {particiones.length > 0 && (
        <>
          <h3>Particiones del tópico</h3>
          <p className="muted small">
            La clave del mensaje (<code>doctor:1</code>, <code>client:1</code>…) decide la partición:
            los cambios de una misma entidad quedan en orden.
          </p>
          <div className="bars">
            {particiones.map((p) => {
              const n = p.offsetMax - p.offsetMin
              return (
                <div className="bar-row" key={p.partition} title={`Partición ${p.partition}: offsets ${p.offsetMin}–${p.offsetMax}`}>
                  <span className="bar-label">Partición {p.partition}</span>
                  <div className="bar-track"><div className="bar-fill" style={{ width: `${(n / maxParticion) * 100}%` }} /></div>
                  <span className="bar-value">{n}</span>
                </div>
              )
            })}
          </div>
        </>
      )}

      {tipos.length > 0 && (
        <>
          <h3>Eventos por tipo</h3>
          <div className="bars">
            {tipos.map(([tipo, n]) => (
              <div className="bar-row" key={tipo}>
                <span className="bar-label"><code>{tipo}</code></span>
                <div className="bar-track"><div className="bar-fill" style={{ width: `${(n / maxTipo) * 100}%` }} /></div>
                <span className="bar-value">{n}</span>
              </div>
            ))}
          </div>
        </>
      )}

      <div className="feed-head">
        <h3>Flujo de eventos</h3>
        <div className="chips">
          <button className={`chip${!filtro ? ' active' : ''}`} onClick={() => setFiltro(null)}>Todos</button>
          {tipos.map(([tipo]) => (
            <button key={tipo} className={`chip${filtro === tipo ? ' active' : ''}`} onClick={() => setFiltro(tipo)}>
              {tipo}
            </button>
          ))}
        </div>
      </div>

      {eventos && !eventos.ok && <Notice>audit-service no responde ({eventos.status}).</Notice>}
      {eventos?.ok && lista.length === 0 && (
        <Notice>Aún no hay eventos. Crea un médico o un paciente abajo y aparecerá aquí en segundos.</Notice>
      )}
      <ol className="feed">
        {visibles.slice(0, 25).map((e) => (
          <li key={e.eventId} className={nuevos.has(e.eventId) ? 'fresh' : undefined}>
            <div className="feed-top">
              <span className="option-label">{e.eventType}</span>
              <code className="feed-key">{e.aggregateId}</code>
              <time>{formatTime(e.occurredAt)}</time>
            </div>
            <div className="feed-payload">
              {Object.entries(e.payload ?? {}).map(([k, v]) => (
                <span key={k}><b>{k}</b> {String(v)}</span>
              ))}
            </div>
          </li>
        ))}
      </ol>
    </Panel>
  )
}
