/**
 * 对话 API
 */
import { api, streamRequest, createStreamRequest } from './client'
import type { ChatRequest, ChatResponse, SessionResponse, SessionListResponse, SessionHistoryResponse, FileSearchResponse } from '@/types'
/**
 * 创建新会话
 */
export const createSession = (): Promise<SessionResponse> => {
  return api.post('/session', {})
}

/**
 * 获取会话列表
 */
export const getSessions = (): Promise<SessionListResponse> => {
  return api.get('/sessions')
}

/**
 * 获取会话历史消息
 */
export const getSessionHistory = (sessionId: string): Promise<SessionHistoryResponse> => {
  return api.get(`/session/${sessionId}/history`)
}

/**
 * 删除会话
 */
export const deleteSession = (sessionId: string): Promise<void> => {
  return api.delete(`/session/${sessionId}`)
}

/**
 * 发送消息（流式响应）
 */
export const sendMessageStream = (
  request: ChatRequest,
  onMessage: (response: ChatResponse) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> => {
  return streamRequest(
    '/chat',
    { ...request, stream: true },
    onMessage as (data: unknown) => void,
    onError,
    onComplete
  )
}

/**
 * 创建可中断的流式请求
 */
export const createChatStream = () => {
  const { start, abort } = createStreamRequest()

  const sendMessage = (
    request: ChatRequest,
    onMessage: (response: ChatResponse) => void,
    onError?: (error: Error) => void,
    onComplete?: () => void
  ): Promise<void> => {
    return start(
      '/chat',
      { ...request, stream: true },
      onMessage as (data: unknown) => void,
      onError,
      onComplete
    )
  }

  return { sendMessage, abort }
}

/**
 * 发送消息（完整响应）
 */
export const sendMessage = (request: ChatRequest): Promise<ChatResponse> => {
  return api.post('/chat/complete', { ...request, stream: false })
}

/**
 * 中断对话
 */
export const interruptChat = (sessionId: string): Promise<void> => {
  return api.post(`/chat/interrupt/${sessionId}`, {})
}

/**
 * 搜索文件
 */
export const searchFiles = (pattern: string): Promise<FileSearchResponse> => {
  const params = new URLSearchParams({ pattern })
  return api.get(`/files/search?${params.toString()}`)
}