<template>
  <div class="step-container" :class="{ 'mobile-layout': isMobile }">
    <!-- 移动端：欢迎区 -->
    <div v-if="isMobile" class="mobile-welcome">
      <div class="welcome-content">
        <h2 class="welcome-title">开始扫描媒体文件</h2>
        <p class="welcome-subtitle">选择媒体文件目录，自动识别和整理</p>
      </div>
    </div>

    <!-- 桌面端：顶部工具栏 -->
    <div v-else class="toolbar">
      <div class="toolbar-title">
        <el-icon class="title-icon"><FolderOpened /></el-icon>
        <span>扫描媒体目录</span>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="content-wrapper">
      <!-- 目录输入选择组件 - 整合管理、输入、扫描功能 -->
      <div class="directory-selector">
        <div class="input-row">
          <!-- 管理按钮 -->
          <el-dropdown trigger="click" @command="handleManageCommand">
            <el-button class="manage-button">
              <el-icon><Setting /></el-icon>
              <span>管理</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="save-favorite" :disabled="!directoryPath">
                  <el-icon><StarFilled /></el-icon>
                  保存为常用
                </el-dropdown-item>
                <el-dropdown-item command="clear-history" divided>
                  <el-icon><Delete /></el-icon>
                  清空历史记录
                </el-dropdown-item>
                <el-dropdown-item command="manage-favorites">
                  <el-icon><FolderOpened /></el-icon>
                  管理常用目录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 输入框 + 下拉选择 -->
          <div class="input-wrapper" ref="inputWrapper">
            <input
              v-model="directoryPath"
              type="text"
              class="path-input"
              :class="{ 'is-focused': isInputFocused }"
              placeholder="请输入或粘贴媒体目录路径..."
              @focus="handleInputFocus"
              @blur="handleInputBlur"
            />
            <el-icon
              class="dropdown-icon"
              :class="{ 'is-active': showDropdown }"
              @click="toggleDropdown"
            >
              <ArrowDown />
            </el-icon>

            <!-- 下拉列表 -->
            <transition name="dropdown">
              <div v-show="showDropdown" class="dropdown-panel" ref="dropdownPanel">
                <!-- 搜索框 -->
                <div class="dropdown-search">
                  <el-icon><Search /></el-icon>
                  <input
                    v-model="searchQuery"
                    type="text"
                    placeholder="搜索路径..."
                    @click.stop
                  />
                </div>

                <!-- 常用目录 -->
                <div v-if="filteredFavorites.length > 0" class="dropdown-section">
                  <div class="section-header">
                    <el-icon><StarFilled /></el-icon>
                    <span>常用目录</span>
                  </div>
                  <div
                    v-for="dir in filteredFavorites"
                    :key="'fav-' + dir.id"
                    class="dropdown-item"
                    @click="selectDirectory(dir)"
                  >
                    <div class="item-content">
                      <el-icon class="item-icon"><Folder /></el-icon>
                      <div class="item-info">
                        <div class="item-name">{{ dir.name }}</div>
                        <div class="item-path">{{ dir.path }}</div>
                      </div>
                    </div>
                    <el-icon
                      class="item-action"
                      @click.stop="removeFavorite(dir.id)"
                      title="删除"
                    >
                      <Delete />
                    </el-icon>
                  </div>
                </div>

                <!-- 历史记录 -->
                <div v-if="filteredHistory.length > 0" class="dropdown-section">
                  <div class="section-header">
                    <el-icon><Clock /></el-icon>
                    <span>历史记录</span>
                  </div>
                  <div
                    v-for="item in filteredHistory"
                    :key="'hist-' + item.id"
                    class="dropdown-item"
                    @click="selectHistoryItem(item)"
                  >
                    <div class="item-content">
                      <el-icon class="item-icon"><Clock /></el-icon>
                      <div class="item-info">
                        <div class="item-path">{{ item.path }}</div>
                        <div class="item-time">{{ formatTime(item.timestamp) }}</div>
                      </div>
                    </div>
                    <el-icon
                      class="item-action star-action"
                      @click.stop="addToFavorites(item)"
                      title="保存为常用"
                    >
                      <Star />
                    </el-icon>
                  </div>
                </div>

                <!-- 空状态 -->
                <div v-if="filteredFavorites.length === 0 && filteredHistory.length === 0" class="dropdown-empty">
                  <el-icon><FolderOpened /></el-icon>
                  <p>{{ searchQuery ? '未找到匹配的路径' : '暂无记录' }}</p>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>



    <!-- 扫描加载状态 -->
    <div v-if="scanning" class="loading-container">
      <div class="scanning-animation">
        <el-icon class="folder-icon bounce-icon" :size="48"><Folder /></el-icon>
        <div class="scanning-dots">
          <span>正在扫描文件</span>
          <div class="dots-animation">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!scanning && videoFiles.length === 0 && directoryPath" class="empty-state">
      <el-icon class="empty-icon" :size="64"><VideoCamera /></el-icon>
      <h3>未找到视频文件</h3>
      <p>该目录下没有发现支持的视频格式文件<br/>支持格式: mp4, mkv, avi, mov, wmv</p>
    </div>

    <!-- 成功状态 -->
    <div v-if="videoFiles.length > 0" class="success-container">
      <el-alert
        :title="`扫描完成! 共找到 ${videoFiles.length} 个视频文件`"
        type="success"
        show-icon
        :closable="false"
        class="success-alert pulse"
      />
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderOpened,
  Folder,
  VideoCamera,
  Setting,
  StarFilled,
  Star,
  Delete,
  Clock,
  ArrowDown,
  Search
} from '@element-plus/icons-vue'
import { scanDirectory, recordUsage } from '@/api/media'
import { useMobileDetection } from '@/composables/useMobileDetection'

// 移动端检测
const { isMobile } = useMobileDetection()

// Props
const props = defineProps({
  savedDirectories: {
    type: Array,
    default: () => []
  },
  videoFiles: {
    type: Array,
    default: () => []
  }
})

// Emits
const emit = defineEmits([
  'update:videoFiles',
  'update:scanRootPath',
  'update:canNext',
  'update:loading',
  'show-directory-manager'
])

// 数据
const directoryPath = ref('')
const scanning = ref(false)

// 下拉列表相关
const showDropdown = ref(false)
const searchQuery = ref('')
const isInputFocused = ref(false)
const inputWrapper = ref(null)
const dropdownPanel = ref(null)

// 历史记录 (从 localStorage 读取)
const historyItems = ref([])
const MAX_HISTORY = 10

// 常用目录 (从 props.savedDirectories 映射)
const favoriteDirectories = computed(() => {
  return props.savedDirectories || []
})

// 过滤后的常用目录
const filteredFavorites = computed(() => {
  if (!searchQuery.value) return favoriteDirectories.value
  const query = searchQuery.value.toLowerCase()
  return favoriteDirectories.value.filter(dir =>
    dir.name.toLowerCase().includes(query) ||
    dir.path.toLowerCase().includes(query)
  )
})

// 过滤后的历史记录
const filteredHistory = computed(() => {
  if (!searchQuery.value) return historyItems.value.slice(0, 5)
  const query = searchQuery.value.toLowerCase()
  return historyItems.value
    .filter(item => item.path.toLowerCase().includes(query))
    .slice(0, 5)
})

// 监听扫描状态和文件列表，更新父组件状态
watch(scanning, (val) => {
  emit('update:loading', val)
})

watch(() => props.videoFiles, (files) => {
  emit('update:canNext', files.length > 0)
}, { immediate: true })

// 加载历史记录
const loadHistory = () => {
  const stored = localStorage.getItem('scan-history')
  if (stored) {
    try {
      historyItems.value = JSON.parse(stored)
    } catch (e) {
      console.error('解析历史记录失败:', e)
      historyItems.value = []
    }
  }
}

// 保存历史记录
const saveHistory = () => {
  localStorage.setItem('scan-history', JSON.stringify(historyItems.value))
}

// 添加到历史记录
const addToHistory = (path) => {
  // 移除重复项
  historyItems.value = historyItems.value.filter(item => item.path !== path)
  // 添加到开头
  historyItems.value.unshift({
    id: Date.now(),
    path,
    timestamp: Date.now()
  })
  // 限制数量
  if (historyItems.value.length > MAX_HISTORY) {
    historyItems.value = historyItems.value.slice(0, MAX_HISTORY)
  }
  saveHistory()
}

// 输入框焦点处理
const handleInputFocus = () => {
  isInputFocused.value = true
  showDropdown.value = true
}

const handleInputBlur = () => {
  isInputFocused.value = false
  // 延迟关闭,确保点击下拉项能生效
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

// 切换下拉列表
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

// 选择目录
const selectDirectory = async (dir) => {
  directoryPath.value = dir.path
  showDropdown.value = false
  searchQuery.value = ''

  // 记录使用次数
  try {
    await recordUsage(dir.id)
  } catch (error) {
    console.error('记录使用次数失败:', error)
  }
}

// 选择历史记录
const selectHistoryItem = (item) => {
  directoryPath.value = item.path
  showDropdown.value = false
  searchQuery.value = ''
}

// 添加到常用
const addToFavorites = async (item) => {
  // 检查是否已存在
  const exists = favoriteDirectories.value.some(dir => dir.path === item.path)
  if (exists) {
    ElMessage.warning('该路径已在常用目录中')
    return
  }

  // 显示对话框让用户输入名称
  try {
    const { value: name } = await ElMessageBox.prompt('请输入目录名称', '保存为常用', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: item.path.split('/').pop() || '未命名'
    })

    if (name) {
      emit('show-directory-manager', { action: 'add', path: item.path, name })
    }
  } catch {
    // 用户取消
  }
}

// 移除常用
const removeFavorite = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个常用目录吗?', '确认删除', {
      type: 'warning'
    })
    emit('show-directory-manager', { action: 'delete', id })
  } catch {
    // 用户取消
  }
}

// 格式化时间
const formatTime = (timestamp) => {
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 管理命令处理
const handleManageCommand = async (command) => {
  switch (command) {
    case 'save-favorite':
      if (!directoryPath.value) {
        ElMessage.warning('请先输入目录路径')
        return
      }

      // 检查是否已存在
      const exists = favoriteDirectories.value.some(dir => dir.path === directoryPath.value)
      if (exists) {
        ElMessage.warning('该路径已在常用目录中')
        return
      }

      try {
        const { value: name } = await ElMessageBox.prompt('请输入目录名称', '保存为常用', {
          confirmButtonText: '保存',
          cancelButtonText: '取消',
          inputValue: directoryPath.value.split('/').pop() || '未命名'
        })

        if (name) {
          emit('show-directory-manager', { action: 'add', path: directoryPath.value, name })
          ElMessage.success('已保存到常用目录')
        }
      } catch {
        // 用户取消
      }
      break

    case 'clear-history':
      try {
        await ElMessageBox.confirm('确定要清空所有历史记录吗?', '确认清空', {
          type: 'warning'
        })
        historyItems.value = []
        saveHistory()
        ElMessage.success('已清空历史记录')
      } catch {
        // 用户取消
      }
      break

    case 'manage-favorites':
      emit('show-directory-manager')
      break
  }
}



// 扫描目录 - 暴露给父组件调用
const executeScan = async () => {
  if (!directoryPath.value) {
    ElMessage.warning('请输入目录路径')
    return false
  }

  scanning.value = true
  try {
    const res = await scanDirectory(directoryPath.value)
    emit('update:videoFiles', res.data)
    emit('update:scanRootPath', directoryPath.value)

    // 添加到历史记录
    addToHistory(directoryPath.value)

    // 根据结果显示不同消息
    if (res.data.length > 0) {
      ElMessage.success(`扫描完成! 找到 ${res.data.length} 个视频文件`)
      return true // 扫描成功
    } else {
      ElMessage.warning('未找到视频文件,请检查路径是否正确')
      return false // 扫描失败
    }
  } catch (error) {
    ElMessage.error('扫描失败: ' + error.message)
    return false // 扫描失败
  } finally {
    scanning.value = false
  }
}

// 暴露方法给父组件
defineExpose({
  executeScan
})

// 点击外部关闭下拉列表
const handleClickOutside = (e) => {
  if (inputWrapper.value && !inputWrapper.value.contains(e.target)) {
    showDropdown.value = false
  }
}

// 生命周期
onMounted(() => {
  loadHistory()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* 步骤容器 */
.step-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--app-bg);
}

/* 顶部工具栏 */
.toolbar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg) var(--space-xl);
  background-color: var(--surface-1);
  border-bottom: 1px solid var(--border-subtle);
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
}

.title-icon {
  color: var(--primary-brand);
  font-size: 22px;
}

/* 主内容区域 */
.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xl);
}

/* ==================== 目录输入选择组件 ==================== */
.directory-selector {
  margin-bottom: var(--space-xl);
}

.input-row {
  display: flex;
  gap: var(--space-md);
  align-items: stretch;
}

/* 管理按钮 */
.manage-button {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  background: var(--surface-1);
  color: var(--text-secondary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.manage-button:hover {
  background: var(--surface-2);
  border-color: var(--border-focus);
  color: var(--text-main);
}

/* 输入框容器 */
.input-wrapper {
  position: relative;
  flex: 1;
}

/* 输入框 */
.path-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  padding-right: 36px; /* 为下拉图标留空间 */
  font-size: 14px;
  line-height: 20px;
  color: var(--text-main);
  background: var(--surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  outline: none;
  transition: all var(--transition-base);
  font-family: var(--font-sans);
}

.path-input::placeholder {
  color: var(--text-tertiary);
}

.path-input:hover {
  border-color: var(--border-focus);
}

.path-input:focus,
.path-input.is-focused {
  border-color: var(--primary-brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

/* 下拉图标 */
.dropdown-icon {
  position: absolute;
  right: var(--space-md);
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.dropdown-icon:hover {
  color: var(--primary-brand);
}

.dropdown-icon.is-active {
  transform: translateY(-50%) rotate(180deg);
  color: var(--primary-brand);
}

/* 下拉面板 */
.dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 400px;
  overflow-y: auto;
  background: var(--surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-float);
  z-index: 1000;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all var(--transition-fast);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 下拉搜索框 */
.dropdown-search {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  border-bottom: 1px solid var(--border-subtle);
  background: var(--surface-2);
}

.dropdown-search .el-icon {
  color: var(--text-tertiary);
  font-size: 16px;
}

.dropdown-search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-main);
}

.dropdown-search input::placeholder {
  color: var(--text-tertiary);
}

/* 下拉分组 */
.dropdown-section {
  padding: var(--space-sm) 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-header .el-icon {
  font-size: 14px;
}

/* 下拉项 */
.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.dropdown-item:hover {
  background: var(--surface-2);
}

.item-content {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex: 1;
  min-width: 0; /* 允许文字截断 */
}

.item-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--primary-brand);
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-path {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.item-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.item-action {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--text-tertiary);
  opacity: 0;
  transition: all var(--transition-fast);
}

.dropdown-item:hover .item-action {
  opacity: 1;
}

.item-action:hover {
  color: var(--danger-text);
}

.star-action:hover {
  color: var(--warning-text);
}

/* 下拉空状态 */
.dropdown-empty {
  padding: var(--space-2xl) var(--space-md);
  text-align: center;
  color: var(--text-tertiary);
}

.dropdown-empty .el-icon {
  font-size: 48px;
  margin-bottom: var(--space-md);
  opacity: 0.5;
}

.dropdown-empty p {
  font-size: 14px;
  margin: 0;
}

/* 扫描按钮 */
.scan-button {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-xl);
  background: var(--primary-brand);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-base);
}

.scan-button:hover:not(:disabled) {
  background: var(--primary-hover);
  box-shadow: var(--shadow-hover);
}

.scan-button:active:not(:disabled) {
  transform: translateY(1px);
}



/* ==================== 加载状态 ==================== */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--space-2xl) 0;
}

.scanning-animation {
  text-align: center;
}

.folder-icon {
  font-size: 48px;
  margin-bottom: var(--space-lg);
  color: var(--primary-brand);
  animation: bounce 1s ease-in-out infinite;
}

.scanning-dots {
  font-size: 16px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* ==================== 空状态 ==================== */
.empty-state {
  text-align: center;
  padding: var(--space-2xl) 0;
  color: var(--text-secondary);
}

.empty-icon {
  margin-bottom: var(--space-lg);
  opacity: 0.6;
  color: var(--text-tertiary);
}

.empty-state h3 {
  margin: var(--space-lg) 0 var(--space-sm);
  font-size: 18px;
  color: var(--text-main);
}

.empty-state p {
  line-height: 1.6;
  color: var(--text-secondary);
}

/* ==================== 成功状态 ==================== */
.success-container {
  margin-top: var(--space-xl);
}

.success-alert {
  border-radius: var(--radius-md);
}

/* ==================== 动画 ==================== */
@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  /* 移动端布局 */
  .step-container.mobile-layout {
    padding: 0;
  }

  /* 移动端欢迎区 */
  .mobile-welcome {
    padding: var(--mobile-section-gap) var(--mobile-gutter);
    background: linear-gradient(135deg, var(--primary-brand) 0%, var(--primary-hover) 100%);
    color: white;
    text-align: center;
  }

  .welcome-content {
    max-width: 400px;
    margin: 0 auto;
  }

  .welcome-title {
    font-size: var(--mobile-font-xl);
    font-weight: 600;
    margin: 0 0 var(--space-sm);
    line-height: var(--mobile-line-height-tight);
  }

  .welcome-subtitle {
    font-size: var(--mobile-font-sm);
    margin: 0;
    opacity: 0.9;
    line-height: var(--mobile-line-height-normal);
  }

  /* 内容区域 */
  .content-wrapper {
    padding: var(--mobile-gutter);
  }

  /* 目录选择器 */
  .directory-selector {
    background: var(--surface-1);
    border-radius: var(--radius-md);
    padding: var(--space-lg);
    box-shadow: var(--shadow-card);
    margin-bottom: var(--mobile-card-gap);
  }

  .input-row {
    flex-direction: column;
    gap: var(--space-md);
  }

  .manage-button,
  .scan-button {
    width: 100%;
    justify-content: center;
    min-height: var(--touch-target-min);
    font-size: var(--mobile-font-base);
  }

  /* 输入框优化 */
  .input-wrapper {
    width: 100%;
  }

  .path-input {
    font-size: 16px; /* 防止 iOS 自动缩放 */
    padding: 12px 40px 12px 16px;
    min-height: var(--touch-target-min);
  }

  /* 下拉面板 */
  .dropdown-panel {
    max-height: 60vh;
  }

  .dropdown-item {
    padding: var(--space-md);
    min-height: var(--touch-target-comfortable);
  }

  .item-name,
  .item-path {
    font-size: var(--mobile-font-sm);
  }

  .item-time {
    font-size: var(--mobile-font-xs);
  }

  /* 加载状态 */
  .loading-container {
    padding: var(--mobile-section-gap) var(--mobile-gutter);
  }

  .scanning-animation {
    text-align: center;
  }

  .folder-icon {
    font-size: 64px;
  }

  .scanning-dots {
    font-size: var(--mobile-font-base);
  }

  /* 空状态 */
  .empty-state {
    padding: var(--mobile-section-gap) var(--mobile-gutter);
  }

  .empty-icon {
    font-size: 48px;
  }

  .empty-state h3 {
    font-size: var(--mobile-font-lg);
  }

  .empty-state p {
    font-size: var(--mobile-font-sm);
  }

  /* 成功状态 */
  .success-container {
    margin: var(--mobile-gutter);
  }

  .success-alert {
    border-radius: var(--radius-md);
    font-size: var(--mobile-font-sm);
  }
}

/* 小屏手机特殊优化 */
@media (max-width: 480px) {
  .mobile-welcome {
    padding: var(--space-xl) var(--mobile-gutter);
  }

  .welcome-title {
    font-size: var(--mobile-font-lg);
  }

  .welcome-subtitle {
    font-size: var(--mobile-font-xs);
  }

  .directory-selector {
    padding: var(--space-md);
  }

  .folder-icon {
    font-size: 48px;
  }
}
</style>
