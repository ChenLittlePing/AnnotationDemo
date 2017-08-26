package annotation.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import annotation.demo.factorys.IFruitFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        produceFruit();
    }

    private void produceFruit() {
        IFruitFactory.create(5).produce();
    }
}
