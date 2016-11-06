package com.jupiter.snifferframework.dataparser;

import android.util.Log;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.data.http.HttpDecodeStateMachine;
import com.jupiter.snifferframework.protocol.Protocol;

/**
 * Created by wangqiang on 16/6/10.
 * HTTP协议处理器,组装HTTP数据,接收到的数据全交给状态机处理
 */

public class HttpParser implements IDataParser{

    final String Tag = "HttpHandler";

    private HttpDecodeStateMachine mReqDecoder =
            new HttpDecodeStateMachine(HttpDecodeStateMachine.TYPE_REQUEST);
    private HttpDecodeStateMachine mRspDecoder =
            new HttpDecodeStateMachine(HttpDecodeStateMachine.TYPE_RESPONSE);

    @Override
    public void onStreamCreate() {
        Log.e(Tag, "New http stream created:" + toString());
    }

    @Override
    public AbsData onStreamClosing() {
        Log.e(Tag, "Http stream closing:" + toString());
        mRspDecoder.sendMessage(HttpDecodeStateMachine.MSG_STREAM_CLOSE, null);
        return null;
    }

    @Override
    public AbsData onOutputData(Packet data) {
        Log.e(Tag, "output data on http stream:" + toString());
        mReqDecoder.sendMessage(HttpDecodeStateMachine.MSG_NEW_DATA, data);
        return null;
    }

    @Override
    public AbsData onInputData(Packet data) {
        Log.e(Tag, "Input data on http stream:" + toString());
        mRspDecoder.sendMessage(HttpDecodeStateMachine.MSG_NEW_DATA, data);
        return null;
    }

    @Override
    public IDataParser canHandle(Packet packet) {
        if (packet.destPort == Protocol.HTTP ||
            packet.srcPort == Protocol.HTTP  ||
            packet.srcPort == Protocol.HTTP_PROXY ||
            packet.destPort == Protocol.HTTP_PROXY) return this;
        return null;
    }

    @Override
    public AbsData parse( boolean output, Packet data) {
        Log.e(Tag, "Parse http data:");
        return null;
    }
}
