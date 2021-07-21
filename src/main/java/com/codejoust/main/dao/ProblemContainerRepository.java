package com.codejoust.main.dao;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemContainer;

// This will be AUTO IMPLEMENTED by Spring into a Bean called 
// problemContainerRepository; CRUD refers Create, Read, Update, Delete
public interface ProblemContainerRepository extends CrudRepository<ProblemContainer, Integer> {
    List<ProblemContainer> findAllByProblem(Problem problem);
}
