package edu.vt.cs.utils;

import edu.vt.cs.evaluation.TriggeringMode;
import edu.vt.cs.models.Bug;
import edu.vt.cs.models.BugType;
import edu.vt.cs.models.Result;
import edu.vt.cs.models.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestSubsetUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    public static <T> List<List<T>> divideListBySizeK(List<T> lst, int k) {
        return new ArrayList<>(IntStream.range(0, lst.size())
                .boxed()
                .collect(Collectors.groupingBy(
                        i -> i / k,
                        Collectors.mapping(lst::get, Collectors.toList())))
                .values());
    }

    private static final Map<Bug, Integer> cache = new ConcurrentHashMap<>();

    /**
     * Calculate average percentage of tests a triggering mode covered against complete mode
     */
    public static void calPortionPercentage(TriggeringMode triggeringMode, BugType bugType) {

        List<Bug> bugs;
        try {
            bugs = BugParser.derBugs(bugType.getInputBugInfoFilePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int executedTestCount = 0;
        int completedTestCount = 0;

        StringBuilder sb = new StringBuilder();

        var header = "Triggering Mode, BugId, #Executed Tests, #Completed Test Set, Pct(%)\n";

        sb.append(header);

        long pctSum = 0;
        long bugCount = 0;

        for (Bug bug : bugs) {
            var c1 = CoverageParser.getTestCount(bug, triggeringMode);
            if (c1 == 0) {
                continue;
            }
            var c2 = cache.computeIfAbsent(bug, k -> CoverageParser.getTestCount(bug, TriggeringMode.COMPLETE));
            var pct = Math.round(100.0 * c1 / c2);
            pctSum += pct;
            bugCount++;
            executedTestCount += c1;
            completedTestCount += c2;
            sb.append(String.format("%s, %s, %d, %d, %d\n", triggeringMode, bug.getName(), c1, c2, pct));
            LOG.info("Done triggering mode = {}, bug = {}", triggeringMode, bug.getName());
        }

        sb.append("\n\n");

        sb.append("Average 1, Average 2\n");

        var averagePct1 = pctSum / bugCount;
        var averagePct2 = Math.round(100.0 * executedTestCount / completedTestCount);

        sb.append(String.format("%d, %d", averagePct1, averagePct2));

        try {
            Files.writeString(Paths.get(bugType.getCsvTestCountDir(), triggeringMode.name() + ".csv"), sb.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOG.info("Done bug type = {}, triggering mode = {}", bugType, triggeringMode);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (BugType bugType : BugType.values()) {
            for (TriggeringMode triggeringMode : TriggeringMode.values()) {
                executorService.submit(() -> calPortionPercentage(triggeringMode, bugType));
            }
        }
    }
}
