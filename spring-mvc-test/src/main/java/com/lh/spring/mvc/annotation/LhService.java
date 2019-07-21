package com.lh.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * @ClassName LhService
 * @Description
 * @Date 2019/7/20 16:19
 * @Aurhor liang.hao
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LhService {

    public String value() default "";
}
