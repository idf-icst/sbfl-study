package edu.vt.cs.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static edu.vt.cs.models.Constants.dest;
import static edu.vt.cs.models.Constants.src;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CoverageDataReaderTest {
    @ParameterizedTest
    @MethodSource("defaultSourceDestProvider")
    @Disabled
    void etlGzoltarCoverageDataTest(String sourcePath, String destPath, int noOfFiles)
            throws IOException, InterruptedException {
        CoverageDataReader.etlGzoltarCoverageData(sourcePath, destPath, noOfFiles);
    }

    static Stream<Arguments> defaultSourceDestProvider() {
        return Stream.of(arguments(src, dest, 1));
    }
}
