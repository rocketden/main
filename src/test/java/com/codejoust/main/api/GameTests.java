package com.codejoust.main.api;

import com.codejoust.main.dto.game.GameDto;
import com.codejoust.main.dto.game.GameNotificationDto;
import com.codejoust.main.dto.game.GameNotificationRequest;
import com.codejoust.main.dto.game.PlayerDto;
import com.codejoust.main.dto.game.StartGameRequest;
import com.codejoust.main.dto.game.SubmissionDto;
import com.codejoust.main.dto.game.SubmissionRequest;
import com.codejoust.main.dto.game.SubmissionResultDto;
import com.codejoust.main.dto.problem.ProblemDto;
import com.codejoust.main.dto.problem.SelectableProblemDto;
import com.codejoust.main.dto.room.CreateRoomRequest;
import com.codejoust.main.dto.room.RoomDto;
import com.codejoust.main.dto.room.UpdateSettingsRequest;
import com.codejoust.main.dto.user.UserDto;
import com.codejoust.main.exception.GameError;
import com.codejoust.main.exception.ProblemError;
import com.codejoust.main.exception.RoomError;
import com.codejoust.main.exception.api.ApiError;
import com.codejoust.main.exception.api.ApiErrorResponse;
import com.codejoust.main.game_object.GameTimer;
import com.codejoust.main.game_object.NotificationType;
import com.codejoust.main.util.MockHelper;
import com.codejoust.main.util.ProblemTestMethods;
import com.codejoust.main.util.RoomTestMethods;
import com.codejoust.main.util.TestFields;
import com.codejoust.main.util.TestUrls;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;

@SpringBootTest(properties = "spring.datasource.type=com.zaxxer.hikari.HikariDataSource")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class GameTests {

    @Autowired
    private MockMvc mockMvc;

    // Helper method to start the game for a given room
    private void startGameHelper(RoomDto room, UserDto host) throws Exception {
        ProblemTestMethods.createSingleVerifiedProblemAndTestCases(this.mockMvc);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(host);

        RoomDto roomDto = MockHelper.postRequest(this.mockMvc, TestUrls.startGame(room.getRoomId()), request, RoomDto.class, HttpStatus.OK);

        assertEquals(room.getRoomId(), roomDto.getRoomId());
        assertTrue(roomDto.isActive());
    }

    @Test
    public void startAndGetGameSuccess() throws Exception {
        UserDto host = TestFields.userDto1();

        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setHost(host);

        RoomDto roomDto = MockHelper.postRequest(this.mockMvc, TestUrls.createRoom(), createRequest, RoomDto.class, HttpStatus.CREATED);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(roomDto.getHost());

        ProblemTestMethods.createSingleVerifiedProblemAndTestCases(this.mockMvc);

        RoomDto actual = MockHelper.postRequest(this.mockMvc, TestUrls.startGame(roomDto.getRoomId()), request, RoomDto.class, HttpStatus.OK);

        assertEquals(roomDto.getRoomId(), actual.getRoomId());
        assertTrue(actual.isActive());

        // Check that game object was created properly
        GameDto gameDto = MockHelper.getRequest(this.mockMvc, TestUrls.getGame(roomDto.getRoomId()), GameDto.class, HttpStatus.OK);

        assertEquals(actual, gameDto.getRoom());
        assertEquals(1, gameDto.getPlayers().size());
        assertEquals(GameTimer.DURATION_15, gameDto.getGameTimer().getDuration());
    }

    @Test
    public void startGameWithProblemIdGetsCorrectProblem() throws Exception {
        UserDto host = TestFields.userDto1();

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);

        ProblemTestMethods.createSingleVerifiedProblemAndTestCases(this.mockMvc);
        ProblemDto problemDto = ProblemTestMethods.createSingleVerifiedProblemAndTestCases(this.mockMvc);

        UpdateSettingsRequest updateRequest = new UpdateSettingsRequest();
        updateRequest.setInitiator(host);
        updateRequest.setNumProblems(3);

        SelectableProblemDto selectableDto = new SelectableProblemDto();
        selectableDto.setProblemId(problemDto.getProblemId());
        selectableDto.setName(problemDto.getName());
        selectableDto.setDifficulty(problemDto.getDifficulty());

        updateRequest.setProblems(Collections.singletonList(selectableDto));

        RoomDto response = MockHelper.putRequest(this.mockMvc, TestUrls.updateSettings(roomDto.getRoomId()), updateRequest, RoomDto.class, HttpStatus.OK);

        assertEquals(1, response.getProblems().size());
        assertEquals(selectableDto.getProblemId(), response.getProblems().get(0).getProblemId());
        assertEquals(selectableDto.getName(), response.getProblems().get(0).getName());
        assertEquals(selectableDto.getDifficulty(), response.getProblems().get(0).getDifficulty());

        // Note: this also creates a problem (so total number of problems is 3)
        startGameHelper(roomDto, host);

        GameDto gameDto = MockHelper.getRequest(this.mockMvc, TestUrls.getGame(roomDto.getRoomId()), GameDto.class, HttpStatus.OK);

        assertEquals(1, gameDto.getRoom().getNumProblems());
        assertEquals(1, gameDto.getProblems().size());
        assertEquals(problemDto.getProblemId(), gameDto.getProblems().get(0).getProblemId());
    }

    @Test
    public void startGameRoomNotFound() throws Exception {
        UserDto user = TestFields.userDto1();

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(user);

        ApiError ERROR = RoomError.NOT_FOUND;

        ApiErrorResponse actual = MockHelper.postRequest(this.mockMvc, TestUrls.startGame(TestFields.ROOM_ID), request, ApiErrorResponse.class, ERROR.getStatus());
        assertEquals(ERROR.getResponse(), actual);
    }

    @Test
    public void startGameProblemNotEnoughFound() throws Exception {
        UserDto host = TestFields.userDto1();

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);

        StartGameRequest request = new StartGameRequest();
        request.setInitiator(host);

        ApiError ERROR = ProblemError.NOT_ENOUGH_FOUND;

        ApiErrorResponse actual = MockHelper.postRequest(this.mockMvc, TestUrls.startGame(roomDto.getRoomId()), request, ApiErrorResponse.class, ERROR.getStatus());
        assertEquals(ERROR.getResponse(), actual);
    }

    @Test
    public void startGameWrongInitiator() throws Exception {
        UserDto host = TestFields.userDto1();

        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setHost(host);

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);

        UserDto user = TestFields.userDto2();
        StartGameRequest request = new StartGameRequest();
        request.setInitiator(user);

        ApiError ERROR = RoomError.INVALID_PERMISSIONS;

        ApiErrorResponse actual = MockHelper.postRequest(this.mockMvc, TestUrls.startGame(roomDto.getRoomId()), request, ApiErrorResponse.class, ERROR.getStatus());
        assertEquals(ERROR.getResponse(), actual);
    }

    @Test
    public void runCodeSuccess() throws Exception {
        UserDto host = TestFields.userDto1();

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);
        startGameHelper(roomDto, host);

        SubmissionRequest request = new SubmissionRequest();
        request.setInitiator(host);
        request.setCode(TestFields.PYTHON_CODE);
        request.setLanguage(TestFields.PYTHON_LANGUAGE);
        request.setInput(TestFields.INPUT);

        SubmissionDto submissionDto = MockHelper.postRequest(this.mockMvc, TestUrls.runCode(roomDto.getRoomId()), request, SubmissionDto.class, HttpStatus.OK);

        assertEquals(TestFields.PYTHON_CODE, submissionDto.getCode());
        assertEquals(TestFields.PYTHON_LANGUAGE, submissionDto.getLanguage());
        assertEquals(submissionDto.getNumCorrect(), submissionDto.getNumTestCases());
        assertNull(submissionDto.getCompilationError());
        assertEquals(TestFields.RUNTIME, submissionDto.getRuntime());
        assertTrue(Instant.now().isAfter(submissionDto.getStartTime())
            || Instant.now().minusSeconds((long) 1).isBefore(submissionDto.getStartTime()));

        SubmissionResultDto resultDto = submissionDto.getResults().get(0);
        assertEquals(TestFields.OUTPUT, resultDto.getUserOutput());
        assertNull(resultDto.getError());
        assertEquals(TestFields.INPUT, resultDto.getInput());
        assertEquals("", resultDto.getCorrectOutput());
        assertFalse(resultDto.isHidden());
        assertTrue(resultDto.isCorrect());

        // Check that the submission is stored in the game object
        GameDto gameDto = MockHelper.getRequest(this.mockMvc, TestUrls.getGame(roomDto.getRoomId()), GameDto.class, HttpStatus.OK);

        // Confirm that running the code does not create a submission.
        assertEquals(1, gameDto.getPlayers().size());
        PlayerDto player = gameDto.getPlayers().get(0);
        assertEquals(0, player.getSubmissions().size());
        assertFalse(gameDto.getAllSolved());
    }
    
    @Test
    public void submitSolutionSuccess() throws Exception {
        UserDto host = TestFields.userDto1();

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);
        startGameHelper(roomDto, host);

        SubmissionRequest request = new SubmissionRequest();
        request.setInitiator(host);
        request.setCode(TestFields.PYTHON_CODE);
        request.setLanguage(TestFields.PYTHON_LANGUAGE);

        SubmissionDto submissionDto = MockHelper.postRequest(this.mockMvc, TestUrls.submitCode(roomDto.getRoomId()), request, SubmissionDto.class, HttpStatus.OK);

        assertNotNull(submissionDto);
        assertEquals(TestFields.PYTHON_CODE, submissionDto.getCode());
        assertEquals(TestFields.PYTHON_LANGUAGE, submissionDto.getLanguage());
        // For now, just assert that all test cases were passed
        assertEquals(submissionDto.getNumCorrect(), submissionDto.getNumTestCases());

        // Check that the submission is stored in the game object
        GameDto gameDto = MockHelper.getRequest(this.mockMvc, TestUrls.getGame(roomDto.getRoomId()), GameDto.class, HttpStatus.OK);

        assertEquals(1, gameDto.getPlayers().size());
        PlayerDto player = gameDto.getPlayers().get(0);
        submissionDto = player.getSubmissions().get(0);
        assertEquals(TestFields.PYTHON_CODE, submissionDto.getCode());
        assertEquals(TestFields.PYTHON_LANGUAGE, submissionDto.getLanguage());
        assertEquals(submissionDto.getNumCorrect(), submissionDto.getNumTestCases());
        assertNull(submissionDto.getCompilationError());
        assertEquals(TestFields.RUNTIME, submissionDto.getRuntime());
        assertTrue(Instant.now().isAfter(submissionDto.getStartTime())
            || Instant.now().minusSeconds(1).isBefore(submissionDto.getStartTime()));

        SubmissionResultDto resultDto = submissionDto.getResults().get(0);
        assertEquals(TestFields.OUTPUT, resultDto.getUserOutput());
        assertNull(resultDto.getError());
        assertEquals(TestFields.INPUT, resultDto.getInput());
        assertEquals(TestFields.OUTPUT, resultDto.getCorrectOutput());
        assertFalse(resultDto.isHidden());
        assertTrue(resultDto.isCorrect());
        
        assertTrue(gameDto.getAllSolved());
    }

    @Test
    public void sendNotificationSuccess() throws Exception {
        UserDto host = TestFields.userDto1();

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);
        startGameHelper(roomDto, host);

        GameNotificationRequest request = new GameNotificationRequest();
        request.setInitiator(host);
        request.setContent(TestFields.CONTENT);
        request.setNotificationType(NotificationType.TEST_CORRECT);

        GameNotificationDto notificationDtoResult = MockHelper.postRequest(this.mockMvc,
                TestUrls.sendNotification(roomDto.getRoomId()), request, GameNotificationDto.class, HttpStatus.OK);

        assertNotNull(notificationDtoResult);
        assertEquals(request.getInitiator(), notificationDtoResult.getInitiator());
        assertEquals(request.getNotificationType(), notificationDtoResult.getNotificationType());
        assertEquals(request.getContent(), notificationDtoResult.getContent());
        assertTrue(Instant.now().isAfter(notificationDtoResult.getTime())
            || Instant.now().minusSeconds(1).isBefore(notificationDtoResult.getTime()));
    }

    @Test
    public void submitSolutionBadLanguage() throws Exception {
        UserDto host = TestFields.userDto1();

        RoomDto roomDto = RoomTestMethods.setUpRoomWithOneUser(this.mockMvc, host);
        startGameHelper(roomDto, host);

        String request = "{\"initiator\": {\"nickname\": \"hi\"}, \"code\": \"print('hi')\", \"language\": \"x\"}";

        ApiError ERROR = GameError.BAD_LANGUAGE;

        ApiErrorResponse actual = MockHelper.postRequest(this.mockMvc, TestUrls.submitCode(roomDto.getRoomId()), request, ApiErrorResponse.class, ERROR.getStatus());
        assertEquals(ERROR.getResponse(), actual);
    }
}
