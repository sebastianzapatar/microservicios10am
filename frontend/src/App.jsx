import { useEffect, useState } from 'react'
import { loadArquitectura, onRequest } from './api.js'
import { ARQUITECTURAS } from './arquitecturas.js'
import Flujo from './components/Flujo.jsx'
import Plataforma from './components/Plataforma.jsx'
import KafkaPanel from './components/KafkaPanel.jsx'
import RabbitPanel from './components/RabbitPanel.jsx'
import RestPanel from './components/RestPanel.jsx'
import Operaciones from './components/Operaciones.jsx'
import Registro from './components/Registro.jsx'

const PANEL = { rest: RestPanel, rabbitmq: RabbitPanel, kafka: KafkaPanel }

export default function App() {
  const [modo, setModo] = useState(null)
  const [peticiones, setPeticiones] = useState([])
  const [pulso, setPulso] = useState(0)

  useEffect(() => {
    loadArquitectura().then((m) => setModo(ARQUITECTURAS[m] ? m : 'rest'))
  }, [])
  useEffect(() => onRequest((p) => setPeticiones((prev) => [p, ...prev].slice(0, 50))), [])

  if (!modo) return null
  const arq = ARQUITECTURAS[modo]
  const PanelMensajeria = PANEL[modo]

  return (
    <>
      <header className="site-header">
        <div className="container header-inner">
          <div className="brand">
            <div className="brand-logo" aria-hidden="true">H+</div>
            <div>
              <div className="brand-name">Hospital Microservicios</div>
              <div className="brand-subtitle">Panel de demostración · rama <code>{arq.rama}</code></div>
            </div>
          </div>
          <span className="badge">{arq.badge}</span>
        </div>
      </header>

      <main className="container main-shell">
        <section className="panel">
          <div className="panel-accent" />
          <div className="panel-body">
            <span className="eyebrow">Arquitectura activa · {arq.badge}</span>
            <h1>{arq.titulo}</h1>
            <p className="lead">{arq.lead}</p>
            <Flujo pasos={arq.flujo} pulso={pulso} />
          </div>
        </section>

        <div className="grid-2">
          <PanelMensajeria peticiones={peticiones} />
          <div className="stack">
            <Plataforma />
            <Registro peticiones={peticiones} />
          </div>
        </div>

        <Operaciones onEscritura={() => setPulso((n) => n + 1)} />
      </main>

      <footer className="site-footer">
        <div className="container">
          <p>API Gateway <code>localhost:8080</code> · Eureka <code>localhost:8762</code>
            {modo === 'rabbitmq' && <> · RabbitMQ <code>localhost:15672</code></>}
            {modo === 'kafka' && <> · Kafka UI <code>localhost:8090</code></>}
          </p>
          <p className="credit">Proyecto académico de microservicios · Spring Boot, Go, React.</p>
        </div>
      </footer>
    </>
  )
}
