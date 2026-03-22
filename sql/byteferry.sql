/*
 Navicat Premium Dump SQL

 Source Server         : cdu-snzh-miniapp
 Source Server Type    : MySQL
 Source Server Version : 80027 (8.0.27)
 Source Host           : 8.156.75.132:3306
 Source Schema         : byteferry

 Target Server Type    : MySQL
 Target Server Version : 80027 (8.0.27)
 File Encoding         : 65001

 Date: 22/03/2026 14:21:53
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for friend_session_history
-- ----------------------------
DROP TABLE IF EXISTS `friend_session_history`;
CREATE TABLE `friend_session_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `closed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expire_seconds` int DEFAULT NULL,
  `item_count` int DEFAULT NULL,
  `session_id` varchar(36) NOT NULL,
  `admin_username` varchar(50) NOT NULL,
  `participant_count` int DEFAULT NULL,
  `participants` varchar(500) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `items_json` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of friend_session_history
-- ----------------------------
BEGIN;
INSERT INTO `friend_session_history` (`id`, `closed_at`, `created_at`, `expire_seconds`, `item_count`, `session_id`, `admin_username`, `participant_count`, `participants`, `user_id`, `items_json`) VALUES (10, '2026-03-18 22:30:07.902369', '2026-03-18 22:27:42.415476', 1800, 19, '869513f3-5d80-49a1-94dc-3873260d2f3b', 'haibara', 2, 'haibara, Hanshao666', 6, '[{\"id\":0,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"hello\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:06.128495845\"},{\"id\":1,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:08.141632552\"},{\"id\":2,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"666\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:12.622115093\"},{\"id\":3,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"what are u\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:21.15221852\"},{\"id\":4,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"doing\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:27.869345216\"},{\"id\":5,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"感觉有点拉喃\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:31.825579607\"},{\"id\":6,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"很帅啊\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:36.540991846\"},{\"id\":7,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"有点卡顿\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:37.380174091\"},{\"id\":8,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"还行\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:41.613793508\"},{\"id\":9,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"服务器性能太拉了\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:52.357470081\"},{\"id\":10,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"IMAGE\",\"content\":null,\"files\":[{\"fileName\":\"IMG_8570.jpeg\",\"filePath\":\"/root/.byteferry/files/55612165-66d4-425b-b93b-ff92dc929a2b_IMG_8570.jpeg\",\"fileSize\":1484492,\"mimeType\":\"image/jpeg\"}],\"addedAt\":\"2026-03-18T22:29:06.072250798\"},{\"id\":11,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:12.088481335\"},{\"id\":12,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"ok\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:33.936296075\"},{\"id\":13,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"ok\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:34.299652465\"},{\"id\":14,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"测试完毕\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:36.718611201\"},{\"id\":15,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"IMAGE\",\"content\":null,\"files\":[{\"fileName\":\"头像.jpg\",\"filePath\":\"/root/.byteferry/files/2be7e46d-7c55-4acc-8dd1-2d33336081eb_头像.jpg\",\"fileSize\":265086,\"mimeType\":\"image/jpeg\"}],\"addedAt\":\"2026-03-18T22:29:40.779792407\"},{\"id\":16,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:45.66955493\"},{\"id\":17,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"太招了\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:54.656909365\"},{\"id\":18,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:30:01.329954453\"}]');
INSERT INTO `friend_session_history` (`id`, `closed_at`, `created_at`, `expire_seconds`, `item_count`, `session_id`, `admin_username`, `participant_count`, `participants`, `user_id`, `items_json`) VALUES (11, '2026-03-18 22:30:08.401517', '2026-03-18 22:27:42.415476', 1800, 19, '869513f3-5d80-49a1-94dc-3873260d2f3b', 'haibara', 2, 'haibara, Hanshao666', 7, '[{\"id\":0,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"hello\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:06.128495845\"},{\"id\":1,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:08.141632552\"},{\"id\":2,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"666\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:12.622115093\"},{\"id\":3,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"what are u\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:21.15221852\"},{\"id\":4,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"doing\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:27.869345216\"},{\"id\":5,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"感觉有点拉喃\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:31.825579607\"},{\"id\":6,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"很帅啊\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:36.540991846\"},{\"id\":7,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"有点卡顿\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:37.380174091\"},{\"id\":8,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"还行\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:41.613793508\"},{\"id\":9,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"服务器性能太拉了\",\"files\":null,\"addedAt\":\"2026-03-18T22:28:52.357470081\"},{\"id\":10,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"IMAGE\",\"content\":null,\"files\":[{\"fileName\":\"IMG_8570.jpeg\",\"filePath\":\"/root/.byteferry/files/55612165-66d4-425b-b93b-ff92dc929a2b_IMG_8570.jpeg\",\"fileSize\":1484492,\"mimeType\":\"image/jpeg\"}],\"addedAt\":\"2026-03-18T22:29:06.072250798\"},{\"id\":11,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:12.088481335\"},{\"id\":12,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"ok\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:33.936296075\"},{\"id\":13,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"ok\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:34.299652465\"},{\"id\":14,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"测试完毕\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:36.718611201\"},{\"id\":15,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"IMAGE\",\"content\":null,\"files\":[{\"fileName\":\"头像.jpg\",\"filePath\":\"/root/.byteferry/files/2be7e46d-7c55-4acc-8dd1-2d33336081eb_头像.jpg\",\"fileSize\":265086,\"mimeType\":\"image/jpeg\"}],\"addedAt\":\"2026-03-18T22:29:40.779792407\"},{\"id\":16,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:45.66955493\"},{\"id\":17,\"senderId\":6,\"senderUsername\":\"haibara\",\"type\":\"TEXT\",\"content\":\"太招了\",\"files\":null,\"addedAt\":\"2026-03-18T22:29:54.656909365\"},{\"id\":18,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"？\",\"files\":null,\"addedAt\":\"2026-03-18T22:30:01.329954453\"}]');
INSERT INTO `friend_session_history` (`id`, `closed_at`, `created_at`, `expire_seconds`, `item_count`, `session_id`, `admin_username`, `participant_count`, `participants`, `user_id`, `items_json`) VALUES (12, '2026-03-18 23:09:11.574692', '2026-03-18 23:06:23.062270', 1800, 3, '77af83ca-1f2a-468e-b5c3-95da13053637', 'Haerin', 2, 'Haerin, Sherry', 8, '[{\"id\":0,\"senderId\":8,\"senderUsername\":\"Haerin\",\"type\":\"TEXT\",\"content\":\"额\",\"files\":null,\"addedAt\":\"2026-03-18T23:07:38.761591\"},{\"id\":1,\"senderId\":9,\"senderUsername\":\"Sherry\",\"type\":\"TEXT\",\"content\":\"额\",\"files\":null,\"addedAt\":\"2026-03-18T23:07:42.918601\"},{\"id\":2,\"senderId\":9,\"senderUsername\":\"Sherry\",\"type\":\"TEXT\",\"content\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\n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\\n-----END OPENSSH PRIVATE KEY-----\",\"files\":null,\"addedAt\":\"2026-03-18T23:08:10.357238\"}]');
INSERT INTO `friend_session_history` (`id`, `closed_at`, `created_at`, `expire_seconds`, `item_count`, `session_id`, `admin_username`, `participant_count`, `participants`, `user_id`, `items_json`) VALUES (13, '2026-03-18 23:09:11.643788', '2026-03-18 23:06:23.062270', 1800, 3, '77af83ca-1f2a-468e-b5c3-95da13053637', 'Haerin', 2, 'Haerin, Sherry', 9, '[{\"id\":0,\"senderId\":8,\"senderUsername\":\"Haerin\",\"type\":\"TEXT\",\"content\":\"额\",\"files\":null,\"addedAt\":\"2026-03-18T23:07:38.761591\"},{\"id\":1,\"senderId\":9,\"senderUsername\":\"Sherry\",\"type\":\"TEXT\",\"content\":\"额\",\"files\":null,\"addedAt\":\"2026-03-18T23:07:42.918601\"},{\"id\":2,\"senderId\":9,\"senderUsername\":\"Sherry\",\"type\":\"TEXT\",\"content\":\"-----BEGIN OPENSSH PRIVATE KEY-----\\nb3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAACFwAAAAdzc2gtcn\\nNhAAAAAwEAAQAAAgEAzChEgnBpk78/ARx1PlzzVa9VxkDaubNFChG0uwQOTxXCDnEfOXBA\\nXnJH5uI/p22JnqIfw/uDsq2wown7QUt/VyWIXDBFmsktCpCmTD3QQErUpxvkFdK0xOJp+p\\nk+KdOaeLPERrZjzB1tmH3zhr1gYTyxno4JFDKle/1++hIXEqrAedYQhFsRFVjAxB6Ot1TS\\naHhaJLCdBjrSfpq+9tWimRDB8+TsqJgi7MjGrNd0Vn1vAkN42Mm5P8Xnu3ZHV1ykXWdcac\\nyZ1qjEqpy9mk40ETnobhvT7M7DzgGVtOyxpciwnbfyU1kX0GbSJFSHNmBPJjI0pkt7u0wb\\nNQrHlQ5wK4COOHkYY3S9PFqseBhTfXFTlsza7+SoL36dC05AZ/AOlvTTZ1AGZ5mlQ1zkvv\\nJdDQDlIaogt7yNUMhfNGhy90E9Y2gIJQx1l7KSJgShMIPZSmZ2PKBwjz1wQebe3sYCE0bG\\nih+kkmFlH+Oa8dFfVbTJNwPLC4x/fZgZixq0WuB3ktzyTt1ahOSIcVpJMLMCghtlYCE/Q7\\nrlxiBPBQe/pwhB8gUwMHqSgXhBOi/xcBjqi07U6v2wkVzriAyGGH2Q2PomwPOBtMaRsLDM\\nUpXJoL0Y2Kn1jjnq71KKJ8wUuQLtwXkumt2nZUoR4NumFbv9lRmgrXF4aQhKOdhbun2859\\nMAAAdQeuBd2HrgXdgAAAAHc3NoLXJzYQAAAgEAzChEgnBpk78/ARx1PlzzVa9VxkDaubNF\\nChG0uwQOTxXCDnEfOXBAXnJH5uI/p22JnqIfw/uDsq2wown7QUt/VyWIXDBFmsktCpCmTD\\n3QQErUpxvkFdK0xOJp+pk+KdOaeLPERrZjzB1tmH3zhr1gYTyxno4JFDKle/1++hIXEqrA\\nedYQhFsRFVjAxB6Ot1TSaHhaJLCdBjrSfpq+9tWimRDB8+TsqJgi7MjGrNd0Vn1vAkN42M\\nm5P8Xnu3ZHV1ykXWdcacyZ1qjEqpy9mk40ETnobhvT7M7DzgGVtOyxpciwnbfyU1kX0GbS\\nJFSHNmBPJjI0pkt7u0wbNQrHlQ5wK4COOHkYY3S9PFqseBhTfXFTlsza7+SoL36dC05AZ/\\nAOlvTTZ1AGZ5mlQ1zkvvJdDQDlIaogt7yNUMhfNGhy90E9Y2gIJQx1l7KSJgShMIPZSmZ2\\nPKBwjz1wQebe3sYCE0bGih+kkmFlH+Oa8dFfVbTJNwPLC4x/fZgZixq0WuB3ktzyTt1ahO\\nSIcVpJMLMCghtlYCE/Q7rlxiBPBQe/pwhB8gUwMHqSgXhBOi/xcBjqi07U6v2wkVzriAyG\\nGH2Q2PomwPOBtMaRsLDMUpXJoL0Y2Kn1jjnq71KKJ8wUuQLtwXkumt2nZUoR4NumFbv9lR\\nmgrXF4aQhKOdhbun2859MAAAADAQABAAACAQCa1QM4RdbcNZOr3RBSDp6BYmtT4wW/BmWa\\nwFjn6KiNc/vrhgFfR3GZ72P2h6os6VcQ1vXuZYa48R9gVwaVu5NE3XMwL2/qdx5Qv+lPMy\\nYITHhAkFeH8XglRW2In8XUZOXnGrcFxfej6sGegA88Jc29kRFIJYJfLhXxFgzBzQxwnM1C\\n2YVHS+WF4fy4AFDOW2cD0hrTbpp4Cg5v9B5WBym8mkfuF7UgG9mI6LA8j15s5c5V0iQJjd\\nuPsSERPB/Y8qq5J9Wfa4tUkEQu/JaTOzIjyl4re7FbDGjViMAoKcaE69J+mu4OJih4ZjKD\\nWRP+ukXsuvDRGbgqEQslTBqXQg7wpDXc1Y2kY9ApoHH7vTURgyr/CaFGq5sPG0gReQQ4wS\\n8tCg7SdzmEafQf5Ncrn38oKdnVl9Vr9FliJNAS7/FqabCcdgJGVromDUihR1V2aLjN+r8C\\n+Ky4ZTprft9YeQr93nnGX0ZmCDfoSyzAOHfGJB256rx9oPjAkiPyP5gixu86tnNRzdrv9k\\nEM6sFoyYAMsVLmtym6PTWOoV8qgDRsB70M6jaWlLJNJWgV8uMoPby3tBasZj+Sc0/5JtWW\\nJpObl/xmaaAg3nEi/OxPpePsd2vU0MskrWjeyM3aPf5HVdiXZLyFtYrOwMjeC6G+Ye3XAU\\nJk2u/yqcxNb/FEofglMQAAAQEAxK4ta5sqzJmxbuGLzF631wSeCpLFe/lTTEvpGQwPhk25\\nNyCmwaObzpCvBFDKYeDYh3rLTv8gBVeziSl5xub26iBiODnGtxPLpzi6ZTInHoOTLx7UUM\\nF/0RnI5fGxZyZKb3iaDxwu/x6fSZ+HpPIjI8jJI0VK6S0M2sSzLCnf+zUiOrCuRL2Zvjy7\\nUw53AWZt3aZuqgD2oqtR5PVfsZ/Dk9cylHl9+kTnuGaNR5n4cEwDEcnyCZ0lqZSCQ+9Ft7\\n8VXdivRGONPd8/gx4VNNBiQIQLN9Grf4W+GdZBPJzRKFc43SwlczgzajysHVPeSZmpN3V9\\nLN2UY+aJ2y3dXZx0PwAAAQEA6wiu/DAcIYOyRS/dvzLxnMsJh5q7fOanUSDEarE0y2x7S8\\nLyKDc6eAUs0uEK8oCwMShx/uvz4zV53k0y7TUmdkr3sEs/2qhf+suSNi9sQGJH29HnT589\\npS2JArJicnJhRxrnFcDZXeVMsdYh3SVR0OKCDvJ2JKLUdm/oxIwY3tY1Svm6xA8QuSfTvc\\nmHTy68loF/CWhSqF6nxpl0w7ZoG2vkJCIadj0nnIF2VEF7UiI3IIL/xp4jB/Y5jZUzPbcm\\nPgvBap4COGnPNLe4bwQ9NlRM9i4o5N68mVWQOLEzSJm3PnnDV+9REdugWK0f8fmGsUjf0k\\nK4So394EHwS39DyQAAAQEA3l55h3v07b6yCLHVV5KY5rUNSlvLflJ3TCQRsB84zS/SAscu\\nvdIIFfohq3OoweZ4QkqbyghHyv855G3na1hMUaUqjeqZkX9GW9l6JsiYwWlI/mQrKjM4iu\\nQBFJ2tsV8xTwcKRD84sEa9AVZoRCnz12/8SN2WjpugYEP0SO21iKnXDcPwKN3HKPThOrUz\\nBkxlweASoZcJQ9nX1C+vTnYY5cDwRatXAXMd+qV0OZZXcm0iohp5rQKtRyMSie22Z2ct0h\\nVEx3i2QXFbYi01cNqwln2xYs0sIHpVEb2qJPkIOlIopFMvL2R2dC1WUmIER4jhUZswFtqZ\\nWoZTsQd5/B1EuwAAABRoYWliYXJhNDA2QGdtYWlsLmNvbQECAwQF\\n-----END OPENSSH PRIVATE KEY-----\",\"files\":null,\"addedAt\":\"2026-03-18T23:08:10.357238\"}]');
INSERT INTO `friend_session_history` (`id`, `closed_at`, `created_at`, `expire_seconds`, `item_count`, `session_id`, `admin_username`, `participant_count`, `participants`, `user_id`, `items_json`) VALUES (14, '2026-03-19 15:56:33.232496', '2026-03-19 15:19:30.309145', 3600, 3, '39fbe890-1046-45bf-8d39-fb647d556944', 'Hanshao666', 2, 'Hanshao666, haibara', 7, '[{\"id\":0,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"您可以详细描述遇到的问题吗？例如：操作时是否有报错提⽰？涉及哪个功能模块？操作步骤是怎样的？\",\"files\":null,\"addedAt\":\"2026-03-19T15:22:50.43191434\"},{\"id\":1,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"# 角色\\n你是仓储系统智能客服的问题分类专家，负责准确识别用户问题的类别，以便为后续的自动回答或人工转接提供依据。你的核心任务是基于用户输入{{input_2}}的问题文本，从预设的类别中选择最匹配的一个，确保分类结果准确且唯一。\\n\\n## 技能\\n### 技能1：识别系统操作类问题\\n- **定义**：用户询问仓储系统的常规操作流程、功能使用方法、系统界面操作等与系统使用相关的问题。\\n- **判断标准**：问题中包含“如何”“怎样”“操作”“流程”“功能”“使用”等关键词，且内容与仓储系统的操作逻辑相关（如库存查询、入库/出库单创建、报表导出等）。\\n- **示例问题**：\\n  - “如何查询某商品的实时库存？”\\n  - “怎样创建新的出库单？”\\n  - “系统的库存预警功能在哪里设置？”\\n- **回复格式**：直接返回“系统操作类”\\n\\n### 技能2：识别故障排查类问题\\n- **定义**：用户反馈系统或设备运行异常、报错、功能失效等问题，需要排查原因或解决方法的问题。\\n- **判断标准**：问题中包含“故障”“报错”“无法”“失败”“异常”“怎么办”等关键词，或描述了具体的系统/设备异常现象（如“系统登录失败”“扫码枪无法识别条码”“数据同步失败”）。\\n- **示例问题**：\\n  - “系统登录时提示‘密码错误’但我确认密码正确，该如何处理？”\\n  - “货物入库后系统显示‘数据不一致’，是什么原因？”\\n  - “仓库扫码枪突然无法读取条码，该排查哪里？”\\n- **回复格式**：直接返回“故障排查类”\\n\\n### 技能3：识别无关类问题\\n- **定义**：用户问题与仓储系统的功能、操作、故障无关，属于外部话题。\\n- **判断标准**：问题内容明显与仓储系统无关（如生活咨询、其他软件问题、个人事务等），且不涉及仓储系统的任何操作或故障场景。\\n- **示例问题**：\\n  - “明天会下雨吗？”\\n  - “如何购买机票？”（与仓储无关）\\n  - “这个系统和XX系统是什么关系？”（泛泛而谈，无具体仓储相关内容）\\n- **回复格式**：直接返回“无关类”\\n\\n### 技能4：识别模糊类问题\\n- **定义**：用户问题表述模糊、语义不明确，或无法明确归类到前三项（系统操作类、故障排查类、无关类）的问题。\\n- **判断标准**：问题中缺少关键信息、表述歧义（如“系统有问题”但未说明具体故障类型），或无法从问题文本中确定是否与仓储系统相关。\\n- **示例问题**：\\n  - “系统好像出了问题，你能看看吗？”（未说明具体问题类型）\\n  - “这个系统怎么样？”（表述模糊，无明确指向）\\n  - “我遇到点麻烦，和系统有关”（缺乏具体信息）\\n- **回复格式**：直接返回“模糊类”\\n\\n## 限制\\n1. **严格归类**：仅返回预设的类别名称（“系统操作类”“故障排查类”“无关类”“模糊类”），不添加任何额外解释或内容。\\n2. **识别优先级**：按“系统操作类”→“故障排查类”→“无关类”→“模糊类”的顺序判断，若问题同时涉及多个类别特征，优先归类到更匹配的具体类别；若无法匹配任何类别，统一返回“模糊类”。\\n3. **无需工具**：无需调用外部搜索或知识库，仅基于用户问题文本内容进行判断。\\n4. **人工转接条件**：若问题无法归类到“系统操作类”或“故障排查类”，则直接返回对应类别（如无关类或模糊类），并在后续流程中进行自动回答或人工转接（具体转接逻辑由仓储系统整体流程决定，此分类任务不涉及转接决策，仅负责分类结果）。\",\"files\":null,\"addedAt\":\"2026-03-19T15:31:22.820045281\"},{\"id\":2,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"# 角色\\n你是仓储系统智能客服的问题分类专家，负责准确识别用户问题的类别，以便为后续的自动回答或人工转接提供依据。你的核心任务是基于用户输入{{input}}的问题文本，从预设的类别中选择最匹配的一个，确保分类结果准确且唯一。\\n\\n## 技能\\n### 技能1：识别系统操作类问题\\n- **定义**：用户询问仓储系统的常规操作流程、功能使用方法、系统界面操作等与系统使用相关的问题。\\n- **判断标准**：问题中包含“如何”“怎样”“操作”“流程”“功能”“使用”等关键词，且内容与仓储系统的操作逻辑相关（如库存查询、入库/出库单创建、报表导出等）。\\n- **示例问题**：\\n  - “如何查询某商品的实时库存？”\\n  - “怎样创建新的出库单？”\\n  - “系统的库存预警功能在哪里设置？”\\n- **回复格式**：直接返回“系统操作类”\\n\\n### 技能2：识别故障排查类问题\\n- **定义**：用户反馈系统或设备运行异常、报错、功能失效等问题，需要排查原因或解决方法的问题。\\n- **判断标准**：问题中包含“故障”“报错”“无法”“失败”“异常”“怎么办”等关键词，或描述了具体的系统/设备异常现象（如“系统登录失败”“扫码枪无法识别条码”“数据同步失败”）。\\n- **示例问题**：\\n  - “系统登录时提示‘密码错误’但我确认密码正确，该如何处理？”\\n  - “货物入库后系统显示‘数据不一致’，是什么原因？”\\n  - “仓库扫码枪突然无法读取条码，该排查哪里？”\\n- **回复格式**：直接返回“故障排查类”\\n\\n### 技能3：识别无关类问题\\n- **定义**：用户问题与仓储系统的功能、操作、故障无关，属于外部话题。\\n- **判断标准**：问题内容明显与仓储系统无关（如生活咨询、其他软件问题、个人事务等），且不涉及仓储系统的任何操作或故障场景。\\n- **示例问题**：\\n  - “明天会下雨吗？”\\n  - “如何购买机票？”（与仓储无关）\\n  - “这个系统和XX系统是什么关系？”（泛泛而谈，无具体仓储相关内容）\\n- **回复格式**：直接返回“无关类”\\n\\n### 技能4：识别模糊类问题\\n- **定义**：用户问题表述模糊、语义不明确，或无法明确归类到前三项（系统操作类、故障排查类、无关类）的问题。\\n- **判断标准**：问题中缺少关键信息、表述歧义（如“系统有问题”但未说明具体故障类型），或无法从问题文本中确定是否与仓储系统相关。\\n- **示例问题**：\\n  - “系统好像出了问题，你能看看吗？”（未说明具体问题类型）\\n  - “这个系统怎么样？”（表述模糊，无明确指向）\\n  - “我遇到点麻烦，和系统有关”（缺乏具体信息）\\n- **回复格式**：直接返回“模糊类”\\n\\n## 限制\\n1. **严格归类**：仅返回预设的类别名称（“系统操作类”“故障排查类”“无关类”“模糊类”），不添加任何额外解释或内容。\\n2. **识别优先级**：按“系统操作类”→“故障排查类”→“无关类”→“模糊类”的顺序判断，若问题同时涉及多个类别特征，优先归类到更匹配的具体类别；若无法匹配任何类别，统一返回“模糊类”。\\n3. **无需工具**：无需调用外部搜索或知识库，仅基于用户问题文本内容进行判断。\\n4. **人工转接条件**：若问题无法归类到“系统操作类”或“故障排查类”，则直接返回对应类别（如无关类或模糊类），并在后续流程中进行自动回答或人工转接（具体转接逻辑由仓储系统整体流程决定，此分类任务不涉及转接决策，仅负责分类结果）。\",\"files\":null,\"addedAt\":\"2026-03-19T15:32:21.890645671\"}]');
INSERT INTO `friend_session_history` (`id`, `closed_at`, `created_at`, `expire_seconds`, `item_count`, `session_id`, `admin_username`, `participant_count`, `participants`, `user_id`, `items_json`) VALUES (15, '2026-03-19 15:56:33.768745', '2026-03-19 15:19:30.309145', 3600, 3, '39fbe890-1046-45bf-8d39-fb647d556944', 'Hanshao666', 2, 'Hanshao666, haibara', 6, '[{\"id\":0,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"您可以详细描述遇到的问题吗？例如：操作时是否有报错提⽰？涉及哪个功能模块？操作步骤是怎样的？\",\"files\":null,\"addedAt\":\"2026-03-19T15:22:50.43191434\"},{\"id\":1,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"# 角色\\n你是仓储系统智能客服的问题分类专家，负责准确识别用户问题的类别，以便为后续的自动回答或人工转接提供依据。你的核心任务是基于用户输入{{input_2}}的问题文本，从预设的类别中选择最匹配的一个，确保分类结果准确且唯一。\\n\\n## 技能\\n### 技能1：识别系统操作类问题\\n- **定义**：用户询问仓储系统的常规操作流程、功能使用方法、系统界面操作等与系统使用相关的问题。\\n- **判断标准**：问题中包含“如何”“怎样”“操作”“流程”“功能”“使用”等关键词，且内容与仓储系统的操作逻辑相关（如库存查询、入库/出库单创建、报表导出等）。\\n- **示例问题**：\\n  - “如何查询某商品的实时库存？”\\n  - “怎样创建新的出库单？”\\n  - “系统的库存预警功能在哪里设置？”\\n- **回复格式**：直接返回“系统操作类”\\n\\n### 技能2：识别故障排查类问题\\n- **定义**：用户反馈系统或设备运行异常、报错、功能失效等问题，需要排查原因或解决方法的问题。\\n- **判断标准**：问题中包含“故障”“报错”“无法”“失败”“异常”“怎么办”等关键词，或描述了具体的系统/设备异常现象（如“系统登录失败”“扫码枪无法识别条码”“数据同步失败”）。\\n- **示例问题**：\\n  - “系统登录时提示‘密码错误’但我确认密码正确，该如何处理？”\\n  - “货物入库后系统显示‘数据不一致’，是什么原因？”\\n  - “仓库扫码枪突然无法读取条码，该排查哪里？”\\n- **回复格式**：直接返回“故障排查类”\\n\\n### 技能3：识别无关类问题\\n- **定义**：用户问题与仓储系统的功能、操作、故障无关，属于外部话题。\\n- **判断标准**：问题内容明显与仓储系统无关（如生活咨询、其他软件问题、个人事务等），且不涉及仓储系统的任何操作或故障场景。\\n- **示例问题**：\\n  - “明天会下雨吗？”\\n  - “如何购买机票？”（与仓储无关）\\n  - “这个系统和XX系统是什么关系？”（泛泛而谈，无具体仓储相关内容）\\n- **回复格式**：直接返回“无关类”\\n\\n### 技能4：识别模糊类问题\\n- **定义**：用户问题表述模糊、语义不明确，或无法明确归类到前三项（系统操作类、故障排查类、无关类）的问题。\\n- **判断标准**：问题中缺少关键信息、表述歧义（如“系统有问题”但未说明具体故障类型），或无法从问题文本中确定是否与仓储系统相关。\\n- **示例问题**：\\n  - “系统好像出了问题，你能看看吗？”（未说明具体问题类型）\\n  - “这个系统怎么样？”（表述模糊，无明确指向）\\n  - “我遇到点麻烦，和系统有关”（缺乏具体信息）\\n- **回复格式**：直接返回“模糊类”\\n\\n## 限制\\n1. **严格归类**：仅返回预设的类别名称（“系统操作类”“故障排查类”“无关类”“模糊类”），不添加任何额外解释或内容。\\n2. **识别优先级**：按“系统操作类”→“故障排查类”→“无关类”→“模糊类”的顺序判断，若问题同时涉及多个类别特征，优先归类到更匹配的具体类别；若无法匹配任何类别，统一返回“模糊类”。\\n3. **无需工具**：无需调用外部搜索或知识库，仅基于用户问题文本内容进行判断。\\n4. **人工转接条件**：若问题无法归类到“系统操作类”或“故障排查类”，则直接返回对应类别（如无关类或模糊类），并在后续流程中进行自动回答或人工转接（具体转接逻辑由仓储系统整体流程决定，此分类任务不涉及转接决策，仅负责分类结果）。\",\"files\":null,\"addedAt\":\"2026-03-19T15:31:22.820045281\"},{\"id\":2,\"senderId\":7,\"senderUsername\":\"Hanshao666\",\"type\":\"TEXT\",\"content\":\"# 角色\\n你是仓储系统智能客服的问题分类专家，负责准确识别用户问题的类别，以便为后续的自动回答或人工转接提供依据。你的核心任务是基于用户输入{{input}}的问题文本，从预设的类别中选择最匹配的一个，确保分类结果准确且唯一。\\n\\n## 技能\\n### 技能1：识别系统操作类问题\\n- **定义**：用户询问仓储系统的常规操作流程、功能使用方法、系统界面操作等与系统使用相关的问题。\\n- **判断标准**：问题中包含“如何”“怎样”“操作”“流程”“功能”“使用”等关键词，且内容与仓储系统的操作逻辑相关（如库存查询、入库/出库单创建、报表导出等）。\\n- **示例问题**：\\n  - “如何查询某商品的实时库存？”\\n  - “怎样创建新的出库单？”\\n  - “系统的库存预警功能在哪里设置？”\\n- **回复格式**：直接返回“系统操作类”\\n\\n### 技能2：识别故障排查类问题\\n- **定义**：用户反馈系统或设备运行异常、报错、功能失效等问题，需要排查原因或解决方法的问题。\\n- **判断标准**：问题中包含“故障”“报错”“无法”“失败”“异常”“怎么办”等关键词，或描述了具体的系统/设备异常现象（如“系统登录失败”“扫码枪无法识别条码”“数据同步失败”）。\\n- **示例问题**：\\n  - “系统登录时提示‘密码错误’但我确认密码正确，该如何处理？”\\n  - “货物入库后系统显示‘数据不一致’，是什么原因？”\\n  - “仓库扫码枪突然无法读取条码，该排查哪里？”\\n- **回复格式**：直接返回“故障排查类”\\n\\n### 技能3：识别无关类问题\\n- **定义**：用户问题与仓储系统的功能、操作、故障无关，属于外部话题。\\n- **判断标准**：问题内容明显与仓储系统无关（如生活咨询、其他软件问题、个人事务等），且不涉及仓储系统的任何操作或故障场景。\\n- **示例问题**：\\n  - “明天会下雨吗？”\\n  - “如何购买机票？”（与仓储无关）\\n  - “这个系统和XX系统是什么关系？”（泛泛而谈，无具体仓储相关内容）\\n- **回复格式**：直接返回“无关类”\\n\\n### 技能4：识别模糊类问题\\n- **定义**：用户问题表述模糊、语义不明确，或无法明确归类到前三项（系统操作类、故障排查类、无关类）的问题。\\n- **判断标准**：问题中缺少关键信息、表述歧义（如“系统有问题”但未说明具体故障类型），或无法从问题文本中确定是否与仓储系统相关。\\n- **示例问题**：\\n  - “系统好像出了问题，你能看看吗？”（未说明具体问题类型）\\n  - “这个系统怎么样？”（表述模糊，无明确指向）\\n  - “我遇到点麻烦，和系统有关”（缺乏具体信息）\\n- **回复格式**：直接返回“模糊类”\\n\\n## 限制\\n1. **严格归类**：仅返回预设的类别名称（“系统操作类”“故障排查类”“无关类”“模糊类”），不添加任何额外解释或内容。\\n2. **识别优先级**：按“系统操作类”→“故障排查类”→“无关类”→“模糊类”的顺序判断，若问题同时涉及多个类别特征，优先归类到更匹配的具体类别；若无法匹配任何类别，统一返回“模糊类”。\\n3. **无需工具**：无需调用外部搜索或知识库，仅基于用户问题文本内容进行判断。\\n4. **人工转接条件**：若问题无法归类到“系统操作类”或“故障排查类”，则直接返回对应类别（如无关类或模糊类），并在后续流程中进行自动回答或人工转接（具体转接逻辑由仓储系统整体流程决定，此分类任务不涉及转接决策，仅负责分类结果）。\",\"files\":null,\"addedAt\":\"2026-03-19T15:32:21.890645671\"}]');
COMMIT;

-- ----------------------------
-- Table structure for friendships
-- ----------------------------
DROP TABLE IF EXISTS `friendships`;
CREATE TABLE `friendships` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accepted_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `friend_id` bigint NOT NULL,
  `status` enum('ACCEPTED','BLOCKED','PENDING') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjwaac0iw9d1fu58mx7afwf9f4` (`user_id`,`friend_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of friendships
-- ----------------------------
BEGIN;
INSERT INTO `friendships` (`id`, `accepted_at`, `created_at`, `friend_id`, `status`, `user_id`) VALUES (9, '2026-03-18 22:27:17.217148', '2026-03-18 22:26:49.724651', 6, 'ACCEPTED', 7);
INSERT INTO `friendships` (`id`, `accepted_at`, `created_at`, `friend_id`, `status`, `user_id`) VALUES (10, '2026-03-18 22:27:17.593253', '2026-03-18 22:27:17.678785', 7, 'ACCEPTED', 6);
INSERT INTO `friendships` (`id`, `accepted_at`, `created_at`, `friend_id`, `status`, `user_id`) VALUES (15, '2026-03-19 16:17:48.149575', '2026-03-19 16:05:46.115157', 9, 'ACCEPTED', 8);
INSERT INTO `friendships` (`id`, `accepted_at`, `created_at`, `friend_id`, `status`, `user_id`) VALUES (16, '2026-03-19 16:17:48.217513', '2026-03-19 16:17:48.226795', 8, 'ACCEPTED', 9);
COMMIT;

-- ----------------------------
-- Table structure for moment_images
-- ----------------------------
DROP TABLE IF EXISTS `moment_images`;
CREATE TABLE `moment_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_live_photo` bit(1) NOT NULL,
  `moment_id` bigint NOT NULL,
  `sort_order` int DEFAULT NULL,
  `video_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of moment_images
-- ----------------------------
BEGIN;
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (4, 'https://minio.haikari.top/byteferry/moment/image/a4dd1f93-97fc-46b5-9eea-a6ab5d9baf5d.jpeg', b'0', 4, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (5, 'http://154.94.235.178:9000/byteferry/moment/image/a82da22d-6f97-46fa-bbd4-1811848a0060.jpeg', b'0', 5, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (6, 'http://154.94.235.178:9000/byteferry/moment/image/6071bdeb-c285-481a-a472-2c46fe60e970.jpeg', b'0', 6, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (7, 'http://154.94.235.178:9000/byteferry/moment/image/1cba99a9-9e83-462c-839f-8d5c03d0f432.jpeg', b'0', 6, 1, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (8, 'http://154.94.235.178:9000/byteferry/moment/image/7563097b-da13-44b5-a25a-14a2544684f5.jpeg', b'0', 6, 2, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (9, 'http://154.94.235.178:9000/byteferry/moment/image/67dcdc0c-c766-4963-8c97-6f9a08eef2b8.jpeg', b'0', 6, 3, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (10, 'http://154.94.235.178:9000/byteferry/moment/image/57bd8015-6bc1-41ad-9308-6480d7429ef5.jpeg', b'0', 6, 4, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (11, 'http://154.94.235.178:9000/byteferry/moment/image/024b9d01-a4f5-4df6-b066-8c980e3560b0.jpeg', b'0', 6, 5, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (12, 'http://154.94.235.178:9000/byteferry/moment/image/d6f828bf-0bb7-4d1a-afad-d36a59b05308.jpeg', b'0', 6, 6, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (13, 'http://154.94.235.178:9000/byteferry/moment/image/b5283227-8a17-4b51-85f2-b1cee1dc52c8.jpeg', b'0', 7, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (14, 'http://154.94.235.178:9000/byteferry/moment/image/254b6ed5-a838-44e8-87d9-6b3e447a759e.jpeg', b'0', 8, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (15, 'http://154.94.235.178:9000/byteferry/moment/image/1dccdf7a-41c1-4f58-bc0d-a11467ef075d.jpeg', b'0', 9, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (16, 'http://154.94.235.178:9000/byteferry/moment/image/c7a03a32-4652-4fd9-8c85-2b3c2dcbbe92.jpeg', b'0', 10, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (17, 'http://154.94.235.178:9000/byteferry/moment/image/fda336d7-06f1-47be-ae93-dee6522e9cca.jpeg', b'0', 11, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (18, 'http://154.94.235.178:9000/byteferry/moment/image/1bcdca82-db99-4cdb-ad29-3dbd0146df7a.jpeg', b'0', 11, 1, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (19, 'http://154.94.235.178:9000/byteferry/moment/image/731d1c2d-674a-4069-b10b-083ba715fa96.jpeg', b'0', 11, 2, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (20, 'http://154.94.235.178:9000/byteferry/moment/image/246573e0-5dab-49aa-85fb-5b3fce8367fb.jpeg', b'0', 11, 3, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (21, 'http://154.94.235.178:9000/byteferry/moment/image/cbae4db4-5520-4590-8eb3-e531c9b1d241.jpeg', b'0', 12, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (22, 'http://154.94.235.178:9000/byteferry/moment/image/c1f9aa0a-c685-4230-ad8a-915f25e75367.jpeg', b'0', 12, 1, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (23, 'http://154.94.235.178:9000/byteferry/moment/image/6ba6b960-253d-4452-a11f-e4d298f35b44.jpeg', b'0', 12, 2, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (24, 'http://154.94.235.178:9000/byteferry/moment/image/680db49f-ed0a-4f75-b384-d001c8cbd76a.jpeg', b'0', 12, 3, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (25, 'http://154.94.235.178:9000/byteferry/moment/image/0c4e09dc-3af5-4a7d-a9ed-b23b95cebe5f.jpeg', b'0', 12, 4, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (26, 'http://154.94.235.178:9000/byteferry/moment/image/0790bab8-d1d2-49a7-8b0d-2936c7ab2c2c.jpeg', b'0', 13, 0, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (27, 'http://154.94.235.178:9000/byteferry/moment/image/037f4410-154b-4123-9441-c9cec978e423.jpeg', b'0', 13, 1, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (28, 'http://154.94.235.178:9000/byteferry/moment/image/ffd83fd0-ccbb-496e-94c5-9b2c013bbf54.jpeg', b'0', 13, 2, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (29, 'http://154.94.235.178:9000/byteferry/moment/image/68c83de9-b236-4079-ae87-57075fe4f4fd.jpeg', b'0', 13, 3, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (30, 'http://154.94.235.178:9000/byteferry/moment/image/383b82ca-3c98-4fe4-9923-c60d82532d7c.jpeg', b'0', 13, 4, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (31, 'http://154.94.235.178:9000/byteferry/moment/image/74744e0b-313e-4ee5-9632-ab514a7973f7.jpeg', b'0', 13, 5, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (32, 'http://154.94.235.178:9000/byteferry/moment/image/a7cb04e9-5f93-4695-8529-595435ca143a.jpeg', b'0', 13, 6, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (33, 'http://154.94.235.178:9000/byteferry/moment/image/131d04d6-f254-47d0-bcf9-e11755f8ad8f.jpeg', b'0', 13, 7, NULL);
INSERT INTO `moment_images` (`id`, `image_url`, `is_live_photo`, `moment_id`, `sort_order`, `video_url`) VALUES (34, 'http://154.94.235.178:9000/byteferry/moment/image/0e3853cc-2895-40a8-92d0-fb5d2074a921.jpeg', b'0', 13, 8, NULL);
COMMIT;

-- ----------------------------
-- Table structure for moment_read_status
-- ----------------------------
DROP TABLE IF EXISTS `moment_read_status`;
CREATE TABLE `moment_read_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_read_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKg8oi5cws0xqpohhvhd4i9yun5` (`user_id`),
  KEY `idx_moment_read_status_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of moment_read_status
-- ----------------------------
BEGIN;
INSERT INTO `moment_read_status` (`id`, `last_read_at`, `user_id`) VALUES (1, '2026-03-22 14:10:02.561468', 11);
COMMIT;

-- ----------------------------
-- Table structure for moment_share_links
-- ----------------------------
DROP TABLE IF EXISTS `moment_share_links`;
CREATE TABLE `moment_share_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `share_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8697h64a10o77rv0tey0dum3u` (`share_code`),
  UNIQUE KEY `UKtjkt3setdybvpe90nrk3ujgva` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of moment_share_links
-- ----------------------------
BEGIN;
INSERT INTO `moment_share_links` (`id`, `created_at`, `share_code`, `user_id`) VALUES (1, '2026-03-22 00:28:20.146069', 'eb92028299774133af78937731c4e39b', 11);
COMMIT;

-- ----------------------------
-- Table structure for moment_templates
-- ----------------------------
DROP TABLE IF EXISTS `moment_templates`;
CREATE TABLE `moment_templates` (
  `id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `html_template` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `preview_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of moment_templates
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for moment_visibility_rules
-- ----------------------------
DROP TABLE IF EXISTS `moment_visibility_rules`;
CREATE TABLE `moment_visibility_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `moment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKti6ufb735etblpf2galbhrdqf` (`moment_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of moment_visibility_rules
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for moments
-- ----------------------------
DROP TABLE IF EXISTS `moments`;
CREATE TABLE `moments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `text_content` text COLLATE utf8mb4_unicode_ci,
  `card_mode` tinyint(1) DEFAULT '0',
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `visibility` enum('HIDDEN_FROM','PRIVATE','PUBLIC','VISIBLE_TO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of moments
-- ----------------------------
BEGIN;
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (5, '2026-03-22 01:04:16.277924', '开心开心', 0, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (6, '2026-03-22 01:14:46.549903', 'haerin', 0, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (7, '2026-03-22 01:15:58.093088', 'haerin haerin', 0, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (8, '2026-03-22 01:17:14.453894', 'Melancholy', 0, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (9, '2026-03-22 01:17:55.731845', 'pink！', 0, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (10, '2026-03-22 01:18:34.329842', 'give a like', 0, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (11, '2026-03-22 12:00:38.896347', '哈喽👋', 1, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (12, '2026-03-22 12:01:15.423269', 'nice', 1, NULL, 11, 'PUBLIC');
INSERT INTO `moments` (`id`, `created_at`, `text_content`, `card_mode`, `updated_at`, `user_id`, `visibility`) VALUES (13, '2026-03-22 13:55:31.354806', '纳尼纳尼', 1, NULL, 11, 'PUBLIC');
COMMIT;

-- ----------------------------
-- Table structure for space_files
-- ----------------------------
DROP TABLE IF EXISTS `space_files`;
CREATE TABLE `space_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) DEFAULT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `mime_type` varchar(100) DEFAULT NULL,
  `item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgr65d4jmy4a8q2csinporj30d` (`item_id`),
  CONSTRAINT `FKgr65d4jmy4a8q2csinporj30d` FOREIGN KEY (`item_id`) REFERENCES `space_items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of space_files
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for space_items
-- ----------------------------
DROP TABLE IF EXISTS `space_items`;
CREATE TABLE `space_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) NOT NULL,
  `type` enum('FILE','IMAGE','TEXT') NOT NULL,
  `user_id` bigint NOT NULL,
  `expire_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of space_items
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(50) NOT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `email_bound` tinyint(1) NOT NULL DEFAULT '0',
  `gender` enum('FEMALE','MALE','OTHER','UNKNOWN') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`id`, `created_at`, `password`, `username`, `avatar`, `email`, `email_bound`, `gender`) VALUES (11, '2026-03-20 23:32:59.259960', '$2a$10$lpAQHa9VMwwZvk6kPjvBreKkQgfAbAZuaSwyD1TZucSMRycjpo0oy', 'haibara', '/images/default-avatar.jpg', 'haibara406@gmail.com', 1, 'MALE');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
