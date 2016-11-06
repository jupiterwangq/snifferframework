package com.jupiter.snifferframework.data.http;

import android.text.TextUtils;

/**
 * Created by wangqiang on 16/6/10.
 * HTTP响应
 */

public class HttpResponse extends AbsHttpData{
    /**
     * Get status code of http response
     * @return
     */
    public int statusCode() {
        if (TextUtils.isEmpty(mStartLine)) return -1;
        int idx = mStartLine.indexOf(' ');
        if (idx >= 0) {
            int idx2 = mStartLine.indexOf(' ', idx + 1);
            if (idx2 >= 0) {
                String code = mStartLine.substring(idx, idx2);
                try {
                    return Integer.valueOf(code.trim());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public String reason() {
        if (TextUtils.isEmpty(mStartLine)) return "";
        int idx = mStartLine.lastIndexOf(' ');
        if (idx >= 0) {
            return mStartLine.substring(idx + 1);
        }
        return "";
    }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(statusCode()).append(" ").append(reason());
        if (isTrunk()) {
            sb.append("\n").append("[chunk] ").append("trunk-length : ").append(mData.size());
        }
        return sb.toString();
    }

}
