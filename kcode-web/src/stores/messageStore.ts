import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Message, ContentBlock, SendStatus, ChatResponse } from '@/types'
import * as chatApi from '@/api/chat'
import { useSessionStore } from './sessionStore'

/**
 * 从 ContentBlock 中提取文本内容
 */
function getTextFromBlock(block: ContentBlock): string {
  if (block.type === 'text') {
    return block.text || block.content || ''
  }
  return ''
}

/**
 * 累积状态：按消息ID分组
 */
interface AccumulatedMessage {
  msgId: string
  blocks: ContentBlock[]
}

/**
 * 工具调用追踪：key = toolCallId, value = 累积的 output
 */
interface ToolCallTracking {
  toolCallId: string
  toolName: string
  output: ContentBlock[]
  firstMsgId: string  // 第一次出现时的消息ID
}

export const useMessageStore = defineStore('message', () => {
  // State
  const messagesBySession = ref<Map<string, Message[]>>(new Map())
  // 按消息ID分组的累积状态
  const accumulatedMessages = ref<Map<string, AccumulatedMessage>>(new Map())
  // 消息ID顺序列表
  const messageOrder = ref<string[]>([])
  // 工具调用追踪：单独的 Map，避免遍历查找
  const toolCallTracking = ref<Map<string, ToolCallTracking>>(new Map())
  const streamingSessionId = ref<string | null>(null)
  const status = ref<SendStatus>('idle')
  const error = ref<string | null>(null)

  // 保存 abort 函数
  let abortStream: (() => void) | null = null

  // Getters
  const getMessages = computed(() => (sessionId: string) => {
    return messagesBySession.value.get(sessionId) || []
  })

  // 流式内容：按消息顺序合并所有累积的内容块
  const streamingContent = computed(() => {
    const allBlocks: ContentBlock[] = []
    for (const msgId of messageOrder.value) {
      const msg = accumulatedMessages.value.get(msgId)
      if (msg) {
        allBlocks.push(...msg.blocks)
      }
    }
    return allBlocks
  })

  const isStreaming = computed(() => status.value === 'streaming')
  const isSending = computed(() => status.value === 'sending' || status.value === 'streaming')

  // Actions
  const loadHistory = async (sessionId: string) => {
    try {
      const response = await chatApi.getSessionHistory(sessionId)
      messagesBySession.value.set(sessionId, response.messages || [])
    } catch (e) {
      console.error('Failed to load history:', e)
    }
  }

  const addMessage = (sessionId: string, message: Message) => {
    const messages = messagesBySession.value.get(sessionId) || []
    messages.push(message)
    messagesBySession.value.set(sessionId, [...messages])
  }

  const clearMessages = (sessionId: string) => {
    messagesBySession.value.set(sessionId, [])
  }

  /**
   * 累积消息块到指定消息ID
   */
  const accumulateBlock = (msgId: string, block: ContentBlock) => {
    let msg = accumulatedMessages.value.get(msgId)
    
    if (!msg) {
      msg = { msgId, blocks: [] }
      accumulatedMessages.value.set(msgId, msg)
      // 添加到顺序列表
      if (!messageOrder.value.includes(msgId)) {
        messageOrder.value.push(msgId)
      }
    }

    const blocks = msg.blocks

    if (block.type === 'text') {
      // 文本块：处理增量模式下的去重
      const existingIndex = blocks.findIndex(b => b.type === 'text')
      const newText = getTextFromBlock(block)
      
      if (existingIndex !== -1) {
        const existing = blocks[existingIndex]
        const existingText = getTextFromBlock(existing)
        
        if (newText.startsWith(existingText) && newText !== existingText) {
          blocks[existingIndex] = { ...existing, text: newText }
        } else if (!newText.startsWith(existingText)) {
          blocks[existingIndex] = { ...existing, text: existingText + newText }
        }
      } else {
        blocks.push(block)
      }
    } else if (block.type === 'thinking') {
      // 思考块：在同一消息内累积
      const existingIndex = blocks.findIndex(b => b.type === 'thinking')
      if (existingIndex !== -1) {
        const existing = blocks[existingIndex]
        const newThinking = block.thinking || ''
        const existingThinking = existing.thinking || ''
        
        if (newThinking.startsWith(existingThinking) && newThinking !== existingThinking) {
          blocks[existingIndex] = { ...existing, thinking: newThinking }
        } else if (!newThinking.startsWith(existingThinking)) {
          blocks[existingIndex] = { ...existing, thinking: existingThinking + newThinking }
        }
      } else {
        blocks.push(block)
      }
    } else if (block.type === 'tool_use' || block.type === 'tool_result') {
      // 工具块：使用单独的 Map 追踪
      const toolCallId = block.id
      const toolName = block.name || block.tool_name || 'unknown'
      
      // 如果没有工具调用ID，直接添加到当前消息
      if (!toolCallId) {
        blocks.push(block)
        return
      }
      
      if (block.type === 'tool_result') {
        // tool_result: 使用 toolCallTracking 追踪累积
        const tracking = toolCallTracking.value.get(toolCallId)
        
        if (tracking) {
          // 已存在，累积 output
          const newOutput = block.output || block.tool_output || []
          const mergedOutput = mergeOutputBlocks(tracking.output, newOutput)
          tracking.output = mergedOutput
          
          // 更新原消息中的 block
          const originalMsg = accumulatedMessages.value.get(tracking.firstMsgId)
          if (originalMsg) {
            const blockIdx = originalMsg.blocks.findIndex(b => b.id === toolCallId)
            if (blockIdx !== -1) {
              originalMsg.blocks[blockIdx] = {
                ...originalMsg.blocks[blockIdx],
                output: mergedOutput,
                tool_output: mergedOutput
              }
            }
          }
        } else {
          // 新的工具调用，添加追踪
          const output = block.output || block.tool_output || []
          toolCallTracking.value.set(toolCallId, {
            toolCallId,
            toolName,
            output: [...output],
            firstMsgId: msgId
          })
          // 添加到当前消息
          blocks.push({
            ...block,
            output: [...output],
            tool_output: [...output]
          })
        }
      } else {
        // tool_use: 合并相同 ID 的块，用后面的非空 input 覆盖前面的
        const existingToolUse = blocks.find(b => b.type === 'tool_use' && b.id === toolCallId)
        if (existingToolUse) {
          // 后面的非空 input 覆盖前面的 input
          const newInput = block.input
          const isInputEmpty = !newInput || (typeof newInput === 'object' && Object.keys(newInput as object).length === 0)
          if (!isInputEmpty) {
            existingToolUse.input = newInput
          }
          // 同样处理 tool_input (兼容旧字段)
          const newToolInput = block.tool_input
          const isToolInputEmpty = !newToolInput || (typeof newToolInput === 'object' && Object.keys(newToolInput as object).length === 0)
          if (!isToolInputEmpty) {
            existingToolUse.tool_input = newToolInput
          }
        } else {
          blocks.push(block)
        }
      }
    } else {
      blocks.push(block)
    }
  }

  /**
   * 合并 output 块，累积文本内容
   */
  const mergeOutputBlocks = (existing: ContentBlock[], newBlocks: ContentBlock[]): ContentBlock[] => {
    const result = [...existing]
    
    for (const newBlock of newBlocks) {
      if (newBlock.type === 'text') {
        // 查找是否已有文本块
        const textIdx = result.findIndex(b => b.type === 'text')
        if (textIdx !== -1) {
          // 累积文本
          const existingText = result[textIdx].text || ''
          const newText = newBlock.text || ''
          result[textIdx] = {
            ...result[textIdx],
            text: existingText + newText
          }
        } else {
          result.push(newBlock)
        }
      } else {
        result.push(newBlock)
      }
    }
    
    return result
  }

  const sendMessage = async (content: string) => {
    const sessionStore = useSessionStore()
    let sessionId = sessionStore.currentSessionId

    if (!sessionId) {
      sessionId = await sessionStore.createSession()
      if (!sessionId) {
        error.value = 'Failed to create session'
        return
      }
    }

    // 添加用户消息
    const userMessage: Message = {
      session_id: sessionId,
      content: content,
      blocks: [{ type: 'text', text: content }],
      role: 'user',
      finished: true,
    }
    addMessage(sessionId, userMessage)

    // 更新会话信息
    sessionStore.updateSessionInfo(sessionId, {
      last_message: content.length > 100 ? content.substring(0, 100) + '...' : content,
      message_count: (sessionStore.currentSession?.message_count || 0) + 1,
    })

    // 重置累积状态
    status.value = 'streaming'
    accumulatedMessages.value.clear()
    toolCallTracking.value.clear()  // 清理工具调用追踪
    messageOrder.value = []
    streamingSessionId.value = sessionId
    error.value = null

    const { sendMessage: sendStream, abort } = chatApi.createChatStream()
    abortStream = abort

    try {
      await sendStream(
        { session_id: sessionId, message: content },
        (response: ChatResponse) => {
          if (!response.finished && response.msg?.content && response.msg.content.length > 0) {
            // 使用消息ID分组累积
            const msgId = response.msg.id || 'default'
            const blocks = response.msg.content
            
            for (const block of blocks) {
              accumulateBlock(msgId, block)
            }
          }
          
          // 流式完成
          if (response.finished) {
            const textContent = streamingContent.value
              .filter(b => b.type === 'text')
              .map(b => getTextFromBlock(b))
              .join('')
            
            const assistantMessage: Message = {
              session_id: sessionId!,
              content: textContent,
              blocks: [...streamingContent.value],
              role: 'assistant',
              finished: true,
              error: response.error,
            }
            addMessage(sessionId!, assistantMessage)
            accumulatedMessages.value.clear()
            toolCallTracking.value.clear()  // 清理工具调用追踪
            messageOrder.value = []
          }
        },
        (e: Error) => {
          error.value = e.message
          status.value = 'error'
        },
        () => {
          status.value = 'idle'
          streamingSessionId.value = null
        }
      )
    } catch (e) {
      error.value = (e as Error).message
      status.value = 'error'
    }
  }

  const stopStreaming = async () => {
    const currentSessionId = streamingSessionId.value
    
    if (abortStream) {
      abortStream()
      abortStream = null
    }
    
    if (currentSessionId) {
      try {
        await chatApi.interruptChat(currentSessionId)
        console.log('已调用后端中断接口:', currentSessionId)
      } catch (e) {
        console.error('调用中断接口失败:', e)
      }
    }
    
    // 保存已输出的内容
    if (streamingContent.value.length > 0 && currentSessionId) {
      const partialMessage: Message = {
        session_id: currentSessionId,
        content: streamingContent.value
          .filter(b => b.type === 'text')
          .map(b => getTextFromBlock(b))
          .join(''),
        blocks: [...streamingContent.value],
        role: 'assistant',
        finished: true,
      }
      addMessage(currentSessionId, partialMessage)
      accumulatedMessages.value.clear()
      toolCallTracking.value.clear()  // 清理工具调用追踪
      messageOrder.value = []
    }
    
    status.value = 'idle'
  }

  return {
    // State
    messagesBySession,
    streamingContent,
    streamingSessionId,
    status,
    error,
    // Getters
    getMessages,
    isStreaming,
    isSending,
    // Actions
    loadHistory,
    addMessage,
    clearMessages,
    sendMessage,
    stopStreaming,
  }
})