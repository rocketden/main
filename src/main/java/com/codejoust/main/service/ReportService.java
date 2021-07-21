package com.codejoust.main.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.codejoust.main.dao.ProblemRepository;
import com.codejoust.main.dao.AccountRepository;
import com.codejoust.main.dao.GameReportRepository;
import com.codejoust.main.dto.game.SubmissionMapper;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.Submission;
import com.codejoust.main.model.Account;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemContainer;
import com.codejoust.main.model.report.GameEndType;
import com.codejoust.main.model.report.GameReport;
import com.codejoust.main.model.report.SubmissionGroupReport;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ReportService {

    private final ProblemRepository problemRepository;
    private final GameReportRepository gameReportRepository;
    private final AccountRepository accountRepository;

    @Autowired
    protected ReportService(ProblemRepository problemRepository,
                                    GameReportRepository gameReportRepository,
                                    AccountRepository accountRepository) {
        this.problemRepository = problemRepository;
        this.gameReportRepository = gameReportRepository;
        this.accountRepository = accountRepository;
    }

    public GameReport createGameReport(Game game) {
        GameReport gameReport = new GameReport();
        int numProblems = game.getProblems().size();
        int numPlayers = game.getPlayers().size();

        // Initialize the statistic variables for each problem.
        int[] userSolved = new int[numProblems];
        double[] totalTestCasesPassed = new double[numProblems];
        double[] totalAttemptCount = new double[numProblems];
        for (Player player : game.getPlayers().values()) {
            // For each user, add the relevant submission info.
            User user = player.getUser();

            // Construct the new submission group report for each user.
            SubmissionGroupReport submissionGroupReport = new SubmissionGroupReport();
            submissionGroupReport.setGameReportId(gameReport.getGameReportId());

            // Iterate through each submission and update group statistics.
            boolean[] problemsSolved = new boolean[numProblems];
            int[] testCasesPassed = new int[numProblems];
            for (Submission submission : player.getSubmissions()) {
                int problemIndex = submission.getProblemIndex();
                totalAttemptCount[problemIndex]++;

                // If the problem was solved, set boolean value to true.
                if (submission.getNumTestCases() == submission.getNumCorrect() && !problemsSolved[problemIndex]) {
                    problemsSolved[problemIndex] = true;
                    userSolved[problemIndex]++;
                }

                // Get the maximum number of test cases passed for each problem.
                testCasesPassed[problemIndex] = Math.max(testCasesPassed[problemIndex], submission.getNumCorrect());

                submissionGroupReport.addSubmissionReport(SubmissionMapper.toSubmissionReport(submission));
            }

            // Iterate through the test cases passed to add to the game total.
            for (int i = 0; i < testCasesPassed.length; i++) {
                totalTestCasesPassed[i] += testCasesPassed[i];
            }
            
            // Set the problems and test cases statistics.
            submissionGroupReport.setProblemsSolved(compactProblemsSolved(problemsSolved));
            submissionGroupReport.setNumTestCasesPassed(Arrays.stream(testCasesPassed).sum());
            
            // Add the submission group report and the user.
            user.addSubmissionGroupReport(submissionGroupReport);
            gameReport.addUser(user);
        }

        // Set problem container variables.
        int numTestCases = 0;
        List<Problem> problems = game.getProblems();
        for (int i = 0; i < numProblems; i++) {
            Problem problem = problems.get(i);
            numTestCases += problem.getTestCases().size();

            ProblemContainer problemContainer = new ProblemContainer();
            problemContainer.setProblem(problem);
            problemContainer.setUserSolvedCount(userSolved[i]);
            problemContainer.setTestCaseCount(problem.getTestCases().size());
            problemContainer.setAverageTestCasesPassed(totalTestCasesPassed[i] / numPlayers);
            problemContainer.setAverageAttemptCount(totalAttemptCount[i] / numPlayers);
            gameReport.addProblemContainer(problemContainer);
        }
        gameReport.setNumTestCases(numTestCases);

        Instant startTime = game.getGameTimer().getStartTime();
        gameReport.setCreatedDateTime(startTime);
        gameReport.setDuration(Duration.between(startTime, Instant.now()).getSeconds());

        if (game.getGameEnded()) {
            gameReport.setGameEndType(GameEndType.MANUAL_END);
        } else if (game.getAllSolved()) {
            gameReport.setGameEndType(GameEndType.ALL_SOLVED);
        } else {
            gameReport.setGameEndType(GameEndType.TIME_UP);
        }

        gameReport.setAverageTestCasesPassed(Arrays.stream(totalTestCasesPassed).sum() / numPlayers);
        gameReport.setAverageProblemsSolved((double) Arrays.stream(userSolved).sum() / numPlayers);

        // Set the Problem fields with the updated database problem.
        for (ProblemContainer problemContainer : gameReport.getProblemContainers()) {
            Problem problem = problemRepository.findProblemByProblemId(problemContainer.getProblem().getProblemId());
            problemContainer.setProblem(problem);
        }

        gameReportRepository.save(gameReport);

        // Iterate through all room users, players and spectators included.
        Set<Account> addedAccounts = new HashSet<>();
        for (User user : game.getRoom().getUsers()) {
            // If account exists and game report is not added, add game report.
            Account account = user.getAccount();
            if (account != null && !addedAccounts.contains(account)) {
                account.addGameReport(gameReport);
                addedAccounts.add(account);
                accountRepository.save(account);
            }
        }

        // Log the completion of the latest game report.
        log.info("Created game report for game with Room ID {}", game.getRoom().getRoomId());
        return gameReport;
    }

    private String compactProblemsSolved(boolean[] problemsSolved) {
        StringBuilder builder = new StringBuilder();
        for (boolean problemSolved : problemsSolved) {
            if (problemSolved) {
                builder.append("1"); 
            } else {
                builder.append("0");
            }
        }
        return builder.toString();
    }
}
