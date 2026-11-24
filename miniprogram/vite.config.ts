import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const apiBaseUrl = env.VITE_API_BASE_URL
  if (!apiBaseUrl) {
    throw new Error(`缺少 VITE_API_BASE_URL，请配置 .env.${mode}`)
  }
  if (mode === 'production') {
    const url = new URL(apiBaseUrl)
    if (url.protocol !== 'https:' || !url.hostname.includes('.') ||
        /^\d+\.\d+\.\d+\.\d+$/.test(url.hostname) || url.hostname === 'localhost' ||
        url.port || url.pathname !== '/api') {
      throw new Error('正式环境 VITE_API_BASE_URL 须为 https://已配置的域名/api，不含端口')
    }
  }
  return {
    plugins: [uni()],
    server: { port: 3000 }
  }
})
