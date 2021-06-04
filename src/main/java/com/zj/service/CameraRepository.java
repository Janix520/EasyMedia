package com.zj.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import com.zj.dto.Camera;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 后期考虑用sqllite数据库，json只适合小数据量
 * 
 * @author ZJ
 *
 */
@Slf4j
@Service
public class CameraRepository {

	// 存储流
	public static LinkedHashMap<String, Camera> cameraMap = new LinkedHashMap<String, Camera>();
	private static File file;
	static {
		ApplicationHome applicationHome = new ApplicationHome();
		file = new File(applicationHome.getDir() + File.separator + "camera.json");
	}

	/**
	 * 新增流
	 * 
	 * @param camera
	 * @return 
	 */
	public String add(Camera camera) {
		String digestHex = MD5.create().digestHex(camera.getUrl());
		camera.setMediaKey(digestHex);

		if (cameraMap.containsKey(digestHex)) {
			log.info("\r\n已存在流>>>\r\n{}\r\n", camera.getUrl());
			return "已经存在流";
		} else {
			cameraMap.put(digestHex, camera);
			List<Camera> newData = new ArrayList<Camera>();
			for (Camera newCamera : cameraMap.values()) {
				newData.add(newCamera);
			}
			writeData(newData);
			return "新增成功";
		}
	}

	public String edit(Camera camera) {
		String digestHex = MD5.create().digestHex(camera.getUrl());
		camera.setMediaKey(digestHex);

		if (cameraMap.containsKey(digestHex)) {
			cameraMap.put(digestHex, camera);
			List<Camera> newData = new ArrayList<Camera>();
			for (Camera newCamera : cameraMap.values()) {
				newData.add(newCamera);
			}
			writeData(newData);
			return "编辑成功";
		} else {
			log.info("未找到您要编辑的流");
			return "未找到您要编辑的流";
		}
	}

	/**
	 * 删除流
	 * @param camera
	 */
	public void del(Camera camera) {
		String digestHex = MD5.create().digestHex(camera.getUrl());

		if (cameraMap.containsKey(digestHex)) {
			cameraMap.remove(digestHex);
			List<Camera> newData = new ArrayList<Camera>();
			for (Camera newCamera : cameraMap.values()) {
				newData.add(newCamera);
			}
			writeData(newData);
		} else {
//			log.info("未找到您要删除的流");
		}
	}

	/**
	 * 读取到缓存
	 */
	public void readDataToMap(List<Camera> list) {
		MD5 md5Digest = MD5.create();
		boolean needUpdate = false;
		for (Camera camera : list) {
			String digestHex = md5Digest.digestHex(camera.getUrl());
			
			if (cameraMap.containsKey(digestHex)) {
				needUpdate = true;
			}
			
			cameraMap.put(digestHex, camera);
		}

		// 自动去重
		if (needUpdate) {
			Collection<Camera> values = cameraMap.values();
			List<Camera> newData = new ArrayList<Camera>();
			for (Camera camera : values) {
				newData.add(camera);
			}
			writeData(newData);
		}
	}

	/**
	 * 写入json文件
	 */
	private synchronized void writeData(List<Camera> list) {
		try {
			FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(list), file);
			log.info("已更新camera.json  {}", DateUtil.now());
		} catch (IORuntimeException e) {
			e.printStackTrace();
			log.error("写入json文件错误", e);
		}
	}
}
