package edu.vt.cs.ranking;

import edu.vt.cs.models.Project;
import edu.vt.cs.utils.BugParser;
import edu.vt.cs.utils.CoverageParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RankerTest {

    @Test
    void rank() throws IOException {
        var bug = BugParser.parse(BugParser.isRealBugFromDefect4JProjects)
                .stream()
                .filter(b -> b.getProject() == Project.Closure && b.getBugId() == 61)
                .findFirst()
                .orElseThrow();

        var ranker = new Ranker();
        var spectrum = CoverageParser.parse(bug);
        var rankedList = ranker.rank(RankingAlgorithm.AMPLE, spectrum).getRankedEntitiesList();

        assertNotNull(rankedList);

        rankedList.stream().limit(10).forEach(System.out::println);
    }
}
