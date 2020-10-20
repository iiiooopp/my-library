# **Flyway学习**
##### **原文链接：https://www.jianshu.com/p/567a8a161641**

>### 一、Flyway是什么

官网解释地非常全面，可先大致阅读一下。

简单地说，flyway是一个能对数据库变更做版本控制的工具。

>### 二、为什么要用Flyway

在多人开发的项目中，我们都习惯了使用SVN或者Git来对代码做版本控制，主要的目的就是为了解决多人开发代码冲突和版本回退的问题。

其实，数据库的变更也需要版本控制，在日常开发中，我们经常会遇到下面的问题：

    自己写的SQL忘了在所有环境执行；
    别人写的SQL我们不能确定是否都在所有环境执行过了；
    有人修改了已经执行过的SQL，期望再次执行；
    需要新增环境做数据迁移；
    每次发版需要手动控制先发DB版本，再发布应用版本；
    其它场景...

有了flyway，这些问题都能得到很好的解决。

>### 三、如何使用Flyway

#### 3.1 准备数据库

首先，我们需要准备好一个空的数据库。（数据库的安装和账密配置此处忽略）

此处以mysql为例，在本地电脑上新建一个空的数据库，名称叫做test，我们通过dbeaver看到的样子如下：

![image](image/1603096004(1).png)

#### 3.2 准备SpringBoot工程

新建一个SpringBoot工程，要求能连上自己本地新建的mysql数据库test，这个步骤也比较简单，就不再细讲。

但要注意的是，application.properties中数据库的配置务必配置正确，下述步骤中系统启动时，flyway需要凭借这些配置连接到数据库。这里贴一份：

```yml
Spring:
  # 特别说明：
  # 如果非空数据库迁移，在目标数据库中手动建flyway_schema_history表并手动写入初始化的脚本记录,
  # 使flyway跳过最初的校验即可，后续可以保证版本的统一;
  flyway:
    # 是否启用flyway
    enabled: true
    # 到新的环境中数据库中有数据，且没有flyway_schema_history表时，是否执行迁移操作。
    # 如果设置为false，在启动时会报错，并停止迁移;
    # 如果设置为true,则生成history表并完成所有的迁移，要根据实际情况设置;
    baseline-on-migrate: false
    # 执行时标记的tag 默认为<<Flyway Baseline>>
    baseline-description: <<Flyway Baseline>>
    # 检测迁移脚本的路径是否存在，如不存在，则抛出异常
    check-location: true
    # 脚本位置
    locations: classpath:db/migration
    # 在迁移时，是否校验脚本，假设V1.0__初始.sql已经迁移过了，在下次启动时会校验该脚本是否有变更过，则抛出异常
    validate-on-migrate: true
    # 禁止清理数据库表
    clean-disabled: true
    # 验证错误时，是否清除数据库表（高危操作）
    clean-on-validation-error: false
  datasource:
    url: jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: 123456
    driver-class-name: com.mysql.jdbc.Driver
```

#### 3.3flyway的引入与尝试

首先，在pom文件中引入flyway的核心依赖包：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

其次，在src/main/resources目录下面新建db.migration文件夹，默认情况下，该目录下的.sql文件就算是需要被flyway做版本控制的数据库SQL语句。

但是此处的SQL语句命名需要遵从一定的规范，否则运行的时候flyway会报错。命名规则主要有两种：

+ 仅需要被执行一次的SQL命名以大写的"V"开头，后面跟上"0~9"数字的组合,数字之间可以用“.”或者下划线"_"分割开，然后再以两个下划线分割，其后跟文件名称，最后以.sql结尾。
比如，`V2.1.5__create_user_ddl.sql`、`V4.1_2__add_user_dml.sql`。

+ 可重复运行的SQL，则以大写的“R”开头，每次启动该文件倘若有变化，则执行一次，后面再以两个下划线分割，其后跟文件名称，最后以.sql结尾。
比如，`R__truncate_user_dml.sql`。

其中，V开头的SQL执行优先级要比R开头的SQL优先级高。

如下，我们准备了一个脚本：

```sql
CREATE DATABASE `test`;

USE `test`;

DROP TABLE IF EXISTS `student`;

CREATE TABLE `student` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(11) DEFAULT NULL,
  `score` double DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

insert  into `student`(`id`,`name`,`score`,`birthday`) values (1,'张三',99,'2020-08-18'),(2,'李四',88,'2020-08-26');
```

到这一步，flyway的默认配置已经足够我们开始运行了。我们启动SpringBoot的主程序，此时，我们刷新数据库，可以看到test的历史记录表已经生成并插入了记录：

![image](image/1603097486(1).png)

#### 3.4 maven插件的使用

以上步骤中，每次想要migration都需要运行整个springboot项目，并且只能执行migrate一种命令，其实flyway还是有很多其它命令的。maven插件给了我们不需要启动项目就能执行flyway各种命令的机会。

在pom中引入flyway的插件，同时配置好对应的数据库连接。

```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <version>5.2.4</version>
    <configuration>
        <url>jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai&amp;characterEncoding=utf8</url>
        <user>root</user>
        <password>123456</password>
        <driver>com.mysql.jdbc.Driver</driver>
    </configuration>
</plugin>
```

然后更新maven插件列表，就可以看到flyway的全部命令了。

![image](image/1603097902(1).png)

此时，我们双击执行上图中的flyway:migrate的效果和启动整个工程执行migrate的效果是一样的。

其它命令的作用如下列出，各位可自行实验体会：

    1.baseline
    对已经存在数据库Schema结构的数据库一种解决方案。实现在非空数据库新建MetaData表，并把Migrations应用到该数据库；也可以在已有表结构的数据库中实现添加Metadata表。

    2.clean
    清除掉对应数据库Schema中所有的对象，包括表结构，视图，存储过程等，clean操作在dev 和 test阶段很好用，但在生产环境务必禁用。

    3.info
    用于打印所有的Migrations的详细和状态信息，也是通过MetaData和Migrations完成的，可以快速定位当前的数据库版本。

    4.repair
    repair操作能够修复metaData表，该操作在metadata出现错误时很有用。

    5.undo
    撤销操作，社区版不支持。

    6.validate
    验证已经apply的Migrations是否有变更，默认开启的，原理是对比MetaData表与本地Migrations的checkNum值，如果值相同则验证通过，否则失败。

#### 3.5 flyway补充知识

+ flyway执行migrate必须在空白的数据库上进行，否则报错；

+ 对于已经有数据的数据库，必须先baseline，然后才能migrate；

+ clean操作是删除数据库的所有内容，包括baseline之前的内容；

+ 尽量不要修改已经执行过的SQL，即便是R开头的可反复执行的SQL，它们会不利于数据迁移；

>### **四、总结**

在进行了如上的实验后，相信我们都已经掌握了flyway的初步使用，当需要做数据迁移的时候，更换一个新的空白数据库，执行下migrate命令，所有的数据库更改都可以一步到位地迁移过去，真的是太方便了。
附录

flyway的配置清单：

```
flyway.baseline-description对执行迁移时基准版本的描述.
flyway.baseline-on-migrate当迁移时发现目标schema非空，而且带有没有元数据的表时，是否自动执行基准迁移，默认false.
flyway.baseline-version开始执行基准迁移时对现有的schema的版本打标签，默认值为1.
flyway.check-location检查迁移脚本的位置是否存在，默认false.
flyway.clean-on-validation-error当发现校验错误时是否自动调用clean，默认false.
flyway.enabled是否开启flywary，默认true.
flyway.encoding设置迁移时的编码，默认UTF-8.
flyway.ignore-failed-future-migration当读取元数据表时是否忽略错误的迁移，默认false.
flyway.init-sqls当初始化好连接时要执行的SQL.
flyway.locations迁移脚本的位置，默认db/migration.
flyway.out-of-order是否允许无序的迁移，默认false.
flyway.password目标数据库的密码.
flyway.placeholder-prefix设置每个placeholder的前缀，默认${.
flyway.placeholder-replacementplaceholders是否要被替换，默认true.
flyway.placeholder-suffix设置每个placeholder的后缀，默认}.
flyway.placeholders.[placeholder name]设置placeholder的value
flyway.schemas设定需要flywary迁移的schema，大小写敏感，默认为连接默认的schema.
flyway.sql-migration-prefix迁移文件的前缀，默认为V.
flyway.sql-migration-separator迁移脚本的文件名分隔符，默认__
flyway.sql-migration-suffix迁移脚本的后缀，默认为.sql
flyway.tableflyway使用的元数据表名，默认为schema_version
flyway.target迁移时使用的目标版本，默认为latest version
flyway.url迁移时使用的JDBC URL，如果没有指定的话，将使用配置的主数据源
flyway.user迁移数据库的用户名
flyway.validate-on-migrate迁移时是否校验，默认为true
```