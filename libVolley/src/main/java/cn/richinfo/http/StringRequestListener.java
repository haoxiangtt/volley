package cn.richinfo.http;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * <pre>
 * copyright  : Copyright ©2004-2018 版权所有　彩讯科技股份有限公司
 * company    : 彩讯科技股份有限公司
 * @author     : OuyangJinfu
 * e-mail     : ouyangjinfu@richinfo.cn
 * createDate : 2017/11/15 0015
 * modifyDate : 2017/11/15 0015
 * @version    : 1.0
 * desc       :
 * </pre>
 */

public abstract class StringRequestListener implements RequestManager.RequestListener {
    @Override
    public void onSuccess(byte[] data, Map<String, String> headers, String url, String actionId) {
        String charset = "UTF-8";
        if (headers != null) {
            String contentType = headers.get("Content-Type");
            if (contentType != null && contentType.length() != 0) {
                String[] splitArray = contentType.split("=");
                if (splitArray.length == 2) {
                    charset = splitArray[1];
                }
            }
        }
        try {
            String result = new String(data, charset);
            onFinish(result, headers, url, actionId);
        } catch (UnsupportedEncodingException e) {
            onError("801", "String parse error", url, actionId);
        }

    }

    public abstract void onFinish(String data, Map<String, String> headers, String url, String actionId);

}
