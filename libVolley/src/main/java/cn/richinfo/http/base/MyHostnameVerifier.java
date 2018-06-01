package cn.richinfo.http.base;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


/**
 * <pre>
 * copyright  : Copyright ©2004-2018 版权所有　XXXXXXXXXXXXXXXXX
 * company    : XXXXXXXXXXXXXXXXX
 * @author     : OuyangJinfu
 * e-mail     : jinfu123.-@163.com
 * createDate : 2017/4/13 0023
 * modifyDate : 2017/4/13 0023
 * @version    : 1.0
 * desc       : MyHostnameVerifier.java
 * </pre>
 */
public class MyHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        boolean result = true;
       /* if (BuildConfig.BASE_URL.contains("www.cmpassport.com")) {
            result = hostname.contains("www.cmpassport.com");
        }*/
        return result;
    }
}
