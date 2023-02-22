package edu.vt.cs.utils;

import edu.vt.cs.models.Entity;
import edu.vt.cs.models.ImmutableEntity;
import edu.vt.cs.models.ImmutableSpectrum;
import edu.vt.cs.models.ImmutableTest;
import edu.vt.cs.models.Project;
import edu.vt.cs.models.Result;
import edu.vt.cs.models.Spectrum;
import edu.vt.cs.models.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static edu.vt.cs.models.Constants.COVERED;
import static edu.vt.cs.models.Constants.GZOLT_ROOT;
import static edu.vt.cs.models.Constants.MATRIX_FILE_NAME;
import static edu.vt.cs.models.Constants.PASSED_SYMBOL;
import static edu.vt.cs.models.Constants.SPECTRA_FILE_NAME;
import static edu.vt.cs.models.Result.Failed;
import static edu.vt.cs.models.Result.Passed;

/**
 * This class build spectrum representation of a project at a snapshot of bugId
 */
public class CoverageParser {

    public static Spectrum parse(Project project, String bugId) throws IOException {

        var executedTests = Files.readAllLines(Paths.get(GZOLT_ROOT, project.name(), bugId, MATRIX_FILE_NAME))
                .stream()
                .map(toExecutedTest)
                .toList();

        var fqnMappings = Files.readAllLines(Paths.get(GZOLT_ROOT, project.name(), bugId, SPECTRA_FILE_NAME));

        return from(Project.valueOf(project.name()), executedTests, fqnMappings);
    }

    private static final Function<String, Test> toExecutedTest = testCoverageLine -> {
        String[] coverageVector = testCoverageLine.split("\s");

        List<Boolean> coverageLocations = Arrays.stream(coverageVector)
                .limit(coverageVector.length - 1)
                .map(COVERED::equals)
                .toList();

        Result result = PASSED_SYMBOL.equals(coverageVector[coverageVector.length - 1])
                ? Passed
                : Failed;

        return ImmutableTest.of(result, coverageLocations, coverageLocations.size());
    };

    private static Spectrum from(Project project, List<Test> executedTests, List<String> fqnMappings) {

        int totalLocations = executedTests.get(0).getNumberOfLocations();

        int totalTests = executedTests.size();

        int totalFailedTests = (int) executedTests.stream()
                .filter(executedTest -> executedTest.getResult() == Failed)
                .count();

        int totalPassedTests = totalTests - totalFailedTests;

        List<Entity> entities = new ArrayList<>(totalLocations);

        for (int i=0; i<totalLocations; i++) {

            int passedTests = 0;
            int failedTests = 0;

            for (Test executedTest : executedTests) {
                boolean isLocationCovered = executedTest.getCoverageVector().get(i);

                if (!isLocationCovered) {
                    continue;
                }

                var testResult = executedTest.getResult();

                if (testResult == Passed) {
                    passedTests++;
                } else {
                    failedTests++;
                }
            }

            var entity = ImmutableEntity.builder()
                    .id(i)
                    .numberOfFailedTests(failedTests)
                    .numberOfPassedTests(passedTests)
                    .fQN(fqnMappings.get(i))
                    .build();

            entities.add(entity);
        }

        return ImmutableSpectrum.builder()
                .project(project)
                .entities(entities)
                .totalOfFailedTests(totalFailedTests)
                .totalOfPassedTests(totalPassedTests)
                .build();
    }
}
