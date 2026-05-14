import { useState, useMemo, useCallback } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, Dropdown, Avatar, theme } from 'antd'
import {
  DashboardOutlined,
  CloudServerOutlined,
  ShopOutlined,
  AppstoreOutlined,
  BarChartOutlined,
  BulbOutlined,
  UserOutlined,
  SafetyOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { useTranslation } from '../i18n'
import LanguageSwitcher from './LanguageSwitcher'
import NotificationCenter from './NotificationCenter'
import CommandPalette from './CommandPalette'

const { Header, Sider, Content } = Layout

export default function UserLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { state, logout } = useAuth()
  const { toggleDarkMode } = useTheme()
  const { t } = useTranslation()
  const { token } = theme.useToken()

  const user = state.userInfo

  const menuItems = useMemo(() => [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: t('common.nav_dashboard'),
    },
    {
      key: '/servers',
      icon: <CloudServerOutlined />,
      label: 'Servers',
    },
    {
      key: '/marketplace',
      icon: <ShopOutlined />,
      label: 'Marketplace',
    },
    {
      key: '/templates',
      icon: <AppstoreOutlined />,
      label: 'Templates',
    },
    {
      key: '/analytics',
      icon: <BarChartOutlined />,
      label: 'Analytics',
    },
  ], [t])

  const selectedKey = '/' + (location.pathname.split('/')[1] || 'dashboard')

  const handleLogout = useCallback(async () => {
    await logout()
  }, [logout])

  const toggleCollapsed = useCallback(() => {
    setCollapsed((v) => !v)
  }, [])

  const handleMenuClick = useCallback(({ key }: { key: string }) => {
    navigate(key)
  }, [navigate])

  const userMenuItems = useMemo(() => [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: t('common.nav_profile'),
      onClick: () => navigate('/profile'),
    },
    {
      key: 'security',
      icon: <SafetyOutlined />,
      label: t('common.nav_security'),
      onClick: () => navigate('/security'),
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: t('common.nav_logout'),
      onClick: handleLogout,
    },
  ], [t, navigate, handleLogout])

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        breakpoint="lg"
        collapsedWidth={64}
        style={{
          background: token.colorBgContainer,
          borderRight: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: collapsed ? 16 : 20,
            color: token.colorPrimary,
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          {collapsed ? 'IC' : 'ICCU'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ border: 'none', marginTop: 8 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: token.colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 24px',
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            height: 64,
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={toggleCollapsed}
          />
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <LanguageSwitcher />
            <Button type="text" icon={<BulbOutlined />} onClick={toggleDarkMode} />
            <NotificationCenter />
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
                <Avatar
                  size={32}
                  icon={<UserOutlined />}
                  src={user?.avatar}
                  style={{ backgroundColor: token.colorPrimary }}
                />
                <span style={{ maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {user?.nickname || user?.username || 'User'}
                </span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content
          style={{
            margin: 24,
            padding: 24,
            background: token.colorBgContainer,
            borderRadius: token.borderRadiusLG,
            minHeight: 280,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
      <CommandPalette />
    </Layout>
  )
}
