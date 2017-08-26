### 写在前面：
越来越多的Android框架都使用了注解来实现，如有名ButterKnife、Dagger2都是用编译时注解来生成代码，好处是比反射效率更高，稳定性、可读性也更好。既然注解这么好用，那么就非常有必要对其进行了解、学习和应用。

学习注解过程中，查找了很多人分享的文章，非常感谢这些无私分享的人。其中参考了比较多的是[这篇文章](https://www.race604.com/annotation-processing/)，本文中的例子也是参考该文章，并结合自己对注解的理解，重新写了本文中的Demo，加入更详细的注释。

本文是本人在学习注解时，对注解的理解和一些基础知识的记录所写，仅仅作为入门，分享给需要的小伙伴们。可能存在一些疏漏和错误，欢迎指正～

### 一、Java注解基础：
在Java中，一个自定义的注解看起来是类似下面这样子的：

```java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Factory {
    String value() default "";
}
```

该注解用于编译时使用，生命周期由@Retention指定，@Taget表示该注解的使用范围，这里用于注解类、接口、枚举。

那么，@Retention和@Target是什么东东？

#### 元注解：

元注解的作用就是负责注解其他非元注解。Java5.0定义了4个标准的meta-annotation类型，它们被用来提供对其它 Annotation类型作说明。
#### Java5.0定义的元注解：

* @Target
* @Retention
* @Documented
* @Inherited

##### 1. @Target
  
    作用：用于描述注解的使用范围（即：被描述的注解可以用在什么地方）
   
取值(ElementType)有：

* CONSTRUCTOR:用于描述构造器
* FIELD:用于描述域
* LOCAL_VARIABLE:用于描述局部变量
* METHOD:用于描述方法
* PACKAGE:用于描述包
* PARAMETER:用于描述参数
* TYPE:用于描述类、接口(包括注解类型) 或enum声明

##### 2. @Retention
    
    作用：表示需要在什么级别保存该注释信息，用于描述注解的生命周期（即：被描述的注解在什么范围内有效）

取值（RetentionPoicy）有：

* SOURCE:在源文件中有效（即源文件保留，只在源文件中，如@Override）
* CLASS:在class文件中有效（即class保留，可在编译时获取，本文主讲内容）
* RUNTIME:在运行时有效（即运行时保留,可在运行是通过反射获取）

##### 3.@Documented:

    @Documented用于描述其它类型的annotation应该被作为被标注的程序成员的公共API，  
    因此可以被例如javadoc此类的工具文档化。Documented是一个标记注解，没有成员。
    
##### 4.@Inherited：

    @Inherited 元注解是一个标记注解，@Inherited阐述了某个被标注的类型是被继承的。  
    如果一个使用了@Inherited修饰的annotation类型被用于一个class，则这个annotation将被用于该class的子类。  
    使用Inherited声明出来的注解，只有在类上使用时才会有效，对方法，属性等其他无效。

#### 自定义注解

##### 格式：public @interface 注解名 {定义体}

##### 注解参数的可支持数据类型：

1. 所有基本数据类型（int,float,boolean,byte,double,char,long,short)
2. String类型
3. Class类型
4. enum类型
5. Annotation类型
6. 以上所有类型的数组

>参数职能用public或默认(default)修饰

>如果只有一个参数成员,最好把参数名称设为"value",后加小括号,即value()

### 二、在Android中应用编译时注解，自动生成工厂代码

首先以工厂模式为例，看看在工厂模式中存在的问题。本例假设为水果工厂。

1.通常，在工厂模式中，我们会定义一个工厂生产接口方法：

```Java
public interface IFruit {
    void produce();
}
```
2.接着，定义具体的工厂生产线类：

```Java
public class Apple implements IFruit {
    @Override
    public void produce() {
        Log.d("AnnotationDemo", "生产苹果");
    }
}

public class Pear implements IFruit {
    @Override
    public void produce() {
        Log.d("AnnotationDemo", "生产梨子");
    }
}

```

3.然后，定义生产工厂类：

```Java
public class FruitFactory {
    public static IFruit create(int id) {
        if (1 == id) {
            return new Apple();
        }
        if (2 == id) {
            return new Pear();
        }
        
        return null;
    }
}
```

4.最后，使用工厂：

```Java
public void produceFruit() {
    FruitFactory.create(1).produce();
    FruitFactory.create(2).produce();
}
```

* 存在问题：

    在以上例子中，每次新增生产线的时候，都需要先定义一个生产线，然后在FruitFactory的create方法中新增判断，返回新的生产线类，并且每次添加的代码都是非常相似重复的。

    为此，“懒惰”的我们肯定会想，是否有方法可以做到：只要我定义好一个生产线类后，无需手动地在工厂类中添加，就马上可以使用？
    
    答案是肯定的，Java的注解处理器（AbstractProcessor）就可以帮助我们实现以上需求。
    
接下来，我们就一步步来实现这个可以让我们懒出新境界的功能：
#### 1. 新建Android工程和Java Module
> 注意：由于Android默认不支持部分javax包的内容，所以我们需要将注解解析相关的类放到Java Module中才能调用到。
* 建立好Android工程 **AnnotationDemo**
* 新建annotator Module ：Filw -> New -> New Module -> Java Library 并命名为**annotator**

#### 2. 配置APT(Annotation Processor Tool)工具。
> 由于android-apt已经不再维护，并且Android官方在Gradle2.2以上已经提供了另一个工具annotationProcessor替代了原来的android-apt，所以我们直接使用annotationProcessor。
Gradle2.2以下版本配置请看最后。

在app的build.gradle中添加如下依赖：

```
dependencies {
    ......
    
    compile project(':annotator')
    annotationProcessor project(':annotator')
}
```

#### 3. 码注解处理器
> 以上配置完成后，就可以开始码注解处理器了。

1）首先，自定义一个注解，用于标识生产线类，该注解包含两个参数:
* 一个生产线类id数组ids，可多个id对应一个类
* 另一个是该生产类的接口父类，用于标识生产线类的接口父类

```Java
@Retention(RetentionPolicy.CLASS) //该注解只保留到编译时
@Target(ElementType.TYPE) //该注解只作用与类、接口、枚举
public @interface Factory {
    /**
     * 工厂对应的ID，可以多个ID对应一个生产线类
     */
    int[] ids();

    /**
     * 生产接口类
     */
    Class superClass();
}
```

#### 2）使用以上注解标记生产线类

```Java
@Factory(ids = {1}, superClass = IFruit.class)
public class Apple implements IFruit {
    @Override
    public void produce() {
        Log.d("AnnotationDemo", "生成苹果");
    }
}

@Factory(ids = {2,3}, superClass = IFruit.class)
public class Pear implements IFruit {
    @Override
    public void produce() {
        Log.d("AnnotationDemo", "生成梨子");
    }
}
```

以上Pear类上，我们使用了Factory注解标记，其中参数ids有两个id，即使用2或者3都可以获取到Pear；superClass为生产接口类。

#### 3）编写注解解析器

* ***i. 首先，定义一个注解属性类，用于保存获取到的每个生产线类相关的属性***

```Java
public class FactoryAnnotatedCls {
    private TypeElement mAnnotatedClsElement; //被注解类元素

    private String mSupperClsQualifiedName; //被注解的类的父类的完全限定名称（即类的绝对路径）

    private String mSupperClsSimpleName; //被注解类的父类类名

    private int[] mIds; //被注解的类的对应的ID数组


    public FactoryAnnotatedCls(TypeElement classElement) throws ProcessingException {
        this.mAnnotatedClsElement = classElement;
        Factory annotation = classElement.getAnnotation(Factory.class);
        mIds = annotation.ids();
        try {
            //直接获取Factory中的supperClass参数的类名和完全限定名字，如果是源码上的注解，会抛异常
            mSupperClsSimpleName = annotation.superClass().getSimpleName();
            mSupperClsQualifiedName = annotation.superClass().getCanonicalName();
        } catch (MirroredTypeException mte) {
            //如果获取异常，通过mte可以获取到上面无法解析的superClass元素
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            mSupperClsQualifiedName = classTypeElement.getQualifiedName().toString();
            mSupperClsSimpleName = classTypeElement.getSimpleName().toString();
        }

        if (mIds == null || mIds.length == 0) { //判断是否存在ID，不存在则抛出异常
            throw new ProcessingException(classElement,
                    "id() in @%s for class %s is null or empty! that's not allowed",
                    Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
        }

        if (mSupperClsSimpleName == null || mSupperClsSimpleName == "") { //判断是否存在父类接口，不存在抛出异常
            throw new ProcessingException(classElement,
                    "superClass() in @%s for class %s is null or empty! that's not allowed",
                    Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
        }
    }

    public int[] getIds() {
        return mIds;
    }

    public String getSupperClsQualifiedName() {
        return mSupperClsQualifiedName;
    }

    public String getSupperClsSimpleName() {
        return mSupperClsSimpleName;
    }

    public TypeElement getAnnotatedClsElement() {
        return mAnnotatedClsElement;
    }
}
```

其中，有个类为TypeElement，该类继承Element。程序编译时，IDE扫描文件所有的属性都可以被看作元素。继承自Element的子类共有四个，分别为：

* TypeElement （类属性元素，对应一个类）
* PackageElement （包元素，对应一个包）
* VariableElement （变量元素，对应变量）
* ExecuteableElement （方法元素，对应函数方法）

在这里，定义的注解目标是Type，因此为TypeElement。FactoryAnnotatedCls类将被Factory注解的类中的必要属性都保存下来，用于后面生成代码。

* ***ii. 接下来，是解析注解代码的关键类：注解处理器***

所有在编译时处理注解的程序，都需要定义一个注解处理器，继承自AbstractProcessor。

```Java
@AutoService(Processor.class)
public class FactoryProcesser extends AbstractProcessor {

    private Types mTypeUtil;
    private Elements mElementUtil;
    private Filer mFiler;
    private Messager mMessager;

    private FactoryCodeBuilder mFactoryCodeBuilder = new FactoryCodeBuilder();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mTypeUtil = processingEnvironment.getTypeUtils();
        mElementUtil = processingEnvironment.getElementUtils();
        mFiler = processingEnvironment.getFiler();
        mMessager = processingEnvironment.getMessager();
    }
    
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Factory.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    ......
}
```

其中，

getSupportedAnnotationTypes()配置需要处理的注解，这里只处理@Factory注解；  
getSupportedSourceVersion()配置支持的Java版本

init()方法中，获取了几个即将用到的工具：  
mTypeUtil--主要用于获取类  
mElementUtil--主要用于解析各种元素  
mFiler--用于写文件，生成代码  
mMessager--用于在控制台输出信息

另外，在第一个行代码中，有一个注解**AutoService(Processor.class)**。这个注解的作用是可以自动生成javax.annotation.processing.Processor文件。该文件位于"build/classes/main/com/META-INF/services/"中。  

文件中只有一句话，配置了注解处理器的完全限定名。

```
com.factorybuilder.FactoryProcesser
```

当然，需要在annotator Module的build.gradle添加依赖才能使用AutoService注解。

```
compile 'com.google.auto.service:auto-service:1.0-rc2'
```

**注：只有在该文件配置了的注解处理器，在编译时才会被调用。**

完成以上配置后，就可以进入注解的解析和处理了。在编译时，编译器将自动调用注解处理器的process方法。如下：

```Java
@AutoService(Processor.class)
public class FactoryProcesser extends AbstractProcessor {
    ......
    
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(Factory.class)) { //遍历所有被Factory注解的元素
                if (annotatedElement.getKind() != ElementKind.CLASS) { //判断是否为类，如果不是class，抛出异常
                    error(annotatedElement,
                          String.format("Only class can be annotated with @%s",
                                Factory.class.getSimpleName()));
                }

                TypeElement typeElement = (TypeElement) annotatedElement; //将元素转换为TypeElement（因为在上面的代码中，已经判断了元素为class类型）
                FactoryAnnotatedCls annotatedCls = new FactoryAnnotatedCls(typeElement); //接着将该元素保存到先前定义的类中
                supperClsPath = annotatedCls.getSupperClsQualifiedName().toString(); //获取元素的父类路径（在这里为IFruit）

                checkValidClass(annotatedCls);//检查元素是否符合规则

                mFactoryCodeBuilder.add(annotatedCls); //将元素压入列表中，等待最后用于生成工厂代码
        }
    
        if (supperClsPath != null && !supperClsPath.equals("")) { //检查是否有父类路径
            mFactoryCodeBuilder
            .setSupperClsName(supperClsPath)
            .generateCode(mMessager, mElementUtil, mFiler); //开始生成代码
        }
    
        return true; //return true表示处理完毕
    }
}
```

在process方法中，  
**首先**，遍历了所有被Factory标记的元素；  
**然后**，对每一个元素进行检查，如果为class类型，并且符合指定的规则，统统压入FactoryCodeBuilder的列表中；  
**最后**，如果所有的元素都符合规则，调用factoryCodeBuilderd的generateCode生成代码。

* ***iii. 最后，来看看FacrotyCodeBuilder都做了些什么***

```Java
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
    
    ......
}
```

代码生成器中定义了一个哈希列表，用于保存所有遍历到的符合规则的元素。

```Java
public class FactoryCodeBuilder {

    ......


    public FactoryCodeBuilder setSupperClsName(String supperClsName) {
        mSupperClsName = supperClsName; //设置上产线接口父类的路径
        return this;
    }

    public void generateCode(Messager messager, Elements elementUtils, Filer filer) throws IOException {
        TypeElement superClassName = elementUtils.getTypeElement(mSupperClsName); //通过Elements工具获取父类元素
        String factoryClassName = superClassName.getSimpleName() + SUFFIX; //然后设置即将生成的工厂类的名字（在这里为IFruitFactory）
        PackageElement pkg = elementUtils.getPackageOf(superClassName); //通过Elements工具，获取父类所在包名路径（在这里为annotation.demo.factorys）
        String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString(); //获取即将生成的工厂类的包名

        TypeSpec typeSpec = TypeSpec
                .classBuilder(factoryClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(newCreateMethod(elementUtils, superClassName))
                .addMethod(newCompareIdMethod())
                .build();

        // Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }
    
    ......
}
```

在generateCode方法中，获取了生产线父类的名称和包名，以及为即将生成的工厂类设置了包名和类名。

然后借助了一个非常厉害的工具JavaPoet。这个工具是由square公司提供的，用于优雅地生成Java代码，如其名字“会写Java的诗人”。  
在annotator build.gradle中添加依赖：

```
compile 'com.squareup:javapoet:1.7.0'
```

简单介绍一下JavaPoet的用法：  

* **TypeSpec**用于创建类、接口或者枚举  
调用**classBuilder**设置类名；  
调用**addModifiers**可以设置类的属性类型，public static final等，可以同时添加多个属性  
调用**addMethod**可以在类中添加一个函数方法
* **JavaFile**将创建的类写入文件中
* **MethodSpec**接下来即将用到的，用于创建函数方法,其使用参考下面代码注释  

更详细用法请自行google，有很多的文章可以查阅。

本例中，给工厂类生成了两个方法分别为

```Java
public static IFruit create(int id)
private static compareId(int[] ids, id)
```

具体代码如下：

```Java
public class FactoryCodeBuilder {

    ......
    
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
                                                            // $L 字面量替换，如上面if中allId的值替换；；
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
```

以上代码创建了两个方法，一个对外的create方法和内部使用的compareId方法。 
 
* 在newCreateMethod中，首先创建了create(int id)方法，然后在里面用for循环遍历所有的生产线类，并生成了对应的判断和返回,最终生成类似如下代码：

```Java
public static IFruit create(int id) {
    if(compareId(new int[]{1},id)) {
        return new Apple();
    }
    if(compareId(new int[]{2,3},id)) {
        return new Pear();
    }
}
```

* 在newCompareIdMethod中，生成了compareId方法，并生了判断输入id与生产线ID匹配的方法，生成类似如下代码：

```Java
private static boolean compareId(int[] ids, int id) {
    for (int i : ids) {
      if (i == id) {
        return true;
      }
    }
    return false;
  }
```

至此，一个自动生成工厂类的注解工具就封装完成了。当然，在执行process过程中，还会对元素做一些判断，具体就不做介绍了，需要可以直接看[源码](https://github.com/ChenLittlePing/AnnotationDemo)。

如何使用该工具呢？如新增一个Orange生产线类型。

在app Mudule中的新建Orange如下：

```Java
@Facroty(ids = {5}, superClass = IFruit.class)
public class Orange implement IFruit {
    @Override
    public void produce () {
        Log.d("AnnotationDemo", "生成橙子");
    }
}
```

Build一下工程，就可以直接使用了，简直不能再爽，哈哈哈～

```Java
private void produceFruit() {
    IFruitFactory.create(5).produce();
}
```

最后，看下自动生成的工厂类,跟手写的基本是一样的（该类位于app/build/generated/source/apt/debug/接口父类包名）：

```Java
package annotation.demo.factorys;

public class IFruitFactory {
  public static IFruit create(int id) {
    if (id < 0) {
      throw new IllegalArgumentException("id is less then 0!");
    }
    if (compareId(new int[]{1}, id)) {
      return new Apple();
    }
    if (compareId(new int[]{4,5}, id)) {
      return new Orange();
    }
    if (compareId(new int[]{2,3}, id)) {
      return new Pear();
    }
    if (compareId(new int[]{6}, id)) {
      return new Persimmon();
    }
    throw new IllegalArgumentException("Unknown id = " + id);
  }

  private static boolean compareId(int[] ids, int id) {
    for (int i : ids) {
      if (i == id) {
        return true;
      }
    }
    return false;
  }
}

```

以上代码中为了方便讲解省略了一些判断和异常处理，具体可以查看[源码](https://github.com/ChenLittlePing/AnnotationDemo)。

------------------------------------==正文End : ) **我是分割线**==----------------------------------


#### gradle2.2以下版本配置
* 由于Android不完全支持Java8，可能会导致编译报错，所以设置Java版本为Java7。

    1）在app的build.gradle的android标签中添加如下配置
    
    ```
    compileOptions {
       sourceCompatibility JavaVersion.VERSION_1_7
       targetCompatibility JavaVersion.VERSION_1_7
    }
    ```
    
    2）在annotator的build.gradle中配置
    
    ```
    sourceCompatibility = 1.7
    targetCompatibility = 1.7
    ```
    
* 配置APT
    
    1）在项目的build.gradle dependencies添加apt插件：
    
    ```
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.3'
        // apt
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
    ```
    
    2）在app build.gradle最上面添加
    
    ```
    apply plugin: 'com.neenbedankt.android-apt'
    ```
    
* 配置annotator build.gradle依赖在dependencies中添加依赖
    
    ```
    dependencies {
        ......
        compile project(':annotator')
        apt project(':annotator')
    }
    ```
