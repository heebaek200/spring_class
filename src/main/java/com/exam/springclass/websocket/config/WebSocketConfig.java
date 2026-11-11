package com.exam.springclass.websocket.config;

import com.exam.springclass.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableWebSocket // 필수
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws-chat");
        //        .setAllowedOrigins("*")                         // 허용 출처 (미지정 시 기본값=같은 출처-주소와 포트 동일-만 허용)
        //        .setAllowedOrigins("http://localhost:5173")     // 추가 예시
        //        .setAllowedOrigins("https://app.example.com");



    }
}
