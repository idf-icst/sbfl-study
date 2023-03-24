package edu.vt.cs.utils;

import edu.vt.cs.models.Project;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static edu.vt.cs.utils.BugParser.isRealBugFromDefect4JProjects;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CoverageParserTest {
    @Test
    void parse() throws IOException {
        var bug = BugParser.parse(isRealBugFromDefect4JProjects)
                .stream()
                .filter(b -> b.getProject() == Project.Closure && b.getBugId() == 61)
                .findFirst()
                .orElseThrow();

        var spectrum = CoverageParser.parse(bug);
        assertEquals(15267, spectrum.getEntities().size());
        assertEquals(3, spectrum.getTotalOfFailedTests());
        assertEquals(5984, spectrum.getTotalOfPassedTests());
    }
}
