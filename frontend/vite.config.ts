import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

declare const process: { env: { VITE_API_PROXY?: string } }

const apiProxy = process.env.VITE_API_PROXY || 'http://localhost:42180'

export default defineConfig({
  plugins: [vue()],
  server: {
    allowedHosts: true,
    port: 5173,
    proxy: {
      '/api': apiProxy
    }
  }
})
