package com.jupiter.snifferframework.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangqiang on 16/8/17.
 */

public class Util {

    public static final BigDecimal unsignedLong(long value) throws IOException {
        if (value >= 0)
            return new BigDecimal(value);
        long lowValue = value & 0x7fffffffffffffffL;
        return BigDecimal.valueOf(lowValue).add(BigDecimal.valueOf(Long.MAX_VALUE)).add(BigDecimal.valueOf(1));
    }

    public static List<String> toHexString(byte[] b, boolean seperate) {
        ArrayList ret = new ArrayList();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;

            }
            if (seperate) hex += " ";
            ret.add(hex);
        }
        return ret;
    }
}
