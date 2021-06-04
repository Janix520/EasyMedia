package com.zj.init;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.PostConstruct;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import com.zj.common.MediaConstant;
import com.zj.dto.Camera;
import com.zj.server.MediaServer;
import com.zj.service.CameraRepository;
import com.zj.service.MediaService;

import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动流媒体
 * 
 * @author ZJ
 *
 */
@Slf4j
@Component
public class InitServer implements CommandLineRunner {

	@Value("${mediaserver.port}")
	private int port;

	@Autowired
	private MediaServer mediaServer;

	@Autowired
	private MediaService mediaService;

	@Autowired
	private CameraRepository cameraRepository;

	@Override
	public void run(String... args) throws Exception {
		initJsonCamera();
		mediaServer.start(new InetSocketAddress("0.0.0.0", port));
	}

	/**
	 * 初始化本地json
	 */
	public void initJsonCamera() {
		ApplicationHome applicationHome = new ApplicationHome();
		File file = new File(applicationHome.getDir() + File.separator + "camera.json");
		if (file.exists()) {
			log.info("发现camera.json，已启动自动拉流！");
			JSONArray readJSONArray = JSONUtil.readJSONArray(file, Charset.forName("utf-8"));
			List<Camera> list = JSONUtil.toList(readJSONArray, Camera.class);
			cameraRepository.readDataToMap(list);

			for (Camera camera : CameraRepository.cameraMap.values()) {
				// 区分不同媒体
				String mediaKey = MD5.create().digestHex(camera.getUrl());
				camera.setMediaKey(mediaKey);
				//已启用的自动拉流，不启用的不自动拉
				if(camera.isEnabledFlv()) {
					mediaService.playForApi(camera);
				}
			}
		} else {
			log.info("未发现camera.json，您可以通过restful api添加或删除流！");
		}
	}

	/**
	 * 提前初始化，可避免推拉流启动耗时太久
	 */
	@PostConstruct
	public void loadFFmpeg() {
		try {
			log.info("正在初始化资源，请稍等...");
			FFmpegFrameGrabber.tryLoad();
			FFmpegFrameRecorder.tryLoad();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
		} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
			e.printStackTrace();
		}

		/**
		 * 初始化ffmpeg路径
		 */
		String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
		System.setProperty(MediaConstant.ffmpegPathKey, ffmpeg);
		log.info("初始化成功");
	}
}
