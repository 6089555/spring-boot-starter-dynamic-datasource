package com.wjusite.boot.datasource;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName: DataSourceAspect
 * @Description: 定义数据源的AOP切面，该类控制了使用Master还是Slave。
 *               如果事务管理中配置了事务策略，则采用配置的事务策略中的标记了ReadOnly的方法是用Slave，其它使用Master。
 *               如果没有配置事务管理的策略，则采用方法名匹配的原则，以query、find、get开头方法用Slave，其它用Master。
 * @author Kola 6089555
 * @date 2017年4月26日 下午1:29:02
 *
 */
@EnableAspectJAutoProxy
@Aspect
@Order(-9999)
public class DataSourceAspect {
    
    private List<String> slaveMethodPattern = new CopyOnWriteArrayList<String>();
    
    private static final String[] defaultSlaveMethodStart = new String[] { "query", "find", "get", "select" };
    
    private String[] slaveMethodStart;
    
    /**
     * 读取事务管理中的策略
     * 
     * @param txAdvice
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void setTxAdvice(TransactionInterceptor txAdvice) throws Exception {
        if (txAdvice == null) {
            // 没有配置事务管理策略
            return;
        }
        // 从txAdvice获取到策略配置信息
        TransactionAttributeSource transactionAttributeSource = txAdvice.getTransactionAttributeSource();
        if (!(transactionAttributeSource instanceof NameMatchTransactionAttributeSource)) {
            return;
        }
        // 使用反射技术获取到NameMatchTransactionAttributeSource对象中的nameMap属性值
        NameMatchTransactionAttributeSource matchTransactionAttributeSource = (NameMatchTransactionAttributeSource) transactionAttributeSource;
        Field nameMapField = ReflectionUtils.findField(NameMatchTransactionAttributeSource.class, "nameMap");
        nameMapField.setAccessible(true); // 设置该字段可访问
        // 获取nameMap的值
        Map<String, TransactionAttribute> map = (Map<String, TransactionAttribute>) nameMapField.get(matchTransactionAttributeSource);
        
        // 遍历nameMap
        for (Map.Entry<String, TransactionAttribute> entry : map.entrySet()) {
            if (!entry.getValue().isReadOnly()) {// 判断之后定义了ReadOnly的策略才加入到slaveMethodPattern
                continue;
            }
            slaveMethodPattern.add(entry.getKey());
        }
    }
    
    /**
     * 在进入Service方法之前执行
     * 
     * @param point
     *            切面对象
     */
    @Before("dataSourcePointcut()")
    public void before(JoinPoint point) {
        // 获取到当前执行的方法名
        String methodName = point.getSignature().getName();
        boolean isSlave = false;
        if (slaveMethodPattern.isEmpty()) {
            // 当前Spring容器中没有配置事务策略，采用方法名匹配方式
            isSlave = isSlave(methodName);
        } else {
            // 使用策略规则匹配
            for (String mappedName : slaveMethodPattern) {
                if (isMatch(methodName, mappedName)) {
                    isSlave = true;
                    break;
                }
            }
        }
        if (isSlave) {
            // 标记为读库
            DynamicDataSourceHolder.markSlave();
        } else {
            // 标记为写库
            DynamicDataSourceHolder.markMaster();
        }
    }
    
    /**
     * 判断是否为读库
     * 
     * @param methodName
     * @return
     */
    private Boolean isSlave(String methodName) {
        // 方法名以query、find、get开头的方法名走从库
        return StringUtils.startsWithAny(methodName, getSlaveMethodStart());
    }
    
    /**
     * 通配符匹配
     */
    protected boolean isMatch(String methodName, String mappedName) {
        return PatternMatchUtils.simpleMatch(mappedName, methodName);
    }
    
    /**
     * 用户指定slave的方法名前缀
     * 
     * @param slaveMethodStart
     */
    public void setSlaveMethodStart(String[] slaveMethodStart) {
        this.slaveMethodStart = slaveMethodStart;
    }
    
    public String[] getSlaveMethodStart() {
        if (this.slaveMethodStart == null) {
            // 没有指定，使用默认
            return defaultSlaveMethodStart;
        }
        return slaveMethodStart;
    }
    
    @Pointcut("(execution(* *..*.service..*.*(..)) || execution(* *..*.services..*.*(..))) && !@within(com.wjusite.boot.datasource.annotations.NoDynamicDataSource)")
    private void dataSourcePointcut() {
    }
}
