package edu.vt.cs.utils;

import edu.vt.cs.models.Result;
import edu.vt.cs.models.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class TestSubsetUtil {
    public static final Function<Integer, UnaryOperator<List<Test>>> getFirstKthFailedTestGenFn = k -> tests -> {
        int indexOfKthFailedTest = -1;
        int count = 0;

        boolean found = false;

        for (int i=0; i<tests.size(); i++) {
            if (tests.get(i).getResult() == Result.Failed) {
                indexOfKthFailedTest = i;
                count++;
                if (k == count) {
                    found = true;
                    break;
                }
            }
        }

        if (indexOfKthFailedTest == -1) {
            throw new RuntimeException("There is no failed test");
        }

        return found ? tests.subList(0, indexOfKthFailedTest + 1) : List.of();
    };

    public static final Function<Integer, UnaryOperator<List<Test>>> getFirstKthExtraAfterFirstFailedTestGenFn = k -> tests -> {
        int indexOfKthFailedTest = -1;

        for (int i=0; i<tests.size(); i++) {
            if (tests.get(i).getResult() == Result.Failed) {
                indexOfKthFailedTest = i;
                break;
            }
        }

        if (indexOfKthFailedTest == -1) {
            throw new RuntimeException("There is no failed test");
        }

        int kIndex = indexOfKthFailedTest + k;

        return kIndex < tests.size() ? tests.subList(0, kIndex + 1) : List.of();
    };
}
