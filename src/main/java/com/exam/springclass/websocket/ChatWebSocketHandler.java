package com.exam.springclass.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket 채팅 핸들러
 * 연결이 열릴 때, 메시지가 올 때, 연결이 닫힐 때 스프링이 아래 메서드를 호출해 준다.
 *
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;

    // 메시지 규칙 접두사. (웹 브라우저와 미리 약속)
    private static final String PREFIX_CHAT = "CHAT:";
    private static final String PREFIX_MESSAGE = "MESSAGE:";
    private static final String PREFIX_ERROR = "ERROR:";

    // 연결된 탭마다 WebSocketSession 하나
    private final List<WebSocketSession> sessionList = new CopyOnWriteArrayList<>();

    /**
     * 연결이 열렸을 때 (101 응답 직후 동작)
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionList.add(session);
        log.info("연결 : {}, 현재 연결 수 : {}", session.getId(), sessionList.size());

    }

    /**
     * 브라우저가 메시지를 보냈을 때 (브라우저의 socket.send())
     * 접두사를 확인해서 메시지 종류를 나눈다.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 1. CHAT: 으로 시작하지 않으면 규칙 위반, 보낸 사람에게만 오류를 알린다.
        if (! payload.startsWith(PREFIX_CHAT)) {
            sendTo(session, PREFIX_ERROR + "알 수 없는 메시지 형식입니다.");
            return;
        }

        // 2. CHAT: 을 제외한 내용 추출
        String msg = payload.substring(PREFIX_CHAT.length()).trim();

        // 3. 빈 메시지를 거른다.
        if (msg.isEmpty()) {
            sendTo(session, PREFIX_ERROR + "빈 메시지는 보낼 수 없습니다.");
            return;
        }

        // 4. 저장한 뒤 모두에게 MESSAGE: 을 붙여보낸다.
        Chat chat = chatService.save(msg);
        broadcast(PREFIX_MESSAGE + chat.getMsg());
    }

    /**
     * 한 브라우저에만 보낸다 (오류, 알림용)
     */
    private void sendTo(WebSocketSession target, String text) {
        send(target, text);
    }

    /**
     * 실제 전송
     * 동기화 처리 필수
     * @param target 메시지를 받을 상대 탭의 WebSocket 연결
     */
    private void send(WebSocketSession target, String text) {
        synchronized (target) {
            try {
                if (target.isOpen()) {
                    target.sendMessage(new TextMessage(text));
                }
            } catch (IOException e) {
                log.warn("전송 실패 : {}", target.getId());
                sessionList.remove(target);
            }
        }
    }

    /**
     * 연결된 모든 브라우저에게 전송
     */
    private void broadcast(String text) {
        for (WebSocketSession target : sessionList) {
            send(target, text);
        }
    }

    /**
     * 연결이 닫혔을 때 (탭 닫기, 새로고침, 네트워크 오류 등)
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionList.remove(session);
        log.info("종료 : {}, 현재 연결 수 : {}", session.getId(), sessionList.size());
    }
}
