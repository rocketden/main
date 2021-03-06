package com.codejoust.main.dto.game;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codejoust.main.dto.problem.ProblemDto;
import com.codejoust.main.dto.problem.ProblemMapper;
import com.codejoust.main.dto.problem.ProblemTestCaseDto;
import com.codejoust.main.dto.room.RoomMapper;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.Submission;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.util.Color;
import com.codejoust.main.util.Utility;

public class GameMapper {

    private static final ModelMapper mapper = new ModelMapper();

    protected GameMapper() {}

    // Removes the correct output for non-hidden testcases and output and input for hidden testcases
    public static GameDto toDto(Game game) {
        if (game == null) {
            return null;
        }

        GameDto gameDto = new GameDto();
        gameDto.setRoom(RoomMapper.toDto(game.getRoom()));
        gameDto.setGameTimer(GameTimerMapper.toDto(game.getGameTimer()));
        gameDto.setPlayAgain(game.getPlayAgain());
        gameDto.setGameEnded(game.getGameEnded());

        // Set loose matching to allow flattening of variables in DTO objects
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);

        List<PlayerDto> players = gameDto.getPlayers();
        game.getPlayers().values().forEach(player -> players.add(mapper.map(player, PlayerDto.class)));
        sortLeaderboard(players);

        List<ProblemDto> problems = new ArrayList<>();
        ProblemDto problemDto;

        for (Problem problem : game.getProblems()) {
            problemDto = ProblemMapper.toDto(problem);
            
            for (ProblemTestCaseDto testcase : problemDto.getTestCases()) {
                if (testcase.isHidden()) {
                    testcase.setInput("");
                }

                testcase.setOutput("");
            }

            problems.add(problemDto);
        }

        gameDto.setProblems(problems);
        gameDto.setAllSolved(game.getAllSolved());

        return gameDto;
    }

    public static Game fromRoom(Room room) {
        if (room == null) {
            return null;
        }

        Game game = new Game();
        game.setRoom(room);

        // Create players and assign colors in random order.
        int index = 0;
        Map<String, Player> players = game.getPlayers();
        List<Color> colorList = new ArrayList<>(Utility.COLOR_LIST);
        Collections.shuffle(colorList);

        // Map all non-spectators to players.
        for (User user : room.getUsers()) {
            if (!user.getSpectator()) {
                Player player = PlayerMapper.playerFromUser(user);
                player.setColor(colorList.get(index));
                player.setSolved(new boolean[room.getNumProblems()]);
                players.put(user.getUserId(), player);
                index = (index + 1) % colorList.size();
            }
        }

        return game;
    }

    // Clears correctOutput from each result, and input, console, and userOutput from hidden results
    public static SubmissionDto submissionToDto(Submission submission) {
        if (submission == null) {
            return null;
        }

        List<SubmissionResultDto> testCases = new ArrayList<>();

        if (submission.getResults() != null) {
            for (int i = 0; i < submission.getResults().size(); i++) {
                SubmissionResultDto testCase = mapper.map(submission.getResults().get(i), SubmissionResultDto.class);
                testCase.setCorrectOutput("");

                if (testCase.isHidden()) {
                    testCase.setInput("");
                    testCase.setConsole("");
                    testCase.setUserOutput("");
                }

                testCases.add(testCase);
            }
        }


        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        SubmissionDto submissionDto = mapper.map(submission, SubmissionDto.class);
        submissionDto.setResults(testCases);

        return submissionDto;
    }

    // Sort by numCorrect followed by startTime
    public static void sortLeaderboard(List<PlayerDto> players) {
        players.sort((player1, player2) -> {
            List<SubmissionDto> submissions1 = player1.getSubmissions();
            List<SubmissionDto> submissions2 = player2.getSubmissions();

            // Players who haven't submitted yet are sorted last
            if (submissions1.isEmpty()) {
                return 1;
            } else if (submissions2.isEmpty()) {
                return -1;
            }

            int score1 = getScore(submissions1);
            int score2 = getScore(submissions2);

            // If both have the same numCorrect, whoever submits earlier is first
            if (score1 == score2) {
                Instant time1 = getTime(submissions1);
                Instant time2 = getTime(submissions2);

                // If neither has submitted correctly, oh well (if one is null, the other must be as well)
                if (time1 == null || time2 == null) {
                    return 0;
                }

                return time1.compareTo(time2);
            }

            // Whoever has higher numCorrect is first
            return score2 - score1;
        });
    }

    // Get total number of problems solved
    private static int getScore(List<SubmissionDto> submissions) {
        Set<Integer> set = new HashSet<>();
        for (SubmissionDto submission : submissions) {
            if (submission.getNumCorrect().equals(submission.getNumTestCases())) {
                set.add(submission.getProblemIndex());
            }
        }

        return set.size();
    }

    // Get time of latest correct solution, or null if none exists
    private static Instant getTime(List<SubmissionDto> submissions) {
        Set<Integer> set = new HashSet<>();
        Instant instant = null;

        for (SubmissionDto submission : submissions) {
            if (submission.getNumCorrect().equals(submission.getNumTestCases())
                    && !set.contains(submission.getProblemIndex())) {
                set.add(submission.getProblemIndex());
                instant = submission.getStartTime();
            }
        }

        return instant;
    }
}
