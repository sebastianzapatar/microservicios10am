// Diagrama horizontal de la arquitectura. Cada vez que cambia `pulso` (una
// petición de escritura), la animación recorre el camino de izquierda a derecha.

export default function Flujo({ pasos, pulso }) {
  return (
    <div className="flow" key={pulso} data-running={pulso > 0 || undefined}>
      {pasos.map((paso, i) =>
        paso.enlace ? (
          <div className="flow-link" key={i} style={{ '--i': i }}>
            <span>{paso.enlace}</span>
          </div>
        ) : (
          <div
            className={`flow-node${paso.destacado ? ' highlight' : ''}`}
            key={paso.id}
            style={{ '--i': i }}
          >
            <strong>{paso.nombre}</strong>
            <span>{paso.detalle}</span>
          </div>
        ),
      )}
    </div>
  )
}
