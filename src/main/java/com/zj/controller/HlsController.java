package com.zj.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zj.dto.Camera;
import com.zj.service.HlsService;
import com.zj.vo.Result;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.extern.slf4j.Slf4j;

/**
 * hls接口
 * 
 * @author ZJ
 *
 */
@Slf4j
@RestController
public class HlsController {
	
	@Autowired
	private HlsService hlsService;

	/**
	 * ts接收接口（回传，这里只占用网络资源，避免使用硬盘资源）
	 * @param request
	 * @param cameraId
	 * @param name
	 */
	@RequestMapping("record/{mediaKey}/{tsname}")
	public void name(HttpServletRequest request, @PathVariable("mediaKey") String mediaKey,
			@PathVariable("tsname") String tsname) {
		
		try {
			if(tsname.indexOf("m3u8") != -1) {
				hlsService.processHls(mediaKey, request.getInputStream());
			} else {
				hlsService.processTs(mediaKey, tsname, request.getInputStream());
			}
		} catch (IORuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param request
	 * @param mediaKey
	 * @param name
	 * @throws IOException 
	 */
	@RequestMapping("ts/{cameraId}/{tsName}")
	public void getts(HttpServletResponse response, @PathVariable("cameraId") String mediaKey,
			@PathVariable("tsName") String tsName) throws IOException {
		
		String tsKey = mediaKey.concat("-").concat(tsName);
		byte[] bs = HlsService.cacheTs.get(tsKey);
		if(null == bs) {
			response.setContentType("application/json");
			response.getOutputStream().write("尚未生成ts".getBytes("utf-8"));
			response.getOutputStream().flush();
			return;
		} else {
			response.getOutputStream().write(bs);
			response.setContentType("video/mp2t");
			response.getOutputStream().flush();
		}
		
	}
	
	/**
	 * hls播放接口
	 * @throws IOException
	 */
	@RequestMapping("hls")
	public void video(Camera camera, HttpServletResponse response)
			throws IOException {
		if (StrUtil.isBlank(camera.getUrl())) {
			response.setContentType("application/json");
			response.getOutputStream().write("参数有误".getBytes("utf-8"));
			response.getOutputStream().flush();
		} else {
			String mediaKey = MD5.create().digestHex(camera.getUrl());
			byte[] hls = HlsService.cacheM3u8.get(mediaKey);
			if(null == hls) {
				response.setContentType("application/json");
				response.getOutputStream().write("尚未生成m3u8".getBytes("utf-8"));
				response.getOutputStream().flush();
			} else {
				response.setContentType("application/vnd.apple.mpegurl");// application/x-mpegURL //video/mp2t ts;
				response.getOutputStream().write(hls);
				response.getOutputStream().flush();
			}
		}

	}
	
	@RequestMapping("stopHls")
	public Result stop(Camera camera) {
		hlsService.closeConvertToHls(camera);
		return new Result("停止推流", 200, true);
	}
	
	@RequestMapping("startHls")
	public Result start(Camera camera) {
		boolean startConvertToHls = hlsService.startConvertToHls(camera);
		return new Result("开始推流", 200, startConvertToHls);
	}
	
}
