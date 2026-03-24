<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import Sidebar from '@/components/sidebar/Sidebar.vue'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import StreamingMessage from '@/components/chat/StreamingMessage.vue'
import SettingsModal from '@/components/common/SettingsModal.vue'
import { useChat } from '@/composables/useChat'
import { useAutoScroll } from '@/composables/useAutoScroll'
import { useSessionStore } from '@/stores/sessionStore'
import { useConfigStore } from '@/stores/configStore'

const chat = useChat()
const sessionStore = useSessionStore()
const configStore = useConfigStore()

// 侧边栏状态
const sidebarCollapsed = ref(false)

// 消息容器引用
const messagesContainer = ref<HTMLElement | null>(null)
const { scrollToBottom, shouldAutoScroll } = useAutoScroll(messagesContainer)

// 输入框引用
const chatInputRef = ref<{ focus: () => void } | null>(null)

// 监听消息变化，自动滚动
watch(
  () => chat.messages.value.length,
  () => {
    nextTick(() => {
      if (shouldAutoScroll.value) {
        scrollToBottom(false) // 无动画滚动
      }
    })
  }
)

// 监听流式内容变化 - 使用 deep watch 监听数组变化
watch(
  () => chat.streamingContent.value,
  () => {
    nextTick(() => {
      if (shouldAutoScroll.value) {
        scrollToBottom(false) // 无动画滚动，实时跟随
      }
    })
  },
  { deep: true }
)

// 监听流式状态变化
watch(
  () => chat.isCurrentSessionStreaming.value,
  (streaming) => {
    if (streaming) {
      // 开始流式时重置自动滚动状态
      shouldAutoScroll.value = true
    }
  }
)

// 初始化
onMounted(async () => {
  await sessionStore.loadSessions()
  await configStore.loadConfig()
  
  // 如果有会话，选择第一个
  if (sessionStore.sessions.length > 0) {
    chat.selectSession(sessionStore.sessions[0].session_id)
  }
})
</script>


<template>
  <div class="flex h-screen bg-slate-50">
    <!-- 侧边栏 -->
    <Sidebar :collapsed="sidebarCollapsed" @toggle="sidebarCollapsed = !sidebarCollapsed" />

    <!-- 主内容区 -->
    <main class="flex-1 flex flex-col min-w-0 bg-white">
      <!-- 消息列表 -->
      <div
        ref="messagesContainer"
        class="flex-1 overflow-y-auto"
      >
        <!-- 空状态 -->
        <div
          v-if="chat.messages.value.length === 0 && !chat.isCurrentSessionStreaming.value"
          class="flex flex-col items-center justify-center h-full text-slate-400"
        >
          <svg class="w-16 h-16 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p class="text-lg">开始新对话</p>
        </div>

        <!-- 消息列表容器 - 居中 -->
        <div v-else class="w-full max-w-3xl mx-auto py-4 px-4">
          <ChatMessage
            v-for="(message, index) in chat.messages.value"
            :key="index"
            :message="message"
          />

          <!-- 流式消息 -->
          <StreamingMessage
            v-if="chat.isCurrentSessionStreaming.value && chat.streamingContent.value.length > 0"
            :blocks="chat.streamingContent.value"
          />
        </div>
      </div>

      <!-- 输入区域 - 居中容器 -->
      <div class="w-full flex justify-center pb-4">
        <div class="w-full max-w-3xl px-4">
          <ChatInput ref="chatInputRef" />
        </div>
      </div>
    </main>

    <!-- 设置弹窗 -->
    <SettingsModal />
  </div>
</template>