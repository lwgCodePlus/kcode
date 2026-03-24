// ==================== 消息相关类型 ====================

/** 消息块类型 */
export type ContentBlockType = 'text' | 'thinking' | 'tool_use' | 'tool_result'

/** 消息块 (对应 io.agentscope.core.message.ContentBlock) */
export interface ContentBlock {
  type: ContentBlockType
  id?: string              // 工具调用ID (tool_use/tool_result)
  // TextBlock 字段
  text?: string
  content?: string         // 旧字段名 (兼容)
  // ThinkingBlock 字段
  thinking?: string
  // ToolUseBlock / ToolResultBlock 字段
  name?: string
  tool_name?: string       // 旧字段名 (兼容)
  // ToolUseBlock 字段
  input?: unknown
  tool_input?: unknown     // 旧字段名 (兼容)
  // ToolResultBlock 字段
  output?: ContentBlock[]
  tool_output?: ContentBlock[] // 旧字段名 (兼容)
  // Metadata
  metadata?: Record<string, unknown>
}

/** Msg 对象 (对应 io.agentscope.core.message.Msg) */
export interface Msg {
  id?: string
  role?: string
  content: ContentBlock[]
}

/** 消息角色 */
export type MessageRole = 'user' | 'assistant'

/** 消息 */
export interface Message {
  session_id: string
  content: string
  blocks: ContentBlock[]
  role: MessageRole
  finished: boolean
  error?: string
}

// ==================== 会话相关类型 ====================

/** 会话摘要 */
export interface SessionSummary {
  session_id: string
  message_count: number
  last_message: string | null
  created_at: string | null
  updated_at: string | null
}

/** 会话列表响应 */
export interface SessionListResponse {
  sessions: SessionSummary[]
  total: number
}

/** 会话响应 */
export interface SessionResponse {
  session_id: string
  message: string
}

/** 会话历史响应 */
export interface SessionHistoryResponse {
  session_id: string
  messages: Message[]
  message_count: number
}

// ==================== 配置相关类型 ====================

/** 模型配置请求 */
export interface ModelConfigRequest {
  base_url?: string
  api_key?: string
  model_name?: string
}

/** 模型配置响应 */
export interface ModelConfigResponse {
  base_url: string
  api_key: string
  model_name: string
  valid: boolean
  config_path: string | null
}

// ==================== API 请求类型 ====================

/** 对话请求 */
export interface ChatRequest {
  session_id?: string
  message: string
  stream?: boolean
}

/** 对话响应 (对应 ChatResponse DTO，包装 Msg) */
export interface ChatResponse {
  session_id: string
  msg: Msg | null       // 直接包装 Msg 对象
  finished: boolean
  error?: string
}

// ==================== UI 状态类型 ====================

/** 发送状态 */
export type SendStatus = 'idle' | 'sending' | 'streaming' | 'error'

/** 侧边栏状态 */
export interface SidebarState {
  collapsed: boolean
  showSettings: boolean
}

// ==================== 文件搜索相关类型 ====================

/** 文件项 */
export interface FileItem {
  path: string
  name: string
  type: string
}

/** 文件搜索响应 */
export interface FileSearchResponse {
  files: FileItem[]
  total: number
}