# Moment 缓存与查询优化

## 优化内容

### 1. ✅ Cache Key 优化
**问题**: 缓存键只包含 `userId:pageNumber`，缺少 `pageSize`，导致不同分页大小的数据发生缓存污染

**修复**:
- `MomentService.java:135` - 修改 myMoments 缓存键为 `userId:pageNumber:pageSize`
- `MomentService.java:218` - 修改 timeline 缓存键为 `viewerId:pageNumber:pageSize`

### 2. ✅ N+1 查询优化
**问题**: 每条 Moment 单独查询 images 和 user，导致查询次数 = 1 + 2N

**修复**:
- 新增 `MomentImageRepository.findByMomentIdInOrderBySortOrder()` - 批量查询所有 images
- 新增 `UserRepository.findByIdIn()` - 批量查询所有 users
- 新增 `MomentService.batchLoadMomentDetails()` - 批量加载方法，将 1+2N 次查询优化为 3 次查询

**优化效果**:
- 查询 10 条 Moment: 从 21 次查询 → 3 次查询
- 查询 100 条 Moment: 从 201 次查询 → 3 次查询

### 3. ✅ 多级缓存架构确认
**架构**: Caffeine (L1) + Redis (L2)

**Caffeine 配置** (`CacheConfig.java`):
- 最大容量: 1000 条
- 写入后过期: 2 小时
- 访问后过期: 1 小时
- 启用统计: recordStats()

**Redis 配置** (`CacheConfig.java`):
- TTL: 24 小时
- 序列化: Jackson JSON
- 禁用 null 值缓存

### 4. ✅ Redis Stream 消费者
**问题**: 只有生产者发送缓存失效消息，没有消费者监听

**修复**: 新增 `CacheInvalidationConsumer.java`
- 自动创建消费者组
- 后台线程持续消费消息
- 自动 ACK 确认
- 异常重试机制
- 优雅关闭

**工作流程**:
1. 数据库更新 → `CacheInvalidationService.publishCacheInvalidation()`
2. 发送消息到 Redis Stream
3. 立即清除本地缓存（同步）
4. `CacheInvalidationConsumer` 异步消费消息
5. 清除其他实例的 Caffeine + Redis 缓存

## 缓存策略

### 缓存层级
```
请求 → Caffeine (L1, 2小时) → Redis (L2, 24小时) → MySQL
```

### 缓存失效
- **创建 Moment**: 清除发布者的 myMoments + 受影响用户的 timeline
- **删除 Moment**: 同上
- **更新 Moment**: 需要手动调用 `cacheInvalidationService.onMomentUpdated()`

### 可见性规则
- `PUBLIC`: 清除所有 timeline 缓存
- `VISIBLE_TO`: 只清除指定用户的 timeline
- `HIDDEN_FROM`: 清除所有 timeline 缓存
- `PRIVATE`: 不影响 timeline

## 性能提升

| 场景 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 查询 10 条 Moment | 21 次 SQL | 3 次 SQL | 85% ↓ |
| 查询 100 条 Moment | 201 次 SQL | 3 次 SQL | 98% ↓ |
| 缓存命中 | Redis 查询 | Caffeine 内存 | 10x+ ↑ |

## 后续优化建议

1. **监控缓存命中率**: 使用 Caffeine 的 `recordStats()` 监控 L1 缓存效果
2. **缓存预热**: 应用启动时预加载热点数据
3. **缓存穿透保护**: 对不存在的数据缓存空值（短时间）
4. **分布式锁优化**: 当前使用本地锁，可考虑 Redisson 分布式锁
5. **异步刷新**: 缓存即将过期时异步刷新，避免缓存雪崩
