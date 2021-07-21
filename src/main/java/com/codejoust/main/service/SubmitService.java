package com.codejoust.main.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.codejoust.main.dto.game.GameMapper;
import com.codejoust.main.dto.game.SubmissionDto;
import com.codejoust.main.dto.game.SubmissionMapper;
import com.codejoust.main.dto.game.SubmissionRequest;
import com.codejoust.main.dto.game.TesterRequest;
import com.codejoust.main.dto.game.TesterResponse;
import com.codejoust.main.dto.game.TesterResult;
import com.codejoust.main.dto.problem.ProblemDto;
import com.codejoust.main.dto.problem.ProblemMapper;
import com.codejoust.main.dto.problem.ProblemTestCaseDto;
import com.codejoust.main.exception.GameError;
import com.codejoust.main.exception.TesterError;
import com.codejoust.main.exception.api.ApiErrorResponse;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.PlayerCode;
import com.codejoust.main.game_object.Submission;
import com.codejoust.main.game_object.SubmissionResult;
import com.codejoust.main.model.problem.Problem;
import com.google.gson.Gson;

import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Class to handle code updates and miscellaneous requests.
 */
@Log4j2
@Service
public class SubmitService {

    public static final Double DUMMY_RUNTIME = 5.5;
    public static final String DUMMY_OUTPUT = "[1, 2, 3]";

    private final Gson gson;

    // Pulls value from application.properties
    @Value("${tester.debugMode}")
    private Boolean debugMode;

    @Value("${tester.url}")
    private String testerUrl;

    private final HttpClient httpClient;

    protected SubmitService() {
        this.httpClient = HttpClientBuilder.create().build();
        this.gson = new Gson();
    }

    // Helper method to return a perfect score dummy submission
    private Submission getDummySubmission(TesterRequest request) {
        int numTestCases = request.getProblem().getTestCases().size();
        PlayerCode playerCode = new PlayerCode(request.getCode(), request.getLanguage());

        Submission submission = new Submission();
        submission.setPlayerCode(playerCode);
        submission.setNumCorrect(numTestCases);
        submission.setNumTestCases(numTestCases);
        submission.setRuntime(DUMMY_RUNTIME);

        ProblemTestCaseDto testCaseDto = request.getProblem().getTestCases().get(0);
        List<SubmissionResult> submissionResults = new ArrayList<>();
        SubmissionResult submissionResult = new SubmissionResult();
        submissionResult.setCorrect(true);
        submissionResult.setUserOutput(DUMMY_OUTPUT);
        submissionResult.setHidden(testCaseDto.isHidden());
        submissionResult.setInput(testCaseDto.getInput());
        submissionResult.setCorrectOutput(DUMMY_OUTPUT);
        submissionResults.add(submissionResult);
        submission.setResults(submissionResults);

        return submission;
    }

    // Test the submission and send a socket update.
    public SubmissionDto runCode(Game game, SubmissionRequest request) {
        PlayerCode playerCode = new PlayerCode();
        playerCode.setCode(request.getCode());
        playerCode.setLanguage(request.getLanguage());

        // Make a call to the tester service
        TesterRequest testerRequest = new TesterRequest();
        testerRequest.setCode(request.getCode());
        testerRequest.setLanguage(request.getLanguage());

        // Set the problem with the single provided test case.
        ProblemDto problemDto = getStrippedProblemDto(game.getProblems().get(request.getProblemIndex()));

        /**
         * Provide a temporary output to circumvent output parsing error.
         * The problem must have at least one test case to work.
         */
        String tempOutput = problemDto.getTestCases().get(0).getOutput();
        problemDto.getTestCases().clear();

        List<ProblemTestCaseDto> problemTestCaseDtos = new ArrayList<>();
        ProblemTestCaseDto problemTestCaseDto = new ProblemTestCaseDto();
        problemTestCaseDto.setInput(request.getInput());
        problemTestCaseDto.setOutput(tempOutput);
        problemTestCaseDto.setHidden(false);
        problemTestCaseDtos.add(problemTestCaseDto);
        problemDto.setTestCases(problemTestCaseDtos);
        testerRequest.setProblem(problemDto);

        // Return submission, and no further records necessary for running code.
        Submission submission = getSubmission(testerRequest);
        submission.setProblemIndex(request.getProblemIndex());
        return GameMapper.submissionToDto(submission);
    }

    // Test the submission and send a socket update.
    public SubmissionDto submitSolution(Game game, SubmissionRequest request) {
        String userId = request.getInitiator().getUserId();
        Player player = game.getPlayers().get(userId);

        PlayerCode playerCode = new PlayerCode();
        playerCode.setCode(request.getCode());
        playerCode.setLanguage(request.getLanguage());

        // Make a call to the tester service
        TesterRequest testerRequest = new TesterRequest();
        testerRequest.setCode(request.getCode());
        testerRequest.setLanguage(request.getLanguage());

        // Invariant: Games have at least one problem (else it will fail to create)
        ProblemDto problemDto = getStrippedProblemDto(game.getProblems().get(request.getProblemIndex()));
        testerRequest.setProblem(problemDto);

        Submission submission = getSubmission(testerRequest);
        submission.setProblemIndex(request.getProblemIndex());
        player.getSubmissions().add(submission);

        if (submission.getNumCorrect().equals(submission.getNumTestCases())) {
            player.getSolved()[request.getProblemIndex()] = true;
        }

        // Variable to indicate whether all players have solved the problem.
        boolean allSolved = true;
        for (Player p : game.getPlayers().values()) {
            for (Boolean b : p.getSolved()) {
                if (b == null || !b) {
                    allSolved = false;
                    break;
                }
            }
        }

        // If the users have all completed the problem, set all solved to true.
        if (allSolved) {
            game.setAllSolved(true);
        }

        return GameMapper.submissionToDto(submission);
    }

    // Get submission (either through tester or using a dummy response)
    protected Submission getSubmission(TesterRequest request) {
        // If in debug mode (tester is unavailable), return a dummy submission
        if (getDebugMode()) {
            return getDummySubmission(request);
        }

        try {
            TesterResponse testerResponse = callTesterService(request);
            ProblemDto problem = request.getProblem();

            Submission submission = new Submission();
            submission.setNumCorrect(testerResponse.getNumCorrect());
            submission.setNumTestCases(testerResponse.getNumTestCases());
            submission.setRuntime(testerResponse.getRuntime());
            submission.setCompilationError(testerResponse.getCompilationError());
            submission.setStartTime(Instant.now());
            submission.setPlayerCode(new PlayerCode(request.getCode(), request.getLanguage()));

            // Set the SubmissionResult objects, add to the list.
            int index = 0;
            List<ProblemTestCaseDto> testCaseDtos = problem.getTestCases();
            List<SubmissionResult> results = new ArrayList<>();
            for (TesterResult testerResult : testerResponse.getResults()) {
                // Match the test case details with each individual result.
                ProblemTestCaseDto testCaseDto = testCaseDtos.get(index);
                SubmissionResult submissionResult = SubmissionMapper.toSubmissionResult(testerResult, testCaseDto);
                results.add(submissionResult);
                index++;
            }
            submission.setResults(results);
            
            return submission;
        } catch (ApiException e) {
            // If custom ApiException is thrown, pass that as the response
            throw e;
        } catch (Exception e) {
            // Throw generic 500 error
            log.info("An error occurred connecting to the tester service:");
            log.error(e.getMessage());
            throw new ApiException(GameError.TESTER_ERROR);
        }
    }

    // Sends a POST request to the tester service to judge the user submission
    protected TesterResponse callTesterService(TesterRequest request) throws IOException {
        HttpPost post = new HttpPost(getTesterUrl());

        StringEntity stringEntity = new StringEntity(gson.toJson(request));
        post.setEntity(stringEntity);
        post.setHeader("Content-type", "application/json");

        HttpResponse response = httpClient.execute(post);
        String jsonResponse = EntityUtils.toString(response.getEntity());

        // Throw tester error if the tester returns an error response
        int status = response.getStatusLine().getStatusCode();
        if (status >= 400) {
            ApiErrorResponse error = new Gson().fromJson(jsonResponse, ApiErrorResponse.class);
            throw new ApiException(new TesterError(HttpStatus.valueOf(status), error));
        }

        return gson.fromJson(jsonResponse, TesterResponse.class);
    }

    // Is null in certain testing environments; if so, return a default value
    private String getTesterUrl() {
        if (testerUrl == null) {
            return "http://localhost:8080";
        }
        return testerUrl;
    }

    // Is null in certain testing environments; if so, return true by default
    private boolean getDebugMode() {
        return debugMode == null || debugMode;
    }

    // This method should only be called for testing purposes
    protected void setDebugModeForTesting(boolean debugMode) {
        this.debugMode = debugMode;
    }

    private ProblemDto getStrippedProblemDto(Problem problem) {
        ProblemDto problemDto = ProblemMapper.toDto(problem);

        // Clear irrelevant fields before sending request to tester service
        problemDto.setProblemId(null);
        problemDto.setName(null);
        problemDto.setDescription(null);
        problemDto.setDifficulty(null);

        return problemDto;
    }
}
