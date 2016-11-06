package com.jupiter.snifferframework.dataparser;

import com.jupiter.snifferframework.Packet;
import com.jupiter.snifferframework.data.AbsData;

/**
 * Created by wangqiang on 16/7/5.
 * 协议处理器接口
 */

public interface IDataParser {

    /**
     * 是否可以处理参数指定的数据包
     * @param packet 新接收到的数据包
     * @return 如果可以处理此包则返回自己,否则返回空
     */
    IDataParser canHandle(Packet packet);

    /**
     * 解析数据包(同步)
     * 如果想异步解析,可以返回null,并且调用#Sniffer.notifyPacketInSubThread将解析好的数据通知到app
     * @param output 是否是请求,true表示是请求,false表示响应
     * @return 解析好以后的协议数据
     */
    AbsData parse(boolean output, Packet data);

    /**
     * 新的数据流建立
     */
    void onStreamCreate();

    /**
     *数据流关闭
     * @return
     */
    AbsData onStreamClosing();

    /**
     * 接受到新的请求数据(可能只是请求的一部分)
     * @param data
     */
    AbsData onOutputData(Packet data);

    /**
     * 接收到新的响应数据(可能只是响应的一部分)
     * @param data
     */
    AbsData onInputData(Packet data);

}
