# Moment System Bug Fixes

## 修复内容

### Bug 1: Moment 创建失败 - Hibernate orphan deletion 错误

**问题描述**：
```
org.hibernate.HibernateException: A collection with orphan deletion was no longer referenced by the owning entity instance: com.byteferry.byteferry.model.entity.Moment.images
```

**原因**：
- Moment 实体中的 `images` 字段使用了 `@OneToMany(mappedBy = "momentId", cascade = CascadeType.ALL, orphanRemoval = true)`
- 这会导致 Hibernate 尝试管理这个集合的生命周期
- 但我们在创建 Moment 时直接保存 MomentImage，不通过 Moment 的 images 集合

**解决方案**：
1. 将 `images` 字段改为 `@Transient`，表示这不是数据库字段
2. 在 Service 层手动加载 images（通过 MomentImageRepository）
3. 修改 `createMoment` 方法，返回时重新加载 Moment 和 images

**修改文件**：
- `Moment.java` - 将 `@OneToMany` 改为 `@Transient`
- `MomentService.java` - 修改 `createMoment` 返回逻辑

---

### Bug 2: Share Link 404 错误

**问题描述**：
```
No static resource moment-share.html
```

**原因**：
- 前端代码生成的分享链接指向 `/moment-share.html`
- 但该文件不存在

**解决方案**：
1. 创建 `moment-share.html` 页面
2. 实现分享链接查看功能
3. 更新 SecurityConfig 允许访问该页面

**功能特性**：
- 从 URL 参数获取 share code
- 检查用户登录状态
- 加载并显示分享的 Moments
- 分页加载
- 遵守可见性规则

**修改文件**：
- 新增 `moment-share.html`
- `SecurityConfig.java` - 添加 `/moment-share.html` 到 permitAll

---

## 测试步骤

### 1. 重启应用
```bash
# 停止当前运行的应用
# 重新启动
mvn spring-boot:run
```

### 2. 测试创建 Moment
```
1. 登录账号
2. 点击 Moment 标签
3. 切换到 Create 子标签
4. 输入文本内容
5. 上传图片（可选）
6. 选择可见性
7. 点击 "Post Moment"
8. 应该成功创建，不再报错
```

### 3. 测试分享链接
```
1. 在 My Moments 标签
2. 点击 "Generate Share Link"
3. 复制生成的链接
4. 在新标签页打开链接
5. 应该能看到 moment-share.html 页面
6. 显示该用户的 Moments（根据可见性过滤）
```

---

## 数据库表检查

确保已执行 SQL 脚本：
```bash
mysql -u root -p byteferry < sql/moment_tables.sql
```

检查表是否存在：
```sql
SHOW TABLES LIKE 'moment%';
```

应该看到：
- moments
- moment_images
- moment_visibility_rules
- moment_templates
- moment_share_links

---

## API 测试

### 创建 Moment
```bash
curl -X POST http://localhost:8080/api/moment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "textContent=Test moment" \
  -F "visibility=PUBLIC"
```

### 获取我的 Moments
```bash
curl -X GET http://localhost:8080/api/moment/my \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 生成分享链接
```bash
curl -X POST http://localhost:8080/api/moment/share/generate \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 通过分享链接查看
```bash
curl -X GET http://localhost:8080/api/moment/share/YOUR_SHARE_CODE \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 注意事项

1. **Transient 字段**：
   - `images` 字段现在是 `@Transient`，不会持久化到数据库
   - 每次需要 images 时都要手动加载
   - 这避免了 Hibernate 的级联管理问题

2. **分享链接访问**：
   - 必须登录才能查看
   - 遵守可见性规则（PUBLIC, PRIVATE, VISIBLE_TO, HIDDEN_FROM）
   - 如果未登录会自动跳转到首页

3. **图片上传**：
   - 确保 MinIO 服务正常运行
   - 检查 application.yml 中的 MinIO 配置
   - 图片会上传到 `byteferry/moment/image/` 目录

4. **可见性规则**：
   - PUBLIC：所有人可见
   - PRIVATE：仅自己可见
   - VISIBLE_TO：仅指定用户可见
   - HIDDEN_FROM：除指定用户外都可见

---

## 已知限制

1. **Timeline 功能未实现**：
   - Timeline 标签目前为空
   - 需要实现全局 Moment 流

2. **用户信息显示**：
   - 目前只显示 User ID
   - 需要关联 User 表显示用户名和头像

3. **Live Photo 上传**：
   - 前端暂未实现 Live Photo 上传 UI
   - 后端已支持

4. **图片预览**：
   - 点击图片暂无放大预览功能
   - 可以后续添加 lightbox 效果
