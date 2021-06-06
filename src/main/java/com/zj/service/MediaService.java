package com.zj.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zj.common.CacheMap;
import com.zj.common.ClientType;
import com.zj.dto.Camera;
import com.zj.thread.MediaTransfer;
import com.zj.thread.MediaTransferFlvByFFmpeg;
import com.zj.thread.MediaTransferFlvByJavacv;

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
	
	@Autowired
	private CameraRepository cameraRepository;

	/**
	 * 缓存流转换线程
	 */
	public static ConcurrentHashMap<String, MediaTransfer> cameras = new ConcurrentHashMap<>();
	
	
	/**
	 * http-flv播放
	 * @param camera
	 * @param ctx
	 */
	public void playForHttp(Camera camera, ChannelHandlerContext ctx) {
		doStore(camera);
		
		if (cameras.containsKey(camera.getMediaKey())) {
			MediaTransfer mediaConvert = cameras.get(camera.getMediaKey());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				//如果当前已经用ffmpeg，则重新拉流
				if(camera.isEnabledFFmpeg()) {
					mediaTransferFlvByJavacv.setRunning(false);
					cameras.remove(camera.getMediaKey());
					this.playForHttp(camera, ctx);
				} else {
					mediaTransferFlvByJavacv.addClient(ctx, ClientType.HTTP);
				}
			} else if (mediaConvert instanceof MediaTransferFlvByFFmpeg) {
				MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;
				//如果当前已经用javacv，则关闭再重新拉流
				if(!camera.isEnabledFFmpeg()) {
					mediaTransferFlvByFFmpeg.stop();
					cameras.remove(camera.getMediaKey());
					this.playForHttp(camera, ctx);
				} else {
					mediaTransferFlvByFFmpeg.addClient(ctx, ClientType.HTTP);
				}
			}
			
		} else {
			if(camera.isEnabledFFmpeg()) {
				MediaTransferFlvByFFmpeg mediaft = new MediaTransferFlvByFFmpeg(camera);
				mediaft.execute();
				cameras.put(camera.getMediaKey(), mediaft);
				mediaft.addClient(ctx, ClientType.HTTP);
			} else {
				MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(camera);
				cameras.put(camera.getMediaKey(), mediaConvert);
				ThreadUtil.execute(mediaConvert);
				mediaConvert.addClient(ctx, ClientType.HTTP);
			}
			
		}
	}

	/**
	 * ws-flv播放
	 * @param camera
	 * @param ctx
	 */
	public void playForWs(Camera camera, ChannelHandlerContext ctx) {
		doStore(camera);
		
		if (cameras.containsKey(camera.getMediaKey())) {
			MediaTransfer mediaConvert = cameras.get(camera.getMediaKey());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				//如果当前已经用ffmpeg，则重新拉流
				if(camera.isEnabledFFmpeg()) {
					mediaTransferFlvByJavacv.setRunning(false);
					cameras.remove(camera.getMediaKey());
					this.playForWs(camera, ctx);
				} else {
					mediaTransferFlvByJavacv.addClient(ctx, ClientType.WEBSOCKET);
				}
			} else if (mediaConvert instanceof MediaTransferFlvByFFmpeg) {
				MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;
				//如果当前已经用javacv，则关闭再重新拉流
				if(!camera.isEnabledFFmpeg()) {
					mediaTransferFlvByFFmpeg.stop();
					cameras.remove(camera.getMediaKey());
					this.playForWs(camera, ctx);
				} else {
					mediaTransferFlvByFFmpeg.addClient(ctx, ClientType.WEBSOCKET);
				}
			}
		} else {
			if(camera.isEnabledFFmpeg()) {
				MediaTransferFlvByFFmpeg mediaft = new MediaTransferFlvByFFmpeg(camera);
				mediaft.execute();
				cameras.put(camera.getMediaKey(), mediaft);
				mediaft.addClient(ctx, ClientType.WEBSOCKET);
			} else {
				MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(camera);
				cameras.put(camera.getMediaKey(), mediaConvert);
				ThreadUtil.execute(mediaConvert);
				mediaConvert.addClient(ctx, ClientType.WEBSOCKET);	
			}
		}
	}
	
	/**
	 * api播放
	 * @param camera
	 * @return 
	 */
	public boolean playForApi(Camera camera) {
		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());
		camera.setMediaKey(mediaKey);
		
		camera.setEnabledFlv(true);
		cameraRepository.edit(camera);
		
		MediaTransfer mediaTransfer = cameras.get(camera.getMediaKey());
		if (null == mediaTransfer) {
			if(camera.isEnabledFFmpeg()) {
				MediaTransferFlvByFFmpeg mediaft = new MediaTransferFlvByFFmpeg(camera);
				mediaft.execute();
				cameras.put(camera.getMediaKey(), mediaft);
			} else {
				MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(camera);
				cameras.put(camera.getMediaKey(), mediaConvert);
				ThreadUtil.execute(mediaConvert);
			}
		}
		
		//同步等待
		if(mediaTransfer instanceof MediaTransferFlvByJavacv) {
			MediaTransferFlvByJavacv mediaft = (MediaTransferFlvByJavacv) mediaTransfer;
			// 30秒还没true认为启动不了
			for (int i = 0; i < 60; i++) {
				if (mediaft.isRunning() && mediaft.isGrabberStatus() && mediaft.isRecorderStatus()) {
					return true;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		} else if (mediaTransfer instanceof MediaTransferFlvByFFmpeg) {
			MediaTransferFlvByFFmpeg mediaft = (MediaTransferFlvByFFmpeg) mediaTransfer;
			// 30秒还没true认为启动不了
			for (int i = 0; i < 60; i++) {
				if (mediaft.isRunning()) {
					return true;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}
		return false;
	}
	
	/**
	 * 关闭流
	 * @param camera
	 */
	public void closeForApi(Camera camera) {
		String mediaKey = MD5.create().digestHex(camera.getUrl());
		camera.setMediaKey(mediaKey);
		
		camera.setEnabledFlv(false);
		cameraRepository.edit(camera);
		
		if (cameras.containsKey(camera.getMediaKey())) {
			MediaTransfer mediaConvert = cameras.get(camera.getMediaKey());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				mediaTransferFlvByJavacv.setRunning(false);
				cameras.remove(camera.getMediaKey());
			} else if (mediaConvert instanceof MediaTransferFlvByFFmpeg) {
				MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;
				mediaTransferFlvByFFmpeg.stop();
				cameras.remove(camera.getMediaKey());
			}
		}
	}
	
	/**
	 * 是否要做存储
	 * @param camera
	 */
	public void doStore(Camera camera) {
		/**
		 * 不需要自动关闭，保存到json，后续可能会采用sqllite等本地数据库
		 */
		if (!camera.isAutoClose()) {
			cameraRepository.add(camera);
		} 
	}
	
}
