import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// La SPA solo habla con el API Gateway (nunca con un microservicio directo).
// En desarrollo, Vite reenvía /api al gateway, así no hace falta configurar CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/docs': 'http://localhost:8080',
    },
  },
})
