<template>
  <div class="step-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-title">
        <el-icon class="title-icon"><CircleCheck /></el-icon>
        <span>执行重命名</span>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="content-wrapper">

    <!-- 确认执行 -->
    <el-result
      v-if="!renaming && !renameComplete"
      icon="warning"
      title="确认执行重命名"
      sub-title="重命名操作不可撤销,请确认无误后点击右侧的执行按钮"
      class="confirm-result"
    >
    </el-result>

    <!-- 执行进度 -->
    <div v-if="renaming" class="rename-progress-container">
      <div class="rename-animation">
        <el-icon class="magic-wand bounce-icon" :size="48"><Loading /></el-icon>
        <div class="progress-info">
          <h3>正在全速处理文件...</h3>
          <p>处理中...</p>
        </div>
      </div>
      <el-progress
        :percentage="renameProgress"
        :status="renameProgress === 100 ? 'success' : undefined"
        :stroke-width="12"
        class="rename-progress"
      />
    </div>

    <!-- 完成状态 -->
    <div v-if="renameComplete" class="completion-container">
      <el-result class="success-result">
        <!-- 自定义图标插槽 -->
        <template #icon>
          <div class="result-icon-wrapper">
            <!-- 核心图标 -->
            <el-icon class="success-icon" :size="72"><SuccessFilled /></el-icon>

            <!-- 装饰性彩带 (绝对定位在图标周围) -->
            <div class="confetti-wrapper">
              <div class="confetti-piece" v-for="n in 12" :key="n"></div>
            </div>
          </div>
        </template>

        <template #title>
          <span class="result-title">重命名完成</span>
        </template>

        <template #sub-title>
          <div class="result-stats">
            <span class="stat-item success">
              成功 <span class="count">{{ successCount }}</span>
            </span>
            <span v-if="failedCount > 0" class="stat-divider">|</span>
            <span v-if="failedCount > 0" class="stat-item error">
              失败 <span class="count">{{ failedCount }}</span>
            </span>
          </div>
        </template>
      </el-result>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { CircleCheck, Loading, SuccessFilled, RefreshRight } from '@element-plus/icons-vue'

// Props
const props = defineProps({
  renaming: {
    type: Boolean,
    default: false
  },
  renameComplete: {
    type: Boolean,
    default: false
  },
  renamedCount: {
    type: Number,
    default: 0
  },
  renamePreviews: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits([
  'update:canNext',
  'update:loading'
])

// 监听状态变化，通知父组件
watch(() => props.renaming, (val) => {
  emit('update:loading', val)
})

watch(() => props.renameComplete, (val) => {
  // 完成后允许重新开始
  emit('update:canNext', true)
}, { immediate: true })

watch(() => props.renamePreviews, (previews) => {
  // 有预览数据时允许执行
  if (!props.renameComplete) {
    emit('update:canNext', previews.length > 0)
  }
}, { immediate: true })

// 计算属性
const renameProgress = computed(() => {
  if (props.renamePreviews.length === 0) return 0
  return Math.round((props.renamedCount / props.renamePreviews.length) * 100)
})

const successCount = computed(() => {
  return props.renamePreviews.filter(p => p.status === 'success').length
})

const failedCount = computed(() => {
  return props.renamePreviews.filter(p => p.status === 'failed').length
})
</script>

<style scoped>
/* 步骤容器 - 填满父容器 */
.step-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--bg-color);
}

/* 顶部工具栏 */
.toolbar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  background-color: var(--bg-color);
  border-bottom: 1px solid var(--border-color);
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.title-icon {
  color: var(--primary-color);
  font-size: 22px;
}

/* 主内容区域 */
.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

/* 按钮样式 */
.execute-button {
  transition: all var(--transition-base);
}

.confirm-result {
  padding: var(--spacing-xl) 0;
}

.prev-button {
  border-radius: var(--border-radius-sm);
  margin-right: var(--spacing-md);
}

/* 重命名执行状态 */
.rename-progress-container {
  text-align: center;
  padding: 40px 0;
}

.rename-animation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-2xl);
}

.magic-wand {
  color: var(--primary-color);
}

.progress-info h3 {
  margin: 0 0 var(--spacing-sm);
  color: var(--primary-color);
}

.progress-info p {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-secondary);
}

.rename-progress {
  max-width: 500px;
  margin: 0 auto;
}

/* 完成庆祝动画 */
.completion-container {
  text-align: center;
  position: relative;
}

/* 图标容器 */
.result-icon-wrapper {
  position: relative;
  display: inline-block;
  padding: var(--spacing-lg);
}

/* 核心图标样式 */
.success-icon {
  color: var(--success-color);
  animation: tada 1s ease-in-out;
  filter: drop-shadow(0 0 20px rgba(103, 194, 58, 0.3));
}

@keyframes tada {
  0% { transform: scale(1); }
  10%, 20% { transform: scale(0.9) rotate(-3deg); }
  30%, 50%, 70%, 90% { transform: scale(1.1) rotate(3deg); }
  40%, 60%, 80% { transform: scale(1.1) rotate(-3deg); }
  100% { transform: scale(1) rotate(0); }
}

/* 彩带容器 */
.confetti-wrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 150px;
  height: 150px;
  pointer-events: none;
}

.confetti-piece {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: confetti-burst 2s ease-out infinite;
  top: 50%;
  left: 50%;
}

/* 12 个彩带粒子的样式和动画 */
.confetti-piece:nth-child(1) {
  background: #ff6b6b;
  animation-delay: 0s;
  --angle: 30deg;
}

.confetti-piece:nth-child(2) {
  background: #4ecdc4;
  animation-delay: 0.1s;
  --angle: 60deg;
}

.confetti-piece:nth-child(3) {
  background: #45b7d1;
  animation-delay: 0.2s;
  --angle: 90deg;
}

.confetti-piece:nth-child(4) {
  background: #f9ca24;
  animation-delay: 0.3s;
  --angle: 120deg;
}

.confetti-piece:nth-child(5) {
  background: #6c5ce7;
  animation-delay: 0.4s;
  --angle: 150deg;
}

.confetti-piece:nth-child(6) {
  background: #a29bfe;
  animation-delay: 0.5s;
  --angle: 180deg;
}

.confetti-piece:nth-child(7) {
  background: #ff6b6b;
  animation-delay: 0.6s;
  --angle: 210deg;
}

.confetti-piece:nth-child(8) {
  background: #4ecdc4;
  animation-delay: 0.7s;
  --angle: 240deg;
}

.confetti-piece:nth-child(9) {
  background: #45b7d1;
  animation-delay: 0.8s;
  --angle: 270deg;
}

.confetti-piece:nth-child(10) {
  background: #f9ca24;
  animation-delay: 0.9s;
  --angle: 300deg;
}

.confetti-piece:nth-child(11) {
  background: #6c5ce7;
  animation-delay: 1s;
  --angle: 330deg;
}

.confetti-piece:nth-child(12) {
  background: #a29bfe;
  animation-delay: 1.1s;
  --angle: 360deg;
}

@keyframes confetti-burst {
  0% {
    transform: translate(-50%, -50%) rotate(var(--angle)) translateY(0) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) rotate(var(--angle)) translateY(-80px) scale(0.5);
    opacity: 0;
  }
}

/* 结果标题 */
.result-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-color-primary);
}

/* 统计信息样式 */
.result-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  margin-top: var(--spacing-md);
  font-size: 16px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.stat-item.success {
  color: var(--text-color-secondary);
}

.stat-item.error {
  color: var(--text-color-secondary);
}

.stat-item .count {
  font-size: 24px;
  font-weight: 700;
  margin-left: var(--spacing-xs);
}

.stat-item.success .count {
  color: var(--success-color);
}

.stat-item.error .count {
  color: var(--danger-color);
}

.stat-divider {
  color: var(--border-color);
  font-size: 20px;
}

.success-result {
  margin-top: var(--spacing-xl);
}

.reset-button {
  font-size: 16px;
  padding: var(--spacing-md) var(--spacing-xl);
  border-radius: var(--border-radius-md);
  display: inline-flex;
  align-items: center;
}
</style>
