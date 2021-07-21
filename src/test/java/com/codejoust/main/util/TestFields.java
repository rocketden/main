package com.codejoust.main.util;

import com.codejoust.main.dto.account.AccountRole;
import com.codejoust.main.dto.account.AccountUidDto;
import com.codejoust.main.dto.problem.CreateProblemRequest;
import com.codejoust.main.dto.problem.ProblemInputDto;
import com.codejoust.main.dto.user.UserDto;
import com.codejoust.main.game_object.PlayerCode;
import com.codejoust.main.model.Account;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.model.problem.ProblemIOType;
import com.codejoust.main.model.problem.ProblemInput;
import com.codejoust.main.model.problem.ProblemTag;
import com.codejoust.main.model.problem.ProblemTestCase;
import com.codejoust.main.model.report.CodeLanguage;
import com.codejoust.main.service.FirebaseService;

import java.util.ArrayList;
import java.util.List;

public class TestFields {

    // User constants
    public static final String NICKNAME = "rocket";
    public static final String NICKNAME_2 = "rocketrocket";
    public static final String NICKNAME_3 = "rocketandrocket";
    public static final String NICKNAME_4 = "rocketrocketrocket";
    public static final String NICKNAME_5 = "rocketandrocketrocket";
    public static final String USER_ID = "012345";
    public static final String USER_ID_2 = "678910";
    public static final String USER_ID_3 = "024681";
    public static final String USER_ID_4 = "401905";
    public static final String SESSION_ID = "abcde";
    public static final String SESSION_ID_2 = "fghij";
    public static final Integer ID = 1;
    public static final Integer ID_2 = 2;

    // Room constants
    public static final String ROOM_ID = "012345";
    public static final long DURATION = 600;

    // Problem constants
    public static final String PROBLEM_NAME = "Sort an Array";
    public static final String PROBLEM_DESCRIPTION = "Sort an array from lowest to highest value.";
    public static final String PROBLEM_ID = "abcxyz";
    public static final String INPUT_NAME = "nums";
    public static final ProblemIOType IO_TYPE = ProblemIOType.ARRAY_INTEGER;
    public static final String TAG_NAME = "Binary Search";
    public static final String TAG_ID = "klmno";

    public static final String INPUT = "[1, 3, 2]";
    public static final String OUTPUT = "[1, 2, 3]";
    public static final String EXPLANATION = "2 < 3, so those are swapped.";
    public static final String INPUT_2 = "[-1, 5, 0, 3]";
    public static final String OUTPUT_2 = "[-1, 0, 3, 5]";
    public static final String EXPLANATION_2 = "5 is the largest, so it should be at the end.";
    public static final Double RUNTIME = 5.5;

    public static final String PROBLEM_NAME_2 = "Find Maximum";
    public static final String PROBLEM_DESCRIPTION_2 = "Find the maximum value in an array.";
    public static final String PROBLEM_ID_2 = "zyx-cba";
    public static final ProblemIOType IO_TYPE_2 = ProblemIOType.INTEGER;
    public static final String INPUT_3 = "[1, 2, 8]";
    public static final String OUTPUT_3 = "8";

    public static final String PYTHON_CODE = "print('hello')";
    public static final CodeLanguage PYTHON_LANGUAGE = CodeLanguage.PYTHON;
    public static final PlayerCode PLAYER_CODE_1 = new PlayerCode(PYTHON_CODE, PYTHON_LANGUAGE);

    public static final String JAVA_CODE = "System.out.println(\"hello\");";
    public static final CodeLanguage JAVA_LANGUAGE = CodeLanguage.JAVA;
    public static final PlayerCode PLAYER_CODE_2 = new PlayerCode(JAVA_CODE, JAVA_LANGUAGE);

    public static final Integer NUM_PROBLEMS = 10;

    // Notification constants
    public static final String CONTENT = "[1, 2, 3]";
    public static final String TIME_CONTENT = "are thirty minutes";
    public static final String NAME = "Sort a List";
    public static final String DESCRIPTION = "Sort the given list in O(n log n) time.";

    // Account constants
    public static final String TOKEN = "aWbXcYdZ123";
    public static final String UID = FirebaseService.TEST_UID;

    public static final String TOKEN_2 = "tYrUeIwO99";
    public static final String UID_2 = FirebaseService.TEST_UID_2;

    public static UserDto userDto1() {
        UserDto user = new UserDto();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        return user;
    }

    public static UserDto userDto2() {
        UserDto user = new UserDto();
        user.setNickname(NICKNAME_2);
        user.setUserId(USER_ID_2);
        return user;
    }

    public static UserDto userDto3() {
        UserDto user = new UserDto();
        user.setNickname(NICKNAME_3);
        user.setUserId(USER_ID_3);
        return user;
    }

    // Problem owned by admin
    public static Problem problem1() {
        Problem problem = new Problem();
        problem.setName(TestFields.PROBLEM_NAME);
        problem.setDescription(TestFields.PROBLEM_DESCRIPTION);
        problem.setDifficulty(ProblemDifficulty.MEDIUM);
        problem.setOwner(account1());

        ProblemInput problemInput = new ProblemInput(TestFields.INPUT_NAME, TestFields.IO_TYPE);
        problem.addProblemInput(problemInput);
        problem.setOutputType(TestFields.IO_TYPE_2);

        ProblemTestCase originalTestCase = new ProblemTestCase();
        originalTestCase.setInput(TestFields.INPUT_3);
        originalTestCase.setOutput(TestFields.OUTPUT_3);
        problem.addTestCase(originalTestCase);

        return problem;
    }

    // Problem owned by teacher
    public static Problem problem2() {
        Problem problem = new Problem();
        problem.setName(TestFields.PROBLEM_NAME_2);
        problem.setDescription(TestFields.PROBLEM_DESCRIPTION_2);
        problem.setDifficulty(ProblemDifficulty.HARD);
        problem.setOwner(account2());

        ProblemInput problemInput = new ProblemInput(TestFields.INPUT_NAME, TestFields.IO_TYPE);
        problem.addProblemInput(problemInput);
        problem.setOutputType(TestFields.IO_TYPE);

        ProblemTestCase originalTestCase = new ProblemTestCase();
        originalTestCase.setInput(TestFields.INPUT);
        originalTestCase.setOutput(TestFields.OUTPUT);
        problem.addTestCase(originalTestCase);

        return problem;
    }

    public static ProblemTag problemTag1() {
        ProblemTag tag = new ProblemTag();
        tag.setTagId(TAG_ID);
        tag.setName(TAG_NAME);
        tag.setOwner(account1());

        return tag;
    }

    public static CreateProblemRequest createProblemRequest1() {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setName(TestFields.NAME);
        request.setDescription(TestFields.DESCRIPTION);
        request.setDifficulty(ProblemDifficulty.MEDIUM);

        List<ProblemInputDto> problemInputs = new ArrayList<>();
        ProblemInputDto problemInput = new ProblemInputDto(TestFields.INPUT_NAME, TestFields.IO_TYPE);
        problemInputs.add(problemInput);
        request.setProblemInputs(problemInputs);
        request.setOutputType(TestFields.IO_TYPE);

        return request;
    }

    public static Account account1() {
        Account account = new Account();
        account.setUid(UID);
        account.setRole(AccountRole.ADMIN);
        return account;
    }

    public static Account account2() {
        Account account = new Account();
        account.setUid(UID_2);
        account.setRole(AccountRole.TEACHER);
        return account;
    }

    public static AccountUidDto accountUidDto1() {
        AccountUidDto account = new AccountUidDto();
        account.setUid(UID);
        return account;
    }
}
