<script setup lang="ts">
import type { ContentBlock } from '@/types'
import MessageBlock from './MessageBlock.vue'

defineProps<{
  blocks: ContentBlock[]
}>()

// 生成稳定的 key
const getBlockKey = (block: ContentBlock, index: number): string => {
  // 优先使用 id，其次使用 type + index 组合
  return block.id || `${block.type}-${index}`
}
</script>

<template>
  <div class="flex gap-3 px-4 group">
    <!-- 消息内容 -->
    <div class="flex-1 max-w-[85%]">
      <MessageBlock
        v-for="(block, index) in blocks"
        :key="getBlockKey(block, index)"
        :block="block"
        :is-user="false"
        :class="[index > 0 ? 'mt-2' : '', 'block py-2']"
      />
    </div>
  </div>
</template>