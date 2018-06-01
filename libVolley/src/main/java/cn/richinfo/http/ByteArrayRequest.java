package cn.richinfo.http;

import org.apache.http.HttpEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import cn.richinfo.http.base.RequestMap;
import cn.richinfo.volley.AuthFailureError;
import cn.richinfo.volley.NetworkResponse;
import cn.richinfo.volley.Request;
import cn.richinfo.volley.Response;
import cn.richinfo.volley.Response.ErrorListener;
import cn.richinfo.volley.toolbox.HttpHeaderParser;


/**
 * <pre>
 * copyright  : Copyright ©2004-2018 版权所有　XXXXXXXXXXXXXXXXX
 * company    : XXXXXXXXXXXXXXXXX
 * @author     : OuyangJinfu
 * e-mail     : jinfu123.-@163.com
 * createDate : 2017/4/13 0023
 * modifyDate : 2017/4/13 0023
 * @version    : 1.0
 * desc       : ByteArrayRequest override getBody() and getParams()
 * </pre>
 */
class ByteArrayRequest extends Request<NetworkResponse> {

	private final Response.Listener<NetworkResponse> mListener;

	private Object mPostBody = null;

	private HttpEntity httpEntity =null;
	
	private Map<String,String> headers = null;

	public ByteArrayRequest(int method, String url, Object postBody, Response.Listener<NetworkResponse> listener, ErrorListener errorListener) {
		this(method, url, postBody, false, listener, errorListener);
	}

	public ByteArrayRequest(int method, String url, Object postBody,boolean isForceDataNet, Response.Listener<NetworkResponse> listener, ErrorListener errorListener) {
		super(method, url, isForceDataNet, errorListener);
		this.mPostBody = postBody;
		this.mListener = listener;

		if (this.mPostBody != null && this.mPostBody instanceof RequestMap) {// contains file
			this.httpEntity = ((RequestMap) this.mPostBody).getEntity();
		}
	}

	/**
	 * mPostBody is null or Map<String, String>, then execute this method
	 */
	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		if (this.httpEntity == null && this.mPostBody != null && this.mPostBody instanceof Map<?, ?>) {
			return ((Map<String, String>) this.mPostBody);//common Map<String, String>
		}
		return null;//process as json, xml or MultipartRequestParams
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
//		Map<String, String> headers = super.getHeaders();
		if (null == headers || headers.equals(Collections.emptyMap())) {
			headers = new HashMap<String, String>();
		}
		return headers;
	}

	@Override
	public String getBodyContentType() {
		if (httpEntity != null) {
			return httpEntity.getContentType().getValue();
		}
		if (mPostBody != null && mPostBody instanceof String) {

			if (mPostBody.toString().matches
				("([0-9a-zA-Z_%]+=[0-9a-zA-Z\\u4e00-\\u9fa5_%]*?&?)+")){
				return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
			}

			try {
				new JSONObject(mPostBody.toString());
				return "application/json; charset=" + getParamsEncoding();
			} catch (JSONException e) {}

			ByteArrayInputStream bis = null;
			try {
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				bis = new ByteArrayInputStream(((String) mPostBody).getBytes());
				db.parse(bis);
				return "text/xml; charset=" + getParamsEncoding();
			} catch (ParserConfigurationException e) {}
			catch (SAXException e) {}
			catch (IOException e) {}
			finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {}
				}
			}
		}
		return super.getBodyContentType();
	}


	@Override
	public byte[] getBody() throws AuthFailureError {
		if (this.mPostBody != null && this.mPostBody instanceof String) {//process as json or xml
			String postString = (String) mPostBody;
			if (postString.length() != 0) {
				try {
					return postString.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				return null;
			}
		}
		if (this.httpEntity != null) {//process as MultipartRequestParams
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					httpEntity.writeTo(baos);
					return baos.toByteArray();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}finally{
					try {
						baos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			
		}
		return super.getBody();// mPostBody is null or Map<String, String>
	}

	@Override
	protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
		return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
	}

	@Override
	protected void deliverResponse(NetworkResponse response) {
		this.mListener.onResponse(response);
	}

}