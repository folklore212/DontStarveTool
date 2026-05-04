import { Input } from 'antd'
import type { PasswordProps } from 'antd/es/input'
import { LockOutlined } from '@ant-design/icons'
import PasswordStrengthBar from './PasswordStrengthBar'

interface PasswordInputProps extends PasswordProps {
  showStrengthBar?: boolean
  username?: string
  email?: string
}

export default function PasswordInput({
  showStrengthBar = false,
  username,
  email,
  value,
  ...rest
}: PasswordInputProps) {
  return (
    <>
      <Input.Password
        prefix={<LockOutlined />}
        value={value}
        {...rest}
      />
      {showStrengthBar && typeof value === 'string' && (
        <PasswordStrengthBar
          password={value}
          username={username}
          email={email}
        />
      )}
    </>
  )
}
