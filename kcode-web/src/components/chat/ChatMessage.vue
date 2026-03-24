<script setup lang="ts">
import { computed } from 'vue'
import type { Message, ContentBlock } from '@/types'
import MessageBlock from './MessageBlock.vue'

const props = defineProps<{
  message: Message
}>()

const isUser = computed(() => props.message.role === 'user')

// 生成稳定的 key
const getBlockKey = (block: ContentBlock, index: number): string => {
  return block.id || `${block.type}-${index}`
}
</script>

<template>
  <div
    class="flex gap-3 px-4 group"
    :class="isUser ? 'flex-row-reverse' : ''"
  >
    <!-- 消息内容 -->
    <div
      class="flex-1 max-w-[85%]"
      :class="isUser ? 'text-right' : ''"
    >
      <!-- 消息块 -->
      <MessageBlock
        v-for="(block, index) in message.blocks"
        :key="getBlockKey(block, index)"
        :block="block"
        :is-user="isUser"
        :class="[index > 0 ? 'mt-2' : '', isUser ? 'inline-block text-left' : 'block py-2']"
      />
      <!-- 错误信息 -->
      <p v-if="message.error" class="text-red-500 text-sm mt-2 px-4">
        {{ message.error }}
      </p>
    </div>
  </div>
</template>