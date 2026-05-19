import { useState } from 'react'
import { message } from 'antd'
import type { FormInstance } from 'antd'

interface UseResourceFormOptions<T> {
  form: FormInstance
  initialValues?: T | null
  onSubmit: (values: Record<string, unknown>, editing: boolean) => Promise<{ code: number; message?: string }>
  onSaved: () => void
  onClose: () => void
  successMsg?: { created?: string; saved?: string }
  errorMsg?: { createFailed?: string; updateFailed?: string }
}

export default function useResourceForm<T>({
  form, initialValues, onSubmit, onSaved, onClose,
  successMsg = {},
  errorMsg = {},
}: UseResourceFormOptions<T>) {
  const [loading, setLoading] = useState(false)
  const editing = !!initialValues

  const handleSubmit = async () => {
    setLoading(true)
    try {
      const values = await form.validateFields()
      const res = await onSubmit(values, editing)
      if (res.code === 0) {
        message.success(editing ? (successMsg.saved || 'Saved') : (successMsg.created || 'Created'))
        onSaved()
        onClose()
      } else {
        message.error(res.message || 'Operation failed')
      }
    } catch {
      message.error(editing ? (errorMsg.updateFailed || 'Update failed') : (errorMsg.createFailed || 'Create failed'))
    } finally {
      setLoading(false)
    }
  }

  return { loading, editing, handleSubmit }
}
