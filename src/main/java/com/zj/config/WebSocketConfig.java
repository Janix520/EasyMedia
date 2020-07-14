package com.zj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.zj.websocket.FlvHandler;
import com.zj.websocket.WebSocketInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer, WebMvcConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(flvHandler(), "/flv")
        		.addInterceptors(new WebSocketInterceptor())
        		.setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler flvHandler() {
        return new FlvHandler();
    }

}