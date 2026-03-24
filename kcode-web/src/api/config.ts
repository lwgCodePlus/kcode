/**
 * 模型配置 API
 */
import { api } from './client'
import type { ModelConfigRequest, ModelConfigResponse } from '@/types'

/**
 * 获取模型配置
 */
export const getModelConfig = (): Promise<ModelConfigResponse> => {
  return api.get('/config/model')
}

/**
 * 更新模型配置（部分更新）
 */
export const updateModelConfig = (config: ModelConfigRequest): Promise<ModelConfigResponse> => {
  return api.put('/config/model', config)
}

/**
 * 设置模型配置（完整设置）
 */
export const setModelConfig = (config: ModelConfigRequest): Promise<ModelConfigResponse> => {
  return api.post('/config/model', config)
}

/**
 * 重置模型配置
 */
export const resetModelConfig = (): Promise<void> => {
  return api.delete('/config/model')
}