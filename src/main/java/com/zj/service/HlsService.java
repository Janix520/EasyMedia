package com.zj.service;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.zj.common.CacheMap;
import com.zj.dto.Camera;
import com.zj.thread.MediaTransferHls;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;

/**
 * 处理hls
 * @author ZJ
 *
 */
@Service
public class HlsService {
	
	@Autowired
	private Environment env;
	
	/**
	 * 
	 */
	public static ConcurrentHashMap<String, MediaTransferHls> cameras = new ConcurrentHashMap<>(); 
	
	/**
	 * 定义ts缓存10秒
	 */
	public static CacheMap<String, byte[]> cacheTs = new CacheMap<>(10000);
	public static CacheMap<String, byte[]> cacheM3u8 = new CacheMap<>(10000);
	
	/**
	 * 保存ts
	 * @param camera
	 */
	public void processTs(String mediaKey, String tsName, InputStream in) {
		byte[] readBytes = IoUtil.readBytes(in);
		String tsKey = mediaKey.concat("-").concat(tsName);
		cacheTs.put(tsKey, readBytes);
	}

	/**
	 * 保存hls
	 * @param mediaKey
	 * @param in
	 */
	public void processHls(String mediaKey, InputStream in) {
		byte[] readBytes = IoUtil.readBytes(in);
		cacheM3u8.put(mediaKey, readBytes);
	}

	/**
	 * 关闭hls切片
	 * 
	 * @param camera
	 */
	public void closeConvertToHls(Camera camera) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaTransferHls mediaTransferHls = cameras.get(mediaKey);
			mediaTransferHls.stop();
			cameras.remove(mediaKey);
			cacheTs.remove(mediaKey);
			cacheM3u8.remove(mediaKey);
		}
	}

	/**
	 * 开始hls切片
	 * 
	 * @param camera
	 * @return
	 */
	public boolean startConvertToHls(Camera camera) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());
		camera.setMediaKey(mediaKey);

		MediaTransferHls mediaTransferHls = cameras.get(mediaKey);

		if (null == mediaTransferHls) {
			mediaTransferHls = new MediaTransferHls(camera, Convert.toInt(env.getProperty("server.port")));
			cameras.put(mediaKey, mediaTransferHls);
			mediaTransferHls.execute();
		}

		// 15秒还没true认为启动不了
		for (int i = 0; i < 30; i++) {
			if (mediaTransferHls.isRunning()) {
				return true;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}

}
