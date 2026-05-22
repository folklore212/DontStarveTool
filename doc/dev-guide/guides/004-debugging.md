---
name: 调试指南
description: 后端和前端的调试技巧和最佳实践
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [debugging, java, typescript, ide, logging]
---

# 调试指南

本文档介绍后端和前端的调试技巧，帮助你快速定位和解决问题。

---

## 后端调试（Java）

### IDEA 调试配置

#### 1. 基础调试

**启动调试模式**:
```
Run → Debug 'CorePlatformApplication' (Shift+F9)
```

**常用快捷键**:
| 快捷键 | 功能 |
|--------|------|
| `F8` | Step Over（单步跳过） |
| `F7` | Step Into（进入方法） |
| `Shift+F8` | Step Out（跳出方法） |
| `F9` | Resume（继续执行） |
| `Ctrl+F8` | Toggle Breakpoint（切换断点） |

#### 2. 条件断点

**使用场景**: 只在特定条件下中断

**配置方法**:
1. 右键点击断点
2. 输入条件表达式
3. 点击 OK

**示例**:
```java
// 只在 userId > 100 时中断
userId > 100

// 只在特定用户时中断
"user123".equals(userName)
```

#### 3. 异常断点

**捕获所有异常**:
```
Run → View Breakpoints → Java Exception Breakpoints
+ 添加：java.lang.Exception
```

**捕获业务异常**:
```
+ 添加：com.iccuu.general_web_backend.exception.BusinessException
```

#### 4. 方法断点

**在方法入口/出口中断**:
1. 在方法名上设置断点
2. 右键断点 → Method Breakpoint
3. 选择 Entry/Exit/Both

---

### 日志调试

#### 日志级别

```java
@Slf4j
public class UserService {
    
    public void createUser(User user) {
        log.trace("TRACE: 最详细的调试信息");
        log.debug("DEBUG: 调试信息，用于开发环境");
        log.info("INFO: 一般信息，用户创建成功：{}", user.getName());
        log.warn("WARN: 警告信息，用户已存在：{}", user.getName());
        log.error("ERROR: 错误信息，数据库连接失败", exception);
    }
}
```

#### 日志配置

```yaml
# application-dev.yml
logging:
  level:
    root: INFO
    com.iccuu.general_web_backend: DEBUG
    org.springframework.web: DEBUG
    org.mybatis: DEBUG
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: logs/app.log
    max-size: 10MB
    max-history: 30
```

#### 动态调整日志级别

**通过 Actuator**:
```bash
# 查看当前日志级别
curl http://localhost:8081/actuator/loggers/com.iccuu.general_web_backend

# 动态调整为 DEBUG
curl -X POST http://localhost:8081/actuator/loggers/com.iccuu.general_web_backend \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel":"DEBUG"}'
```

---

### 远程调试

#### 1. 配置远程调试参数

```bash
# 启动应用时添加参数
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar app.jar
```

#### 2. IDEA 配置

```
Run → Edit Configurations → + → Remote JVM Debug
- Name: Remote Debug
- Host: localhost
- Port: 5005
```

#### 3. 启动调试

1. 先启动远程应用
2. 在 IDEA 中点击 Debug
3. 本地可以设置断点调试

---

### 常见问题调试

#### 问题 1: 空指针异常

**调试步骤**:
```java
// 1. 在可能为 null 的地方设置断点
User user = userRepository.findById(id);  // 断点

// 2. 查看变量值
// user = null

// 3. 检查上游数据
log.debug("查询 ID: {}", id);
log.debug("查询结果：{}", user);
```

**解决方案**:
```java
// 使用 Optional
return userRepository.findById(id)
    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
```

#### 问题 2: 数据库查询慢

**调试步骤**:
```java
// 1. 开启 SQL 日志
logging.level.org.mybatis: DEBUG
logging.level.java.sql.Connection: DEBUG
logging.level.java.sql.Statement: DEBUG
logging.level.java.sql.ResultSet: DEBUG

// 2. 查看执行的 SQL 和耗时
// 3. 使用 Explain 分析 SQL
```

**优化方案**:
```java
// 添加索引
@Index(name = "idx_user_name", columnList = "userName")

// 使用缓存
@Cacheable(value = "users", key = "#id")
public User getUser(Long id) {
    return userRepository.findById(id);
}
```

#### 问题 3: 内存泄漏

**调试步骤**:
```bash
# 1. 查看堆内存使用
jstat -gc <pid> 1000

# 2. 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 3. 使用 MAT 分析
# 打开 heap.hprof，查看 Dominator Tree
```

**常见原因**:
- 静态集合类持有对象引用
- 未关闭的资源（数据库连接、文件流）
- 监听器未注销

---

## 前端调试（TypeScript + React）

### Chrome DevTools 调试

#### 1. 基础调试

**打开 DevTools**: `F12` 或 `Ctrl+Shift+I`

**常用功能**:
| 功能 | 快捷键 | 说明 |
|------|--------|------|
| Elements | `Ctrl+Shift+C` | 查看和编辑 DOM |
| Console | `Ctrl+Shift+J` | 控制台 |
| Sources | `Ctrl+Shift+\` | 源码调试 |
| Network | `Ctrl+Shift+E` | 网络请求 |
| Application | - | 查看 Storage、Cache |

#### 2. 断点调试

**在 Sources 中设置断点**:
1. 打开源码文件
2. 点击行号设置断点
3. 刷新页面触发代码执行

**条件断点**:
```javascript
// 右键断点 → Add conditional breakpoint
userId > 100
```

#### 3. 网络请求调试

**查看请求详情**:
```
Network → 点击请求 → 查看：
- Headers: 请求头和响应头
- Payload: 请求参数
- Response: 响应内容
- Timing: 耗时分析
```

**重放请求**:
```
右键请求 → Replay XHR
```

---

### React DevTools

#### 安装扩展

```
Chrome Web Store → React Developer Tools
```

#### 常用功能

**Components 面板**:
- 查看组件树
- 查看和修改 Props/State
- 查看 Hook 值

**Profiler 面板**:
- 性能分析
- 查看渲染耗时
- 找出性能瓶颈

---

### 调试技巧

#### 1. Console 技巧

```javascript
// 表格输出
console.table(userList);

// 分组输出
console.group('用户信息');
console.log('姓名:', user.name);
console.log('年龄:', user.age);
console.groupEnd();

// 计时
console.time('fetchData');
await fetchData();
console.timeEnd('fetchData');  // 输出耗时

// 断言
console.assert(user.age > 0, '年龄必须大于 0');
```

#### 2. Source Map 调试

**配置 Vite**:
```typescript
// vite.config.ts
export default defineConfig({
  build: {
    sourcemap: true  // 生成 Source Map
  }
});
```

**Chrome 配置**:
```
Settings → Preferences → Sources
✓ Enable JavaScript source maps
✓ Enable CSS source maps
```

#### 3. 调试 Hook

```typescript
// 自定义 Hook 调试
function useDebugValue(value: any) {
  useEffect(() => {
    console.log('[Hook Value]', value);
  }, [value]);
}

// 使用
const [count, setCount] = useState(0);
useDebugValue({ count });
```

---

### 常见问题调试

#### 问题 1: 组件不更新

**调试步骤**:
```typescript
// 1. 检查 Props 是否变化
console.log('Props changed:', props);

// 2. 检查 State 是否更新
console.log('State:', state);

// 3. 检查是否被 React.memo 阻止
console.log('Prev props:', prevProps);
console.log('Next props:', nextProps);
```

**解决方案**:
```typescript
// 确保引用变化
setUsers([...newUsers]);  // 创建新数组
setUser({ ...user, name: 'new' });  // 创建新对象
```

#### 问题 2: 内存泄漏

**调试步骤**:
```
1. Chrome DevTools → Memory
2. Take heap snapshot
3. 执行操作
4. Take another snapshot
5. 对比两个快照，查找未释放的对象
```

**常见原因**:
- 未清理的定时器
- 未取消的订阅
- 未移除的事件监听器

**解决方案**:
```typescript
useEffect(() => {
  const timer = setInterval(() => {}, 1000);
  const subscription = store.subscribe(() => {});
  
  // 清理函数
  return () => {
    clearInterval(timer);
    subscription.unsubscribe();
  };
}, []);
```

#### 问题 3: API 请求失败

**调试步骤**:
```typescript
// 1. 查看 Network 面板
// - 请求 URL 是否正确
// - 请求头是否包含认证信息
// - 响应状态码

// 2. 在代码中捕获错误
try {
  const response = await fetch('/api/users');
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const data = await response.json();
} catch (error) {
  console.error('请求失败:', error);
  console.log('请求 URL:', response?.url);
  console.log('响应状态:', response?.status);
}
```

---

## 调试工具

### 后端工具

| 工具 | 用途 |
|------|------|
| **IDEA Debugger** | 断点调试 |
| **Actuator** | 运行时监控 |
| **JConsole** | JVM 监控 |
| **VisualVM** | 性能分析 |
| **MAT** | 内存分析 |
| **Arthas** | 在线诊断 |

### 前端工具

| 工具 | 用途 |
|------|------|
| **Chrome DevTools** | 基础调试 |
| **React DevTools** | React 组件调试 |
| **Redux DevTools** | 状态管理调试 |
| **Lighthouse** | 性能分析 |
| **Webpack Bundle Analyzer** | 打包分析 |

---

## 下一步

- 📖 [测试指南](005-testing.md)
- 📖 [编码规范](003-coding-standards.md)
- 📖 [故障排查](../operations/008-troubleshooting.md)

---

**参考链接**:
- [IDEA 调试文档](https://www.jetbrains.com/help/idea/debugging-code.html)
- [Chrome DevTools 文档](https://developer.chrome.com/docs/devtools/)
- [React DevTools 文档](https://react.dev/learn/react-developer-tools)

**最后更新**: 2026-05-22
