package annotation.demo.factorys;

import android.util.Log;

import com.factorybuilder.Factory;

/**
 * 柿子生成器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version AnnotationDemo
 * @Datetime 2017-08-18 15:45
 * @since AnnotationDemo
 */

@Factory(ids = {6}, superClass = IFruit.class)
public class Persimmon implements IFruit {
    @Override
    public void produce() {
        Log.d("AnnotationDemo", "生成柿子");
    }
}
