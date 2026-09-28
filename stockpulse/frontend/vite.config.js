import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The console calls /api/*; Vite forwards it to Spring Boot so no CORS setup is needed in dev.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
