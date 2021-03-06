package com.codejoust.main.dto.game;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TesterResponse {
    // This class is mapped directly to the tester service's RunDto class.
    private List<TesterResult> results;
    private Integer numCorrect;
    private Integer numTestCases;
    private Double runtime;
    private String compilationError;
}
