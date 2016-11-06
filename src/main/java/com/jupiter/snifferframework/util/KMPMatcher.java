package com.jupiter.snifferframework.util;

/**
 * Created by wangqiang on 16/6/11.
 */

public class KMPMatcher {

    private int[] failure;

    private int matchPoint;

    private byte[] bytePattern;

    /**
     * Method indexOf
     * @param text of type byte[]
     * @param startIndex of type int
     * @return int
     */
    public int indexOf(byte[] text, int startIndex) {
        int j = 0;
        if (text.length == 0 || startIndex > text.length) return -1;
        for (int i = startIndex; i < text.length; i++) {
            while (j > 0 && bytePattern[j] != text[i]) {
                j = failure[j - 1];
            }
            if (bytePattern[j] == text[i]) {
                j++;
            }
            if (j == bytePattern.length) {
                matchPoint =i - bytePattern.length + 1;
                return matchPoint;
            }
        }
        return -1;
    }

    /**
     * 找到末尾后重头开始找
     * @param text of type byte[]
     * @param startIndex of type int
     * @return int
     */
    public int lastIndexOf(byte[] text, int startIndex) {
        matchPoint = -1;
        int j = 0;
        if (text.length == 0 || startIndex > text.length) return - 1;
        int end = text.length;
        for (int i = startIndex; i < end; i++) {
            while (j > 0 && bytePattern[j] != text[i]) {
                j = failure[j - 1];
            }
            if (bytePattern[j] == text[i]) {
                j++;
            }
            if (j == bytePattern.length) {
                matchPoint = i - bytePattern.length + 1;
                if ((text.length - i) > bytePattern.length) {
                    j = 0;
                    continue;
                }
                return matchPoint;
            }
            //如果从中间某个位置找，找到末尾没找到后，再重头开始找
            if (startIndex != 0 && i + 1 == end) {
                end = startIndex;
                i = -1;
                startIndex = 0;
            }
        }
        return matchPoint;
    }

    /**
     * @param text of type byte[]
     * @param startIndex of type int
     * @return int
     */
    public int lastIndexOfWithNoLoop(byte[] text, int startIndex) {
        matchPoint = -1;
        int j = 0;
        if (text.length == 0 || startIndex > text.length) return -1;
        for (int i = startIndex; i < text.length; i++) {
            while (j > 0 && bytePattern[j] != text[i]) {
                j = failure[j - 1];
            }
            if (bytePattern[j] == text[i]) {
                j++;
            }
            if (j == bytePattern.length) {
                matchPoint = i - bytePattern.length + 1;
                if ((text.length - i) > bytePattern.length) {
                    j = 0;
                    continue;
                }
                return matchPoint;
            }
        }
        return matchPoint;
    }

    /**
     * Method computeFailure4Byte …
     *
     * @param patternStr of type byte[]
     */
    public void computeFailure4Byte(byte[] patternStr) {
        bytePattern = patternStr;
        int j = 0;
        int len = bytePattern.length;
        failure = new int[len];
        for (int i = 1; i < len; i++) {
            while (j > 0 && bytePattern[j] != bytePattern[i]) {
                j = failure[j - 1];
            }
            if (bytePattern[j] == bytePattern[i]) {
                j++;
            }
            failure[i] = j;
        }
    }

}