package com.rocketden.main.service;

import com.rocketden.main.dao.RoomRepository;
import com.rocketden.main.dto.game.GameDto;
import com.rocketden.main.dto.game.GameMapper;
import com.rocketden.main.dto.game.GameNotificationDto;
import com.rocketden.main.dto.game.PlayAgainRequest;
import com.rocketden.main.dto.game.StartGameRequest;
import com.rocketden.main.dto.game.SubmissionDto;
import com.rocketden.main.dto.game.SubmissionRequest;
import com.rocketden.main.dto.room.RoomDto;
import com.rocketden.main.dto.room.RoomMapper;
import com.rocketden.main.dto.user.UserDto;
import com.rocketden.main.dto.user.UserMapper;
import com.rocketden.main.exception.GameError;
import com.rocketden.main.exception.RoomError;
import com.rocketden.main.exception.api.ApiException;
import com.rocketden.main.game_object.CodeLanguage;
import com.rocketden.main.game_object.Game;
import com.rocketden.main.game_object.NotificationType;
import com.rocketden.main.game_object.Player;
import com.rocketden.main.game_object.Submission;
import com.rocketden.main.game_object.PlayerCode;
import com.rocketden.main.model.Room;
import com.rocketden.main.model.User;
import com.rocketden.main.model.problem.ProblemDifficulty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
public class GameManagementServiceTests {

    @Mock
    private RoomRepository repository;

    @Mock
    private SocketService socketService;

    @Mock
    private SubmitService submitService;

    @Mock
    private ProblemService problemService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LiveGameService liveGameService;

    @Spy
    @InjectMocks
    private GameManagementService gameService;

    // Predefine user and room attributes.
    private static final String ROOM_ID = "012345";
    private static final String NICKNAME = "rocket";
    private static final String NICKNAME_2 = "rocketden";
    private static final String NICKNAME_3 = "rocketrocket";
    private static final String USER_ID = "098765";
    private static final String USER_ID_2 = "345678";
    private static final String USER_ID_3 = "678910";
    private static final String SESSION_ID = "abcdefghijk";
    private static final String CODE = "print('hi')";
    private static final CodeLanguage LANGUAGE = CodeLanguage.PYTHON;
    private static final Integer NUM_PROBLEMS = 10;
    private static final PlayerCode PLAYER_CODE = new PlayerCode(CODE, LANGUAGE);
    private static final long DURATION = 600;

    // Predefine notification content.
    private static final String CONTENT = "[1, 2, 3]";
    private static final String TIME_CONTENT = "are thirty minutes";

    // Helper method to add a dummy submission to a Player object
    private void addSubmissionHelper(Player player, int numCorrect) {
        Submission submission = new Submission();
        submission.setNumCorrect(numCorrect);
        submission.setNumTestCases(NUM_PROBLEMS);
        submission.setStartTime(LocalDateTime.now());

        player.getSubmissions().add(submission);
        if (numCorrect == NUM_PROBLEMS) {
            player.setSolved(true);
        }
    }

    @Test
    public void addGetAndRemoveGame() {
        // Initially, room doesn't exist
        ApiException exception = assertThrows(ApiException.class, () -> gameService.getGameFromRoomId(ROOM_ID));
        assertEquals(GameError.NOT_FOUND, exception.getError());

        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        // Create a game from a room
        gameService.createAddGameFromRoom(room);

        // Confirm that the problem service method is called correctly.
        verify(problemService).getProblemsFromDifficulty(eq(room.getDifficulty()), eq(1));

        // Check that game has copied over the correct details
        Game game = gameService.getGameFromRoomId(ROOM_ID);
        assertEquals(room, game.getRoom());
        assertEquals(user, game.getPlayers().get(USER_ID).getUser());

        gameService.removeGame(ROOM_ID);

        // Check that game has been removed
        exception = assertThrows(ApiException.class, () -> gameService.getGameFromRoomId(ROOM_ID));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void startGameSuccess() {
        User host = new User();
        host.setNickname(NICKNAME);
        host.setUserId(USER_ID);

        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setHost(host);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        room.setDuration(DURATION);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(ROOM_ID);
        RoomDto response = gameService.startGame(ROOM_ID, request);

        // Confirm that the problem service method is called correctly.
        verify(problemService).getProblemsFromDifficulty(eq(room.getDifficulty()), eq(1));

        verify(socketService).sendSocketUpdate(eq(response));

        assertEquals(ROOM_ID, response.getRoomId());
        assertTrue(response.isActive());

        // Game object is created when the room chooses to start
        Game game = gameService.getGameFromRoomId(ROOM_ID);
        assertNotNull(game);

        assertNotNull(game.getGameTimer());
        assertEquals(room.getDuration(), game.getGameTimer().getDuration());
    }

    @Test
    public void startGameRoomNotFound() {
        UserDto user = new UserDto();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(user);

        Mockito.doReturn(null).when(repository).findRoomByRoomId(ROOM_ID);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.startGame(ROOM_ID, request));
        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }

    @Test
    public void startGameWrongInitiator() {
        User host = new User();
        host.setNickname(NICKNAME);
        host.setUserId(USER_ID);

        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setHost(host);

        UserDto initiator = new UserDto();
        initiator.setNickname(NICKNAME_2);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(initiator);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(ROOM_ID);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.startGame(ROOM_ID, request));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void getGameSuccess() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        gameService.createAddGameFromRoom(room);

        // Confirm that the problem service method is called correctly.
        verify(problemService).getProblemsFromDifficulty(eq(room.getDifficulty()), eq(1));

        GameDto gameDto = gameService.getGameDtoFromRoomId(ROOM_ID);

        assertEquals(RoomMapper.toDto(room), gameDto.getRoom());

        assertEquals(1, gameDto.getPlayers().size());
        assertEquals(UserMapper.toDto(user), gameDto.getPlayers().get(0).getUser());
        assertNotNull(gameDto.getGameTimer());
        assertEquals(room.getDuration(), gameDto.getGameTimer().getDuration());
    }

    @Test
    public void getGameNotFound() {
        ApiException exception = assertThrows(ApiException.class, () -> gameService.getGameDtoFromRoomId(ROOM_ID));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void submitSolutionSuccess() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(ROOM_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(LANGUAGE);
        request.setCode(CODE);
        request.setInitiator(UserMapper.toDto(user));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(NUM_PROBLEMS);
        submissionDto.setNumTestCases(NUM_PROBLEMS);
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(USER_ID), 10);
                game.setAllSolved(true);
                return submissionDto;
            }})
          .when(submitService).submitSolution(game, request);

        gameService.submitSolution(ROOM_ID, request);

        // Test that both submit service methods were called.
        verify(submitService).submitSolution(eq(game), eq(request));

        // Test that game has been updated in socket message.
        verify(socketService).sendSocketUpdate(GameMapper.toDto(game));
        assertTrue(game.getAllSolved());
    }

    @Test
    public void sendAllSolvedSocketUpdate() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        User user2 = new User();
        user2.setNickname(NICKNAME_2);
        user2.setUserId(USER_ID_2);
        room.addUser(user2);

        User user3 = new User();
        user3.setNickname(NICKNAME_3);
        user3.setUserId(USER_ID_3);
        room.addUser(user3);

        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(ROOM_ID);

        // Add submissions for the first two users.
        addSubmissionHelper(game.getPlayers().get(USER_ID), 10);
        addSubmissionHelper(game.getPlayers().get(USER_ID_2), 10);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(LANGUAGE);
        request.setCode(CODE);
        request.setInitiator(UserMapper.toDto(user3));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(NUM_PROBLEMS);
        submissionDto.setNumTestCases(NUM_PROBLEMS);
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(USER_ID_3), 10);
                game.setAllSolved(true);
                return submissionDto;
            }})
          .when(submitService).submitSolution(game, request);

        gameService.submitSolution(ROOM_ID, request);

        verify(submitService).submitSolution(eq(game), eq(request));

        // Confirm that socket sent updated GameDto object.
        verify(socketService).sendSocketUpdate(eq(GameMapper.toDto(game)));
        assertTrue(game.getAllSolved());
    }

    @Test
    public void submitSolutionNotAllSolved() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        User user2 = new User();
        user2.setNickname(NICKNAME_2);
        user2.setUserId(USER_ID_2);
        room.addUser(user2);

        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(ROOM_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(LANGUAGE);
        request.setCode(CODE);
        request.setInitiator(UserMapper.toDto(user));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(NUM_PROBLEMS);
        submissionDto.setNumTestCases(NUM_PROBLEMS);
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(USER_ID), 10);
                return submissionDto;
            }})
          .when(submitService).submitSolution(game, request);

        gameService.submitSolution(ROOM_ID, request);

        verify(submitService).submitSolution(eq(game), eq(request));

        // Confirm the same update is sent even when all players solved problem.
        verify(socketService).sendSocketUpdate(eq(GameMapper.toDto(game)));
        assertFalse(game.getAllSolved());
    }

    @Test
    public void submitSolutionInvalidPermissions() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        gameService.createAddGameFromRoom(room);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(LANGUAGE);
        request.setCode(CODE);
        request.setInitiator(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () -> gameService.submitSolution(ROOM_ID, request));
        assertEquals(GameError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void submitSolutionEmptyField() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        gameService.createAddGameFromRoom(room);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);

        SubmissionRequest missingRequest = new SubmissionRequest();
        missingRequest.setLanguage(null);
        missingRequest.setCode(CODE);
        missingRequest.setInitiator(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () -> gameService.submitSolution(ROOM_ID, missingRequest));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }

    @Test
    public void playAgainSuccess() throws Exception {
        User host = new User();
        host.setNickname(NICKNAME);
        host.setUserId(USER_ID);
        host.setSessionId(SESSION_ID);

        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setHost(host);
        room.setDifficulty(ProblemDifficulty.HARD);
        room.setActive(true);
        room.addUser(host);
        room.setDuration(1L);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(ROOM_ID);
        gameService.startGame(ROOM_ID, request);

        // Wait 1 second until the game timeUp socket update is sent
        Mockito.verify(socketService, Mockito.timeout(1500)).sendSocketUpdate(Mockito.any(GameDto.class));

        PlayAgainRequest playAgainRequest = new PlayAgainRequest();
        playAgainRequest.setInitiator(UserMapper.toDto(host));
        RoomDto response = gameService.playAgain(ROOM_ID, playAgainRequest);

        Game game = gameService.getGameFromRoomId(room.getRoomId());

        verify(socketService).sendSocketUpdate(Mockito.eq(GameMapper.toDto(game)));

        assertTrue(game.getPlayAgain());
        assertEquals(room.getRoomId(), response.getRoomId());
        assertEquals(room.getDifficulty(), response.getDifficulty());
        assertFalse(room.getActive());
        assertNull(room.getHost().getSessionId());
    }

    @Test
    public void playAgainWrongInitiator() throws Exception {
        User host = new User();
        host.setNickname(NICKNAME);
        host.setUserId(USER_ID);

        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setHost(host);
        room.setDuration(1L);

        StartGameRequest startRequest = new StartGameRequest();
        startRequest.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(ROOM_ID);
        gameService.startGame(ROOM_ID, startRequest);

        Mockito.verify(socketService, Mockito.timeout(1500)).sendSocketUpdate(Mockito.any(GameDto.class));

        UserDto initiator = new UserDto();
        initiator.setNickname(NICKNAME_2);
        PlayAgainRequest request = new PlayAgainRequest();
        request.setInitiator(initiator);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.playAgain(ROOM_ID, request));
        assertEquals(GameError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void playAgainGameNotOver() {
        User host = new User();
        host.setNickname(NICKNAME);
        host.setUserId(USER_ID);

        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setHost(host);

        StartGameRequest startRequest = new StartGameRequest();
        startRequest.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(ROOM_ID);
        gameService.startGame(ROOM_ID, startRequest);

        PlayAgainRequest request = new PlayAgainRequest();
        request.setInitiator(UserMapper.toDto(host));

        ApiException exception = assertThrows(ApiException.class, () -> gameService.playAgain(ROOM_ID, request));
        assertEquals(GameError.GAME_NOT_OVER, exception.getError());
    }

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

        gameService.sendNotification(ROOM_ID, notificationDto);

        verify(notificationService).sendNotification(eq(ROOM_ID), eq(notificationDto));
    }

    @Test
    public void sendNotificationNoInitiatorSuccess() throws Exception {
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

        // Change notification type to time left, as no initiator is required.
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(null);
        notificationDto.setTime(LocalDateTime.now());
        notificationDto.setContent(TIME_CONTENT);
        notificationDto.setNotificationType(NotificationType.TIME_LEFT);

        gameService.sendNotification(ROOM_ID, notificationDto);

        verify(notificationService).sendNotification(eq(ROOM_ID), eq(notificationDto));
    }

    @Test
    public void sendNotificationInitiatorRequired() throws Exception {
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

        // Change notification type to time left, as no initiator is required.
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(null);
        notificationDto.setTime(LocalDateTime.now());
        notificationDto.setContent(TIME_CONTENT);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(ROOM_ID, notificationDto));
        assertEquals(GameError.NOTIFICATION_REQUIRES_INITIATOR, exception.getError());
    }

    @Test
    public void sendNotificationContentRequired() throws Exception {
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

        // Change notification type to time left, as no initiator is required.
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(LocalDateTime.now());
        notificationDto.setContent(null);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(ROOM_ID, notificationDto));
        assertEquals(GameError.NOTIFICATION_REQUIRES_CONTENT, exception.getError());
    }

    @Test
    public void sendNotificationNotFound() throws Exception {
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

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification("999999", notificationDto));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void sendNotificationMissingNotificationType() throws Exception {
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

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(ROOM_ID, notificationDto));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }

    @Test
    public void sendNotificationUserNotInGame() throws Exception {
        Room room = new Room();
        room.setRoomId(ROOM_ID);

        // Do not add user to game, then send invalid notification from them.
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);

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

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(ROOM_ID, notificationDto));
        assertEquals(GameError.USER_NOT_IN_GAME, exception.getError());
    }

    @Test
    public void updateCodeSuccess() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(ROOM_ID);
        gameService.updateCode(ROOM_ID, USER_ID, PLAYER_CODE);

        Player player = game.getPlayers().get(USER_ID);

        // Confirm that the live game service method is called correctly.
        verify(liveGameService).updateCode(eq(player), eq(PLAYER_CODE));
    }

    @Test
    public void updateCodeInvalidRoomId() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        gameService.createAddGameFromRoom(room);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.updateCode("999999", USER_ID, PLAYER_CODE));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void updateCodeInvalidUserId() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        gameService.createAddGameFromRoom(room);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.updateCode(ROOM_ID, "999999", PLAYER_CODE));
        assertEquals(GameError.USER_NOT_IN_GAME, exception.getError());
    }

    @Test
    public void updateCodeEmptyPlayerCode() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        gameService.createAddGameFromRoom(room);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.updateCode(ROOM_ID, USER_ID, null));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }
}
