package com.codejoust.main.service.generators;

import java.util.List;

import com.codejoust.main.game_object.CodeLanguage;
import com.codejoust.main.model.problem.ProblemIOType;
import com.codejoust.main.model.problem.ProblemInput;

import org.springframework.stereotype.Service;

@Service
public class PythonDefaultCodeGeneratorService implements DefaultCodeGeneratorService {

    @Override
    public String getDefaultCode(List<ProblemInput> problemInputs, ProblemIOType outputType) {

        // Initialize method line StringBuilder.
        StringBuilder methodLineBuilder = new StringBuilder();
        methodLineBuilder.append("\tdef solve(");

        // Add all of the method input names.
        String prefix = "";
        for (ProblemInput problemInput : problemInputs) {
            methodLineBuilder.append(prefix);
            prefix = ", ";
            methodLineBuilder.append(problemInput.getName());
        }
        methodLineBuilder.append("):");

        return String.join("\n",
            "class Solution(object):",
            methodLineBuilder.toString(),
            "\t\t"
        );
    }

    @Override
    public String typeInstantiationToString(ProblemIOType ioType) {
        // Python does not require type instantiation.
        return null;
    }

    @Override
    public CodeLanguage getLanguage() {
        return CodeLanguage.PYTHON;
    }
}