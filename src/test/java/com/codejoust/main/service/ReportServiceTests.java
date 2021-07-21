package com.codejoust.main.service;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import com.codejoust.main.dao.AccountRepository;
import com.codejoust.main.dao.GameReportRepository;
import com.codejoust.main.dao.RoomRepository;
import com.codejoust.main.dao.UserRepository;
import com.codejoust.main.dto.game.SubmissionDto;
import com.codejoust.main.dto.game.SubmissionRequest;
import com.codejoust.main.dto.user.UserMapper;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemContainer;
import com.codejoust.main.model.report.GameEndType;
import com.codejoust.main.model.report.GameReport;
import com.codejoust.main.model.report.SubmissionGroupReport;
import com.codejoust.main.util.UtilityTestMethods;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTests {

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
    private GameManagementService gameManagementService;
    
    @Spy
    @InjectMocks
    private ReportService reportService;

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

        gameManagementService.createAddGameFromRoom(room);
        Game game = gameManagementService.getGameFromRoomId(room.getRoomId());

        // First correct submission for problem 0 with user1.
        SubmissionRequest correctSubmission = new SubmissionRequest();
        correctSubmission.setLanguage(TestFields.PYTHON_LANGUAGE);
        correctSubmission.setCode(TestFields.PYTHON_CODE);
        correctSubmission.setInitiator(UserMapper.toDto(user1));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                UtilityTestMethods.addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_1, 1);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, correctSubmission);

          gameManagementService.submitSolution(TestFields.ROOM_ID, correctSubmission);

        // Second correct submission for problem 0 with user3.
        correctSubmission.setInitiator(UserMapper.toDto(user3));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                UtilityTestMethods.addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID_3), 0, TestFields.PLAYER_CODE_1, 1);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, correctSubmission);

        gameManagementService.submitSolution(TestFields.ROOM_ID, correctSubmission);

        // Incorrect submission for problem 1 with user3.
        SubmissionRequest incorrectSubmission = new SubmissionRequest();
        incorrectSubmission.setLanguage(TestFields.PYTHON_LANGUAGE);
        incorrectSubmission.setCode(TestFields.PYTHON_CODE);
        incorrectSubmission.setInitiator(UserMapper.toDto(user3));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                UtilityTestMethods.addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID_3), 1, TestFields.PLAYER_CODE_2, 0);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, incorrectSubmission);
        gameManagementService.submitSolution(TestFields.ROOM_ID, incorrectSubmission);

        // Incorrect submission for problem 0 with user1.
        incorrectSubmission.setInitiator(UserMapper.toDto(user1));
        Mockito.doAnswer(new Answer<SubmissionDto>() {
            public SubmissionDto answer(InvocationOnMock invocation) {
                UtilityTestMethods.addSubmissionHelper(game.getPlayers().get(TestFields.USER_ID), 0, TestFields.PLAYER_CODE_2, 0);
                game.setAllSolved(true);
                return new SubmissionDto();
            }})
          .when(submitService).submitSolution(game, incorrectSubmission);
        gameManagementService.submitSolution(TestFields.ROOM_ID, incorrectSubmission);

        // Set manually end game, and trigger game report creation directly.
        game.setGameEnded(true);
        GameReport gameReport = reportService.createGameReport(game);

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

        gameManagementService.createAddGameFromRoom(room);
        Game game = gameManagementService.getGameFromRoomId(room.getRoomId());
        game.setGameEnded(true);
        reportService.createGameReport(game);

        gameManagementService.createAddGameFromRoom(room);
        game = gameManagementService.getGameFromRoomId(room.getRoomId());
        game.setAllSolved(true);
        GameReport gameReport = reportService.createGameReport(game);
    
        assertEquals(GameEndType.ALL_SOLVED, gameReport.getGameEndType());
        assertEquals(2, gameReport.getUsers().get(0).getSubmissionGroupReports().size());
    }
}
