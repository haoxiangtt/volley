package cn.richinfo.http;

import cn.richinfo.http.base.LoadControler;
import cn.richinfo.volley.Request;

/**
 * <pre>
 * @copyright  : Copyright ©2004-2018 版权所有　XXXXXXXXXXXXXXXXX
 * @company    : XXXXXXXXXXXXXXXXX
 * @author     : OuyangJinfu
 * @e-mail     : jinfu123.-@163.com
 * @createDate : 2017/4/13 0023
 * @modifyDate : 2017/4/13 0023
 * @version    : 1.0
 * @desc       : Abstract LoaderControler that implements LoadControler
 * </pre>
 */
public abstract class AbsLoadControler implements LoadControler {

    protected Request<?> mRequest;

    public void bindRequest(Request<?> request) {
        this.mRequest = request;
    }

    @Override
    public void cancel() {
        if (this.mRequest != null) {
            this.mRequest.cancel();
        }
    }

    protected String getUrl() {
        return this.mRequest.getUrl();
    }
}