package com.wjusite.boot.datasource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Kola 6089555
 * @ClassName: DynamicDataSourceRegister
 * @Description: 动态数据源注册 启动动态数据源请在启动类中（如SpringBootSampleApplication）
 *               添加 @Import(DynamicDataSourceRegister.class)
 * @date 2017年4月24日 下午2:31:51
 */
public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDataSourceRegister.class);
    
    private static final String DATASOURCE_TYPE_DEFAULT = "com.alibaba.druid.pool.DruidDataSource";
    
    private ConversionService conversionService = new DefaultConversionService();
    
    private DataSource defaultDataSource = null;
    
    private Map<String, DataSource> targetDataSources = new HashMap<>();
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerDynamicDataSourceBeanDefinition(registry);
    }
    
    /**
     * @Title: registerDynamicDataSourceBeanDefinition
     * @Description: 注册动态数据源
     * @author Kola 6089555
     * @date 2017年4月26日 下午1:26:55
     * @param registry
     */
    private void registerDynamicDataSourceBeanDefinition(BeanDefinitionRegistry registry) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        beanDefinition.setPrimary(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultTargetDataSource", defaultDataSource);
        mpv.addPropertyValue("targetDataSources", targetDataSources);
        registry.registerBeanDefinition("dataSource", beanDefinition);
        LOGGER.info("Dynamic DataSource Registry");
    }
    
    /**
     * @param dataSourceClassType
     * @return
     * @Title: buildDataSource
     * @Description: 根据数据源class类型创建数据源
     * @author Kola 6089555
     * @date 2017年4月24日 下午6:46:47
     */
    @SuppressWarnings("unchecked")
    public DataSource buildDataSource(String dataSourceClassType) {
        DataSource dataSource = null;
        try {
            if (StringUtils.isBlank(dataSourceClassType))
                dataSourceClassType = DATASOURCE_TYPE_DEFAULT;
            Class<? extends DataSource> dataSourceType = (Class<? extends DataSource>) Class.forName(dataSourceClassType);
            dataSource = DataSourceBuilder.create().type(dataSourceType).build();
        } catch (ClassNotFoundException e) {
            LOGGER.error("buildDataSource failure.because {}", e.getMessage());
            e.printStackTrace();
        }
        return dataSource;
    }
    
    @Override
    public void setEnvironment(Environment env) {
        initDefaultDataSource(env);
        initTargetDataSources(env);
    }
    
    /**
     * @param env
     * @Title: initDefaultDataSource
     * @Description: 初始化主数据源
     * @author Kola 6089555
     * @date 2017年4月24日 下午6:46:16
     */
    private void initDefaultDataSource(Environment env) {
        defaultDataSource = buildDataSource(new RelaxedPropertyResolver(env, "druid.dataSource.").getProperty("type"));
        dataBinder(defaultDataSource, env, "druid.dataSource");
        targetDataSources.put("master", defaultDataSource);
    }
    
    /**
     * @param dataSource
     * @param env
     * @param prefix
     * @return
     * @Title: dataBinder
     * @Description: 绑定数据源属性数据
     * @author Kola 6089555
     * @date 2017年4月24日 下午6:45:44
     */
    private DataSource dataBinder(DataSource dataSource, Environment env, String prefix) {
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(dataSource);
        dataBinder.setConversionService(conversionService);
        dataBinder.setIgnoreNestedProperties(false);
        dataBinder.setIgnoreInvalidFields(false);
        dataBinder.setIgnoreUnknownFields(true);
        Map<String, Object> rpr = new RelaxedPropertyResolver(env, prefix).getSubProperties(".");
        dataBinder.bind(new MutablePropertyValues(rpr));
        return dataSource;
    }
    
    /**
     * @param env
     * @Title: initTargetDataSources
     * @Description: 初始化更多数据源
     * @author Kola 6089555
     * @date 2017年4月24日 下午6:45:32
     */
    private void initTargetDataSources(Environment env) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "druid.slaveSource.");
        String slaveNodes = propertyResolver.getProperty("node");
        if (StringUtils.isNotBlank(slaveNodes)) {
            for (String dsPrefix : slaveNodes.split(",")) {
                if (StringUtils.isNotBlank(dsPrefix)) {
                    String dataSourceClass = new RelaxedPropertyResolver(env, "druid.slaveSource." + dsPrefix + ".").getProperty("type");
                    if (StringUtils.isNotBlank(dataSourceClass)) {
                        DataSource dataSource = buildDataSource(dataSourceClass);
                        dataBinder(dataSource, env, "druid.slaveSource." + dsPrefix);
                        targetDataSources.put(dsPrefix, dataSource);
                    }
                }
            }
        }
    }
}
