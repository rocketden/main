package com.codejoust.main.task;

import java.time.Instant;
import java.util.TimerTask;

import com.codejoust.main.dto.game.GameNotificationDto;
import com.codejoust.main.exception.TimerError;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.game_object.NotificationType;
import com.codejoust.main.service.SocketService;

public class NotificationTimerTask extends TimerTask {

    private String roomId;

    private String timeRemaining;

    private final SocketService socketService;

    public NotificationTimerTask(SocketService socketService, String roomId,
        String timeRemaining) {
        this.socketService = socketService;
        this.roomId = roomId;
        this.timeRemaining = timeRemaining;

        /**
         * Handle potential errors for run(); roomId assumed to match
         * an existing game.
         */
        if (socketService == null || roomId == null || timeRemaining == null) {
            throw new ApiException(TimerError.NULL_SETTING);
        }
    }

	@Override
    public void run() {
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(null);
        notificationDto.setNotificationType(NotificationType.TIME_LEFT);
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(timeRemaining);

        // Get the Game DTO and send the relevant socket update.
        socketService.sendSocketUpdate(roomId, notificationDto);
    }
    
}
