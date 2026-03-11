import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)) 
      // @/api/user 代替 ../../api/user，避免写一堆 ../../../
    }
  },
  server: {
    port: 3000,// 前端跑在 3000 端口
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 所有 /api 开头的请求，转发到后端 8080
        changeOrigin: true
      }
    }
  }
})
/*
浏览器有同源策略，直接跨端口请求会被拦截（CORS 错误）。
解决方案：前端发请求到自己（/api/xxx），Vite 开发服务器悄悄把它转发给后端。浏览器以为一直在和自己说话，不会报错。
*/