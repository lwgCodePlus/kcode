/**
 * API 客户端
 * 封装 fetch 请求，支持 SSE 流式响应
 */

const API_BASE = '/api'

/** 通用请求配置 */
interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  headers?: Record<string, string>
  body?: unknown
}

/**
 * 发送 HTTP 请求
 */
export async function request<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { method = 'GET', headers = {}, body } = options

  const config: RequestInit = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  }

  if (body) {
    config.body = JSON.stringify(body)
  }

  const response = await fetch(`${API_BASE}${endpoint}`, config)

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  // 读取响应文本
  const text = await response.text()
  
  // 如果响应为空，返回 undefined
  if (!text || text.trim() === '') {
    return undefined as T
  }

  // 尝试解析 JSON
  try {
    return JSON.parse(text) as T
  } catch {
    // 如果解析失败，返回原始文本
    return text as T
  }
}

/**
 * SSE 流式请求
 */
export async function streamRequest(
  endpoint: string,
  body: unknown,
  onMessage: (data: unknown) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('Response body is null')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data) {
            try {
              const parsed = JSON.parse(data)
              onMessage(parsed)
            } catch {
              // 非 JSON 数据，忽略
            }
          }
        }
      }
    }

    onComplete?.()
  } catch (error) {
    onError?.(error as Error)
    throw error
  }
}

/**
 * 创建 AbortController 支持的流式请求
 */
export function createStreamRequest() {
  let controller: AbortController | null = null

  const abort = () => {
    controller?.abort()
    controller = null
  }

  const start = async (
    endpoint: string,
    body: unknown,
    onMessage: (data: unknown) => void,
    onError?: (error: Error) => void,
    onComplete?: () => void
  ): Promise<void> => {
    controller = new AbortController()

    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(body),
      signal: controller.signal,
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error(error || `HTTP ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is null')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data) {
              try {
                const parsed = JSON.parse(data)
                onMessage(parsed)
              } catch {
                // 非 JSON 数据，忽略
              }
            }
          }
        }
      }

      onComplete?.()
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        // 用户主动取消
        return
      }
      onError?.(error as Error)
      throw error
    }
  }

  return { start, abort }
}

export const api = {
  get: <T>(endpoint: string) => request<T>(endpoint),
  post: <T>(endpoint: string, body: unknown) => request<T>(endpoint, { method: 'POST', body }),
  put: <T>(endpoint: string, body: unknown) => request<T>(endpoint, { method: 'PUT', body }),
  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),
}