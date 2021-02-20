package com.zj.init;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.zj.websocket.MediaServer;

/**
 * 启动流媒体
 * @author ZJ
 *
 */
@Component
public class InitServer implements CommandLineRunner {
	
	@Value("${mediaserver.port}")
	private int port;

	@Autowired
	private MediaServer mediaServer;
	
	@Override
	public void run(String... args) throws Exception {
		mediaServer.start(new InetSocketAddress("0.0.0.0", port));
	}
}
