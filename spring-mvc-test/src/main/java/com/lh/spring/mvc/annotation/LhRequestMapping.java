package com.lh.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * @ClassName LhRequestMapping
 * @Description
 * @Date 2019/7/20 16:48
 * @Aurhor liang.hao
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LhRequestMapping {
    public String value() default "";
}
