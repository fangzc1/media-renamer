<template>
  <div class="action-menu-item" @click="handleClick">
    <el-icon class="item-icon" :color="iconColor" :size="24">
      <component :is="iconComponent" />
    </el-icon>
    <span class="item-label">{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Search, Edit } from '@element-plus/icons-vue'

const props = defineProps({
  icon: {
    type: String,
    required: true,
    validator: (v) => ['search', 'edit'].includes(v)
  },
  label: {
    type: String,
    required: true
  },
  iconColor: {
    type: String,
    default: 'var(--el-color-primary)'
  }
})

const emit = defineEmits(['click'])

const iconComponent = computed(() => ({
  search: Search,
  edit: Edit
}[props.icon]))

const handleClick = () => {
  emit('click')
}
</script>

<style scoped>
.action-menu-item {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 56px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  border-radius: 8px;
  margin: 4px 0;
}

.action-menu-item:active {
  background-color: var(--el-fill-color);
  transform: scale(0.98);
}

.item-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-label {
  flex: 1;
  font-size: 15px;
  color: var(--el-text-color-primary);
  font-weight: 500;
  line-height: 1.5;
}

/* 深色模式优化 */
[data-theme="dark"] .action-menu-item:active {
  background-color: var(--el-fill-color-dark);
}
</style>
