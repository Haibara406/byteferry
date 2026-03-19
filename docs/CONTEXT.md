# ByteFerry - Project Context Document

> 本文档用于跨会话传递项目上下文，让新会话的 AI 助手能快速了解项目全貌。

---

## 1. 项目概述

ByteFerry 是一个 **跨设备剪切板共享 / 文件传输 Web 工具**。用户无需安装软件，通过浏览器即可在不同设备间快速传输文本、图片、文件。

### 产品定位（三种模式）

| 模式 | 说明 | 是否需要登录 |
|------|------|-------------|
| **Quick（令牌码快传）** | 一次性分享码，发送内容后获得 6 位码，接收方输入码获取内容 | 不需要 |
| **Session（会话模式）** | 一个码开一个持久会话，发送方可持续推送多条内容，接收方实时接收 | 不需要 |
| **Multi-device（多设备同步）** | 同一账号的个人空间（Space），多设备通过 WebSocket 实时同步内容 | 需要登录 |
| **Friend Session（好友会话）** | 好友间开启双向传输会话 | 需要登录 |

---

## 2. 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.11 (Java 17) |
| 缓存/临时存储 | Redis（密码连接，host: localhost:6379） |
| 数据库 | MySQL 8.0（localhost:3306/byteferry, root/Ww249260523..） |
| 认证 | Spring Security + JWT（jjwt 0.12.6, BCrypt 密码加密） |
| 实时通信 | WebSocket（原生，用于 Space 多设备同步） |
| 文件存储 | 本地磁盘 `~/.byteferry/files/`（UUID 前缀命名） |
| 前端 | 纯 HTML + 自定义 CSS + 原生 JS（无框架、无 CDN 依赖） |
| 构建工具 | Maven |

### 运行方式

```bash
mvn spring-boot:run
# 访问 http://localhost:8076
```

**注意**：开发机上有 HTTP 代理 `http_proxy=127.0.0.1:7897`，测试 API 时需 `--noproxy '*'` 或将 localhost 加入代理排除。

---

## 3. 项目结构

```
ByteFerry/
├── pom.xml
├── FEATURES.md                          # 功能追踪（# 标记已完成）
├── CONTEXT.md                           # 本文档
├── src/main/java/com/byteferry/byteferry/
│   ├── ByteFerryApplication.java        # 启动类
│   ├── config/
│   │   ├── RedisConfig.java             # Redis 序列化配置（Jackson + 类型信息）
│   │   ├── SecurityConfig.java          # Spring Security 配置（JWT 认证）
│   │   ├── JwtUtil.java                 # JWT 生成/验证工具
│   │   ├── JwtAuthFilter.java           # JWT 认证过滤器（支持 Header + query param）
│   │   └── WebSocketConfig.java         # WebSocket 配置（注册 /ws/space 端点）
│   ├── websocket/
│   │   └── SpaceWebSocketHandler.java   # Space WebSocket 处理器（按 userId 推送）
│   │   └── FriendWebSocketHandler.java  # Friend WebSocket 处理器（好友通知+在线状态）
│   ├── controller/
│   │   ├── ShareController.java         # Quick 模式 REST API
│   │   ├── SessionController.java       # Session 模式 REST API
│   │   ├── AuthController.java          # 注册/登录/用户信息 API
│   │   ├── SpaceController.java         # 个人空间 CRUD API
│   │   └── FriendController.java        # 好友管理 + 好友会话 API
│   ├── model/
│   │   ├── ShareData.java               # Quick 模式数据模型（含 FileInfo 内部类）
│   │   ├── SessionData.java             # Session 模式数据模型（含 SessionItem 内部类）
│   │   ├── FriendSessionData.java       # 好友会话数据模型（Redis，多人会话，含 Participant/FriendSessionItem）
│   │   ├── SessionInvitation.java       # 会话邀请数据模型（Redis）
│   │   └── entity/
│   │       ├── User.java                # JPA 用户实体
│   │       ├── SpaceItem.java           # JPA 空间内容实体
│   │       ├── SpaceFile.java           # JPA 空间文件实体
│   │       ├── Friendship.java          # JPA 好友关系实体
│   │       └── FriendSessionHistory.java # JPA 好友会话历史实体（多参与者）
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── SpaceItemRepository.java
│   │   ├── SpaceFileRepository.java
│   │   ├── FriendshipRepository.java
│   │   └── FriendSessionHistoryRepository.java
│   ├── service/
│   │   ├── ShareService.java            # Quick 模式业务逻辑
│   │   ├── SessionService.java          # Session 模式业务逻辑
│   │   ├── FileStorageService.java      # 文件存储服务
│   │   ├── AuthService.java             # 注册/登录/BCrypt
│   │   ├── SpaceService.java            # 个人空间 CRUD
│   │   ├── FriendService.java           # 好友管理（请求/接受/删除/拉黑）
│   │   └── FriendSessionService.java    # 好友会话（Redis CRUD + 历史）
│   ├── task/
│   │   ├── SpaceCleanupTask.java        # Space 过期 item 定时清理（30s）
│   │   └── FriendSessionCleanupTask.java # 好友会话过期清理（30s）
│   └── util/
│       └── CodeGenerator.java           # 6 位分享码生成器（A-Z + 0-9）
├── src/main/resources/
│   ├── application.yml                  # 配置文件
│   └── static/
│       ├── index.html                   # 单页面应用
│       ├── css/style.css                # 全自定义 CSS（无 Tailwind CDN）
│       └── js/app.js                    # 前端交互（全 addEventListener，无 onclick）
```

---

## 4. 已实现功能详解

### 4.1 Quick 模式（MVP + Phase 1）

**Redis 数据结构：** `share:{CODE}` → ShareData JSON，TTL 600 秒

**API：**

| 端点 | 说明 |
|------|------|
| `POST /api/share/text` | 发送文本，返回 `{code}` |
| `POST /api/share/file` | 上传文件/图片（支持批量 MultipartFile[]），返回 `{code}` |
| `GET /api/share/{code}` | 获取分享信息（type, content, files[], createdAt） |
| `GET /api/share/{code}/preview/{index}` | 内联预览文件（不触发 delete-after-download） |
| `GET /api/share/{code}/download/{index}` | 下载文件（触发 delete-after-download） |
| `DELETE /api/share/{code}` | 手动删除分享 |

**数据模型 ShareData：**
- `type`: TEXT / IMAGE / FILE
- `content`: 文本内容（TEXT 类型）
- `files`: List\<FileInfo\>（IMAGE/FILE 类型，每个含 fileName/filePath/fileSize/mimeType）
- `expireSeconds`, `deleteAfterDownload`, `createdAt`

### 4.2 Session 模式（Phase 2）

**Redis 数据结构：** `session:{CODE}` → SessionData JSON，TTL 1800 秒（默认 30 分钟）

**API：**

| 端点 | 说明 |
|------|------|
| `POST /api/session` | 创建会话，返回 `{code, expireSeconds}` |
| `GET /api/session/{code}` | 获取会话信息 + 所有 items + remainingSeconds |
| `POST /api/session/{code}/items/text` | 添加文本到会话 |
| `POST /api/session/{code}/items/file` | 添加文件/图片到会话（支持批量） |
| `GET /api/session/{code}/items/{itemId}/preview/{fileIndex}` | 预览 |
| `GET /api/session/{code}/items/{itemId}/download/{fileIndex}` | 下载 |
| `DELETE /api/session/{code}` | 关闭会话（清理文件 + 保留 CLOSED 状态 60 秒） |

**数据模型 SessionData：**
- `code`, `status`（ACTIVE/CLOSED）, `createdAt`, `expireSeconds`
- `items`: List\<SessionItem\>，每个 item 有 id/type/content/files/addedAt
- `nextItemId`: 自增序号

**Session 行为：**
- 发送方创建 Session → 选择过期时间（10min/30min/1h/2h）→ 获得 6 位 code
- 发送方可持续添加 text/image/file（统一输入窗口，支持文本和附件同时发送）
- 接收方输入 code 加入 → 2 秒轮询获取新内容
- 发送方可关闭 Session → 文件被删除 → 保留 CLOSED 状态 60 秒 → 接收方检测到关闭
- 接收方只能 Leave（离开），不能 Close（关闭）
- 添加 item 时保持原 TTL（不刷新过期时间）

### 4.3 前端架构

**导航结构：**
```
[Quick] [Session]              ← 顶层模式切换
  ├── [Send] [Receive]         ← 每个模式内的子标签
```

**技术实现：**
- 全自定义 CSS（CSS 变量系统，不依赖任何 CDN）
- 所有事件绑定通过 `addEventListener`（无 inline onclick）
- 动画：slideUp 入场、hover 微上浮、toast 上滑、focus 蓝色光环
- Session 发送方：composer 聊天输入风格（textarea + 附件工具栏）
- Session 接收方：timeline 卡片列表，2 秒轮询自动刷新
- 响应式移动端适配

### 4.4 用户系统 & 个人空间（Phase 3）

**MySQL 表：** users, space_items, space_files（JPA 自动建表）

**Auth API（无需认证）：**

| 端点 | 说明 |
|------|------|
| `POST /api/auth/register` | 注册 `{username, password}` |
| `POST /api/auth/login` | 登录 → `{token, username}` |
| `GET /api/auth/me` | 获取当前用户信息（需 JWT） |

**Space API（需 JWT）：**

| 端点 | 说明 |
|------|------|
| `GET /api/space/items` | 获取当前用户未过期 items（按时间倒序，含 remainingSeconds） |
| `POST /api/space/items/text` | 添加文本（支持 expireSeconds 参数） |
| `POST /api/space/items/file` | 添加文件/图片（支持批量，支持 expireSeconds 参数） |
| `DELETE /api/space/items/{id}` | 删除 item + 关联文件 |
| `DELETE /api/space/items/clear` | 清空所有 items |
| `GET /api/space/items/{itemId}/files/{fileId}/preview` | 预览 |
| `GET /api/space/items/{itemId}/files/{fileId}/download` | 下载 |

**WebSocket：**
- 端点：`/ws/space?token=<jwt>`
- 消息类型：`"refresh"` = 数据变化，`"empty"` = 无存活 items
- 有存活 items 时连接，所有 items 过期/删除/清空后断开
- 断连后自动回退到 10 秒轮询

**Space 过期机制：**
- 每条 item 有独立 `expireAt`，用户添加时选择过期时间（10min/30min/1h/2h）
- 定时任务每 30 秒清理过期 items 和磁盘文件
- 前端每条 item 显示倒计时

**认证方式：**
- JWT token，72 小时过期
- 请求头 `Authorization: Bearer <token>` 或 query param `?token=<token>`（用于文件下载）
- BCrypt 密码加密
- Spring Security 放行 `/api/auth/**`, `/api/share/**`, `/api/session/**`, `/ws/**`, 静态资源

**前端：**
- Header 右侧：未登录显示 Login 按钮，已登录显示用户名 + Logout
- Login/Register 模态框（标签切换）
- Space 作为第三个顶层 tab（仅登录后可见）
- Space 视图：过期时间选择器 + Clear All 按钮 + composer 输入 + timeline 列表
- Space 实时同步：WebSocket 推送（有存活 items 时连接），断连自动回退轮询
- 每条 item 支持 复制/预览/下载/删除，显示剩余时间倒计时
- Token 存 localStorage，401 时自动弹出登录框

### 4.5 好友系统 & 双向会话（Phase 4）

**MySQL 表：** friendships, friend_session_history（JPA 自动建表）

**好友 API（需 JWT）`/api/friend`：**

| 端点 | 说明 |
|------|------|
| `POST /request` | 发送好友请求 `{username}` |
| `POST /request/{id}/accept` | 接受请求 |
| `POST /request/{id}/reject` | 拒绝请求 |
| `GET /list` | 好友列表（含在线状态） |
| `GET /requests` | 收到+发出的待处理请求 |
| `DELETE /{friendId}` | 删除好友 |
| `POST /{friendId}/block` | 拉黑 |

**好友会话 API（需 JWT）`/api/friend/session`：**

| 端点 | 说明 |
|------|------|
| `POST /invite` | 发送邀请 `{friendId, expireSeconds}` 或邀请到已有会话 `{friendId, sessionId}` |
| `POST /invite/{invitationId}/accept` | 接受邀请（加入会话） |
| `POST /invite/{invitationId}/decline` | 拒绝邀请 |
| `GET /invitations` | 获取待处理邀请列表 |
| `GET /{sessionId}` | 获取会话信息+items+participants |
| `POST /{sessionId}/items/text` | 发送文本 |
| `POST /{sessionId}/items/file` | 发送文件 |
| `DELETE /{sessionId}` | 关闭会话（仅管理员，保留文件+写历史） |
| `POST /{sessionId}/leave` | 离开会话（非管理员） |
| `POST /{sessionId}/kick/{targetId}` | 踢出成员（仅管理员） |
| `POST /{sessionId}/toggle-invite` | 切换全局邀请权限 `{enabled}` |
| `POST /{sessionId}/toggle-member-invite` | 切换成员邀请权限 `{userId, enabled}` |
| `GET /active` | 当前活跃会话列表 |
| `GET /history` | 历史记录列表（含参与者名称、id） |
| `GET /history/{historyId}` | 历史记录详情（含完整 items，可复制/下载） |
| `GET /history/{historyId}/items/{itemId}/preview/{fileIndex}` | 历史文件预览 |
| `GET /history/{historyId}/items/{itemId}/download/{fileIndex}` | 历史文件下载 |

**数据模型：**
- Friendship（MySQL）：双向对称行，PENDING/ACCEPTED/BLOCKED
- FriendSessionData（Redis）：`fsession:{uuid}`，多人会话模型
  - adminId/adminUsername：会话管理员
  - participants：List\<Participant\>（userId, username, joinedAt, inviteAllowed）
  - globalInviteEnabled：是否允许成员邀请他人
  - items：List\<FriendSessionItem\>，每条含 senderId/senderUsername
  - status：WAITING_FOR_PEER → ACTIVE → CLOSED
- SessionInvitation（Redis）：`finvite:{uuid}`，邀请数据（fromUserId, toUserId, sessionId, expireSeconds, status）
- FriendSessionHistory（MySQL）：关闭后为每个参与者写入一条记录，含 participants 名称列表、itemsJson（MEDIUMTEXT，序列化的会话内容）
- 历史记录限制：每用户最多保留 10 条，超出自动清理最旧记录及无引用的文件
- Redis Set `fsession-user:{userId}` 索引活跃 sessionId

**WebSocket `/ws/friend`：**
- 登录后连接，JSON 消息格式
- 单设备限制：新连接建立时关闭同用户旧连接（发送 session_replaced 通知）
- 事件类型：friend_request, friend_accepted, friend_removed, session_invitation, invitation_accepted, invitation_declined, session_member_joined, session_member_left, session_member_kicked, session_update, session_closed, session_replaced, online_status
- 在线状态：连接/断开时广播给好友

**前端：**
- Friends 作为第四个顶层 tab（仅登录后可见）
- 三个子视图：Friends（好友列表+搜索+添加+活跃会话+待处理邀请）/ Requests（收发请求）/ History（会话历史，可点击查看内容）
- 好友列表：搜索过滤、在线优先排序
- 邀请流程：选择过期时间 → 发送邀请 → 等待对方接受 → 进入会话
- 好友会话覆盖层：
  - Header：成员面板切换、邀请好友、离开、关闭（仅管理员）
  - 成员面板：参与者列表、Admin 标识、踢人按钮（仅管理员）
  - 邀请模态框：从好友列表选择邀请
  - 消息区：每个发送者分配不同颜色（8 色循环），发送者名称标签
  - Composer：文本+附件输入
- 历史详情覆盖层：点击历史卡片 → 查看完整会话内容（发送者颜色标识）→ 支持复制文本/预览图片/下载文件 → 返回列表

---

## 5. 关键设计决策与踩过的坑

1. **文件存储用 `Files.copy()` 而非 `transferTo()`** — 后者在某些 Servlet 实现下无法正确写入
2. **下载端点返回 `ResponseEntity<Resource>` + `InputStreamResource`** — 避免 Spring 将 Resource 序列化为 JSON
3. **分离 preview / download 端点** — preview 为 inline 不触发删除，download 为 attachment 触发删除
4. **Session 关闭后保留 CLOSED 状态 60 秒** — 让接收方轮询时能感知到关闭，而非直接 404
5. **Redis 序列化用 GenericJackson2JsonRedisSerializer + DefaultTyping.NON_FINAL** — 支持多态类型存储
6. **前端不依赖 Tailwind CDN** — 避免 CDN 加载失败导致样式全丢
7. **JWT 支持 query param 传递** — `<a>` 标签下载无法设置 Authorization header，JwtAuthFilter 同时检查 `?token=`
8. **Spring Security 无状态** — `SessionCreationPolicy.STATELESS`，不创建 HTTP session
9. **Space 文件下载用 token query param** — 前端 JS 在下载 URL 后拼接 `?token=` 解决认证问题
10. **Space WebSocket 按需连接** — 有存活 items 时才连接，空了发 `"empty"` 断开，断连回退到 10s 轮询
11. **Space item 过期由前端倒计时 + 后端定时清理配合** — 前端倒计时到 0 立刻刷新（用户感知精确），后端 30s 定时清理磁盘文件和 DB 记录（后台无感）
12. **多人会话用 Participant 列表替代双用户字段** — FriendSessionData 从 userIdA/userIdB 改为 List\<Participant\>，支持任意人数
13. **邀请系统用 Redis 独立存储** — SessionInvitation 独立于 FriendSessionData，支持邀请到新会话或已有会话
14. **会话 TTL 延迟启动** — 创建会话时不设 TTL（WAITING_FOR_PEER），第一个受邀者加入后才开始倒计时
15. **单设备 WebSocket 限制** — 新连接建立时关闭同用户旧连接，发送 session_replaced 通知让前端退出会话视图
16. **History 按参与者存储** — 关闭会话时为每个参与者写入一条记录，避免复杂的多对多查询
17. **savePreserveTTL 模式** — 修改 Redis 中的会话数据时保持原有 TTL，避免意外延长或缩短过期时间
18. **History 内容保留用 JSON 列** — 会话关闭时将 items 序列化为 JSON 存入 `items_json` MEDIUMTEXT 列，文件保留在磁盘上；避免新增子表，items 为一次写入批量读取场景
19. **History 文件多用户安全清理** — 同一会话的多个参与者共享相同文件，清理历史记录时通过 `countBySessionId` 检查是否还有其他用户引用该会话，仅在无引用时删除磁盘文件

---

## 6. 配置信息

```yaml
# application.yml 核心配置
spring.data.redis.host: localhost
spring.data.redis.port: 6379
spring.data.redis.password: "Ww249260523.."
spring.datasource.url: jdbc:mysql://localhost:3306/byteferry
spring.datasource.username: root
spring.datasource.password: "Ww249260523.."
spring.jpa.hibernate.ddl-auto: update
spring.servlet.multipart.max-file-size: 100MB

byteferry.storage.file-dir: ${user.home}/.byteferry/files/
byteferry.share.default-expire-seconds: 600    # Quick 模式 10 分钟
byteferry.share.code-length: 6
byteferry.session.default-expire-seconds: 1800  # Session 模式 30 分钟
byteferry.jwt.secret: ByteFerrySecretKey2026...
byteferry.jwt.expiration-hours: 72              # JWT 72 小时过期
```

**外部资源（后续可能用到）：**
- MySQL: localhost, root / Ww249260523..
- MinIO: http://154.94.235.178:9000, accessKey=haibara, secretKey=Ww249260523.., bucket=byteferry

---

## 7. 待完成功能（按 Phase 排列）

### Phase 2 剩余

（无）

### Phase 3 剩余

（无）

### Phase 4 剩余

（无）

### Phase 5 — 体验增强

- [ ] 二维码生成
- [ ] 剪切板自动检测
- [ ] 图片粘贴（Ctrl+V）
- [ ] URL 链接预览
- [ ] 代码语法高亮
- [ ] 客户端 AES 加密
- [ ] 浏览器扩展
- [ ] CLI 工具
- [ ] 开放 API
- [ ] PWA 支持
- [ ] Docker + Nginx 部署配置

---

## 8. 开发规范

- 包路径：`com.byteferry.byteferry`
- Lombok 注解：`@Data`, `@Builder`, `@RequiredArgsConstructor`
- 错误处理：Controller 层抛 `ResponseStatusException`
- Redis key 前缀：`share:` / `session:`
- 文件命名：`UUID_原始文件名`
- 分享码：6 位大写字母 + 数字，`SecureRandom` 生成
- 前端：纯原生 JS，`addEventListener` 绑定，自定义 CSS 变量系统