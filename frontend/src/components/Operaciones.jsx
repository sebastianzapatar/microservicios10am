import { useEffect, useState } from 'react'
import { get, post } from '../api.js'
import { Panel, HttpStatus, formatTime } from './ui.jsx'

const NOMBRES_MEDICOS = ['Dra. Ana Pérez', 'Dr. Luis Gómez', 'Dra. Sofía Ríos', 'Dr. Andrés Mejía']
const NOMBRES_PACIENTES = ['Carlos Ruiz', 'María López', 'Juan Restrepo', 'Laura Ortiz']
const DIAGNOSTICOS = ['Control general', 'Faringitis aguda', 'Migraña', 'Chequeo anual']
const azar = (lista) => lista[Math.floor(Math.random() * lista.length)]

/** Formularios para ejercitar la API a través del gateway. */
export default function Operaciones({ onEscritura }) {
  const [medicos, setMedicos] = useState([])
  const [pacientes, setPacientes] = useState([])
  const [historias, setHistorias] = useState([])
  const [medico, setMedico] = useState('')
  const [paciente, setPaciente] = useState('')
  const [historia, setHistoria] = useState({ clientId: '', doctorId: '', diagnosis: '', notes: '' })
  const [ocupado, setOcupado] = useState(false)
  const [ultima, setUltima] = useState(null)

  const cargar = async () => {
    const [m, p] = await Promise.all([get('/api/doctors', { log: false }), get('/api/clients', { log: false })])
    if (m.ok && Array.isArray(m.data)) setMedicos(m.data)
    if (p.ok && Array.isArray(p.data)) setPacientes(p.data)
  }
  useEffect(() => {
    cargar()
    const id = setInterval(cargar, 5000)
    return () => clearInterval(id)
  }, [])

  const cargarHistorias = async (clientId) => {
    if (!clientId) return setHistorias([])
    const r = await get(`/api/histories/client/${clientId}`)
    setHistorias(r.ok && Array.isArray(r.data) ? r.data : [])
  }
  useEffect(() => {
    cargarHistorias(historia.clientId)
  }, [historia.clientId])

  // Envuelve cada escritura: bloquea botones, anima el flujo y refresca listas.
  const escribir = async (fn) => {
    setOcupado(true)
    onEscritura()
    try {
      const r = await fn()
      setUltima(r)
      await cargar()
      return r
    } finally {
      setOcupado(false)
    }
  }

  const crearMedico = (e) => {
    e.preventDefault()
    escribir(() => post('/api/doctors', { name: medico.trim() || azar(NOMBRES_MEDICOS) }))
    setMedico('')
  }
  const crearPaciente = (e) => {
    e.preventDefault()
    escribir(() => post('/api/clients', { name: paciente.trim() || azar(NOMBRES_PACIENTES) }))
    setPaciente('')
  }
  const crearHistoria = async (e, doctorId = historia.doctorId) => {
    e.preventDefault()
    const r = await escribir(() =>
      post('/api/histories', {
        clientId: String(historia.clientId),
        doctorId: Number(doctorId),
        diagnosis: historia.diagnosis || azar(DIAGNOSTICOS),
        notes: historia.notes,
      }),
    )
    if (r.ok) cargarHistorias(historia.clientId)
  }

  // Recorre el flujo completo de una vez: médico, paciente e historia.
  const demo = () =>
    escribir(async () => {
      const m = await post('/api/doctors', { name: azar(NOMBRES_MEDICOS) })
      const p = await post('/api/clients', { name: azar(NOMBRES_PACIENTES) })
      if (!m.ok || !p.ok) return m.ok ? p : m
      const h = await post('/api/histories', {
        clientId: String(p.data.id),
        doctorId: m.data.id,
        diagnosis: azar(DIAGNOSTICOS),
        notes: 'Creada con la demo rápida',
      })
      setHistoria((s) => ({ ...s, clientId: String(p.data.id), doctorId: String(m.data.id) }))
      return h
    })

  const faltaHistoria = !historia.clientId || !historia.doctorId

  return (
    <Panel
      eyebrow="Todo pasa por el gateway :8080"
      title="Probar el flujo"
      aside={
        <button className="btn btn-primary" onClick={demo} disabled={ocupado}>
          Demo rápida: médico + paciente + historia
        </button>
      }
    >
      <div className="program-grid">
        <form className="program-card" onSubmit={crearMedico}>
          <span className="option-label">POST /api/doctors</span>
          <h4>Registrar médico</h4>
          <p>PostgreSQL · doctor-service (Spring)</p>
          <input value={medico} onChange={(e) => setMedico(e.target.value)} placeholder={NOMBRES_MEDICOS[0]} maxLength={120} />
          <button className="btn btn-primary" disabled={ocupado}>Registrar</button>
        </form>

        <form className="program-card" onSubmit={crearPaciente}>
          <span className="option-label">POST /api/clients</span>
          <h4>Registrar paciente</h4>
          <p>MySQL · client-service (Go)</p>
          <input value={paciente} onChange={(e) => setPaciente(e.target.value)} placeholder={NOMBRES_PACIENTES[0]} maxLength={120} />
          <button className="btn btn-primary" disabled={ocupado}>Registrar</button>
        </form>

        <form className="program-card" onSubmit={crearHistoria}>
          <span className="option-label">POST /api/histories</span>
          <h4>Abrir historia clínica</h4>
          <p>MongoDB · valida al médico antes de guardar</p>
          <select value={historia.clientId} onChange={(e) => setHistoria({ ...historia, clientId: e.target.value })}>
            <option value="">Paciente…</option>
            {pacientes.map((p) => <option key={p.id} value={p.id}>#{p.id} {p.name}</option>)}
          </select>
          <select value={historia.doctorId} onChange={(e) => setHistoria({ ...historia, doctorId: e.target.value })}>
            <option value="">Médico…</option>
            {medicos.map((m) => <option key={m.id} value={m.id}>#{m.id} {m.name}</option>)}
          </select>
          <input value={historia.diagnosis} onChange={(e) => setHistoria({ ...historia, diagnosis: e.target.value })} placeholder="Diagnóstico" maxLength={200} />
          <div className="row">
            <button className="btn btn-primary" disabled={ocupado || faltaHistoria}>Abrir</button>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={ocupado || !historia.clientId}
              title="Envía doctorId 999999 para ver la respuesta 400"
              onClick={(e) => crearHistoria(e, 999999)}
            >
              Médico inexistente
            </button>
          </div>
        </form>
      </div>

      {ultima && (
        <div className={`result-box ${ultima.ok ? 'ok' : 'error'}`}>
          <HttpStatus status={ultima.status} /> <span>{ultima.ms} ms</span>
          <pre>{JSON.stringify(ultima.data, null, 2)}</pre>
        </div>
      )}

      <div className="tables">
        <div>
          <h3>Médicos <small>{medicos.length}</small></h3>
          <ul className="table-list">
            {medicos.slice(-8).reverse().map((m) => <li key={m.id}><code>#{m.id}</code> {m.name}</li>)}
          </ul>
        </div>
        <div>
          <h3>Pacientes <small>{pacientes.length}</small></h3>
          <ul className="table-list">
            {pacientes.slice(-8).reverse().map((p) => <li key={p.id}><code>#{p.id}</code> {p.name}</li>)}
          </ul>
        </div>
        <div>
          <h3>Historias del paciente <small>{historia.clientId ? `#${historia.clientId}` : '—'}</small></h3>
          <ul className="table-list">
            {historias.slice(-8).reverse().map((h) => (
              <li key={h.id}>
                <code>médico #{h.doctorId}</code> {h.diagnosis}
                <small>{formatTime(h.createdAt)}</small>
              </li>
            ))}
            {historia.clientId && historias.length === 0 && <li className="muted">Sin historias.</li>}
          </ul>
        </div>
      </div>
    </Panel>
  )
}
