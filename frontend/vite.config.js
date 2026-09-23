import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo (npm run dev) se replican los mismos proxies que hace nginx en
// el contenedor, apuntando a los puertos que Compose publica en el host.
// La arquitectura se elige con VITE_ARQUITECTURA=rest|rabbitmq|kafka.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
      '/registry': {
        target: 'http://localhost:8762',
        rewrite: (path) => path.replace(/^\/registry/, '/eureka/apps'),
        headers: { Accept: 'application/json' },
      },
      '/broker/rabbitmq': {
        target: 'http://localhost:15672',
        rewrite: (path) => path.replace(/^\/broker\/rabbitmq/, '/api'),
        auth: 'hospital:hospital',
      },
      '/broker/kafka': {
        target: 'http://localhost:8090',
        rewrite: (path) => path.replace(/^\/broker\/kafka/, '/api'),
      },
    },
  },
})
