package com.codejoust.main.game_object;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class Submission {

    private PlayerCode playerCode;

    private int problemIndex;

    private List<SubmissionResult> results;

    // The time that the submission was received.
    private Instant startTime = Instant.now();

    private Integer numCorrect;

    private Integer numTestCases;

    private Double runtime;
    
    private String compilationError;
}
