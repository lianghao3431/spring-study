package com.lh.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * @ClassName LhRequestParam
 * @Description
 * @Date 2019/7/20 21:05
 * @Aurhor liang.hao
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LhRequestParam {

    public String value() default "";
}
