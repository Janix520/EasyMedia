
# EasyMedia

#### 介绍
Springboot、netty实现的http-flv、websocket-flv流媒体服务（可用于直播点播），支持rtsp、h264、h265等、rtmp等多种源，h5纯js播放（不依赖flash），不需要依赖nginx等第三方，延迟大部分在1-5秒内（已经支持转复用，h264的流自动转封装，超低延迟。PS:当然还有种更低延迟的不用flv方案没时间写了，但是主要是flv比较大众，这个一般也够用了）。


[成品下载](https://data-hz-pds.teambition.net/9SJkXQ1A%2F72196%2F60bceac44870391c158a4ff5a9de06138213d509%2F60bceac490e6b6e8f6a6486197edc647d7e99e17?di=hz423&dr=72196&f=60bceac44870391c158a4ff5a9de06138213d509&response-content-disposition=attachment%3B%20filename%2A%3DUTF-8%27%27EasyMedia-0.0.1-SNAPSHOT.jar&u=228014786770217879&x-oss-access-key-id=LTAIsE5mAn2F493Q&x-oss-expires=1622994541&x-oss-signature=gsN0jUwoenq0gfF1x9p2vPoDgTCdFb4qj0JuajEhHkk%3D&x-oss-signature-version=OSS2 "成品下载")


[前端源码传送门](https://download.csdn.net/download/Janix520/15785632 "前端源码传送门")


PS：项目里已经集成最新版编译好的前端，由于前端只是个demo，这里是没有hls功能的版本，hls前端播放比较简单，我就懒得上传了


#### 功能汇总 （文档水平有限，使用前尽量先看完readme）
- 支持播放 rtsp、rtmp、http、文件等流……
- pc端桌面投影
- 支持永久播放、按需播放（无人观看自动断开）
- 自动判断流格式h264、h265，自动转封装
- 支持http、ws协议的flv
- 支持hls内存切片（不占用本地磁盘，只占用网络资源）
- 重连功能
- 支持javacv、ffmpeg方式切换

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
电脑桌面投影（url改成desktop即可）：
http://localhost:8866/live?url=desktop
ws://localhost:8866/live?url=desktop
```

- 推拉流（有两种方式）
1. 按需播放（默认）直接使用上面播放地址就可以播放，每次第一个用户打开会创建推流，没人看时十几秒后会自动断开流。
2. 永久播放，通过restfu api或者页面先新增流，再开启推流，在服务同级目录会生成一个camera.json，重启服务发现camera.json也会自动推流，用json文件是方便手动维护源地址，之后通过播放地址可以在浏览器直接秒开。
3. hls播放， http://localhost:8888/hls?url={您的源地址}

```
永久播放还有一种捷径，就是在播放地址后面加上autoClose=false参数，也会加入到json中。
例如：
http://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102&&&autoClose=false
ws://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102&&&autoClose=false
###
hls播放例子：(注意：hls为http端口8888，并且不支持url后面参数，开启切片后可以播放)
http://localhost:8888/hls?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102
```

- url参数说明
```
参数加在播放地址url最后面，使用 [&&&] 符号
例如：
http://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102&&&autoClose=false&&&ffmpeg=true
ws://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102&&&autoClose=false&&&ffmpeg=true
###
autoClose=false 设置为永久播放
ffmpeg=true 使用ffmpeg方式，提高兼容稳定性（不支持的流可以试试这个参数）
//hls=true（目前还不支持此参数，只能api或者网页端控制开启）
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
停止flv推流 http://localhost:8888/stop?url={您的源地址}
开启flv推流 http://localhost:8888/start?url={您的源地址}
开启hls切片 http://localhost:8888/startHls?url={您的源地址}
停止hls切片 http://localhost:8888/stopHls?url={您的源地址}
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
![](https://gitee.com/52jian/EasyMedia/raw/master/snapshot/4.png)


#### 源码教程

1.  环境：java8+
2.  标准的maven项目，sts、eclipse或者idea导入，直接运行main方法


#### 更新说明 2021-06-06
- 新增支持使用ffmpeg推拉流，提高兼容稳定性（流几乎全支持，再无花屏，绿色杠杠啥的）
- 新增“hls内存切片”，不占用本地磁盘读写，速度你懂的，只占用网络资源，目前默认全部转码，延迟在5秒左右，稍微费点cpu
- 优化接口、优化服务、新增其他配置参数
- 新增pc端桌面投影
- 更新前端功能
- 完善项目注释
- 新增启动logo


#### 更新说明 2021-05-21
- 支持转复用或转码，h264的流支持自动转封装，超低延迟


#### 更新说明 2021-05-18
- 解决大华等带有参数的地址解析问题


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


#### 网上找的测试地址 安徽卫视，建议用自己内网的，公开的可能速度就比较慢 （有时候会失效，尽量用自己的）
`rtmp://58.200.131.2:1935/livetv/ahtv`


#### 为什么要写个这个
现在flash已经被抛弃，h5播放的时代，网上实现大多不是特别完整的（比如拿到一个rtsp或者rtmp，也不知道怎么在h5页面直接播放），当然现在直播点播有很多方式，可以通过nginx带flv模块的当rtmp服务、还有srs等流媒体服务，而这里我们通过javacv来处理，事实上javacv性能足够，底层ffmpeg也是通过c实现，使用java调用c跟使用c++去调用c差不了多少毫秒延迟。java流媒体资料比较少，但从应用层来说，java有着庞大的生态优势，配合netty写出的流媒体性能可想而知，而此源码目前也比较简单，可读性比较强，有能力者完全能自主改成java分布式流媒体。随着人工智能图像识别的发展，从流媒体获取图像数据是必要条件，有bug希望你们也能及时提出。


**最后感谢eguid的javacv文档，https://eguid.blog.csdn.net/**


#### 后续计划
- ✔ web管理页面其实只也是个demo，以后看情况更新了
- ✖ 增加录制功能（打算专门做存储至分布式文件系统，独立开来，先不集成进来了）
- ✔ 由于hls(m3u8)兼容性最好，水果、安卓和PC通吃，所以后续会加入m3u8切片方式
- ✔ 原本还写了个通过ffmpeg子进程推流，然后用socket服务接收的方案，等javacv版搞完善了再弄。
- 云台控制（集成海康大华云台接口），看情况集成
- ***大部分功能已完成，个人精力有限，后续更新频率会适当降低***

