import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Las llamadas a /api se redirigen al backend Spring Boot, así el navegador
// las ve como del mismo origen y no hace falta configurar CORS.
const apiProxy = {
  '/api': 'http://localhost:8080',
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: { proxy: apiProxy },
  preview: { proxy: apiProxy },
})
