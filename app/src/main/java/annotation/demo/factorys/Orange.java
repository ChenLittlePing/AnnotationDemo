package annotation.demo.factorys;

import android.util.Log;

import com.factorybuilder.Factory;

/**
 * 橙子生成器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @version AnnotationDemo
 * @Datetime 2017-08-18 15:45
 * @since AnnotationDemo
 */

@Factory(ids = {4,5}, superClass = IFruit.class)
public class Orange implements IFruit {
    @Override
    public void produce() {
        Log.d("AnnotationDemo", "生成橙子");
    }
}
