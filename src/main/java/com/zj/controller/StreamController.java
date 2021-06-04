package com.zj.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zj.dto.Camera;
import com.zj.service.CameraRepository;
import com.zj.service.MediaService;
import com.zj.thread.MediaTransfer;
import com.zj.thread.MediaTransferFlvByFFmpeg;
import com.zj.thread.MediaTransferFlvByJavacv;
import com.zj.vo.CameraVo;
import com.zj.vo.Result;

import cn.hutool.crypto.digest.MD5;

/**
 * api管理接口
 * 后续可能改为使用数据库
 * @author ZJ
 *
 */
@RestController
public class StreamController {
	
	@Autowired
	private CameraRepository cameraRepository;
	@Autowired
	private MediaService mediaService;
	
	/**
	 * 新增流
	 * @param camera
	 * @return
	 */
	@RequestMapping("add")
	public Result add(Camera camera) {
		String res = cameraRepository.add(camera);
		return new Result(res, 200, true);
	}
	
	/**
	 * 编辑流
	 * @param camera
	 * @return
	 */
	@RequestMapping("edit")
	public Result edit(Camera camera) {
		String res = cameraRepository.edit(camera);
		return new Result(res, 200, true);
	}
	
	/**
	 * 删除流（会停止推流）
	 * @param camera
	 * @return
	 */
	@RequestMapping("del")
	public Result del(Camera camera) {
		mediaService.closeForApi(camera);
		cameraRepository.del(camera);
		return new Result("删除成功", 200, true);
	}
	
	/**
	 * 停止推流
	 * @param camera
	 * @return
	 */
	@RequestMapping("stop")
	public Result stop(Camera camera) {
		mediaService.closeForApi(camera);
		return new Result("停止推流", 200, true);
	}
	
	/**
	 * 开始推流
	 * @param camera
	 * @return
	 */
	@RequestMapping("start")
	public Result start(Camera camera) {
		boolean playForApi = mediaService.playForApi(camera);
		return new Result("开始推流", 200, playForApi);
	}
	
	/**
	 * 开启hls
	 * @param camera
	 * @return
	 */
	@RequestMapping("startHls")
	public Result startHls(Camera camera) {
//		boolean playForApi = mediaService.playForApi(camera);
		return new Result("开始推流", 200, true);
	}
	
	/**
	 * 关闭hls
	 * @param camera
	 * @return
	 */
	@RequestMapping("stopHls")
	public Result stopHls(Camera camera) {
//		mediaService.closeForApi(camera);
		return new Result("停止推流", 200, true);
	}
	
	/**
	 * 列表
	 * @return
	 */
	@RequestMapping("list")
	public Result list() {
		Collection<Camera> values = CameraRepository.cameraMap.values();
		
		List<CameraVo> list = new ArrayList<CameraVo>();
		for (Camera camera : values) {
			String digestHex = MD5.create().digestHex(camera.getUrl());
			
			MediaTransfer mediaConvert = MediaService.cameras.get(digestHex);
			
			CameraVo cameraVo = new CameraVo();
			
			cameraVo.setUrl(camera.getUrl());
			cameraVo.setRemark(camera.getRemark());
			if(mediaConvert instanceof MediaTransferFlvByJavacv) {
				MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
				cameraVo.setEnabledFlv(mediaTransferFlvByJavacv.isRunning());
				cameraVo.setMode("javacv");
				
			} if(mediaConvert instanceof MediaTransferFlvByFFmpeg) {
				MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;
				cameraVo.setEnabledFlv(mediaTransferFlvByFFmpeg.isRunning());
				cameraVo.setMode("ffmpeg");
			} 

			list.add(cameraVo);
		}
		return new Result("查询成功", 200, list);
	}

}
