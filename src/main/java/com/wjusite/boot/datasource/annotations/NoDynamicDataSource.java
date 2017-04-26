package com.wjusite.boot.datasource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName: NoDynamicDataSource
 * @Description: 非动态数据源自定义注解,用于标识那些不需要动态切换数据源的Service类
 * @author Kola 6089555
 * @date 2017年3月20日 下午7:29:05
 *
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoDynamicDataSource {
    
}
