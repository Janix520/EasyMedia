package com.zj.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.zj.entity.Camera;
import com.zj.thread.MediaConvert;

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
	public static ConcurrentHashMap<String, MediaConvert> cameras = new ConcurrentHashMap<>();

	/**
	 * 
	 * @param url 源地址
	 * @param id  源地址唯一标识（表示同一个媒体）
	 */
	public void playForHttp(Camera camera, ChannelHandlerContext ctx) {

		// 区分不媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaConvert mediaConvert = cameras.get(mediaKey);
			cameras.put(mediaKey, mediaConvert);
			mediaConvert.addHttpClient(ctx);
		} else {
			MediaConvert mediaConvert = new MediaConvert(camera);
			cameras.put(mediaKey, mediaConvert);
			ThreadUtil.execute(mediaConvert);
			mediaConvert.addHttpClient(ctx);
		}
	}

	public void playForWs(Camera camera, ChannelHandlerContext ctx) {

		// 区分不媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaConvert mediaConvert = cameras.get(mediaKey);
			cameras.put(mediaKey, mediaConvert);
			mediaConvert.addWsClient(ctx);
		} else {
			MediaConvert mediaConvert = new MediaConvert(camera);
			cameras.put(mediaKey, mediaConvert);
			ThreadUtil.execute(mediaConvert);
			mediaConvert.addWsClient(ctx);
		}
	}

}
