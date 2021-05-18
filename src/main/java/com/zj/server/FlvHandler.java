package com.zj.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zj.entity.Camera;
import com.zj.service.CameraRepository;
import com.zj.service.MediaService;

import cn.hutool.core.util.StrUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

// http://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102
// ws://localhost:8866/live?url=rtsp://admin:VZCDOY@192.168.2.84:554/Streaming/Channels/102
@Slf4j
public class FlvHandler extends SimpleChannelInboundHandler<Object> {

	private MediaService mediaService;
	private WebSocketServerHandshaker handshaker;
	private CameraRepository cameraRepository;

	public FlvHandler(MediaService mediaService, CameraRepository cameraRepository) {
		super();
		this.mediaService = mediaService;
		this.cameraRepository = cameraRepository;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

		if (msg instanceof FullHttpRequest) {
			FullHttpRequest req = (FullHttpRequest) msg;
//			Map<String, String> parmMap = new RequestParser(msg).parse();
			QueryStringDecoder decoder = new QueryStringDecoder(req.uri());

			// 判断请求uri
			if (!"/live".equals(decoder.path())) {
				log.info("uri有误");
				sendError(ctx, HttpResponseStatus.BAD_REQUEST);
				return;
			}
			
			Map<String, String> parse = parseUrl(req.uri());

			String streamUrl = parse.get("url");
			String autoCloseStr = parse.get("autoClose");
			if (StrUtil.isBlank(streamUrl)) {
				log.info("url有误");
				sendError(ctx, HttpResponseStatus.BAD_REQUEST);
				return;
			}

			Camera camera = new Camera();
			camera.setUrl(streamUrl);

			//是否需要自动关闭流
			boolean autoClose = true;
			if (StrUtil.isNotBlank(autoCloseStr)) {
				if ("false".equals(autoCloseStr)) {
					autoClose = false;
					cameraRepository.add(camera);
				}
			}

			if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
				// http请求
				sendFlvReqHeader(ctx);
				mediaService.playForHttp(camera, ctx, autoClose);

			} else {
				// websocket握手，请求升级

				// 参数分别是ws地址，子协议，是否扩展，最大frame长度
				WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
						getWebSocketLocation(req), null, true, 5 * 1024 * 1024);
				handshaker = factory.newHandshaker(req);
				if (handshaker == null) {
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
				} else {
					handshaker.handshake(ctx.channel(), req);
					mediaService.playForWs(camera, ctx, autoClose);
				}
			}

		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketRequest(ctx, (WebSocketFrame) msg);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// 添加连接
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// 断开连接
	}

	/**
	 * ws握手地址
	 */
	private String getWebSocketLocation(FullHttpRequest request) {
		String location = request.headers().get(HttpHeaderNames.HOST) + request.uri();
		return "ws://" + location;
	}

	/**
	 * 发送req header，告知浏览器是flv格式
	 * 
	 * @param ctx
	 */
	private void sendFlvReqHeader(ChannelHandlerContext ctx) {
		HttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

		rsp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
				.set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv").set(HttpHeaderNames.ACCEPT_RANGES, "bytes")
				.set(HttpHeaderNames.PRAGMA, "no-cache").set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
				.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED).set(HttpHeaderNames.SERVER, "zhang");
		ctx.writeAndFlush(rsp);
	}

	/**
	 * 错误请求响应
	 * 
	 * @param ctx
	 * @param status
	 */
	private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				Unpooled.copiedBuffer("请求地址有误: " + status + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * websocket处理
	 * 
	 * @param ctx
	 * @param frame
	 */
	private void handleWebSocketRequest(ChannelHandlerContext ctx, WebSocketFrame frame) {
		// 关闭
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}

		// 握手PING/PONG
		if (frame instanceof PingWebSocketFrame) {
			ctx.write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		// 文本
		if (frame instanceof TextWebSocketFrame) {
			return;
		}

		if (frame instanceof BinaryWebSocketFrame) {
			return;
		}
	}
	
	private Map<String, String> parseUrl(String url) {
		String[] split = url.split("url=");
		String urlParent = split[1];
		Map<String, String> pMap = new HashMap<String, String>();
		//有autoClose
		if(urlParent.indexOf("autoClose") != -1) {
			
			String[] urlChild = urlParent.split("\\?");
			if(urlChild.length > 1) {
				String rUrl = urlChild[0];
				String fullParam = urlChild[1];
				
				String[] params = fullParam.split("&");
				
				StringBuffer sb = new StringBuffer();
				sb.append(rUrl);
				sb.append("?");
				
				for (String param : params) {
					if(param.indexOf("autoClose") == -1) {
						sb.append(param);
						sb.append("&");
					} else {
						String[] as = param.split("=");
						pMap.put(as[0], as[1]);		
					}
				}
				int lastIndexOf = sb.toString().lastIndexOf("&");
				String xxx = sb.toString().substring(0, lastIndexOf);
				pMap.put("url", xxx);
			}
			
		} else {
			pMap.put("url", urlParent);
		}
		return pMap;
	}

}
