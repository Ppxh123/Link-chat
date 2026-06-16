# LinkChat 项目全面解析

> 本文档为 Claude Code 新对话提供项目全局上下文，确保快速理解项目结构、核心逻辑和关键细节。

---

## 一、项目身份

**LinkChat** 是一个全栈即时通讯平台，采用前后端分离架构：
- **后端**：`linkchat-server/` — SpringBoot 3.3.0 + Java 17 + Maven
- **前端**：`linkchat-web/` — Vue 3 + TypeScript + Vite 5
- **基础设施**：MySQL 8.0 / Redis 7.x / MinIO / Nginx（Docker Compose 编排）

---

## 二、总体技术架构

### 2.1 服务端口

| 组件 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 静态资源 + API 代理 + WebSocket 代理 |
| SpringBoot | 8080 | REST API（context-path: `/api`）+ WebSocket（`/ws`） |
| Vite Dev Server | 3000 | 前端开发，proxy `/api`→8080, `/ws`→8080 |
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存/在线状态/离线消息/限流 |
| MinIO | 9000/9001 | 对象存储/管理控制台 |

### 2.2 请求链路

```
浏览器 → Nginx:80 → /api/* → SpringBoot:8080/api/*
                  → /ws/*  → SpringBoot:8080/api/ws/* (WebSocket)
                  → /*     → Vue SPA 静态文件

开发环境：
浏览器 → Vite:3000 → proxy /api → SpringBoot:8080
                  → proxy /ws  → SpringBoot:8080 (WebSocket)
```

---

## 三、数据库全部 9 张表

### 3.1 表结构与外键关系

```
user (用户表)
├── id BIGINT PK AUTO_INCREMENT
├── email VARCHAR(128) UNIQUE        — 登录账号
├── password VARCHAR(256)            — BCrypt 加密
├── nickname VARCHAR(64)
├── user_code VARCHAR(32) UNIQUE     — 10位数字唯一码，可搜索加好友
├── avatar_url VARCHAR(512)          — MinIO URL
├── signature VARCHAR(256)           — 个性签名
├── status TINYINT                   — 0=离线 1=在线 2=忙碌 3=隐身
├── last_online DATETIME
├── is_deleted TINYINT               — 逻辑删除
├── created_at / updated_at DATETIME
└── 外键：被 friend, group_chat, group_member, message, message_ack,
          offline_message, file_record, online_user 引用

friend (好友关系表)
├── id BIGINT PK
├── user_id BIGINT FK→user.id
├── friend_id BIGINT FK→user.id
├── status TINYINT      — 0=PENDING 1=ACCEPTED 2=REJECTED 3=DELETED
├── remark VARCHAR(64)  — 好友备注
├── created_at / updated_at
└── UNIQUE(user_id, friend_id)  ← 重要：不区分status！多状态共存需 upsert

group_chat (群聊表)
├── id BIGINT PK
├── name VARCHAR(128)
├── avatar_url VARCHAR(512)      — MinIO URL
├── owner_id BIGINT FK→user.id
├── announcement TEXT
├── member_count INT DEFAULT 1
├── is_muted TINYINT DEFAULT 0   — V2迁移新增：全员禁言
├── is_deleted TINYINT           — 0=正常 1=已解散
├── created_at / updated_at
└── 外键：被 group_member, message 引用

group_member (群成员表)
├── id BIGINT PK
├── group_id BIGINT FK→group_chat.id
├── user_id BIGINT FK→user.id
├── role TINYINT              — 0=OWNER 1=ADMIN 2=MEMBER
├── is_muted TINYINT DEFAULT 0 — V2迁移新增：成员禁言
├── nickname_in_group VARCHAR(64)
├── joined_at DATETIME
├── created_at
└── UNIQUE(group_id, user_id)

message (消息表)
├── id BIGINT PK                        — 雪花算法生成（非自增！）
├── sender_id BIGINT FK→user.id
├── receiver_id BIGINT FK→user.id NULL  — 单聊时有值，群聊为NULL
├── group_id BIGINT FK→group_chat.id NULL — 群聊时有值，单聊为NULL
├── message_type VARCHAR(16)            — TEXT/IMAGE/FILE/EMOJI/SYSTEM
├── content TEXT
├── file_url/file_name/file_size/file_mime
├── is_recalled TINYINT                 — 0=否 1=是
├── is_deleted TINYINT                  — 0=否 1=是
├── quoted_msg_id BIGINT               — 引用回复的消息ID
├── ack_status VARCHAR(16)             — SENT/DELIVERED/READ/FAILED
├── retry_count INT DEFAULT 0          — 重试次数（最大3）
├── created_at/updated_at
└── 索引：idx_sender_receiver, idx_group, idx_created_at, idx_quoted_msg

message_ack (消息ACK确认表)
├── id BIGINT PK
├── message_id BIGINT FK→message.id
├── user_id BIGINT FK→user.id
├── ack_type TINYINT           — 1=DELIVERED 2=READ
├── ack_time DATETIME
├── created_at
└── UNIQUE(message_id, user_id, ack_type)

offline_message (离线消息表)
├── id BIGINT PK
├── message_id BIGINT FK→message.id
├── receiver_id BIGINT FK→user.id
├── message_type/content/file_url — 消息快照
├── is_pushed TINYINT           — 0=未推送 1=已推送
├── created_at                  — 消息原始发送时间
├── pushed_at DATETIME          — 推送时间
└── 索引：idx_receiver_pushed, idx_message_id

file_record (文件记录表)
├── id BIGINT PK
├── uploader_id BIGINT FK→user.id
├── file_name/file_url/file_size/file_type/mime_type
├── message_id BIGINT           — 关联消息
├── created_at

online_user (在线用户表 - 预留)
├── id BIGINT PK
├── user_id BIGINT FK→user.id UNIQUE
├── status/server_node/client_ip/heartbeat_at/connected_at/disconnected_at
```

### 3.2 关键约束和陷阱

1. **`friend` 表的 `UNIQUE(user_id, friend_id)` 不区分 status**：这是多轮 Bug 的根源。同一个 (user_id, friend_id) 对只能存在一条记录，状态变更必须用 UPDATE，且 DELETE/REJECTED 状态的旧记录在重新添加时必须 UPDATE 而非 INSERT。`acceptFriend` 也需要检查反向记录是否存在。

2. **`message.id` 使用雪花算法**：不是自增主键。前端接收时 Jackson 序列化为字符串，需要 `Number()` 转换后比较。

3. **`user.user_code` 是 10 位随机数字**：在 `AuthServiceImpl.register()` 中用 `RandomUtil.randomNumbers(10)` 生成，循环检查唯一性。

---

## 四、WebSocket 通信详解

### 4.1 连接建立

```
端点：ws://host:port/api/ws/{token}
认证：JWT Token 通过 URL Path Param 传递
Spring Security：/ws/** 配置为 permitAll，认证在 onOpen 中手动校验
```

**连接流程** (`WebSocketServer.onOpen()`):
1. 验证 JWT Token → 失败则关闭连接（VIOLATED_POLICY）
2. 将 `(userId, session)` 存入 `sessionMap`（ConcurrentHashMap）
3. 更新心跳时间、调用 `onlineStatusService.userOnline(userId)`
4. 设置 Redis session key（120s TTL）
5. 推送离线消息 `pushOfflineMessages(userId, session)`

### 4.2 消息类型完整清单

| type | 方向 | 触发时机 | 处理逻辑 |
|------|------|---------|---------|
| `ping` | C→S | 每 30s 自动发送 | 更新心跳 + 返回 `pong` + Redis 续期 |
| `pong` | S→C | 收到 ping 后 | 客户端确认连接存活 |
| `message` | C→S | 用户发送消息 | 根据 `receiverId`/`groupId` 转发（单聊→目标用户，群聊→群成员广播） |
| `message` | S→C | 收到转发的消息 | 前端渲染到聊天界面 |
| `ack` | C→S | 客户端确认收到消息 | 更新 Redis ACK 状态 + 通知发送者 + 更新数据库已读状态 |
| `ack` | S→C | 服务端通知发送者 | 发送者更新消息气泡状态（已送达/已读） |
| `recall` | C→S | 用户撤回消息 | 查询消息→根据单聊/群聊范围通知相关用户 |
| `recall` | S→C | 通知撤回 | 前端替换消息气泡为"消息已撤回" |
| `typing` | C→S | 用户正在输入 | 转发 `typing` 状态给接收者 |
| `typing` | S→C | 显示对方输入中 | 前端显示"对方正在输入..." |
| `status_change` | S→C | 好友上下线 | `onlineStatusService` 触发，前端更新好友在线状态 |
| `friend_request` | S→C | 收到好友申请 | `FriendServiceImpl.addFriend()` 中调用 `WebSocketServer.sendMessage()` |
| `offline_msg` | S→C | 用户上线时 | 批量推送 Redis 队列中的离线消息 |

### 4.3 群聊消息广播的安全性（曾出严重 Bug）

```java
// broadcastGroupMessage() 正确实现（Bug #1 修复后）：
// 1. 查询 GroupMember 表获取群成员列表
// 2. 仅向 memberIds.contains(userId) 的在线用户发送
// 3. 排除发送者自身
// 
// 错误实现（修复前）：直接遍历 sessionMap 所有在线用户发送 → 任何在线用户都能收到！

// handleRecall() 正确实现（Bug #2 修复后）：
// 1. 查询 message 获取 groupId 和 receiverId
// 2. 群聊：仅向群成员广播 + 发送者自己
// 3. 单聊：仅通知发送者和接收者
```

### 4.4 Jackson 序列化注意事项

```java
// WebSocket 消息使用 Jackson ObjectMapper 序列化
// Long 类型字段（id, senderId, receiverId, groupId）会被序列化为 JSON 字符串
// 前端接收后必须 Number() 转换：Number(msg.senderId) === myId
// 而不是直接用 === 比较

// WebSocketServer.buildWsMessage() 使用 Map<String, Object> 构建消息
// 在 handleAck/handleTyping 等处理器中，显式将 Long 转为 String.valueOf()
```

---

## 五、REST API 完整清单

### 5.1 认证模块 `/api/auth`（公开接口）

| 方法 | 路径 | 说明 | 关键逻辑 |
|------|------|------|---------|
| POST | `/auth/register` | 注册 | BCrypt 加密密码、生成 10 位 user_code |
| POST | `/auth/login` | 登录 | 生成 JWT（含 tv=Token版本号），**不递增版本号**（支持多窗口） |
| PUT | `/auth/password` | 修改密码 | **递增 Token 版本号**，使所有旧 Token 失效 |

### 5.2 用户模块 `/api/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/profile` | 获取当前用户信息 |
| GET | `/user/profile/{userId}` | 获取指定用户信息（公开） |
| PUT | `/user/profile` | 更新个人资料 |
| POST | `/user/avatar` | 上传头像（multipart） |
| GET | `/user/search?keyword=` | 搜索用户（按邮箱/昵称/用户码） |

### 5.3 好友模块 `/api/friend`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/friend/add` | 发送好友申请（keyword: 邮箱或用户码） |
| GET | `/friend/requests` | 获取待处理的好友申请列表 |
| PUT | `/friend/accept/{friendId}` | 同意好友申请 |
| PUT | `/friend/reject/{friendId}` | 拒绝好友申请 |
| DELETE | `/friend/{friendId}` | 删除好友（软删除，status=DELETED） |
| GET | `/friend/list` | 获取好友列表（仅 status=ACCEPTED） |
| GET | `/friend/search?keyword=` | 搜索好友（内存过滤） |

### 5.4 消息模块 `/api/message`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/message/send` | 发送消息（持久化 + WebSocket 转发双路径） |
| GET | `/message/history` | 获取聊天历史（分页，按 created_at 排序） |
| PUT | `/message/recall/{messageId}` | 撤回消息（2分钟时限） |
| DELETE | `/message/{messageId}` | 删除消息 |
| POST | `/message/forward/{messageId}?targetId=` | 转发消息 |
| GET | `/message/search?peerId=&keyword=` | 搜索聊天记录 |
| PUT | `/message/read/{senderId}` | 标记已读（批量更新） |

### 5.5 群聊模块 `/api/group`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/group/create` | 创建群聊 |
| GET | `/group/{groupId}` | 获取群信息（需是成员） |
| DELETE | `/group/{groupId}` | 解散群（仅群主） |
| DELETE | `/group/{groupId}/leave` | 退出群聊 |
| POST | `/group/{groupId}/invite` | 邀请成员 |
| DELETE | `/group/{groupId}/member/{memberId}` | 踢出成员 |
| PUT | `/group/{groupId}/admin/{memberId}` | 设置管理员 |
| DELETE | `/group/{groupId}/admin/{memberId}` | 撤销管理员 |
| PUT | `/group/{groupId}/transfer/{newOwnerId}` | 转让群主 |
| PUT | `/group/{groupId}/name` | 修改群名称 |
| PUT | `/group/{groupId}/announcement` | 修改群公告 |
| POST | `/group/{groupId}/avatar` | 上传群头像 |
| PUT | `/group/{groupId}/mute-all` | 全员禁言/取消 |
| PUT | `/group/{groupId}/mute-member/{memberId}` | 成员禁言/取消 |
| GET | `/group/{groupId}/members` | 获取成员列表 |
| GET | `/group/list` | 获取用户的所有群聊 |

### 5.6 文件模块 `/api/file`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/file/upload` | 上传文件（MinIO，50MB限制） |
| POST | `/file/upload/avatar` | 上传头像 |

### 5.7 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误码在 `ResultCode.java` 中定义（如 USER_NOT_FOUND=1001, UNAUTHORIZED=401 等）。

---

## 六、安全架构详解

### 6.1 JWT Token 版本号机制（关键设计）

```
Redis Key: token:version:{userId} → 存储当前有效版本号

登录流程：
1. 读取 Redis token:version:{userId}（首次登录初始化为 1）
2. 生成 JWT，claim 中包含 "tv": tokenVersion
3. 登录不递增版本号 → 多窗口/多端共享同一版本号 ✅

改密流程：
1. 修改密码
2. redisUtils.increment("token:version:{userId}", 1) → 递增版本号
3. 所有旧 JWT 的 tv 落后于 Redis 版本号 → JwtAuthenticationFilter 拒绝 → 返回 401

验证流程（JwtAuthenticationFilter）：
1. 从 Authorization header 提取 Bearer token
2. 验证 JWT 签名和过期时间
3. 提取 tv claim，与 Redis token:version:{userId} 对比
4. 版本不匹配 → 不设置认证 → SecurityConfig 返回 401 JSON
```

### 6.2 认证过滤器路径匹配

```java
// SecurityConfig 配置：
// - /auth/** 放行（注册/登录）
// - /ws/** 放行（WebSocket 单独的 JWT 握手认证）
// - 其余全部需要认证

// JwtAuthenticationFilter.shouldNotFilter()：
// - 不拦截 /auth/**（使用 servletPath，不含 context-path）
// - 注意：context-path 是 /api，servletPath 已去除此前缀！
//   - 例如：/api/auth/login → servletPath = /auth/login

// 未认证返回：401 JSON（自定义 AuthenticationEntryPoint）
```

### 6.3 前端 Axios 拦截器

```typescript
// request.ts 响应拦截器：
// 401/403 → 清除 token + userInfo → ElMessage.warning → 跳转 /login
// 500+  → ElMessage.error("服务器异常")
// 其他   → ElMessage.error(error.message)

// 请求拦截器：
// 自动从 localStorage 读取 token 添加到 Authorization header
```

### 6.4 SecurityUtils 三种获取用户ID的方法

```java
getCurrentUserId()          // 可能返回 null（旧接口，部分 Controller 仍使用）
getCurrentUserIdRequired()  // 未认证时抛 BusinessException(UNAUTHORIZED)，FriendController/GroupController 使用
getCurrentUserIdSafely()    // 静态方法，可能返回 null，非注入场景使用
```

---

## 七、关键代码路径

### 7.1 发送消息的完整路径

```
1. 用户点击发送 → ChatInput.vue emit('send')
2. ChatWindow.vue handleSendMessage()
3. → messageApi.sendMessage(request) → POST /api/message/send
4. MessageController.sendMessage() → MessageServiceImpl.sendMessage()
   ├── 消息去重检查（Redis: msg:dedup:{senderId}:{targetId}:{contentHash}, 5s TTL）
   ├── 雪花算法生成 messageId
   ├── 持久化到 message 表
   ├── 如果接收者离线 → 存入 offline_message 表 + Redis 离线队列
   └── 通过 WebSocketServer 转发消息
       ├── 单聊：发送给 receiver 的 session
       └── 群聊：broadcastGroupMessage() → 查询群成员 → 仅向群内在线成员发送
5. 同时，REST 返回 MessageResponse → 前端 addMessage() 显示已发送消息
6. 接收者通过 WebSocket 收到 message → cacheMessage（跨会话缓存/未读计数）→ addMessage（当前会话）
7. 接收者自动发送 ack → WebSocketServer.handleAck()
   ├── 更新 Redis msg:ack:{messageId}
   ├── 通知发送者（ack WebSocket 消息）
   └── messageService.markAsRead() 批量更新数据库
```

### 7.2 好友添加的完整路径

```
1. 用户在搜索框输入邮箱/用户码 → POST /api/user/search → 显示搜索结果
2. 点击"添加" → friendStore.addFriend(keyword)
3. → POST /api/friend/add → FriendController.addFriend()
4. → FriendServiceImpl.addFriend()
   ├── userId == null → 抛 UNAUTHORIZED（防御性检查）
   ├── 查询目标用户（按 email 或 userCode）
   ├── 检查是否添加自己 → 抛 SELF_ADD_FORBIDDEN
   ├── 检查是否已是好友（status=ACCEPTED）→ 抛 ALREADY_FRIENDS
   ├── 检查是否存在旧记录（status=DELETED/REJECTED/PENDING）
   │   ├── 存在 → UPDATE status 为 PENDING
   │   └── 不存在 → INSERT 新记录
   └── WebSocket 通知目标用户 "friend_request"
5. 目标用户端：
   ├── WebSocket 收到 friend_request → friendStore.onNewFriendRequest()
   ├── 自动重新加载 pendingRequests
   └── ChatView 显示"好友请求(N)"，包含"接受"/"拒绝"按钮
6. 接受申请 → PUT /api/friend/accept/{friendId}
   ├── 更新原始 PENDING 记录 → ACCEPTED
   ├── 检查反向记录是否存在（Bug #37 修复：先查再决定 INSERT/UPDATE）
   │   ├── 存在 → UPDATE 状态为 ACCEPTED
   │   └── 不存在 → INSERT 反向 ACCEPTED 记录
   └── 双方好友列表刷新
```

### 7.3 WebSocket 断线重连流程

```
WebSocketClient.ts:
1. onclose 触发 → stopHeartbeat() + reconnect()
2. reconnect() 使用指数退避：1s → 2s → 4s → 8s → 最大 16s
3. 重连成功后 reconnectDelay 重置为 1s
4. 重连后自动 startHeartbeat()（30s 间隔发 ping）

后端：
1. onClose → 从 sessionMap 移除 → 移除心跳 → userOffline()
2. onError → 同样处理 + 更新在线状态（Bug #17 修复后）
```

### 7.4 页面刷新后用户信息恢复

```
1. 应用启动 → main.ts → createPinia() → useAuthStore 初始化
2. authStore 构造函数：
   ├── token = localStorage.getItem('token') || ''
   └── userInfo = localStorage.getItem('userInfo') JSON.parse || null
3. router.beforeEach → to.meta.auth && token → authStore.init()
4. init() → fetchUserInfo() → 拉取最新用户信息 → 写入 localStorage
5. 结果：刷新后立即有头像/昵称（从 localStorage），init 完成后更新到最新
```

---

## 八、Redis Key 命名规范

所有 Redis Key 前缀定义在 `Constants.java`：

| 前缀 | 格式 | 用途 | TTL |
|------|------|------|-----|
| `token:version:` | `token:version:{userId}` | Token 版本号 | 7天 |
| `online:status:` | `online:status:{userId}` | 在线状态 | 120s |
| `online:session:` | `online:session:{userId}` | 在线 session 标记 | 120s |
| `offline:msg:` | `offline:msg:{userId}` | 离线消息队列（List） | 推送后删除 |
| `msg:ack:` | `msg:ack:{messageId}` | 消息 ACK 状态 | 30分钟 |
| `msg:dedup:` | `msg:dedup:{senderId}:{targetId}:{hash}` | 消息去重 | 5秒 |
| `typing:` | `typing:{senderId}:{receiverId}` | 正在输入状态 | 10s |
| `ratelimit:` | `ratelimit:{ip}:{path}` | 限流 Token Bucket | - |

---

## 九、前端架构要点

### 9.1 路由表

| 路径 | 组件 | meta | 说明 |
|------|------|------|------|
| `/login` | LoginView.vue | guest: true | 已登录则跳转 `/` |
| `/register` | RegisterView.vue | guest: true | 已登录则跳转 `/` |
| `/` | ChatView.vue | auth: true | 主聊天界面，未登录跳转 `/login` |
| `/settings` | SettingsView.vue | auth: true | 个人设置页 |

### 9.2 Pinia Store 清单

| Store | 文件 | 关键状态 |
|-------|------|---------|
| `auth` | stores/auth.ts | token, userInfo, init(), login(), logout() |
| `chat` | stores/chat.ts | messages, currentPeerId, currentGroupId, unreadCounts, peerMessages |
| `friend` | stores/friend.ts | friends, pendingRequests, unreadRequestCount |
| `group` | stores/group.ts | groups |
| `theme` | stores/theme.ts | dark/light 切换 |

### 9.3 自定义设计系统 (Lc* 组件)

位于 `src/components/design/`，共 13 个组件：

`LcAvatar`, `LcBadge`, `LcButton`, `LcCheckbox`, `LcContextMenu`, `LcEmpty`, `LcInput`, `LcModal`, `LcSearchBox`, `LcSpinner`, `LcToast` (含 `.ts` 命令式调用), `LcTooltip`

设计 Token 在 `src/design-tokens/`：colors, radii, shadows, spacing, transitions, typography（均为 SCSS 变量）。

### 9.4 ChatView.vue 状态管理

`ChatView.vue` 是整个前端最复杂的组件（约 790 行），包含了：
- 好友列表/群聊列表/好友申请列表的渲染
- 添加好友弹窗（搜索 + 结果展示 + 发送申请）
- 好友资料弹窗
- 创建群聊弹窗（选好友 + 填名称）
- 群管理弹窗（改名称/公告/禁言/转让/解散/踢人/设管理员/邀请成员/退出）

所有弹窗状态都在 ChatView 的 `<script setup>` 中用 `ref()` 管理。

---

## 十、Bug 修复历史摘要

项目经过 **4 轮全面代码审查 + 修复**（2026-06-12），共 **37 个 Bug**：

### 最关键的修复（必须了解）

| Bug# | 问题 | 影响 | 修复方式 |
|------|------|------|---------|
| #1 | 群聊消息广播给所有在线用户 | 🔴 隐私泄露 | 改为仅向群成员广播 |
| #2 | 撤回通知广播给所有在线用户 | 🔴 隐私泄露 | 仅通知消息相关用户 |
| #3 | getGroupInfo 用群主ID查角色 | 🔴 权限全错 | 用当前用户ID查询 |
| #5 | 删除好友后无法重新添加 | 🔴 功能阻塞 | upsert 模式：存在则UPDATE |
| #12 | 改密后旧Token不失效 | 🔴 安全漏洞 | Token版本号机制 |
| #29 | 刷新后用户昵称消失 | 🔴 UX | localStorage + router init |
| #30 | 未认证返回403而非401 | 🔴 登录循环 | 自定义401 JSON响应 |
| #31 | 每次登录递增版本号 | 🔴 多窗口互踢 | 登录不递增，仅改密递增 |
| #34 | 好友申请无查询接口 | 🔴 功能缺失 | 新增 API + WebSocket 通知 |
| #37 | acceptFriend 唯一键冲突 | 🔴 崩溃 | 先查反向记录再决定INSERT/UPDATE |

完整日志见 [BUG_FIX_LOG.md](../BUG_FIX_LOG.md)

---

## 十一、配置要点

### 11.1 application.yml 关键配置

```yaml
server.port: 8080
server.servlet.context-path: /api           # 所有 REST API 前缀
jwt.secret: (HMAC-SHA256 密钥)
jwt.expiration: 86400000                    # 24小时
minio.endpoint: http://localhost:9000
minio.bucket-name: linkchat
mybatis-plus.configuration.map-underscore-to-camel-case: true  # 驼峰映射
```

### 11.2 开发环境

- **IDE**: VSCode（有 `.vscode/settings.json`）
- **Java**: JDK 21（`C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot`）—— pom.xml 声明 java.version=17，但实际运行在 JDK 21
- **本地工具路径**: `C:\Users\彭勋豪\linkchat-tools\`（Redis + MinIO 可执行文件）
- **启动脚本**: `start-services.bat` 先启动 Redis → MinIO → 后端 jar

### 11.3 Docker Compose

包含 5 个服务：mysql, redis, minio, server（SpringBoot）, web（Nginx+Vue）
- MySQL 自动执行 `db/migration/` 下的 SQL 文件
- 依赖健康检查（healthcheck），按顺序启动
- 网络：`linkchat-net`（bridge）

---

## 十二、测试

### 后端测试

- 框架：JUnit 5 + Spring Boot Test
- 数据库：H2 内存数据库（`application-test.yml` 配置）
- 测试类（4个）：
  - `BaseTest.java` — 测试基类（@SpringBootTest）
  - `BusinessExceptionTest.java` — 异常测试
  - `ResultTest.java` — 响应体测试
  - `JwtTokenProviderTest.java` — JWT 生成/验证测试
  - `SecurityIntegrationTest.java` — 安全集成测试
- 运行：`mvn test`

### 前端测试

- 框架：Vitest + jsdom
- 已有：`src/stores/__tests__/auth.test.ts`
- 运行：`npm test`

---

## 十三、已知限制和设计债务

1. **转发功能不区分目标类型**：`MessageController.forwardMessage` 的 `targetId` 为 Long，不区分转发到用户还是群（当前仅支持用户到用户转发）。

2. **好友搜索为内存过滤**：`FriendServiceImpl.searchFriends()` 先加载所有 ACCEPTED 好友，再在内存中过滤。好友数量少时无影响。

3. **HeartbeatManager 使用静态 Map**：由于 JSR-356 为每个 WebSocket 连接创建新实例，静态字段是跨实例共享的唯一方式。这不是 Bug，是架构要求。

4. **WebSocket 双重实例化风险已修复**（Bug #19）：移除了显式 `@Bean`，仅保留 `@Component` + `ServerEndpointExporter`。

5. **群聊禁言功能仅后端校验**：前端未完全限制禁言状态下的消息发送按钮。

6. **消息已读状态**：使用批量 UPDATE 标记所有未读消息为已读（Bug #16 修复），而非逐条标记。

---

## 十四、给新对话的指引

### 如果收到后端的开发任务：
1. 先查看对应的 Controller → Service → ServiceImpl 调用链
2. 注意 `SecurityUtils` 选择正确的获取用户ID方法（Required vs 可能null）
3. 注意 context-path 是 `/api`，servletPath 已去除此前缀
4. 涉及数据库操作注意 `friend` 表的 UNIQUE 约束和 upsert 模式

### 如果收到前端的开发任务：
1. 所有 API 请求通过 `src/api/` 下的模块调用
2. WebSocket 消息通过 `WebSocketClient.ts` 的 `handleMessage()` 分发
3. ChatView.vue 非常巨大，优先考虑拆分组件
4. 注意 Long 类型的字符串序列化问题

### 如果需要修改配置：
1. 应用配置：`linkchat-server/src/main/resources/application.yml`
2. 数据库架构：`linkchat-server/src/main/resources/db/migration/`（新增迁移文件）
3. 前端环境变量：`linkchat-web/.env.development` / `.env.production`
4. Docker 编排：根目录 `docker-compose.yml`

---

> **最后更新**：2026-06-16  
> **项目版本**：linkchat-server 1.0.0 / linkchat-web 1.0.0
