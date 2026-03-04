# Power Manage System (电力管理系统)

Power Manage 是一个集成了实时监控、异常检测和用电模式分析的综合电力管理系统。该系统利用先进的算法（如马氏距离、DTW、Z-Score）识别用电异常，并为不同类型的用户提供定制化的用电画像分析，旨在提高电力管理的效率和安全性。

## 1. 功能特性

*   **实时监控 (Real-time Monitoring)**
    *   基于 WebSocket 的实时电力数据推送。
    *   动态仪表盘展示当前功率、电压、电流等关键指标。
*   **异常检测 (Anomaly Detection)**
    *   **混合算法引擎**：结合马氏距离 (Mahalanobis Distance)、动态时间规整 (DTW) 和 Z-Score 算法。
    *   **多维度识别**：支持高负荷、低负荷、模式偏离等多种异常类型的自动判定。
    *   **参数可配置**：支持动态调整检测阈值和算法权重。
*   **用户画像分析 (User Pattern Analysis)**
    *   构建用户用电基准画像（Baseline Pattern）。
    *   分析用户用电习惯（商业型、住宅型、高耗能型）。
    *   可视化展示用户用电趋势对比。
*   **告警与报表 (Alerts & Reports)**
    *   多级告警管理（CRITICAL, HIGH, MEDIUM, LOW）。
    *   支持导出 Excel 和 PDF 格式的详细用电报表。
*   **系统管理**
    *   基于 RBAC 的用户权限管理。
    *   完善的操作日志审计功能。

## 2. 技术栈

### 后端 (Backend)
*   **核心框架**: Java 17, Spring Boot 2.7.15
*   **数据库**: MySQL 8.0.32, Spring Data JPA
*   **安全认证**: Spring Security, JWT
*   **实时通信**: WebSocket (STOMP)
*   **工具库**: Lombok, POI (Excel), iText (PDF), JSON

### 前端 (Frontend)
*   **核心框架**: Vue 3.5, TypeScript
*   **构建工具**: Vite 7.1
*   **UI 组件库**: Element Plus
*   **数据可视化**: ECharts 6.0
*   **网络请求**: Axios

## 3. 安装指南

### 环境要求
*   **Java**: JDK 17+
*   **Node.js**: v16+ (推荐 v18)
*   **Database**: MySQL 8.0+

### 安装步骤

1.  **克隆项目**
    ```bash
    git clone <repository-url>
    cd power_manage
    ```

2.  **数据库配置**
    *   创建数据库 `power_manage`。
    *   导入初始化脚本 `src/main/resources/init_database.sql`。
    *   修改配置文件 `src/main/resources/application.properties` 中的数据库连接信息：
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/power_manage?useSSL=false&...
        spring.datasource.username=root
        spring.datasource.password=your_password
        ```

3.  **后端构建**
    ```bash
    mvn clean install
    ```

4.  **前端安装**
    ```bash
    cd frontend
    npm install
    ```

## 4. 使用说明

### 启动系统

1.  **启动后端服务**
    *   运行主类 `com.powermanage.PowerManageApplication`。
    *   或者使用 Maven 命令：
        ```bash
        mvn spring-boot:run
        ```
    *   服务默认端口：`8081` (Context Path: `/power_manage`)

2.  **启动前端服务**
    ```bash
    cd frontend
    npm run dev
    ```
    *   访问地址：`http://localhost:5173` (具体端口请查看控制台输出)

### 默认账号
*   **管理员**: `admin` / `123456` (请参照 `init_database.sql`)

### 关键配置说明
在 `application.properties` 中可调整异常检测参数：
```properties
# 异常检测高阈值
anomaly.detection.high-threshold=10.0
# 模式偏离容忍度
anomaly.detection.pattern-deviation=0.3
```

## 5. 项目结构

```
power_manage/
├── frontend/                # Vue3 前端项目
│   ├── src/                 # 前端源码
│   ├── vite.config.ts       # Vite 配置
│   └── package.json         # 前端依赖配置
├── src/
│   ├── main/
│   │   ├── java/com/powermanage/
│   │   │   ├── config/      # 配置类 (Security, WebSocket, etc.)
│   │   │   ├── controller/  # REST 接口控制器
│   │   │   ├── entity/      # 数据库实体类
│   │   │   ├── repository/  # JPA 数据访问层
│   │   │   ├── service/     # 业务逻辑层
│   │   │   └── util/        # 工具类 (算法实现等)
│   │   └── resources/
│   │       ├── application.properties  # 全局配置文件
│   │       └── init_database.sql       # 数据库初始化脚本
├── pom.xml                  # Maven 依赖配置
└── README.md                # 项目说明文档
```

## 6. 开发指南

### 开发环境搭建
推荐使用 IntelliJ IDEA 或 VS Code。
*   **后端**: 安装 "Extension Pack for Java"。
*   **前端**: 安装 "Vue - Official" 插件。

### 测试方法
*   **单元测试**: 运行 `mvn test` 执行 JUnit 测试用例。
*   **接口测试**: 使用 Postman 或 Swagger (如果集成) 测试 `/power_manage/api/*` 接口。

### 贡献规范
1.  Fork 本仓库。
2.  新建特性分支 (`git checkout -b feature/AmazingFeature`)。
3.  提交更改 (`git commit -m 'Add some AmazingFeature'`)。
4.  推送到分支 (`git push origin feature/AmazingFeature`)。
5.  提交 Pull Request。

## 7. 许可证信息

本项目采用 [MIT License](LICENSE) 开源协议。
