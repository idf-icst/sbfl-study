package edu.vt.cs.models;

import edu.vt.cs.evaluation.EvalResult;

import java.util.function.Function;

public enum ResultReducedType {
    BY_TRIGGERING_MODE(EvalResult::getTriggeringMode),

    BY_ALGORITHM(EvalResult::getRankingAlgorithm),

    BY_PROJECT(EvalResult::getProject),

    BY_BUG(evalResult -> evalResult.getProject() + "::" + evalResult.getBugId());

    final Function<EvalResult, Object> reducer;

    ResultReducedType(Function<EvalResult, Object> reducer) {
        this.reducer = reducer;
    }

    public Function<EvalResult, Object> getReducer() {
        return reducer;
    }
}
