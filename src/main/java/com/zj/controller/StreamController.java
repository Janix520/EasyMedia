package com.zj.controller;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zj.dto.Camera;
import com.zj.service.CameraRepository;
import com.zj.service.MediaService;
import com.zj.thread.MediaConvert;
import com.zj.thread.MediaTransfer;
import com.zj.thread.MediaTransferFlvByJavacv;
import com.zj.vo.Result;

import cn.hutool.crypto.digest.MD5;

/**
 * 
 * @author ZJ
 *
 */
@RestController
public class StreamController {
	
	@Autowired
	private CameraRepository cameraRepository;
	@Autowired
	private MediaService mediaService;
	
	@RequestMapping("add")
	public Result add(Camera camera) {
		String res = cameraRepository.add(camera);
		return new Result(res, 200, null);
	}
	@RequestMapping("edit")
	public Result edit(Camera camera) {
		String res = cameraRepository.edit(camera);
		return new Result(res, 200, null);
	}
	@RequestMapping("del")
	public Result del(Camera camera) {
		mediaService.closeForApi(camera);
		cameraRepository.del(camera);
		return new Result("删除成功", 200, null);
	}
	@RequestMapping("stop")
	public Result stop(Camera camera) {
		mediaService.closeForApi(camera);
		return new Result("停止推流", 200, null);
	}
	
	@RequestMapping("start")
	public Result start(Camera camera) {
		mediaService.playForApi(camera);
		return new Result("开始推流", 200, null);
	}
	
	@RequestMapping("list")
	public Result list() {
		Collection<Camera> values = CameraRepository.cameraMap.values();
		for (Camera camera : values) {
			String digestHex = MD5.create().digestHex(camera.getUrl());
			MediaTransfer mediaConvert = MediaService.cameras.get(digestHex);
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
//				MediaConvert mediaConvert = MediaService.cameras.get(digestHex);
				if(mediaConvert != null) {
					camera.setEnabledFlv(mediaTransferFlvByJavacv.isRunning());
				} else {
					camera.setEnabledFlv(false);
				}
			}

		}
		return new Result("查询成功", 200, values);
	}

}
