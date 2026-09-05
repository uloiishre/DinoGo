import { mergeConfig } from 'vite'
import { defineConfig } from 'vitest/config'

import viteConfig from './vite.config.js'

export default defineConfig((configEnv) => {
  const resolvedViteConfig =
    typeof viteConfig === 'function' ? viteConfig(configEnv) : viteConfig

  return mergeConfig(resolvedViteConfig, {
    test: {
      environment: 'jsdom',
      include: ['test/component/**/*.test.js'],
      restoreMocks: true,
    },
  })
})
