package com.factorybuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工厂注解
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version AnnotationDemo
 * @Datetime 2017-08-18 15:41
 * @since AnnotationDemo
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Factory {
    /**
     * 工厂对应的ID，可以多个ID对应一个工厂
     */
    int[] ids();

    /**
     * 工厂接口类
     */
    Class superClass();
}
