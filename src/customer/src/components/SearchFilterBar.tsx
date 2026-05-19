import { Input, Select } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useTranslation } from '../i18n'

interface FilterOption {
  value: string
  label: string
}

interface Props {
  keyword: string
  onKeywordChange: (v: string) => void
  searchPlaceholder?: string
  category?: string
  onCategoryChange?: (v: string | undefined) => void
  categoryPlaceholder?: string
  categories?: FilterOption[]
  sort?: string
  onSortChange?: (v: string) => void
  sortOptions?: FilterOption[]
  extra?: React.ReactNode
}

export const CATEGORY_OPTIONS: FilterOption[] = [
  { value: 'survival', label: 'Survival' },
  { value: 'pvp', label: 'PvP' },
  { value: 'caves', label: 'Caves' },
  { value: 'modpack', label: 'Modpack' },
  { value: 'endless', label: 'Endless' },
]

export const SORT_OPTIONS: FilterOption[] = [
  { value: 'downloads', label: 'downloads' },
  { value: 'rating', label: 'rating' },
  { value: 'newest', label: 'newest' },
]

export default function SearchFilterBar({
  keyword, onKeywordChange, searchPlaceholder,
  category, onCategoryChange, categoryPlaceholder, categories,
  sort, onSortChange, sortOptions,
  extra,
}: Props) {
  const { t } = useTranslation()

  return (
    <div style={{ marginBottom: 20, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
      <Input
        prefix={<SearchOutlined />}
        placeholder={searchPlaceholder || t('common.templates_search')}
        value={keyword}
        onChange={(e) => onKeywordChange(e.target.value)}
        style={{ width: 220 }}
        allowClear
        size="middle"
      />
      {onCategoryChange && (
        <Select
          value={category}
          onChange={onCategoryChange}
          allowClear
          placeholder={categoryPlaceholder || t('common.templates_category')}
          style={{ width: 130 }}
          options={(categories || CATEGORY_OPTIONS).map((o) => ({ value: o.value, label: o.label }))}
        />
      )}
      {onSortChange && (
        <Select
          value={sort}
          onChange={onSortChange}
          style={{ width: 140 }}
          options={(sortOptions || SORT_OPTIONS).map((o) => ({ value: o.value, label: t(`common.templates_${o.label}`) || o.label }))}
        />
      )}
      <div style={{ flex: 1 }} />
      {extra}
    </div>
  )
}
