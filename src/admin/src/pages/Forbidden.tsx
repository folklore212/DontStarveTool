import { useNavigate } from 'react-router-dom'
import { Result, Button } from 'antd'

function Forbidden() {
  const navigate = useNavigate()

  return (
    <Result
      status="403"
      title="暂无访问权限"
      subTitle="抱歉，您没有访问此页面的权限，请联系管理员"
      extra={
        <Button type="primary" onClick={() => navigate('/')}>
          返回首页
        </Button>
      }
    />
  )
}

export default Forbidden
