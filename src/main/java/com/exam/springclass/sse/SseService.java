package com.exam.springclass.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE
 * 브라우저가 한 번 연결을 열어두면, 서버가 그 연결로 메시지를 계속 밀어 넣는다.
 * 방향은 서버 --> 브라우저 한쪽뿐이다.
 */
@Slf4j
@Service
public class SseService {

    // 연결된 탭마다 SseEmitter하나 맵핑됨
    // 멀티쓰레드이므로 ArrayList 대신 CopyOnWriteArrayList 사용
    private final List<SseEmitter> emitterList = new CopyOnWriteArrayList<>();

    // 연결 유지 시간 (10분)
    private static final long TIMEOUT = 10L * 60 * 1_000;

    // 브라우저가 연결을 요청하면 SseEmitter를 생성하여 List에 보관한다.
    public SseEmitter connect() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitterList.add(emitter);

        // 연결이 정상 종료되면 목록에서 뺀다.
        emitter.onCompletion(() -> emitterList.remove(emitter));

        // 시간이 지나면 목록에서 빼고 연결을 마무리한다.
        emitter.onTimeout(() -> {
            emitterList.remove(emitter);
            emitter.complete();
        });

        // 네트워크 오류 등으로 끊기면 목록에서 뺀다.
        emitter.onError(e -> emitterList.remove(emitter));

        // 연결 확인 이벤트를 브라우저 측으로 1회 발생시킴
        // ( 아무것도 보내지 않은채 가만히 있으면 TIMEOUT 발생, 스프링이 503응답으로 종료시킴 )
        // 이 때 응답 헤더도 함께 전송되어 브라우저의 onopen이 실행된다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("연결 성공"));
        } catch (IOException e) {
            emitterList.remove(emitter);
        }

        log.info("SSE 연결 추가, 현재 연결 수 : {}", emitterList.size());

        return emitter; // 브라우저의 자바스크립트 측 코드로 던짐
    }

    // 연결된 모든 브라우저에 "chat" 이벤트로 메시지를 보낸다.
    public void send(String msg) {
        emitterList.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("chat").data(msg));
            } catch (IOException e) {
                // 이미 끊긴 연결이면 보내기에 실패이므로 목록에서 뺀다.
                emitterList.remove(emitter);
            }
        });
    }

}
