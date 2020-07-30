# EasyMedia

#### 介绍
Springboot实现的http-flv、websocket-flv直播点播，支持rtsp、本地文件、rtmp等多种源，h5纯js播放（不依赖flash）

#### 软件架构
* 通过javacv推拉流存到内存里，直接输出到前端播放，现在只是一个播放实现，没有完善关闭回收，还不适用于生产环境。
* 后端：springboot，集成websocket
* 前端：html5
* 播放器：西瓜播放器 http://h5player.bytedance.com/ （字节跳动家的，不介绍了，抖音视频、西瓜视频都杠杠的，当然只要支持flv的播放器都可以）
* 媒体框架：javacv

#### 截图
![Image text](https://gitee.com/52jian/EasyMedia/raw/master/snapshot/image1.png)


#### 安装教程

1.  环境：java8+
2.  标准的maven项目，sts、eclipse或者idea导入，直接运行main方法，或者直接命令打包运行

#### 使用说明

1.  运行后访问：http://localhost:8888/
2.  默认读取的是项目根目录下的video本地视频
3.  http接口：http://localhost:8888/test?url=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov&id=1
4.  ws接口：ws://localhost:8888/flv?url=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov&id=2
5.  url为输地址、可以是本地地址也可以是rtsp、rtmp等，id为视频的唯一编号


#### 为什么要写个这个
现在flash已经被抛弃，h5播放的时代，网上实现大多不是特别完整的（比如拿到一个rtsp或者rtmp，也不知道怎么在h5页面直接播放），当然现在直播点播有很多方式，可以通过nginx带flv模块的当rtmp服务、还有srs等流媒体服务，而这里我们通过javacv来处理，事实上javacv在性能上会好很多，底层ffmpeg也是通过c实现，跟使用c++去调用差不了多少毫秒延迟


#### 后续计划
* 原本还写了个通过ffmpeg子进程推流，然后用socket服务接收的方案，等javacv版搞完善了再弄。
* 由于m3u8是兼容性最强，水果、安卓和PC通吃，所以后续会加入m3u8切片方式
* 完善web端，方便管理点播文件和播放列表等