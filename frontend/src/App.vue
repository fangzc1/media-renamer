<template>
  <div id="app">
    <el-container class="app-container">
      <!-- 顶部精简 Header -->
      <el-header class="compact-header" height="50px">
        <div class="header-content">
          <div class="header-left">
            <img :src="logoSrc" alt="Media Renamer" class="logo" />
            <h1>Media Renamer</h1>
          </div>
          <ThemeSwitch />
        </div>
      </el-header>

      <el-container class="main-container">
        <!-- 全屏内容区 -->
        <el-main class="content-main">
          <router-view @update:current-step="currentStep = $event" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, provide, computed, onMounted } from 'vue'
import ThemeSwitch from '@/components/ThemeSwitch.vue'

// 全局步骤状态
const currentStep = ref(0)

// 提供给子组件
provide('currentStep', currentStep)

// 主题状态
const isDark = ref(false)

// 根据主题选择 Logo
// 浅色模式下 Header 是蓝色背景，使用暖橙黄配色 Logo（高对比度）
// 深色模式下使用深色版本的 Logo
const logoSrc = computed(() => {
  return isDark.value ? '/logo-dark.svg' : '/logo-warm.svg'
})

// 监听主题变化
onMounted(() => {
  // 初始化主题
  const theme = localStorage.getItem('theme') || 'light'
  isDark.value = theme === 'dark'

  // 监听主题变化
  const observer = new MutationObserver(() => {
    const theme = document.documentElement.getAttribute('data-theme')
    isDark.value = theme === 'dark'
  })

  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme']
  })
})
</script>

<style>
/* 全局重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  overflow: hidden;
}

/* 应用容器 - 填满视口 */
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 精简 Header - 压缩高度到 50px */
.compact-header {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-color-light) 100%);
  color: white;
  display: flex;
  align-items: center;
  box-shadow: var(--shadow-sm);
  transition: background var(--transition-base);
  height: 50px !important;
  flex-shrink: 0;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--spacing-lg);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left .logo {
  height: 36px;
  width: auto;
  transition: transform 0.3s ease;
}

.header-left .logo:hover {
  transform: scale(1.05);
}

.header-content h1 {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* 主容器 - 占据剩余空间 */
.main-container {
  flex: 1;
  overflow: hidden;
}

/* 内容区 - 无 padding,填满空间 */
.content-main {
  background-color: var(--bg-color);
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: background-color var(--transition-base);
}

/* 深色模式 */
[data-theme="dark"] .compact-header {
  background: linear-gradient(135deg, var(--primary-color-dark) 0%, var(--primary-color) 100%);
}
</style>
