volley框架改进版
根据公司的需要，主要改进内容：
1、优化二次封装接口；
2、添加wifi下强制数据网络访问开关；
3、修复部分bug；
4、删减部分扩展类和辅助类，精简框架。

引入方式：在build.gradle中加入以下：
    android {
        ......
        useLibrary 'org.apache.http.legacy'
    }

    dependencies {
        ......
        compile 'com.bfy:volley:1.0.4'
    }


混淆规则：
    -keep class org.apache.http.**
    -keep class cn.bfy.volley.Request {*;}
    -keep class cn.bfy.volley.RequestQueue {*;}
    -keep class cn.bfy.volley.toolbox.HttpStack {*;}
    -keep class cn.bfy.volley.toolbox.Volley {*;}
    -keep class * extends cn.bfy.volley.Request {*;}
    -keep class cn.bfy.http.** {*;}

简单用法：
    RequestManager类为主要接口类，里面提供了init()、post()、get()多种重构方法，其中init()为
初始化方法。
    RequestManager.getInstance().init(this);
        String url = "http://pv.sohu.com/cityjson";
        RequestManager.getInstance().post(url,null, null, new StringRequestListener() {
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
          }, UUID.randomUUID().toString());