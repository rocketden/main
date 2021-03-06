package com.codejoust.main.dao;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemDifficulty;

// This will be AUTO IMPLEMENTED by Spring into a Bean called problemRepository
// CRUD refers Create, Read, Update, Delete
public interface ProblemRepository extends CrudRepository<Problem, Integer> {

    Problem findProblemByProblemId(String problemId);
    List<Problem> findAllByDifficultyAndVerified(ProblemDifficulty difficulty, Boolean verified);
    List<Problem> findAllByVerified(Boolean verified);
    List<Problem> findByProblemTags_TagId(String tagId);
    @Override
    List<Problem> findAll();
}
