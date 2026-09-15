# 本地JAR包依赖配置示例

## UIAS和ESF相关依赖配置

如果需要使用UIAS认证和ESF相关的本地JAR包而不是Maven仓库中的依赖，可以按照以下步骤配置：

### 1. 在pom.xml中添加本地JAR依赖

```xml
<!-- UIAS Spring Boot Starter -->
<dependency>
    <groupId>com.spdb.speedstudio</groupId>
    <artifactId>uias-spring-boot-starter</artifactId>
    <version>1.0.7-RELEASE</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/speedstudio/uias-spring-boot-starter/1.0.7-RELEASE/uias-spring-boot-starter-1.0.7-RELEASE.jar</systemPath>
    <exclusions>
        <exclusion>
            <artifactId>asm</artifactId>
            <groupId>org.ow2.asm</groupId>
        </exclusion>
        <exclusion>
            <artifactId>guava</artifactId>
            <groupId>com.google.guava</groupId>
        </exclusion>
        <exclusion>
            <artifactId>commons-lang</artifactId>
            <groupId>commons-lang</groupId>
        </exclusion>
        <exclusion>
            <artifactId>commons-httpclient</artifactId>
            <groupId>commons-httpclient</groupId>
        </exclusion>
        <exclusion>
            <artifactId>commons-logging</artifactId>
            <groupId>commons-logging</groupId>
        </exclusion>
    </exclusions>
</dependency>

<!-- UIAS SDK -->
<dependency>
    <groupId>com.spdb.speedstudio</groupId>
    <artifactId>uias-sdk</artifactId>
    <version>1.0.7-RELEASE</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/speedstudio/uias-sdk/1.0.7-RELEASE/uias-sdk-1.0.7-RELEASE.jar</systemPath>
</dependency>

<!-- ESF SDK (可选) -->
<dependency>
    <groupId>com.trs</groupId>
    <artifactId>esf-sdk</artifactId>
    <version>3.2-RELEASE</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/esf/esf-sdk/3.2-RELEASE/esf-sdk-3.2-RELEASE.jar</systemPath>
</dependency>

<!-- TRS HyBase API -->
<dependency>
    <groupId>com.trs</groupId>
    <artifactId>api</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/trshybase-api.jar</systemPath>
</dependency>
```

### 2. 确保Spring Boot打包配置正确

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <!-- 确保本地system scope的jar打包到fat jar中 -->
        <includeSystemScope>true</includeSystemScope>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

### 3. 目录结构要求

确保libs目录按以下结构组织：
```
libs/
├── esf/
│   └── esf-sdk/
│       └── 3.2-RELEASE/
│           └── esf-sdk-3.2-RELEASE.jar
├── speedstudio/
│   ├── uias-spring-boot-starter/
│   │   └── 1.0.7-RELEASE/
│   │       └── uias-spring-boot-starter-1.0.7-RELEASE.jar
│   └── uias-sdk/
│       └── 1.0.7-RELEASE/
│           └── uias-sdk-1.0.7-RELEASE.jar
└── trshybase-api.jar
```

### 4. 配置文件设置

在配置文件中添加UIAS相关配置：

```properties
# application-dev.xml
<entry key="app.uias.redirect-url">${DEV_UIAS_REDIRECT_URL:https://uias-idp.sit.spdb.com/NIDP/}</entry>
<entry key="app.uias.token-type">${DEV_UIAS_TOKEN_TYPE:SAMLResponse}</entry>
<entry key="app.uias.realm-name">${DEV_UIAS_REALM_NAME:myrealm}</entry>
<entry key="app.uias.dc-name">${DEV_UIAS_DC_NAME:biz}</entry>
<entry key="app.uias.cert-path">${DEV_UIAS_CERT_PATH:classpath:idp-2023-test.cer}</entry>
<entry key="app.uias.check-recipient">${DEV_UIAS_CHECK_RECIPIENT:true}</entry>
<entry key="app.uias.validation-errorpage">${DEV_UIAS_VALIDATION_ERROR_PAGE:/api/login?error=validation}</entry>
```

### 5. 使用示例

在代码中正常使用这些JAR包中的类：

```java
import com.spdb.speedstudio.uias.authentication.filter.SAMLAuthTomcatFilter;
import com.spdb.speedstudio.uias.authorisation.client.s120030044.NewAuthrQueryClient;
import com.spdb.speedstudio.uias.authorisation.client.s120030044.NewAuthrQueryClient_ESF;

// 正常使用这些类
@Configuration
public class WebMvcConfig {
    
    @Bean
    public FilterRegistrationBean<SAMLAuthTomcatFilter> uiasFilter() {
        // 配置UIAS过滤器
    }
    
    @Bean
    public NewAuthrQueryClient newAuthrQueryClient() {
        // 创建UIAS客户端
    }
}
```

### 6. 注意事项

1. **本地依赖不传递**：system scope的依赖不会被Maven自动传递给其他模块
2. **IDEA刷新**：修改依赖后需要刷新Maven项目（Reload All Maven Projects）
3. **团队同步**：确保团队成员的libs目录结构一致
4. **版本管理**：版本变更需要手动更新pom.xml中的配置
5. **打包验证**：部署前验证fat jar中是否包含了本地JAR包

### 7. 验证方法

```bash
# 编译验证
mvn clean compile

# 测试编译验证  
mvn test-compile

# 打包验证（检查fat jar中是否包含本地jar）
mvn clean package
jar tf target/*.jar | grep -E "(uias|esf|trshybase)"
```