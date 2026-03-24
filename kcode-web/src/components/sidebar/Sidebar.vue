<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Settings, ChevronLeft, ChevronRight, MessageSquare, Trash2 } from 'lucide-vue-next'
import { useChat } from '@/composables/useChat'
import { useConfigStore } from '@/stores/configStore'
import { useSessionStore } from '@/stores/sessionStore'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle'): void
}>()

const {
  currentSessionId,
  selectSession,
  createSession,
  deleteSession,
  isSending,
} = useChat()

const configStore = useConfigStore()
const sessionStore = useSessionStore()

const handleSelectSession = (sessionId: string) => {
  selectSession(sessionId)
}

const deleteTargetId = ref<string | null>(null)

const handleDeleteSession = (sessionId: string, event: Event) => {
  event.stopPropagation()
  deleteTargetId.value = sessionId
}

const confirmDelete = () => {
  if (deleteTargetId.value) {
    deleteSession(deleteTargetId.value)
    deleteTargetId.value = null
  }
}

const cancelDelete = () => {
  deleteTargetId.value = null
}

const handleCreateSession = async () => {
  await createSession()
}

const handleOpenSettings = () => {
  configStore.openSettings()
}
</script>

<template>
  <aside
    class="relative flex flex-col h-full bg-white border-r border-slate-200 transition-all duration-300"
    :class="collapsed ? 'w-0' : 'w-72'"
  >
    <!-- 折叠按钮 -->
    <button
      @click="emit('toggle')"
      class="absolute -right-3 top-1/2 -translate-y-1/2 z-50 w-7 h-7 bg-white border border-slate-200 rounded-full flex items-center justify-center hover:bg-slate-50 transition-colors shadow-sm"
    >
      <ChevronLeft v-if="!collapsed" class="w-5 h-5 text-slate-500" />
      <ChevronRight v-else class="w-5 h-5 text-slate-500" />
    </button>

    <div v-show="!collapsed" class="flex flex-col h-full">
      <!-- 新会话按钮 -->
      <div class="p-3">
        <button
          @click="handleCreateSession"
          :disabled="isSending"
          class="w-full flex items-center gap-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg text-slate-700 font-medium transition-colors border border-slate-200"
        >
          <Plus class="w-5 h-5" />
          <span>新会话</span>
        </button>
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto px-2">
        <div class="space-y-1">
          <div
            v-for="session in sessionStore.sortedSessions"
            :key="session.session_id"
            @click="handleSelectSession(session.session_id)"
            class="group flex items-center gap-3 px-3 py-2.5 rounded-lg cursor-pointer transition-colors"
            :class="
              currentSessionId === session.session_id
                ? 'bg-primary-50 text-primary-700 border border-primary-200'
                : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
            "
          >
            <MessageSquare class="w-4 h-4 flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-mono truncate">
                {{ session.session_id }}
              </p>
              <p class="text-xs text-slate-400">
                {{ session.message_count }} 条消息
              </p>
            </div>
            <button
              @click="handleDeleteSession(session.session_id, $event)"
              class="opacity-0 group-hover:opacity-100 p-1 hover:bg-slate-200 rounded transition-all"
            >
              <Trash2 class="w-4 h-4 text-slate-400 hover:text-red-500" />
            </button>
          </div>

          <div
            v-if="sessionStore.sessions.length === 0"
            class="text-center py-8 text-slate-400 text-sm"
          >
            暂无会话
          </div>
        </div>
      </div>

      <!-- 底部设置按钮 -->
      <div class="p-3 border-t border-slate-200">
        <button
          @click="handleOpenSettings"
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors"
        >
          <Settings class="w-5 h-5" />
          <span class="text-sm">模型配置</span>
        </button>
      </div>
    </div>
  </aside>


    <!-- 删除确认对话框 -->
    <ConfirmDialog
      :visible="deleteTargetId !== null"
      title="删除会话"
      message="确定要删除这个会话吗？此操作无法撤销。"
      confirm-text="删除"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />
</template>