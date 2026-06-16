# LinkChat - 企业级即时通讯平台

> 基于 SpringBoot 3 + Vue 3 的全栈即时通讯系统，具备可靠消息机制、断线重连、离线消息等企业级特性。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | ^3.4 |
| 构建工具 | Vite | ^5.2 |
| 状态管理 | Pinia | ^2.1 |
| UI 组件库 | Element Plus | ^2.7 |
| 后端框架 | Spring Boot | 3.3.0 |
| 安全框架 | Spring Security + JWT (jjwt 0.12.5) | - |
| ORM | MyBatis Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 文件存储 | MinIO | latest |
| 工具库 | Hutool | 5.8.28 |
| 容器化 | Docker + Docker Compose | - |
| Web 服务器 | Nginx | 1.27 |

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│                     Nginx (80)                       │
│             静态资源 / API代理 / WS代理                │
└────────┬──────────────────────┬─────────────────────┘
         │                      │
    ┌────▼─────┐          ┌─────▼──────┐
    │  Vue3 SPA │          │ SpringBoot │
    │  (3000)   │          │  (8080)    │
    └───────────┘          └──┬──┬──┬──┘
                              │  │  │
                    ┌─────────┘  │  └─────────┐
                    │            │            │
               ┌────▼───┐  ┌────▼───┐  ┌─────▼──┐
               │ MySQL  │  │ Redis  │  │ MinIO  │
               │ (3306) │  │ (6379) │  │ (9000) │
               └────────┘  └────────┘  └────────┘
```

## 核心特性

### 可靠消息机制
- **ACK 确认机制**：客户端收到消息后返回 ACK（DELIVERED/READ），服务端标记投递状态
- **消息重发**：未收到 ACK 自动重试（最多 3 次，间隔 5s）
- **消息去重**：基于 senderId + receiverId/groupId + 内容哈希，5 秒去重窗口

### 实时通信
- WebSocket 长连接，JWT 握手认证（`/ws/{token}`）
- 心跳检测：30s 间隔 PING/PONG，90s 超时断开
- 断线重连：指数退避策略 (1s → 2s → 4s → 8s → 16s)
- 在线状态同步：Redis + MySQL 双重保障

### 离线消息
- 接收者离线时消息存入 Redis 队列 + MySQL `offline_message` 表
- 上线后自动推送所有离线消息
- 消息持久化保障不丢失

### 安全机制
- **JWT Token 版本号**：修改密码后递增版本号，所有旧 Token 立即失效
- **多窗口登录支持**：同一用户多窗口共享版本号，互不踢出
- **未认证返回 401**：自定义 AuthenticationEntryPoint，前端自动跳转登录
- **CORS 白名单**：仅允许 `localhost:*` 和 `127.0.0.1:*`

### 功能模块
| 模块 | 功能 |
|------|------|
| 用户系统 | 注册 / 登录 / JWT认证 / 个人资料修改 / 头像上传 / 用户搜索 |
| 好友系统 | 添加 / 同意 / 拒绝 / 删除 / 搜索 / 实时申请通知 |
| 单聊 | 文字 / 图片 / 文件 / 表情 / 正在输入状态 |
| 群聊 | 创建 / 解散 / 邀请 / 踢出 / 转让 / 管理员 / 全员禁言 / 成员禁言 |
| 消息管理 | 撤回（2分钟内）/ 删除 / 转发 / 搜索 / 已读标记 |
| 文件上传 | MinIO 存储，50MB 限制，支持 20+ 文件格式 |

## 项目结构

```
chat-1
├── .claude/                             # Claude Code 配置
│   ├── CLAUDE.md                        # LLM 行为准则
│   ├── README.md                        # 项目说明（即本文件）
│   └── PROJECT_OVERVIEW.md              # 项目全面解析（给新对话快速上手）
├── linkchat-server/                     # 后端项目
│   ├── src/main/java/com/linkchat/server
│   │   ├── common/                      # 通用组件
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   ├── Constants.java           # 常量定义（含 Redis Key 前缀）
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── Result.java              # 统一响应体
│   │   │   └── ResultCode.java          # 响应码枚举
│   │   ├── config/                      # 配置类
│   │   │   ├── CorsConfig.java          # CORS 白名单配置
│   │   │   ├── JacksonConfig.java       # Jackson 序列化配置
│   │   │   ├── MinioConfig.java         # MinIO 客户端配置
│   │   │   ├── MyBatisPlusConfig.java   # MyBatis Plus 分页配置
│   │   │   ├── RateLimitConfig.java     # Token Bucket 限流过滤器
│   │   │   ├── RedisConfig.java         # Redis 序列化配置
│   │   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   │   ├── SnowflakeConfig.java     # 雪花 ID 生成器配置
│   │   │   └── WebSocketConfig.java     # WebSocket 端点注册
│   │   ├── controller/                  # REST 控制器（7个）
│   │   │   ├── AuthController.java      # 注册/登录/改密
│   │   │   ├── FileController.java      # 文件上传
│   │   │   ├── FriendController.java    # 好友增删改查/申请
│   │   │   ├── GroupController.java     # 群管理全套
│   │   │   ├── MessageController.java   # 消息收发/撤回/转发
│   │   │   ├── OfflineMessageController.java
│   │   │   ├── OnlineStatusController.java
│   │   │   └── UserController.java      # 用户信息/搜索
│   │   ├── dto/request/                 # 请求 DTO（10个）
│   │   ├── dto/response/                # 响应 DTO（5个）
│   │   ├── entity/                      # 数据库实体（9张表）
│   │   ├── repository/                  # MyBatis Plus Mapper（9个）
│   │   ├── security/                    # 安全组件
│   │   │   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   │   │   ├── JwtTokenProvider.java         # JWT 生成/验证
│   │   │   └── SecurityUtils.java            # 认证用户获取工具
│   │   ├── service/                     # 业务接口（8个）
│   │   ├── service/impl/                # 业务实现（8个）
│   │   ├── util/                        # 工具类
│   │   │   ├── RedisUtils.java          # Redis 操作封装
│   │   │   └── SnowflakeIdWorker.java   # 雪花算法实现
│   │   └── websocket/                   # WebSocket 层
│   │       ├── HeartbeatManager.java    # 心跳状态管理（静态 Map）
│   │       ├── MessageAckHandler.java   # ACK 确认处理
│   │       └── WebSocketServer.java     # WebSocket 服务端主逻辑
│   ├── src/main/resources
│   │   ├── db/migration/
│   │   │   ├── V1__init_schema.sql      # 9张初始化表
│   │   │   └── V2__add_mute_columns.sql # 禁言字段迁移
│   │   ├── application.yml              # 主配置
│   │   └── logback-spring.xml           # 日志配置
│   ├── src/test/                        # 测试代码（4个测试类 + H2）
│   ├── logs/                            # 日志输出目录
│   ├── Dockerfile
│   └── pom.xml
├── linkchat-web/                        # 前端项目
│   ├── src
│   │   ├── api/                         # API 请求封装（8个模块）
│   │   │   ├── auth.ts                  # 认证 API
│   │   │   ├── file.ts                  # 文件上传 API
│   │   │   ├── friend.ts                # 好友 API
│   │   │   ├── group.ts                 # 群聊 API
│   │   │   ├── message.ts               # 消息 API
│   │   │   ├── request.ts              # Axios 实例 + 拦截器
│   │   │   ├── status.ts                # 在线状态 API
│   │   │   └── user.ts                  # 用户 API
│   │   ├── assets/icons/                # SVG 图标组件（13个）
│   │   ├── components/
│   │   │   ├── chat/                    # 聊天组件（7个：窗口/输入/气泡/文件/图片/系统消息）
│   │   │   ├── conversation/            # 会话列表组件
│   │   │   ├── design/                  # 自建设计系统组件（13个 Lc* 组件）
│   │   │   ├── file/                    # 文件处理组件
│   │   │   ├── friend/                  # 好友资料弹窗
│   │   │   └── layout/                  # 布局组件（导航栏/侧边栏/面板）
│   │   ├── composables/                 # 组合式函数
│   │   │   ├── useTyping.ts             # 正在输入状态逻辑
│   │   │   └── useWebSocket.ts          # WebSocket 连接生命周期
│   │   ├── design-tokens/               # 设计 Token（颜色/圆角/阴影/间距/过渡/排版）
│   │   ├── router/index.ts              # 路由配置（4个路由 + 导航守卫）
│   │   ├── stores/                      # Pinia 状态管理（5个 store）
│   │   │   ├── auth.ts                  # 认证状态（含 localStorage 持久化）
│   │   │   ├── chat.ts                  # 聊天状态（消息/未读/缓存）
│   │   │   ├── friend.ts                # 好友状态（列表/申请）
│   │   │   ├── group.ts                 # 群聊状态
│   │   │   └── theme.ts                 # 主题切换
│   │   ├── styles/                      # 全局样式
│   │   ├── types/                       # TypeScript 类型定义（4个）
│   │   ├── utils/                       # 工具函数（日期/文件）
│   │   ├── views/                       # 页面视图（4个）
│   │   │   ├── ChatView.vue             # 主聊天界面（含全部弹窗/管理逻辑）
│   │   │   ├── LoginView.vue            # 登录页
│   │   │   ├── RegisterView.vue         # 注册页
│   │   │   └── SettingsView.vue         # 设置页
│   │   └── websocket/
│   │       └── WebSocketClient.ts        # WebSocket 客户端（重连/心跳/消息分发）
│   ├── dist/                            # 构建产物
│   ├── Dockerfile
│   ├── nginx.conf                       # Nginx 反向代理配置
│   └── package.json
├── .github/modernize/java-upgrade/       # GitHub 自动化升级脚本
├── .vscode/settings.json                # VSCode 项目设置
├── .gitignore
├── BUG_FIX_LOG.md                       # 37 个 Bug 修复详情
├── docker-compose.yml                    # Docker 一键部署编排
├── start-services.bat                    # Windows 本地服务启动脚本
└── logs/                                # 项目级日志
```

## 数据库设计

9 张核心表，使用 InnoDB 引擎，utf8mb4 字符集：

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `user` | 用户表 | id, email, password(BCrypt), nickname, user_code(唯一码), avatar_url, status, last_online |
| `friend` | 好友关系表 | user_id, friend_id, status(0=PENDING/1=ACCEPTED/2=REJECTED/3=DELETED), remark, **UNIQUE(user_id, friend_id)** |
| `group_chat` | 群聊表 | id, name, owner_id, announcement, member_count, is_muted |
| `group_member` | 群成员表 | group_id, user_id, role(0=OWNER/1=ADMIN/2=MEMBER), is_muted, **UNIQUE(group_id, user_id)** |
| `message` | 消息表 | id(雪花算法), sender_id, receiver_id, group_id, message_type, content, file_url, is_recalled, ack_status, retry_count, quoted_msg_id |
| `message_ack` | 消息ACK表 | message_id, user_id, ack_type(1=DELIVERED/2=READ), **UNIQUE(message_id, user_id, ack_type)** |
| `offline_message` | 离线消息表 | message_id, receiver_id, message_type, content, file_url, is_pushed |
| `file_record` | 文件记录表 | uploader_id, file_name, file_url, file_size, file_type, mime_type, message_id |
| `online_user` | 在线用户表（预留） | user_id, status, server_node, client_ip, heartbeat_at |

> 详见 `linkchat-server/src/main/resources/db/migration/V1__init_schema.sql`

## WebSocket 通信协议

WebSocket 端点：`/ws/{token}`（通过 URL Path Param 传递 JWT Token）

| 类型 | 方向 | 说明 | Payload 关键字段 |
|------|------|------|-----------------|
| `ping` | Client→Server | 心跳检测 | `{}` |
| `pong` | Server→Client | 心跳响应 | `{}` |
| `message` | 双向 | 聊天消息转发 | `{id, senderId, receiverId?, groupId?, messageType, content, fileUrl?}` |
| `ack` | 双向 | 消息已送达/已读确认 | `{messageId, ackType:(DELIVERED\|READ), senderId}` |
| `recall` | 双向 | 消息撤回通知 | `{messageId, senderId}` |
| `typing` | 双向 | 正在输入状态 | `{senderId, active:boolean}` |
| `status_change` | Server→Client | 在线状态变更 | `{userId, status}` |
| `offline_msg` | Server→Client | 离线消息推送 | `[{message}, ...]` |
| `friend_request` | Server→Client | 好友申请通知 | `{requesterId, nickname}` |

> **注意**：Jackson 序列化后 Long 类型字段转为字符串，前端需要 `Number()` 转换后比较。

## REST API 概览

| 前缀 | 模块 | 主要端点 |
|------|------|---------|
| `/api/auth` | 认证 | POST register, POST login, PUT password |
| `/api/user` | 用户 | GET profile, PUT profile, POST avatar, GET search |
| `/api/friend` | 好友 | POST add, GET requests, PUT accept/{id}, PUT reject/{id}, DELETE/{id}, GET list, GET search |
| `/api/message` | 消息 | POST send, GET history, PUT recall/{id}, DELETE/{id}, POST forward/{id}, GET search, PUT read/{senderId} |
| `/api/group` | 群聊 | POST create, GET/{id}, DELETE/{id}, DELETE/{id}/leave, POST/{id}/invite, DELETE/{id}/member/{id}, PUT/{id}/admin/{id}, PUT/{id}/transfer/{id}, PUT/{id}/name, PUT/{id}/announcement, POST/{id}/avatar, PUT/{id}/mute-all, PUT/{id}/mute-member/{id}, GET/{id}/members, GET list |
| `/api/file` | 文件 | POST upload, POST upload/avatar |

## 快速开始

### Docker 一键部署（推荐）

```bash
docker-compose up -d
```

访问：http://localhost

### 本地开发

**前置条件**：MySQL 8.0、Redis 7.x、MinIO（或使用 `start-services.bat` 启动 Redis + MinIO）

**后端**：
```bash
cd linkchat-server
# 确保 MySQL/Redis/MinIO 已运行
mvn spring-boot:run
```

**前端**：
```bash
cd linkchat-web
npm install
npm run dev
```

### Windows 快速启动

```batch
# 启动 Redis + MinIO + 后端（一键）
start-services.bat

# 然后另开终端启动前端
cd linkchat-web && npm run dev
```

## Bug 修复历史

项目经过 **4 轮全面审查**，共修复 **37 个 Bug**：

| 轮次 | 时间 | 数量 | 严重级别 | 关键修复 |
|------|------|------|---------|---------|
| 第一轮 | 2026-06-12 | 28 个 | P0:15, P1:7, P2:7, P3:8 | 群聊消息隐私泄露、撤回广播泄露、好友唯一键冲突、JWT Token版本机制、CORS/RateLimiter配置修正 |
| 第二轮 | 2026-06-12 | 5 个 | P0:3, P1:1, P2:1 | 刷新后昵称消失、未认证返回403、多窗口互踢、validateToken异常处理、前端403处理 |
| 第三轮 | 2026-06-12 | 3 个 | P0:2, P1:1 | 好友申请无查询接口/无实时通知、好友申请UI缺失、Controller不安全userId |
| 第四轮 | 2026-06-12 | 1 个 | P0:1 | acceptFriend 反向插入唯一键冲突 |

详见 [BUG_FIX_LOG.md](BUG_FIX_LOG.md)

## 运行测试

```bash
# 后端测试（使用 H2 内存数据库）
cd linkchat-server && mvn test

# 前端测试（使用 Vitest）
cd linkchat-web && npm test
```

## 已知限制

- 转发功能 `targetId` 不区分用户/群（当前仅用户到用户）
- 好友搜索为内存过滤（好友数少时影响不大）
- `HeartbeatManager` 使用静态 Map（JSR-356 架构必需）

## License

MIT
