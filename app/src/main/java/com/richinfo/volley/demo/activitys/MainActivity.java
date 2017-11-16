package com.richinfo.volley.demo.activitys;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.richinfo.volley.demo.R;

import java.util.Map;
import java.util.UUID;

import cn.richinfo.http.RequestManager;
import cn.richinfo.http.StringRequestListener;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * 调用方式
         */
        RequestManager.getInstance().init(this);
        String url = "http://pv.sohu.com/cityjson";
        RequestManager.getInstance().post(url,null, null, listener, UUID.randomUUID().toString());
    }

    StringRequestListener listener = new StringRequestListener() {
        @Override
        public void onRequest() {

        }

        @Override
        public void onFinish(String data, Map<String, String> headers, String url, String actionId) {
            Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String errorCode, String errorMsg, String url, String actionId) {
            Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    };

}
