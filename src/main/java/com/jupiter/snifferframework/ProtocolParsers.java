package com.jupiter.snifferframework;

import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.dataparser.DnsParser;
import com.jupiter.snifferframework.dataparser.HttpParser;
import com.jupiter.snifferframework.dataparser.IDataParser;
import com.jupiter.snifferframework.dataparser.UnknownProtoParser;

import java.util.ArrayList;

/**
 * Created by wangqiang on 16/7/5.
 * 协议处理器,所有协议的处理器全部组合到这里,当新的数据到来时从
 * 其中找到可以处理数据的处理器去处理数据
 */

public class ProtocolParsers implements IDataParser {

    public ProtocolParsers() {
        //1.常见协议的处理器添加到这里
        addHandler(new HttpParser());
        addHandler(new DnsParser());
        //TODO 这里可以添加其他各种协议数据的解析器

        //2.加载用户自定义的协议处理器
        for (IDataParser h : Sniffer.get().getDataParser()) {
            addHandler(h);
        }
    }

    public void addHandler(IDataParser h) {
        synchronized (mParsers) {
            if (mParsers.contains(h)) return;
            mParsers.add(h);
        }
    }

    public void removeHandler(IDataParser h) {
        synchronized (mParsers) {
            if (mParsers.contains(h)) {
                mParsers.remove(h);
            }
        }
    }

    @Override
    public IDataParser canHandle(Packet packet) {
        for (IDataParser h : mParsers) {
            if (h.canHandle(packet) != null) {
                mParser = h;
                return mParser;
            }
        }
        //找不到能处理当前包的处理器就用默认的
        mParser = new UnknownProtoParser();
        return mParser;
    }

    @Override
    public AbsData parse(boolean output, Packet data) {
        if (mParser != null) {
            return mParser.parse(output, data);
        }
        return null;
    }

    @Override
    public void onStreamCreate() {
        if (mParser != null) {
            mParser.onStreamCreate();
        }
    }

    @Override
    public AbsData onStreamClosing() {
        if (mParser != null) {
            return mParser.onStreamClosing();
        }
        return null;
    }

    @Override
    public AbsData onOutputData(Packet data) {
        if (mParser != null) {
            return mParser.onOutputData(data);
        }
        return null;
    }

    @Override
    public AbsData onInputData(Packet data) {
        if (mParser != null) {
            return mParser.onInputData(data);
        }
        return null;
    }

    public void clear() {
        mParsers.clear();
        mParser = null;
    }

    private ArrayList<IDataParser> mParsers = new ArrayList<>();

    private IDataParser mParser;
}
