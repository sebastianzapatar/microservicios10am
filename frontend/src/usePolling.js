import { useEffect, useRef, useState } from 'react'

/**
 * Ejecuta fn() al montar y luego cada `ms` milisegundos. Devuelve la última
 * respuesta de request() (o null mientras llega la primera).
 */
export function usePolling(fn, ms) {
  const [result, setResult] = useState(null)
  const fnRef = useRef(fn)
  fnRef.current = fn

  useEffect(() => {
    let alive = true
    const tick = async () => {
      const value = await fnRef.current()
      if (alive) setResult(value)
    }
    tick()
    const id = setInterval(tick, ms)
    return () => {
      alive = false
      clearInterval(id)
    }
  }, [ms])

  return result
}
