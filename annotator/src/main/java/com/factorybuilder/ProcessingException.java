package com.factorybuilder;

import javax.lang.model.element.Element;

/**
 * 注解解析异常
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version AnnotationDemo
 * @Datetime 2017-08-18 16:52
 * @since AnnotationDemo
 */

public class ProcessingException extends Exception {

    Element element;

    public ProcessingException(Element element, String msg, Object... args) {
        super(String.format(msg, args));
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
