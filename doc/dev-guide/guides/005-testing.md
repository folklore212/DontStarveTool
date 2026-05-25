---
name: 测试指南
description: 测试指南
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [testing, unit-test, integration-test, e2e, junit, vitest]
---

# 测试指南

本文档介绍项目的测试策略和最佳实践，包括单元测试、集成测试和 E2E 测试。

---

## 测试策略

### 测试金字塔

```
        /\
       /  \
      / E2E \      少量：端到端测试
     /------\
    /        \
   /  集成测试  \    适量：服务间集成测试
  /------------\
 /              \
/   单元测试      \  大量：单元测试
------------------
```

**比例建议**:
- 单元测试：70%
- 集成测试：20%
- E2E 测试：10%

---

## 后端测试（Java）

### 测试框架

- **JUnit 5**: 测试框架
- **Mockito**: Mock 框架
- **AssertJ**: 流式断言
- **Testcontainers**: 容器化集成测试
- **Spring Boot Test**: Spring 测试支持

### 单元测试

#### 1. 基础单元测试

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("创建用户 - 成功")
    void createUser_Success() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test", "test@example.com");
        User savedUser = User.builder()
            .id(1L)
            .name("test")
            .email("test@example.com")
            .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // When
        UserDTO result = userService.createUser(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("test");
        
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @DisplayName("创建用户 - 邮箱已存在")
    void createUser_EmailExists() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test", "test@example.com");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_EXISTS);
    }
}
```

#### 2. 参数化测试

```java
@ParameterizedTest
@ValueSource(strings = {"admin", "user", "test"})
@DisplayName("验证用户名 - 有效用户名")
void validateUsername_ValidUsernames(String username) {
    // When
    boolean isValid = userService.validateUsername(username);
    
    // Then
    assertThat(isValid).isTrue();
}

@ParameterizedTest
@CsvSource({
    "ab, too_short",
    "very_long_username_over_50_characters, too_long",
    "user@name, invalid_characters"
})
@DisplayName("验证用户名 - 无效用户名")
void validateUsername_InvalidUsernames(String username, String reason) {
    // When
    boolean isValid = userService.validateUsername(username);
    
    // Then
    assertThat(isValid).isFalse();
}
```

#### 3. 异常测试

```java
@Test
@DisplayName("删除用户 - 用户不存在")
void deleteUser_UserNotFound() {
    // Given
    Long userId = 999L;
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    
    // When & Then
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.deleteUser(userId)
    );
    
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
}
```

---

### 集成测试

#### 1. Spring Boot 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("获取用户列表 - 成功")
    void getUserList_Success() throws Exception {
        // When
        ResultActions result = mockMvc.perform(
            get("/api/v1/users")
                .param("page", "0")
                .param("size", "10")
        );
        
        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.content").isArray())
              .andExpect(jsonPath("$.totalElements").value(0));
    }
    
    @Test
    @DisplayName("创建用户 - 成功")
    void createUser_Success() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest("test", "test@example.com");
        
        // When
        ResultActions result = mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
        
        // Then
        result.andExpect(status().isCreated())
              .andExpect(jsonPath("$.name").value("test"));
    }
}
```

#### 2. Testcontainers 集成测试

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class UserServiceWithDatabaseTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private UserService userService;
    
    @Test
    @DisplayName("数据库集成测试 - 创建和查询用户")
    void createAndFindUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test", "test@example.com");
        
        // When
        UserDTO created = userService.createUser(request);
        UserDTO found = userService.getUser(created.getId());
        
        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test");
    }
}
```

---

### 测试覆盖率

#### 配置 JaCoCo

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

#### 生成覆盖率报告

```bash
# 运行测试并生成报告
./mvnw test jacoco:report

# 查看报告
# 打开 target/site/jacoco/index.html
```

**覆盖率要求**:
- 行覆盖率：≥ 80%
- 分支覆盖率：≥ 70%
- 核心业务：≥ 90%

---

## 前端测试（TypeScript + React）

### 测试框架

- **Vitest**: 测试运行器
- **@testing-library/react**: React 测试工具
- **@testing-library/jest-dom**: DOM 断言
- **MSW (Mock Service Worker)**: API Mock

### 组件测试

#### 1. 基础组件测试

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ServerCard from './ServerCard';

describe('ServerCard', () => {
  const mockServer = {
    id: 1,
    name: 'Test Server',
    status: 'running',
    playerCount: 5,
    maxPlayers: 10
  };

  it('渲染服务器信息', () => {
    // Arrange
    render(<ServerCard server={mockServer} />);
    
    // Act & Assert
    expect(screen.getByText('Test Server')).toBeInTheDocument();
    expect(screen.getByText('5/10')).toBeInTheDocument();
    expect(screen.getByText('运行中')).toBeInTheDocument();
  });

  it('调用重启回调', async () => {
    // Arrange
    const onRestart = vi.fn();
    render(<ServerCard server={mockServer} onRestart={onRestart} />);
    
    // Act
    const restartButton = screen.getByRole('button', { name: /重启/i });
    fireEvent.click(restartButton);
    
    // Assert
    expect(onRestart).toHaveBeenCalledWith(1);
    expect(onRestart).toHaveBeenCalledTimes(1);
  });

  it('显示加载状态', () => {
    // Arrange
    render(<ServerCard server={null} isLoading={true} />);
    
    // Assert
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });
});
```

#### 2. Hook 测试

```typescript
import { renderHook, act, waitFor } from '@testing-library/react';
import { useServerList } from './useServerList';

describe('useServerList', () => {
  it('加载服务器列表', async () => {
    // Arrange
    const mockServers = [
      { id: 1, name: 'Server 1' },
      { id: 2, name: 'Server 2' }
    ];
    
    global.fetch = vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockServers)
    });
    
    // Act
    const { result } = renderHook(() => useServerList());
    
    // Assert
    await waitFor(() => {
      expect(result.current.servers).toHaveLength(2);
    });
    
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('处理加载错误', async () => {
    // Arrange
    global.fetch = vi.fn().mockRejectedValue(new Error('Network error'));
    
    // Act
    const { result } = renderHook(() => useServerList());
    
    // Assert
    await waitFor(() => {
      expect(result.current.error).toBe('Network error');
    });
    
    expect(result.current.loading).toBe(false);
    expect(result.current.servers).toHaveLength(0);
  });
});
```

#### 3. 集成测试

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import ServerList from './ServerList';

// Mock Server
const server = setupServer(
  rest.get('/api/servers', (req, res, ctx) => {
    return res(
      ctx.json([
        { id: 1, name: 'Server 1', status: 'running' },
        { id: 2, name: 'Server 2', status: 'stopped' }
      ])
    );
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('ServerList Integration', () => {
  it('从 API 加载并显示服务器列表', async () => {
    // Arrange
    render(<ServerList />);
    
    // Act & Assert
    expect(screen.getByTestId('loading')).toBeInTheDocument();
    
    await waitFor(() => {
      expect(screen.getByText('Server 1')).toBeInTheDocument();
      expect(screen.getByText('Server 2')).toBeInTheDocument();
    });
    
    expect(screen.queryByTestId('loading')).not.toBeInTheDocument();
  });

  it('显示错误信息', async () => {
    // Arrange
    server.use(
      rest.get('/api/servers', (req, res, ctx) => {
        return res(ctx.status(500));
      })
    );
    
    render(<ServerList />);
    
    // Act & Assert
    await waitFor(() => {
      expect(screen.getByText(/加载失败/i)).toBeInTheDocument();
    });
  });
});
```

---

### E2E 测试

#### Playwright 配置

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:3000',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' }
    },
    {
      name: 'firefox',
      use: { browserName: 'firefox' }
    }
  ]
});
```

#### E2E 测试示例

```typescript
// e2e/server-management.spec.ts
import { test, expect } from '@playwright/test';

test.describe('服务器管理', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'password');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  });

  test('创建新服务器', async ({ page }) => {
    // 导航到服务器列表
    await page.click('text=服务器管理');
    await expect(page).toHaveURL('/servers');
    
    // 点击创建按钮
    await page.click('text=创建服务器');
    
    // 填写表单
    await page.fill('[name="serverName"]', 'E2E Test Server');
    await page.selectOption('[name="template"]', 'default');
    await page.fill('[name="maxPlayers"]', '10');
    
    // 提交
    await page.click('button[type="submit"]');
    
    // 验证创建成功
    await expect(page.locator('text=E2E Test Server')).toBeVisible();
  });

  test('重启服务器', async ({ page }) => {
    // 导航到服务器详情页
    await page.goto('/servers/1');
    
    // 点击重启按钮
    await page.click('button:has-text("重启")');
    
    // 确认对话框
    await page.click('button:has-text("确认")');
    
    // 验证状态变化
    await expect(page.locator('.status-restarting')).toBeVisible();
  });
});
```

---

## 测试最佳实践

### 1. 测试命名规范

```java
// ✅ 正确：方法_场景_预期结果
@Test
void createUser_ValidRequest_ReturnsCreatedUser() {}

@Test
void createUser_EmailExists_ThrowsBusinessException() {}

// ❌ 错误：模糊的命名
@Test
void testCreate() {}

@Test
void test1() {}
```

### 2. AAA 模式

```java
@Test
void testWithAAA() {
    // Arrange (准备)
    User user = new User("test");
    when(repository.save(user)).thenReturn(user);
    
    // Act (执行)
    UserDTO result = service.createUser(user);
    
    // Assert (断言)
    assertThat(result.getName()).isEqualTo("test");
}
```

### 3. 测试隔离

```java
// ✅ 正确：每个测试独立
@Test
void test1() {
    // 不依赖其他测试的状态
}

@Test
void test2() {
    // 不依赖 test1 的结果
}

// ❌ 错误：测试之间有依赖
@Test
void test1() {
    // 创建数据
}

@Test
void test2() {
    // 使用 test1 创建的数据 - 错误！
}
```

### 4. 避免过度 Mock

```java
// ✅ 正确：只 Mock 外部依赖
@Mock
private UserRepository userRepository;  // 数据库依赖

@InjectMocks
private UserService userService;  // 真实对象

// ❌ 错误：Mock 太多
@Mock
private UserService userService;  // 不应该 Mock 被测对象
```

---

## 运行测试

### 后端测试

```bash
# 运行所有测试
./mvnw test

# 运行特定测试类
./mvnw test -Dtest=UserServiceTest

# 运行特定包下的测试
./mvnw test -Dtest="com.iccuu.**.service.*Test"

# 运行并生成覆盖率报告
./mvnw test jacoco:report

# 跳过测试
./mvnw package -DskipTests
```

### 前端测试

```bash
# 运行所有测试
npm run test

# 监听模式
npm run test -- --watch

# 覆盖率
npm run test -- --coverage

# 运行特定测试文件
npm run test -- server-list.test.ts

# E2E 测试
npx playwright test

# E2E 带 UI
npx playwright test --ui
```

---

## 下一步

- 📖 [调试指南](004-debugging.md)
- 📖 [编码规范](003-coding-standards.md)
- 📖 [部署指南](../deployment/006-docker-guide.md)

---

**参考链接**:
- [JUnit 5 官方文档](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 官方文档](https://site.mockito.org/)
- [Testing Library 官方文档](https://testing-library.com/)
- [Playwright 官方文档](https://playwright.dev/)

**最后更新**: 2026-05-22
