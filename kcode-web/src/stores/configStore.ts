import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ModelConfigRequest, ModelConfigResponse } from '@/types'
import * as configApi from '@/api/config'

export const useConfigStore = defineStore('config', () => {
  // State
  const config = ref<ModelConfigResponse | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)
  const showSettings = ref(false)

  // Getters
  const isValid = computed(() => config.value?.valid ?? false)

  // Actions
  const loadConfig = async () => {
    isLoading.value = true
    error.value = null
    try {
      config.value = await configApi.getModelConfig()
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to load config:', e)
    } finally {
      isLoading.value = false
    }
  }

  const updateConfig = async (updates: ModelConfigRequest) => {
    isSaving.value = true
    error.value = null
    try {
      config.value = await configApi.updateModelConfig(updates)
      return true
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to update config:', e)
      return false
    } finally {
      isSaving.value = false
    }
  }

  const setConfig = async (newConfig: ModelConfigRequest) => {
    isSaving.value = true
    error.value = null
    try {
      config.value = await configApi.setModelConfig(newConfig)
      return true
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to set config:', e)
      return false
    } finally {
      isSaving.value = false
    }
  }

  const resetConfig = async () => {
    isSaving.value = true
    error.value = null
    try {
      await configApi.resetModelConfig()
      config.value = null
      return true
    } catch (e) {
      error.value = (e as Error).message
      console.error('Failed to reset config:', e)
      return false
    } finally {
      isSaving.value = false
    }
  }

  const openSettings = () => {
    showSettings.value = true
  }

  const closeSettings = () => {
    showSettings.value = false
  }

  return {
    // State
    config,
    isLoading,
    isSaving,
    error,
    showSettings,
    // Getters
    isValid,
    // Actions
    loadConfig,
    updateConfig,
    setConfig,
    resetConfig,
    openSettings,
    closeSettings,
  }
})