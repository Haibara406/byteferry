# Moment System - Frontend Integration Complete

## 前端入口

### 导航栏
- 在主导航栏添加了 "Moment" 按钮
- 仅在登录后显示
- 位置：Quick | Session | XHS | Space | Friends | **Moment**

### 子标签
1. **Timeline** - 查看所有用户的 Moment（待实现）
2. **Create** - 创建新 Moment
3. **My Moments** - 查看和管理自己的 Moment

## 功能特性

### 创建 Moment (Create Tab)
- ✅ 文本内容输入（支持多行）
- ✅ 图片上传（最多 9 张）
  - 拖拽上传
  - 点击选择
  - 预览和删除
- ✅ 模版选择（5 种内置模版）
  - Card Style
  - Magazine Style
  - Minimal Style
  - Polaroid Style
  - Gradient Style
- ✅ 可见性控制
  - Public - 所有人可见
  - Private - 仅自己可见
  - Visible To - 指定用户可见
  - Hidden From - 指定用户不可见
- ✅ 用户 ID 选择（用于 Visible To / Hidden From）

### 我的 Moment (My Moments Tab)
- ✅ 显示所有自己的 Moment
- ✅ 分页加载（每页 10 条）
- ✅ 显示可见性标签
- ✅ 删除功能
- ✅ 生成分享链接
- ✅ 复制分享链接

### Moment 卡片显示
- ✅ 用户 ID 和创建时间
- ✅ 可见性标签（带颜色区分）
- ✅ 文本内容
- ✅ 图片网格展示
- ✅ Live Photo 标识
- ✅ 操作按钮（删除）

## 样式设计

### 可见性标签颜色
- **PUBLIC** - 蓝色 (#1976d2)
- **PRIVATE** - 粉色 (#c2185b)
- **VISIBLE_TO** - 紫色 (#7b1fa2)
- **HIDDEN_FROM** - 橙色 (#f57c00)

### 图片展示
- 响应式网格布局
- 最小宽度 200px
- 1:1 宽高比
- 圆角 8px
- Hover 放大效果

## 使用流程

### 1. 创建 Moment
```
登录 → 点击 Moment → Create 标签
→ 输入文本 / 上传图片
→ 选择模版（可选）
→ 选择可见性
→ 点击 "Post Moment"
```

### 2. 查看我的 Moment
```
登录 → 点击 Moment → My Moments 标签
→ 查看所有 Moment
→ 点击 "Load More" 加载更多
```

### 3. 生成分享链接
```
My Moments 标签 → 点击 "Generate Share Link"
→ 复制链接
→ 分享给其他用户
```

### 4. 删除 Moment
```
My Moments 标签 → 找到要删除的 Moment
→ 点击 "Delete" 按钮
→ 确认删除
```

## 技术实现

### 文件结构
```
/js/moment.js          - Moment 功能 JavaScript
/css/style.css         - Moment 样式（已集成）
/index.html            - Moment 视图区域
```

### API 调用
- `POST /api/moment` - 创建 Moment
- `GET /api/moment/my` - 获取我的 Moment
- `DELETE /api/moment/{id}` - 删除 Moment
- `GET /api/moment/templates` - 获取模版列表
- `POST /api/moment/share/generate` - 生成分享链接

### 状态管理
- `momentImages[]` - 待上传的图片数组
- `selectedVisibility` - 当前选择的可见性
- `currentMyMomentPage` - 当前分页页码
- `templates[]` - 模版列表

## 待实现功能

### Timeline 标签
- 显示所有用户的公开 Moment
- 按时间倒序排列
- 分页加载
- 根据可见性规则过滤

### 分享链接页面
- 创建独立的 `moment-share.html` 页面
- 通过 share code 查看用户的 Moment
- 需要登录才能访问
- 遵守可见性规则

### 增强功能
- Live Photo 上传（图片 + 视频）
- 图片点击放大预览
- Moment 编辑功能
- 点赞和评论（未来扩展）
- 用户头像和用户名显示（需要关联 User 数据）

## 测试步骤

1. **启动应用**
   ```bash
   cd /Users/haibara/Documents/java_program/ByteFerry
   mvn spring-boot:run
   ```

2. **执行数据库脚本**
   ```bash
   mysql -u root -p byteferry < sql/moment_tables.sql
   ```

3. **登录并测试**
   - 访问 http://localhost:8080
   - 登录账号
   - 点击 Moment 标签
   - 创建测试 Moment
   - 查看 My Moments
   - 生成分享链接

## 注意事项

1. **图片限制**
   - 最多 9 张图片
   - 单张最大 10MB
   - 支持格式：JPG, JPEG, PNG, GIF, WEBP

2. **可见性规则**
   - VISIBLE_TO 和 HIDDEN_FROM 需要输入用户 ID
   - 用户 ID 用逗号分隔（例如：2,3,5）

3. **分享链接**
   - 每个用户只有一个分享链接
   - 重新生成会替换旧链接
   - 访问者必须登录

4. **删除操作**
   - 删除 Moment 会同时删除 MinIO 中的图片文件
   - 删除操作不可恢复
