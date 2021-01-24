package com.rocketden.main.service;

import java.util.Timer;
import java.util.Map.Entry;

import com.rocketden.main.dto.game.GameNotificationDto;
import com.rocketden.main.game_object.GameTimer;
import com.rocketden.main.util.NotificationTimerTask;

import org.springframework.stereotype.Service;

/**
 * Class to handle sending out notifications.
 */
@Service
public class NotificationService {

    private final SocketService socketService;

    protected NotificationService(SocketService socketService) {
        this.socketService = socketService;
    }
    
    // Send a notification through a socket update.
    public GameNotificationDto sendNotification(String roomId, GameNotificationDto notificationDto) {
        socketService.sendSocketUpdate(roomId, notificationDto);
        return notificationDto;
    }

    public void scheduleTimeLeftNotifications(String roomId, Long time) {
        // Create notifications for different "time left" milestones.
        for (Entry<Long, String> timeLeft : GameTimer.TIME_LEFT_DURATION_CONTENT.entrySet()) {
            if (timeLeft.getKey() < time) {
                Timer timer = new Timer();
                NotificationTimerTask notificationTimerTask =
                    new NotificationTimerTask(socketService, roomId,
                    timeLeft.getValue());
                timer.schedule(notificationTimerTask,
                    (time - timeLeft.getKey()) * 1000);
            }
        }
    }
}
