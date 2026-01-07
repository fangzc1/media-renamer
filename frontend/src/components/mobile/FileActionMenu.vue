<template>
  <BottomSheet
    v-model="internalVisible"
    title="操作菜单"
    size="small"
    :close-on-overlay="true"
  >
    <div class="action-menu-list">
      <!-- 手动搜索（所有层级都有） -->
      <ActionMenuItem
        icon="search"
        label="手动搜索媒体信息"
        @click="handleAction('manual-search')"
      />

      <!-- 修改季号（仅季度和文件层级有） -->
      <ActionMenuItem
        v-if="level !== 'group'"
        icon="edit"
        label="修改季号"
        icon-color="var(--el-color-warning)"
        @click="handleAction('edit-season')"
      />
    </div>
  </BottomSheet>
</template>

<script setup>
import { computed } from 'vue'
import BottomSheet from './BottomSheet.vue'
import ActionMenuItem from './ActionMenuItem.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  level: {
    type: String,
    required: true,
    validator: (v) => ['group', 'season', 'file'].includes(v)
  },
  context: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:visible', 'action'])

const internalVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const handleAction = (type) => {
  emit('action', {
    type,
    level: props.level,
    context: props.context
  })
  internalVisible.value = false
}
</script>

<style scoped>
.action-menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
}
</style>
