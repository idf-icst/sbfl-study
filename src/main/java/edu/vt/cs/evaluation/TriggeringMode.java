package edu.vt.cs.evaluation;

import edu.vt.cs.models.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static edu.vt.cs.utils.TestSubsetUtil.getFirstKthExtraAfterFirstFailedTestGenFn;
import static edu.vt.cs.utils.TestSubsetUtil.getFirstKthFailedTestGenFn;

public enum TriggeringMode {

    COMPLETE(tests -> tests),

    FIRST_FAILED_TEST(getFirstKthFailedTestGenFn.apply(1)),
    SECOND_FAILED_TEST(getFirstKthFailedTestGenFn.apply(2)),
    THIRD_FAILED_TEST(getFirstKthFailedTestGenFn.apply(3)),
    FOURTH_FAILED_TEST(getFirstKthFailedTestGenFn.apply(4)),
    FIFTH_FAILED_TEST(getFirstKthFailedTestGenFn.apply(5)),

    EXTRA_PASSED_TESTS_1(getFirstKthExtraAfterFirstFailedTestGenFn.apply(1)),
    EXTRA_PASSED_TESTS_2(getFirstKthExtraAfterFirstFailedTestGenFn.apply(2)),
    EXTRA_PASSED_TESTS_3(getFirstKthExtraAfterFirstFailedTestGenFn.apply(3)),
    EXTRA_PASSED_TESTS_4(getFirstKthExtraAfterFirstFailedTestGenFn.apply(4)),
    EXTRA_PASSED_TESTS_5(getFirstKthExtraAfterFirstFailedTestGenFn.apply(5)),
    EXTRA_PASSED_TESTS_6(getFirstKthExtraAfterFirstFailedTestGenFn.apply(6)),
    EXTRA_PASSED_TESTS_7(getFirstKthExtraAfterFirstFailedTestGenFn.apply(7)),
    EXTRA_PASSED_TESTS_8(getFirstKthExtraAfterFirstFailedTestGenFn.apply(8)),
    EXTRA_PASSED_TESTS_9(getFirstKthExtraAfterFirstFailedTestGenFn.apply(9)),
    EXTRA_PASSED_TESTS_10(getFirstKthExtraAfterFirstFailedTestGenFn.apply(10));

    final UnaryOperator<List<Test>> toTestSubSet;

    TriggeringMode(UnaryOperator<List<Test>> toTestSubSet) {
        this.toTestSubSet = toTestSubSet;
    }

    public UnaryOperator<List<Test>> getSubSetFn() {
        return toTestSubSet;
    }
}
