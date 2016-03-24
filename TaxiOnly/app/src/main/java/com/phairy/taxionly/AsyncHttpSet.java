package com.phairy.taxionly;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Created by dowon on 2016-02-09.
 */
public class AsyncHttpSet {

    public AsyncHttpSet(Boolean b) {
        if (b == true){
            final String BASE_URL = Start.SURL;
        }else{
            final String BASE_URL = Start.URL;
        }

    }
    private static final String BASE_URL = Start.URL;

    private static com.loopj.android.http.AsyncHttpClient client = new com.loopj.android.http.AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
