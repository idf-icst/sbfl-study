package edu.vt.cs.models;

import edu.vt.cs.evaluation.EvalResult;

import java.util.Map;
import java.util.function.Function;

public enum ResultReducedType {
    BY_TRIGGERING_MODE(Map.of(
            BugType.REAL, Map.of(Constants.RESULT_OF_REAL_BUGS_CSV, Constants.RESULT_REAL_BY_TRIGGERING_MODE_CSV),
            BugType.ARTIFICIAL, Map.of(Constants.RESULT_OF_ARTIFICIAL_BUGS_CSV, Constants.RESULT_ARTIFICIAL_BY_TRIGGERING_MODE_CSV)
    ), EvalResult::getTriggeringMode),
    BY_ALGORITHM(Map.of(
            BugType.REAL, Map.of(Constants.RESULT_OF_REAL_BUGS_CSV, Constants.RESULT_REAL_BY_ALGORITHM_CSV),
            BugType.ARTIFICIAL, Map.of(Constants.RESULT_OF_ARTIFICIAL_BUGS_CSV, Constants.RESULT_ARTIFICIAL_BY_ALGORITHM_CSV)
    ), EvalResult::getRankingAlgorithm),
    BY_PROJECT(Map.of(
            BugType.REAL, Map.of(Constants.RESULT_OF_REAL_BUGS_CSV, Constants.RESULT_REAL_BY_PROJECT_CSV),
            BugType.ARTIFICIAL, Map.of(Constants.RESULT_OF_ARTIFICIAL_BUGS_CSV, Constants.RESULT_ARTIFICIAL_BY_PROJECT_CSV)
    ), EvalResult::getProject),
    BY_BUG(Map.of(
            BugType.REAL, Map.of(Constants.RESULT_OF_REAL_BUGS_CSV, Constants.RESULT_REAL_BY_BUG_CSV),
            BugType.ARTIFICIAL, Map.of(Constants.RESULT_OF_ARTIFICIAL_BUGS_CSV, Constants.RESULT_ARTIFICIAL_BY_BUG_CSV)
    ), evalResult -> evalResult.getProject() + "::" + evalResult.getBugId());

    final Map<BugType, Map<String, String>> inputOutputConfig;
    final Function<EvalResult, Object> reducer;

    ResultReducedType(Map<BugType, Map<String, String>> inputOutputConfig, Function<EvalResult, Object> reducer) {
        this.inputOutputConfig = inputOutputConfig;
        this.reducer = reducer;
    }

    public Map<BugType, Map<String, String>> getIOConfigs() {
        return inputOutputConfig;
    }

    public Function<EvalResult, Object> getReducer() {
        return reducer;
    }
}
