# ByteFerry 升级开发计划

## 概述

本次升级包含三大核心功能，拆分为 6 个 Part，每个 Part 是一个完整可交付的功能模块，可独立开发和测试。

### Part 总览

| Part | 功能 | 依赖 | 状态 |
|------|------|------|------|
| Part 1 | MinIO 文件存储集成 | 无 | Done |
| Part 2 | 邮箱验证码服务（SMTP） | 无 | Done |
| Part 3 | 用户系统升级（注册/登录/迁移） | Part 1, 2 | Done |
| Part 4 | 用户个人主页 | Part 1, 3 | Done |
| Part 5 | Moment 系统 | Part 1, 3 | Pending |
| Part 6 | Moment Share Link | Part 5 | Pending |

### 文件存储方案

所有文件存储使用 MinIO 对象存储（已有 `byteferry` bucket），代码从 haibara-blog-backend 复制并适配。

- **MinIO Endpoint**: `http://154.94.235.178:9000`
- **Public URL**: `https://minio.haikari.top`
- **Bucket**: `byteferry`
- **SDK**: `io.minio:minio:8.5.7`

---

## Part 1: MinIO 文件存储集成

### 目标
将 MinIO 对象存储集成到 ByteFerry 项目中，为后续头像上传、Moment 图片/视频上传提供基础设施。

### 1.1 Maven 依赖

在 `pom.xml` 中添加：
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

### 1.2 application.yml 配置

```yaml
minio:
  endpoint: http://154.94.235.178:9000
  url: https://minio.haikari.top
  accessKey: haibara
  secretKey: ${MINIO_SECRET_KEY}
  bucketName: byteferry
```

### 1.3 新增文件

**`config/MinioConfig.java`**
- 从博客项目复制并适配
- 注入 `minio.endpoint`、`minio.accessKey`、`minio.secretKey`
- 创建 `MinioClient` Bean
- 参考: `/Users/haibara/Documents/java_program/haibara-blog-backend/blog-backend/src/main/java/com/blog/config/MinioConfig.java`

**`enums/UploadEnum.java`**
- 定义 ByteFerry 专属的上传类型枚举

| 枚举值 | 目录 | 允许格式 | 大小限制 |
|--------|------|----------|----------|
| USER_AVATAR | `user/avatar/` | JPG, JPEG, PNG, WEBP | 5MB |
| MOMENT_IMAGE | `moment/image/` | JPG, JPEG, PNG, GIF, WEBP | 10MB |
| MOMENT_VIDEO | `moment/video/` | MP4, MOV | 50MB |

- 参考: `/Users/haibara/Documents/java_program/haibara-blog-backend/blog-backend/src/main/java/com/blog/enums/UploadEnum.java`

**`utils/FileUploadUtils.java`**
- 从博客项目复制并适配
- 核心方法: `upload()`, `deleteFile()`, `deleteFiles()`, `listFiles()`, `isFileExist()`
- 文件命名: UUID + 原始扩展名
- 返回公开 URL: `https://minio.haikari.top/byteferry/{dir}/{filename}`
- 参考: `/Users/haibara/Documents/java_program/haibara-blog-backend/blog-backend/src/main/java/com/blog/utils/FileUploadUtils.java`

### 1.4 验证方式

- 启动应用，确认 MinioClient Bean 正常创建
- 通过单元测试或临时接口测试上传/下载/删除文件到 byteferry bucket

---

## Part 2: 邮箱验证码服务（SMTP）

### 目标
实现邮件发送和验证码管理服务，为注册、登录、换绑邮箱提供基础能力。

### 2.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### 2.2 application.yml 配置

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

### 2.3 新增文件

**`service/VerificationCodeService.java`**
- `generateCode()`: 生成 6 位随机数字验证码
- `storeCode(String email, String code)`: 存入 Redis，key: `verify:email:{email}`，TTL: 5 分钟
- `verifyCode(String email, String code)`: 校验验证码（校验成功后删除 key）
- `hasRecentCode(String email)`: 检查是否在 60 秒内已发送过（防刷）

**`service/EmailService.java`**
- 注入 `JavaMailSender`
- `sendVerificationCode(String toEmail, String code)`: 发送验证码邮件
- 邮件模版: 简洁的 HTML 格式，包含验证码和有效期说明

### 2.4 API 端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/send-code` | 发送验证码到邮箱（body: `{"email": "xxx"}`) | 无 |

### 2.5 验证码流程

```
前端 → POST /api/auth/send-code {email}
    ↓
后端检查 60 秒内是否已发送（防刷）
    ↓
生成 6 位验证码 → 存入 Redis（TTL 5分钟）
    ↓
通过 SMTP 发送邮件
    ↓
返回 {"message": "验证码已发送"}
```

### 2.6 修改文件

- `controller/AuthController.java` — 新增 `POST /api/auth/send-code` 端点
- `config/SecurityConfig.java` — 放行 `/api/auth/send-code`

### 2.7 验证方式

- 调用 send-code API，确认邮件能正常收到
- 检查 Redis 中验证码的存储和过期

---

## Part 3: 用户系统升级（注册/登录/老用户迁移）

### 目标
升级用户实体，实现邮箱注册、双模式登录（用户名密码 / 邮箱验证码），以及老用户强制邮箱绑定。

### 3.1 数据库变更

```sql
ALTER TABLE users
  ADD COLUMN email VARCHAR(100) UNIQUE AFTER password,
  ADD COLUMN avatar VARCHAR(500) DEFAULT 'https://minio.haikari.top/byteferry/default/default-avatar.png' AFTER email,
  ADD COLUMN gender ENUM('MALE', 'FEMALE', 'OTHER', 'UNKNOWN') DEFAULT 'UNKNOWN' AFTER avatar,
  ADD COLUMN email_bound TINYINT(1) DEFAULT 0 AFTER gender;
```

- `email`: 可为 NULL（兼容老用户），UNIQUE 约束
- `avatar`: 默认头像为 MinIO 中的默认图片 URL
- `gender`: 性别枚举
- `email_bound`: 老用户是否已绑定邮箱（新注册用户默认为 1）

### 3.2 修改文件

**`model/entity/User.java`**
- 新增字段: `email`, `avatar`, `gender`(枚举), `emailBound`
- 新增 `Gender` 枚举: `MALE`, `FEMALE`, `OTHER`, `UNKNOWN`

**`repository/UserRepository.java`**
- 新增: `Optional<User> findByEmail(String email)`
- 新增: `boolean existsByEmail(String email)`

**`service/AuthService.java`**
- 重构 `register()`: 邮箱验证码注册（email + code + username + password）
  - 验证邮箱验证码
  - 检查 email 和 username 唯一性
  - 新用户 emailBound = true
- 保留 `login()`: 用户名 + 密码登录（不变）
- 新增 `loginByEmail()`: 邮箱 + 验证码登录
- 新增 `bindEmail()`: 老用户绑定邮箱
  - 验证邮箱验证码
  - 检查邮箱唯一性
  - 更新 email 和 emailBound

**`controller/AuthController.java`**
- 修改 `POST /api/auth/register`: 接收 email, code, username, password
- 新增 `POST /api/auth/login/email`: 邮箱验证码登录
- 新增 `POST /api/auth/bind-email`: 绑定邮箱（JWT 认证）
- 修改 `GET /api/auth/me`: 返回完整用户信息（含 email, avatar, gender, emailBound）

**`config/SecurityConfig.java`**
- 放行: `/api/auth/login/email`

### 3.3 API 端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 邮箱验证码注册 | 无 |
| POST | `/api/auth/login` | 用户名密码登录 | 无 |
| POST | `/api/auth/login/email` | 邮箱验证码登录 | 无 |
| POST | `/api/auth/bind-email` | 老用户绑定邮箱 | JWT |
| GET  | `/api/auth/me` | 获取当前用户信息 | JWT |

### 3.4 注册请求格式

```json
POST /api/auth/register
{
    "email": "user@example.com",
    "code": "123456",
    "username": "myname",
    "password": "mypassword"
}
```

### 3.5 邮箱登录请求格式

```json
POST /api/auth/login/email
{
    "email": "user@example.com",
    "code": "123456"
}
```

### 3.6 老用户迁移策略

1. `GET /api/auth/me` 返回 `emailBound` 字段
2. 前端检测到 `emailBound == false` → 强制弹出邮箱绑定页面
3. 绑定流程: 发送验证码 → 输入验证码 → `POST /api/auth/bind-email` → 提示重新登录
4. 绑定成功后 `emailBound = true`，后续正常使用
5. 老用户的 username、password、好友关系等数据完全保留

### 3.7 验证方式

- 新用户注册（邮箱验证码）→ 用户名密码登录 → 邮箱验证码登录 → /me 返回完整信息
- 老用户登录 → 检测未绑定邮箱 → 绑定 → 重新登录 → /me 显示 emailBound=true
- 重复邮箱/用户名注册应返回错误

---

## Part 4: 用户个人主页

### 目标
提供用户个人资料的查看和编辑功能，包括头像上传、用户名修改、性别修改、换绑邮箱。

### 4.1 新增文件

**`service/UserService.java`**
- `getProfile(Long userId)`: 获取用户资料
- `getProfileByUsername(String username)`: 查看他人资料
- `updateProfile(Long userId, String username, String gender)`: 修改基本信息
- `uploadAvatar(Long userId, MultipartFile file)`: 上传头像到 MinIO
- `changeEmail(Long userId, String newEmail, String code)`: 换绑邮箱（需验证码）

**`controller/UserController.java`**

### 4.2 API 端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET  | `/api/user/profile` | 获取自己的个人资料 | JWT |
| GET  | `/api/user/profile/{username}` | 查看他人资料（脱敏） | JWT |
| PUT  | `/api/user/profile` | 修改个人资料（username, gender） | JWT |
| POST | `/api/user/avatar` | 上传/更换头像 | JWT |
| POST | `/api/user/email/change` | 更换邮箱（需先发验证码） | JWT |

### 4.3 修改文件

- `config/SecurityConfig.java` — 认证 `/api/user/**`

### 4.4 头像上传

- 使用 `FileUploadUtils.upload(UploadEnum.USER_AVATAR, file)` 上传到 MinIO
- 返回 URL: `https://minio.haikari.top/byteferry/user/avatar/{uuid}.ext`
- 更新 User 实体的 avatar 字段
- 如果有旧头像且非默认头像，删除旧文件

### 4.5 查看他人资料脱敏

- 他人资料只返回: username, avatar, gender
- 不返回: email, emailBound 等敏感信息

### 4.6 验证方式

- 上传头像 → 检查 MinIO 文件存在 → 头像 URL 可访问
- 修改用户名 → 检查唯一性 → /me 返回新用户名
- 换绑邮箱 → 发送验证码 → 验证 → 新邮箱可用于登录

---

## Part 5: Moment 系统

### 目标
实现 Moment 动态发布系统，支持文本 + 图片（含 Live Photo）+ HTML 模版，以及四种可见性控制。

### 5.1 数据库新增表

**`moments` 表：**
```sql
CREATE TABLE moments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    text_content TEXT,
    html_content MEDIUMTEXT,
    template_id VARCHAR(50),
    visibility ENUM('PUBLIC', 'PRIVATE', 'VISIBLE_TO', 'HIDDEN_FROM') DEFAULT 'PUBLIC',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);
```

**`moment_images` 表（单条 Moment 最多 9 张图）：**
```sql
CREATE TABLE moment_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    video_url VARCHAR(500),
    is_live_photo TINYINT(1) DEFAULT 0,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (moment_id) REFERENCES moments(id) ON DELETE CASCADE
);
```

**`moment_visibility_rules` 表：**
```sql
CREATE TABLE moment_visibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_moment_user (moment_id, user_id)
);
```
- visibility = `VISIBLE_TO` → 此表记录**可见**用户
- visibility = `HIDDEN_FROM` → 此表记录**不可见**用户

**`moment_templates` 表：**
```sql
CREATE TABLE moment_templates (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    html_template MEDIUMTEXT NOT NULL,
    preview_image VARCHAR(500),
    sort_order INT DEFAULT 0
);
```

### 5.2 新增实体类

- `model/entity/Moment.java`
- `model/entity/MomentImage.java`
- `model/entity/MomentVisibilityRule.java`
- `model/entity/MomentTemplate.java`
- `model/enums/Visibility.java` — 枚举: PUBLIC, PRIVATE, VISIBLE_TO, HIDDEN_FROM

### 5.3 新增 Repository

- `repository/MomentRepository.java` — 含分页查询、按用户查询
- `repository/MomentImageRepository.java` — 按 momentId 查询
- `repository/MomentVisibilityRuleRepository.java` — `existsByMomentIdAndUserId()`
- `repository/MomentTemplateRepository.java` — 查询所有模版

### 5.4 新增 Service

**`service/MomentService.java`**
- `createMoment()`: 创建 Moment（文本 + 图片上传到 MinIO + 可见性规则）
- `getMoment(Long id, Long viewerId)`: 获取单条（含可见性校验）
- `getMyMoments(Long userId, Pageable)`: 获取自己的 Moment（分页）
- `getUserMoments(String username, Long viewerId, Pageable)`: 获取他人的（按可见性过滤，分页）
- `updateMoment()`: 编辑
- `deleteMoment()`: 删除（同时删除 MinIO 文件）
- `canView(Moment, Long viewerId)`: 可见性判断核心逻辑

**`service/MomentTemplateService.java`**
- `getAllTemplates()`: 获取所有模版列表
- `getTemplate(String id)`: 获取模版详情

### 5.5 新增 Controller

**`controller/MomentController.java`**

### 5.6 API 端点

**Moment CRUD：**

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/moment` | 创建 Moment | JWT |
| GET  | `/api/moment/{id}` | 获取单条（可见性校验） | JWT |
| GET  | `/api/moment/my` | 获取自己的所有 Moment（分页） | JWT |
| GET  | `/api/moment/user/{username}` | 获取某用户的 Moment（分页，按可见性过滤） | JWT |
| PUT  | `/api/moment/{id}` | 编辑 Moment（仅自己） | JWT |
| DELETE | `/api/moment/{id}` | 删除 Moment（仅自己） | JWT |

**模版：**

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET  | `/api/moment/templates` | 获取所有可用模版 | JWT |
| GET  | `/api/moment/templates/{id}` | 获取模版详情 | JWT |

### 5.7 创建 Moment 请求格式

```
POST /api/moment
Content-Type: multipart/form-data

字段：
- textContent: String (可选，纯文本内容)
- htmlContent: String (可选，自定义 HTML)
- templateId: String (可选，使用预定义模版 ID)
- visibility: String (PUBLIC / PRIVATE / VISIBLE_TO / HIDDEN_FROM)
- visibleUserIds: String (逗号分隔的用户 ID，当 VISIBLE_TO 或 HIDDEN_FROM 时)
- images: MultipartFile[] (普通图片，最多 9 张)
- liveImages: MultipartFile[] (Live Photo 静态图)
- liveVideos: MultipartFile[] (Live Photo 视频，与 liveImages 一一对应)
```

### 5.8 可见性过滤核心逻辑

```java
boolean canView(Moment moment, Long viewerId) {
    if (moment.getUserId().equals(viewerId)) return true; // 自己始终可见
    switch (moment.getVisibility()) {
        case PUBLIC:      return true;
        case PRIVATE:     return false;
        case VISIBLE_TO:  return visibilityRuleRepo.existsByMomentIdAndUserId(moment.getId(), viewerId);
        case HIDDEN_FROM: return !visibilityRuleRepo.existsByMomentIdAndUserId(moment.getId(), viewerId);
    }
}
```

### 5.9 图片/视频上传

- 普通图片: `FileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, file)` → 存 image_url
- Live Photo 图片: `FileUploadUtils.upload(UploadEnum.MOMENT_IMAGE, liveImage)` → 存 image_url
- Live Photo 视频: `FileUploadUtils.upload(UploadEnum.MOMENT_VIDEO, liveVideo)` → 存 video_url
- 所有 URL 格式: `https://minio.haikari.top/byteferry/moment/{type}/{uuid}.ext`

### 5.10 内置 HTML 模版

应用启动时通过初始化脚本插入 `moment_templates` 表，预置 5 个模版：

| ID | 名称 | 描述 |
|----|------|------|
| card | 卡片式 | 圆角、阴影、居中排版 |
| magazine | 杂志风 | 大图 + 文字叠加效果 |
| minimal | 极简风 | 大量留白、衬线字体 |
| polaroid | 拍立得 | 图片边框 + 手写体文字 |
| gradient | 渐变色 | 渐变背景 + 白色文字 |

### 5.11 修改文件

- `config/SecurityConfig.java` — 认证 `/api/moment/**`

### 5.12 验证方式

- 创建纯文本 Moment → 创建图片 Moment → 创建 Live Photo Moment
- 使用 HTML 模版创建 Moment → 使用自定义 HTML 创建 Moment
- 设置 PUBLIC → 其他用户可见
- 设置 PRIVATE → 其他用户不可见
- 设置 VISIBLE_TO [userA] → 仅 userA 可见
- 设置 HIDDEN_FROM [userB] → userB 不可见，其他人可见
- 编辑/删除 Moment → MinIO 文件同步删除

---

## Part 6: Moment Share Link

### 目标
用户可以生成个人 Moment 页面的分享链接，其他已登录用户通过链接可查看该用户的 Moment（受可见性规则约束）。

### 6.1 数据库新增表

```sql
CREATE TABLE moment_share_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    share_code VARCHAR(32) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

- 每个用户只有一个 Share Link（UNIQUE user_id）
- share_code 使用 UUID 生成（32 位无横线）

### 6.2 新增实体类

- `model/entity/MomentShareLink.java`

### 6.3 新增 Repository

- `repository/MomentShareLinkRepository.java`
  - `Optional<MomentShareLink> findByUserId(Long userId)`
  - `Optional<MomentShareLink> findByShareCode(String shareCode)`

### 6.4 新增/修改 Service

在 `service/MomentService.java` 中新增方法：
- `generateShareLink(Long userId)`: 生成/重新生成 Share Link
- `getMyShareLink(Long userId)`: 获取自己的 Share Link
- `getMomentsByShareCode(String shareCode, Long viewerId, Pageable)`: 通过 Share Code 获取用户 Moment（需登录 + 可见性过滤）

### 6.5 API 端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/moment/share/generate` | 生成/重新生成 Share Link | JWT |
| GET  | `/api/moment/share/my` | 获取自己的 Share Link | JWT |
| GET  | `/api/moment/share/{shareCode}` | 通过 Share Link 查看 Moment 页面（分页） | JWT |

### 6.6 Share Link 访问流程

```
访问者点击 Share Link
    ↓
前端检查是否已登录（有 JWT token）
    ↓ 没有
跳转登录页 → 登录后重定向回 Share Link
    ↓ 已登录
GET /api/moment/share/{shareCode} (携带 JWT)
    ↓
后端通过 shareCode 找到对应用户
    ↓
查询该用户的所有 Moment，按可见性过滤（相对于当前访问者）
    ↓
返回过滤后的 Moment 列表（分页）
```

### 6.7 验证方式

- 生成 Share Link → 获取 Share Code
- 用另一个账号访问 Share Link → 看到对其可见的 Moment
- 未登录访问 → 提示需要登录
- 重新生成 → 旧 Share Code 失效，新 Share Code 生效

---

## 前端页面规划（贯穿所有 Part）

每个 Part 的后端 API 完成后，在 `src/main/resources/static/` 中开发对应前端页面。使用原生 JS + 现有 CSS 风格。

| Part | 前端页面 |
|------|----------|
| Part 2 | 验证码发送 UI 组件 |
| Part 3 | 注册页改造（邮箱注册）、登录页改造（双模式）、邮箱绑定弹窗 |
| Part 4 | 个人主页（资料展示/编辑、头像上传） |
| Part 5 | Moment 发布页（编辑器 + 图片上传 + 模版选择 + 可见性设置）、Moment 时间线 |
| Part 6 | Share Link Moment 浏览页 |

---

## 关键文件索引

### 现有需修改的文件
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/com/byteferry/byteferry/model/entity/User.java`
- `src/main/java/com/byteferry/byteferry/repository/UserRepository.java`
- `src/main/java/com/byteferry/byteferry/service/AuthService.java`
- `src/main/java/com/byteferry/byteferry/controller/AuthController.java`
- `src/main/java/com/byteferry/byteferry/config/SecurityConfig.java`
- `sql/byteferry.sql`

### 参考的博客项目文件（MinIO）
- `/Users/haibara/Documents/java_program/haibara-blog-backend/blog-backend/src/main/java/com/blog/config/MinioConfig.java`
- `/Users/haibara/Documents/java_program/haibara-blog-backend/blog-backend/src/main/java/com/blog/utils/FileUploadUtils.java`
- `/Users/haibara/Documents/java_program/haibara-blog-backend/blog-backend/src/main/java/com/blog/enums/UploadEnum.java`
