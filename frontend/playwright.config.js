import { defineConfig } from '@playwright/test'
import process from 'node:process'
export default defineConfig({
    testDir: './tests/e2e', workers: 1, timeout: 30000,
    use: { baseURL: 'http://127.0.0.1:5175', channel: process.env.E2E_BROWSER || 'chrome', trace: 'off' },
    webServer: { command: 'npm run dev -- --host 127.0.0.1 --port 5175 --strictPort', url: 'http://127.0.0.1:5175',
        reuseExistingServer: false, env: { API_PROXY: 'http://127.0.0.1:18081' } },
})
