<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ArrowUp, Square, Loader2, File, FileCode, FileText, Folder, FileJson, FileArchive, FileImage, FileType, Braces, Coffee } from 'lucide-vue-next'
import { useChat } from '@/composables/useChat'
import { searchFiles } from '@/api/chat'
import type { FileItem } from '@/types'

const { sendMessage, stopStreaming, isSending, isCurrentSessionStreaming } = useChat()

const inputText = ref('')
const textareaRef = ref<HTMLTextAreaElement | null>(null)

// @ 文件选择相关状态
const showFilePicker = ref(false)
const fileSearchQuery = ref('')
const fileResults = ref<FileItem[]>([])
const selectedIndex = ref(0)
const isLoadingFiles = ref(false)
const filePickerPosition = ref({ top: 0, left: 0 })
const atIndex = ref(-1) // 记录@符号的位置
const filePickerRef = ref<HTMLElement | null>(null)
const fileItemRefs = ref<Map<number, HTMLElement>>(new Map())
// 自动调整高度
const adjustHeight = () => {
  if (!textareaRef.value) return
  textareaRef.value.style.height = 'auto'
  textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
}

// 监听输入
watch(inputText, () => {
  nextTick(adjustHeight)
  handleInputChange()
})

// 处理输入变化，检测@符号
const handleInputChange = () => {
  if (!textareaRef.value) return
  
  const value = inputText.value
  const cursorPos = textareaRef.value.selectionStart
  
  // 查找光标前最近的@符号
  let foundAtIndex = -1
  for (let i = cursorPos - 1; i >= 0; i--) {
    if (value[i] === '@') {
      foundAtIndex = i
      break
    }
    // 如果遇到空格，停止搜索
    if (value[i] === ' ' || value[i] === '\n') {
      break
    }
  }
  
  if (foundAtIndex !== -1) {
    // 提取@后面的搜索词
    const query = value.substring(foundAtIndex + 1, cursorPos)
    
    // 检查@后面是否有空格（如果有，说明@文件选择已结束）
    if (query.includes(' ') || query.includes('\n')) {
      hideFilePicker()
      return
    }
    
    atIndex.value = foundAtIndex
    fileSearchQuery.value = query
    
    // 计算文件选择器的位置
    calculatePickerPosition()
    
    // 搜索文件
    searchFilesDebounced(query)
  } else {
    hideFilePicker()
  }
}

// 防抖搜索
let searchTimeout: ReturnType<typeof setTimeout> | null = null
const searchFilesDebounced = (query: string) => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  
  searchTimeout = setTimeout(() => {
    performFileSearch(query)
  }, 150)
}

// 执行文件搜索
const performFileSearch = async (query: string) => {
  if (!query) {
    fileResults.value = []
    showFilePicker.value = false
    return
  }
  
  isLoadingFiles.value = true
  
  try {
    const response = await searchFiles(query)
    fileResults.value = response.files || []
    selectedIndex.value = 0
    
    if (fileResults.value.length > 0) {
      showFilePicker.value = true
    } else {
      showFilePicker.value = false
    }
  } catch (error) {
    console.error('搜索文件失败:', error)
    fileResults.value = []
    showFilePicker.value = false
  } finally {
    isLoadingFiles.value = false
  }
}

// 计算文件选择器位置
const calculatePickerPosition = () => {
  if (!textareaRef.value) return
  
  const textarea = textareaRef.value
  const rect = textarea.getBoundingClientRect()
  
  // 简单定位：在输入框上方显示
  filePickerPosition.value = {
    top: rect.top - 8,
    left: rect.left
  }
}

// 隐藏文件选择器
const hideFilePicker = () => {
  showFilePicker.value = false
  fileResults.value = []
  selectedIndex.value = 0
  atIndex.value = -1
}

// 选择文件
const selectFile = (file: FileItem) => {
  if (!textareaRef.value || atIndex.value === -1) return
  
  const value = inputText.value
  const cursorPos = textareaRef.value.selectionStart
  
  // 替换@xxx为@filepath 并添加空格
  const before = value.substring(0, atIndex.value)
  const after = value.substring(cursorPos)
  
  inputText.value = before + `@${file.path} ` + after
  
  hideFilePicker()
  
  nextTick(() => {
    adjustHeight()
    textareaRef.value?.focus()
    // 将光标移动到插入内容+空格之后
    const newPos = before.length + file.path.length + 2 // +1 for @, +1 for space
    textareaRef.value?.setSelectionRange(newPos, newPos)
  })
}

// 获取文件图标
const getFileIcon = (type: string) => {
  // 文件夹
  if (type === 'directory') return Folder
  
  // Java
  if (type === 'java') return Coffee
  
  // JavaScript
  if (type === 'js' || type === 'mjs' || type === 'cjs') return Braces
  
  // TypeScript
  if (type === 'ts' || type === 'tsx') return FileCode
  
  // Vue/React
  if (type === 'vue' || type === 'jsx') return FileCode
  
  // Python
  if (type === 'py') return FileCode
  
  // Go/Rust/C/C++
  if (['go', 'rs', 'c', 'cpp', 'h', 'hpp'].includes(type)) return FileCode
  
  // HTML/XML
  if (['html', 'htm', 'xml', 'svg'].includes(type)) return FileType
  
  // CSS/SCSS/LESS
  if (['css', 'scss', 'sass', 'less'].includes(type)) return FileCode
  
  // JSON
  if (type === 'json') return FileJson
  
  // 配置文件
  if (['yaml', 'yml', 'toml', 'ini', 'env', 'properties', 'conf', 'config'].includes(type)) return FileText
  
  // Markdown/文本
  if (['md', 'markdown', 'txt'].includes(type)) return FileText
  
  // 图片
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'ico', 'bmp', 'svg'].includes(type)) return FileImage
  
  // 压缩包
  if (['zip', 'tar', 'gz', 'rar', '7z', 'bz2'].includes(type)) return FileArchive
  
  // Shell 脚本
  if (['sh', 'bash', 'zsh', 'bat', 'cmd', 'ps1'].includes(type)) return FileCode
  
  // SQL
  if (type === 'sql') return FileCode
  
  return File
}

// 获取文件图标颜色类
const getFileIconColor = (type: string) => {
  // Java - 橙色
  if (type === 'java') return 'text-orange-500'
  
  // JavaScript - 黄色
  if (['js', 'mjs', 'cjs', 'jsx'].includes(type)) return 'text-yellow-500'
  
  // TypeScript - 蓝色
  if (['ts', 'tsx'].includes(type)) return 'text-blue-500'
  
  // Vue - 绿色
  if (type === 'vue') return 'text-emerald-500'
  
  // Python - 蓝色
  if (type === 'py') return 'text-blue-400'
  
  // HTML - 橙红色
  if (['html', 'htm', 'xml'].includes(type)) return 'text-orange-600'
  
  // CSS - 蓝紫色
  if (['css', 'scss', 'sass', 'less'].includes(type)) return 'text-purple-500'
  
  // JSON - 黄色
  if (type === 'json') return 'text-yellow-600'
  
  // Go - 青色
  if (type === 'go') return 'text-cyan-500'
  
  // Rust - 橙色
  if (type === 'rs') return 'text-orange-600'
  
  // C/C++ - 蓝色
  if (['c', 'cpp', 'h', 'hpp'].includes(type)) return 'text-blue-600'
  
  // 文件夹 - 默认
  if (type === 'directory') return 'text-slate-500'
  
  return 'text-slate-400'
}

// 滚动到选中的文件
const scrollToSelectedFile = () => {
  nextTick(() => {
    const item = fileItemRefs.value.get(selectedIndex.value)
    if (item && filePickerRef.value) {
      const container = filePickerRef.value
      const itemTop = item.offsetTop
      const itemBottom = itemTop + item.offsetHeight
      const containerTop = container.scrollTop
      const containerBottom = containerTop + container.clientHeight
      
      if (itemTop < containerTop) {
        container.scrollTop = itemTop
      } else if (itemBottom > containerBottom) {
        container.scrollTop = itemBottom - container.clientHeight
      }
    }
  })
}

// 发送消息
const handleSend = async () => {
  // 如果正在流式输出中，不允许发送
  if (isCurrentSessionStreaming.value) return
  if (!inputText.value.trim()) return
  
  const text = inputText.value.trim()
  inputText.value = ''
  
  nextTick(() => {
    adjustHeight()
  })
  
  hideFilePicker()
  await sendMessage(text)
}

// 停止生成
const handleStop = () => {
  stopStreaming()
}

// 键盘事件
const handleKeydown = (e: KeyboardEvent) => {
  // 如果文件选择器显示中
  if (showFilePicker.value && fileResults.value.length > 0) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, fileResults.value.length - 1)
      scrollToSelectedFile()
      return
    }
    
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      scrollToSelectedFile()
      return
    }
    
    if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault()
      selectFile(fileResults.value[selectedIndex.value])
      return
    }
    
    if (e.key === 'Escape') {
      e.preventDefault()
      hideFilePicker()
      return
    }
  }
  
  // 正常发送消息
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// 焦点
const focus = () => {
  textareaRef.value?.focus()
}

defineExpose({ focus })
</script>

<template>
  <div class="p-4 bg-white border-t border-slate-200">
    <!-- 输入框容器 -->
    <div class="relative">
      <!-- 文件选择器 -->
      <Teleport to="body">
        <div
          ref="filePickerRef"
          v-if="showFilePicker && fileResults.length > 0"
          class="fixed z-50 bg-white border border-slate-200 rounded-lg shadow-xl max-h-64 overflow-y-auto min-w-80"
          :style="{
            bottom: `calc(100vh - ${filePickerPosition.top}px)`,
            left: `${filePickerPosition.left}px`
          }"
        >
          <div class="p-1">
            <div
              v-for="(file, index) in fileResults"
              :key="file.path"
              :ref="(el) => el && fileItemRefs.set(index, el as HTMLElement)"
              @click="selectFile(file)"
              class="flex items-center gap-2 px-3 py-1.5 rounded-lg cursor-pointer transition-colors"
              :class="index === selectedIndex ? 'bg-primary-50 text-primary-700' : 'hover:bg-slate-100'"
            >
              <component :is="getFileIcon(file.type)" class="w-4 h-4 flex-shrink-0" :class="getFileIconColor(file.type)" />
              <span class="text-sm truncate flex-1">{{ file.path }}</span>
            </div>
          </div>
        </div>
      </Teleport>
      
      <!-- 加载指示器 -->
      <div
        v-if="isLoadingFiles"
        class="absolute right-3 top-1/2 -translate-y-1/2"
      >
        <Loader2 class="w-4 h-4 animate-spin text-slate-400" />
      </div>
      
      <textarea
        ref="textareaRef"
        v-model="inputText"
        @keydown="handleKeydown"
        placeholder="输入消息... (@ 提及文件, Shift+Enter 换行)"
        rows="3"
        class="w-full pl-4 pr-14 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 resize-none focus:outline-none focus:border-slate-400 focus:ring-2 focus:ring-slate-200 transition-colors scrollbar-hide min-h-12"
      />
      
      <!-- 发送/停止按钮 -->
      <button
        v-if="isCurrentSessionStreaming"
        @click="handleStop"
        class="absolute right-2 bottom-2 w-10 h-10 bg-red-100 hover:bg-red-200 rounded-lg flex items-center justify-center transition-colors"
      >
        <Square class="w-4 h-4 text-red-600" />
      </button>
      <button
        v-else
        @click="handleSend"
        :disabled="!inputText.trim() || isSending"
        class="absolute right-2 bottom-2 w-10 h-10 bg-slate-200 hover:bg-slate-300 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg flex items-center justify-center transition-colors"
      >
        <Loader2 v-if="isSending" class="w-4 h-4 text-slate-600 animate-spin" />
        <ArrowUp v-else class="w-4 h-4 text-slate-600" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
</style>