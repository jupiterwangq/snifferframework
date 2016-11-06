package com.jupiter.snifferframework.data.http;

import android.content.Intent;
import android.text.TextUtils;
import com.jupiter.snifferframework.data.AbsData;

/**
 * Created by wangqiang on 16/6/10.
 * HTTP请求
 */

public class HttpRequest extends AbsHttpData{
    /**
     * Http request method
     * @return
     */
    public String method() {
        if (TextUtils.isEmpty(mStartLine)) {
            return "";
        }
        int idx = mStartLine.indexOf(' ');
        if (idx >= 0) {
            return mStartLine.substring(0, idx);
        }
        return "";
    }

    public String getHost() {
        String host = mHeaders.get("Host");
        if (TextUtils.isEmpty(host)) {
            host = mHeaders.get("host");
        }
        return "Host : " + host;
    }

    public String url() {
        if (TextUtils.isEmpty(mStartLine)) return "";
        int idx = mStartLine.indexOf(' ');
        if ( idx >= 0) {
            int idx2 = mStartLine.indexOf(' ', idx + 1);
            if (idx2 >= 0) {
                return mStartLine.substring(idx + 1, idx2);
            }
        }
        return "";
    }

    @Override
    public String getSummary() {
        if (TextUtils.isEmpty(mStartLine)) return "";

        return method() + " " + url();
    }

    private HttpResponse mResponse;
}
