# 本地第三方 JAR 存放目录 (libs)

本项目已在 `pom.xml` 中配置了对本地 JAR 包的支持，包括UIAS认证相关和ESF相关的依赖包。

## 已配置的本地JAR包

### UIAS相关包
- `speedstudio/uias-spring-boot-starter/1.0.7-RELEASE/uias-spring-boot-starter-1.0.7-RELEASE.jar`
- `speedstudio/uias-sdk/1.0.7-RELEASE/uias-sdk-1.0.7-RELEASE.jar`

### ESF相关包
- `esf/esf-sdk/3.2-RELEASE/esf-sdk-3.2-RELEASE.jar`

### 其他包
- `trshybase-api.jar` - 海贝TRS客户端API

## 使用方法

### 1. Profile 环境配置
项目已在 `pom.xml` 中通过 Maven Profile 区分环境：
- **`dev`（开发环境，默认激活）**：自动引用本 `libs/` 目录下的本地 JAR（`system` 作用域）。本地在 IDEA 中直接刷新即可开发调试，无需额外配置。
- **`test` / `prod` / `repo`（测试与生产环境）**：从公司私有 Maven 仓库拉取远程依赖（`compile` 作用域），构建打包时通过 `-P prod`（或 `-P test` / `-P repo`）切换。

示例打包命令：
```bash
# 本地开发打包（默认使用本地 JAR）
mvn clean package

# 生产环境打包（强制使用远程 Maven 仓库）
mvn clean package -P prod
```

### 2. 目录结构规范
本地JAR包应按照以下结构组织：
```text
libs/
├── esf/
│   ├── esf-sdk/3.2-RELEASE/esf-sdk-3.2-RELEASE.jar
│   ├── esf-auth-client/1.0.0-RELEASE/esf-auth-client-1.0.0-RELEASE.jar
│   ├── esf-auth-core/1.0.0-RELEASE/esf-auth-core-1.0.0-RELEASE.jar
│   ├── esf-trafficstainer/3.2-RELEASE/esf-trafficstainer-3.2-RELEASE.jar
│   └── esf-unitroute-core/1.0.1-RELEASE/esf-unitroute-core-1.0.1-RELEASE.jar
├── speedstudio/
│   ├── uias-spring-boot-starter/1.0.7-RELEASE/uias-spring-boot-starter-1.0.7-RELEASE.jar
│   └── uias-sdk/1.0.7-RELEASE/uias-sdk-1.0.7-RELEASE.jar
└── trshybase-api.jar
```

### 3. IDE刷新
在 IDEA 中点击 Maven 的 **Reload All Maven Projects** 刷新，即可在代码中直接 import 并使用这些 JAR 包中的类！

## 打包说明

`pom.xml` 中的 `spring-boot-maven-plugin` 已经开启了 `<includeSystemScope>true</includeSystemScope>`，执行 `mvn package` 时会自动将本项目使用的本地JAR包打包进最终的 Fat JAR，部署到服务器无需额外配置。

## 注意事项

1. **scope为system的依赖不会被Maven传递**：如果其他依赖需要这些包，需要手动配置
2. **版本管理**：本地JAR包的版本变更需要手动更新pom.xml中的version和systemPath
3. **团队协作**：确保所有团队成员的libs目录结构一致
4. **生产部署**：Spring Boot Fat JAR会包含这些本地JAR包，无需额外配置
5. **依赖冲突**：如果有版本冲突，需要在exclusions中排除冲突的传递依赖
