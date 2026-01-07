<template>
  <el-switch
    v-model="isDark"
    :active-action-icon="Moon"
    :inactive-action-icon="Sunny"
    inline-prompt
    class="theme-switch"
    @change="toggleTheme"
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'

// 当前主题状态
const isDark = ref(false)

// 初始化主题
onMounted(() => {
  // 从 localStorage 读取保存的主题
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme) {
    isDark.value = savedTheme === 'dark'
    applyTheme(savedTheme)
  } else {
    // 检测系统主题偏好
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    isDark.value = prefersDark
    applyTheme(prefersDark ? 'dark' : 'light')
  }

  // 监听系统主题变化
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    if (!localStorage.getItem('theme')) {
      isDark.value = e.matches
      applyTheme(e.matches ? 'dark' : 'light')
    }
  })
})

// 切换主题
const toggleTheme = (value) => {
  const theme = value ? 'dark' : 'light'
  applyTheme(theme)
  localStorage.setItem('theme', theme)
}

// 应用主题
const applyTheme = (theme) => {
  document.documentElement.setAttribute('data-theme', theme)

  // Element Plus 深色模式支持
  if (theme === 'dark') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}
</script>

<style scoped>
.theme-switch {
  --el-switch-on-color: var(--primary-color);
  --el-switch-off-color: #dcdfe6;
}
</style>
