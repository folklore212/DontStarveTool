import { useState, Suspense, lazy } from 'react'
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, theme, Dropdown, Spin } from 'antd'
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  KeyOutlined,
  ApiOutlined,
  SafetyOutlined,
  AuditOutlined,
  LoginOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  ProfileOutlined,
} from '@ant-design/icons'
import { AuthProvider, useAuth } from './context/AuthContext'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const Login = lazy(() => import('./pages/Login'))
const UserList = lazy(() => import('./pages/users/UserList'))
const RoleList = lazy(() => import('./pages/roles/RoleList'))
const OAuthClientList = lazy(() => import('./pages/oauth/OAuthClientList'))
const ApiKeyList = lazy(() => import('./pages/apikeys/ApiKeyList'))
const AuditLogList = lazy(() => import('./pages/audit/AuditLogList'))
const LoginLogList = lazy(() => import('./pages/audit/LoginLogList'))
const MfaPage = lazy(() => import('./pages/mfa/MfaPage'))
const Profile = lazy(() => import('./pages/Profile'))
const Forbidden = lazy(() => import('./pages/Forbidden'))

const { Header, Sider, Content } = Layout

type MenuItem = {
  key: string
  icon: React.ReactNode
  label: string
  /** Optional permission code — if set, the menu item is hidden when the user lacks this permission. */
  permission?: string
}

// TODO: Filter menuItems based on permissions extracted from the JWT
// access-token claims. Currently all items are shown unconditionally;
// the backend enforces access via @RequirePermission on each Controller.
const menuItems: MenuItem[] = [
  { key: '/', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/users', icon: <UserOutlined />, label: 'Users' },
  { key: '/roles', icon: <TeamOutlined />, label: 'Roles' },
  { key: '/oauth-clients', icon: <ApiOutlined />, label: 'OAuth Clients' },
  { key: '/api-keys', icon: <KeyOutlined />, label: 'API Keys' },
  { key: '/mfa', icon: <SafetyOutlined />, label: 'MFA' },
  { key: '/audit-logs', icon: <AuditOutlined />, label: 'Audit Logs' },
  { key: '/login-logs', icon: <LoginOutlined />, label: 'Login Logs' },
]

function PageLoader() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 200 }}>
      <Spin size="large" />
    </div>
  )
}

function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()
  const { logout } = useAuth()

  const selectedKey = location.pathname === '/' ? '/' : location.pathname

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed}>
        <div
          style={{
            height: 32,
            margin: 16,
            color: '#fff',
            fontWeight: 'bold',
            fontSize: collapsed ? 14 : 18,
            textAlign: 'center',
            lineHeight: '32px',
            overflow: 'hidden',
            whiteSpace: 'nowrap',
          }}
        >
          {collapsed ? 'Auth' : 'Auth System'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems.map((item) => ({
            key: item.key,
            icon: item.icon,
            label: item.label,
          }))}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 16px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
          <span style={{ fontSize: 18, fontWeight: 'bold' }}>Auth System Admin</span>
          <Dropdown
            menu={{
              items: [
                { key: 'profile', icon: <ProfileOutlined />, label: 'Profile', onClick: () => navigate('/profile') },
                { type: 'divider' },
                { key: 'logout', icon: <LogoutOutlined />, label: 'Logout', onClick: logout },
              ],
            }}
          >
            <Button type="text" icon={<UserOutlined />}>
              Account
            </Button>
          </Dropdown>
        </Header>
        <Content
          style={{
            margin: 24,
            padding: 24,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            minHeight: 280,
          }}
        >
          <Suspense fallback={<PageLoader />}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/users" element={<UserList />} />
              <Route path="/roles" element={<RoleList />} />
              <Route path="/oauth-clients" element={<OAuthClientList />} />
              <Route path="/api-keys" element={<ApiKeyList />} />
              <Route path="/mfa" element={<MfaPage />} />
              <Route path="/audit-logs" element={<AuditLogList />} />
              <Route path="/login-logs" element={<LoginLogList />} />
              <Route path="/profile" element={<Profile />} />
            </Routes>
          </Suspense>
        </Content>
      </Layout>
    </Layout>
  )
}

function AppRoutes() {
  const { isLoggedIn } = useAuth()

  return (
    <Routes>
      <Route path="/forbidden" element={
        <Suspense fallback={<PageLoader />}>
          <Forbidden />
        </Suspense>
      } />
      <Route path="/login" element={
        <Suspense fallback={<PageLoader />}>
          <Login />
        </Suspense>
      } />
      <Route path="/*" element={isLoggedIn ? <AppLayout /> : (
        <Suspense fallback={<PageLoader />}>
          <Login />
        </Suspense>
      )} />
    </Routes>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}

export default App
