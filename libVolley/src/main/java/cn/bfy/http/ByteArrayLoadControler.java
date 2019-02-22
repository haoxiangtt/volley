package cn.bfy.http;

import cn.bfy.http.base.LoadListener;
import cn.bfy.volley.NetworkResponse;
import cn.bfy.volley.Response.ErrorListener;
import cn.bfy.volley.Response.Listener;
import cn.bfy.volley.VolleyError;

/**
 * <pre>
 * copyright  : Copyright ©2004-2018 版权所有　XXXXXXXXXXXXXXXXX
 * company    : XXXXXXXXXXXXXXXXX
 * @author     : OuyangJinfu
 * e-mail     : jinfu123.-@163.com
 * createDate : 2017/4/13 0023
 * modifyDate : 2017/4/13 0023
 * @version    : 1.0
 * desc       : ByteArrayLoadControler implements Volley Listener & ErrorListener
 * </pre>
 */
class ByteArrayLoadControler extends AbsLoadControler implements
		Listener<NetworkResponse>, ErrorListener {

	private LoadListener mOnLoadListener;

	private String mAction = "0";

	public ByteArrayLoadControler(LoadListener requestListener, String actionId) {
		this.mOnLoadListener = requestListener;
		this.mAction = actionId;
	}

	@Override
	public void onErrorResponse(VolleyError error) {
		String errorMsg;
		String errorCode;
		if (error.getMessage() != null) {
			errorMsg = error.getMessage();
			errorCode = "102101";
		} else if (error.networkResponse != null) {
			errorMsg = "Server Response Error ("
						+ error.networkResponse.statusCode + ")";
			errorCode = error.networkResponse.statusCode + "";
		} else {
			errorMsg = "Server Response Error (102101)";
			errorCode = "102101";
		}
		this.mOnLoadListener.onError(errorCode, errorMsg, getUrl(), this.mAction);
	}

	@Override
	public void onResponse(NetworkResponse response) {
		this.mOnLoadListener.onSuccess(response.data, response.headers,
				getUrl(), this.mAction);
	}
}