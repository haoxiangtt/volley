/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.richinfo.volley.toolbox;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import cn.richinfo.BuildConfig;
import cn.richinfo.volley.AuthFailureError;
import cn.richinfo.volley.Request;
import cn.richinfo.volley.Request.Method;
import cn.richinfo.volley.VolleyLog;

/**
 * An {@link HttpStack} based on {@link HttpURLConnection}.
 */
public class HurlStack implements HttpStack {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * An interface for transforming URLs before use.
     */
    public interface UrlRewriter {
        /**
         * Returns a URL to use instead of the provided one, or null to indicate
         * this URL should not be used at all.
         */
        public String rewriteUrl(String originalUrl);
    }

    /**
     * add by ouyangjinfu
     */
    public interface ForceNetCallback{
        void call(Network network) throws IOException;
    }

    private final UrlRewriter mUrlRewriter;
    private final SSLSocketFactory mSslSocketFactory;
    //add by ouyangjinfu
    private  Context mContext;

    public HurlStack() {
        this(null);
    }

    /**
     * @param urlRewriter Rewriter to use for request URLs
     */
    public HurlStack(UrlRewriter urlRewriter) {
        this(urlRewriter, null);
    }

    /**
     * @param urlRewriter Rewriter to use for request URLs
     * @param sslSocketFactory SSL factory to use for HTTPS connections
     */
    public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
    }

    //add by ouyangjinfu
    public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory, Context context) {
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
        mContext = context;
    }

    @Override
    public HttpResponse performRequest(final Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        String url = request.getUrl();
        HashMap<String, String> map = new HashMap<String, String>();
        map.putAll(request.getHeaders());
        map.putAll(additionalHeaders);
        if (mUrlRewriter != null) {
            String rewritten = mUrlRewriter.rewriteUrl(url);
            if (rewritten == null) {
                throw new IOException("URL blocked by rewriter: " + url);
            }
            url = rewritten;
        }


        final URL parsedUrl = new URL(url);
        //change by ouyangjinfu
        HttpURLConnection connection;
        final HttpURLConnection[] connections = new HttpURLConnection[1];
        if (mContext != null && request.isForceNet()) {
            forceNet(mContext, url, new ForceNetCallback() {
                @Override
                @SuppressWarnings("NewApi")
                public void call(Network network) throws  IOException{
                    if (network != null) {
                        connections[0] = (HttpURLConnection) network.openConnection(parsedUrl);
                        int timeoutMs = request.getTimeoutMs();
                        connections[0].setConnectTimeout(timeoutMs);
                        connections[0].setReadTimeout(timeoutMs);
                        connections[0].setUseCaches(false);
                        connections[0].setDoInput(true);
                        // use caller-provided custom SslSocketFactory, if any, for HTTPS
                        if ("https".equals(parsedUrl.getProtocol()) && mSslSocketFactory != null) {
                            ((HttpsURLConnection)connections[0]).setSSLSocketFactory(mSslSocketFactory);
                        }
                    } else {
                        connections[0] = openConnection(parsedUrl, request);
                    }
                }
            });
            int count = 0;
            while(true) {
                if (connections[0] != null){
                    connection = connections[0];
                    break;
                } else {
                    if (count >= 10) {
                        connection = openConnection(parsedUrl, request);
                        break;
                    } else {
                        count++;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                }
            }

        } else {
            connection = openConnection(parsedUrl, request);
        }

        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        setConnectionParametersForRequest(connection, request);
        // Initialize HttpResponse with data from the HttpURLConnection.
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }
        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                connection.getResponseCode(), connection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        if (hasResponseBody(request.getMethod(), responseStatus.getStatusCode())) {
            response.setEntity(entityFromConnection(connection));
        }
        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }
        return response;
    }

    /**
     * add by ouyangjinfu
     * 强制打开数据网络访问网络；(注意：此方法会阻塞线程)
     * @param context
     * @param url
     * @param callback
     */
    private void forceNet(Context context, final String url, final ForceNetCallback callback)
            throws IOException{
        final ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= 21){
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            NetworkRequest networkRequest = builder.build();
            ConnectivityManager.NetworkCallback myNetCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {

                    //需要使用数据网络的链接,均调用network建立连接
                    if (callback != null) {
                        try {
                            callback.call(network);
                        } catch (IOException e) {}
                    }
                }
            };
            connectivityManager.requestNetwork(networkRequest, myNetCallback);
        }else {

            //4.4及其以下版本不能按次控制哪个网卡访问url而是按时间段(路由表更新时间)来控制的.
            //重点在第一次访问时的路由,新配路由会在一段时间后(20-30s)失效又会回归默认,此特性不影响数据网络取号.

            //enableHIPRI,开启数据网卡
            connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableHIPRI");
            //wait some time needed to connection manager for waking up
            try {
                for (int counter = 0; counter < 30; counter++) {
                    NetworkInfo.State checkState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI)
                            .getState();
                    if (0 == checkState.compareTo(NetworkInfo.State.CONNECTED))
                        break;
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                    VolleyLog.e("check hipri failed");
            }
            int hostAddress = lookupHost(extractAddressFromUrl(url));
            //新增路由
            boolean resultBool1 = connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);
            //新增域则需要新增路由,比如有302跳转到其他域下,就需要把location的路由加入.

            if (BuildConfig.DEBUG) {
                VolleyLog.e("change network result >>> " + resultBool1 );
            }

            if (resultBool1) {
                if (callback != null) {
                    callback.call(null);
                }
            } else {
                VolleyLog.e("change network failed or mobile network is null");
            }
        }

    }

    /**
     * add by ouyangjinfu
     * method
     * @param hostname
     * @return -1 if the host doesn't exists, elsewhere its translation
     * to an integer
     */
    private int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                | (addrBytes[0] & 0xff);
        return addr;
    }

    /**
     * add by ouyangjinfu
     * This method extracts from address the hostname
     * @param url eg. http://some.where.com:8080/sync
     * @return some.where.com
     */
    private String extractAddressFromUrl(String url) {
        String urlToProcess = null;

        //find protocol
        int protocolEndIndex = url.indexOf("://");
        if (protocolEndIndex > 0) {
            urlToProcess = url.substring(protocolEndIndex + 3);
        } else {
            urlToProcess = url;
        }

        // If we have port number in the address we strip everything
        // after the port number
        int pos = urlToProcess.indexOf(':');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }

        // If we have resource location in the address then we strip
        // everything after the '/'
        pos = urlToProcess.indexOf('/');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }

        // If we have ? in the address then we strip
        // everything after the '?'
        pos = urlToProcess.indexOf('?');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }
        return urlToProcess;
    }

    /**
     * Checks if a response message contains a body.
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3">RFC 7230 section 3.3</a>
     * @param requestMethod request method
     * @param responseCode response status code
     * @return whether the response has a body
     */
    private static boolean hasResponseBody(int requestMethod, int responseCode) {
        return requestMethod != Request.Method.HEAD
            && !(HttpStatus.SC_CONTINUE <= responseCode && responseCode < HttpStatus.SC_OK)
            && responseCode != HttpStatus.SC_NO_CONTENT
            && responseCode != HttpStatus.SC_NOT_MODIFIED;
    }

    /**
     * Initializes an {@link HttpEntity} from the given {@link HttpURLConnection}.
     * @param connection
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     * @param url
     * @return an open connection
     * @throws IOException
     */
    private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection)connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

    @SuppressWarnings("deprecation")
    /* package */ static void setConnectionParametersForRequest(HttpURLConnection connection,
            Request<?> request) throws IOException, AuthFailureError {
        switch (request.getMethod()) {
            case Method.DEPRECATED_GET_OR_POST:
                // This is the deprecated way that needs to be handled for backwards compatibility.
                // If the request's post body is null, then the assumption is that the request is
                // GET.  Otherwise, it is assumed that the request is a POST.
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    // Prepare output. There is no need to set Content-Length explicitly,
                    // since this is handled by HttpURLConnection using the size of the prepared
                    // output stream.
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HEADER_CONTENT_TYPE,
                            request.getPostBodyContentType());
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.write(postBody);
                    out.close();
                }
                break;
            case Method.GET:
                // Not necessary to set the request method because connection defaults to GET but
                // being explicit here.
                connection.setRequestMethod("GET");
                break;
            case Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            case Method.HEAD:
                connection.setRequestMethod("HEAD");
                break;
            case Method.OPTIONS:
                connection.setRequestMethod("OPTIONS");
                break;
            case Method.TRACE:
                connection.setRequestMethod("TRACE");
                break;
            case Method.PATCH:
                connection.setRequestMethod("PATCH");
                addBodyIfExists(connection, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static void addBodyIfExists(HttpURLConnection connection, Request<?> request)
            throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
}
