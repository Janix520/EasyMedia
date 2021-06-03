package com.zj.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zj.entity.Camera;
import com.zj.thread.MediaConvert;
import com.zj.thread.MediaRecodeOrTransfer;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;
import io.netty.channel.ChannelHandlerContext;

/**
 * 媒体服务，支持全局网络超时、读写超时、无人拉流持续时长自动关闭流等配置
 * 
 * @author ZJ
 * @author eguid
 *
 */
@Service
public class MediaService {

	// 缓存流转换线程
	public static ConcurrentHashMap<String, MediaRecodeOrTransfer> cameras = new ConcurrentHashMap<>();

	/**
	 * 网络超时
	 */
	@Value("${mediaserver.netTimeout}")
	private String netTimeout = "15000000";
	/**
	 * 读写超时
	 */
	@Value("${mediaserver.readOrWriteTimeout}")
	private String readOrWriteTimeout = "15000000";

	/**
	 * 无人拉流观看是否自动关闭流
	 */
	@Value("${mediaserver.autoClose}")
	private boolean autoClose=true;

	/**
	 * 无人拉流观看持续多久自动关闭，1分钟
	 */
	@Value("${mediaserver.autoClose.noClientsDuration}")
	private long noClientsDuration=60000;
	/**
	 *http-flv播放
	 * @param camera
	 * @param ctx
	 * @param isAutoClose
	 */
	public void playForHttp(Camera camera, ChannelHandlerContext ctx, Boolean isAutoClose) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaRecodeOrTransfer mediaConvert = cameras.get(mediaKey);
			cameras.put(mediaKey, mediaConvert);
			mediaConvert.addHttpClient(ctx);
		} else {
			MediaRecodeOrTransfer mediaConvert = new MediaRecodeOrTransfer(camera, isAutoClose==null?autoClose:isAutoClose,noClientsDuration, netTimeout,readOrWriteTimeout);
			cameras.put(mediaKey, mediaConvert);
			ThreadUtil.execute(mediaConvert);
			mediaConvert.addHttpClient(ctx);
		}
	}

	public void playForWs(Camera camera, ChannelHandlerContext ctx, Boolean isAutoClose) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaRecodeOrTransfer mediaConvert = cameras.get(mediaKey);
			cameras.put(mediaKey, mediaConvert);
			mediaConvert.addWsClient(ctx);
		} else {
			MediaRecodeOrTransfer mediaConvert = new MediaRecodeOrTransfer(camera, isAutoClose==null?autoClose:isAutoClose,noClientsDuration, netTimeout,readOrWriteTimeout);
			cameras.put(mediaKey, mediaConvert);
			ThreadUtil.execute(mediaConvert);
			mediaConvert.addWsClient(ctx);
		}
	}
	
	/**
	 * api拉流
	 * @param camera
	 */
	public void playForApi(Camera camera) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (!cameras.containsKey(mediaKey)) {
			MediaRecodeOrTransfer mediaConvert = new MediaRecodeOrTransfer(camera, autoClose,noClientsDuration,netTimeout,readOrWriteTimeout);
			cameras.put(mediaKey, mediaConvert);
			ThreadUtil.execute(mediaConvert);
		}
	}
	
	/**
	 * 关闭流
	 * @param camera
	 */
	public void closeForApi(Camera camera) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaRecodeOrTransfer mediaConvert = cameras.get(mediaKey);
			mediaConvert.setRunning(false);
			cameras.remove(mediaKey);
		}
	}
}
