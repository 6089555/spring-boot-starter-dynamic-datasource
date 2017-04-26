# spring-boot-starter-dynamic-datasource

## spring-boot-starter-dynamic-datasource是什么
spring-boot-starter-dynamic-datasource基于springboot将动态数据源和读写分离进行集成,目前已经可以零xml实现读写分离

## 包含哪些功能

### 声明式事务管理
> 目前已自动配置了save*,update*,delete*,del*,placed*,select*,query*,find*,get* 这些事务策略

### 读写分离
> 可以支持一主多从配置,通过spring aop 拦截service层方法识别出其事务策略并切换为对应数据源

## 配置示例
```
druid:
    dataSource:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class: com.mysql.jdbc.Driver
        url: jdbc:mysql://localhost:3306/test
        username: root
        password: root
        initial-size: 1
        min-idle: 1
        max-active: 100
        test-on-borrow: true
        log-abandoned: true
        max-wait: 60000
        time-between-eviction-runs-millis: 60000
        min-evictable-idle-time-millis: 300000
        validation-query: SELECT 'x'
        test-While-Idle: true
        test-on-return: false
        pool-prepared-statements: false
        max-pool-prepared-statement-per-connection-size: 20
        filters: wall,mergeStat
        connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000;config.decrypt=false
    slaveSource:
        node: slave1,slave2
        slave1:
            type: com.alibaba.druid.pool.DruidDataSource
            driver-class: com.mysql.jdbc.Driver
            url: jdbc:mysql://localhost:3306/test1
            username: root
            password: root
            initial-size: 1
            min-idle: 1
            max-active: 100
            test-on-borrow: true
            log-abandoned: true
            max-wait: 60000
            time-between-eviction-runs-millis: 60000
            min-evictable-idle-time-millis: 300000
            validation-query: SELECT 'x'
            test-While-Idle: true
            test-on-return: false
            pool-prepared-statements: false
            max-pool-prepared-statement-per-connection-size: 20
            filters: wall,mergeStat
            connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000;config.decrypt=false
        slave2:
            type: com.alibaba.druid.pool.DruidDataSource
            driver-class: com.mysql.jdbc.Driver
            url: jdbc:mysql://localhost:3306/test2
            username: root
            password: root
            initial-size: 1
            min-idle: 1
            max-active: 100
            test-on-borrow: true
            log-abandoned: true
            max-wait: 60000
            time-between-eviction-runs-millis: 60000
            min-evictable-idle-time-millis: 300000
            validation-query: SELECT 'x'
            test-While-Idle: true
            test-on-return: false
            pool-prepared-statements: false
            max-pool-prepared-statement-per-connection-size: 20
            filters: wall,mergeStat
            connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000;config.decrypt=false

```
查看[Druid配置](https://github.com/alibaba/druid/wiki/DruidDataSource配置属性列表)