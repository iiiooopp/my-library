>#### 原文地址：https://www.cnblogs.com/fengzheng/p/11242128.html
配置文件是我们再熟悉不过的了，尤其是 Spring Boot 项目，除了引入相应的 maven 包之外，剩下的工作就是完善配置文件了，例如 mysql、redis 、security 相关的配置。除了项目运行的基础配置之外，还有一些配置是与我们业务有关系的，比如说七牛存储、短信相关、邮件相关，或者一些业务上的开关。

对于一些简单的项目来说，我们一般都是直接把相关配置放在单独的配置文件中，以 properties 或者 yml 的格式出现，更省事儿的方式是直接放到 application.properties 或 application.yml 中。但是这样的方式有个明显的问题，那就是，当修改了配置之后，必须重启服务，否则配置无法生效。

目前有一些用的比较多的开源的配置中心，比如携程的 Apollo、蚂蚁金服的 disconf 等，对比 Spring Cloud Config，这些配置中心功能更加强大。有兴趣的可以拿来试一试。

接下来，我们开始在 Spring Boot 项目中集成 Spring Cloud Config，并以 github 作为配置存储。除了 git 外，还可以用数据库、svn、本地文件等作为存储。主要从以下三块来说一下 Config 的使用。

+ 1.基础版的配置中心（不集成 Eureka）;
+ 2.结合 Eureka 版的配置中心;
+ 3.实现配置的自动刷新;

>### **实现最简单的配置中心**

最简单的配置中心，就是启动一个服务作为服务方，之后各个需要获取配置的服务作为客户端来这个服务方获取配置。

先在 github 中建立配置文件

我创建的仓库地址为：配置中心仓库

目录结构如下：
![image](https://img2018.cnblogs.com/blog/273364/201907/273364-20190725084302799-353615966.png)

配置文件的内容大致如下，用于区分，略有不同。

```yml
data:
  env: config-eureka-dev
  user:
    username: eureka-client-user
    password: 1291029102
```

注意文件的名称不是乱起的，例如上面的 config-single-client-dev.yml 和 config-single-client-prod.yml 这两个是同一个项目的不同版本，项目名称为 config-single-client， 一个对应开发版，一个对应正式版。config-eureka-client-dev.yml 和 config-eureka-client-prod.yml 则是另外一个项目的，项目的名称就是 config-eureka-client 。

>### **创建配置中心服务端**

1、新建 Spring Boot 项目，引入 config-server 和 starter-web
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- spring cloud config 服务端包 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```
2、配置 config 相关的配置项

bootstrap.yml 文件
```yml
spring:
  application:
    name: config-single-server  # 应用名称
  cloud:
     config:
        server:
          git:
            uri: https://github.com/huzhicheng/config-only-a-demo #配置文件所在仓库
            username: github 登录账号
            password: github 登录密码
            default-label: master #配置文件分支
            search-paths: config  #配置文件所在根目录
```
application.yml
```yml
server:
  port: 3301
```
3、在 Application 启动类上增加相关注解 `@EnableConfigServer`
```java
@SpringBootApplication
@EnableConfigServer
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
启动服务，接下来测试一下。

Spring Cloud Config 有它的一套访问规则，我们通过这套规则在浏览器上直接访问就可以。
```
/{application}/{profile}[/{label}]
/{application}-{profile}.yml
/{label}/{application}-{profile}.yml
/{application}-{profile}.properties
/{label}/{application}-{profile}.properties
```
{application} 就是应用名称，对应到配置文件上来，就是配置文件的名称部分，例如我上面创建的配置文件。

{profile} 就是配置文件的版本，我们的项目有开发版本、测试环境版本、生产环境版本，对应到配置文件上来就是以 application-{profile}.yml 加以区分，例如application-dev.yml、application-sit.yml、application-prod.yml。

{label} 表示 git 分支，默认是 master 分支，如果项目是以分支做区分也是可以的，那就可以通过不同的 label 来控制访问不同的配置文件了。

上面的 5 条规则中，我们只看前三条，因为我这里的配置文件都是 yml 格式的。根据这三条规则，我们可以通过以下地址查看配置文件内容:

http://localhost:3301/config-single-client/dev/master

http://localhost:3301/config-single-client/prod

http://localhost:3301/config-single-client-dev.yml

http://localhost:3301/config-single-client-prod.yml

http://localhost:3301/master/config-single-client-prod.yml

通过访问以上地址，如果可以正常返回数据，则说明配置中心服务端一切正常。

>### **创建配置中心客户端，使用配置**

配置中心服务端好了，配置数据准备好了，接下来，就要在我们的项目中使用它了。

1、引用相关的 maven 包。
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- spring cloud config 客户端包 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
2、初始化配置文件

bootstrap.yml
```yml
spring:
  profiles:
    active: dev

---
spring:
  profiles: prod
  application:
    name: config-single-client
  cloud:
     config:
       uri: http://localhost:3301
       label: master
       profile: prod


---
spring:
  profiles: dev
  application:
    name: config-single-client
  cloud:
     config:
       uri: http://localhost:3301
       label: master
       profile: dev
```
配置了两个版本的配置，并通过 spring.profiles.active 设置当前使用的版本，例如本例中使用的 dev 版本。

application.yml
```yml
server:
  port: 3302
management:
  endpoint:
    shutdown:
      enabled: false
  endpoints:
    web:
      exposure:
        include: "*"

data:
  env: localhost-dev-edit
  user:
    username: username-dev
    password: password-dev
```
其中 management 是关于 actuator 相关的，接下来自动刷新配置的时候需要使用。

data 部分是当无法读取配置中心的配置时，使用此配置，以免项目无法启动。

3、要读取配置中心的内容，需要增加相关的配置类，Spring Cloud Config 读取配置中心内容的方式和读取本地配置文件中的配置是一模一样的。可以通过 @Value 或 @ConfigurationProperties 来获取。
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
4、要读取配置中心的内容，需要增加相关的配置类，Spring Cloud Config 读取配置中心内容的方式和读取本地配置文件中的配置是一模一样的。可以通过 @Value 或 @ConfigurationProperties 来获取。

~~使用 `@Value` 的方式（不推荐，无法自动更新）：~~
```java
@Data
@Component
public class GitConfig {

    @Value("${data.env}")
    private String env;

    @Value("${data.user.username}")
    private String username;

    @Value("${data.user.password}")
    private String password;

}
```
使用 `@ConfigurationProperties` 的方式：
```java
@Component
@Data
@ConfigurationProperties(prefix = "data")
public class GitAutoRefreshConfig {

    public static class UserInfo {
        private String username;

        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }

    private String env;

    private UserInfo user;
}
```
4、增加一个 RESTController 来测试使用配置
```java
@RestController
public class GitController {

    @Autowired
    private GitConfig gitConfig;

    @Autowired
    private GitAutoRefreshConfig gitAutoRefreshConfig;

    @GetMapping(value = "show")
    public Object show(){
        return gitConfig;
    }

    @GetMapping(value = "autoShow")
    public Object autoShow(){
        return gitAutoRefreshConfig;
    }
}
```
5、项目启动类
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
启动项目，访问 RESTful 接口

http://localhost:3302/show，结果如下：
```
{
  "env": "localhost-dev-edit",
  "username": "username-dev",
  "password": "password-dev"
}
```
http://localhost:3302/autoShow，结果如下：
```
{
  "env": "localhost-dev-edit",
  "username": "username-dev",
  "password": "password-dev"
}
```
>### **实现自动刷新**

Spring Cloud Config 在项目启动时加载配置内容这一机制，导致了它存在一个缺陷，修改配置文件内容后，不会自动刷新。例如我们上面的项目，当服务已经启动的时候，去修改 github 上的配置文件内容，这时候，再次刷新页面，对不起，还是旧的配置内容，新内容不会主动刷新过来。
但是，总不能每次修改了配置后重启服务吧。如果是那样的话，还是不要用它了为好，直接用本地配置文件岂不是更快。

它提供了一个刷新机制，但是需要我们主动触发。那就是 @RefreshScope 注解并结合 actuator ，注意要引入 spring-boot-starter-actuator 包。

1、在 config client 端配置中增加 actuator 配置，上面大家可能就注意到了。
```yml
management:
  endpoint:
    shutdown:
      enabled: false
  endpoints:
    web:
      exposure:
        include: "*"
```
其实这里主要用到的是 refresh 这个接口

2、在需要读取配置的类上增加 @RefreshScope 注解，我们是 controller 中使用配置，所以加在 controller 中。
```java
@RestController
@RefreshScope
public class GitController {

    @Autowired
    private GitConfig gitConfig;

    @Autowired
    private GitAutoRefreshConfig gitAutoRefreshConfig;

    @GetMapping(value = "show")
    public Object show(){
        return gitConfig;
    }

    @GetMapping(value = "autoShow")
    public Object autoShow(){
        return gitAutoRefreshConfig;
    }
}
```
注意，以上都是在 client 端做的修改。

之后，重启 client 端，重启后，我们修改 github 上的配置文件内容，并提交更改，再次刷新页面，没有反应。没有问题。

接下来，我们发送 POST 请求到 http://localhost:3302/actuator/refresh 这个接口，用 postman 之类的工具即可，此接口就是用来触发加载新配置的，返回内容如下:
```
[
    "config.client.version",
    "data.env"
]
```
之后，再次访问 RESTful 接口，http://localhost:3302/autoShow 这个接口获取的数据已经是最新的了，说明 refresh 机制起作用了。

而 http://localhost:3302/show 获取的还是旧数据，这与 @Value 注解的实现有关，所以，我们在项目中就不要使用这种方式加载配置了。

>### **在 github 中配置 Webhook**

这就结束了吗，并没有，总不能每次改了配置后，就用 postman 访问一下 refresh 接口吧，还是不够方便呀。 github 提供了一种 webhook 的方式，当有代码变更的时候，会调用我们设置的地址，来实现我们想达到的目的。

1、进入 github 仓库配置页面，选择 Webhooks ，并点击 add webhook；

![image](https://img2018.cnblogs.com/blog/273364/201907/273364-20190725090559970-931500557.png)

2、之后填上回调的地址，也就是上面提到的 actuator/refresh 这个地址，但是必须保证这个地址是可以被 github 访问到的。如果是内网就没办法了。这也仅仅是个演示，一般公司内的项目都会有自己的代码管理工具，例如自建的 gitlab，gitlab 也有 webhook 的功能，这样就可以调用到内网的地址了。

![image](https://img2018.cnblogs.com/blog/273364/201907/273364-20190725090614229-390401707.png)


