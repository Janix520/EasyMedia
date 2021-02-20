package com.zj.thread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;

import com.zj.entity.Camera;
import com.zj.service.MediaService;

import cn.hutool.crypto.digest.MD5;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * 拉流转换推流处理线程
 * 
 * @author ZJ
 *
 */
@Slf4j
public class MediaConvert extends Thread {
	
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
	private boolean isStart = false;
	
	/**
	 * flv header
	 */
	private byte[] header = null;
	// 输出流，视频最终会输出到此
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();

	/**
	 * 相机
	 */
	private Camera camera;
	
	public MediaConvert(Camera camera) {
		super();
		this.camera = camera;
	}

	/**
	 * 
	 */
	private void convert() {
		// 拉流器
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(camera.getUrl());
		// 超时时间(15秒)
		grabber.setOption("stimoout", "15000000");
		grabber.setOption("threads", "1");
		// 如果为rtsp流，增加配置
		if ("rtsp".equals(camera.getUrl().substring(0, 4))
				|| "rtmp".equals(camera.getUrl().substring(0, 4))) {
			// 设置打开协议tcp / udp
			grabber.setOption("rtsp_transport", "tcp");
			// 设置缓存大小，提高画质、减少卡顿花屏
			grabber.setOption("buffer_size", "1024000");
		}
		
		try {
			grabber.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 推流器
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(), grabber.getImageHeight());

		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		recorder.setInterleaved(true);
		recorder.setVideoOption("tune","zerolatency");
		recorder.setVideoOption("preset", "ultrafast");
		recorder.setVideoOption("crf", "26");
		recorder.setVideoOption("threads", "1");
		recorder.setFormat("flv");
		recorder.setFrameRate(25);// 设置帧率
		recorder.setGopSize(5);// 设置gop
//		recorder.setVideoBitrate(500 * 1000);// 码率500kb/s
		recorder.setVideoCodecName("libx264");
//		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
		
		try {
			recorder.start();
			grabber.flush();
		} catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
			
			log.info("启动录制器失败", e1);
			e1.printStackTrace();
		} catch (Exception e1) {
			log.info("拉流器异常", e1);
			e1.printStackTrace();
		}

		if (header == null) {
			header = bos.toByteArray();
//			System.out.println(HexUtil.encodeHexStr(header));
			bos.reset();
		}
		
		runing = true;
		long startTime = 0;
		long videoTS = 0;
		
		while (runing) {
			
			//如果没有客户端，则关闭媒体流
			if(isStart && wsClients.isEmpty() && httpClients.isEmpty()) {
				runing = false;
			} 
			
			try {
				Frame frame = grabber.grabImage();
				if(frame != null) {
					if (startTime == 0) {
						startTime = System.currentTimeMillis();
					}
					videoTS = 1000 * (System.currentTimeMillis() - startTime);
					if (videoTS > recorder.getTimestamp()) {
//					System.out.println("矫正时间戳: " + videoTS + " : " + recorder.getTimestamp() + " -> "
//							+ (videoTS - recorder.getTimestamp()));
						recorder.setTimestamp(videoTS);
					}
					recorder.record(frame);
					
					if (bos.size() > 0) {
						byte[] b = bos.toByteArray();
						bos.reset();

						// ws输出帧流
						for (Entry<String, ChannelHandlerContext> entry : wsClients.entrySet()) {
							try {
								if(entry.getValue().channel().isWritable()) {
									entry.getValue().writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(b)));
								} else {
									wsClients.remove(entry.getKey());
									log.info("当前 http连接数：{}, ws连接数：{}", httpClients.size(), wsClients.size());
									needClose();
								}
							} catch (java.lang.Exception e) {
								wsClients.remove(entry.getKey());
								log.info("当前 http连接数：{}, ws连接数：{}", httpClients.size(), wsClients.size());
								needClose();
								e.printStackTrace();
							}
						}
						
						//http
						for (Entry<String, ChannelHandlerContext> entry : httpClients.entrySet()) {
							try {
								if(entry.getValue().channel().isWritable()) {
									entry.getValue().writeAndFlush(Unpooled.copiedBuffer(b));
								} else {
									httpClients.remove(entry.getKey());
									log.info("当前 http连接数：{}, ws连接数：{}", httpClients.size(), wsClients.size());
									needClose();
								}
							} catch (java.lang.Exception e) {
								httpClients.remove(entry.getKey());
								log.info("当前 http连接数：{}, ws连接数：{}", httpClients.size(), wsClients.size());
								needClose();
								e.printStackTrace();
							}
						}
					}
				}
			} catch (Exception e) {
				runing = false;
				// 这里后续 处理断线重连
				e.printStackTrace();
			} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
				runing = false;
				e.printStackTrace();
			}
		}
		
		//close包含stop和release方法。录制文件必须保证最后执行stop()方法
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
	 * 新增ws客戶端
	 * @param session
	 */
	public void addWsClient(ChannelHandlerContext ctx) {
		int timeout = 0;
		while (true) {
			try {
				if(runing) {
					try {
						if(ctx.channel().isWritable()) {
							//发送帧前先发送header
							ChannelFuture writeAndFlush = ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(header)));
							writeAndFlush.sync();
						} 
						
						wsClients.put(ctx.channel().id().toString(), ctx);
						
						isStart = true;
						
						log.info("当前 http连接数：{}, ws连接数：{}", httpClients.size(), wsClients.size());
					} catch (java.lang.Exception e) {
						e.printStackTrace();
					}
					break;
				}
				
				//等待推拉流启动
				Thread.currentThread().sleep(100);
				//启动录制器失败
				timeout += 100;
				if(timeout > 15000) {
					break;
				}
			} catch (java.lang.Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 关闭流
	 */
	public void needClose() {
		if(httpClients.isEmpty() && wsClients.isEmpty()) {
			runing = false;
			String mediaKey = MD5.create().digestHex(camera.getUrl());
			MediaService.cameras.remove(mediaKey);
		}
	}
	
	/**
	 * 新增http客戶端
	 * @param session
	 */
	public void addHttpClient(ChannelHandlerContext ctx) {
		int timeout = 0;
		while (true) {
			try {
				if(runing) {
					try {
						if(ctx.channel().isWritable()) {
							//发送帧前先发送header
							ctx.writeAndFlush(Unpooled.copiedBuffer(header));
						} 
						
						httpClients.put(ctx.channel().id().toString(), ctx);
						isStart = true;
						
						log.info("当前 http连接数：{}, ws连接数：{}", httpClients.size(), wsClients.size());
					} catch (java.lang.Exception e) {
						e.printStackTrace();
					}
					break;
				}
				
				//等待推拉流启动
				Thread.currentThread().sleep(100);
				
				//启动录制器失败
				timeout += 100;
				if(timeout > 15000) {
					break;
				}
			} catch (java.lang.Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		convert();
	}

}
