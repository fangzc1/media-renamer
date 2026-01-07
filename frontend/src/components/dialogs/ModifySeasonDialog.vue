<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="500px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <!-- 对话框内容 -->
    <div class="dialog-content">
      <div class="info-section">
        <el-icon class="info-icon"><InfoFilled /></el-icon>
        <span class="info-text">{{ infoText }}</span>
      </div>

      <!-- 季号输入框 -->
      <el-form label-width="100px">
        <el-form-item label="新季号">
          <el-input-number
            v-model="newSeasonValue"
            :min="0"
            :max="99"
            :precision="0"
            placeholder="请输入季号 (0-99)"
            style="width: 100%"
            @keyup.enter="handleConfirm"
          />
        </el-form-item>
      </el-form>

      <!-- 提示信息 -->
      <el-alert
        title="修改后，文件将自动移动到对应的季度分组中。"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 对话框底部按钮 -->
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :disabled="!isValid">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

// Props
const props = defineProps({
  // 控制对话框显示隐藏
  visible: {
    type: Boolean,
    default: false
  },
  // 当前季号 (单季/单文件时显示，批量时可为空或 null)
  currentSeason: {
    type: Number,
    default: null
  },
  // 受影响的文件数量
  count: {
    type: Number,
    default: 0
  },
  // 上下文类型: 'season' | 'file' | 'batch'
  contextType: {
    type: String,
    default: 'file'
    // 'season': 整季修改
    // 'file': 单文件修改
    // 'batch': 批量文件修改
  }
})

// Emits
const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

// 对话框显示状态
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 新季号输入值
const newSeasonValue = ref(null)

// 监听对话框打开，初始化输入框
watch(() => props.visible, (visible) => {
  if (visible) {
    // 默认值为当前季号
    newSeasonValue.value = props.currentSeason ?? null
  }
})

// 计算标题
const title = computed(() => {
  switch (props.contextType) {
    case 'season':
      return '修改整季季号'
    case 'file':
      return '修改文件季号'
    case 'batch':
      return '批量修改季号'
    default:
      return '修改季号'
  }
})

// 计算提示文本
const infoText = computed(() => {
  const seasonText = props.currentSeason != null ? `Season ${props.currentSeason}` : ''

  switch (props.contextType) {
    case 'season':
      return `正在修改 "${seasonText}" 下的 ${props.count} 个文件`
    case 'file':
      return `正在修改 1 个文件的季号`
    case 'batch':
      return `已选择 ${props.count} 个文件`
    default:
      return `将修改 ${props.count} 个文件`
  }
})

// 验证输入是否有效
const isValid = computed(() => {
  return newSeasonValue.value != null &&
         newSeasonValue.value >= 0 &&
         newSeasonValue.value <= 99
})

// 确认操作
const handleConfirm = () => {
  if (!isValid.value) return

  emit('confirm', newSeasonValue.value)
  dialogVisible.value = false
}

// 取消操作
const handleCancel = () => {
  emit('cancel')
  dialogVisible.value = false
}
</script>

<style scoped>
.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.info-icon {
  color: var(--el-color-primary);
  font-size: 20px;
  flex-shrink: 0;
}

.info-text {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}
</style>
