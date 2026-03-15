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
| **Multi-device（多设备同步）** | 同一账号在多设备间共享剪切板内容 | 需要登录 |
| **Friend Session（好友会话）** | 好友间开启双向传输会话 | 需要登录 |

---

## 2. 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.11 (Java 17) |
| 缓存/临时存储 | Redis（密码连接，host: localhost:6379） |
| 数据库 | MySQL 8.0（localhost:3306/byteferry, root/Ww249260523..） |
| 认证 | Spring Security + JWT（jjwt 0.12.6, BCrypt 密码加密） |
| 文件存储 | 本地磁盘 `~/.byteferry/files/`（UUID 前缀命名） |
| 前端 | 纯 HTML + 自定义 CSS + 原生 JS（无框架、无 CDN 依赖） |
| 构建工具 | Maven |

### 运行方式

```bash
mvn spring-boot:run
# 访问 http://localhost:8080
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
│   │   └── JwtAuthFilter.java           # JWT 认证过滤器（支持 Header + query param）
│   ├── controller/
│   │   ├── ShareController.java         # Quick 模式 REST API
│   │   ├── SessionController.java       # Session 模式 REST API
│   │   ├── AuthController.java          # 注册/登录/用户信息 API
│   │   └── SpaceController.java         # 个人空间 CRUD API
│   ├── model/
│   │   ├── ShareData.java               # Quick 模式数据模型（含 FileInfo 内部类）
│   │   ├── SessionData.java             # Session 模式数据模型（含 SessionItem 内部类）
│   │   └── entity/
│   │       ├── User.java                # JPA 用户实体
│   │       ├── SpaceItem.java           # JPA 空间内容实体
│   │       └── SpaceFile.java           # JPA 空间文件实体
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── SpaceItemRepository.java
│   │   └── SpaceFileRepository.java
│   ├── service/
│   │   ├── ShareService.java            # Quick 模式业务逻辑
│   │   ├── SessionService.java          # Session 模式业务逻辑
│   │   ├── FileStorageService.java      # 文件存储服务
│   │   ├── AuthService.java             # 注册/登录/BCrypt
│   │   └── SpaceService.java            # 个人空间 CRUD
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
- 发送方创建 Session → 获得 6 位 code
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

### 4.3 用户系统 & 个人空间（Phase 3）

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
| `GET /api/space/items` | 获取当前用户所有 items（按时间倒序） |
| `POST /api/space/items/text` | 添加文本 |
| `POST /api/space/items/file` | 添加文件/图片（支持批量） |
| `DELETE /api/space/items/{id}` | 删除 item + 关联文件 |
| `GET /api/space/items/{itemId}/files/{fileId}/preview` | 预览 |
| `GET /api/space/items/{itemId}/files/{fileId}/download` | 下载 |

**认证方式：**
- JWT token，72 小时过期
- 请求头 `Authorization: Bearer <token>` 或 query param `?token=<token>`（用于文件下载）
- BCrypt 密码加密
- Spring Security 放行 `/api/auth/**`, `/api/share/**`, `/api/session/**`, 静态资源

**前端：**
- Header 右侧：未登录显示 Login 按钮，已登录显示用户名 + Logout
- Login/Register 模态框（标签切换）
- Space 作为第三个顶层 tab（仅登录后可见）
- Space 视图：composer 输入 + timeline 列表（10 秒轮询同步）
- 每条 item 支持 复制/预览/下载/删除
- Token 存 localStorage，401 时自动弹出登录框

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

- [ ] 自定义过期时间选择 UI（Session 创建时可选择过期时长）

### Phase 3 剩余

- [ ] 跨设备剪切板同步（WebSocket 实时推送）

### Phase 4 — 好友系统 & 双向会话

- [ ] 添加好友（通过用户名/好友码）
- [ ] 好友列表管理（添加/删除/拉黑）
- [ ] 好友请求 & 接受流程
- [ ] 与好友开启双向传输会话
- [ ] 双向会话：双方都可以发送 text/image/file
- [ ] 会话内操作：复制文本、预览图片、下载文件
- [ ] 会话过期：可配置超时，到期自动关闭
- [ ] 关闭会话：自动清理所有传输资源
- [ ] 会话历史（列表，不保留内容）
- [ ] 好友在线状态

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
