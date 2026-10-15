import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Las llamadas a /api se redirigen al backend Spring Boot, así el navegador
// las ve como del mismo origen y no hace falta configurar CORS.
// API_URL permite apuntar a un backend en otro puerto (p. ej. si el 8080 está ocupado).
const apiProxy = {
  '/api': process.env.API_URL ?? 'http://localhost:8080',
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: { proxy: apiProxy },
  preview: { proxy: apiProxy },
})
