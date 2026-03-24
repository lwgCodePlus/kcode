<script setup lang="ts">
import { AlertTriangle, X } from 'lucide-vue-next'

defineProps<{
  visible: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        @click.self="handleCancel"
      >
        <!-- 对话框 -->
        <div class="w-full max-w-sm bg-white rounded-xl shadow-2xl border border-slate-200 mx-4">
          <!-- 头部 -->
          <div class="flex items-center justify-between px-5 py-4 border-b border-slate-200">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center">
                <AlertTriangle class="w-4 h-4 text-red-500" />
              </div>
              <h2 class="text-base font-semibold text-slate-900">{{ title }}</h2>
            </div>
            <button
              @click="handleCancel"
              class="p-1 hover:bg-slate-100 rounded-lg transition-colors"
            >
              <X class="w-4 h-4 text-slate-400" />
            </button>
          </div>

          <!-- 内容 -->
          <div class="px-5 py-4">
            <p class="text-sm text-slate-600">{{ message }}</p>
          </div>

          <!-- 底部 -->
          <div class="flex justify-end gap-3 px-5 py-4 border-t border-slate-200">
            <button
              @click="handleCancel"
              class="px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors text-sm font-medium"
            >
              {{ cancelText || '取消' }}
            </button>
            <button
              @click="handleConfirm"
              class="px-4 py-2 bg-red-500 hover:bg-red-600 rounded-lg text-white font-medium transition-colors shadow-sm text-sm"
            >
              {{ confirmText || '确定' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>