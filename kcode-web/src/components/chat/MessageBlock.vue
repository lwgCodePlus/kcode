<script setup lang="ts">
import { ref, computed } from 'vue'
import { ChevronDown, ChevronRight, Wrench, Lightbulb, FileText } from 'lucide-vue-next'
import type { ContentBlock } from '@/types'
import { marked } from 'marked'

const props = defineProps<{
  block: ContentBlock
  isUser?: boolean
}>()

// 思考块默认展开，其他默认折叠
const expanded = ref(props.block.type === 'thinking')

// 内容文本（兼容新旧字段名）
const contentText = computed(() => {
  switch (props.block.type) {
    case 'text':
      // 兼容: text (新) 或 content (旧)
      return props.block.text || props.block.content || ''
    case 'thinking':
      return props.block.thinking || ''
    case 'tool_use':
    case 'tool_result':
      // 兼容: name (新) 或 tool_name (旧)
      return props.block.name || props.block.tool_name || ''
    default:
      return ''
  }
})

// 解析 Markdown
const parsedContent = computed(() => {
  if (props.block.type !== 'text') return contentText.value
  
  // 使用 marked 解析
  const raw = marked.parse(contentText.value) as string
  return raw.replace(/<pre><code class="language-(\w+)">/g, (_match, lang) => {
    return `<pre><code class="language-${lang} hljs">`
  })
})

// 切换展开
const toggleExpand = () => {
  expanded.value = !expanded.value
}
</script>

<template>
  <!-- 文本块 -->
  <div
    v-if="block.type === 'text'"
    class="message-text-block prose prose-slate max-w-none"
    :class="isUser ? 'message-user' : 'message-assistant'"
    v-html="parsedContent"
  />

  <!-- 思考块 -->
  <div v-else-if="block.type === 'thinking'">
    <button
      @click="toggleExpand"
      class="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 transition-colors"
    >
      <Lightbulb class="w-4 h-4 text-amber-500" />
      <span>思考过程</span>
      <ChevronRight v-if="!expanded" class="w-4 h-4" />
      <ChevronDown v-else class="w-4 h-4" />
    </button>
    <div v-if="expanded" class="mt-1 text-sm text-slate-600 whitespace-pre-wrap pl-6 border-l-2 border-amber-200">
      {{ block.thinking }}
    </div>
  </div>

  <!-- 工具调用块 -->
  <div v-else-if="block.type === 'tool_use'">
    <button
      @click="toggleExpand"
      class="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 transition-colors"
    >
      <Wrench class="w-4 h-4 text-blue-500" />
      <span>调用工具 {{ block.name || block.tool_name }}</span>
      <ChevronRight v-if="!expanded" class="w-4 h-4" />
      <ChevronDown v-else class="w-4 h-4" />
    </button>
    <div v-if="expanded" class="mt-1 pl-6 border-l-2 border-blue-200">
      <pre class="text-xs text-slate-600 bg-slate-50 rounded p-2 overflow-x-auto">{{ JSON.stringify(block.input || block.tool_input, null, 2) }}</pre>
    </div>
  </div>

  <!-- 工具结果块 -->
  <div v-else-if="block.type === 'tool_result'">
    <button
      @click="toggleExpand"
      class="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 transition-colors"
    >
      <FileText class="w-4 h-4 text-emerald-500" />
      <span>工具结果 {{ block.name || block.tool_name }}</span>
      <ChevronRight v-if="!expanded" class="w-4 h-4" />
      <ChevronDown v-else class="w-4 h-4" />
    </button>
    <div v-if="expanded && (block.output || block.tool_output)" class="mt-1 pl-6 border-l-2 border-emerald-200">
      <MessageBlock
        v-for="(subBlock, index) in (block.output || block.tool_output)"
        :key="index"
        :block="subBlock"
        :class="index > 0 ? 'mt-2' : ''"
      />
    </div>
  </div>
</template>