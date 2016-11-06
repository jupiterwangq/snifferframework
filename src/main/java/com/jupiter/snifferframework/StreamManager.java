package com.jupiter.snifferframework;

import android.text.TextUtils;
import android.util.Log;
import com.jupiter.snifferframework.data.AbsData;
import java.util.HashMap;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by wangqiang on 16/6/9.
 * 用来管理TCP/UDP数据流
 */
public class StreamManager {

    public static final String Tag = "StreamManager";

    /**
     * 数据流,由四元组来决定
     */
    public static class Stream {
        public String sourceIP;
        public String destIP;
        public int sourcePort;
        public int destPort;

        public static Stream fromPacket(Packet pkt) {
            Stream stream = new Stream();
            stream.sourceIP = pkt.srcIP;
            stream.destIP   = pkt.destIP;
            stream.sourcePort = pkt.srcPort;
            stream.destPort   = pkt.destPort;
            stream.mHandler.canHandle(pkt);
            return stream;
        }

        /**
         * 数据流建立
         */
        public void onStreamCreate( ) {
            mHandler.onStreamCreate();
        }

        /**
         * 数据流关闭
         * @return
         */
        public AbsData onStreamClosing() {
            AbsData data = mHandler.onStreamClosing();
            mHandler.clear();
            return data;
        }

        /**
         * 新的请求数据
         * @param data
         */
        public AbsData onOutputData(Packet data) {
            return mHandler.onOutputData(data);
        }

        /**
         * 新的响应数据
         * @param data
         */
        public AbsData onInputData(Packet data) {
            return mHandler.onInputData(data);
        }

        /**
         * 解析协议数据并返回解析好的数据
         * @param output 是否是请求
         * @return 解析好的数据(如果是异步处理,返回null)
         */
        protected AbsData parse(boolean output, Packet data) {
            return mHandler.parse(output, data);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            return sb.append(sourceIP).append(",").append(sourcePort)
                     .append(",").append(destIP).append(",").append(destPort)
                     .toString();
        }

        @Override
        public boolean equals(Object stream) {
            if (stream == null) return false;
            if (stream instanceof String) return toString().equals(stream);
            if (stream instanceof Stream) return toString().equals(stream.toString());
            return false;
        }

        protected ProtocolParsers mHandler = new ProtocolParsers();
    }

    public static StreamManager get() {
        return sInstance;
    }

    public void clear() {
        mStreamList.clear();
    }

    //添加数据流
    public Stream addStream(Packet packet) {
        String tuple4 = packet.tuple4();
        Log.e(Tag, "Add stream:" + tuple4);
        synchronized (mStreamList) {
            Stream stream = mStreamList.get(tuple4);
            if (stream == null) {
                stream = Stream.fromPacket(packet);
                mStreamList.put(tuple4, stream);
                stream.onStreamCreate();
                return stream;
            }
            return stream;
        }
    }

    //移除数据流
    public AbsData removeStream(String tuple) {
        if (TextUtils.isEmpty(tuple)) return null;
        synchronized (mStreamList) {
            Stream stream = mStreamList.get(tuple);
            if (stream != null) {
                AbsData ret = stream.onStreamClosing();
                mStreamList.remove(tuple);
                return ret;
            }
        }
        return null;
    }

    /**
     * 将数据分发到合适的流里(没有就新建一个流)
     * @param output 是否是请求
     * @param data 新收到的数据包
     * @return 组装好的数据
     */
    public AbsData dispatchData(boolean output, Packet data) {
        String tuple4 = data.tuple4();
        Log.e(Tag, "dispatch packet" + data.toString());
        if (TextUtils.isEmpty(tuple4) || data == null) return null;

        Stream stream = null;
        synchronized (mStreamList) {
            stream = mStreamList.get(tuple4);
        }
        if (stream != null) {
            if (output) {
                return stream.onOutputData(data);
            } else {
                return stream.onInputData(data);
            }
        }
        stream = addStream(data);
        if (output) {
            return stream.onOutputData(data);
        } else {
            return stream.onInputData(data);
        }
    }

    /**
     * 分发udp数据
     * @param data udp数据包
     * @return 解析好的数据
     */
    public AbsData dispatchData(Packet data) {
        Log.e(Tag, "dispatch udp, data.type" + data.type);
        if (data == null || data.type != Packet.TYPE_UDP) return null;

        Stream stream = Stream.fromPacket(data);
        if (stream != null) {
            stream.onStreamCreate();
            AbsData ret = stream.parse(false, data);
            stream.onStreamClosing();
            return ret;
        }
        return null;
    }

    /**
     * @param event
     * @param data
     */
    void onNewPacket(int event, final Packet data) {
        final String tuple4 = data.tuple4();
        Observable.just(event)
            .map(new Func1<Integer, Object[]>() {
                @Override
                public Object[] call(Integer ev) {
                    //Get address tuple and payload data from raw data
                    switch( ev ) {
                        case Event.ENV_NEW_TCP_CONNECTION:
                            Log.e(Tag, "onNewPacket:new tcp connection event");
                            addStream(data);
                            return new Object[]{ev, tuple4};
                        case Event.ENV_TCP_CONNECTION_CLOSE:
                        case Event.ENV_TCP_CONNECTION_CLOSED_BY_RST:
                            Log.e(Tag, "onNewPacket:tcp connection closing evet");
                            return new Object[]{ev, removeStream(data.tuple4())};
                        case Event.ENV_NEW_DATA_TO_SERVER:
                            Log.e(Tag, "onNewPacket:-->request event");
                            Object r1 = dispatchData(true, data);
                            return new Object[]{r1};
                        case Event.ENV_NEW_DATA_FROM_SERVER:
                            Log.e(Tag, "onNewPacket:<--response event");
                            Object r2 = dispatchData(false, data);
                            return new Object[]{r2};
                        case Event.ENV_NEW_UDP_DATA:
                            Log.e(Tag, "new udp packet,dest port " + data.destPort);
                            return new Object[]{dispatchData(data)};
                        default:
                            return null;
                    }
                }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Object[]>() {
                @Override
                public void call(Object[] ar) {
                    if (ar != null ) {
                        //TODO Now give the final data to application
                        //...
                        handleResult(ar);
                    } else {
                        Log.e(Tag, "Data not complete yet");
                    }
                }
            });
    }

    private void handleResult(Object ar[]) {
        if (ar == null) return;
        if (ar.length == 2 && (ar[0] instanceof Integer)) {
            switch((Integer)ar[0]) {
                //TODO Notify these event to UI
                case Event.ENV_NEW_TCP_CONNECTION:
                    break;
                case Event.ENV_TCP_CONNECTION_CLOSE:
                case Event.ENV_TCP_CONNECTION_CLOSED_BY_RST:
                    if (ar[1] instanceof AbsData) {
                        AbsData data = (AbsData)ar[1];
                        if (data.isDataComplete() && !data.isNotified()) {
                            data.setNotified();
                            Log.e(Tag, "Data is ok:" + data.toString());
                            Sniffer.get().notifyPacket(data);
                        }
                    }
                    break;
                default:
            }
        } else if (ar.length == 1 && (ar[0] instanceof AbsData)) {
            AbsData r = (AbsData)ar[0];
            if (r.isDataComplete() && !r.isNotified()) {
                r.setNotified();
                Log.e(Tag, "Data is ok:" + r.toString());
                Sniffer.get().notifyPacket(r);
            } else {
                Log.e(Tag, "Data is not complete yet.");
            }
        } else {
            Log.e(Tag, "Unknown result data...");
        }
    }

    private static StreamManager sInstance = new StreamManager();

    private HashMap<String,Stream> mStreamList = new HashMap<>();
}
