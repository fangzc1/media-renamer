<template>
  <div class="step-container">
    <!-- 1. 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="toolbar-title">
          <el-icon class="title-icon"><View /></el-icon>
          <span class="title-text">预览重命名</span>
        </div>
        <!-- 预览总数标签 -->
        <div v-if="renamePreviews.length > 0 && !generatingPreview" class="preview-badge">
          <el-tag type="info" size="small" effect="plain" round>
            {{ isMobile ? `${renamePreviews.length} 个` : `共 ${renamePreviews.length} 个文件` }}
          </el-tag>
        </div>
      </div>

      <div class="toolbar-actions">
        <!-- 移动端：切换图标按钮 -->
        <el-button
          v-if="groupedPreviews.length > 0 && isMobile"
          circle
          class="action-icon-button"
          @click="toggleExpandState"
        >
          <el-icon :size="20">
            <Fold v-if="defaultExpanded" />
            <Expand v-else />
          </el-icon>
        </el-button>

        <!-- PC端：展开/折叠切换按钮 -->
        <el-button
          v-if="groupedPreviews.length > 0 && !isMobile"
          class="toggle-expand-button"
          @click="toggleExpandState"
        >
          <el-icon>
            <Fold v-if="defaultExpanded" />
            <Expand v-else />
          </el-icon>
          <span>{{ defaultExpanded ? '全部折叠' : '全部展开' }}</span>
        </el-button>
      </div>
    </div>

    <!-- 2. 表格区域 (占据剩余所有空间) -->
    <div class="content-wrapper">
      <!-- 生成预览加载状态 -->
      <div v-if="generatingPreview" class="loading-container">
        <div class="preview-animation">
          <el-icon class="magic-icon bounce-icon" :size="48"><MagicStick /></el-icon>
          <div class="preview-text">正在生成重命名预览...</div>
        </div>
      </div>

      <!-- 分组卡片列表 -->
      <div v-else class="groups-container">
        <div class="groups-scroll">
          <FileGroupItem
            v-for="(group, index) in groupedPreviews"
            :key="index"
            :group="group"
            :show-preview-info="true"
            :default-expanded="defaultExpanded"
            :selected-ids="selectedIds"
            :ref="(el) => setGroupRef(index, el)"
            @row-click="handleItemClick"
          />

          <!-- 空状态 -->
          <el-empty
            v-if="groupedPreviews.length === 0"
            description="暂无预览数据"
          />
        </div>
      </div>
    </div>

    <!-- 3. 警告提示区域（不包含按钮） -->
    <div class="warning-section">
      <el-alert
        title="⚠️ 重命名操作不可撤销,请仔细检查"
        type="warning"
        :closable="false"
        :show-icon="true"
        class="warning-alert"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { View, MagicStick, Expand, Fold } from '@element-plus/icons-vue'
import FileGroupItem from '@/components/FileGroupItem.vue'
import { groupPreviewsBySeriesName } from '@/utils/grouping'
import { useMobileDetection } from '@/composables/useMobileDetection'

// Props
const props = defineProps({
  renamePreviews: {
    type: Array,
    default: () => []
  },
  selectedTemplate: {
    type: String,
    default: 'STANDARD'
  },
  generatingPreview: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits([
  'update:selectedTemplate',
  'update:canNext',
  'update:loading'
])

// 移动端检测
const { isMobile } = useMobileDetection()

// 分组数据
const groupedPreviews = computed(() => {
  return groupPreviewsBySeriesName(props.renamePreviews)
})

// 默认展开状态
const defaultExpanded = ref(true)

// 存储所有分组组件的引用
const groupRefs = ref([])

const setGroupRef = (index, el) => {
  if (el) {
    groupRefs.value[index] = el
  }
}

// ==================== 多选状态管理 ====================
// 选中文件的 ID 集合 (使用数组便于响应式更新)
const selectedIdsArray = ref([])

// 计算属性：将数组转为 Set 供子组件使用
const selectedIds = computed(() => new Set(selectedIdsArray.value))

// 上一次点击的文件 ID (用于 Shift 范围选择)
const lastClickedId = ref(null)

// 扁平化的文件列表 (用于计算范围索引)
const flattenedItems = computed(() => {
  const items = []
  groupedPreviews.value.forEach(group => {
    // 电视剧按季度
    if (group.isTvShow && group.seasons) {
      group.seasons.forEach(season => {
        // 预览对象没有 type 字段，所有 items 都是有效的
        items.push(...season.items)
      })
    } else {
      // 电影或未分季
      items.push(...group.items)
    }
  })
  return items
})

// 处理点击事件
const handleItemClick = (item, event) => {
  // 预览对象使用 filePath 作为唯一标识
  if (!item || !item.filePath) return

  const id = item.filePath

  // 1. Shift + Click (范围选择)
  if (event.shiftKey && lastClickedId.value) {
    const startId = lastClickedId.value
    const endId = id

    const allItems = flattenedItems.value
    const startIndex = allItems.findIndex(i => i.filePath === startId)
    const endIndex = allItems.findIndex(i => i.filePath === endId)

    if (startIndex !== -1 && endIndex !== -1) {
      const min = Math.min(startIndex, endIndex)
      const max = Math.max(startIndex, endIndex)

      const rangeIds = allItems.slice(min, max + 1).map(i => i.filePath)

      // 如果按住 Ctrl/Cmd，则合并选择；否则替换选择
      if (event.ctrlKey || event.metaKey) {
        const newSet = new Set(selectedIdsArray.value)
        rangeIds.forEach(rid => newSet.add(rid))
        selectedIdsArray.value = [...newSet]
      } else {
        selectedIdsArray.value = rangeIds
      }
    }
  }
  // 2. Ctrl/Cmd + Click (切换选择)
  else if (event.ctrlKey || event.metaKey) {
    const currentSet = new Set(selectedIdsArray.value)
    if (currentSet.has(id)) {
      currentSet.delete(id)
    } else {
      currentSet.add(id)
    }
    selectedIdsArray.value = [...currentSet]
    lastClickedId.value = id
  }
  // 3. 普通点击 (单选)
  else {
    selectedIdsArray.value = [id]
    lastClickedId.value = id
  }
}

// 全选
const selectAll = () => {
  const allIds = flattenedItems.value.map(i => i.filePath)
  selectedIdsArray.value = allIds
}

// 取消选择
const clearSelection = () => {
  selectedIdsArray.value = []
  lastClickedId.value = null
}

// 全局快捷键处理
const handleKeyDown = (event) => {
  // Ctrl + A / Cmd + A: 全选
  if ((event.ctrlKey || event.metaKey) && event.key === 'a') {
    event.preventDefault()
    selectAll()
  }
  // Esc: 取消选择
  else if (event.key === 'Escape') {
    clearSelection()
  }
}

// 注册全局快捷键
onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

// 全部展开
const expandAll = () => {
  defaultExpanded.value = true
  groupRefs.value.forEach(group => {
    if (group && group.expand) {
      group.expand()
    }
  })
}

// 全部折叠
const collapseAll = () => {
  defaultExpanded.value = false
  groupRefs.value.forEach(group => {
    if (group && group.collapse) {
      group.collapse()
    }
  })
}

// 切换展开/折叠状态（移动端）
const toggleExpandState = () => {
  if (defaultExpanded.value) {
    collapseAll()
  } else {
    expandAll()
  }
}

// 监听状态变化，通知父组件
watch(() => props.generatingPreview, (val) => {
  emit('update:loading', val)
})

watch(() => props.renamePreviews, (previews) => {
  emit('update:canNext', previews.length > 0)
}, { immediate: true })
</script>

<style scoped>
/* 根容器：占满 100% 高度 */
.step-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ==================== 卡片化布局 ==================== */
/* 外层包装：创建卡片效果 */
.step-container {
  background: var(--surface-1);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-subtle);
}

/* 顶部工具栏：融合进卡片顶部 */
.toolbar {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-xl);
  background-color: var(--surface-1);
  border-bottom: 1px solid var(--surface-2);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  letter-spacing: 0.01em;
  white-space: nowrap; /* 禁止换行 */
  flex-shrink: 0; /* 禁止被挤压 */
}

.title-icon {
  color: var(--primary-brand);
  font-size: 20px;
}

.preview-badge {
  display: flex;
  align-items: center;
}

.toolbar-actions {
  display: flex;
  gap: var(--space-md);
  align-items: center; /* 垂直居中 */
  flex-shrink: 0; /* 按钮区也不被挤压 */
  margin-left: auto; /* 自动推到最右 */
}

/* PC端展开/折叠切换按钮 */
.toggle-expand-button {
  border: none;
  background-color: rgba(30, 136, 229, 0.08); /* 极浅品牌色背景 */
  color: var(--primary-color, #1E88E5); /* 品牌色文字 */
  border-radius: 8px; /* 匹配移动端圆角风格 */
  transition: all 0.2s;
  padding: 8px 16px;
  font-size: 14px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.toggle-expand-button:hover {
  background-color: rgba(30, 136, 229, 0.15); /* Hover 加深 */
  color: var(--primary-color-dark, #1976D2);
}

.toggle-expand-button:active {
  background-color: rgba(30, 136, 229, 0.2); /* 点击时更深 */
}

/* ==================== 内容区域 ==================== */
.content-wrapper {
  flex: 1;
  overflow: hidden;
  position: relative;
  padding: 0;
}

/* 加载状态 */
.loading-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--surface-1);
}

.preview-animation {
  text-align: center;
}

.magic-icon {
  color: var(--primary-brand);
  margin-bottom: var(--space-lg);
}

.preview-text {
  font-size: 15px;
  color: var(--text-secondary);
  margin-bottom: var(--space-xl);
}

/* ==================== 表格样式 ==================== */
/* 分组容器 */
.groups-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.groups-scroll {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  background: var(--surface-1);
}

/* ==================== 警告提示区域 ==================== */
.warning-section {
  flex-shrink: 0;
  padding: var(--space-lg) var(--space-xl);
  background-color: var(--surface-1);
  border-top: 1px solid var(--surface-2);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}

.warning-alert {
  border-radius: var(--radius-sm);
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .toolbar {
    height: 56px;
    padding: 0 var(--space-lg);
  }

  .toolbar-left {
    gap: var(--space-sm);
    flex: 1; /* 允许左侧占据剩余空间 */
    min-width: 0; /* 允许 flex item 压缩内容 */
    overflow: hidden; /* 防止溢出 */
  }

  /* 调整移动端按钮样式 */
  .action-icon-button {
    width: 32px;
    height: 32px;
    padding: 0;
    border: none;
    background: transparent;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}
</style>

