package com.zj.thread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;

import com.zj.entity.Camera;
import com.zj.service.MediaService;

import cn.hutool.crypto.digest.MD5;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * <b>支持转复用或转码线程<b>
 * <b> 什么情况下会转复用?</b>
 * <p> 视频源的音视频编码必须是浏览器和flv规范两者同时支持的编码，比如H264/AAC，</p>
 * <p> 否则将进行转码。</p>
 * <p> 不支持hevc、vvc、vp8、vp9、g711、g771a等编码</p>
 * @author eguid
 */
@Slf4j
public class MediaRecodeOrTransfer extends Thread {
	static {
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		FFmpegLogCallback.set();
	}
	
	/**
	 * ws客户端
	 */
	private ConcurrentHashMap<String, ChannelHandlerContext> wsClients = new ConcurrentHashMap<>();
	/**
	 * http客户端
	 */
	private ConcurrentHashMap<String, ChannelHandlerContext> httpClients = new ConcurrentHashMap<>();

	/**
	 * 运行状态
	 */
	private boolean runing = false;

	private boolean grabberStatus = false;
	
	private boolean recorderStatus = false;

	/**
	 * 是否可以自动关闭流
	 */
	private boolean autoClose = true;

	private int hcSize, wcSize = 0;

	/**
	 * 没有客户端计数
	 */
	private int noClient = 0;

	/**
	 * flv header
	 */
	private byte[] header = null;
	// 输出流，视频最终会输出到此
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();

	FFmpegFrameGrabber grabber;//拉流器
	FFmpegFrameRecorder recorder;//推流录制器

	/**
	 * true:转复用,false:转码
	 */
	boolean transferFlag=false;//默认转码
	
	/**
	 * 相机
	 */
	private Camera camera;
	
	/**
	 * 监听线程，用于监听状态
	 */
	private Thread listenThread;

	/**
	 * @param camera
	 * @param auto   流是否可以自动关闭
	 */
	public MediaRecodeOrTransfer(Camera camera, boolean autoClose) {
		super();
		this.autoClose = autoClose;
		this.camera = camera;
	}

	public boolean isRuning() {
		return runing;
	}

	public void setRuning(boolean runing) {
		this.runing = runing;
	}

	/**
	 * 创建拉流器
	 * @return
	 */
	protected boolean createGrabber() {
		// 拉流器
		grabber = new FFmpegFrameGrabber(camera.getUrl());
		// 超时时间(15秒)
		grabber.setOption("stimoout", "15000000");
		grabber.setOption("threads", "1");
		grabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
		// 设置缓存大小，提高画质、减少卡顿花屏
		grabber.setOption("buffer_size", "1024000");
		// 如果为rtsp流，增加配置
		if ("rtsp".equals(camera.getUrl().substring(0, 4))) {
			// 设置打开协议tcp / udp
			grabber.setOption("rtsp_transport", "tcp");
			//首选TCP进行RTP传输
			grabber.setOption("rtsp_flags", "prefer_tcp");
			//设置超时时间
			grabber.setOption("stimeout","3000000");
		}

		try {
			grabber.start();
			return grabberStatus = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return grabberStatus = false;
	}
	
	/**
	 * 创建转码推流录制器
	 * @return
	 */
	protected boolean createTransterOrRecodeRecorder() {
		recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(), grabber.getImageHeight(),grabber.getAudioChannels());
		recorder.setFormat("flv");
		if(!transferFlag) {
			//转码
			recorder.setInterleaved(false);
			recorder.setVideoOption("tune", "zerolatency");
			recorder.setVideoOption("preset", "ultrafast");
			recorder.setVideoOption("crf", "26");
			recorder.setVideoOption("threads", "1");
			recorder.setFrameRate(25);// 设置帧率
			recorder.setGopSize(25);// 设置gop,与帧率相同，相当于间隔1秒chan's一个关键帧
//						recorder.setVideoBitrate(500 * 1000);// 码率500kb/s
			recorder.setVideoCodecName("libx264");
//						recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
//						recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
			recorder.setAudioCodecName("aac");
			try {
				recorder.start();
				return recorderStatus=true;
			} catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
				log.info("启动转码录制器失败", e1);
				e1.printStackTrace();
			}
		}else {
			//转复用
			try {
				recorder.start(grabber.getFormatContext());
				return recorderStatus=true;
			} catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
				log.info("启动转复用录制器失败", e1);
				e1.printStackTrace();
			}
		}
		return recorderStatus=false;
	}
	
	/**
	 * 是否支持flv的音视频编码
	 * @return
	 */
	private boolean supportFlvFormatCodec() {
		int vcodec=grabber.getVideoCodec();
		int acodec=grabber.getAudioCodec();
		return (camera.getType() == 0) && (avcodec.AV_CODEC_ID_H264==vcodec||avcodec.AV_CODEC_ID_H263==vcodec)&&(avcodec.AV_CODEC_ID_AAC==acodec||avcodec.AV_CODEC_ID_AAC_LATM==acodec);
	}
	
	/**
	 * 是否是本地文件
	 * @return 
	 */
	private boolean isLocalFile(String streamUrl) {
		String[] split = streamUrl.trim().split("\\:");
		if(split.length > 0) {
			if(split[0].length() <= 1) {
				return true;
			} 
		}
		return false;
	}
	
	/**
	 * 将视频源转换为flv
	 */
	protected void transferStream2Flv() {
		if(isLocalFile(camera.getUrl())) {
			camera.setType(1);
		}
		
		if(!createGrabber()) {
			return;
		}
		transferFlag = supportFlvFormatCodec();
		if(!createTransterOrRecodeRecorder()) {
			return;
		}
		
		try {
			grabber.flush();
		} catch (Exception e) {
			log.info("清空拉流器缓存失败", e);
			e.printStackTrace();
		}
		if (header == null) {
			header = bos.toByteArray();
//				System.out.println(HexUtil.encodeHexStr(header));
			bos.reset();
		}

		runing = true;
		
		//启动监听线程（用于判断是否需要自动关闭推流）
		listenClient();
		
		//时间戳计算
		long startTime = 0;
		long videoTS = 0;
		long lastTime=0;
		//累积延迟计算
		long latencyDifference=0;//延迟差值
		long maxLatencyThreshold=3000;//最大延迟阈值，如果lastLatencyDifference-latencyDifference>maxLatencyThreshold，则重启拉流器
		long lastLatencyDifference=0;//当前最新延迟差值，
		
		long processTime=0;//上一帧处理耗时，用于延迟时间补偿，处理耗时不算进累积延迟
		for(;runing && grabberStatus && recorderStatus;) {
			
			lastTime=System.currentTimeMillis();
			//累积延迟过大，则重新建立连接
			if (lastLatencyDifference-latencyDifference>maxLatencyThreshold) {
				try {
					grabber.restart(); // grabber.grabFrame() avformat
					grabber.flush();
					log.warn("\r\n{}\r\n重连成功》》》", camera.getUrl());
				} catch (Exception e) {
					log.warn("\r\n{}\r\n重连失败！", camera.getUrl());
					//跳出循环，销毁拉流器和录制器
					break;
				}
			}

			try {
				if(transferFlag) {
//					log.error("转复用流程");
					//转复用
					AVPacket pkt = grabber.grabPacket();
					if (null!=pkt&&!pkt.isNull()) {
						if (startTime == 0) {
							startTime = System.currentTimeMillis();
						}
						videoTS = 1000 * (System.currentTimeMillis() - startTime);
						// 判断时间偏移
						if (videoTS > recorder.getTimestamp()) {
							//System.out.println("矫正时间戳: " + videoTS + " : " + recorder.getTimestamp() + " -> "
							//+ (videoTS - recorder.getTimestamp()));
							recorder.setTimestamp((videoTS));
						}
						recorder.recordPacket(pkt);
					}
				}else {
//					log.error("转码流程");
					//转码
					Frame frame = grabber.grabFrame();
					if (frame != null) {
						if (startTime == 0) {
							startTime = System.currentTimeMillis();
						}
						videoTS = 1000 * (System.currentTimeMillis() - startTime);
						// 判断时间偏移
						if (videoTS > recorder.getTimestamp()) {
							//System.out.println("矫正时间戳: " + videoTS + " : " + recorder.getTimestamp() + " -> "
							//+ (videoTS - recorder.getTimestamp()));
							recorder.setTimestamp((videoTS));
						}
						recorder.record(frame);
					}
				}
			} catch (Exception e) {
				//log.info("\r\n{}\r\n尝试重连。。。", camera.getUrl());
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
				//e.printStackTrace();
			} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
				//runing = false;
				log.info("\r\n{}\r\n录制器出现异常。。。", camera.getUrl());
				e.printStackTrace();
			}
			if (bos.size() > 0) {
				byte[] b = bos.toByteArray();
				bos.reset();

				// 发送视频到前端
				sendFrameData(b);
				
				//流程耗时记录
				if(lastTime>0) {
					processTime=System.currentTimeMillis()-lastTime;
				}
			}
		}

		// close包含stop和release方法。录制文件必须保证最后执行stop()方法
		try {
			recorder.close();
			grabber.close();
			bos.close();
		} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			runing = false;
		}
		log.info("关闭媒体流，{} ", camera.getUrl());
	}
	
	/**
	 * 发送帧数据
	 * 
	 * @param data
	 */
	private void sendFrameData(byte[] data) {
		// ws
		for (Entry<String, ChannelHandlerContext> entry : wsClients.entrySet()) {
			try {
				if (entry.getValue().channel().isWritable()) {
					entry.getValue().writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(data)));
				} else {
					wsClients.remove(entry.getKey());
					hasClient();
				}
			} catch (java.lang.Exception e) {
				wsClients.remove(entry.getKey());
				hasClient();
				e.printStackTrace();
			}
		}
		// http
		for (Entry<String, ChannelHandlerContext> entry : httpClients.entrySet()) {
			try {
				if (entry.getValue().channel().isWritable()) {
					entry.getValue().writeAndFlush(Unpooled.copiedBuffer(data));
				} else {
					httpClients.remove(entry.getKey());
					hasClient();
				}
			} catch (java.lang.Exception e) {
				httpClients.remove(entry.getKey());
				hasClient();
				e.printStackTrace();
			}
		}
	}

	/**
	 * 新增ws客戶端
	 * 
	 * @param session
	 */
	public void addWsClient(ChannelHandlerContext ctx) {
		int timeout = 0;
		while (true) {
			try {
				if (runing) {
					try {
						if (ctx.channel().isWritable()) {
							// 发送帧前先发送header
							ChannelFuture future = ctx
									.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(header)));
							future.addListener(new GenericFutureListener<Future<? super Void>>() {
								@Override
								public void operationComplete(Future<? super Void> future) throws Exception {
									if (future.isSuccess()) {
										wsClients.put(ctx.channel().id().toString(), ctx);
									}
								}
							});
						}

					} catch (java.lang.Exception e) {
						e.printStackTrace();
					}
					break;
				}

				// 等待推拉流启动
				Thread.sleep(100);
				// 启动录制器失败
				timeout += 100;
				if (timeout > 15000) {
					break;
				}
			} catch (java.lang.Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 判断有没有客户端，关闭流
	 * 
	 * @return
	 */
	public void hasClient() {

		int newHcSize = httpClients.size();
		int newWcSize = wsClients.size();
		if (hcSize != newHcSize || wcSize != newWcSize) {
			hcSize = newHcSize;
			wcSize = newWcSize;
			log.info("\r\n{}\r\nhttp连接数：{}, ws连接数：{} \r\n", camera.getUrl(), newHcSize, newWcSize);
		}

		// 自动拉流无需关闭
		if (!autoClose) {
			return;
		}
		if (httpClients.isEmpty() && wsClients.isEmpty()) {
			// 等待20秒还没有客户端，则关闭推流
			if (noClient > 20) {
				runing = false;
				String mediaKey = MD5.create().digestHex(camera.getUrl());
				MediaService.cameras.remove(mediaKey);
			} else {
				noClient += 1;
//				log.info("\r\n{}\r\n {} 秒自动关闭推拉流 \r\n", camera.getUrl(), 11-noClient);
			}
		} else {
			noClient = 0;
		}
	}

	/**
	 * 监听客户端，用于判断无人观看时自动关闭推流
	 */
	public void listenClient() {
		listenThread = new Thread(new Runnable() {
			public void run() {
				while (runing) {
					hasClient();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		listenThread.start();
	}

	/**
	 * 新增http客戶端
	 * 
	 * @param session
	 */
	public void addHttpClient(ChannelHandlerContext ctx) {
		int timeout = 0;
		while (true) {
			try {
				if (runing) {
					try {
						if (ctx.channel().isWritable()) {
							// 发送帧前先发送header
							ChannelFuture future = ctx.writeAndFlush(Unpooled.copiedBuffer(header));
							future.addListener(new GenericFutureListener<Future<? super Void>>() {
								@Override
								public void operationComplete(Future<? super Void> future) throws Exception {
									if (future.isSuccess()) {
										httpClients.put(ctx.channel().id().toString(), ctx);
									}
								}
							});
						}

					} catch (java.lang.Exception e) {
						e.printStackTrace();
					}
					break;
				}

				// 等待推拉流启动
				Thread.sleep(100);

				// 启动录制器失败
				timeout += 100;
				if (timeout > 15000) {
					break;
				}
			} catch (java.lang.Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		transferStream2Flv();
	}

}
