package com.jupiter.snifferframework;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.jupiter.snifferframework.data.AbsData;
import com.jupiter.snifferframework.data.EventData;
import com.jupiter.snifferframework.dataparser.IDataParser;
import com.jupiter.snifferframework.eventhandler.EventHandler;
import com.jupiter.snifferframework.protocol.Protocol;
import com.jupiter.snifferframework.protocol.ProtocolConfigParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by wangqiang on 16/6/9.
 * Sniffer,将组装好的数据通知到app,同时接收native侧传递的数据包
 */

public class Sniffer {

    public static final String Tag = "Sniffer";

    //守护进程pid
    public int mNativeSnifferPid = -1;

    static {
        System.loadLibrary("pcap");
    }

    public static final int MSG_NEW_PACKET = 0;
    public static final int MSG_EVENT = 1;

    public static final int SNIFFER_EVENT = -1000;  //app内部使用的特殊协议,用于通知底层事件用

    //sniffer的初始化参数
    public static class Params {
        /**
         * 过滤表达式
         * @return
         */
        public String getPcapFilter() {
            return "";
        }

        /**
         * 是否使能校验和检查
         * @return
         */
        public boolean enableChecksum() {
            return false;
        }

        /**
         * 守护进程的位置
         * @return
         */
        public String daemonPath() {
            return "";
        }

        /**
         *
         */
    }


    /**
     * 获取协议的名称
     * @param proto 协议端口号
     * @return 对应的名称
     */
    public static String getProtoName(int proto, int transport) {
        return ProtocolConfigParser.getInstance().getProtoName(proto, transport);
    }

    public static Sniffer get() {
        return sInstance;
    }

    /**
     * 解析协议信息
     * @param config 协议配置文件
     */
    public void parseProtocols(InputStream config) {
        ProtocolConfigParser.getInstance().parse(config);
    }

    /**
     * 注册协议数据的监听器
     * @param proto 协议的端口号
     * @param h 协议数据监听器
     */
    public void registerHandler(int proto, Subscriber<? extends AbsData> h) {
        synchronized (mDataListeners) {
            if (mDataListeners.containsKey(proto)) {
                List<Subscriber<? extends AbsData>> handlers = mDataListeners.get(proto);
                if (handlers == null) {
                    ArrayList<Subscriber<? extends AbsData>> hl = new ArrayList<>();
                    hl.add(h);
                    mDataListeners.put(proto, hl);
                } else {
                    if (handlers.contains(h)) return;
                    handlers.add(h);
                }
            } else {
                ArrayList<Subscriber<? extends AbsData>> hl = new ArrayList<>();
                hl.add(h);
                mDataListeners.put(proto, hl);
            }
        }
    }

    /**
     * 注销协议数据监听器
     * @param h
     */
    public void unregisterHandler(Subscriber<? extends AbsData> h) {
        synchronized (mDataListeners) {
            for (List<Subscriber<? extends AbsData>> handlers : mDataListeners.values()) {
                if ( handlers.contains(h)) {
                    handlers.remove(h);
                }
            }
        }
    }

    /**
     * 将组合好的数据通知给app(主线程中调用)
     * @param data
     */
    public void notifyPacket(AbsData data) {
        List<Subscriber<? extends AbsData>> H = mDataListeners.get(data.getProto());
        List<Subscriber<? extends AbsData>> ALL = mDataListeners.get(Protocol.ALL);
        Observable observable = Observable.just(data);
        if (H != null) {
            for (Subscriber<? extends AbsData> subscriber : H) {
                observable.subscribe(subscriber);
            }
        }
        if (ALL != null) {
            for (Subscriber<? extends AbsData> subscriber : ALL) {
                observable.subscribe(subscriber);
            }
        }
    }

    /**
     * 将组合好的数据通知给app(子线程中调用)
     * @param pkt
     */
    public void notifyPacketInSubThread(AbsData pkt) {
        Message msg = Message.obtain(mHandler, MSG_NEW_PACKET, pkt);
        msg.sendToTarget();
    }

    /**
     * 注册底层事件的观察者
     * @param l
     */
    public void registerEventListener(Subscriber<? extends AbsData> l) {
        if (mEventListener.contains(l)) return;
        mEventListener.add(l);
    }

    public void unregisterEventListener(Subscriber<? extends AbsData> l) {
        mEventListener.remove(l);
    }

    /**
     * 用指定的选项初始化嗅探器,异步操作
     * @param context
     * @param p 启动sniffer守护进程的选项参数
     */
    public Observable<Integer> init(Context context, Params p) {
        return startSniffer(context, p);
    }

    /**
     * 停止sniffer守护进程,调用此函数会强制杀死sniffer守护进程
     */
    public void stopSniffer(final int pid, boolean async) {
        if (pid > 0) {
            Observable<String[]> observable = Observable.just(new String[] {"kill -9 " + pid});
            if (async) {
                observable.observeOn(Schedulers.newThread());
            }
            observable.map(new Func1<String[], Integer>() {

                @Override
                public Integer call(String[] cmds) {
                    return CmdLineExecuter.exec(cmds, true);
                }
            }).subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer result) {
                    Log.e(Tag, "Stop native sniffer " + pid + " finished with:" + result);
                }
            });
        }
    }

    /**
     * 停止sniffer守护进程,调用此函数会强制杀死sniffer守护进程.
     */
    public void stopSniffer() {
        if (mNativeSnifferPid > 0) {
            stopSniffer(mNativeSnifferPid, false);
        }
    }

    /**
     * 添加一个自定义的协议处理器用来解析指定协议的数据
     * @param h
     */
    public void addParser(IDataParser h) {
        if (!mCustomDataParser.contains(h)) {
            mCustomDataParser.add(h);
        }
    }

    /**
     * 移除一个自定义的协议处理器
     * @param h
     */
    public void removeParser(IDataParser h) {
        if (mCustomDataParser.contains(h)) {
            mCustomDataParser.remove(h);
        }
    }

    public List<IDataParser> getDataParser() {
        return mCustomDataParser;
    }

    /**
     * 此函数有native层调用,用来通知新的数据包
     * @param event 事件
     * @param pkt 数据包
     */
    public void onNewPacket(int event, Packet pkt) {
        if (pkt == null) return;
        if (mEventHandler.handleEvent(event, pkt)) {
            return;
        }
        //协议数据,直接交给StreamManager去处理(同步函数)
        mStreamManager.onNewPacket(event, pkt);
        //数据组装好了,可以把pkt置空会好点
        pkt.recycle();
        pkt = null;
    }

    private native int nativeSniff(String daemonPath);

    private native void nativeStop();

    private static Sniffer sInstance = new Sniffer();
    private StreamManager mStreamManager = StreamManager.get();

    private String daemonDir(Params p) {
        String daemonPath = p.daemonPath();
        int idx = daemonPath.lastIndexOf("/");
        return daemonPath.substring(0, idx + 1);
    }

    private Observable<Integer> startSniffer(Context context, Params p) {
        //1.启动native sniffer
        nativeSniff(daemonDir(p));

        //2.将流全部清除
        mStreamManager.clear();

        //3.以root权限启动daemon进程
        String daemonPath = p.daemonPath();
        if (TextUtils.isEmpty(daemonPath)) {
            return Observable.just(-1);
        }
        return Observable.just(new String[]{"chmod 4777 " + daemonPath,
                "chown root " + daemonPath,
                daemonPath + makeParams(p)})
                .observeOn(Schedulers.newThread())
                .map(new Func1<String[], Integer>() {
                    @Override
                    public Integer call(String[] cmds) {
                        return CmdLineExecuter.exec(cmds, true);
                    }
                });
    }

    private String makeParams(Params p) {
        if (p == null) return null;
        StringBuilder sb = new StringBuilder("");
        if (!TextUtils.isEmpty(p.getPcapFilter())) {
            sb.append(" -p ").append(p.getPcapFilter()).append(" pend");
        }
        if (p.enableChecksum()) {
            sb.append(" -checksum true");
        }
        sb.append( " -path ").append(daemonDir(p));
        //TODO 后续有其他启动参数添加到这里
        return sb.toString();
    }

    private void notifyEvent(EventData event) {
        Observable observable = Observable.just(event);
        for (Subscriber<? extends AbsData> l : mEventListener) {
            observable.subscribe(l);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_NEW_PACKET:
                    AbsData data = (AbsData)msg.obj;
                    notifyPacket(data);
                    break;
                case MSG_EVENT:
                    EventData event = (EventData)msg.obj;
                    notifyEvent(event);
                default:
            }
        }
    };

    private EventHandler mEventHandler = new EventHandler(mHandler);

    /**
     * 用来监听协议数据的观察者
     */
    private HashMap<Integer, ArrayList<Subscriber<? extends AbsData>>> mDataListeners = new HashMap<>();

    /**
     * 用来监听底层事件的观察者
     */
    private ArrayList<Subscriber<? extends AbsData>> mEventListener = new ArrayList<>();

    /**
     * 用户自定义的数据解析器
     */
    private ArrayList<IDataParser> mCustomDataParser = new ArrayList<>();
}
