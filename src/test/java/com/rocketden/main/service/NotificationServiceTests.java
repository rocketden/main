package com.rocketden.main.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import com.rocketden.main.dto.game.GameNotificationDto;
import com.rocketden.main.dto.user.UserMapper;
import com.rocketden.main.game_object.NotificationType;
import com.rocketden.main.model.Room;
import com.rocketden.main.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTests {

    @Mock
    private SocketService socketService;

    @Mock
    private GameManagementService gameService;

    @Spy
    @InjectMocks
    private NotificationService notificationService;

    // Predefine user and room attributes.
    private static final String NICKNAME = "rocket";
    private static final String NICKNAME_2 = "rocketrocket";
    private static final String ROOM_ID = "012345";
    private static final String USER_ID = "098765";
    private static final String USER_ID_2 = "345678";

    // Predefine notification content.
    private static final String CONTENT = "[1, 2, 3]";

    @Test
    public void sendNotificationSuccess() throws Exception {
        Room room = new Room();
        room.setRoomId(ROOM_ID);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(NICKNAME_2);
        user.setUserId(USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        gameService.createAddGameFromRoom(room);

        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(LocalDateTime.now());
        notificationDto.setContent(CONTENT);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        GameNotificationDto result = notificationService.sendNotification(ROOM_ID, notificationDto);

        verify(socketService).sendSocketUpdate(eq(ROOM_ID), eq(notificationDto));
        assertEquals(notificationDto, result);
    }
    
}