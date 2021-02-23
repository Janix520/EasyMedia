
# EasyMedia

#### 介绍
Springboot、netty实现的http-flv、websocket-flv流媒体服务（可用于直播点播），支持rtsp、h264、h265等、rtmp等多种源，h5纯js播放（不依赖flash），不需要依赖nginx等第三方，自身提供推流服务，测试延迟在2-3秒左右。

#### 更新说明 2021-02-20
- 移除原有spring websocket，采用高性能的netty作为http、ws服务。
- 完善关闭流逻辑，没人看时会自动断开。
- 由于替换netty，暂时移除本地文件支持。

**成品下载**
链接：https://pan.baidu.com/s/1mIy0rfjhorr98p3Oa_3X-A 
提取码：0nli 
复制这段内容后打开百度网盘手机App，操作更方便哦--来自百度网盘超级会员V4的分享

#### 软件架构
- 通过javacv推拉流存到内存里，直接输出到前端播放
- 后端：springboot、netty，集成websocket
- 前端：html5（后面打算做简单的管理页面）
- 播放器：西瓜播放器 http://h5player.bytedance.com/ （字节跳动家的，不介绍了，抖音视频、西瓜视频都杠杠的，当然只要支持flv的播放器都可以）
- 媒体框架：javacv、ffmpeg

#### 截图
![Image text](https://img-blog.csdnimg.cn/img_convert/e8944fb7e61fbead2e773edfd6beeaf6.png)


#### 源码教程

1.  环境：java8+
2.  标准的maven项目，sts、eclipse或者idea导入，直接运行main方法

#### 使用说明

- 流媒体服务会绑定两个端口，分别为 8866（媒体端口）、8888（web端口，后续会做简单的管理页面）
- 您只需要将{rtsp}替换成您的，然后放播放器里就能看了


`http://localhost:8866/live?url={rtsp}`


`ws://localhost:8866/live?url={rtsp}`


 **例如：**


`http://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102`


`ws://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102`


- 成品运行方式（由于是跨平台的，未做ffmpeg精简，所以包会比较大）


` java -jar EasyMedia-0.0.1-SNAPSHOT.jar`

#### 为什么要写个这个
现在flash已经被抛弃，h5播放的时代，网上实现大多不是特别完整的（比如拿到一个rtsp或者rtmp，也不知道怎么在h5页面直接播放），当然现在直播点播有很多方式，可以通过nginx带flv模块的当rtmp服务、还有srs等流媒体服务，而这里我们通过javacv来处理，事实上javacv在性能足够，底层ffmpeg也是通过c实现，使用java调用c跟使用c++去调用c差不了多少毫秒延迟


#### 后续计划
- 后续会先完善服务端rtsp重连机制
- 实现简单的web管理页面，方便管理点播文件和播放列表等
- 增加录制功能
- 由于hls(m3u8)兼容性最好，水果、安卓和PC通吃，所以后续会加入m3u8切片方式
- 原本还写了个通过ffmpeg子进程推流，然后用socket服务接收的方案，等javacv版搞完善了再弄。

