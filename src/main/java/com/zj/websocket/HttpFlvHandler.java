package com.zj.websocket;

import java.util.List;

import com.zj.entity.Camera;
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

// http://172.16.64.92:1984/live/liveflv?deviceid=123abcdef32153421
// http://106.15.184.203:1984/live/liveflv?deviceid=123abcdef32153421
@Slf4j
public class HttpFlvHandler extends SimpleChannelInboundHandler<Object> {

	private MediaService mediaService;
	private WebSocketServerHandshaker handshaker;

	public HttpFlvHandler(MediaService mediaService) {
		super();
		this.mediaService = mediaService;
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

			List<String> list = decoder.parameters().get("url");

			if (!list.isEmpty() && StrUtil.isBlank(list.get(0))) {
				log.info("url有误");
				sendError(ctx, HttpResponseStatus.BAD_REQUEST);
				return;
			}

			Camera camera = new Camera();
			camera.setUrl(list.get(0));

			if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
				// http请求
				sendFlvReqHeader(ctx);
				mediaService.playForHttp(camera, ctx);
				
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
					mediaService.playForWs(camera, ctx);
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
	 * @param ctx
	 * @param frame
	 */
	private void handleWebSocketRequest(ChannelHandlerContext ctx, WebSocketFrame frame) {
        //关闭
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        //握手PING/PONG
        if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        //文本
        if (frame instanceof TextWebSocketFrame) {
            return;
        }

        if (frame instanceof BinaryWebSocketFrame) {
            return;
        }
    }
	

}
