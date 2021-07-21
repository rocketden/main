package com.codejoust.main.mapper;

import com.codejoust.main.util.TestFields;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.codejoust.main.dto.game.GameDto;
import com.codejoust.main.dto.game.GameMapper;
import com.codejoust.main.dto.game.PlayerDto;
import com.codejoust.main.dto.game.SubmissionDto;
import com.codejoust.main.dto.game.SubmissionResultDto;
import com.codejoust.main.dto.problem.ProblemDto;
import com.codejoust.main.dto.problem.ProblemTestCaseDto;
import com.codejoust.main.dto.room.RoomMapper;
import com.codejoust.main.dto.user.UserMapper;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.PlayerCode;
import com.codejoust.main.game_object.Submission;
import com.codejoust.main.game_object.SubmissionResult;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.model.problem.ProblemTestCase;

@SpringBootTest
public class GameMapperTests {

    private static final int TEST_CASES = 10;

    // Helper method to add a dummy submission to a PlayerDto object
    private void addSubmissionHelper(PlayerDto playerDto, int numCorrect, int problemIndex) {
        SubmissionDto submissionDto = new SubmissionDto();
        submissionDto.setNumCorrect(numCorrect);
        submissionDto.setProblemIndex(problemIndex);
        submissionDto.setStartTime(Instant.now());

        playerDto.getSubmissions().add(submissionDto);
    }

    @Test
    public void fromRoom() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user1 = new User();
        user1.setNickname(TestFields.NICKNAME);
        user1.setUserId(TestFields.USER_ID);
        User user2 = new User();
        user2.setNickname(TestFields.NICKNAME_2);
        user2.setUserId(TestFields.USER_ID_2);
        user2.setSpectator(true);
        room.addUser(user1);
        room.addUser(user2);
        
        Game game = GameMapper.fromRoom(room);

        assertEquals(room, game.getRoom());
        assertNotNull(game.getPlayers().get(TestFields.USER_ID));
        assertEquals(user1, game.getPlayers().get(TestFields.USER_ID).getUser());
        assertEquals(1, game.getPlayers().size());
        assertEquals(false, game.getAllSolved());
    }

    @Test
    public void toDto() {
        Problem problem = new Problem();
        problem.setName(TestFields.PROBLEM_NAME);
        problem.setDescription(TestFields.PROBLEM_DESCRIPTION);
        problem.setDifficulty(ProblemDifficulty.MEDIUM);

        ProblemTestCase testCase = new ProblemTestCase();
        testCase.setInput(TestFields.INPUT);
        testCase.setOutput(TestFields.OUTPUT);
        testCase.setHidden(false);
        testCase.setProblem(problem);
        problem.addTestCase(testCase);
        
        ProblemTestCase testCase2 = new ProblemTestCase();
        testCase2.setInput(TestFields.INPUT_2);
        testCase2.setOutput(TestFields.OUTPUT_2);
        testCase2.setHidden(true);
        testCase2.setProblem(problem);
        problem.addTestCase(testCase2);


        List<Problem> problems = new ArrayList<>();
        problems.add(problem);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Game game = GameMapper.fromRoom(room);
        game.setProblems(problems);
        game.setPlayAgain(true);
        game.setAllSolved(true);
        game.setGameEnded(true);

        PlayerCode playerCode = new PlayerCode();
        playerCode.setCode(TestFields.PYTHON_CODE);
        playerCode.setLanguage(TestFields.PYTHON_LANGUAGE);

        Submission submission = new Submission();
        submission.setPlayerCode(playerCode);
        submission.setNumTestCases(TEST_CASES);
        submission.setNumCorrect(TEST_CASES);

        Player player = game.getPlayers().get(TestFields.USER_ID);
        player.setSolved(new boolean[]{true});
        player.setPlayerCode(playerCode);
        player.getSubmissions().add(submission);

        GameDto gameDto = GameMapper.toDto(game);

        assertEquals(RoomMapper.toDto(room), gameDto.getRoom());
        assertEquals(1, gameDto.getPlayers().size());
        assertEquals(game.getPlayAgain(), gameDto.getPlayAgain());
        assertEquals(game.getAllSolved(), gameDto.getAllSolved());
        assertEquals(game.getGameEnded(), gameDto.getGameEnded());

        ProblemDto problemDto = gameDto.getProblems().get(0);

        assertEquals(TestFields.PROBLEM_NAME, problemDto.getName());
        assertEquals(TestFields.PROBLEM_DESCRIPTION, problemDto.getDescription());
        assertEquals(ProblemDifficulty.MEDIUM, problemDto.getDifficulty());

        ProblemTestCaseDto testCaseDto = problemDto.getTestCases().get(0);

        assertEquals(TestFields.INPUT, testCaseDto.getInput());
        assertEquals("", testCaseDto.getOutput());

        PlayerDto playerDto = gameDto.getPlayers().get(0);
        assertEquals(UserMapper.toDto(user), playerDto.getUser());
        assertArrayEquals(player.getSolved(), playerDto.getSolved());
        assertEquals(1, playerDto.getSubmissions().size());
        assertEquals(player.getColor(), playerDto.getColor());

        SubmissionDto submissionDto = playerDto.getSubmissions().get(0);
        assertEquals(submission.getPlayerCode().getCode(), submissionDto.getCode());
        assertEquals(submission.getPlayerCode().getLanguage(), submissionDto.getLanguage());
        assertEquals(submission.getNumCorrect(), submissionDto.getNumCorrect());
        assertEquals(submission.getNumTestCases(), submissionDto.getNumTestCases());
        assertEquals(submission.getStartTime(), submissionDto.getStartTime());
    }

    @Test
    public void submissionToDto() {
        PlayerCode playerCode = new PlayerCode();
        playerCode.setCode(TestFields.PYTHON_CODE);
        playerCode.setLanguage(TestFields.PYTHON_LANGUAGE);

        Submission submission = new Submission();
        submission.setPlayerCode(playerCode);
        submission.setNumTestCases(TEST_CASES);
        submission.setNumCorrect(TEST_CASES);
        submission.setRuntime(5.5);

        SubmissionResult result = new SubmissionResult();
        result.setCorrectOutput(TestFields.OUTPUT);
        result.setHidden(false);
        List<SubmissionResult> submissionResults = Collections.singletonList(result);
        submission.setResults(submissionResults);

        SubmissionDto submissionDto = GameMapper.submissionToDto(submission);
        assertEquals(submission.getPlayerCode().getCode(), submissionDto.getCode());
        assertEquals(submission.getPlayerCode().getLanguage(), submissionDto.getLanguage());
        assertEquals(submission.getNumCorrect(), submissionDto.getNumCorrect());
        assertEquals(submission.getStartTime(), submissionDto.getStartTime());
        assertEquals(submission.getRuntime(), submissionDto.getRuntime());
        assertEquals("", submissionDto.getResults().get(0).getCorrectOutput());
    }

    @Test
    public void submissionToDtoHiddenTestCases() {
        Submission submission = new Submission();

        SubmissionResult result = new SubmissionResult();
        result.setHidden(true);
        result.setConsole(TestFields.OUTPUT);
        result.setUserOutput(TestFields.OUTPUT);
        result.setInput(TestFields.INPUT);
        result.setCorrectOutput(TestFields.OUTPUT);
        List<SubmissionResult> submissionResults = Collections.singletonList(result);
        submission.setResults(submissionResults);

        SubmissionResultDto resultDto = GameMapper.submissionToDto(submission).getResults().get(0);
        assertEquals("", resultDto.getConsole());
        assertEquals("", resultDto.getUserOutput());
        assertEquals("", resultDto.getInput());
        assertEquals("", resultDto.getCorrectOutput());
    }

    @Test
    public void sortLeaderboardSuccess() {
        List<PlayerDto> players = new ArrayList<>();

        // Note: order of addSubmissionHelper matters (time of submission)
        PlayerDto player1 = new PlayerDto();
        addSubmissionHelper(player1, TEST_CASES, 0);
        addSubmissionHelper(player1, TEST_CASES, 0);
        addSubmissionHelper(player1, TEST_CASES, 0);

        PlayerDto player2 = new PlayerDto();
        addSubmissionHelper(player2, TEST_CASES, 0);
        addSubmissionHelper(player2, TEST_CASES, 1);

        PlayerDto player3 = new PlayerDto();
        addSubmissionHelper(player3, 0, 0);

        PlayerDto player4 = new PlayerDto();
        addSubmissionHelper(player4, TEST_CASES, 0);
        addSubmissionHelper(player4, TEST_CASES, 1);

        // Player 2 submits wrong afterwards, but it doesn't count against his time
        addSubmissionHelper(player2, 0, 1);

        PlayerDto player5 = new PlayerDto();

        players.add(player1);
        players.add(player2);
        players.add(player3);
        players.add(player4);
        players.add(player5);

        // Player order should be: [2, 4, 1, 3, 5]
        GameMapper.sortLeaderboard(players);

        assertEquals(player2, players.get(0));
        assertEquals(player4, players.get(1));
        assertEquals(player1, players.get(2));
        assertEquals(player3, players.get(3));
        assertEquals(player5, players.get(4));
    }

    @Test
    public void toDtoSortsLeaderboard() {
        Submission sub1 = new Submission();
        sub1.setNumCorrect(0);
        sub1.setNumTestCases(TEST_CASES);
        Submission sub2 = new Submission();
        sub2.setNumCorrect(TEST_CASES);
        sub2.setNumTestCases(TEST_CASES);

        Player player1 = new Player();
        player1.getSubmissions().add(sub1);
        Player player2 = new Player();
        player2.getSubmissions().add(sub2);

        Game game = new Game();
        game.getPlayers().put("player1", player1);
        game.getPlayers().put("player2", player2);

        GameDto gameDto = GameMapper.toDto(game);

        List<PlayerDto> players = gameDto.getPlayers();

        assertEquals(2, players.size());
        assertEquals(TEST_CASES, players.get(0).getSubmissions().get(0).getNumCorrect());
        assertEquals(0, players.get(1).getSubmissions().get(0).getNumCorrect());
    }
}
