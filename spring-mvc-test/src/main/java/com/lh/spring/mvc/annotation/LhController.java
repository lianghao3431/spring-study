package com.lh.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * @ClassName LhController
 * @Description
 * @Date 2019/7/20 16:12
 * @Aurhor liang.hao
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LhController {

    public String value() default "";
}
