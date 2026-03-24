import { computed } from 'vue'
import { useSessionStore } from '@/stores/sessionStore'
import { useMessageStore } from '@/stores/messageStore'

export function useChat() {
  const sessionStore = useSessionStore()
  const messageStore = useMessageStore()

  // 当前会话ID
  const currentSessionId = computed(() => sessionStore.currentSessionId)

  // 当前会话
  const currentSession = computed(() => sessionStore.currentSession)

  // 当前会话的消息
  const messages = computed(() => {
    if (!currentSessionId.value) return []
    return messageStore.getMessages(currentSessionId.value)
  })

  // 流式内容
  const streamingContent = computed(() => messageStore.streamingContent)

  // 流式会话ID
  const streamingSessionId = computed(() => messageStore.streamingSessionId)

  // 是否正在流式输出（当前会话）
  const isCurrentSessionStreaming = computed(() => 
    messageStore.isStreaming && streamingSessionId.value === currentSessionId.value
  )

  // 是否正在发送
  const isSending = computed(() => messageStore.isSending)

  // 错误信息
  const error = computed(() => messageStore.error)

  // 发送消息
  const sendMessage = async (content: string) => {
    if (!content.trim() || isSending.value) return
    await messageStore.sendMessage(content.trim())
  }

  // 停止流式输出
  const stopStreaming = () => {
    messageStore.stopStreaming()
  }

  // 选择会话
  const selectSession = async (sessionId: string) => {
    if (isSending.value) return
    sessionStore.selectSession(sessionId)
    await messageStore.loadHistory(sessionId)
  }

  // 创建新会话
  const createSession = async () => {
    if (isSending.value) return
    const sessionId = await sessionStore.createSession()
    if (sessionId) {
      messageStore.clearMessages(sessionId)
    }
    return sessionId
  }

  // 删除会话
  const deleteSession = async (sessionId: string) => {
    if (isSending.value) return
    await sessionStore.deleteSession(sessionId)
    messageStore.clearMessages(sessionId)
  }

  return {
    // State
    currentSessionId,
    currentSession,
    messages,
    streamingContent,
    isCurrentSessionStreaming,
    isSending,
    error,
    // Actions
    sendMessage,
    stopStreaming,
    selectSession,
    createSession,
    deleteSession,
  }
}