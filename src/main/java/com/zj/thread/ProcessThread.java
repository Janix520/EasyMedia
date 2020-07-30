package com.zj.thread;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import cn.hutool.core.lang.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * 拉流转换推流处理线程
 * 
 * @author ZJ
 *
 */
@Slf4j
@Getter
@Setter
public class ProcessThread extends Thread {

	private String input;
	/**
	 * flv输出流
	 */
	private Map<String, ByteArrayOutputStream> outData;
	/**
	 * flv header
	 */
	private byte[] header = null;
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();
	/**
	 * 推拉流开关
	 */
	public boolean status = true;


	/**
	 * 拉流推流器
	 */
	private void frameRecord() throws Exception {

		// 拉流器
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input);
		// 超时时间
		grabber.setOption("stimoout", "15000000");
		grabber.setOption("threads", "2");
		// 如果为rtsp流，增加配置
		if ("rtsp".equals(input.substring(0, 4)) || "rtmp".equals(input.substring(0, 4))) {
			// 设置打开协议tcp / udp
			grabber.setOption("rtsp_transport", "tcp");
			// 设置缓存大小，提高画质、减少卡顿花屏
			// grabber.setOption("buffer_size", "1024000");
			// 设置视频比例
			// grabber.setAspectRatio(1.7777);
		} 
		
		//开始拉流
		grabber.start();
		
		// 推流器
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(),
	            grabber.getImageHeight(), grabber.getAudioChannels());
		recorder.setInterleaved(true);
//		// 降低编码延时 zerolatency零延迟（设置zerolatency可能会卡顿）
		recorder.setVideoOption("tune", "fastdecode");
//		// 提升编码速度 ultrafast最快
		recorder.setVideoOption("preset", "fast");
//		// 视频质量参数(详见 https://trac.ffmpeg.org/wiki/Encode/H.264)
		recorder.setVideoOption("crf", "29");
		recorder.setVideoOption("threads", "2");
		// 视频编码格式
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
		recorder.setFormat("flv");
		// 视频帧率
		recorder.setFrameRate(grabber.getFrameRate());
		// 视频比特率
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
//		recorder.setGopSize((int) (grabber.getFrameRate() * 2));
        
        // 音频比特率（有时候抓流抓不到比特率，这里给他固定）
        recorder.setAudioBitrate(192000);
        // 声道
		recorder.setAudioChannels(grabber.getAudioChannels());
		// 音频采样率
		recorder.setSampleRate(44100);
		// aac编码
		recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
		
		// 开始转换
		recordByFrame(grabber, recorder);
	}

	/**
	 * 拉流转化为推流
	 */
	private void recordByFrame(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder) throws Exception {
		try {
			recorder.start();
			Frame frame = null;
			while (status && (frame = grabber.grabFrame()) != null) {
				recorder.record(frame);
				
				if (header == null) {
					header = bos.toByteArray();
				} else {
					byte[] frameData = bos.toByteArray();
					for (ByteArrayOutputStream bos2 : outData.values()) {
						bos2.write(frameData);
					}
				}
				
	            bos.reset();
			}
			
			recorder.stop();
			recorder.release();
			grabber.stop();
		} finally {
			if (recorder != null) {
				recorder.stop();
			}
			if (grabber != null) {
				grabber.stop();
			}
		}
	}
	
	@Override
	public void run() {
//		String inputFile = "D:\\workspace-git\\EasyMedia\\video\\file.mp4";
		try {
			frameRecord();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * 新增客户端返回视频数据输出流
	 * @param sessionId
	 * @return
	 */
	public ByteArrayOutputStream addClient() {
		String id = UUID.fastUUID().toString().replace("-", "");
		if (outData == null) {
			outData = new HashMap<String, ByteArrayOutputStream>();
		}
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		outData.put(id, byteArrayOutputStream);
		return byteArrayOutputStream;
	}

	public ProcessThread(String input) {
		super();
		this.input = input;
	}
}
