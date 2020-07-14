package com.zj.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.zj.service.MediaService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 视频控制器
 * @author ZJ
 *
 */
@Slf4j
@Controller
public class VideoController {
	
	@Autowired MediaService mediaService;
	
	/**
	 * 
	 * @param url	源地址
	 * @param id	源地址唯一标识（表示同一个媒体）
	 */
	@RequestMapping("test")
	public void play(String url, String id, HttpServletResponse response) {
		try {
			//设置响应头
			response.setContentType("video/x-flv");
			response.setStatus(HttpServletResponse.SC_OK);
			//写出缓冲信息，并清空
			response.flushBuffer();
			
			//测试地址
//			id = "1";
//			url = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
//			url = "video/file.mp4";
			if (StrUtil.isBlank(url) || StrUtil.isBlank(id)) {
				log.info("url和id不能为空");
				return;
			}
			
			mediaService.playForHttp(url, id, response);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
