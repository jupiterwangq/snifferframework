package com.jupiter.snifferframework.data.http;

import android.os.Message;
import android.util.Log;
import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.Sniffer;
import com.jupiter.snifferframework.util.Util;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.statemachine.State;
import com.jupiter.snifferframework.statemachine.StateMachine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.jupiter.snifferframework.util.ByteArrayHelper;

/**
 * Created by wangqiang on 16/7/31.
 * 解码Http数据的状态机,由于libnids库重组后的数据可能是分批次到来,
 * 这里用个状态机,不同状态下有数据到来时的处理方式有所不同
 */

public class HttpDecodeStateMachine extends StateMachine {

    public static final int TYPE_REQUEST  = 0;
    public static final int TYPE_RESPONSE = 1;

    public static final String TAG = "HttpDecodeStateMachine";

    public static final int MSG_NEW_DATA = 1;
    public static final int MSG_STREAM_CLOSE = 2;

    public static final String HEADER_END_FLAG = "\r\n\r\n";
    public static final String HTTP_VER_FLAG   = "HTTP/";

    private int mType = -1;

    //数据
    AbsHttpData mData;

    //初始化各种状态
    private InitState S_INIT = new InitState();
    private WaitHeadInfoState S_WAIT_HEADER = new WaitHeadInfoState();
    private WaitAllTrunkDataEndState S_WAIT_ALL_TRUNK = new WaitAllTrunkDataEndState();
    private WaitOneTrunkData S_WAIT_ONE_TRUNK = new WaitOneTrunkData();
    private WaitDataState S_WAIT_DATA = new WaitDataState();
    private DataCompleteState S_COMPLETE = new DataCompleteState();

    public HttpDecodeStateMachine(int type) {
        super("HttpStateMachine");
        mType = type;
        setInitialState(S_INIT);
    }

    public AbsData getData() {
        return mData;
    }

    //接收的数据中是否包含了头部数据(碰到\r\n\r\n表明起始行和头部均已OK)
    private boolean hasHeaders( byte[] data ) {
        if (data == null || data.length == 0 ) return false;
        int headerEnd = ByteArrayHelper.indexOf(data, HEADER_END_FLAG.getBytes(), 0);
        int ver = ByteArrayHelper.indexOf(data, HTTP_VER_FLAG.getBytes(), 0);
        if (headerEnd >= 0 && ver >= 0) {
            return true;
        }
        return false;
    }

    /**
     * 初始状态,等待数据到来
     */
    class InitState extends State {

        @Override
        public void enter() {
            Log.e(TAG, "enter InitState");
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case MSG_NEW_DATA:
                    if (msg.obj == null) return true;
                    check((Packet)msg.obj);
                    break;
            }
            return true;
        }

        @Override
        public String getName() {
            return "InitState";
        }

        private void setInfo(Packet pkt) {
            mData.seq = pkt.seq;
            mData.ack_seq = pkt.ackSeq;
            mData.acked   = pkt.acked;
            mData.window  = pkt.window;
            mData.srcIP   = pkt.srcIP;
            mData.destIP  = pkt.destIP;
            mData.srcPort = pkt.srcPort;
            mData.destPort= pkt.destPort;
            mData.setIsRequest(mType == TYPE_REQUEST);
            try {
                mData.addHeader("window", String.valueOf(mData.window));
                mData.addHeader("seq", Util.unsignedLong(mData.seq).toString());
                mData.addHeader("ack-seq", Util.unsignedLong(mData.ack_seq).toString());
                mData.addHeader("acked", Util.unsignedLong(mData.acked).toString());
            } catch (IOException e) {
            }
        }

        /**
         * 检查数据并确定一下个状态
         * @param pkt
         */
        private void check(Packet pkt) {
            byte[] data = pkt.data;
            if (data == null || data.length == 0) return;

            int ver = ByteArrayHelper.indexOf(data, HTTP_VER_FLAG.getBytes(), 0);
            int crlf = ByteArrayHelper.indexOf(data, "\r\n".getBytes());
            if (ver >= 0 && crlf > ver) {
                //有http头
                Log.e(TAG, "maybe a header");
                if (mType == TYPE_REQUEST) {
                    mData = new HttpRequest();
                    setInfo(pkt);
                } else if (mType == TYPE_RESPONSE) {
                    mData = new HttpResponse();
                    setInfo(pkt);
                }
                int headerEnd = ByteArrayHelper.indexOf(data, HEADER_END_FLAG.getBytes());
                if (headerEnd < 0) {
                    //数据中没有头部结束的标识,进入下一状态等待头部数据
                    Log.e(TAG, "wait header");
                    transitionTo(S_WAIT_HEADER);
                    Packet p = new Packet();
                    p.data = data;
                    sendMessage(MSG_NEW_DATA, p);
                    return;
                }
                //数据中包含了头部,填充起始行和头部信息
                byte[] head = ByteArrayHelper.copyOfRange(data, 0, headerEnd);
                String sHead = new String(head);
                Log.e(TAG, "Http head:" + sHead);
                mData.fillStartLineAndHeaders(sHead);   //fill head info
                if (mData.isTrunk()) {
                    //是chunked编码的数据,进入下一状态等待所有的chunk全部结束
                    transitionTo(S_WAIT_ALL_TRUNK);
                    byte[] trunkData = ByteArrayHelper.copyOfRange(data,
                            headerEnd + HEADER_END_FLAG.length(), data.length);
                    Log.e(TAG, "trunk data:" + new String(trunkData));
                    Packet p = new Packet();
                    p.data = trunkData;
                    Log.e(TAG, "Trunk data,transition state");
                    sendMessage(MSG_NEW_DATA, p);
                } else {
                    //其余情况下从头部Content-Length字段获得负载数据长度
                    int contentLen = mData.contentLength();
                    if (contentLen <= 0) {
                        //没有Content-Length字段,表明没有负载数据,此时数据已OK
                        Log.e(TAG, "No payload,data is complete.");
                        transitionTo(S_COMPLETE);
                    } else {
                        //等待负载数据全部接收完成
                        Log.e(TAG, "Wait " + contentLen + " bytes payload");
                        transitionTo(S_WAIT_DATA);
                        byte[] payload = ByteArrayHelper.copyOfRange(data,
                                headerEnd + HEADER_END_FLAG.length(), data.length);
                        Packet p = new Packet();
                        p.data = payload;
                        sendMessage(MSG_NEW_DATA, p);
                    }
                }
            } else {
                Log.e(TAG, "Ignore dirty data at " + getName());
            }
        }
    }

    /**
     * 等待HTTP头部数据
     */
    class WaitHeadInfoState extends State {

        private ByteArrayOutputStream mHeader = new ByteArrayOutputStream();

        @Override
        public void enter() {
            Log.e(TAG, "enter " + getName());
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what != MSG_NEW_DATA && msg.obj == null) return true;
            Packet p = (Packet)msg.obj;
            byte[] data = p.data;
            if (data == null || data.length == 0) return true;
            try {
                mHeader.write(data);
                byte[] current = mHeader.toByteArray();
                int headerEnd = ByteArrayHelper.indexOf(current, HEADER_END_FLAG.getBytes());
                if (headerEnd >= 0) {
                    //找到了头部结束标记,填充起始行和头部数据
                    byte[] header = ByteArrayHelper.copyOfRange(current, 0, headerEnd);
                    mData.fillStartLineAndHeaders(new String(header));
                    mHeader.reset();
                    //提取负载数据并将数据交给下一状态
                    byte[] extra = ByteArrayHelper.copyOfRange(current,
                            headerEnd + HEADER_END_FLAG.getBytes().length, current.length);
                    if (mData.isTrunk()) {
                        //头部有TE,等待所有chunk全部接收完毕
                        transitionTo(S_WAIT_ALL_TRUNK);
                        Packet pp = new Packet();
                        pp.data = extra;
                        Log.e(TAG, "Trunk data,transition state");
                        sendMessage(MSG_NEW_DATA, pp);
                    } else {
                        int contentLen = mData.contentLength();
                        if (contentLen <= 0) {
                            //没有Content-Length,数据结束
                            Log.e(TAG, "No payload,data is complete.");
                            transitionTo(S_COMPLETE);
                        } else {
                            //等待所有负载数据接收完毕
                            Log.e(TAG, "Wait " + contentLen + " bytes payload");
                            transitionTo(S_WAIT_DATA);
                            Packet pp = new Packet();
                            pp.data = extra;
                            sendMessage(MSG_NEW_DATA, pp);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "write header info error");
            }
            return true;
        }

        @Override
        public String getName() {
            return "WaitHeadInfoState";
        }
    }

    /**
     * 等待所有chunk全部接收OK,根据HTTP协议,最后一个chunk以0结束
     */
    class WaitAllTrunkDataEndState extends State {

        /**
         * 从数据中提取chunk的长度
         * @param data
         * @return
         */
        private int traitTrunkLength(byte[] data) {
            if (data == null || data.length == 0) return -1;
            int chunkLenE = ByteArrayHelper.indexOf(data, "\r\n".getBytes());
            if (chunkLenE < 0) return -1;
            if (chunkLenE == 0) {
                //ignore the "\r\n" at start
                data = ByteArrayHelper.copyOfRange(data, "\r\n".length(), data.length);
                chunkLenE = ByteArrayHelper.indexOf(data, "\r\n".getBytes(), 0);
                if (chunkLenE < 0) return -1;
            }

            byte[] len = ByteArrayHelper.copyOfRange(data, 0, chunkLenE);
            Log.e(TAG, "trunk data,len=" + Util.toHexString(len, false) + ",str=" + new String(len));
            String sLen = new String(len);
            if (sLen.contains(" ")) {
                sLen = sLen.substring(0, sLen.indexOf(' '));
            }
            try {
                int iLen = Integer.valueOf(sLen, 16);
                return iLen;
            } catch( Exception e) {
                return -1;
            }
        }

        @Override
        public void enter() {
            Log.e(TAG, "enter " + getName());
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what == MSG_STREAM_CLOSE) {
                transitionTo(S_COMPLETE);
                return true;
            }
            if (msg.what != MSG_NEW_DATA && msg.obj == null) return true;
            Packet pkt = (Packet)msg.obj;
            byte[] data = pkt.data;
            if (data == null || data.length == 0) return true;
            if (hasHeaders(data)) {
                //在当前状态下,如果收到疑似http头部的数据,直接丢弃
                Log.e(TAG, "dirty data at " + getName());
                return true;
            }

            int trunkLen = traitTrunkLength(data);
            if (trunkLen < 0) {
                Log.e(TAG, "could not get trunk len");
                transitionTo(S_INIT);
                return true;
            }
            if (trunkLen == 0) {
                //最后一个chunk,数据OK
                Log.e(TAG, "Last trunk got");
                transitionTo(S_COMPLETE);
                return true;
            }

            int idx = ByteArrayHelper.indexOf(data, "\r\n".getBytes());
            int s = idx + "\r\n".getBytes().length;
            int e = s + trunkLen;
            if (e < data.length) {
                //数据中已经包含了一个chunk的所有数据,并且还包含了下一chunk的数据
                mData.appendData(ByteArrayHelper.copyOfRange(data, s, e));
                byte[] extra = ByteArrayHelper.copyOfRange(data, e, data.length);
                Packet pp = new Packet();
                pp.data = extra;
                sendMessage(MSG_NEW_DATA, pp);
            } else if (e == data.length) {
                //数据中包含一个chunk的数据
                mData.appendData(ByteArrayHelper.copyOfRange(data, s, e));
            } else if ( e > data.length) {
                //数据中只包含了一个chunk的部分数据,进入下一状态等待该不完整的chunk结束
                S_WAIT_ONE_TRUNK.mTrunkLen = trunkLen;
                transitionTo(S_WAIT_ONE_TRUNK);
                Packet pp = new Packet();
                pp.data = ByteArrayHelper.copyOfRange(data, s, data.length);
                sendMessage(MSG_NEW_DATA, pp);
            }

            return true;
        }

        @Override
        public String getName() {
            return "WaitTrunkDataState";
        }
    }

    /**
     * 等待单个chunk数据
     */
    class WaitOneTrunkData extends State {

        public int mTrunkLen, mReceived = 0;

        @Override
        public void enter() {
            Log.e(TAG, "enter " + getName());
            mReceived = 0;
        }

        @Override
        public void exit() {
            Log.e(TAG, "leave " + getName());
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what == MSG_STREAM_CLOSE) {
                transitionTo(S_COMPLETE);
                return true;
            }
            if (msg.what != MSG_NEW_DATA && msg.obj == null ) return true;
            Packet p = (Packet)msg.obj;
            byte[] data = p.data;
            if (data == null || data.length == 0) return true;

            if ( mReceived + data.length < mTrunkLen) {
                //chunk的部分数据
                mData.appendData(data);
                mReceived += data.length;
            } else if (mReceived + data.length == mTrunkLen) {
                //chunk刚好结束
                mData.appendData(data);
                transitionTo(S_WAIT_ALL_TRUNK);
            } else if (mReceived + data.length > mTrunkLen) {
                //包含其他chunk的数据
                mData.appendData(ByteArrayHelper.copyOfRange(data, 0, mTrunkLen - mReceived));
                transitionTo(S_WAIT_ALL_TRUNK);
                byte[] d = ByteArrayHelper.copyOfRange(data, mTrunkLen - mReceived, data.length);
                Packet pp = new Packet();
                pp.data = d;
                sendMessage(MSG_NEW_DATA, pp);
            }
            return true;
        }
    }

    /**
     * 等待负载数据
     */
    class WaitDataState extends State {
        @Override
        public void enter() {
            Log.e(TAG, "enter " + getName());
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what != MSG_NEW_DATA && msg.obj == null) return true;
            Packet pkt = (Packet)msg.obj;
            byte[] data = pkt.data;
            if (data == null || data.length == 0) return true;
            if (hasHeaders(data)) {
                Log.e(TAG, "dirty data found.");
                return true;
            }

            mContentLength = mData.contentLength();

            if (mReceived + data.length < mContentLength) {
                mData.appendData(data);
                mReceived += data.length;
                Log.e(TAG, "data not complete,received=" + mReceived);
                return true;
            } else if (mReceived + data.length == mContentLength) {
                Log.e(TAG, "data is complete,data len=" + data.length);
                mData.appendData(data);
                transitionTo(S_COMPLETE);
                return true;
            } else if (mReceived + data.length > mContentLength) {
                Log.e(TAG, "extra data, received=" + mReceived + ",content len:" + mContentLength
                );
                byte[] b = ByteArrayHelper.copyOfRange(data, 0, mContentLength - mReceived);
                mData.appendData(b);
                transitionTo(S_COMPLETE);
                return true;
            }
            return true;
        }

        @Override
        public String getName() {
            return "WaitDataState";
        }

        @Override
        public void exit() {
            mContentLength = 0;
            mReceived = 0;
        }

        private int mReceived = 0;
        private int mContentLength;
    }

    /**
     * 数据结束了
     */
    class DataCompleteState extends State {

        @Override
        public void enter() {
            Log.e(TAG, "enter " + getName());
            mData.setState(AbsData.STATE_DATA_COMPLETE);
            Log.e(TAG, "notify,payload size=" + mData.getPayLoad().length);
            Sniffer.get().notifyPacketInSubThread(mData);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what != MSG_NEW_DATA) return true;
            transitionTo(S_INIT);
            sendMessage(MSG_NEW_DATA, msg.obj);
            return true;
        }

        @Override
        public String getName() {
            return "DataCompleteState";
        }
    }
}
