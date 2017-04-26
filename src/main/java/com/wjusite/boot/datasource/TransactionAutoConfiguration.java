package com.wjusite.boot.datasource;

import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Kola 6089555
 * @ClassName: TransactionAutoConfiguration
 * @Description: 事务配置类 添加 @Import(DynamicDataSourceRegister.class)
 * @date 2017年4月24日 下午2:31:51
 */
@Configuration
@EnableTransactionManagement
@Import(DataSourceAspect.class)
public class TransactionAutoConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionAutoConfiguration.class);
    
    @Autowired
    private DataSourceAspect dataSourceAspect;
    
    @Resource(name = "dataSource")
    private DataSource dataSource;
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
    
    @Bean(name = "transactionInterceptor")
    public TransactionInterceptor transactionInterceptor(PlatformTransactionManager transactionManager) {
        Properties transactionAttributes = new Properties();
        transactionAttributes.setProperty("insert*", "PROPAGATION_REQUIRED,-Throwable");
        transactionAttributes.setProperty("save*", "PROPAGATION_REQUIRED,-Throwable");
        transactionAttributes.setProperty("update*", "PROPAGATION_REQUIRED,-Throwable");
        transactionAttributes.setProperty("delete*", "PROPAGATION_REQUIRED,-Throwable");
        transactionAttributes.setProperty("del*", "PROPAGATION_REQUIRED,-Throwable");
        transactionAttributes.setProperty("placed*", "PROPAGATION_REQUIRED,-Throwable");
        transactionAttributes.setProperty("select*", "readOnly");
        transactionAttributes.setProperty("query*", "readOnly");
        transactionAttributes.setProperty("find*", "readOnly");
        transactionAttributes.setProperty("get*", "readOnly");
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(transactionManager, transactionAttributes);
        try {
            dataSourceAspect.setTxAdvice(transactionInterceptor);
        } catch (Exception e) {
            LOGGER.error("dataSourceAspect fill transactionInterceptor failure.because {}", e.getMessage());
        }
        return transactionInterceptor;
    }
    
    @Bean
    public DefaultPointcutAdvisor defaultPointcutAdvisor(TransactionInterceptor transactionInterceptor, AspectJExpressionPointcut aspectJExpressionPointcut) {
        return new DefaultPointcutAdvisor(aspectJExpressionPointcut, transactionInterceptor);
    }
    
    @Bean
    public AspectJExpressionPointcut aspectJExpressionPointcut() {
        AspectJExpressionPointcut aspectJExpressionPointcut = new AspectJExpressionPointcut();
        aspectJExpressionPointcut.setExpression("(execution(* *..*.service..*.*(..)) || execution(* *..*.services..*.*(..))) && !@within(com.wjusite.boot.datasource.annotations.NoDynamicDataSource)");
        return aspectJExpressionPointcut;
    }
}
