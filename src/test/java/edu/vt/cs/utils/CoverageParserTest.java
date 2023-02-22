package edu.vt.cs.utils;

import edu.vt.cs.models.Project;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoverageParserTest {
    @Test
    void parse() throws IOException {
        var spectrum = CoverageParser.parse(Project.Closure, "61");
        assertEquals(15267, spectrum.getEntities().size());
        assertEquals(3, spectrum.getTotalOfFailedTests());
        assertEquals(5984, spectrum.getTotalOfPassedTests());
    }
}