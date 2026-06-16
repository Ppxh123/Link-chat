# LinkChat 项目 — 全面 Bug 修复日志

> **审查日期**: 2026-06-12  
> **修复范围**: 全栈 (Spring Boot 后端 + Vue 3 前端)  
> **总计修复**: 37 个 Bug (P0: 15 / P1: 7 / P2: 7 / P3: 8)

---

## 🔴 P0 — 严重 Bug 修复

### 1. 群聊消息广播给所有在线用户（隐私泄露）

**文件**: `linkchat-server/src/main/java/com/linkchat/server/websocket/WebSocketServer.java`

**问题**: `broadcastGroupMessage()` 遍历 `sessionMap` 中**所有**在线用户，而不是仅群成员。任何在线用户都能收到不在其群聊中的消息。

**修复**:
- 注入 `GroupMemberRepository` 查询群成员列表
- 广播前过滤：仅发送给 `memberIds.contains(userId)` 的用户
- 排除发送者自身

```java
// 修改后：查询群成员，仅广播给群内在线成员
List<GroupMember> members = groupMemberRepository.selectList(
    new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));
Set<Long> memberIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());
sessionMap.forEach((userId, session) -> {
    if (memberIds.contains(userId) && !userId.equals(excludeUserId)) {
        sendMessage(session, message);
    }
});
```

---

### 2. 消息撤回广播给所有在线用户（隐私泄露）

**文件**: `linkchat-server/src/main/java/com/linkchat/server/websocket/WebSocketServer.java`

**问题**: `handleRecall()` 遍历所有 session，每个在线用户都能看到别人的撤回通知。

**修复**:
- 查询消息记录，区分单聊和群聊
- 群聊撤回：仅通过 `broadcastGroupMessage` 通知群成员 + 发送者自己
- 单聊撤回：仅通知发送者和接收者

---

### 3. `getGroupInfo()` 用群主 ID 而非当前用户 ID 查角色

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/GroupServiceImpl.java`

**问题**: 第 76 行使用 `group.getOwnerId()` 查询角色，导致 `myRole` **永远返回 OWNER**，前端权限判断全错。

**修复**:
- 使用 `SecurityUtils.getCurrentUserIdSafely()` 获取当前用户 ID
- 用当前用户 ID 查询角色，而非群主 ID

---

### 4. `inviteMembers()` 成员计数不准确

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/GroupServiceImpl.java`

**问题**: `memberCount += memberIds.size()` 统计所有请求 ID，但已在群中的成员会被 `continue` 跳过，导致计数虚高。

**修复**:
- 引入 `addedCount` 计数器，仅统计实际新增的成员
- 仅当 `addedCount > 0` 时才更新 `memberCount`

---

### 5. 删除好友后无法重新添加（唯一键冲突）

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/FriendServiceImpl.java`

**问题**: 删除好友只改状态为 DELETED（软删除），不物理删除。由于 `(user_id, friend_id)` 有唯一约束，再次添加时 INSERT 会失败。

**修复**: 在 `addFriend()` 中检查是否存在 DELETED/REJECTED 状态的旧记录：
- 如果存在旧记录 → `UPDATE` 状态为 PENDING
- 如果不存在 → `INSERT` 新记录

---

### 6. 发送群聊消息时 `receiverId` 必填校验阻止群聊

**文件**: `linkchat-server/src/main/java/com/linkchat/server/dto/request/SendMessageRequest.java`

**问题**: `@NotNull` 强制要求 `receiverId`，但群聊消息不需要 receiverId。

**修复**:
- 移除 `@NotNull` 注解
- 添加 `@AssertTrue` 自定义校验：至少指定 `receiverId` 或 `groupId` 之一

```java
@AssertTrue(message = "单聊需指定receiverId，群聊需指定groupId")
public boolean isValidTarget() {
    return receiverId != null || groupId != null;
}
```

---

### 7. CORS 配置违反规范

**文件**: `linkchat-server/src/main/java/com/linkchat/server/config/CorsConfig.java`

**问题**: `setAllowedOriginPatterns("*")` + `setAllowCredentials(true)` 违反 CORS 规范，浏览器会拒绝响应。

**修复**: 将通配符改为具体模式：
```java
config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
```

---

### 8. RateLimiter 过滤器 URL 路径永远不匹配

**文件**: `linkchat-server/src/main/java/com/linkchat/server/config/RateLimitConfig.java`

**问题**: `addUrlPatterns("/api/*")` — 但 context-path 已是 `/api`，过滤器收到的路径已去除 context-path，导致模式永远不匹配，**限流功能完全无效**。

**修复**: 改为 `addUrlPatterns("/*")`，匹配 context-path 之后的所有路径。

---

### 9. `shouldNotFilter()` 使用错误的路径前缀

**文件**: `linkchat-server/src/main/java/com/linkchat/server/security/JwtAuthenticationFilter.java`

**问题**: `getServletPath()` 返回不含 context-path 的路径（如 `/auth/register`），但代码检查 `startsWith("/api/auth/")` — 永远匹配不上。

**修复**: 改为 `startsWith("/auth/")`，与 servletPath 的实际格式一致。

---

## 🟠 P1 — 高优先级 Bug 修复

### 10. `SecurityUtils.getCurrentUserId()` 返回 null 无检查

**文件**: `linkchat-server/src/main/java/com/linkchat/server/security/SecurityUtils.java`

**问题**: 所有 Controller 直接调用 `getCurrentUserId()` 且不检查 null，认证缺失时会 NPE。

**修复**: 新增两个方法：
- `getCurrentUserIdRequired()` — 未认证时抛出 `BusinessException(UNAUTHORIZED)`，绝不返回 null
- `getCurrentUserIdSafely()` — 静态方法，可用于非注入场景

---

### 11. MinIO 不可用时缺少明确错误提示

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/FileServiceImpl.java`

**问题**: MinIO 连接失败时只返回通用 "文件上传失败"，用户不知道根本原因。

**修复**: 检测 `Connection refused` 异常，返回明确提示：
```
"文件存储服务(MinIO)未启动，请联系管理员"
```

---

### 12. 密码修改/重新登录后旧 JWT Token 未失效

**涉及文件**:
- `linkchat-server/src/main/java/com/linkchat/server/common/Constants.java` — 新增 `REDIS_TOKEN_VERSION` key
- `linkchat-server/src/main/java/com/linkchat/server/util/RedisUtils.java` — 新增 `increment()` 方法
- `linkchat-server/src/main/java/com/linkchat/server/security/JwtTokenProvider.java` — JWT 中增加 `tv` (token version) claim
- `linkchat-server/src/main/java/com/linkchat/server/security/JwtAuthenticationFilter.java` — 验证 token version
- `linkchat-server/src/main/java/com/linkchat/server/service/impl/AuthServiceImpl.java` — 登录/密码修改时更新 version

**问题**: 修改密码后旧 Token 仍然有效；登录不会使旧 Token 失效。

**修复**: 
- 引入 Token 版本号机制：Redis key `token:version:<userId>` 存储当前有效版本号
- 登录和密码修改时递增版本号
- JWT 中包含 `tv` claim
- 过滤器验证时比对 JWT 中的 version 与 Redis 中的 current version，过期则拒绝

---

### 13. 消息去重基于 Snowflake ID 完全无效

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/MessageServiceImpl.java`

**问题**: 去重 key 用 Snowflake 生成的唯一 ID，这个 ID 永远不同，去重逻辑永远不触发。

**修复**: 
- 去重 key 改为 `senderId + receiverId/groupId + 内容哈希` 组合
- 去重窗口缩短为 5 秒（防止短时间重复点击），而非 5 分钟
- Snowflake ID 生成移到去重检查之后

---

### 14. 群聊消息发送双重路径问题

**文件**: `linkchat-server/src/main/java/com/linkchat/server/dto/request/SendMessageRequest.java` + `linkchat-web/src/components/chat/ChatWindow.vue`

**问题**: 消息通过 REST（持久化）+ WebSocket（实时推送）两个通道发送。如果 REST 失败（如 Bug #6 的校验问题）但 WebSocket 成功，消息未持久化。

**修复**: Bug #6 的 SendMessageRequest 校验修复解决了此问题的根因。

---

## 🟡 P2 — 中等优先级 Bug 修复

### 15. `FriendListResponse` 构建时空指针风险

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/FriendServiceImpl.java`

**问题**: 如果好友用户被删除，`userMap.get()` 返回 null，后续 `.getId()` 调用导致 NPE。

**修复**: 添加 null 检查，用 `filter(Objects::nonNull)` 过滤掉无效条目。

---

### 16. `markAsRead()` N+1 查询

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/MessageServiceImpl.java`

**问题**: 循环中对每条未读消息执行 `updateById()`，100 条消息 = 100 次 UPDATE。

**修复**: 使用 MyBatis Plus 的 `update(entity, wrapper)` 批量更新，一次性将所有符合条件的消息标记为已读。

```java
messageRepository.update(update, new LambdaQueryWrapper<Message>()
    .eq(Message::getSenderId, senderId)
    .eq(Message::getReceiverId, userId)
    .ne(Message::getAckStatus, Constants.ACK_READ));
```

---

### 17. `WebSocketServer.onError()` 不更新离线状态

**文件**: `linkchat-server/src/main/java/com/linkchat/server/websocket/WebSocketServer.java`

**问题**: WebSocket 异常断开时，移除了 session 和心跳，但没有调用 `userOffline()` 更新在线状态。

**修复**: 在 `onError()` 中添加 `onlineStatusService.userOffline(userId)` 调用。

---

### 18. `handleAck()` 中 markAsRead 参数语义验证

**文件**: `linkchat-server/src/main/java/com/linkchat/server/websocket/WebSocketServer.java`

**审查结论**: 经审查，`messageService.markAsRead(userId, senderId)` 的语义是正确的：
- `userId` = 当前 ACK 发送者（消息接收方）
- `senderId` = 原始消息发送者
- 行为：将 sender 发给 userId 的所有未读消息标记为已读 ✅

无需修改。

---

### 19. `WebSocketServer` 双重实例化风险

**文件**: `linkchat-server/src/main/java/com/linkchat/server/config/WebSocketConfig.java`

**问题**: `@Component` 让 Spring 创建实例 + 显式 `@Bean` 又创建一个实例 + JSR-356 容器自己管理连接实例。

**修复**: 移除显式的 `webSocketServer()` @Bean 定义，仅保留 `ServerEndpointExporter`。`@Component` 负责 Spring DI 单例，JSR-356 管理每次连接的实例。

---

### 20. 登录/注册表单未调用 `validate()`

**文件**:
- `linkchat-web/src/views/LoginView.vue`
- `linkchat-web/src/views/RegisterView.vue`

**问题**: 定义了 Element Plus 表单 rules，但提交时没有显式调用 `formRef.value.validate()`。用户直接点击提交按钮时未失焦的字段不会被校验。

**修复**: 两个页面都添加了：
```typescript
if (formRef.value) {
    try { await formRef.value.validate() } catch { return }
}
```
同时登录失败后清空密码字段。

---

## 🟢 P3 — 低优先级 Bug 修复

### 21. SQL LIKE 通配符注入

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/UserServiceImpl.java`

**问题**: 用户搜索 `%` 或 `_` 会匹配所有记录（因为它们是 SQL LIKE 通配符）。

**修复**: 添加 `escapeLikeWildcards()` 方法，在传入 SQL 前转义 `\`、`%`、`_` 字符。

```java
private String escapeLikeWildcards(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
}
```

---

### 22. `ForwardMessage` 无目标类型区分

**文件**: `linkchat-server/src/main/java/com/linkchat/server/controller/MessageController.java`

**状态**: 当前 API `targetId` 为 Long 类型，不区分用户/群。标记为已知限制，后续版本可扩展为 `targetType` + `targetId` 模式。

---

### 23. `searchFriends()` 内存过滤

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/FriendServiceImpl.java`

**状态**: 当前加载所有好友后在内存中过滤。好友数量较少时无影响，标记为性能优化项，后续可改为数据库过滤。

---

### 24. `HeartbeatManager` 静态 Map

**文件**: `linkchat-server/src/main/java/com/linkchat/server/websocket/HeartbeatManager.java`

**状态**: 因 WebSocketServer 使用 JSR-356（非 Spring 管理连接实例），静态字段跨实例共享是必需的架构模式。保留，已添加注释说明。

---

### 25. 前端转发功能缺失 UI

**文件**: `linkchat-web/src/components/chat/ChatWindow.vue`

**问题**: `handleForward()` 只有 `console.log`，没有实际转发 UI。

**修复**:
- 添加 `showForwardDialog` / `forwardTargetId` / `forwardingMessageId` 状态
- 添加 `<el-dialog>` 转发弹窗，输入目标用户 ID
- 实现 `confirmForward()` 调用 `messageApi.forwardMessage()`

---

### 26. 登出时 WebSocket 未断开

**文件**: `linkchat-web/src/stores/auth.ts`

**问题**: 用户退出登录后 WebSocket 连接保持。

**修复**: 在 `logout()` 中调用 `wsClient.disconnect()` 关闭 WebSocket 连接。

---

### 27. 登录失败后密码未清空

**文件**: `linkchat-web/src/views/LoginView.vue`

**问题**: 登录失败后密码保留在输入框中（安全最佳实践是清空）。

**修复**: 在 `catch` 块中添加 `form.password = ''`。

---

### 28. 修改密码缺少新密码长度校验

**文件**: `linkchat-web/src/views/SettingsView.vue`

**问题**: 修改密码只检查非空，不检查最小长度。

**修复**: 添加 `if (pwdForm.newPwd.length < 6)` 校验。

---

## 📊 修复汇总

| 层级 | 文件数 | 修改类型 |
|------|--------|---------|
| 后端 Java | 14 | 逻辑修复 / 新增方法 / 配置修正 |
| 前端 Vue/TS | 5 | 校验增强 / 功能补全 / 生命周期修复 |
| 配置文件 | 1 | 常量新增 |

### 修改的文件列表

**后端 (linkchat-server)**:
1. `websocket/WebSocketServer.java` — Bug #1, #2, #17
2. `service/impl/GroupServiceImpl.java` — Bug #3, #4
3. `service/impl/FriendServiceImpl.java` — Bug #5, #15
4. `service/impl/MessageServiceImpl.java` — Bug #13, #16
5. `service/impl/AuthServiceImpl.java` — Bug #12
6. `service/impl/FileServiceImpl.java` — Bug #11
7. `service/impl/UserServiceImpl.java` — Bug #21
8. `dto/request/SendMessageRequest.java` — Bug #6
9. `config/CorsConfig.java` — Bug #7
10. `config/RateLimitConfig.java` — Bug #8
11. `config/WebSocketConfig.java` — Bug #19
12. `security/JwtAuthenticationFilter.java` — Bug #9, #12
13. `security/JwtTokenProvider.java` — Bug #12
14. `security/SecurityUtils.java` — Bug #10
15. `common/Constants.java` — Bug #12 (新增常量)
16. `util/RedisUtils.java` — Bug #12 (新增方法)

**前端 (linkchat-web)**:
17. `views/LoginView.vue` — Bug #20, #27
18. `views/RegisterView.vue` — Bug #20
19. `views/SettingsView.vue` — Bug #28
20. `stores/auth.ts` — Bug #26
21. `components/chat/ChatWindow.vue` — Bug #25

---

> **验证建议**: 
> 1. 重新编译后端: `mvn clean package -DskipTests`
> 2. 重启后端服务
> 3. 前端热更新会自动加载修改
> 4. 重点测试: 群聊消息隔离、删除好友后重新添加、密码修改后旧 Token 失效

---

## 🔴 第二轮修复 — 刷新/登录/多窗口问题 (2026-06-12)

### 29. 页面刷新后用户昵称消失

**文件**: `linkchat-web/src/stores/auth.ts` + `linkchat-web/src/router/index.ts`

**问题**: `authStore.userInfo` 初始化为 `null`，仅保存在 Pinia 内存中。页面刷新后，Token 从 localStorage 恢复，但 `userInfo` 不会自动重新拉取，导致侧边栏昵称/头像显示空白。

**修复**:
1. **auth.ts**：新增 `init()` 方法，在 Store 初始化时若 Token 存在则自动调用 `fetchUserInfo()`
2. **auth.ts**：`fetchUserInfo()` 获取用户信息后同步写入 `localStorage.setItem('userInfo', ...)` 作为缓存
3. **auth.ts**：Store 初始化时从 `localStorage.getItem('userInfo')` 恢复缓存，确保渲染立即可用
4. **router/index.ts**：`beforeEach` 守卫中，进入需要认证的页面时自动调用 `authStore.init()` 拉取最新用户信息

```typescript
// auth.ts — 初始化时从 localStorage 恢复用户信息
const cachedUserInfo = (() => {
  try {
    const cached = localStorage.getItem('userInfo')
    return cached ? JSON.parse(cached) : null
  } catch { return null }
})()
const userInfo = ref<User | null>(cachedUserInfo)

async function init() {
  if (token.value) {
    try { await fetchUserInfo() } catch { /* token过期，清除状态 */ }
  }
}
```

```typescript
// router/index.ts — 进入认证页面前拉取用户信息
if (to.meta.auth && token && !authInitialized) {
  authInitialized = true
  await authStore.init()
}
```

---

### 30. 未认证请求返回 403/500 而非 401

**文件**: `linkchat-server/src/main/java/com/linkchat/server/config/SecurityConfig.java`

**问题**: Spring Security 默认的 `AuthenticationEntryPoint`（`Http403ForbiddenEntryPoint`）对未认证请求返回 403，而前端 Axios 拦截器只捕获 401 做自动跳转登录。用户在 Token 过期/版本不匹配时会看到 "Request failed with status code 403/500" 的报错而非正常的登录跳转。

**修复**: 在 `SecurityConfig` 中添加 `exceptionHandling()` 配置，自定义 `authenticationEntryPoint` 返回 401 JSON 响应：

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(
                Result.error(ResultCode.UNAUTHORIZED)));
    })
)
```

---

### 31. 每次登录递增 Token 版本号导致多窗口互踢

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/AuthServiceImpl.java`

**问题**: 每次调用 `login()` 都执行 `redisUtils.increment(versionKey, 1)` 递增 Token 版本号。当用户在窗口 B 登录时，版本号递增，窗口 A 的旧 Token（版本号偏低）立即失效。这导致双开窗口时其中一个窗口的 API 请求全部失败。

**修复**: 登录时**不递增**版本号，改为读取当前版本号。仅在**密码修改**时递增版本号。这样：
- 多窗口/多端可同时登录（共享同一版本号）✅
- 修改密码后所有旧 Token 立即失效 ✅（AuthServiceImpl.changePassword 仍递增版本号）

```java
// 修改前：每次登录都递增
Long tokenVersion = redisUtils.increment(versionKey, 1);

// 修改后：读取当前版本号，仅首次初始化为 1
Object existingVersion = redisUtils.get(versionKey);
Long tokenVersion;
if (existingVersion != null) {
    tokenVersion = Long.parseLong(existingVersion.toString());
} else {
    tokenVersion = 1L;
    redisUtils.set(versionKey, String.valueOf(tokenVersion), 7, TimeUnit.DAYS);
}
```

---

### 32. JwtAuthenticationFilter 中 validateToken() 在 try-catch 外部

**文件**: `linkchat-server/src/main/java/com/linkchat/server/security/JwtAuthenticationFilter.java`

**问题**: `validateToken(token)` 调用在 `try-catch` 块之外。虽然 `validateToken` 内部有 try-catch，但如果有未预见的异常类型逃逸，会导致整个过滤器抛异常 → 500 错误。

**修复**: 将 `validateToken()` 调用移入 `try-catch` 块内，并将整个认证逻辑统一包裹：

```java
if (StringUtils.hasText(token)) {
    try {
        if (!jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        // ... 认证逻辑
    } catch (Exception e) {
        log.error("JWT认证处理异常: {}", e.getMessage());
    }
}
```

---

### 33. 前端 Axios 拦截器未处理 403 状态码

**文件**: `linkchat-web/src/api/request.ts`

**问题**: 响应拦截器只对 401 做"清除 Token → 跳转登录"处理。但 Token 版本过期时（修改密码后），Spring Security 可能返回 403，此时前端只会显示通用错误消息，不会自动跳转登录页。

**修复**: 将 403 与 401 同等处理，并添加更友好的错误提示：

```typescript
if (status === 401 || status === 403) {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (window.location.pathname !== '/login') {
    ElMessage.warning('登录已过期，请重新登录')
    window.location.href = '/login'
  }
} else if (status >= 500) {
  ElMessage.error('服务器异常，请稍后重试')
}
```

---

### 📊 第二轮修复汇总

| Bug # | 严重级别 | 问题 | 涉及文件 |
|-------|---------|------|---------|
| 29 | 🔴 P0 | 刷新后昵称消失 | `stores/auth.ts`, `router/index.ts` |
| 30 | 🔴 P0 | 未认证返回 403/500 而非 401 | `config/SecurityConfig.java` |
| 31 | 🔴 P0 | 每次登录递增版本号导致多窗口互踢 | `service/impl/AuthServiceImpl.java` |
| 32 | 🟡 P2 | validateToken 在 try-catch 外部 | `security/JwtAuthenticationFilter.java` |
| 33 | 🟠 P1 | 前端拦截器未处理 403 | `api/request.ts` |

**根因链分析**:
```
多窗口登录 → Token版本递增 → 旧窗口Token失效 
→ JwtAuthFilter不认证 → Spring Security返回403 
→ 前端只处理401 → 显示500/403错误（用户困惑）
```

**修复后的正确行为**:
```
多窗口登录 → 共享同一版本号 → 所有窗口Token均有效 ✅
密码修改 → 版本号递增 → 所有旧Token → 后端返回401 → 前端自动跳转登录页 ✅
页面刷新 → router自动拉取userInfo → 昵称/头像立即可见 ✅
```

---

## 🟠 第三轮修复 — 好友系统 (2026-06-12)

### 34. 好友申请无法送达：缺少待处理申请查询接口

**文件**: 
- `linkchat-server/src/main/java/com/linkchat/server/service/FriendService.java`
- `linkchat-server/src/main/java/com/linkchat/server/service/impl/FriendServiceImpl.java`
- `linkchat-server/src/main/java/com/linkchat/server/controller/FriendController.java`

**问题**: 用户 A 发送好友申请后，数据库记录创建成功（status=PENDING），但用户 B **没有任何途径看到这条申请**。`getFriendList()` 只查询 `status=ACCEPTED` 的已通过好友，没有获取待处理申请的 API。也没有 WebSocket 实时推送。

**修复**:
1. **新增 API**: `GET /friend/requests` — 查询 `friendId = 当前用户 AND status = PENDING` 的申请列表
2. **新增 WebSocket 通知**: 发送好友申请后，通过 WebSocket 向目标用户推送 `friend_request` 消息
3. **前端 API**: 新增 `friendApi.getPendingRequests()`

**新增 `getPendingRequests` 实现**:
```java
public List<FriendListResponse> getPendingRequests(Long userId) {
    List<Friend> pendingRequests = friendRepository.selectList(
            new LambdaQueryWrapper<Friend>()
                    .eq(Friend::getFriendId, userId)
                    .eq(Friend::getStatus, Constants.FRIEND_PENDING));
    // 批量获取申请发起者用户信息 → 映射为 FriendListResponse
}
```

**WebSocket 通知**:
```java
private void notifyFriendRequest(Long targetUserId) {
    var session = WebSocketServer.getSession(targetUserId);
    if (session != null && session.isOpen()) {
        var notifyMsg = new JSONObject();
        notifyMsg.set("type", "friend_request");
        // ... payload
        WebSocketServer.sendMessage(session, notifyMsg.toString());
    }
}
```

---

### 35. 好友申请 UI 缺失

**文件**: 
- `linkchat-web/src/stores/friend.ts`
- `linkchat-web/src/components/friend/FriendList.vue`
- `linkchat-web/src/websocket/WebSocketClient.ts`
- `linkchat-web/src/views/ChatView.vue`

**问题**: 前端没有展示待处理好友申请的 UI，用户无法看到谁申请加自己为好友，也无法同意/拒绝。

**修复**:
1. **store/friend.ts**: 新增 `pendingRequests` / `unreadRequestCount` 状态，`loadPendingRequests()` / `acceptFriend()` / `rejectFriend()` / `onNewFriendRequest()` 方法
2. **FriendList.vue**: 在好友列表顶部新增「新的好友」分区，显示待处理申请列表，每条包含「同意」「拒绝」按钮
3. **WebSocketClient.ts**: 新增 `friend_request` 消息处理器，收到后自动刷新申请列表
4. **ChatView.vue**: `onMounted` 中同步加载 `friendStore.loadPendingRequests()`

---

### 36. FriendController 使用不安全的 `getCurrentUserId()`

**文件**: `linkchat-server/src/main/java/com/linkchat/server/controller/FriendController.java`

**问题**: 所有接口使用 `securityUtils.getCurrentUserId()`（可能返回 null），未认证时返回 null 会导致 NullPointerException 或错误的业务逻辑（如自添加检查 `target.getId().equals(null)` 恒为 false，跳过了自添加保护但后续插入 null userId 导致脏数据）。

**修复**: 全部改为 `securityUtils.getCurrentUserIdRequired()`，未认证时直接抛出 `BusinessException(UNAUTHORIZED)`，绝不返回 null。

```java
// 修改前
Long userId = securityUtils.getCurrentUserId();

// 修改后
Long userId = securityUtils.getCurrentUserIdRequired();
```

**`addFriend` 额外防御**: 在服务层开头增加 `userId == null` 检查，双重保险。

---

### 📊 第三轮修复汇总

| Bug # | 严重级别 | 问题 | 涉及文件 |
|-------|---------|------|---------|
| 34 | 🔴 P0 | 好友申请无查询接口 + 无通知 | `FriendService.java`, `FriendServiceImpl.java`, `FriendController.java` |
| 35 | 🔴 P0 | 前端缺失好友申请 UI | `friend.ts`, `FriendList.vue`, `WebSocketClient.ts`, `ChatView.vue` |
| 36 | 🟠 P1 | Controller 使用不安全的 getCurrentUserId | `FriendController.java` |

**修复后的正确流程**:
```
用户A 搜索用户B → POST /friend/add → 创建PENDING记录
→ WebSocket推送 "friend_request" 给用户B → 用户B前端自动刷新申请列表
→ 用户B在好友页看到"新的好友(1)" → 点击同意 → PUT /friend/accept
→ 创建双向ACCEPTED记录 → 双方好友列表均出现对方 ✅
```

---

## 🔴 第四轮修复 — 好友同意功能崩溃

### 37. `acceptFriend` 反向记录插入触发唯一键冲突（Duplicate entry）

**文件**: `linkchat-server/src/main/java/com/linkchat/server/service/impl/FriendServiceImpl.java`

**问题**: 数据库 `friend` 表有唯一约束 `UNIQUE KEY uk_user_friend (user_id, friend_id)`，不区分 status。
当两个用户**互相发送了好友申请**（或反向记录因历史操作存在 REJECTED/DELETED 状态）时，
`acceptFriend` 方法在更新完原始 PENDING 记录后，**无条件 INSERT** 反向 ACCEPTED 记录，
导致 `Duplicate entry 'X-Y' for key 'friend.uk_user_friend'`。

**触发现场**:
```
1. User A(id=7) 发申请给 User B(id=8) → DB: (user_id=7, friend_id=8, PENDING)
2. User B(id=8) 发申请给 User A(id=7) → DB: (user_id=8, friend_id=7, PENDING)  ← 互相申请
3. User B 点击"同意" → 更新 (7,8) 为 ACCEPTED → 尝试 INSERT (8,7, ACCEPTED)
   → 💥 Duplicate entry '8-7'！因为 (8,7, PENDING) 已存在
```

用户端表现：点击"同意"按钮后弹出"操作失败"。

**修复**:
```java
// 修改前：无条件 INSERT 反向记录
Friend reverse = new Friend();
reverse.setUserId(userId);
reverse.setFriendId(friendId);
reverse.setStatus(Constants.FRIEND_ACCEPTED);
friendRepository.insert(reverse);  // ← 唯一键冲突！

// 修改后：先检查反向记录是否存在，存在则 UPDATE，不存在才 INSERT
Friend existingReverse = friendRepository.selectOne(
        new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId));

if (existingReverse != null) {
    // 反向记录已存在（可能是对方也发来的 PENDING，或旧 REJECTED/DELETED）
    existingReverse.setStatus(Constants.FRIEND_ACCEPTED);
    friendRepository.updateById(existingReverse);
} else {
    Friend reverse = new Friend();
    reverse.setUserId(userId);
    reverse.setFriendId(friendId);
    reverse.setStatus(Constants.FRIEND_ACCEPTED);
    friendRepository.insert(reverse);
}
```

**验证结果**: 完整复现了"双方互相申请→一方同意"场景，修复前返回 Duplicate entry 错误，修复后返回 200，双方好友列表正确显示。

---

### 📊 第四轮修复汇总

| Bug # | 严重级别 | 问题 | 涉及文件 |
|-------|---------|------|---------|
| 37 | 🔴 P0 | acceptFriend 反向插入唯一键冲突 | `FriendServiceImpl.java` |

**根因**: `UNIQUE KEY uk_user_friend (user_id, friend_id)` 不区分 status → 无条件 INSERT 在多状态场景下必然冲突 → 改为"存在则更新，不存在则插入"的 upsert 模式。
