package com.codejoust.main.dto.problem;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import com.codejoust.main.dto.account.AccountUidDto;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.model.problem.ProblemIOType;

@Getter
@Setter
@EqualsAndHashCode
public class ProblemDto {
    private String problemId;
    private String name;
    private AccountUidDto owner;
    private String description;
    private Boolean verified;
    private ProblemDifficulty difficulty;
    private List<ProblemTestCaseDto> testCases = new ArrayList<>();
    private List<ProblemTagDto> problemTags = new ArrayList<>();
    private List<ProblemInputDto> problemInputs;
    private ProblemIOType outputType;
}
