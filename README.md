
# EasyMedia

#### 介绍
Springboot、netty实现的http-flv、websocket-flv流媒体服务（可用于直播点播），支持rtsp、h264、h265等、rtmp等多种源，h5纯js播放（不依赖flash），不需要依赖nginx等第三方，由于全部经过转码，延迟大部分在3-5秒左右（如果转封装可以在2-3秒左右，需自行修改）。


#### 成品下载
链接：https://pan.baidu.com/s/1ZIjZXXKx6-6X0SEvju5e_w
提取码：b9pf
[前端源码传送门](https://download.csdn.net/download/Janix520/15785632 "前端源码传送门")


#### 软件架构
- netty负责播放地址解析及视频传输，通过javacv推拉流存到内存里，直接通过输出到前端播放
- 后端：springboot、netty，集成websocket
- 前端：vue、html5（简单的管理页面）
- 播放器：西瓜播放器 http://h5player.bytedance.com/ （字节跳动家的，不介绍了，抖音视频、西瓜视频都杠杠的，当然只要支持flv的播放器都可以）
- 媒体框架：javacv、ffmpeg


#### 使用教程
> 流媒体服务会绑定两个端口，分别为 8866（媒体端口）、8888（web端口，后续会做简单的管理页面）
您只需要将 {您的源地址} 替换成您的，然后放播放器里就能看了


- 播放地址（播放器里直接用这个地址播放）
```
http://localhost:8866/live?url={您的源地址}
ws://localhost:8866/live?url={您的源地址}
例如rtsp：
http://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102
ws://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102
文件（支持格式参照ffmpeg支持的格式）：
http://localhost:8866/live?url=d:/flv/testVideo.mp4
ws://localhost:8866/live?url=d:/flv/testVideo.mp4
```

- 推拉流（有两种方式）
1. 按需播放（默认）直接使用上面播放地址就可以播放，每次第一个用户打开会创建推流，没人看时十几秒后会自动断开流。
2. 永久播放，通过restfu api或者页面先新增流，再开启推流，在服务同级目录会生成一个camera.json，重启服务发现camera.json也会自动推流，用json文件是方便手动维护源地址，之后通过播放地址可以在浏览器直接秒开。
```
永久播放还有一种捷径，就是在播放地址后面加上autoClose=false参数，也会加入到json中。
例如：
http://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102&autoClose=false
ws://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102&autoClose=false
```

- 页面功能
```
可以访问 http://localhost:8888
具体功能参照截图
```

- restful api
```
新增流 http://localhost:8888/add?url={您的源地址}&remark={备注}
停止并删除 http://localhost:8888/del?url={您的源地址}
停止推流 http://localhost:8888/stop?url={您的源地址}
开始推流 http://localhost:8888/start?url={您的源地址}
查看保存的流 http://localhost:8888/list
```

- 成品运行方式（由于是跨平台的，未做ffmpeg精简，所以包会比较大）
```
java -jar EasyMedia-0.0.1-SNAPSHOT.jar
还可以这样改端口
java -jar -Dserver.port=页面端口 -Dmediaserver.port=媒体端口 EasyMedia-0.0.1-SNAPSHOT.jar
```

#### 疑问解答
- 在vlc、ffplay等播放器测试存在延迟较高是正常的，是因为他们默认的嗅探关键帧的时间比较长，测延迟建议还是用flv.js播放器测试。
- 是否需要ffmpeg推流，不需要，就是为了简化使用，只需运行一个服务即可。
- 很多人想用文件点播，可以参考截图。


#### 截图
![](https://gitee.com/52jian/EasyMedia/raw/master/snapshot/1.png)
![](https://gitee.com/52jian/EasyMedia/raw/master/snapshot/2.png)
![](https://gitee.com/52jian/EasyMedia/raw/master/snapshot/3.png)


#### 源码教程

1.  环境：java8+
2.  标准的maven项目，sts、eclipse或者idea导入，直接运行main方法


#### 更新说明 2021-03-14
- 新增简单的web页面管理
- 优化自动断开
- 新增服务端自动重连
- 支持本地文件点播
- 支持启动服务自动推流
- 支持音频转码
- 启动服务前初始化资源（防止第一次启动慢）
- 新增保存数据到同级目录的camera.json


#### 更新说明 2021-02-20
- 移除原有spring websocket，采用高性能的netty作为http、ws服务。
- 完善关闭流逻辑，没人看时会自动断开。
- 由于替换netty，考虑到视频文件需要上传到服务器，所以暂时移除本地文件支持。


#### 网上找的测试地址 安徽卫视，建议用自己内网的，公开的可能速度就比较慢 
`rtmp://58.200.131.2:1935/livetv/ahtv`


#### 为什么要写个这个
现在flash已经被抛弃，h5播放的时代，网上实现大多不是特别完整的（比如拿到一个rtsp或者rtmp，也不知道怎么在h5页面直接播放），当然现在直播点播有很多方式，可以通过nginx带flv模块的当rtmp服务、还有srs等流媒体服务，而这里我们通过javacv来处理，事实上javacv性能足够，底层ffmpeg也是通过c实现，使用java调用c跟使用c++去调用c差不了多少毫秒延迟。java流媒体资料比较少，但从应用层来说，java有着庞大的生态优势，配合netty写出的流媒体性能可想而知，而此源码目前也比较简单，可读性比较强，有能力者完全能自主改成java分布式流媒体。随着人工智能图像识别的发展，从流媒体获取图像数据是必要条件，有bug希望你们也能及时提出。


**最后感谢eguid的javacv文档，https://eguid.blog.csdn.net/**


#### 后续计划
- 完善web管理页面，方便管理点播文件和播放列表等
- 增加录制功能
- 由于hls(m3u8)兼容性最好，水果、安卓和PC通吃，所以后续会加入m3u8切片方式
- 原本还写了个通过ffmpeg子进程推流，然后用socket服务接收的方案，等javacv版搞完善了再弄。
- 没想到这项目会有这么多人用，我会抽空慢慢维护。

