package com.codejoust.main.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.codejoust.main.dao.RoomRepository;
import com.codejoust.main.dto.game.EndGameRequest;
import com.codejoust.main.dto.game.GameDto;
import com.codejoust.main.dto.game.GameMapper;
import com.codejoust.main.dto.game.GameNotificationDto;
import com.codejoust.main.dto.game.PlayAgainRequest;
import com.codejoust.main.dto.game.StartGameRequest;
import com.codejoust.main.dto.game.SubmissionDto;
import com.codejoust.main.dto.game.SubmissionRequest;
import com.codejoust.main.dto.room.RoomDto;
import com.codejoust.main.dto.room.RoomMapper;
import com.codejoust.main.dto.user.UserMapper;
import com.codejoust.main.exception.GameError;
import com.codejoust.main.exception.ProblemError;
import com.codejoust.main.exception.RoomError;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.GameTimer;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.PlayerCode;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.task.CreateGameReportTask;
import com.codejoust.main.task.EndGameTimerTask;
import com.codejoust.main.util.Utility;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class GameManagementService {

    private final RoomRepository repository;
    private final SocketService socketService;
    private final LiveGameService liveGameService;
    private final NotificationService notificationService;
    private final SubmitService submitService;
    private final ProblemService problemService;
    private final ReportService reportService;
    private final Map<String, Game> currentGameMap;

    @Autowired
    protected GameManagementService(RoomRepository repository,
                                    SocketService socketService,
                                    LiveGameService liveGameService,
                                    NotificationService notificationService,
                                    SubmitService submitService,
                                    ProblemService problemService,
                                    ReportService reportService) {
        this.repository = repository;
        this.socketService = socketService;
        this.liveGameService = liveGameService;
        this.notificationService = notificationService;
        this.submitService = submitService;
        this.problemService = problemService;
        this.reportService = reportService;
        currentGameMap = new HashMap<>();
    }

    protected Game getGameFromRoomId(String roomId) {
        Game game = currentGameMap.get(roomId);
        if (game == null) {
            throw new ApiException(GameError.NOT_FOUND);
        }

        return game;
    }

    protected void removeGame(String roomId) {
        currentGameMap.remove(roomId);
    }

    public GameDto getGameDtoFromRoomId(String roomId) {
        return GameMapper.toDto(getGameFromRoomId(roomId));
    }

    // When host starts the game, redirect everyone and initialize the game state
    public RoomDto startGame(String roomId, StartGameRequest request) {
        Room room = repository.findRoomByRoomId(roomId);

        // If requested room does not exist in database, throw an exception.
        if (room == null) {
            throw new ApiException(RoomError.NOT_FOUND);
        }

        // If user making request is not the host, throw an exception.
        User initiator = UserMapper.toEntity(request.getInitiator());
        if (!room.getHost().equals(initiator)) {
            throw new ApiException(RoomError.INVALID_PERMISSIONS);
        }

        // Initialize game state
        createAddGameFromRoom(room);

        room.setActive(true);
        repository.save(room);

        RoomDto roomDto = RoomMapper.toDto(room);
        socketService.sendSocketUpdate(roomDto);
        return roomDto;
    }

    public RoomDto playAgain(String roomId, PlayAgainRequest request) {
        Game game = getGameFromRoomId(roomId);

        if (!isGameOver(game)) {
            throw new ApiException(GameError.GAME_NOT_OVER);
        }

        // Get up to date room using repository
        Room room = repository.findRoomByRoomId(game.getRoom().getRoomId());

        if (room == null) {
            throw new ApiException(RoomError.NOT_FOUND);
        }

        User initiator = UserMapper.toEntity(request.getInitiator());
        if (!room.getHost().equals(initiator)) {
            throw new ApiException(GameError.INVALID_PERMISSIONS);
        }

        // Set all users to be disconnected
        room.getUsers().forEach((user) -> user.setSessionId(null));

        // Change room to be no longer active
        room.setActive(false);
        repository.save(room);

        // Notify users to play again
        game.setPlayAgain(true);
        socketService.sendSocketUpdate(GameMapper.toDto(game));

        return RoomMapper.toDto(room);
    }

    // Initialize and add a game object from a room object, start game timer
    public void createAddGameFromRoom(Room room) {
        Game game = GameMapper.fromRoom(room);
        Long time = room.getDuration();

        // If specific problems specified, use those instead of difficulty setting
        List<Problem> problems = game.getProblems();
        problems.addAll(room.getProblems());

        // Otherwise, fetch random problems
        if (problems.size() == 0) {
            List<Problem> randomProblems = problemService.getProblemsFromDifficulty(room.getDifficulty(), room.getNumProblems());
            problems.addAll(randomProblems);

            if (problems.size() < room.getNumProblems()) {
                throw new ApiException(ProblemError.NOT_ENOUGH_FOUND);
            }
        }

        setStartGameTimer(game, time);
        currentGameMap.put(room.getRoomId(), game);
        notificationService.scheduleTimeLeftNotifications(game, time);
    }

    // Set and start the Game Timer.
    public void setStartGameTimer(Game game, Long duration) {
        GameTimer gameTimer = new GameTimer(duration);
        game.setGameTimer(gameTimer);

        // Schedule the game to end after <duration> seconds.
        EndGameTimerTask endGameTimerTask = new EndGameTimerTask(this, socketService, game);
        gameTimer.getTimer().schedule(endGameTimerTask, duration * 1000);
    }

    // Test the submission, return the results, and send a socket update
    public SubmissionDto runCode(String roomId, SubmissionRequest request) {
        Game game = getGameFromRoomId(roomId);

        if (request.getInitiator() == null || request.getCode() == null || request.getLanguage() == null || request.getInput() == null) {
            throw new ApiException(GameError.EMPTY_FIELD);
        }

        if (request.getProblemIndex() >= game.getProblems().size() || request.getProblemIndex() < 0) {
            throw new ApiException(GameError.BAD_SETTING);
        }

        String initiatorUserId = request.getInitiator().getUserId();
        if (!game.getPlayers().containsKey(initiatorUserId)) {
            throw new ApiException(GameError.INVALID_PERMISSIONS);
        }

        return submitService.runCode(game, request);
    }

    // Test the submission, return the results, and send a socket update
    public SubmissionDto submitSolution(String roomId, SubmissionRequest request) {
        Game game = getGameFromRoomId(roomId);

        if (request.getInitiator() == null || request.getCode() == null || request.getLanguage() == null) {
            throw new ApiException(GameError.EMPTY_FIELD);
        }

        if (request.getProblemIndex() >= game.getProblems().size() || request.getProblemIndex() < 0) {
            throw new ApiException(GameError.BAD_SETTING);
        }

        String initiatorUserId = request.getInitiator().getUserId();
        if (!game.getPlayers().containsKey(initiatorUserId)) {
            throw new ApiException(GameError.INVALID_PERMISSIONS);
        }

        SubmissionDto submissionDto = submitService.submitSolution(game, request);

        if (isGameOver(game)) {
            handleEndGame(game);
        }

        // Send socket update with latest leaderboard info
        socketService.sendSocketUpdate(GameMapper.toDto(game));
        
        return submissionDto;
    }

    // Send a notification through a socket update.
    public GameNotificationDto sendNotification(String roomId, GameNotificationDto notificationDto) {
        Game game = getGameFromRoomId(roomId);

        //  The notification type must be present.
        if (notificationDto.getNotificationType() == null) {
            throw new ApiException(GameError.EMPTY_FIELD);
        }
        
        // If initiator exists, they must be in the game.
        if (notificationDto.getInitiator() != null
            && !game.getPlayers().containsKey(notificationDto.getInitiator().getUserId())) {
            throw new ApiException(GameError.USER_NOT_IN_GAME);
        }
        
        // If initiator doesn't exist, the notification must not require one.
        if (notificationDto.getInitiator() == null
            && Utility.initiatorNotifications.contains(notificationDto.getNotificationType())) {
            throw new ApiException(GameError.NOTIFICATION_REQUIRES_INITIATOR);
        }
        
        // If content doesn't exist, the notification must not require any.
        if (notificationDto.getContent() == null
            && Utility.contentNotifications.contains(notificationDto.getNotificationType())) {
            throw new ApiException(GameError.NOTIFICATION_REQUIRES_CONTENT);       
        }

        return notificationService.sendNotification(roomId, notificationDto);
    }

    // Update a specific player's code.
    public void updateCode(String roomId, String userId, PlayerCode playerCode) {
        Game game = getGameFromRoomId(roomId);

        // The user must be present in the game.
        if (userId != null && !game.getPlayers().containsKey(userId)) {
            throw new ApiException(GameError.USER_NOT_IN_GAME);
        }
        
        // The player must have code to update (even if empty string).
        if (playerCode == null) {
            throw new ApiException(GameError.EMPTY_FIELD);
        }

        liveGameService.updateCode(game.getPlayers().get(userId), playerCode);
    }

    public GameDto manuallyEndGame(String roomId, EndGameRequest request) {
        Game game = getGameFromRoomId(roomId);

        User initiator = UserMapper.toEntity(request.getInitiator());
        if (!game.getRoom().getHost().equals(initiator)) {
            throw new ApiException(GameError.INVALID_PERMISSIONS);
        }

        game.setGameEnded(true);
        handleEndGame(game);

        GameDto gameDto = GameMapper.toDto(game);
        socketService.sendSocketUpdate(gameDto);

        return gameDto;
    }

    public void handleEndGame(Game game) {
        // Cancel all previously scheduled timers
        GameTimer gameTimer = game.getGameTimer();
        gameTimer.getTimer().cancel();

        for (Timer timer : gameTimer.getNotificationTimers()) {
            timer.cancel();
        }

        CreateGameReportTask createGameReportTask = new CreateGameReportTask(reportService, game);
        Timer createGameReportTimer = new Timer();
        createGameReportTimer.schedule(createGameReportTask, GameTimer.DURATION_1 * 1000);
    }

    protected boolean isGameOver(Game game) {
        return game.getGameEnded() || game.getAllSolved() || (game.getGameTimer() != null && game.getGameTimer().isTimeUp());
    }

    // Update people's socket active status
    public void conditionallyUpdateSocketInfo(Room room, User user) {
        Game game = currentGameMap.get(room.getRoomId());

        if (game != null) {
            Player player = game.getPlayers().get(user.getUserId());
            if (player != null) {
                log.info("Updating socket info for game {}", room.getRoomId());
                game.setRoom(room);
                player.setUser(user);
                socketService.sendSocketUpdate(GameMapper.toDto(game));
            }
        }
    }
}
