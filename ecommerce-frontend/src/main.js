import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)  // 创建Vue应用
const pinia = createPinia() // 创建Pinia实例，作为全局状态管理工具

/*
把 Element Plus 的所有图标全局注册，
这样在任何 Vue 文件里都能直接用 <el-icon><Search /></el-icon>，
不用每次 import 需要的图标了。
*/
// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)         // 安装 Pinia（状态管理）
app.use(router)        // 安装路由
app.use(ElementPlus)   // 安装 UI 组件库
app.mount('#app')      // 挂载到 index.html 里的 <div id="app">
