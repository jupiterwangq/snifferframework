package com.jupiter.snifferframework.dataparser;

import android.util.Log;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.data.UnknownData;

/**
 * Created by wangqiang on 16/6/10.
 * 用于处理暂时还不支持的任意协议数据
 */

public class UnknownProtoParser implements IDataParser {

    public static final String Tag = "UnknownProtoHandler";

    @Override
    public IDataParser canHandle(Packet packet) {
        return this;
    }

    @Override
    public AbsData parse(boolean output, Packet data) {
        UnknownData unknownData = new UnknownData();
        unknownData.fillData(data);
        unknownData.mIsRequest = output;
        unknownData.type = data.type;
        unknownData.setState(AbsData.STATE_DATA_COMPLETE);
        return  unknownData;
    }

    @Override
    public void onStreamCreate() {

    }

    @Override
    public AbsData onStreamClosing() {
        return null;
    }

    @Override
    public AbsData onOutputData(Packet data) {
        Log.e(Tag, "onOutputData");
        return parse(true, data);
    }

    @Override
    public AbsData onInputData(Packet data) {
        Log.e(Tag, "onInputData");
        return parse(false, data);
    }
}
