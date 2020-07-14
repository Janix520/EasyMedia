# EasyMedia

#### 介绍
Springboot实现的http-flv、websocket-flv直播点播，支持rtsp、本地文件、rtmp等多种源，h5纯js播放（不依赖flash）

#### 软件架构
通过javacv推拉流存到内存里，直接输出到前端播放，现在只是一个播放实现，没有完善关闭回收，还不适用于生产环境。
后端：springboot，集成websocket
前端：html5
播放器：西瓜播放器（字节跳动家的，不介绍了，抖音视频、西瓜视频都杠杠的，当然只要支持flv的播放器都可以）
媒体框架：javacv（原本还写了个通过ffmpeg子进程推流，用socket服务接收的方案，等javacv版搞完善了再弄）

#### 安装教程

1.  环境：java8+
2.  标准的maven项目，sts、eclipse或者idea导入，直接运行main方法，或者直接命令打包运行
3.  

#### 使用说明

1.  运行后访问：http://localhost:8888/
2.  默认读取的是项目根目录下的video本地视频
3.  http接口：http://localhost:8888/test?url=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov&id=1
4.  ws接口：ws://localhost:8888/flv?url=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov&id=2
5.  url为输地址、可以是本地地址也可以是rtsp、rtmp等，id为视频的唯一编号

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 码云特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  码云官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解码云上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是码云最有价值开源项目，是码云综合评定出的优秀开源项目
5.  码云官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  码云封面人物是一档用来展示码云会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
