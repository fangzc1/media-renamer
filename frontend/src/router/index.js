import { createRouter, createWebHistory } from 'vue-router'
import FileManager from '../views/FileManager.vue'

const routes = [
  {
    path: '/',
    name: 'FileManager',
    component: FileManager
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
