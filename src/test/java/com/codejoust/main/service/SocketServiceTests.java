package com.codejoust.main.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.codejoust.main.config.WebSocketConfig;
import com.codejoust.main.dto.room.RoomDto;
import com.codejoust.main.dto.user.UserDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
public class SocketServiceTests {

    @Mock
    private SimpMessagingTemplate template;

    @Spy
    @InjectMocks
    private SocketService socketService;

    // Predefine user and room attributes.
    private static final String NICKNAME = "rocket";
    private static final String ROOM_ID = "012345";

    @Test
    public void sendSocketUpdate() {
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(ROOM_ID);
        UserDto userDto = new UserDto();
        userDto.setNickname(NICKNAME);
        roomDto.setHost(userDto);

        socketService.sendSocketUpdate(roomDto);
        verify(template).convertAndSend(
                eq(String.format(WebSocketConfig.SOCKET_LOBBY, roomDto.getRoomId())),
                eq(roomDto));
    }
    
}