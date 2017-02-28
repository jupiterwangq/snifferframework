SnifferAndroid
=================

# 简介
SnifferAndroid是安卓平台上的手机抓包sdk,底层基于libnids实现。有抓包需求的应用可以方便的接入该sdk。特别提醒：使用此sdk需要root权限。使用此SDK的抓包app例子可参考[安卓抓包](http://sj.qq.com/myapp/detail.htm?apkName=com.jupiter.sniffer)
# 功能
* 支持HTTP
* 支持DNS
* 网络入侵检测
* 支持pcap过滤表达式
* 可方便扩展以支持其他协议


# 应用接入方法
    1.首先将编译出的pcapd可执行程序放入到应用程序某个路径下，比如libs/pcapd；
    
    2.定义自己需要传递给框架的参数：
```Java
Sniffer.Params mParam = new Sniffer.Params() {
  //返回你的pcap过滤表达式，不需要过滤直接返回空
  public String getPcapFilter() {
     return "";
  }
  //是否使能校验和检查，一般返回false即可
  public boolean enableChecksum() {
     return false;
  }
  //返回守护进程pcapd的存放路径
  public String daemonPath() {
    //
  }
}
```
    3.监听sniffer-framework推送的底层事件：

 ```Java
 Sniffer.get().registerEventListener(new Subscriber<EventData>() {
  @Override
  public void onNext(EventData eventData) {
    if (eventData.event == Event.ENV_PID_EVENT) {
      //记录下pcapd的pid
    } 
    ...
  }
  ```
  
由于pcapd可执行文件是一个独立进程，所以通常应用很有必要监听Event.ENV_PID_EVENT事件以便在合适的时候停止该进程，停止进程可以调用
```Java
Sniffer.stopSniffer(pid, true)
```
其中第一个参数是pcapd进程的pid,第二个参数决定是否异步。

    4.在应用的任意地方（建议在Application中）调用如下初始化接口：
```Java
Sniffer.get().init(context, mParam);
```
该接口返回一个rxjava的Observable<Integer>，完整代码参考如下。
```Java
Sniffer.get().init(SnifferApp.this, mParam)
       .observeOn(AndroidSchedulers.mainThread())
       .subscribe(new Action1<Integer>() {
           @Override
           public void call(Integer resultCode) {
              if (resultCode == 0) {
                //TODO 框架初始化成功
              } else {
                //TODO 框架初始化失败
              }
           }
        });
```
        
    5.最后注册监听需要处理的协议：
    
```Java
  Sniffer.get().registerHandler(mProto, new Subscriber<AbsData>() {
      @Override
      public void onNext(AbsData absData) {
        //absData就是抓包的数据包了
      }
    });
```
  第一个参数是协议端口号，比如http为80,第二个参数是抓包后的回调。
  
  注意：sniffer-framework目前只做了Http和DNS数据的解析还原。如果你想解析还原其他协议的数据，需要自行添加协议处理器：
```Java
  class MyProtoParser implements IDataParser {
      ....
  }
```
  并调用Sniffer.addParser将自己的协议处理器加入到框架中。

# 依赖
  框架实现中使用到了RxJava和RxAndroid，需要接入的app需要引入对Rx的依赖
  
