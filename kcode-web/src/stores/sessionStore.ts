import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { SessionSummary } from '@/types'
import * as chatApi from '@/api/chat'

export const useSessionStore = defineStore('session', () => {
  // State
  const sessions = ref<SessionSummary[]>([])
  const currentSessionId = ref<string | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const currentSession = computed(() =>
    sessions.value.find(s => s.session_id === currentSessionId.value)
  )

  const sortedSessions = computed(() =>
    [...sessions.value].sort((a, b) => {
      const timeA = a.updated_at ? new Date(a.updated_at).getTime() : 0
      const timeB = b.updated_at ? new Date(b.updated_at).getTime() : 0
      return timeB - timeA
    })
  )

  // Actions
  const loadSessions = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await chatApi.getSessions()
      sessions.value = response.sessions || []
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to load sessions:', e)
    } finally {
      isLoading.value = false
    }
  }

  const createSession = async () => {
    error.value = null
    try {
      const response = await chatApi.createSession()
      const now = new Date().toISOString()
      const newSession: SessionSummary = {
        session_id: response.session_id,
        message_count: 0,
        last_message: null,
        created_at: now,
        updated_at: now,
      }
      sessions.value.unshift(newSession)
      currentSessionId.value = response.session_id
      return response.session_id
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to create session:', e)
      return null
    }
  }

  const selectSession = (sessionId: string) => {
    currentSessionId.value = sessionId
  }

  const deleteSession = async (sessionId: string) => {
    error.value = null
    try {
      await chatApi.deleteSession(sessionId)
      // 从本地列表中移除
      sessions.value = sessions.value.filter(s => s.session_id !== sessionId)
      // 如果删除的是当前会话，切换到其他会话
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = sessions.value[0]?.session_id || null
      }
      // 重新加载会话列表以确保同步
      await loadSessions()
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to delete session:', e)
    }
  }

  const updateSessionInfo = (sessionId: string, updates: Partial<SessionSummary>) => {
    const index = sessions.value.findIndex(s => s.session_id === sessionId)
    if (index !== -1) {
      sessions.value[index] = { ...sessions.value[index], ...updates }
    }
  }

  return {
    // State
    sessions,
    currentSessionId,
    isLoading,
    error,
    // Getters
    currentSession,
    sortedSessions,
    // Actions
    loadSessions,
    createSession,
    selectSession,
    deleteSession,
    updateSessionInfo,
  }
})