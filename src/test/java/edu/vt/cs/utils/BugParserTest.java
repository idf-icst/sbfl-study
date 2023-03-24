package edu.vt.cs.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class BugParserTest {

    @Test
    @Disabled
    void parse() throws IOException {
        BugParser.parse(BugParser.isRealBugFromDefect4JProjects).stream().limit(10).forEach(System.out::println);
    }

    @Test
    @Disabled
    void exportGroundTruthsToFile() throws IOException {
        BugParser.exportGroundTruthsToFile("src/main/resources/bugs/all-multi-locations-bugs.txt", BugParser.isRealBugFromDefect4JProjects);
    }
}
