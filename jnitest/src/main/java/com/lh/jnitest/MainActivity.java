package com.lh.jnitest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.lh.jni.JniKit;
import com.lh.jni.R;

public class MainActivity extends AppCompatActivity {

    private TextView test_jni_text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        test_jni_text= (TextView) findViewById(R.id.test_jni_text);
        test_jni_text.setText("4*4="+JniKit.calculate(4));
    }

    public void getShowToast() {
        Toast.makeText(this, "jni show", Toast.LENGTH_LONG).show();
    }
}
