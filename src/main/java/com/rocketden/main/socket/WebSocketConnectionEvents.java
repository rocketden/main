package com.rocketden.main.socket;

import java.util.LinkedList;
import java.util.Map;

import com.rocketden.main.dao.UserRepository;
import com.rocketden.main.dto.room.RoomDto;
import com.rocketden.main.dto.room.RoomMapper;
import com.rocketden.main.model.Room;
import com.rocketden.main.model.User;
import com.rocketden.main.service.RoomService;
import com.rocketden.main.service.SocketService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Configuration
@EnableWebSocketMessageBroker
@Transactional
public class WebSocketConnectionEvents {

    private final UserRepository userRepository;
    private final SocketService socketService;
    private final RoomService roomService;

    private static final String CONNECT_MESSAGE = "simpConnectMessage";
    private static final String NATIVE_HEADERS = "nativeHeaders";
    public static final String USER_ID_KEY = "userId";

    @Autowired
    public WebSocketConnectionEvents(UserRepository userRepository, SocketService socketService, RoomService roomService) {
        this.userRepository = userRepository;
        this.socketService = socketService;
        this.roomService = roomService;
    }

    @EventListener
    @SuppressWarnings("unchecked")
    public void onSocketConnected(SessionConnectedEvent event) {
        /**
         * Grab the custom headers on connection. Unchecked cast warnings are
         * suppressed: see method header.
         */
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        GenericMessage<byte[]> genericMessage = 
            (GenericMessage<byte[]>) event.getMessage().getHeaders().get(CONNECT_MESSAGE);
        Map<String, LinkedList<String>> customHeaderMap = 
            (Map<String, LinkedList<String>>) genericMessage.getHeaders().get(NATIVE_HEADERS);

        // Retrieve the ID of the user to update. 
        if (!customHeaderMap.containsKey(USER_ID_KEY)) {
            // If user ID is not passed in through headers, return.
            return;
        }
        String userId = customHeaderMap.get(USER_ID_KEY).get(0);

        // Get the unique auto-generated session ID for this connection.
        String sessionId = sha.getSessionId();

        // Update the session ID of the relevant user, if it is found.
        User user = userRepository.findUserByUserId(userId);
        if (user != null) {
            user.setSessionId(sessionId);
            userRepository.save(user);

            // Get room and send socket update.
            Room room = user.getRoom();
            RoomDto roomDto = RoomMapper.toDto(room);
            socketService.sendSocketUpdate(roomDto);
        }
    }

    @EventListener
    public void onSocketDisconnected(SessionDisconnectEvent event) {
        // Grab the custom headers on connection.
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        sha.getSessionId();
        String sessionId = sha.getSessionId();

        // Remove the user from the database and send socket update, if user exists.
        User user = userRepository.findUserBySessionId(sessionId);
        if (user != null) {
            user.setSessionId(null);
            userRepository.save(user);

            // Get room, conditionally update the host, and send socket update.
            Room room = user.getRoom();
            roomService.conditionallyUpdateRoomHost(room.getRoomId(), user);
            RoomDto roomDto = RoomMapper.toDto(room);
            socketService.sendSocketUpdate(roomDto);
        }
    }
}
