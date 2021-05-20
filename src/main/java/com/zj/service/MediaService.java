package com.zj.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.zj.entity.Camera;
import com.zj.thread.MediaConvert;
import com.zj.thread.MediaRecodeOrTransfer;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;
import io.netty.channel.ChannelHandlerContext;

/**
 * 媒体服务
 * 
 * @author ZJ
 *
 */
@Service
public class MediaService {

	// 缓存流转换线程
	public static ConcurrentHashMap<String, MediaRecodeOrTransfer> cameras = new ConcurrentHashMap<>();

	/**
	 * 
	 * @param url 源地址
	 */
	public void playForHttp(Camera camera, ChannelHandlerContext ctx, boolean autoClose) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaRecodeOrTransfer mediaConvert = cameras.get(mediaKey);
			cameras.put(mediaKey, mediaConvert);
			mediaConvert.addHttpClient(ctx);
		} else {
			MediaRecodeOrTransfer mediaConvert = new MediaRecodeOrTransfer(camera, autoClose);
			cameras.put(mediaKey, mediaConvert);
			ThreadUtil.execute(mediaConvert);
			mediaConvert.addHttpClient(ctx);
		}
	}

	public void playForWs(Camera camera, ChannelHandlerContext ctx, boolean autoClose) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaRecodeOrTransfer mediaConvert = cameras.get(mediaKey);
			cameras.put(mediaKey, mediaConvert);
			mediaConvert.addWsClient(ctx);
		} else {
			MediaRecodeOrTransfer mediaConvert = new MediaRecodeOrTransfer(camera, autoClose);
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
			MediaRecodeOrTransfer mediaConvert = new MediaRecodeOrTransfer(camera, false);
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
			mediaConvert.setRuning(false);
			cameras.remove(mediaKey);
		}
	}


}
