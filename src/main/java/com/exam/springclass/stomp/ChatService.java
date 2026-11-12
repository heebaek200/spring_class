package com.exam.springclass.stomp;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public List<Chat> findByRoomId(Integer roomId) {
        return chatRepository.findByRoomId(roomId);
    }

    @Transactional
    public Chat save(Integer roomId, String sender, String msg) {
        Chat chat = Chat.builder()
                .roomId(roomId)
                .sender(sender)
                .msg(msg)
                .build();
        return chatRepository.save(chat);
    }
}
