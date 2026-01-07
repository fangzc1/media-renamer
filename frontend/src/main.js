import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css' // Element Plus 深色模式支持
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 引入全局样式
import './styles/design-tokens.css' // 设计 token（需要最先导入）
import './styles/variables.css'
import './styles/animations.css'
import './styles/card.css'
import './styles/mobile.css' // 移动端样式

const app = createApp(App)
const pinia = createPinia()

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
