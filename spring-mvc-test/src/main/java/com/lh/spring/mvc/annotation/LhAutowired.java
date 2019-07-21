package com.lh.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * @ClassName LhAutowired
 * @Description
 * @Date 2019/7/20 16:37
 * @Aurhor liang.hao
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LhAutowired {

    boolean required() default true;


    public String value() default "";
}
