// Cliente HTTP mínimo. Todas las rutas son relativas: en el contenedor las
// resuelve nginx y en desarrollo el proxy de Vite.

const listeners = new Set()

/** Suscribe una función a cada petición registrada; devuelve la baja. */
export function onRequest(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

let nextId = 1

/**
 * Hace la petición y mide la latencia. Nunca lanza: los fallos de red vuelven
 * como status 0 para que la interfaz los muestre igual que un 4xx o 5xx.
 * Con log=false (sondeos periódicos) no aparece en el registro de peticiones.
 */
export async function request(method, path, body, { log = true } = {}) {
  const started = performance.now()
  let result
  try {
    const res = await fetch(path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    })
    const text = await res.text()
    let data = null
    try {
      data = text ? JSON.parse(text) : null
    } catch {
      data = text
    }
    result = { ok: res.ok, status: res.status, data }
  } catch (error) {
    result = { ok: false, status: 0, data: { message: error.message } }
  }
  result.ms = Math.round(performance.now() - started)

  if (log) {
    const entry = { id: nextId++, method, path, body, at: new Date(), ...result }
    listeners.forEach((listener) => listener(entry))
  }
  return result
}

export const get = (path, options) => request('GET', path, undefined, options)
export const post = (path, body) => request('POST', path, body)
export const put = (path, body) => request('PUT', path, body)

/** Arquitectura activa: la fija Compose en /config.json; en desarrollo, Vite. */
export async function loadArquitectura() {
  const res = await get('/config.json', { log: false })
  const value = res.ok && res.data?.arquitectura
  return value || import.meta.env.VITE_ARQUITECTURA || 'rest'
}
