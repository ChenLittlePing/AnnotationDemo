package com.factorybuilder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * 工厂代码生成器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version AnnotationDemo
 * @Datetime 2017-08-18 17:14
 * @since AnnotationDemo
 */

public class FactoryCodeBuilder {

    private static final String SUFFIX = "Factory";

    private String mSupperClsName;

    private Map<String, FactoryAnnotatedCls> mAnnotatedClasses = new LinkedHashMap<>();

    public void add(FactoryAnnotatedCls annotatedCls) {
        if (mAnnotatedClasses.get(annotatedCls.getAnnotatedClsElement().getQualifiedName().toString()) != null)
            return ;

        mAnnotatedClasses.put(
                annotatedCls.getAnnotatedClsElement().getQualifiedName().toString(),
                annotatedCls);
    }

    public void clear() {
        mAnnotatedClasses.clear();
    }

    public FactoryCodeBuilder setSupperClsName(String supperClsName) {
        mSupperClsName = supperClsName;
        return this;
    }

    public void generateCode(Messager messager, Elements elementUtils, Filer filer) throws IOException {
        TypeElement superClassName = elementUtils.getTypeElement(mSupperClsName);
        String factoryClassName = superClassName.getSimpleName() + SUFFIX;
        PackageElement pkg = elementUtils.getPackageOf(superClassName);
        String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();

        TypeSpec typeSpec = TypeSpec
                .classBuilder(factoryClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(newCreateMethod(elementUtils, superClassName))
                .addMethod(newCompareIdMethod())
                .build();

        // Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private MethodSpec newCreateMethod(Elements elementUtils, TypeElement superClassName) {

        MethodSpec.Builder method =
                MethodSpec.methodBuilder("create") //设置方法名字
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC) //设置方法类型为public static
                .addParameter(int.class, "id") //设置参数int id
                .returns(TypeName.get(superClassName.asType())); //设置返回IFruit

        method.beginControlFlow("if (id < 0)") //beginControlFlow与endControlFlow要成对调用
                .addStatement("throw new IllegalArgumentException($S)", "id is less then 0!")
                .endControlFlow();

        for (FactoryAnnotatedCls annotatedCls : mAnnotatedClasses.values()) { //遍历所有保存起来的被注解的生产线类
            String packName = elementUtils
                    .getPackageOf(annotatedCls.getAnnotatedClsElement())
                    .getQualifiedName().toString(); //获取生产线类的包名全路径
            String clsName = annotatedCls.getAnnotatedClsElement().getSimpleName().toString(); //获取生产线类名字
            ClassName cls = ClassName.get(packName, clsName); //组装成一个ClassName

            //将该生产线类的所有id组成数组
            int[] ids = annotatedCls.getIds();
            String allId = "{";
            for (int id : ids) {
                allId = allId + (allId.equals("{")? "":",") + id;
            }
            allId+="}";

            method.beginControlFlow("if (compareId(new int[]$L, id))", allId) //开始一个控制流，判断该生产线类是否包含了指定的id
                    .addStatement("return new $T()", cls)   // $T 替换类名，可以自动import对应的类。还有以下占位符：
                                                            // $N 用于方法名或者变量名替换，也可用于类名，但是不会自动生成import；
                                                            // $L 字面量替换，如上面if中allId的值替换；
                                                            // $S 为替换成String
                    .endControlFlow();
        }

        method.addStatement("throw new IllegalArgumentException($S + id)", "Unknown id = ");

        return method.build();
    }

    private MethodSpec newCompareIdMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("compareId") //设置函数方法名字
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC) //设置方法类型为private static
                .addParameter(int[].class, "ids") //设置参数int[] ids
                .addParameter(int.class, "id") //设置参数int id
                .returns(TypeName.BOOLEAN); //设置返回类型

        builder.beginControlFlow("for (int i : ids)") //开始一个控制流
                .beginControlFlow("if (i == id)") //在以上for循环中加入一个if控制流
                .addStatement("return true") //添加一行代码，最后会自动添加分号";"
                .endControlFlow() //结束一个控制流，add和end要成对调用。这里对应if的控制流
                .endControlFlow() //结束for控制流
                .addStatement("return false"); //按添加返回

        return builder.build();
    }
}
