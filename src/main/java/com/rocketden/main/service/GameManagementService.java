package com.rocketden.main.service;

import java.util.HashMap;
import java.util.Map;

import com.rocketden.main.dao.RoomRepository;
import com.rocketden.main.game_object.Game;
import com.rocketden.main.model.Room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameManagementService {

    private final RoomRepository repository;
    private final SocketService socketService;
    private final LiveGameService liveGameService;
    private final NotificationService notificationService;
    private final SubmitService submitService;
    private Map<String, Game> currentGameMap;

    @Autowired
    protected GameManagementService(RoomRepository repository, SocketService socketService, LiveGameService liveGameService, NotificationService notificationService, SubmitService submitService) {
        this.repository = repository;
        this.socketService = socketService;
        this.liveGameService = liveGameService;
        this.notificationService = notificationService;
        this.submitService = submitService;
        currentGameMap = new HashMap<>();
    }

    public Game createAddGameFromRoom(Room room) {
        // TODO: Create the game from the room (or roomId).
        Game game = new Game();
        currentGameMap.put(game.getRoom().getRoomId(), game);
        return null;
    }

    protected Game getGameFromRoomId(String roomId) {
        return currentGameMap.get(roomId);
    }

    protected void removeGame(String roomId) {
        currentGameMap.remove(roomId);
    }

    // Test the submission and return a socket update.
    public GameDto testSubmission(String userId, String roomId) {

    }

    // Send a notification through a socket update.
    public NotificationDto sendNotification(List<String> userIdList) {

    }

    // Update a specific player's code.
    public void updateCode(String userId, PlayerCode playerCode) {
        return liveGameService.updateCode(new Player(), playerCode);
    }

}
