#!/bin/bash
# 设置 MinIO bucket 为公开访问

# 安装 mc 客户端（如果还没安装）
# wget https://dl.min.io/client/mc/release/linux-amd64/mc
# chmod +x mc
# sudo mv mc /usr/local/bin/

# 配置 MinIO 连接
mc alias set myminio http://154.94.235.178:9000 haibara 'Ww249260523..'

# 设置 byteferry bucket 为公开读取
mc anonymous set download myminio/byteferry

# 验证策略
mc anonymous get myminio/byteferry

echo "完成！现在 byteferry bucket 应该可以公开访问了"
