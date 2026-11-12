package com.exam.springclass.stomp;

/**
 * 브라우저와 주고받는 메시지 한 건 구조화
 * {"sender": "홍길동", "msg": "안녕"}
 */
public record ChatMessageDto (
        String sender,
        String msg
) {
}
