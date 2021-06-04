package com.zj.thread;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;

import com.zj.dto.Camera;

import lombok.extern.slf4j.Slf4j;

/**
 * hls切片
 * @author ZJ
 *
 */
@Slf4j
public class MediaTransferHls extends MediaTransfer implements Runnable {
	static {
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		FFmpegLogCallback.set();
	}
	
	/**
	 * 运行状态
	 */
	private volatile boolean running = false;

	private boolean grabberStatus = false;
	
	private boolean recorderStatus = false;

	/**
	 * 用于没有客户端时候的计时
	 */
	private int noClient = 0;

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
	 * @param autoClose   流是否可以自动关闭
	 */
	public MediaTransferHls(Camera camera) {
		super();
		this.camera = camera;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * 创建拉流器
	 * @return
	 */
	protected boolean createGrabber() {
		return false;
	}
	

	@Override
	public void run() {
		
	}

}
