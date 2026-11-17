import { defineConfig, loadEnv } from 'vite'
import process from 'node:process'
import react from '@vitejs/plugin-react'

export default defineConfig(({ command, mode }) => {
  const enderecoApi = loadEnv(mode, process.cwd(), 'VITE_').VITE_API_URL || '/api'
  if (command === 'build' && enderecoApi !== '/api') {
    let url
    try { url = new URL(enderecoApi) } catch { throw new Error('VITE_API_URL deve ser /api ou uma URL HTTPS da API publicada.') }
    if (url.protocol !== 'https:' || ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname) || url.username || url.password || url.search || url.hash || url.pathname !== '/api') {
      throw new Error('Build de produção exige /api ou HTTPS público terminando em /api; localhost não é permitido.')
    }
  }
  return {
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    proxy: {
      '/api': {
        target: process.env.API_PROXY || 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  }
})
