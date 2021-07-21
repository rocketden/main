package com.codejoust.main.service;

import com.codejoust.main.dao.AccountRepository;
import com.codejoust.main.dao.ProblemContainerRepository;
import com.codejoust.main.dao.ProblemRepository;
import com.codejoust.main.dao.ProblemTagRepository;
import com.codejoust.main.dao.RoomRepository;
import com.codejoust.main.dto.account.AccountRole;
import com.codejoust.main.dto.problem.CreateProblemRequest;
import com.codejoust.main.dto.problem.CreateProblemTagRequest;
import com.codejoust.main.dto.problem.CreateTestCaseRequest;
import com.codejoust.main.dto.problem.ProblemDto;
import com.codejoust.main.dto.problem.ProblemInputDto;
import com.codejoust.main.dto.problem.ProblemMapper;
import com.codejoust.main.dto.problem.ProblemTagDto;
import com.codejoust.main.dto.problem.ProblemTestCaseDto;
import com.codejoust.main.exception.AccountError;
import com.codejoust.main.exception.ProblemError;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.model.Account;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemContainer;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.model.problem.ProblemIOType;
import com.codejoust.main.model.problem.ProblemInput;
import com.codejoust.main.model.problem.ProblemTag;
import com.codejoust.main.model.problem.ProblemTestCase;
import com.codejoust.main.model.report.CodeLanguage;
import com.codejoust.main.service.generators.DefaultCodeGeneratorService;
import com.codejoust.main.util.Utility;
import com.google.gson.Gson;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ProblemService {
    
    private final FirebaseService service;
    private final ProblemRepository problemRepository;
    private final ProblemTagRepository problemTagRepository;
    private final ProblemContainerRepository problemContainerRepository;
    private final AccountRepository accountRepository;
    private final RoomRepository roomRepository;
    private final List<DefaultCodeGeneratorService> defaultCodeGeneratorServiceList;
    private final Random random = new Random();
    private final Gson gson = new Gson();

    @Autowired
    public ProblemService(FirebaseService service,
        ProblemRepository problemRepository,
        ProblemTagRepository problemTagRepository,
        ProblemContainerRepository problemContainerRepository,
        AccountRepository accountRepository,
        RoomRepository roomRepository,
        List<DefaultCodeGeneratorService> defaultCodeGeneratorServiceList) {

        this.service = service;
        this.problemRepository = problemRepository;
        this.problemTagRepository = problemTagRepository;
        this.problemContainerRepository = problemContainerRepository;
        this.accountRepository = accountRepository;
        this.roomRepository = roomRepository;
        this.defaultCodeGeneratorServiceList = defaultCodeGeneratorServiceList;
    }

    public ProblemDto createProblem(CreateProblemRequest request, String token) {
        String uid = service.verifyToken(token);

        if (request.getName() == null || request.getDescription() == null
                || request.getName().isEmpty() || request.getDescription().isEmpty()
                || request.getDifficulty() == null
                || request.getProblemInputs() == null
                || request.getOutputType() == null) {
            throw new ApiException(ProblemError.EMPTY_FIELD);
        }

        if (request.getDifficulty() == ProblemDifficulty.RANDOM) {
            throw new ApiException(ProblemError.BAD_DIFFICULTY);
        }

        Problem problem = new Problem();
        problem.setName(request.getName());
        problem.setDescription(request.getDescription());
        problem.setDifficulty(request.getDifficulty());
        problem.setOutputType(request.getOutputType());

        // Add all problem inputs in list.
        for (ProblemInputDto problemInput : request.getProblemInputs()) {
            if (problemInput == null) {
                throw new ApiException(ProblemError.BAD_INPUT);
            } else if (!Utility.validateIdentifier(problemInput.getName())) {
                throw new ApiException(ProblemError.INVALID_VARIABLE_NAME);
            } else {
                problem.addProblemInput(ProblemMapper.toProblemInputEntity(problemInput));
            }
        }

        // Set the owner of this problem (guaranteed to be non-null due to the verifyToken call)
        problem.setOwner(accountRepository.findAccountByUid(uid));
        problemRepository.save(problem);

        return ProblemMapper.toDto(problem);
    }

    public ProblemDto getProblem(String problemId, String token) {
        Problem problem = problemRepository.findProblemByProblemId(problemId);

        if (problem == null || problem.getOwner() == null) {
            throw new ApiException(ProblemError.NOT_FOUND);
        }

        if (!problem.getVerified()) {
            service.verifyTokenMatchesUid(token, problem.getOwner().getUid());
        }

        return ProblemMapper.toDto(problem);
    }

    // Method used by game management service to fetch specific problem
    public Problem getProblemEntity(String problemId) {
        return problemRepository.findProblemByProblemId(problemId);
    }

    public ProblemDto editProblem(String problemId, ProblemDto updatedProblem, String token) {
        Problem problem = problemRepository.findProblemByProblemId(problemId);

        if (problem == null) {
            throw new ApiException(ProblemError.NOT_FOUND);
        }

        if (updatedProblem == null || updatedProblem.getName() == null
                || updatedProblem.getDescription() == null
                || updatedProblem.getName().isEmpty()
                || updatedProblem.getDescription().isEmpty()
                || updatedProblem.getOwner() == null
                || updatedProblem.getDifficulty() == null
                || updatedProblem.getProblemInputs() == null
                || updatedProblem.getTestCases() == null
                || updatedProblem.getOutputType() == null
                || updatedProblem.getVerified() == null) {
            throw new ApiException(ProblemError.EMPTY_FIELD);
        }

        service.verifyTokenMatchesUid(token, problem.getOwner().getUid());

        if (updatedProblem.getVerified() != problem.getVerified() && problem.getOwner().getRole() != AccountRole.ADMIN) {
            throw new ApiException(AccountError.INVALID_CREDENTIALS);
        }

        if (updatedProblem.getVerified() && updatedProblem.getTestCases().size() == 0) {
            throw new ApiException(ProblemError.BAD_VERIFIED_STATUS);
        }

        if (updatedProblem.getDifficulty() == ProblemDifficulty.RANDOM) {
            throw new ApiException(ProblemError.BAD_DIFFICULTY);
        }

        problem.setName(updatedProblem.getName());
        problem.setDescription(updatedProblem.getDescription());
        problem.setDifficulty(updatedProblem.getDifficulty());
        problem.setOutputType(updatedProblem.getOutputType());
        problem.setVerified(updatedProblem.getVerified());

        problem.getProblemInputs().clear();
        for (ProblemInputDto problemInput : updatedProblem.getProblemInputs()) {
            if (!Utility.validateIdentifier(problemInput.getName())) {
                throw new ApiException(ProblemError.INVALID_VARIABLE_NAME);
            }

            problem.addProblemInput(ProblemMapper.toProblemInputEntity(problemInput));
        }

        problem.getTestCases().clear();
        for (ProblemTestCaseDto testCaseDto : updatedProblem.getTestCases()) {
            // Ensure that the user entered valid inputs and outputs for the problem
            validateInputsGsonParseable(testCaseDto.getInput(), updatedProblem.getProblemInputs());
            validateGsonParseable(testCaseDto.getOutput(), updatedProblem.getOutputType());

            ProblemTestCase testCase = new ProblemTestCase();
            testCase.setInput(testCaseDto.getInput());
            testCase.setOutput(testCaseDto.getOutput());
            testCase.setHidden(testCaseDto.isHidden());
            testCase.setExplanation(testCaseDto.getExplanation());

            problem.addTestCase(testCase);
        }

        // If the same tag name is added multiple times, throw error.
        List<ProblemTagDto> updatedProblemTags = updatedProblem.getProblemTags();
        if (!problemTagNamesUnique(updatedProblemTags)) {
            throw new ApiException(ProblemError.DUPLICATE_TAG_NAME);
        }

        Account owner = problem.getOwner();

        problem.getProblemTags().clear();
        for (ProblemTagDto problemTagDto : updatedProblemTags) {
            // Add the tag if the name already exists, or create a new one.
            ProblemTag existingProblemTag = problemTagRepository.findTagByNameAndOwner_Uid(problemTagDto.getName(), owner.getUid());
            if (existingProblemTag != null) {
                problem.addProblemTag(existingProblemTag);
            } else if (validProblemTagName(problemTagDto.getName())) {
                ProblemTag problemTag = new ProblemTag();
                problemTag.setName(problemTagDto.getName());
                problemTag.setOwner(owner);
                problem.addProblemTag(problemTag);
            } else {
                throw new ApiException(ProblemError.BAD_PROBLEM_TAG);
            }
        }

        problemRepository.save(problem);

        return ProblemMapper.toDto(problem);
    }

    /**
     * Helper method that returns true iff all problem tag names provided are
     * unique within the list, false otherwise.
     * 
     * @param problemTags The problem tag DTOs in question.
     * @return true if all names are unique, false otherwise.
     */
    private boolean problemTagNamesUnique(List<ProblemTagDto> problemTags) {
        Set<String> problemTagNames = new HashSet<>();
        for (ProblemTagDto problemTag : problemTags) {
            // If problem tag name is already recorded, return false.
            if (problemTagNames.contains(problemTag.getName())) {
                return false;
            }
            problemTagNames.add(problemTag.getName());
        }
        return true;
    }

    public ProblemDto deleteProblem(String problemId, String token) {
        Problem problem = problemRepository.findProblemByProblemId(problemId);
        if (problem == null) {
            throw new ApiException(ProblemError.NOT_FOUND);
        }

        service.verifyTokenMatchesUid(token, problem.getOwner().getUid());

        /**
         * Before the problem can be deleted, set all foreign key references
         * to the Problem within all ProblemContainers to null.
         * See https://stackoverflow.com/a/10030873/7517518.
         */
        List<ProblemContainer> problemContainers = problemContainerRepository.findAllByProblem(problem);
        for (ProblemContainer problemContainer : problemContainers) {
            problemContainer.setProblem(null);
            problemContainerRepository.save(problemContainer);
        }

        // Remove this problem from all the associated rooms.
        List<Room> rooms = roomRepository.findByProblems_ProblemId(problemId);
        for (Room room : rooms) {
            room.removeProblem(problem);
            roomRepository.save(room);
        }

        problemRepository.delete(problem);

        return ProblemMapper.toDto(problem);
    }

    public List<ProblemDto> getAllProblems(Boolean verified, String token) {
        List<ProblemDto> problems = new ArrayList<>();
        if (verified != null && verified) {
            problemRepository.findAllByVerified(true).forEach(problem -> problems.add(ProblemMapper.toDto(problem)));
        } else {
            service.verifyAdminAccount(token);
            problemRepository.findAll().forEach(problem -> problems.add(ProblemMapper.toDto(problem)));
        }

        return problems;
    }

    /**
     * Get a list of random problems with the provided parameters.
     * 
     * @param difficulty The problem difficulty the problems must match.
     * @param numProblems The number of problems to fetch.
     */
    public List<Problem> getProblemsFromDifficulty(ProblemDifficulty difficulty, Integer numProblems) {
        if (difficulty == null || numProblems == null) {
            throw new ApiException(ProblemError.EMPTY_FIELD);
        }

        if (numProblems <= 0) {
            throw new ApiException(ProblemError.INVALID_NUMBER_REQUEST);
        }

        List<Problem> problems;
        if (difficulty == ProblemDifficulty.RANDOM) {
            problems = problemRepository.findAllByVerified(true);
            log.info("Fetching random problems");
        } else {
            problems = problemRepository.findAllByDifficultyAndVerified(difficulty, true);
            log.info("Fetching problems for difficulty {}", difficulty);
        }
        log.info(problems);

        if (problems == null) {
            throw new ApiException(ProblemError.INTERNAL_ERROR);
        }

        // If the user wants more problems than exists, throw an error
        if (numProblems > problems.size()) {
            throw new ApiException(ProblemError.NOT_ENOUGH_FOUND);
        }

        // Get numProblem random integers used to map to problems.
        Set<Integer> randomIntegers = new HashSet<>();
        while (randomIntegers.size() < numProblems) {
            randomIntegers.add(random.nextInt(problems.size()));
        }

        // Get the numProblem problems mapped to those integers.
        List<Problem> chosenProblems = new ArrayList<>();
        for (Integer i : randomIntegers) {
            chosenProblems.add(problems.get(i));
        }

        return chosenProblems;
    }

    public ProblemTestCaseDto createTestCase(String problemId, CreateTestCaseRequest request, String token) {
        Problem problem = problemRepository.findProblemByProblemId(problemId);
        if (problem == null) {
            throw new ApiException(ProblemError.NOT_FOUND);
        }

        service.verifyTokenMatchesUid(token, problem.getOwner().getUid());

        // Problem input and output are the two required fields.
        if (request.getInput() == null || request.getOutput() == null) {
            throw new ApiException(ProblemError.EMPTY_FIELD);
        }

        // Verify inputs are of valid form
        List<ProblemInputDto> inputs = problem.getProblemInputs()
                .stream()
                .map(ProblemMapper::toProblemInputDto)
                .collect(Collectors.toList());

        validateInputsGsonParseable(request.getInput(), inputs);
        validateGsonParseable(request.getOutput(), problem.getOutputType());


        ProblemTestCase testCase = new ProblemTestCase();
        testCase.setInput(request.getInput());
        testCase.setOutput(request.getOutput());

        // Test case is not hidden by default.
        testCase.setHidden(request.isHidden());

        // Explanation may be null, indicating no explanation is attached.
        testCase.setExplanation(request.getExplanation());

        problem.addTestCase(testCase);
        problemRepository.save(problem);

        return ProblemMapper.toTestCaseDto(testCase);
    }

    // Check to make sure test case inputs and outputs are Gson-parsable
    protected void validateInputsGsonParseable(String input, List<ProblemInputDto> types) {
        if (input == null) {
            throw new ApiException(ProblemError.INVALID_INPUT);
        }

        // Each parameter input should be on a separate line
        String[] inputs = input.trim().split("\n");
        if (inputs.length != types.size()) {
            throw new ApiException(ProblemError.INCORRECT_INPUT_COUNT);
        }

        for (int i = 0; i < types.size(); i++) {
            ProblemInputDto type = types.get(i);
            if (type == null || type.getName() == null || type.getName().isEmpty() || type.getType() == null) {
                throw new ApiException(ProblemError.BAD_INPUT);
            }

            validateGsonParseable(inputs[i], types.get(i).getType());
        }
    }

    private void validateGsonParseable(String input, ProblemIOType type) {
        if (input == null) {
            throw new ApiException(ProblemError.INVALID_INPUT);
        }

        try {
            Object result = gson.fromJson(input, type.getClassType());

            // Trigger catch block to return invalid input error
            if (result == null) {
                throw new ApiException(ProblemError.INVALID_INPUT);
            }

        } catch (Exception e) {
            throw new ApiException(ProblemError.INVALID_INPUT);
        }
    }
    
    public Map<CodeLanguage, String> getDefaultCode(String problemId) {
        // Convert from the Problem object to Problem DTOs.
        Problem problem = problemRepository.findProblemByProblemId(problemId);

        if (problem == null) {
            throw new ApiException(ProblemError.NOT_FOUND);
        }
        
        Map<CodeLanguage, String> defaultCodeMap = new EnumMap<>(CodeLanguage.class);
        
        // Get the relevant problem type information.
        List<ProblemInput> problemInputs = problem.getProblemInputs();
        ProblemIOType outputType = problem.getOutputType();

        // Loop through all default code generators, and add them to EnumMap.
        for (DefaultCodeGeneratorService defaultCodeGeneratorService : defaultCodeGeneratorServiceList) {
            CodeLanguage language = defaultCodeGeneratorService.getLanguage();
            String defaultCode = defaultCodeGeneratorService.getDefaultCode(problemInputs, outputType);
            // Replace tabs with spaces
            defaultCodeMap.put(language, defaultCode.replace("\t", "    "));
        }

        return defaultCodeMap;
    }

    public List<ProblemDto> getProblemsWithTag(String tagId) {
        List<Problem> problems = problemRepository.findByProblemTags_TagId(tagId);

        List<ProblemDto> problemDtos = new ArrayList<>();
        problems.forEach(problem -> problemDtos.add(ProblemMapper.toDto(problem)));
        return problemDtos;
    }

    public List<ProblemTagDto> getAllProblemTags(String token) {
        // Get all problem tags this person owns and convert them to DTOs.
        String uid = service.verifyToken(token);

        List<ProblemTagDto> problemTagDtos = new ArrayList<>();
        problemTagRepository.findAllByOwner_Uid(uid).forEach(problemTag -> problemTagDtos.add(ProblemMapper.toProblemTagDto(problemTag)));
        return problemTagDtos;
    }

    public ProblemTagDto createProblemTag(CreateProblemTagRequest request, String token) {
        String uid = service.verifyToken(token);

        ProblemTag existingProblemTag = problemTagRepository.findTagByNameAndOwner_Uid(request.getName(), uid);

        // Handle invalid request, with restraints on the length of the name.
        if (!validProblemTagName(request.getName())) {
            throw new ApiException(ProblemError.BAD_PROBLEM_TAG);
        }

        // Do not create the new problem tag if one with this name exists.
        if (existingProblemTag != null) {
            throw new ApiException(ProblemError.TAG_NAME_ALREADY_EXISTS);
        }

        // Add the problem tag to the database.
        ProblemTag problemTag = new ProblemTag();
        problemTag.setName(request.getName());
        problemTag.setOwner(accountRepository.findAccountByUid(uid));
        problemTagRepository.save(problemTag);
        return ProblemMapper.toProblemTagDto(problemTag);
    }

    public ProblemTagDto deleteProblemTag(String tagId, String token) {
        ProblemTag problemTag = problemTagRepository.findTagByTagId(tagId);

        if (problemTag == null || problemTag.getOwner() == null) {
            throw new ApiException(ProblemError.TAG_NOT_FOUND);
        }

        service.verifyTokenMatchesUid(token, problemTag.getOwner().getUid());

        // Remove this tag from problems associated with it
        List<Problem> problems = problemRepository.findByProblemTags_TagId(tagId);
        for (Problem problem : problems) {
            problem.removeProblemTag(problemTag);
            problemRepository.save(problem);
        }

        // Remove the problem tag from the database.
        ProblemTagDto problemTagDto = ProblemMapper.toProblemTagDto(problemTag);
        problemTagRepository.delete(problemTag);
        return problemTagDto;
    }

    private boolean validProblemTagName(String name) {
        return name != null && name.length() > 0 && name.length() < 20;
    }
}
