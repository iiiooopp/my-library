# Nacos 模块学习

>参考文档：https://nacos.io/zh-cn/docs/what-is-nacos.html
>
>参考文档：https://blog.csdn.net/qq_33619378/category_9291906.html

>### 1.安装Nacos

+ 版本选择

    您可以在Nacos的release notes及博客中找到每个版本支持的功能的介绍，当前推荐的稳定版本为1.3.1。

+ 预备环境准备

    Nacos 依赖 Java 环境来运行。如果您是从代码开始构建并运行Nacos，还需要为此配置 Maven环境，请确保是在以下版本环境中安装使用:

    64 bit OS，支持 Linux/Unix/Mac/Windows，推荐选用 Linux/Unix/Mac。
    64 bit JDK 1.8+；下载 & 配置。
    Maven 3.2.x+；下载 & 配置。

+ 下载源码或者安装包

    你可以通过源码和发行包两种方式来获取 Nacos。
    
    + 从 **`Github`** 上下载源码方式
    
        ```jvm
        git clone https://github.com/alibaba/nacos.git
        cd nacos/
        mvn -Prelease-nacos -Dmaven.test.skip=true clean install -U  
        ls -al distribution/target/
        
        // change the $version to your actual path
        cd distribution/target/nacos-server-$version/nacos/bin
        ```
    
    + 下载编译后压缩包方式
  
        您可以从 [最新稳定版本](https://github.com/alibaba/nacos/releases) 下载 **`nacos-server-$version.zip`** 包。
        
+ 启动服务器
    
    + Linux/Unix/Mac

        启动命令(standalone代表着单机模式运行，非集群模式):
        
        ```jvm
        cd nacos/bin
        sh startup.sh -m standalone
        ```

        如果您使用的是ubuntu系统，或者运行脚本报错提示[[符号找不到，可尝试如下运行：
        
        ```jvm
        cd nacos/bin
        bash startup.sh -m standalone
        ```

    + Windows

        启动命令：

        `cmd startup.cmd`

        或者双击`startup.cmd`运行文件。
        
+ 服务注册&发现和配置管理

    + 服务注册

        `curl -X POST 'http://127.0.0.1:8848/nacos/v1/ns/instance?serviceName=nacos.naming.serviceName&ip=20.18.7.10&port=8080'`

    + 服务发现

        `curl -X GET 'http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=nacos.naming.serviceName'`

    + 发布配置

        `curl -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=nacos.cfg.dataId&group=test&content=HelloWorld"`

    + 获取配置

        `curl -X GET "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=nacos.cfg.dataId&group=test"`

+ 关闭服务器

    + Linux/Unix/Mac

        `sh shutdown.sh`

    + Windows

        `cmd shutdown.cmd`

        或者双击shutdown.cmd运行文件。
        
+ 持久化

    + 安装数据库

        目前Nacos仅支持Mysql数据库，且版本要求：5.6.5+
        
    + 初始化数据库
    
        Nacos的数据库脚本文件在我们下载Nacos-server时的压缩包中就有
    
        进入nacos\conf目录，初始化文件：`nacos-mysql.sql`
    
        此处我创建一个名为 mynacos 的数据库，然后执行初始化脚本，成功后会生成 12 张表
        
        ![image](image/1603697515(1).png)
        
    + 修改配置文件
    
        这里是需要修改Nacos-server的配置文件
    
        Nacos-server其实就是一个Java工程或者说是一个Springboot项目，他的配置文件在nacos\conf目录下，名为 `application.properties`，在文件底部添加数据源配置：
        
        ```properties
        #*************** Config Module Related Configurations ***************#
        ### If use MySQL as datasource:
        spring.datasource.platform=mysql
        
        ### Count of DB:
        db.num=1
        
        ### Connect URL of DB:
        db.url.0=jdbc:mysql://127.0.0.1:3306/mynacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
        db.user=root
        db.password=123456
        ```
      
    + 重启Nacos
        
>### 2.SpringCloud整合Nacos(注册中心以及配置中心),Feign(远程调用),Hystrix(熔断器)

![image](https://cdn.nlark.com/lark/0/2018/png/15914/1542119181336-b6dc0fc1-ed46-43a7-9e5f-68c9ca344d60.png)

首先启动NacosServer，windows下双击启动`startup.cmd`文件。

启动成功后，此时Nacos控制台就可以访问了，浏览器访问：http://127.0.0.1:8848/nacos/index.html ，默认的账号密码为nacos/nacos，控制台页面如下：

![image](image/1603423914(1).png)

+ #### 创建服务提供者

    + ProviderController.java

        ```java
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.cloud.context.config.annotation.RefreshScope;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;
        
        @RestController
        @RefreshScope // 实现配置中心配置自动更新
        @RequestMapping("/config")
        public class ProviderController {
        
            @Value("${value}") // 获取配置文件信息
            private String value;
        
            @GetMapping("/getValue")
            public String getValue(){
                String s = "获取Value值：" + value;
                System.out.println(s);
                return s;
            }
        }
        ```

    + bootstrap.yml

        ```yml
        spring:
          application:
            name: quickstart-server #服务名称
          cloud:
            nacos:
              discovery:
                server-addr: 127.0.0.1:8848 #注册中心地址
              config:
                server-addr: 127.0.0.1:8848 #配置中心地址
                file-extension: yml #配置文件类型
        #        namespace: cbc398b4-0266-4501-b23f-77332ced56b4 #指定该命名空间下的配置
        #        group: group_dev #指定该组下的配置
        ```

        >说明：

        `spring.application.name` 是构成 **Nacos** 配置管理 `dataId` 字段的一部分。
        
        在 **Nacos Spring Cloud** 中，`dataId` 的完整格式如下：
        
        `${prefix}-${spring.profiles.active}.${file-extension}`
        
        `prefix` 默认为 `spring.application.name` 的值，也可以通过配置项 `spring.cloud.nacos.config.prefix` 来配置。
        
        `spring.profiles.active` 即为当前环境对应的 `profile`，详情可以参考 **Spring Boot** 文档。 注意：当 `spring.profiles.active` 为空时，对应的连接符 - 也将不存在，`dataId` 的拼接格式变成 `${prefix}.${file-extension}`
        
        `file-exetension` 为配置内容的数据格式，可以通过配置项 `spring.cloud.nacos.config.file-extension` 来配置。目前只支持 `properties` 和 `yaml` 类型。

    + application.yml

        ```yml
        server:
            port: 12000
        ```

    + NacosServerApplication.java

        ```java
        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
        
        @SpringBootApplication
        public class NacosServerApplication {
        
            public static void main(String[] args) {
                SpringApplication.run(NacosServerApplication.class, args);
            }
        
        }
        ```

    + pom.xml

        ```xml
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.2.10.RELEASE</version>
                <relativePath/> <!-- lookup parent from repository -->
            </parent>
            <groupId>com.example</groupId>
            <artifactId>nacos-server</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <name>nacos-server</name>
            <description>Demo project for Spring Boot</description>
        
            <properties>
                <java.version>11</java.version>
                <spring-cloud-alibaba.version>2.2.1.RELEASE</spring-cloud-alibaba.version>
                <spring-cloud.version>Hoxton.SR8</spring-cloud.version>
            </properties>
        
            <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                </dependency>
                <dependency>
                    <groupId>com.alibaba.cloud</groupId>
                    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
                </dependency>
                <dependency>
                    <groupId>com.alibaba.cloud</groupId>
                    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
                </dependency>
        
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                    <exclusions>
                        <exclusion>
                            <groupId>org.junit.vintage</groupId>
                            <artifactId>junit-vintage-engine</artifactId>
                        </exclusion>
                    </exclusions>
                </dependency>
            </dependencies>
        
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-dependencies</artifactId>
                        <version>${spring-cloud.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.alibaba.cloud</groupId>
                        <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                        <version>${spring-cloud-alibaba.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                    </dependency>
                </dependencies>
            </dependencyManagement>
        
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        
        </project>
        ```

+ #### 创建服务消费者

    + ConsumerController.java
    
        ```java
        import com.example.nacosconsumer.server.ProviderServer;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;
        
        @RestController
        @RequestMapping("/config")
        public class ConsumerController {
        
            /**
             * 动态代理对象，内部远程调用服务生产者
             */
            @Autowired
            private ProviderServer providerServer;
        
        
            @GetMapping("/getValue")
            public String getValue() {
                System.out.println("远程调用getValue");
                //远程调用
                String value = providerServer.getValue();
                return value;
            }
        
        }
        ```

    + ProviderServer.java
    
        ```java
        import com.example.nacosconsumer.error.FeignError;
        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.context.annotation.Primary;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        
        @FeignClient(value = "nacos-server", fallback = FeignError.class)
        @Primary
        public interface ProviderServer {
        
            @GetMapping("/config/getValue")
            public String getValue();
        
        }
        ```

    + FeignError.java
    
        ```java
        import com.example.nacosconsumer.server.ProviderServer;
        import org.springframework.stereotype.Component;
        
        @Component
        public class FeignError implements ProviderServer {
        
            @Override
            public String getValue() {
                return "服务器维护中....";
            }
        }
        ```

    + bootstrap.yml
    
        ```yml
        spring:
          application:
            name: nacos-consumer #服务名称
          cloud:
            nacos:
              discovery:
                server-addr: 127.0.0.1:8848 #注册中心地址
              config:
                server-addr: 127.0.0.1:8848 #配置中心地址
                file-extension: yml #配置文件类型
        #        namespace: cbc398b4-0266-4501-b23f-77332ced56b4 #指定该命名空间下的配置
        #        group: group_dev #指定该组下的配置
        
        feign:
          hystrix:
            enabled: true #在feign中开启 Hystrix(熔断器)
        
        management:
          endpoints:
            web:
              exposure:
                include: '*' #公开端点的ID。 默认： health, info
              base-path: / #对于网络端点的基本路径。 默认： /actuator
        
        hystrix:
          dashboard:
            proxy-stream-allow-list: '*' #开启Hystrix可视化监控需要添加此项
        ```

    + application.yml
    
        ```yml
        server:
            port: 12001
        ```

    + NacosConsumerApplication.java
    
        ```java
        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
        import org.springframework.cloud.netflix.hystrix.EnableHystrix;
        import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
        import org.springframework.cloud.openfeign.EnableFeignClients;
        
        @SpringBootApplication
        @EnableFeignClients     // 启用feign客服端
        @EnableHystrix          // 开启熔断器
        @EnableHystrixDashboard // 开启Hystrix可视化界面
        public class NacosConsumerApplication {
        
            public static void main(String[] args) {
                SpringApplication.run(NacosConsumerApplication.class, args);
            }
        
        }
        ```

    + pom.xml
    
        ```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <!-- 监控 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        ```

+ #### 测试

    + 开启Nacos-Server，然后启动项目服务提供端与服务消费端
    
        ![image](image/1603762650(1).png)
        
    + 新增nacos-server配置文件
    
        ![image](image/1603766767(1).png)
        
        ![image](image/1603766890(1).png)
        
    + 访问服务提供端 http://localhost:12000/config/getValue ,获得如下信息:
        
        ![image](image/1603767153(1).png)
        
        通过getValue方法成功获取远程配置参数
    
    + 访问服务消费端 http://localhost:12001/config/getValue ,获得如下信息:
        
        ![image](image/1603767346(1).png)
    
        通过调用服务端的getValue方法成功获取远程配置参数

    + 开启服务消费端Hystrix可视化监控 http://localhost:12001/hystrix 并输入监控数据地址 http://localhost:12001/hystrix.stream ，然后点击 `Monitor Stream` :
        
        ![image](image/1603769898(1).png)
        
        ![image](image/1603771091(1).png)
        
        成功！
    