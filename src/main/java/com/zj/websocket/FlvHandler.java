package com.zj.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.zj.service.MediaService;

import cn.hutool.core.convert.Convert;

/**
 * websocket客户端推流控制器
 * @author ZJ
 *
 */
public class FlvHandler extends BinaryWebSocketHandler {
	
	@Autowired MediaService mediaService;

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		
		/*
		 * 后续做关闭处理，现在只是一个能播放视频的实现，还需完善
		 */
//		for (TcpServer tcpServer : MediaService.map.values()) {
//    		Map<String, ByteArrayOutputStream> outData = tcpServer.getStream().getOutData();
//    		outData.remove(session.getId());
//		}
	}

	/**
	 * 建立连接
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		 System.out.println("session："+session.getId());
		
		String url = Convert.toStr(session.getAttributes().get("url"));
		String id = Convert.toStr(session.getAttributes().get("id"));
		
		mediaService.playForWs(url, id, session);
		
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(message.getPayloadLength());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// TODO Auto-generated method stub
		super.handleTransportError(session, exception);
	}


}