export const GAME_MODES = ['survival', 'endless', 'wilderness', 'relaxed', 'lights_out'] as const
export type GameMode = typeof GAME_MODES[number]

export const CATEGORIES = ['survival', 'pvp', 'caves', 'modpack', 'endless'] as const
export type TemplateCategory = typeof CATEGORIES[number]

export const SORT_OPTIONS = ['downloads', 'rating', 'newest'] as const

export const STATUS_COLORS: Record<string, string> = {
  online: 'green', offline: 'red', unknown: 'default',
  running: 'green', stopped: 'default', error: 'red',
}

export const CARD_GRADIENT = {
  server: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  modpack: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
}
