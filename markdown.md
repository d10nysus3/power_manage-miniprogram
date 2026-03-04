# Power Manage 项目文档

生成时间：2025-12-13 09:42:16

> 本文档基于当前仓库的实际文件结构与内容自动生成，符合 CommonMark 规范，并包含项目结构、模块说明、核心代码片段、依赖清单与使用说明。

## 项目结构

```text
d:\Projects\power_manage
├─ .idea/
├─ .vite/
├─ frontend/                      # 前端（Vue3 + Vite + Element Plus）
│  ├─ .vscode/
│  ├─ dist/                       # 构建产物
│  ├─ public/
│  ├─ src/
│  │  ├─ assets/
│  │  ├─ components/
│  │  ├─ composables/
│  │  ├─ enums/
│  │  ├─ hooks/
│  │  ├─ utils/
│  │  │  ├─ api.ts
│  │  │  └─ websocket.ts
│  │  ├─ views/
│  │  ├─ App.vue
│  │  ├─ main.ts
│  │  ├─ router.ts
│  │  └─ style.css
│  ├─ .env
│  ├─ index.html
│  ├─ list-icons.js
│  ├─ package.json
│  ├─ package-lock.json
│  ├─ tsconfig*.json
│  └─ vite.config.ts
├─ src/                           # 后端（Java Spring Boot）
│  ├─ main/
│  │  ├─ java/com/powermanage/
│  │  │  ├─ config/
│  │  │  │  ├─ SecurityConfig.java
│  │  │  │  ├─ WebSocketConfig.java
│  │  │  │  └─ CustomHandshakeHandler.java
│  │  │  ├─ controller/
│  │  │  ├─ entity/
│  │  │  ├─ enums/
│  │  │  ├─ filter/
│  │  │  │  └─ JwtAuthenticationFilter.java
│  │  │  ├─ repository/
│  │  │  ├─ service/
│  │  │  │  └─ impl/
│  │  │  ├─ util/
│  │  │  ├─ utils/
│  │  │  └─ PowerManageApplication.java
│  │  └─ resources/
│  │     ├─ application.properties
│  │     ├─ application-anomaly-detection.yml
│  │     └─ init_database.sql
├─ target/                        # 后端编译产物
├─ markdown.md                    # 本文件
├─ package.json                   # 根目录依赖（脚本/调试用）
├─ package-lock.json
└─ pom.xml                        # Maven 构建文件
```

## 模块与主要文件说明

### 后端（Spring Boot）
- `d:\Projects\power_manage\src\main\java\com\powermanage\PowerManageApplication.java`
  - 应用入口，排除默认 `SecurityAutoConfiguration`。
- `d:\Projects\power_manage\src\main\java\com\powermanage\config\SecurityConfig.java`
  - Spring Security 规则，配置 CORS/CSRF、接口授权、角色限制与 JWT 过滤器。
- `d:\Projects\power_manage\src\main\java\com\powermanage\config\WebSocketConfig.java`
  - 启用 STOMP，端点 `/ws`，应用前缀 `/app`，主题 `/topic`、队列 `/queue`，用户目的 `/user`。
- `d:\Projects\power_manage\src\main\java\com\powermanage\config\CustomHandshakeHandler.java`
  - 握手阶段基于 `SecurityContext` 绑定用户标识。
- `d:\Projects\power_manage\src\main\java\com\powermanage\filter\JwtAuthenticationFilter.java`
  - 简易 Base64 token 解析，从仓库加载用户与角色，注入认证上下文。
- 其他模块：`controller/`（REST 与 WebSocket 控制器）、`entity/`（JPA 实体）、`repository/`（JPA 仓储接口）、`service/impl/`（业务实现）、`utils/`（工具）。

### 前端（Vue3 + Vite + Element Plus）
- `d:\Projects\power_manage\frontend\src\main.ts`
  - 应用引导，注册 ElementPlus 与路由。
- `d:\Projects\power_manage\frontend\src\router.ts`
  - 路由与导航守卫，基于本地存储 `token` 与 `role` 控制访问。
- `d:\Projects\power_manage\frontend\src\utils\api.ts`
  - Axios 客户端：自动附加 `Authorization`、错误处理与退避重试；`/api` 代理。
- `d:\Projects\power_manage\frontend\src\utils\websocket.ts`
  - STOMP 客户端封装：用户队列订阅、断线重连、消息分发与回调。
- `d:\Projects\power_manage\frontend\vite.config.ts`
  - 开发代理：`/api` → `http://localhost:8081/power_manage/api`；`/ws` → `ws://localhost:8081/power_manage/ws`。

## 核心代码片段

> 以下片段为关键函数/类的节选，并添加说明性注释。

### 应用入口（后端）
文件：`d:\Projects\power_manage\src\main\java\com\powermanage\PowerManageApplication.java`

```java
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class}) // 排除默认安全自动配置，改用自定义安全规则
public class PowerManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(PowerManageApplication.class, args); // 启动 Spring Boot 应用
    }
}
```

### 安全配置（后端）
文件：`d:\Projects\power_manage\src\main\java\com\powermanage\config\SecurityConfig.java`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors().and()                 // 允许跨域
        .csrf().disable()             // 禁用 CSRF，用于纯 API 场景
        .authorizeRequests()
            .antMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh-token").permitAll() // 认证接口放行
            .antMatchers("/ws/**").permitAll() // WebSocket 握手放行
            .antMatchers("/static/**", "/public/**", "/css/**", "/js/**", "/images/**").permitAll() // 静态资源放行
            .antMatchers("/api/admin/**").hasRole("ADMIN") // 管理接口需 ADMIN 角色
            .antMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER") // 管理员或经理可访问
            .anyRequest().authenticated() // 其他请求需认证
        .and()
        .formLogin().disable()
        .httpBasic().disable()
        .headers().frameOptions().disable();

    // 在用户名密码过滤器之前加入自定义 JWT 过滤器
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### WebSocket 配置（后端）
文件：`d:\Projects\power_manage\src\main\java\com\powermanage\config\WebSocketConfig.java`

```java
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 开启简单代理：主题与队列
        config.setApplicationDestinationPrefixes("/app"); // 应用消息前缀
        config.setUserDestinationPrefix("/user"); // 用户目的地前缀
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new CustomHandshakeHandler()); // 自定义握手，绑定用户

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS(); // 兼容 SockJS 回退
    }
}
```

### 自定义握手（后端）
文件：`d:\Projects\power_manage\src\main\java\com\powermanage\config\CustomHandshakeHandler.java`

```java
public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            if (username != null && !username.isEmpty()) {
                // 使用认证用户名作为 WebSocket Principal
                return () -> username;
            }
        }
        return null; // 未认证时不绑定用户
    }
}
```

### JWT 过滤器（后端）
文件：`d:\Projects\power_manage\src\main\java\com\powermanage\filter\JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // 登录/注册、WebSocket 握手与静态资源直接放行
        if (path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/register") ||
            path.startsWith("/ws/") ||
            path.startsWith("/static/") ||
            path.startsWith("/public/") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            String username = parseUsername(token); // Base64("username:token")
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<SystemUser> userOpt = systemUserRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    SystemUser user = userOpt.get();
                    // 将角色注入到认证上下文中
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### WebSocket 客户端（前端）
文件：`d:\Projects\power_manage\frontend\src\utils\websocket.ts`

```ts
export class WebSocketClient {
  // 连接与重连控制、订阅管理与回调
  private stompClient: Client | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 6;
  private reconnectInterval = 2000;
  private isConnecting = false;
  private userId: string | null = null;

  connect(userId: string): Promise<void> {
    // 读取本地 token 并构建 brokerURL：ws(s)://host:port/power_manage/ws
    const token = localStorage.getItem('token');
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const host = window.location.hostname;
    const port = import.meta.env.VITE_WS_PORT || '8081';
    const brokerURL = `${protocol}://${host}:${port}/power_manage/ws`;
    // 省略：stompClient 初始化、订阅与重连逻辑
  }
}
```

## 依赖清单

### 后端（Maven）`d:\Projects\power_manage\pom.xml`

```xml
<properties>
  <java.version>17</java.version>
</properties>
<dependencies>
  <dependency>spring-boot-starter-web</dependency>
  <dependency>spring-boot-starter-security</dependency>
  <dependency>spring-boot-starter-validation</dependency>
  <dependency>spring-boot-starter-data-jpa</dependency>
  <dependency>spring-boot-starter-websocket</dependency>
  <dependency>mysql:mysql-connector-java:8.0.32</dependency>
  <dependency>org.json:json:20231013</dependency>
  <dependency>org.apache.poi:poi:5.2.3</dependency>
  <dependency>org.apache.poi:poi-ooxml:5.2.3</dependency>
  <dependency>com.itextpdf:itextpdf:5.5.13.3</dependency>
  <dependency>lombok (optional)</dependency>
  <dependency>spring-boot-starter-test (test)</dependency>
</dependencies>
```

### 前端（子项目）`d:\Projects\power_manage\frontend\package.json`

```json
{
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.2",
    "@stomp/stompjs": "^7.0.0",
    "axios": "^1.6.2",
    "echarts": "^6.0.0",
    "element-plus": "^2.11.4",
    "sockjs-client": "^1.6.1",
    "vue": "^3.5.21",
    "vue-router": "^4.5.1",
    "xlsx": "^0.18.5"
  },
  "devDependencies": {
    "@types/echarts": "^5.0.0",
    "@vitejs/plugin-vue": "^6.0.1",
    "@vue/tsconfig": "^0.8.1",
    "typescript": "~5.8.3",
    "vite": "^7.1.7",
    "vue-tsc": "^3.0.7"
  }
}
```

### 根目录 `d:\Projects\power_manage\package.json`

```json
{
  "dependencies": {
    "@stomp/stompjs": "^7.2.1",
    "axios": "^1.12.2",
    "ws": "^8.18.3"
  }
}
```

## 使用说明

### 环境要求
- 后端：`Java 17`、`MySQL 8+`。
- 前端：`Node.js`（建议使用当前 LTS），`npm` 或 `pnpm`。

### 安装与初始化

```bash
# 后端（根目录）
mvn clean package

# 初始化数据库（可选，在 MySQL 中执行）
# 文件：d:\Projects\power_manage\src\main\resources\init_database.sql

# 前端（进入 frontend 子目录）
cd frontend
npm install
```

### 开发与运行

```bash
# 后端开发运行（根目录）
mvn spring-boot:run
# 后端访问：
# HTTP:  http://localhost:8081/power_manage/api
# WS:    ws://localhost:8081/power_manage/ws

# 前端开发运行（frontend 子目录）
npm run dev
# 默认开发端口：5173（由 Vite 决定），已配置代理：
# /api → http://localhost:8081/power_manage/api
# /ws  → ws://localhost:8081/power_manage/ws
```

### 基本用法
- 登录后本地存储 `token`、`role`、`userId`；路由守卫基于角色控制访问。
- WebSocket：前端通过 STOMP 订阅 `/topic/connection`、`/topic/monitoring`、`/user/{userId}/queue/user-alerts` 等主题/队列。
- 告警确认与连接管理：发送 `/app/connect`、`/app/disconnect`、`/app/acknowledge-alert`。


