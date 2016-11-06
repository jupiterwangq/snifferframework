package com.jupiter.snifferframework.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by wangqiang on 16/6/11.
 */

public class ByteArrayHelper {

    public static int byte2ToUnsignedShort(byte[] bytes, int off) {
        int high = bytes[off];
        int low = bytes[off + 1];
        return (high << 8 & 0xFF00) | (low & 0xFF);
    }

    public static byte[] arrayReplace(byte[] org, byte[] search, byte[] replace, int startIndex) throws UnsupportedEncodingException {
        int index = indexOf(org, search, startIndex);
        if (index != -1) {
            int newLength = org.length + replace.length - search.length;
            byte[] newByte = new byte[newLength];
            System.arraycopy(org, 0, newByte, 0, index);
            System.arraycopy(replace, 0, newByte, index, replace.length);
            System.arraycopy(org, index + search.length, newByte, index + replace.length, org.length - index - search.length);
            int newStart = index + replace.length;
            if ((newByte.length - newStart) > replace.length) {
                return arrayReplace(newByte, search, replace, newStart);
            }
            return newByte;
        } else {
            return org;
        }
    }

    public static byte[] append(byte[] org, byte[] to) {
        byte[] newByte = new byte[org.length + to.length];
        System.arraycopy(org, 0, newByte, 0, org.length);
        System.arraycopy(to, 0, newByte, org.length, to.length);
        return newByte;
    }

    public static byte[] append(byte[] org, byte to) {
        byte[] newByte = new byte[org.length + 1];
        System.arraycopy(org, 0, newByte, 0, org.length);
        newByte[org.length] = to;
        return newByte;

    }

    public static void append(byte[] org, int from, byte[] append) {
        System.arraycopy(append, 0, org, from, append.length);

    }

    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException("");
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));

        return copy;

    }

    public static byte[] char2byte(String encode, char... chars) {
        Charset cs = Charset.forName(encode);
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    public static int indexOf(byte[] org, byte[] search) {
        return indexOf(org, search, 0);
    }

    public static int indexOf(byte[] org, byte[] search, int startIndex) {
        KMPMatcher kmpMatcher = new KMPMatcher();
        kmpMatcher.computeFailure4Byte(search);
        return kmpMatcher.indexOf(org, startIndex);
    }

    public static int lastIndexOf(byte[] org, byte[] search) {
        return lastIndexOf(org, search, 0);
    }

    public static int lastIndexOf(byte[] org, byte[] search, int fromIndex) {
        KMPMatcher kmpMatcher = new KMPMatcher();
        kmpMatcher.computeFailure4Byte(search);
        return kmpMatcher.lastIndexOf(org, fromIndex);
    }
}
