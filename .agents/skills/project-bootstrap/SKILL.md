---
name: project-bootstrap
description: 基于本母版初始化或重新建立兼容 Java 8 的 Spring Boot 生产服务基线。适用于创建新服务、复制母版、重命名包/制品、建立环境、规划受支持 Java 版本升级，或检查新仓库是否具备全部必需质量与交付文件。
---

# 项目初始化

## 工作流

1. 阅读 `AGENTS.md`、`REQUIREMENTS.md` 和 `ARCHITECTURE.md`；只有无法安全推断且影响发布的产品决策才可以阻塞进度。
2. 正式命名确认后，一致替换临时技术代号 `intelligent-qa-audit-service`、`com.acme.intelligentqa`、Maven 坐标、应用名、镜像名和责任人占位符。
3. 保留领域/应用/适配器/配置边界和 ArchUnit 规则。
4. 为 GoldenDB、Redis 和 OBS 选择 Profile 与外部配置，保证凭据不进入文件和提交。
5. 始终将本地 MySQL 标注为 GoldenDB 兼容性替身，把真实 GoldenDB 版本/拓扑验证记录为必需证据。
6. 更新需求、责任归属、SLO、数据分类、部署和回滚章节。
7. 运行 `mvn -B -ntp clean verify`；修复全部门禁，不得削弱门禁。
8. 记录是否具备 Java 8/Spring Boot 2.7 扩展支持；否则制定带日期的计划，升级到受支持的 Java LTS/Spring Boot 版本。
9. 按需启动本地依赖，验证健康检查和一条 API 链路，然后记录剩余环境验证项。

## 完成输出

报告已重命名的范围、选定的运行时假设、门禁证据、尚未解决的 `TBD`，以及仍须在生产环境执行的准确验证项。
