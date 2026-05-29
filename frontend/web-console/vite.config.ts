import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // Local browser verification routes project management to the project service.
      '/api/projects': {
        target: 'http://127.0.0.1:18081',
        changeOrigin: true,
      },
      // Local browser verification routes query requests straight to the context service.
      '/api/query': {
        target: 'http://127.0.0.1:18082',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
