---
name: REST API 参考
description: REST API 参考
status: draft
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [api, rest, reference, openapi]
---

# REST API 参考

本文档提供完整的 REST API 接口参考。

> **注意**: 本文档由 OpenAPI 规范自动生成。最新的 API 文档请访问 [API Reference](/api) 或查看 `docs-website/static/openapi.json`。

---

## API 概览

### 基础信息

```
Base URL: http://localhost/api/v1
认证方式：JWT Bearer Token
数据格式：application/json
```

### 认证流程

```bash
# 1. 获取访问令牌
POST /auth/login
{
  "username": "admin",
  "password": "your-password"
}

# 响应
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 900
}

# 2. 使用令牌访问 API
GET /api/v1/users
Authorization: Bearer eyJhbGc...
```

---

## 接口分类

### 认证接口 (Auth)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/logout` | 用户登出 |
| POST | `/auth/refresh` | 刷新令牌 |
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/password/reset` | 重置密码 |

### 用户接口 (Users)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users` | 获取用户列表 |
| GET | `/users/{id}` | 获取用户详情 |
| POST | `/users` | 创建用户 |
| PUT | `/users/{id}` | 更新用户 |
| DELETE | `/users/{id}` | 删除用户 |

### 服务器接口 (Servers)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/servers` | 获取服务器列表 |
| GET | `/servers/{id}` | 获取服务器详情 |
| POST | `/servers` | 创建服务器 |
| PUT | `/servers/{id}` | 更新服务器 |
| DELETE | `/servers/{id}` | 删除服务器 |
| POST | `/servers/{id}/start` | 启动服务器 |
| POST | `/servers/{id}/stop` | 停止服务器 |
| POST | `/servers/{id}/restart` | 重启服务器 |

### 模板接口 (Templates)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/templates` | 获取模板列表 |
| GET | `/templates/{id}` | 获取模板详情 |
| POST | `/templates` | 创建模板 |
| PUT | `/templates/{id}` | 更新模板 |
| DELETE | `/templates/{id}` | 删除模板 |

### 集群接口 (Clusters)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/clusters` | 获取集群列表 |
| GET | `/clusters/{id}` | 获取集群详情 |
| POST | `/clusters` | 创建集群 |
| PUT | `/clusters/{id}` | 更新集群 |
| DELETE | `/clusters/{id}` | 删除集群 |

---

## 错误码

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 405 | 方法不允许 |
| 409 | 资源冲突 |
| 422 | 参数验证失败 |
| 500 | 服务器内部错误 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 10001 | 用户不存在 |
| 10002 | 密码错误 |
| 10003 | 邮箱已存在 |
| 10004 | 用户名已存在 |
| 20001 | 服务器不存在 |
| 20002 | 服务器名称冲突 |
| 20003 | 服务器状态不允许此操作 |
| 30001 | 模板不存在 |
| 30002 | 模板名称冲突 |
| 40001 | Node Agent 未连接 |
| 40002 | 命令执行失败 |

---

## 请求示例

### 创建服务器

```bash
curl -X POST http://localhost/api/v1/servers \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的服务器",
    "templateId": 1,
    "clusterName": "my-cluster",
    "maxPlayers": 10,
    "pvp": false,
    "serverPassword": "123456"
  }'
```

### 响应示例

```json
{
  "id": 1,
  "name": "我的服务器",
  "status": "deploying",
  "templateId": 1,
  "clusterName": "my-cluster",
  "maxPlayers": 10,
  "pvp": false,
  "createdAt": "2026-05-22T10:00:00Z",
  "updatedAt": "2026-05-22T10:00:00Z"
}
```

---

## 响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 响应数据
  }
}
```

### 错误响应

```json
{
  "code": 10001,
  "message": "用户不存在",
  "data": null,
  "traceId": "abc123def456"
}
```

### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      // 数据列表
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0
  }
}
```

---

## 速率限制

| 接口类型 | 限制 | 时间窗口 |
|----------|------|----------|
| 认证接口 | 10 次/分钟 | 60 秒 |
| 普通接口 | 100 次/分钟 | 60 秒 |
| 管理接口 | 50 次/分钟 | 60 秒 |

**响应头**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1621670400
```

---

## SDK 示例

### JavaScript/TypeScript

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

// 添加认证拦截器
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 自动刷新令牌
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // 尝试刷新令牌
      const refreshToken = localStorage.getItem('refreshToken');
      const { data } = await axios.post('/auth/refresh', { refreshToken });
      localStorage.setItem('accessToken', data.accessToken);
      
      // 重试原请求
      error.config.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(error.config);
    }
    return Promise.reject(error);
  }
);

// 使用示例
export const serverApi = {
  getList: () => api.get('/servers'),
  getById: (id: number) => api.get(`/servers/${id}`),
  create: (data: CreateServerRequest) => api.post('/servers', data),
  start: (id: number) => api.post(`/servers/${id}/start`),
  stop: (id: number) => api.post(`/servers/${id}/stop`),
};
```

### Java (Spring)

```java
@Configuration
public class ApiConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 添加认证拦截器
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String token = getTokenFromContext();
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
    
    @Service
    public class ServerService {
        
        @Autowired
        private RestTemplate restTemplate;
        
        public ServerDTO getServer(Long id) {
            return restTemplate.getForObject(
                "/api/v1/servers/{id}",
                ServerDTO.class,
                id
            );
        }
        
        public ServerDTO createServer(CreateServerRequest request) {
            return restTemplate.postForObject(
                "/api/v1/servers",
                request,
                ServerDTO.class
            );
        }
    }
}
```

---

## 下一步

- 📖 [JSON-RPC 协议](002-json-rpc.md) - Node 通信协议
- 📖 [Node 命令集](003-node-commands.md) - Node 命令参考
- 📖 [认证流程](../../architecture/design/002-authentication.md) - 认证详解

---

**参考链接**:
- [OpenAPI 规范](https://swagger.io/specification/)
- [REST API 最佳实践](https://restfulapi.net/)
- [HTTP 状态码](https://httpstatuses.com/)

**最后更新**: 2026-05-22
