package com.zj.thread;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bytedeco.javacv.FrameGrabber.Exception;

import com.zj.common.ClientType;
import com.zj.common.MediaConstant;
import com.zj.dto.Camera;
import com.zj.service.MediaService;

import cn.hutool.core.collection.CollUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 使用ffmpeg推拉流，可以说无敌了
 * 
 * 优点：支持各种杂七杂八的流，兼容性比较好，稳定，不容易出错，自身带有重连机制，可以自己使用命令封装
 * 缺点：系统会存在多个ffmpeg进程, 无法直接操作帧，延迟优化没javacv方便
 * 
 * @author ZJ
 *
 */
@Slf4j
public class MediaTransferFlvByFFmpeg extends MediaTransfer {

	/**
	 * ws客户端
	 */
	private ConcurrentHashMap<String, ChannelHandlerContext> wsClients = new ConcurrentHashMap<>();
	/**
	 * http客户端
	 */
	private ConcurrentHashMap<String, ChannelHandlerContext> httpClients = new ConcurrentHashMap<>();

	/**
	 * flv header
	 */
	private byte[] header = null;
	/**
	 * 相机
	 */
	private Camera camera;

	private List<String> command = new ArrayList<>();

	private ServerSocket tcpServer = null;

	private Process process;
	private Thread inputThread;
	private Thread errThread;
	private Thread outputThread;
	private Thread listenThread;
	private boolean running = false; // 启动
	private boolean enableLog = false;
	
	private int hcSize, wcSize = 0;

	/**
	 * 用于没有客户端时候的计时
	 */
	private int noClient = 0;

	public MediaTransferFlvByFFmpeg(final String executable) {
		command.add(executable);
		buildCommand();
	}

	public MediaTransferFlvByFFmpeg(Camera camera) {
		command.add(System.getProperty(MediaConstant.ffmpegPathKey));
		this.camera = camera;
		buildCommand();
	}
	
	public MediaTransferFlvByFFmpeg(final String executable, Camera camera) {
		command.add(executable);
		this.camera = camera;
		buildCommand();
	}

	public MediaTransferFlvByFFmpeg(final String executable, Camera camera, boolean enableLog) {
		command.add(executable);
		this.camera = camera;
		this.enableLog = enableLog;
		buildCommand();
	}
	
	public boolean isEnableLog() {
		return enableLog;
	}

	public void setEnableLog(boolean enableLog) {
		this.enableLog = enableLog;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	private MediaTransferFlvByFFmpeg addArgument(String argument) {
		command.add(argument);
		return this;
	}

	/**
	 * 构建ffmpeg命令
	 */
	private void buildCommand() {
		this
		.addArgument("-rtsp_transport").addArgument("tcp")
		.addArgument("-i").addArgument(camera.getUrl())
		.addArgument("-max_delay").addArgument("100")
//		.addArgument("-strict").addArgument("experimental")
		.addArgument("-g").addArgument("25")
		.addArgument("-r").addArgument("25")
//		.addArgument("-b").addArgument("200000")
//		.addArgument("-filter_complex").addArgument("setpts='(RTCTIME - RTCSTART) / (TB * 1000000)'")
		.addArgument("-c:v").addArgument("libx264")
//		.addArgument("-preset:v").addArgument("ultrafast")
		.addArgument("-preset:v").addArgument("fast")
//		.addArgument("-tune:v").addArgument("zerolatency")
//		.addArgument("-crf").addArgument("26")
		.addArgument("-c:a").addArgument("aac")
//		.addArgument("-qmin").addArgument("28")
//		.addArgument("-qmax").addArgument("32")
//		.addArgument("-b:v").addArgument("448k")
//		.addArgument("-b:a").addArgument("64k")
		.addArgument("-f").addArgument("flv");
	}
	
//	private void buildCommand() {
//		this
////		.addArgument("-rtsp_transport").addArgument("tcp")
//		.addArgument("-i").addArgument(camera.getUrl())
//		.addArgument("-max_delay").addArgument("100")
////		.addArgument("-strict").addArgument("experimental")
//		.addArgument("-g").addArgument("10")
////		.addArgument("-r").addArgument("25")
////		.addArgument("-b").addArgument("200000")
////		.addArgument("-filter_complex").addArgument("setpts='(RTCTIME - RTCSTART) / (TB * 1000000)'")
//		.addArgument("-c:v").addArgument("libx264")
//		.addArgument("-preset:v").addArgument("ultrafast")
//		.addArgument("-tune:v").addArgument("zerolatency")
////		.addArgument("-crf").addArgument("26")
//		.addArgument("-c:a").addArgument("aac")
//		.addArgument("-qmin").addArgument("28")
//		.addArgument("-qmax").addArgument("32")
//		.addArgument("-b:v").addArgument("448k")
//		.addArgument("-b:a").addArgument("64k")
//		.addArgument("-f").addArgument("flv");
//	}

	/**
	 * 执行推流
	 * @return
	 */
	public MediaTransferFlvByFFmpeg execute() {
		String output = getOutput();
		command.add(output);

		String join = CollUtil.join(command, " ");
		log.info(join);
		try {
			process = new ProcessBuilder(command).start();
			running = true;
			dealStream(process);
			outputData();
			listenClient();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	/**
	 * flv数据
	 */
	private void outputData() {
		outputThread = new Thread(new Runnable() {
			public void run() {
				Socket client = null;
				try {
					client = tcpServer.accept();
					DataInputStream input = new DataInputStream(client.getInputStream());

					byte[] buffer = new byte[1024];
					int len = 0;
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					while (running) {
						
						len = input.read(buffer);
						if (len == -1) {
							break;
						}

						bos.write(buffer, 0, len);

						if (header == null) {
							header = bos.toByteArray();
//							System.out.println(HexUtil.encodeHexStr(header));
							bos.reset();
							continue;
						}
						
						// 帧数据
						byte[] data = bos.toByteArray();
						bos.reset();
						
						// 发送到前端
						sendFrameData(data);
					}
					
					try {
						client.close();
					} catch (java.lang.Exception e) {
					}
					try {
						input.close();
					} catch (java.lang.Exception e) {
					}
					try {
						bos.close();
					} catch (java.lang.Exception e) {
					}
					
					log.info("关闭媒体流-ffmpeg，{} ", camera.getUrl());

				} catch (SocketTimeoutException e1) {
//					e1.printStackTrace();
//					超时关闭
				} catch (IOException e) {
//					e.printStackTrace();
				} finally {
					MediaService.cameras.remove(camera.getMediaKey());
					running = false;
					process.destroy();
					try {
						if (null != client) {
							client.close();
						}
					} catch (IOException e) {
					}
					try {
						if (null != tcpServer) {
							tcpServer.close();
						}
					} catch (IOException e) {
					}
				}
			}
		});

		outputThread.start();
	}
	
	/**
	 * 监听客户端
	 */
	public void listenClient() {
		listenThread = new Thread(new Runnable() {
			public void run() {
				while (running) {
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

	public static MediaTransferFlvByFFmpeg atPath() {
		return atPath(null);
	}

	public static MediaTransferFlvByFFmpeg atPath(final String absPath) {
		final String executable;
		if (absPath != null) {
			executable = absPath;
		} else {
//			executable = "ffmpeg";
			executable = System.getProperty(MediaConstant.ffmpegPathKey);
		}
		return new MediaTransferFlvByFFmpeg(executable);
	}

	/**
	 * 控制台输出
	 * 
	 * @param process
	 */
	private void dealStream(Process process) {
		if (process == null) {
			return;
		}
		// 处理InputStream的线程
		inputThread = new Thread() {
			@Override
			public void run() {
				BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = null;
				try {
					while (running) {
						line = in.readLine();
						if (line == null) {
							break;
						}
						if (enableLog) {
							log.info("output: " + line);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		// 处理ErrorStream的线程
		errThread = new Thread() {
			@Override
			public void run() {
				BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String line = null;
				try {
					while (running) {
						line = err.readLine();
						if (line == null) {
							break;
						}
						if (enableLog) {
							log.info("err: " + line);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						err.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};

		inputThread.start();
		errThread.start();
	}

	/**
	 * 输出到tcp
	 * 
	 * @return
	 */
	private String getOutput() {
		try {
			tcpServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
			StringBuffer sb = new StringBuffer();
			sb.append("tcp://");
			sb.append(tcpServer.getInetAddress().getHostAddress());
			sb.append(":");
			sb.append(tcpServer.getLocalPort());
			tcpServer.setSoTimeout(10000);
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new RuntimeException("无法启用端口");
		return "";
	}
	
	/**
	 * 关闭流
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

		// 无需自动关闭
		if (!camera.isAutoClose()) {
			return;
		}
		
		if (httpClients.isEmpty() && wsClients.isEmpty()) {
			// 等待20秒还没有客户端，则关闭推流
			if (noClient > camera.getNoClientsDuration()) {
				running = false;
				MediaService.cameras.remove(camera.getMediaKey());
			} else {
				noClient += 1000;
//				log.info("\r\n{}\r\n {} 秒自动关闭推拉流 \r\n", camera.getUrl(), noClientsDuration-noClient);
			}
		} else {
			//重置计时
			noClient = 0;
		}
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
	 * 新增客户端
	 * 
	 * @param ctx   netty client
	 * @param ctype enum,ClientType
	 */
	public void addClient(ChannelHandlerContext ctx, ClientType ctype) {
		int timeout = 0;
		while (true) {
			try {
				if (header != null) {
					try {
						if (ctx.channel().isWritable()) {
							// 发送帧前先发送header
							if (ClientType.HTTP.getType() == ctype.getType()) {
								ChannelFuture future = ctx.writeAndFlush(Unpooled.copiedBuffer(header));
								future.addListener(new GenericFutureListener<Future<? super Void>>() {
									@Override
									public void operationComplete(Future<? super Void> future) throws Exception {
										if (future.isSuccess()) {
											httpClients.put(ctx.channel().id().toString(), ctx);
										}
									}
								});
							} else if (ClientType.WEBSOCKET.getType() == ctype.getType()) {
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
						}

					} catch (java.lang.Exception e) {
						e.printStackTrace();
					}
					break;
				}

				// 等待推拉流启动
				Thread.sleep(50);
				// 启动录制器失败
				timeout += 50;
				if (timeout > 30000) {
					break;
				}
			} catch (java.lang.Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {
//		ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
//		System.out.println(serverSocket.getLocalPort());
//		System.out.println(serverSocket.getInetAddress().getHostAddress());

		MediaTransferFlvByFFmpeg.atPath().addArgument("-i").addArgument("rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102")
				.addArgument("-g").addArgument("5").addArgument("-c:v").addArgument("libx264").addArgument("-c:a")
				.addArgument("aac").addArgument("-f").addArgument("flv").execute();
	}

}
