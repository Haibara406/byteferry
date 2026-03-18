# ByteFerry 部署指南

## 目录
- [部署流程](#部署流程)
- [Nginx 配置](#nginx-配置)
- [HTTPS 证书申请](#https-证书申请)

---

## 部署流程

### 1. 环境准备（服务器端）

```bash
# 1.1 创建数据目录
ssh root@154.94.235.178 "mkdir -p /root/.byteferry/files"

# 1.2 创建 MySQL 数据库
mysql -u root -p'Ww249260523..' -e "CREATE DATABASE IF NOT EXISTS byteferry CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 1.3 手动测试启动（首次）
docker run -d --name byteferry \
  -p 8076:8076 \
  -v /root/.byteferry/files:/root/.byteferry/files \
  -e SPRING_DATA_REDIS_HOST=154.94.235.178 \
  -e SPRING_DATA_REDIS_PASSWORD=Ww249260523.. \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://154.94.235.178:3306/byteferry?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=Ww249260523.. \
  --restart unless-stopped \
  haibaraiii/byteferry:latest

# 1.4 查看日志
docker logs -f byteferry
```

### 2. GitHub Secrets 配置

在 GitHub 仓库设置中添加以下 Secrets：

| Secret 名称 | 说明 | 示例值 |
|-------------|------|--------|
| `DOCKERHUB_TOKEN` | DockerHub 访问令牌 | DockerHub 生成的 token |
| `SSH_PRIVATE_KEY` | 服务器 SSH 私钥 | `~/.ssh/id_rsa` 内容 |

```bash
# 生成 SSH 密钥对（本地执行）
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# 将公钥添加到服务器
ssh-copy-id root@154.94.235.178
```

### 3. 首次手动部署

```bash
# 在 GitHub 仓库页面
# 进入 Actions -> ByteFerry CI/CD -> Run workflow
```

---

## Nginx 配置

> ⚠️ 服务器 Nginx 是 Docker 运行的，配置在宿主机 `/data/nginx/conf/conf.d/` 目录
> 
> ✅ **已配置完成，无需手动操作**

### 1. 创建 ByteFerry 配置文件

在 `/data/nginx/conf/conf.d/` 目录下创建 `byteferry.conf`：

```nginx
# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name byteferry.haikari.top;

    # Let's Encrypt 证书验证
    location /.well-known/acme-challenge/ {
        root /usr/share/nginx/html;
    }

    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS 配置
server {
    listen 443 ssl http2;
    server_name byteferry.haikari.top;

    # SSL 证书（使用 Let's Encrypt）
    ssl_certificate /etc/nginx/ssl/byteferry.haikari.top.crt;
    ssl_certificate_key /etc/nginx/ssl/byteferry.haikari.top.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # 上传文件大小限制（100MB）
    client_max_body_size 100M;

    # ByteFerry 后端代理
    location / {
        proxy_pass http://172.17.0.1:8076;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

### 2. 应用 Nginx 配置

```bash
# SSH 到服务器
ssh root@154.94.235.178

# 创建配置文件
cat > /data/nginx/conf/conf.d/byteferry.conf << 'EOF'
# 粘贴上面的配置内容
EOF

# 重载 Nginx 容器配置
docker exec nginx nginx -s reload
```

---

## ⚠️ 证书已自动续期

> 续期脚本已更新，会自动处理 byteferry.haikari.top 的证书续期

### 方式一：使用 Certbot（推荐）

```bash
# SSH 到服务器
ssh root@154.94.235.178

# 1. 安装 Certbot
apt update && apt install -y certbot

# 2. 申请证书（确保域名已解析到此服务器）
certbot certonly --webroot -w /usr/share/nginx/html -d byteferry.haikari.top

# 3. 复制证书到 SSL 目录
cp /etc/letsencrypt/live/byteferry.haikari.top/fullchain.pem /data/nginx/ssl/byteferry.haikari.top.crt
cp /etc/letsencrypt/live/byteferry.haikari.top/privkey.pem /data/nginx/ssl/byteferry.haikari.top.key

# 4. 重载 Nginx
docker exec nginx nginx -s reload
```

### 方式二：手动申请（如果方式一失败）

```bash
# 1. 停止 Nginx
docker stop nginx

# 2. 申请证书
certbot certonly --standalone -d byteferry.haikari.top

# 3. 复制证书到 SSL 目录
cp /etc/letsencrypt/live/byteferry.haikari.top/fullchain.pem /data/nginx/ssl/byteferry.haikari.top.crt
cp /etc/letsencrypt/live/byteferry.haikari.top/privkey.pem /data/nginx/ssl/byteferry.haikari.top.key

# 4. 启动 Nginx
docker start nginx
```

### 证书自动续期

```bash
# 添加续期任务
echo "0 0 * * * certbot renew --quiet --deploy-hook 'docker exec nginx nginx -s reload'" | crontab -
```

---

## 快速命令参考

```bash
# Docker 操作
docker logs -f byteferry           # 查看日志
docker restart byteferry           # 重启容器
docker stop byteferry && docker start byteferry  # 停止后启动

# Nginx 操作（Docker）
docker exec nginx nginx -t         # 测试配置
docker exec nginx nginx -s reload  # 重载配置

# 访问地址
# https://byteferry.haikari.top
```

---

## 常见问题

### 1. 数据库连接失败
- 确认 MySQL 用户权限：`GRANT ALL ON byteferry.* TO 'root'@'%';`

### 2. 文件上传失败
- 检查目录权限：`chmod 777 /root/.byteferry/files`

### 3. WebSocket 连接失败
- 确保 Nginx 配置了 WebSocket 代理头
