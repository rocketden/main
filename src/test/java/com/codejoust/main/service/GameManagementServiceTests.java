package com.codejoust.main.service;

import com.codejoust.main.dto.game.EndGameRequest;
import com.codejoust.main.util.TestFields;
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
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.codejoust.main.dao.AccountRepository;
import com.codejoust.main.dao.GameReportRepository;
import com.codejoust.main.dao.RoomRepository;
import com.codejoust.main.dao.UserRepository;
import com.codejoust.main.dto.game.GameDto;
import com.codejoust.main.dto.game.GameMapper;
import com.codejoust.main.dto.game.GameNotificationDto;
import com.codejoust.main.dto.game.PlayAgainRequest;
import com.codejoust.main.dto.game.StartGameRequest;
import com.codejoust.main.dto.game.SubmissionDto;
import com.codejoust.main.dto.game.SubmissionRequest;
import com.codejoust.main.dto.room.RoomDto;
import com.codejoust.main.dto.room.RoomMapper;
import com.codejoust.main.dto.user.UserDto;
import com.codejoust.main.dto.user.UserMapper;
import com.codejoust.main.exception.GameError;
import com.codejoust.main.exception.ProblemError;
import com.codejoust.main.exception.RoomError;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.GameTimer;
import com.codejoust.main.game_object.NotificationType;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.PlayerCode;
import com.codejoust.main.game_object.Submission;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemContainer;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.model.report.GameEndType;
import com.codejoust.main.model.report.GameReport;
import com.codejoust.main.model.report.SubmissionGroupReport;

@ExtendWith(MockitoExtension.class)
public class GameManagementServiceTests {

    @Mock
    private RoomRepository repository;

    @Mock
    private GameReportRepository gameReportRepository;
    
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

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

    // Helper method to add a dummy submission to a Player object
    private void addSubmissionHelper(Player player, int problemIndex, PlayerCode playerCode, int numCorrect) {
        Submission submission = new Submission();
        submission.setProblemIndex(problemIndex);
        submission.setNumCorrect(numCorrect);
        submission.setNumTestCases(1);
        submission.setStartTime(Instant.now());
        submission.setPlayerCode(playerCode);

        player.getSubmissions().add(submission);
        if (numCorrect == 1) {
            boolean[] solved = player.getSolved();
            solved[problemIndex] = true;
            player.setSolved(solved);
        }
    }

    @Test
    public void addGetAndRemoveGame() {
        // Initially, room doesn't exist
        ApiException exception = assertThrows(ApiException.class, () -> gameService.getGameFromRoomId(TestFields.ROOM_ID));
        assertEquals(GameError.NOT_FOUND, exception.getError());

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        // Return a non-empty list so at least one problem exists
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(eq(room.getDifficulty()), eq(1));

        // Create a game from a room
        gameService.createAddGameFromRoom(room);

        // Check that game has copied over the correct details
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);
        assertEquals(room, game.getRoom());
        assertEquals(user, game.getPlayers().get(TestFields.USER_ID).getUser());

        gameService.removeGame(TestFields.ROOM_ID);

        // Check that game has been removed
        exception = assertThrows(ApiException.class, () -> gameService.getGameFromRoomId(TestFields.ROOM_ID));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void startGameSuccess() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        room.setDuration(TestFields.DURATION);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        RoomDto response = gameService.startGame(TestFields.ROOM_ID, request);

        // Confirm that the problem service method is called correctly.
        verify(problemService).getProblemsFromDifficulty(eq(room.getDifficulty()), eq(1));

        verify(socketService).sendSocketUpdate(eq(response));

        assertEquals(TestFields.ROOM_ID, response.getRoomId());
        assertTrue(response.isActive());

        // Game object is created when the room chooses to start
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);
        assertNotNull(game);

        assertNotNull(game.getGameTimer());
        assertEquals(room.getDuration(), game.getGameTimer().getDuration());
    }

    @Test
    public void startGameWithOnlySelectedProblems() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.setDifficulty(ProblemDifficulty.HARD);
        room.setNumProblems(5);

        Problem problem = new Problem();
        problem.setProblemId(TestFields.PROBLEM_ID);
        problem.setName(TestFields.PROBLEM_NAME);
        problem.setDescription(TestFields.PROBLEM_DESCRIPTION);
        problem.setDifficulty(ProblemDifficulty.EASY);

        Problem problem2 = new Problem();
        problem2.setProblemId(TestFields.PROBLEM_ID_2);
        problem2.setName(TestFields.PROBLEM_NAME_2);
        problem2.setDescription(TestFields.PROBLEM_DESCRIPTION_2);
        problem2.setDifficulty(ProblemDifficulty.MEDIUM);

        room.setProblems(Arrays.asList(problem, problem2));

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(room.getRoomId());
        verify(problemService, never()).getProblemsFromDifficulty(Mockito.any(), Mockito.any());

        gameService.startGame(TestFields.ROOM_ID, request);
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);

        assertEquals(2, game.getProblems().size());
        assertEquals(problem.getProblemId(), game.getProblems().get(0).getProblemId());
        assertEquals(problem.getOutputType(), game.getProblems().get(0).getOutputType());
    }

    @Test
    public void startGameRoomNotFound() {
        UserDto user = new UserDto();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(user);

        Mockito.doReturn(null).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.startGame(TestFields.ROOM_ID, request));
        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }

    @Test
    public void startGameNoProblemsFound() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.startGame(TestFields.ROOM_ID, request));
        assertEquals(ProblemError.NOT_ENOUGH_FOUND, exception.getError());
    }

    @Test
    public void startGameWrongInitiator() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);

        UserDto initiator = new UserDto();
        initiator.setNickname(TestFields.NICKNAME_2);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(initiator);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.startGame(TestFields.ROOM_ID, request));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void getGameSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        // Confirm that the problem service method is called correctly.
        verify(problemService).getProblemsFromDifficulty(eq(room.getDifficulty()), eq(1));

        GameDto gameDto = gameService.getGameDtoFromRoomId(TestFields.ROOM_ID);

        assertEquals(RoomMapper.toDto(room), gameDto.getRoom());

        assertEquals(1, gameDto.getPlayers().size());
        assertEquals(UserMapper.toDto(user), gameDto.getPlayers().get(0).getUser());
        assertNotNull(gameDto.getGameTimer());
        assertEquals(room.getDuration(), gameDto.getGameTimer().getDuration());
    }

    @Test
    public void getGameNotFound() {
        ApiException exception = assertThrows(ApiException.class, () -> gameService.getGameDtoFromRoomId(TestFields.ROOM_ID));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void runCodeSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setCode(TestFields.PYTHON_CODE);
        request.setInput(TestFields.INPUT);
        request.setInitiator(UserMapper.toDto(user));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(TestFields.NUM_PROBLEMS);
        submissionDto.setNumTestCases(TestFields.NUM_PROBLEMS);

        gameService.runCode(TestFields.ROOM_ID, request);

        // Test that both submit service methods were called.
        verify(submitService).runCode(eq(game), eq(request));
    }

    @Test
    public void runCodeNullInputError() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setCode(TestFields.PYTHON_CODE);
        request.setInitiator(UserMapper.toDto(user));

        // Mock the return of the submissionDto, and mock update of player.
        ApiException exception = assertThrows(ApiException.class, () -> gameService.runCode(TestFields.ROOM_ID, request));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }

    @Test
    public void submitSolutionSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setCode(TestFields.PYTHON_CODE);
        request.setInitiator(UserMapper.toDto(user));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(TestFields.NUM_PROBLEMS);
        submissionDto.setNumTestCases(TestFields.NUM_PROBLEMS);
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_1, 1);
                game.setAllSolved(true);
                return submissionDto;
            }})
          .when(submitService).submitSolution(game, request);

        gameService.submitSolution(TestFields.ROOM_ID, request);

        // Test that both submit service methods were called.
        verify(submitService).submitSolution(eq(game), eq(request));

        // Test that game has been updated in socket message.
        verify(socketService).sendSocketUpdate(GameMapper.toDto(game));
        assertTrue(game.getAllSolved());
    }

    @Test
    public void sendAllSolvedSocketUpdate() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User user2 = new User();
        user2.setNickname(TestFields.NICKNAME_2);
        user2.setUserId(TestFields.USER_ID_2);
        room.addUser(user2);

        User user3 = new User();
        user3.setNickname(TestFields.NICKNAME_3);
        user3.setUserId(TestFields.USER_ID_3);
        room.addUser(user3);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);

        // Add submissions for the first two users.
        addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_1, 1);
        addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID_2), 0, TestFields.PLAYER_CODE_1, 1);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setCode(TestFields.PYTHON_CODE);
        request.setInitiator(UserMapper.toDto(user3));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(TestFields.NUM_PROBLEMS);
        submissionDto.setNumTestCases(TestFields.NUM_PROBLEMS);
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID_3), 0,TestFields.PLAYER_CODE_1, 1);
                game.setAllSolved(true);
                return submissionDto;
            }})
          .when(submitService).submitSolution(game, request);

        gameService.submitSolution(TestFields.ROOM_ID, request);

        verify(submitService).submitSolution(eq(game), eq(request));
        verify(gameService).handleEndGame(eq(game));

        // Confirm that socket sent updated GameDto object.
        verify(socketService).sendSocketUpdate(eq(GameMapper.toDto(game)));
        assertTrue(game.getAllSolved());
    }

    @Test
    public void submitSolutionNotAllSolved() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User user2 = new User();
        user2.setNickname(TestFields.NICKNAME_2);
        user2.setUserId(TestFields.USER_ID_2);
        room.addUser(user2);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setCode(TestFields.PYTHON_CODE);
        request.setInitiator(UserMapper.toDto(user));

        // Mock the return of the submissionDto, and mock update of player.
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(TestFields.NUM_PROBLEMS);
        submissionDto.setNumTestCases(TestFields.NUM_PROBLEMS);
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_1, 1);
                return submissionDto;
            }})
          .when(submitService).submitSolution(game, request);

        gameService.submitSolution(TestFields.ROOM_ID, request);

        verify(submitService).submitSolution(eq(game), eq(request));

        // Confirm the same update is sent even when all players solved problem.
        verify(socketService).sendSocketUpdate(eq(GameMapper.toDto(game)));
        assertFalse(game.getAllSolved());
    }

    @Test
    public void submitSolutionInvalidPermissions() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);

        SubmissionRequest request = new SubmissionRequest();
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setCode(TestFields.PYTHON_CODE);
        request.setInitiator(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () -> gameService.submitSolution(TestFields.ROOM_ID, request));
        assertEquals(GameError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void submitSolutionEmptyField() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);

        SubmissionRequest missingRequest = new SubmissionRequest();
        missingRequest.setLanguage(null);
        missingRequest.setCode(TestFields.PYTHON_CODE);
        missingRequest.setInitiator(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () -> gameService.submitSolution(TestFields.ROOM_ID, missingRequest));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }

    @Test
    public void playAgainSuccess() throws Exception {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);
        host.setSessionId(TestFields.SESSION_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.setDifficulty(ProblemDifficulty.HARD);
        room.setActive(true);
        room.addUser(host);
        room.setDuration(1L);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.startGame(TestFields.ROOM_ID, request);

        // Wait 1 second until the game timeUp socket update is sent
        Mockito.verify(socketService, Mockito.timeout(1500)).sendSocketUpdate(Mockito.any(GameDto.class));

        PlayAgainRequest playAgainRequest = new PlayAgainRequest();
        playAgainRequest.setInitiator(UserMapper.toDto(host));
        RoomDto response = gameService.playAgain(TestFields.ROOM_ID, playAgainRequest);

        Game game = gameService.getGameFromRoomId(room.getRoomId());

        verify(socketService).sendSocketUpdate(Mockito.eq(GameMapper.toDto(game)));

        assertTrue(game.getPlayAgain());
        assertEquals(room.getRoomId(), response.getRoomId());
        assertEquals(room.getDifficulty(), response.getDifficulty());
        assertFalse(room.getActive());
        assertNull(room.getHost().getSessionId());
    }

    @Test
    public void playAgainSuccessPlayerLeavesRoom() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);
        host.setSessionId(TestFields.SESSION_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.setActive(true);
        room.addUser(host);
        room.addUser(user);
        room.setDuration(1L);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.startGame(TestFields.ROOM_ID, request);

        // Wait 1 second until the game timeUp socket update is sent
        Mockito.verify(socketService, Mockito.timeout(1500)).sendSocketUpdate(Mockito.any(GameDto.class));

        Room newRoom = new Room();
        newRoom.setRoomId(TestFields.ROOM_ID);
        newRoom.setHost(host);
        newRoom.addUser(host);

        Mockito.doReturn(newRoom).when(repository).findRoomByRoomId(TestFields.ROOM_ID);

        PlayAgainRequest playAgainRequest = new PlayAgainRequest();
        playAgainRequest.setInitiator(UserMapper.toDto(host));

        RoomDto response = gameService.playAgain(TestFields.ROOM_ID, playAgainRequest);

        Game game = gameService.getGameFromRoomId(room.getRoomId());

        verify(socketService).sendSocketUpdate(Mockito.eq(GameMapper.toDto(game)));

        assertTrue(game.getPlayAgain());
        assertEquals(newRoom.getRoomId(), response.getRoomId());
        assertEquals(host, newRoom.getUsers().get(0));
        assertEquals(1, newRoom.getUsers().size());
        assertFalse(newRoom.getActive());
        assertNull(newRoom.getHost().getSessionId());
    }

    @Test
    public void playAgainWrongInitiator() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.setDuration(1L);

        StartGameRequest startRequest = new StartGameRequest();
        startRequest.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.startGame(TestFields.ROOM_ID, startRequest);

        Mockito.verify(socketService, Mockito.timeout(1500)).sendSocketUpdate(Mockito.any(GameDto.class));

        UserDto initiator = new UserDto();
        initiator.setNickname(TestFields.NICKNAME_2);
        PlayAgainRequest request = new PlayAgainRequest();
        request.setInitiator(initiator);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.playAgain(TestFields.ROOM_ID, request));
        assertEquals(GameError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void playAgainGameNotOver() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);

        StartGameRequest startRequest = new StartGameRequest();
        startRequest.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.startGame(TestFields.ROOM_ID, startRequest);

        PlayAgainRequest request = new PlayAgainRequest();
        request.setInitiator(UserMapper.toDto(host));

        ApiException exception = assertThrows(ApiException.class, () -> gameService.playAgain(TestFields.ROOM_ID, request));
        assertEquals(GameError.GAME_NOT_OVER, exception.getError());
    }

    @Test
    public void playAgainRoomNotFound() {
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.setDuration(1L);

        StartGameRequest startRequest = new StartGameRequest();
        startRequest.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(room).when(repository).findRoomByRoomId(TestFields.ROOM_ID);
        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.startGame(TestFields.ROOM_ID, startRequest);

        Mockito.verify(socketService, Mockito.timeout(1500)).sendSocketUpdate(Mockito.any(GameDto.class));

        PlayAgainRequest request = new PlayAgainRequest();
        request.setInitiator(UserMapper.toDto(host));

        Mockito.doReturn(null).when(repository).findRoomByRoomId(TestFields.ROOM_ID);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.playAgain(TestFields.ROOM_ID, request));
        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }


    public void sendNotificationSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(TestFields.CONTENT);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        gameService.sendNotification(TestFields.ROOM_ID, notificationDto);

        verify(notificationService).sendNotification(eq(TestFields.ROOM_ID), eq(notificationDto));
    }

    @Test
    public void sendNotificationNoInitiatorSuccess() throws Exception {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        // Change notification type to time left, as no initiator is required.
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(null);
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(TestFields.TIME_CONTENT);
        notificationDto.setNotificationType(NotificationType.TIME_LEFT);

        gameService.sendNotification(TestFields.ROOM_ID, notificationDto);

        verify(notificationService).sendNotification(eq(TestFields.ROOM_ID), eq(notificationDto));
    }

    @Test
    public void sendNotificationInitiatorRequired() throws Exception {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        // Change notification type to time left, as no initiator is required.
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(null);
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(TestFields.TIME_CONTENT);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(TestFields.ROOM_ID, notificationDto));
        assertEquals(GameError.NOTIFICATION_REQUIRES_INITIATOR, exception.getError());
    }

    @Test
    public void sendNotificationContentRequired() throws Exception {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        // Change notification type to time left, as no initiator is required.
        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(null);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(TestFields.ROOM_ID, notificationDto));
        assertEquals(GameError.NOTIFICATION_REQUIRES_CONTENT, exception.getError());
    }

    @Test
    public void sendNotificationNotFound() throws Exception {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(TestFields.CONTENT);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification("999999", notificationDto));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void sendNotificationMissingNotificationType() throws Exception {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(TestFields.CONTENT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(TestFields.ROOM_ID, notificationDto));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }

    @Test
    public void sendNotificationUserNotInGame() throws Exception {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        // Do not add user to game, then send invalid notification from them.
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);

        User host = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(host);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        GameNotificationDto notificationDto = new GameNotificationDto();
        notificationDto.setInitiator(UserMapper.toDto(user));
        notificationDto.setTime(Instant.now());
        notificationDto.setContent(TestFields.CONTENT);
        notificationDto.setNotificationType(NotificationType.TEST_CORRECT);

        ApiException exception = assertThrows(ApiException.class, () -> gameService.sendNotification(TestFields.ROOM_ID, notificationDto));
        assertEquals(GameError.USER_NOT_IN_GAME, exception.getError());
    }

    @Test
    public void updateCodeSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(TestFields.ROOM_ID);
        gameService.updateCode(TestFields.ROOM_ID, TestFields.USER_ID, TestFields.PLAYER_CODE_1);

        Player player = game.getPlayers().get(TestFields.USER_ID);

        // Confirm that the live game service method is called correctly.
        verify(liveGameService).updateCode(eq(player), eq(TestFields.PLAYER_CODE_1));
    }

    @Test
    public void updateCodeInvalidRoomId() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.updateCode("999999", TestFields.USER_ID, TestFields.PLAYER_CODE_1));
        assertEquals(GameError.NOT_FOUND, exception.getError());
    }

    @Test
    public void updateCodeInvalidUserId() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.updateCode(TestFields.ROOM_ID, "999999", TestFields.PLAYER_CODE_1));
        assertEquals(GameError.USER_NOT_IN_GAME, exception.getError());
    }

    @Test
    public void updateCodeEmptyPlayerCode() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.RANDOM);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        ApiException exception = assertThrows(ApiException.class, () -> gameService.updateCode(TestFields.ROOM_ID, TestFields.USER_ID, null));
        assertEquals(GameError.EMPTY_FIELD, exception.getError());
    }

    @Test
    public void isGameOverFunctionsCorrectly() {
        Game game = new Game();
        game.setGameTimer(new GameTimer(TestFields.DURATION));

        game.setAllSolved(false);
        game.getGameTimer().setTimeUp(false);
        assertFalse(gameService.isGameOver(game));

        game.setAllSolved(true);
        game.getGameTimer().setTimeUp(false);
        assertTrue(gameService.isGameOver(game));

        game.setAllSolved(false);
        game.getGameTimer().setTimeUp(true);
        assertTrue(gameService.isGameOver(game));

        game.setAllSolved(false);
        game.setGameTimer(null);
        assertFalse(gameService.isGameOver(game));
    }

    @Test
    public void endGameCancelsTimersCreateReport() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDuration(12L);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(room.getRoomId());

        // Manually schedule notification tasks due to service being mocked
        new NotificationService(socketService).scheduleTimeLeftNotifications(game, 12L);

        gameService.handleEndGame(game);

        // Neither the end game nor time left notifications are sent
        verify(socketService, after(13000).never()).sendSocketUpdate(Mockito.any(String.class), Mockito.any(GameNotificationDto.class));
        verify(socketService, never()).sendSocketUpdate(Mockito.any(GameDto.class));

        // Game report is saved after one minute past handleEndGame
        verify(gameReportRepository, after(61300)).save(Mockito.any(GameReport.class));
    }

    @Test
    public void createGameReportMultipleAttributes() {
        /**
         * Create a game report to test multiple different attributes:
         * 1. Logged-in users, logged-out users, players, and spectators.
         * 2. Correct and partially correct submissions.
         * 3. Multiple problems, with submissions for each.
         * 
         * The test goes through the following steps:
         * 1. Create a room with four users: two with same account, one
         * anonymous, and one anonymous spectator.
         * 2. Add two different problems to the room, and start the game.
         * 3. Add submissions: user1 submits correctly to the first problem,
         * then a new incorrect submission to the first problem. user2 does
         * not submit. user3 submits correctly to the first problem, then
         * incorrectly to the second problem.
         * 4. Check that the game and relevant account and user objects save.
         * 5. Check assertions for the general game report statistics,
         * each of the problem containers, and each of the users' submission
         * group reports.
         */

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDuration(120L);
        User user1 = new User();
        user1.setNickname(TestFields.NICKNAME);
        user1.setUserId(TestFields.USER_ID);
        user1.setAccount(TestFields.account1());
        User user2 = new User();
        user2.setNickname(TestFields.NICKNAME_2);
        user2.setUserId(TestFields.USER_ID_2);
        user2.setAccount(TestFields.account1());
        User user3 = new User();
        user3.setNickname(TestFields.NICKNAME_3);
        user3.setUserId(TestFields.USER_ID_3);
        User user4 = new User();
        user4.setNickname(TestFields.NICKNAME_4);
        user4.setUserId(TestFields.USER_ID_4);
        user4.setSpectator(true);
        room.addUser(user1);
        room.addUser(user2);
        room.addUser(user3);
        room.addUser(user4);
        room.setHost(user1);

        List<Problem> problems = new ArrayList<>();
        problems.add(TestFields.problem1());
        problems.add(TestFields.problem2());
        room.setProblems(problems);
        room.setNumProblems(problems.size());

        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(room.getRoomId());

        // First correct submission for problem 0 with user1.
        SubmissionRequest correctSubmission = new SubmissionRequest();
        correctSubmission.setLanguage(TestFields.PYTHON_LANGUAGE);
        correctSubmission.setCode(TestFields.PYTHON_CODE);
        correctSubmission.setInitiator(UserMapper.toDto(user1));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_1, 1);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, correctSubmission);

        gameService.submitSolution(TestFields.ROOM_ID, correctSubmission);

        // Second correct submission for problem 0 with user3.
        correctSubmission.setInitiator(UserMapper.toDto(user3));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID_3), 0, TestFields.PLAYER_CODE_1, 1);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, correctSubmission);

        gameService.submitSolution(TestFields.ROOM_ID, correctSubmission);

        // Incorrect submission for problem 1 with user3.
        SubmissionRequest incorrectSubmission = new SubmissionRequest();
        incorrectSubmission.setLanguage(TestFields.PYTHON_LANGUAGE);
        incorrectSubmission.setCode(TestFields.PYTHON_CODE);
        incorrectSubmission.setInitiator(UserMapper.toDto(user3));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID_3), 1, TestFields.PLAYER_CODE_2, 0);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, incorrectSubmission);
        gameService.submitSolution(TestFields.ROOM_ID, incorrectSubmission);

        // Incorrect submission for problem 0 with user1.
        incorrectSubmission.setInitiator(UserMapper.toDto(user1));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_2, 0);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, incorrectSubmission);
        gameService.submitSolution(TestFields.ROOM_ID, incorrectSubmission);

        // Set manually end game, and trigger game report creation directly.
        game.setGameEnded(true);
        GameReport gameReport = gameService.createGameReport(game);

        // Confirm that the game report, account, and users are saved.
        verify(gameReportRepository).save(Mockito.any(GameReport.class));
        verify(accountRepository).save(eq(user1.getAccount()));

        // Check assertions for top-level report variables.
        assertEquals(game.getGameTimer().getStartTime(), gameReport.getCreatedDateTime());
        assertEquals(2, gameReport.getNumTestCases());
        assertEquals((double) 2 / 3, gameReport.getAverageProblemsSolved());
        assertEquals((double) 2 / 3, gameReport.getAverageTestCasesPassed());
        assertEquals(GameEndType.MANUAL_END, gameReport.getGameEndType());

        // Check assertions for individual problem containers.
        assertEquals(2, gameReport.getProblemContainers().size());
        ProblemContainer problemContainer1 = gameReport.getProblemContainers().get(0);
        assertEquals(1, problemContainer1.getAverageAttemptCount());
        assertEquals((double) 2 / 3, problemContainer1.getAverageTestCasesPassed());
        assertEquals(TestFields.problem1().getName(), problemContainer1.getProblem().getName());
        assertEquals(1, problemContainer1.getTestCaseCount());
        assertEquals(2, problemContainer1.getUserSolvedCount());
        ProblemContainer problemContainer2 = gameReport.getProblemContainers().get(1);
        assertEquals((double) 1 / 3, problemContainer2.getAverageAttemptCount());
        assertEquals(0, problemContainer2.getAverageTestCasesPassed());
        assertEquals(TestFields.problem2().getName(), problemContainer2.getProblem().getName());
        assertEquals(1, problemContainer2.getTestCaseCount());
        assertEquals(0, problemContainer2.getUserSolvedCount());

        // Check assertions for each user and submission group.
        assertEquals(3, gameReport.getUsers().size());
        assertEquals(1, gameReport.getUsers().get(0).getSubmissionGroupReports().size());
        SubmissionGroupReport submissionGroupReport1 = gameReport.getUsers().get(0).getSubmissionGroupReports().get(0);
        assertEquals(gameReport.getGameReportId(), submissionGroupReport1.getGameReportId());
        assertEquals(1, submissionGroupReport1.getNumTestCasesPassed());
        assertEquals("10", submissionGroupReport1.getProblemsSolved());
        assertEquals(2, submissionGroupReport1.getSubmissionReports().size());
        
        assertEquals(1, gameReport.getUsers().get(1).getSubmissionGroupReports().size());
        SubmissionGroupReport submissionGroupReport2 = gameReport.getUsers().get(1).getSubmissionGroupReports().get(0);
        assertEquals(gameReport.getGameReportId(), submissionGroupReport2.getGameReportId());
        assertEquals(0, submissionGroupReport2.getNumTestCasesPassed());
        assertEquals("00", submissionGroupReport2.getProblemsSolved());
        assertEquals(0, submissionGroupReport2.getSubmissionReports().size());

        assertEquals(1, gameReport.getUsers().get(2).getSubmissionGroupReports().size());
        SubmissionGroupReport submissionGroupReport3 = gameReport.getUsers().get(2).getSubmissionGroupReports().get(0);
        assertEquals(gameReport.getGameReportId(), submissionGroupReport3.getGameReportId());
        assertEquals(1, submissionGroupReport3.getNumTestCasesPassed());
        assertEquals("10", submissionGroupReport3.getProblemsSolved());
        assertEquals(2, submissionGroupReport3.getSubmissionReports().size());
    }

    @Test
    public void createTwoGameReports() {
        /**
         * Create two games to verify that two submission group reports are
         * made with the same user.
         * 1. Create room with one user and one problem.
         * 2. Start and immediately end game, manually triggering the create
         * game report.
         * 3. Start and immediately end game again, and check that user has two
         * submission group reports, as well as the all solved game end type.
         */

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDuration(120L);
        User user1 = new User();
        user1.setNickname(TestFields.NICKNAME);
        user1.setUserId(TestFields.USER_ID);
        user1.setAccount(TestFields.account1());
        room.addUser(user1);
        room.setHost(user1);

        List<Problem> problems = new ArrayList<>();
        problems.add(TestFields.problem1());
        room.setProblems(problems);
        room.setNumProblems(problems.size());

        gameService.createAddGameFromRoom(room);
        Game game = gameService.getGameFromRoomId(room.getRoomId());
        game.setGameEnded(true);
        gameService.createGameReport(game);

        gameService.createAddGameFromRoom(room);
        game = gameService.getGameFromRoomId(room.getRoomId());
        game.setAllSolved(true);
        GameReport gameReport = gameService.createGameReport(game);
    
        assertEquals(GameEndType.ALL_SOLVED, gameReport.getGameEndType());
        assertEquals(2, gameReport.getUsers().get(0).getSubmissionGroupReports().size());
    }

    @Test
    public void conditionallyUpdateSocketInfoSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());

        gameService.createAddGameFromRoom(room);

        User newUser = new User();
        newUser.setNickname(TestFields.NICKNAME);
        newUser.setUserId(TestFields.USER_ID);
        newUser.setSessionId(TestFields.SESSION_ID);

        Room newRoom = new Room();
        newRoom.setRoomId(TestFields.ROOM_ID);
        newRoom.addUser(newUser);

        gameService.conditionallyUpdateSocketInfo(newRoom, newUser);

        Game game = gameService.getGameFromRoomId(room.getRoomId());

        assertEquals(newRoom, game.getRoom());
        assertEquals(user, game.getRoom().getUsers().get(0));
        assertEquals(user, game.getPlayers().get(user.getUserId()).getUser());

        verify(socketService).sendSocketUpdate(GameMapper.toDto(game));
    }

    @Test
    public void conditionallyUpdateSocketInfoNotFound() {
        Room room = new Room();
        room.setRoomId("999998");

        gameService.conditionallyUpdateSocketInfo(room, null);

        verify(socketService, never()).sendSocketUpdate(Mockito.any(GameDto.class));

        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());

        gameService.createAddGameFromRoom(room);

        User newUser = new User();
        newUser.setNickname(TestFields.NICKNAME_2);
        newUser.setUserId(TestFields.USER_ID_2);

        gameService.conditionallyUpdateSocketInfo(room, newUser);
        verify(socketService, never()).sendSocketUpdate(Mockito.any(GameDto.class));
    }

    @Test
    public void manuallyEndGamePermissionDenied() {
        User host = new User();
        host.setUserId(TestFields.USER_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);

        Mockito.doReturn(Collections.singletonList(new Problem())).when(problemService).getProblemsFromDifficulty(Mockito.any(), Mockito.any());
        gameService.createAddGameFromRoom(room);

        EndGameRequest request = new EndGameRequest();
        request.setInitiator(new UserDto());

        ApiException exception = assertThrows(ApiException.class, () -> gameService.manuallyEndGame(TestFields.ROOM_ID, request));
        assertEquals(GameError.INVALID_PERMISSIONS, exception.getError());
    }
}
