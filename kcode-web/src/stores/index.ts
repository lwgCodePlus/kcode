import { createPinia } from 'pinia'

export const pinia = createPinia()

export { useSessionStore } from './sessionStore'
export { useMessageStore } from './messageStore'
export { useConfigStore } from './configStore'