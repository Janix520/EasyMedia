package com.zj.websocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 握手拦截器，握手成功才能连接
 * 
 * @author jzhang
 *
 */
@Slf4j
public class WebSocketInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
			Map<String, Object> map) throws Exception {
		// 处理握手
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
			String url = serverHttpRequest.getServletRequest().getParameter("url");
			String id = serverHttpRequest.getServletRequest().getParameter("id");

			if (StrUtil.isNotBlank(url) && StrUtil.isNotBlank(id)) {
				map.put("url", url);
				map.put("id", id);
				log.info("握手成功, id: {}, url:{}", id, url);
				return true;
			}
		}
		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse,
			WebSocketHandler webSocketHandler, Exception e) {

	}
}