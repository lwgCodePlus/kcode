import { ref, onMounted, onUnmounted } from 'vue'

export function useAutoScroll(containerRef: { value: HTMLElement | null }) {
  const shouldAutoScroll = ref(true)
  const isNearBottom = ref(true)

  // 检查是否在底部附近
  const checkNearBottom = () => {
    if (!containerRef.value) return
    const { scrollTop, scrollHeight, clientHeight } = containerRef.value
    isNearBottom.value = scrollHeight - scrollTop - clientHeight < 100
  }

  // 滚动到底部
  const scrollToBottom = (smooth = true) => {
    if (!containerRef.value) return
    containerRef.value.scrollTo({
      top: containerRef.value.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto',
    })
  }

  // 立即滚动到底部（无动画）
  const scrollToBottomImmediate = () => {
    if (!containerRef.value) return
    containerRef.value.scrollTop = containerRef.value.scrollHeight
  }

  // 用户手动滚动时检查
  const handleScroll = () => {
    checkNearBottom()
    // 如果用户滚动到底部，恢复自动滚动
    if (isNearBottom.value) {
      shouldAutoScroll.value = true
    } else {
      shouldAutoScroll.value = false
    }
  }

  // 监听滚动
  onMounted(() => {
    containerRef.value?.addEventListener('scroll', handleScroll)
  })

  onUnmounted(() => {
    containerRef.value?.removeEventListener('scroll', handleScroll)
  })

  return {
    shouldAutoScroll,
    isNearBottom,
    scrollToBottom,
    scrollToBottomImmediate,
    checkNearBottom,
  }
}