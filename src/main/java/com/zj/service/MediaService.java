package com.zj.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import com.zj.thread.ProcessThread;

import cn.hutool.core.thread.ThreadUtil;

/**
 * 媒体服务
 * @author ZJ
 *
 */
@Service
public class MediaService {
	
	// 缓存线程
	public static Map<String, ProcessThread> map = new HashMap<String, ProcessThread>();

	/**
	 * 
	 * @param url 源地址
	 * @param id  源地址唯一标识（表示同一个媒体）
	 */
	public void playForHttp(String input, String id, HttpServletResponse response) {
		
		ProcessThread processThread = map.get(id);
		//新增的媒体需要进行推流初始化
		if (processThread == null) {
			processThread = new ProcessThread(input);
			//初始化推拉流
			map.put(id, processThread);
			ThreadUtil.execute(processThread);
		} 
		
		//创建客户端的输出流
		ByteArrayOutputStream byteArrayOutputStream = processThread.addClient();
		
		//发送头部
		sendHeaderForHttp(response, processThread);
		
		//发送数据
		sendAVDataForHttp(response, byteArrayOutputStream);
	}
	
	public void playForWs(String input, String id, WebSocketSession session) {
		ProcessThread processThread = map.get(id);
		//新增的媒体需要进行推流初始化
		if (processThread == null) {
			processThread = new ProcessThread(input);
			//初始化推拉流
			map.put(id, processThread);
			ThreadUtil.execute(processThread);
		} 
		
		//创建客户端的输出流
		ByteArrayOutputStream byteArrayOutputStream = processThread.addClient();
		
		//发送头部
		sendHeaderForWs(session, processThread);
		
		//发送数据
		sendAVDataForWs(session, byteArrayOutputStream);
	}
	
	/**
	 * 发送FLV header
	 * @param response
	 * @param stream
	 */
	private void sendHeaderForHttp(HttpServletResponse response, ProcessThread processThread) {
		try {
			//最多等三分钟，如果没有header则认为没取到视频，发送header后续要优化
			for (int i = 0; i < 1200; i++) {
				if (processThread.getHeader() != null) {
					response.getOutputStream().write(processThread.getHeader());
					break;
				}
				Thread.sleep(100);
			}
			
			/*
			 * 这里后续还要修改，如果没获取到header怎么和前段交互，后端线程怎么处理
			 */
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}
		
	}
	
	/**
	 * 发送视频数据
	 * @param response
	 * @param outData
	 */
	private void sendAVDataForHttp(HttpServletResponse response, ByteArrayOutputStream outData) {
		try {
			while (true) {
				if (outData.size() > 0) {
					response.getOutputStream().write(outData.toByteArray());
					outData.reset();
				} else {
					Thread.sleep(100);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * 发送FLV header
	 * @param response
	 * @param stream
	 */
	private void sendHeaderForWs(WebSocketSession session, ProcessThread processThread) {
		try {
			//最多等三分钟，如果没有header则认为没取到视频，发送header后续要优化
			for (int i = 0; i < 1200; i++) {
				if (processThread.getHeader() != null) {
					session.sendMessage(new BinaryMessage(processThread.getHeader()));
					break;
				}
				Thread.sleep(100);
			}
			
			/*
			 * 这里后续还要修改，如果没获取到header怎么和前段交互，后端线程怎么处理
			 */
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}
		
	}
	
	/**
	 * 发送视频数据
	 * @param response
	 * @param outData
	 */
	private void sendAVDataForWs(WebSocketSession session, ByteArrayOutputStream outData) {
		try {
			while (true) {
				if (outData.size() > 0) {
					session.sendMessage(new BinaryMessage(outData.toByteArray()));
					outData.reset();
				} else {
					Thread.sleep(100);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}
	}
}
