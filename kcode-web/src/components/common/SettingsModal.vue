<script setup lang="ts">
import { ref, watch, computed, toRef } from 'vue'
import { X, Save, Loader2, AlertCircle, CheckCircle } from 'lucide-vue-next'
import { useConfigStore } from '@/stores/configStore'

const configStore = useConfigStore()

// 本地表单状态
const formData = ref({
  base_url: '',
  api_key: '',
  model_name: '',
})

// 获取响应式引用
const config = toRef(configStore, 'config')
const showSettings = toRef(configStore, 'showSettings')

// 监听配置变化
watch(
  config,
  (newConfig) => {
    if (newConfig) {
      formData.value = {
        base_url: newConfig.base_url || '',
        api_key: newConfig.api_key || '',
        model_name: newConfig.model_name || '',
      }
    }
  },
  { immediate: true }
)

// 监听弹窗显示 - 每次打开都重新加载配置
watch(
  showSettings,
  async (show) => {
    if (show) {
      await configStore.loadConfig()
    }
  }
)

// 关闭弹窗
const handleClose = () => {
  configStore.closeSettings()
}

// 保存配置
const handleSave = async () => {
  const updates: Record<string, string> = {}
  if (formData.value.base_url) updates.base_url = formData.value.base_url
  if (formData.value.api_key) updates.api_key = formData.value.api_key
  if (formData.value.model_name) updates.model_name = formData.value.model_name

  if (Object.keys(updates).length === 0) {
    handleClose()
    return
  }

  await configStore.updateConfig(updates)
  handleClose()
}

// 配置状态
const isValid = computed(() => configStore.isValid)
const isSaving = computed(() => configStore.isSaving)
const error = computed(() => configStore.error)
</script>

<template>
  <!-- 遮罩层 -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="configStore.showSettings"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        @click.self="handleClose"
      >
        <!-- 弹窗 -->
        <div class="w-full max-w-md bg-white rounded-xl shadow-2xl border border-slate-200">
          <!-- 头部 -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200">
            <h2 class="text-lg font-semibold text-slate-900">模型配置</h2>
            <button
              @click="handleClose"
              class="p-1 hover:bg-slate-100 rounded-lg transition-colors"
            >
              <X class="w-5 h-5 text-slate-400" />
            </button>
          </div>

          <!-- 内容 -->
          <div class="px-6 py-4 space-y-4">
            <!-- 状态提示 -->
            <div
              v-if="isValid"
              class="flex items-center gap-2 p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-700 text-sm"
            >
              <CheckCircle class="w-4 h-4" />
              <span>配置有效</span>
            </div>

            <div
              v-else
              class="flex items-center gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg text-amber-700 text-sm"
            >
              <AlertCircle class="w-4 h-4" />
              <span>OpenAI 兼容接口</span>
            </div>

            <!-- 错误提示 -->
            <div
              v-if="error"
              class="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm"
            >
              <AlertCircle class="w-4 h-4" />
              <span>{{ error }}</span>
            </div>

            <!-- Base URL -->
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-1">
                Base URL
              </label>
              <input
                v-model="formData.base_url"
                type="text"
                placeholder="https://api.openai.com/v1"
                class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 placeholder-slate-400 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
              />
            </div>

            <!-- API Key -->
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-1">
                API Key
              </label>
              <input
                v-model="formData.api_key"
                type="password"
                placeholder="sk-..."
                class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 placeholder-slate-400 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
              />
              <p class="text-xs text-slate-500 mt-1">
                留空则保持原配置不变
              </p>
            </div>

            <!-- Model Name -->
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-1">
                Model Name
              </label>
              <input
                v-model="formData.model_name"
                type="text"
                placeholder="gpt-5.4"
                class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 placeholder-slate-400 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
              />
            </div>
          </div>

          <!-- 底部 -->
          <div class="flex justify-end gap-3 px-6 py-4 border-t border-slate-200">
            <button
              @click="handleClose"
              class="px-4 py-2 text-slate-600 hover:text-slate-900 transition-colors"
            >
              取消
            </button>
            <button
              @click="handleSave"
              :disabled="isSaving"
              class="flex items-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 disabled:opacity-50 rounded-lg text-white font-medium transition-colors shadow-sm"
            >
              <Loader2 v-if="isSaving" class="w-4 h-4 animate-spin" />
              <Save v-else class="w-4 h-4" />
              <span>保存</span>
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