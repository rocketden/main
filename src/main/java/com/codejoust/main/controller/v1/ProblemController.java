package com.codejoust.main.controller.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codejoust.main.dto.problem.CreateProblemRequest;
import com.codejoust.main.dto.problem.CreateTestCaseRequest;
import com.codejoust.main.dto.problem.ProblemDto;
import com.codejoust.main.dto.problem.ProblemMapper;
import com.codejoust.main.dto.problem.ProblemSettingsDto;
import com.codejoust.main.dto.problem.ProblemTestCaseDto;
import com.codejoust.main.game_object.CodeLanguage;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.service.ProblemService;

@RestController
public class ProblemController extends BaseRestController {

    private final ProblemService service;

    @Autowired
    public ProblemController(ProblemService service) {
        this.service = service;
    }

    @PostMapping("/problems")
    public ResponseEntity<ProblemDto> createProblem(@RequestBody CreateProblemRequest request) {
        return new ResponseEntity<>(service.createProblem(request), HttpStatus.CREATED);

    }

    @GetMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDto> getProblem(@PathVariable String problemId) {
        return new ResponseEntity<>(service.getProblem(problemId), HttpStatus.OK);
    }

    @PutMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDto> editProblem(@PathVariable String problemId, @RequestBody ProblemDto updatedProblem) {
        return new ResponseEntity<>(service.editProblem(problemId, updatedProblem), HttpStatus.OK);
    }

    @DeleteMapping("/problems/{problemId}")
    public ResponseEntity<ProblemDto> deleteProblem(@PathVariable String problemId) {
        return new ResponseEntity<>(service.deleteProblem(problemId), HttpStatus.OK);
    }


    @PostMapping("/problems/{problemId}/test-case")
    public ResponseEntity<ProblemTestCaseDto> createTestCase(@PathVariable String problemId, @RequestBody CreateTestCaseRequest request) {
        return new ResponseEntity<>(service.createTestCase(problemId, request), HttpStatus.CREATED);
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemDto>> getAllProblems(@RequestParam(required = false) Boolean approved) {
        return new ResponseEntity<>(service.getAllProblems(approved), HttpStatus.OK);
    }

    // Note: Since this GET request takes query parameters, the difficulty to enum
    // conversion does not automatically match case (i.e. Easy != easy != EASY)
    @GetMapping("/problems/random")
    public ResponseEntity<List<ProblemDto>> getRandomProblem(ProblemSettingsDto request) {
        // Convert from the Problem object to Problem DTOs.
        List<Problem> problems = service.getProblemsFromDifficulty(request.getDifficulty(), request.getNumProblems());
        List<ProblemDto> problemDtos = new ArrayList<>();
        problems.forEach(problem -> problemDtos.add(ProblemMapper.toDto(problem)));
        return new ResponseEntity<>(problemDtos, HttpStatus.OK);
    }

    @GetMapping("/problems/{problemId}/default-code")
    public ResponseEntity<Map<CodeLanguage, String>> getDefaultCode(@PathVariable String problemId) {
        return new ResponseEntity<>(service.getDefaultCode(problemId), HttpStatus.OK);
    }

    @GetMapping("/problems/access/{password}")
    public ResponseEntity<Boolean> accessProblems(@PathVariable String password) {
        return new ResponseEntity<>(service.accessProblems(password), HttpStatus.OK);
    }
}