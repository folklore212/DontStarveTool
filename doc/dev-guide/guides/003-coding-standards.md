---
name: 编码规范
description: Java + TypeScript 编码规范和最佳实践
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [coding-standards, java, typescript, best-practices]
---

# 编码规范

本文档规定了项目的编码规范，所有开发人员必须遵守。

---

## Java 编码规范

### 命名规范

#### 类名（大驼峰）

```java
// ✅ 正确
public class UserService {}
public class ServerController {}
public class BusinessException {}

// ❌ 错误
public class userService {}
public class SERVER_CONTROLLER {}
```

#### 方法和变量（小驼峰）

```java
// ✅ 正确
private String userName;
public void createUser() {}
private List<Server> getServerList() {}

// ❌ 错误
private String UserName;
public void CreateUser() {}
```

#### 常量（全大写）

```java
// ✅ 正确
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_CHARSET = "UTF-8";

// ❌ 错误
public static final int maxRetryCount = 3;
```

#### 包名（全小写）

```java
// ✅ 正确
package com.iccuu.general_web_backend.core;
package com.iccuu.general_web_backend.server.service;

// ❌ 错误
package com.iccuu.GeneralWebBackend.Core;
```

---

### 代码格式

#### 缩进和空格

```java
// 使用 4 个空格缩进（不要使用 Tab）
public class UserService {
    private String userName;  // 声明前空一行
    
    public void createUser() {
        if (condition) {  // 关键字后加空格
            // 4 个空格缩进
        }
    }
}
```

#### 空行规则

```java
// ✅ 正确
public class UserService {
    // 字段之间不空行
    private String userName;
    private Integer age;
    
    // 字段和方法之间空一行
    public UserService() {
    }
    
    // 方法之间空一行
    public void method1() {
    }
    
    public void method2() {
    }
}
```

#### 行宽限制

```java
// 单行不超过 120 个字符
// 超过时进行换行，第二行缩进 4 个空格
public void veryLongMethod(String param1, String param2, 
    String param3, String param4) {
    // ...
}
```

---

### 注释规范

#### 类注释

```java
/**
 * 用户服务类
 * <p>
 * 处理用户相关的业务逻辑，包括用户创建、查询、更新、删除等操作
 * </p>
 *
 * @author your-name
 * @since 1.0.0
 */
public class UserService {
}
```

#### 方法注释

```java
/**
 * 创建新用户
 *
 * @param request 用户创建请求
 * @return 创建的用户信息
 * @throws BusinessException 当用户已存在时抛出
 */
public UserDTO createUser(CreateUserRequest request) {
    // ...
}
```

#### 行内注释

```java
// ✅ 正确：解释为什么
int retryCount = 3;  // 重试次数，与配置保持一致

// ❌ 错误：解释是什么
int retryCount = 3;  // 设置重试次数为 3
```

---

### 最佳实践

#### 1. 使用 Optional 处理空值

```java
// ✅ 正确
public Optional<User> findUser(Long id) {
    return Optional.ofNullable(userMapper.selectById(id));
}

// 使用
findUser(1L).ifPresent(user -> {
    // 处理用户
});

// ❌ 错误
public User findUser(Long id) {
    return userMapper.selectById(id);  // 可能返回 null
}
```

#### 2. 使用 Stream API

```java
// ✅ 正确
List<String> userNames = users.stream()
    .filter(user -> user.isActive())
    .map(User::getName)
    .collect(Collectors.toList());

// ❌ 错误（传统方式）
List<String> userNames = new ArrayList<>();
for (User user : users) {
    if (user.isActive()) {
        userNames.add(user.getName());
    }
}
```

#### 3. 异常处理

```java
// ✅ 正确
try {
    // 业务逻辑
} catch (SpecificException e) {
    log.error("具体异常信息", e);
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}

// ❌ 错误
try {
    // 业务逻辑
} catch (Exception e) {
    e.printStackTrace();  // 不要打印堆栈
}
```

#### 4. 使用 Lombok

```java
// ✅ 正确
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
}

// ❌ 错误：手动编写 getter/setter
public class UserDTO {
    private Long id;
    private String name;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ... 省略 setter
}
```

---

## TypeScript 编码规范

### 命名规范

#### 组件名（大驼峰）

```typescript
// ✅ 正确
const ServerDetail: React.FC = () => {};
const DeployWizard: React.FC = () => {};

// ❌ 错误
const serverDetail: React.FC = () => {};
```

#### 函数和变量（小驼峰）

```typescript
// ✅ 正确
const userName: string;
function fetchUserList() {}
const getUserById = (id: number) => {};

// ❌ 错误
const UserName: string;
function FetchUserList() {}
```

#### 类型和接口（大驼峰）

```typescript
// ✅ 正确
interface UserDTO {
    id: number;
    name: string;
}

type ServerStatus = 'running' | 'stopped' | 'error';

// ❌ 错误
interface userDTO {
    id: number;
}
```

---

### 代码格式

#### 缩进

```typescript
// 使用 2 个空格缩进
const user = {
  name: 'John',  // 2 个空格
  age: 25,
  address: {     // 对象属性对齐
    city: 'New York',
    zip: '10001'
  }
};
```

#### 分号

```typescript
// ✅ 推荐：使用分号
const name = 'John';

// ⚠️ 允许：不使用分号（团队统一即可）
const name = 'John'
```

---

### React 规范

#### 组件定义

```typescript
// ✅ 正确：使用 React.FC
const ServerList: React.FC<ServerListProps> = ({ servers }) => {
  return (
    <div>
      {servers.map(server => (
        <ServerCard key={server.id} server={server} />
      ))}
    </div>
  );
};

// ❌ 错误
function ServerList(props: ServerListProps) {
  return <div>{/* ... */}</div>;
}
```

#### Hooks 使用

```typescript
// ✅ 正确
const [count, setCount] = useState<number>(0);
const user = useMemo(() => fetchUser(), []);
useEffect(() => {
  // 副作用
}, [dependency]);

// ❌ 错误：在条件语句中使用 Hooks
if (condition) {
  const [count, setCount] = useState(0);  // 错误！
}
```

#### Props 解构

```typescript
// ✅ 正确
const ServerCard: React.FC<ServerCardProps> = ({ 
  server, 
  onRestart,
  onDelete 
}) => {
  return <div>{/* ... */}</div>;
};

// ❌ 错误
const ServerCard: React.FC<ServerCardProps> = (props) => {
  return <div>{props.server.name}</div>;
};
```

---

## Git 提交规范

### Commit Message 格式

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Type 说明

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响逻辑） |
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/配置 |

### 示例

```bash
# 新功能
git commit -m "feat(server): add server restart API endpoint"

# Bug 修复
git commit -m "fix(auth): resolve JWT token expiration issue"

# 文档更新
git commit -m "docs(api): update REST API documentation"

# 重构
git commit -m "refactor(core): simplify user service logic"

# 带 scope 和 body
git commit -m "feat(api): add pagination support

- Add page and size parameters
- Update response format with total count
- Add pagination example to documentation

Closes #123"
```

---

## 代码审查清单

### 自查清单

提交代码前，请检查：

- [ ] 代码是否遵循命名规范
- [ ] 是否有适当的注释
- [ ] 是否有单元测试
- [ ] 是否通过所有测试
- [ ] 是否有代码异味（重复代码、过长方法等）
- [ ] 是否更新了相关文档

### 审查要点

**代码质量**:
- 可读性：代码是否清晰易懂
- 可维护性：是否易于修改和扩展
- 可测试性：是否易于编写测试

**性能**:
- 是否有性能问题（循环查询、内存泄漏等）
- 是否使用了缓存
- 是否有不必要的计算

**安全**:
- 是否有 SQL 注入风险
- 是否有 XSS 风险
- 敏感信息是否加密

---

## 工具配置

### IntelliJ IDEA 配置

```xml
<!-- .idea/codeStyles/JavaCodeStyle.xml -->
<code_scheme>
  <option name="CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" value="99" />
  <option name="NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND" value="99" />
  <option name="IMPORT_LAYOUT_TABLE">
    <value>
      <package name="" withSubpackages="true" static="true" />
      <emptyLine />
      <package name="java" withSubpackages="true" static="false" />
      <emptyLine />
      <package name="javax" withSubpackages="true" static="false" />
      <emptyLine />
      <package name="org" withSubpackages="true" static="false" />
      <emptyLine />
      <package name="com" withSubpackages="true" static="false" />
    </value>
  </option>
</code_scheme>
```

### ESLint 配置

```json
// .eslintrc.json
{
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react/recommended"
  ],
  "rules": {
    "indent": ["error", 2],
    "quotes": ["error", "single"],
    "semi": ["error", "always"],
    "react/react-in-jsx-scope": "off"
  }
}
```

### Prettier 配置

```json
// .prettierrc
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100
}
```

---

## 下一步

- 📖 [调试指南](004-debugging.md)
- 📖 [测试指南](005-testing.md)
- 📖 [本地开发环境](../setup/001-local-setup.md)

---

**参考链接**:
- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [TypeScript 官方文档](https://www.typescriptlang.org/docs/)
- [React 官方文档](https://react.dev/)

**最后更新**: 2026-05-22
