package edu.vt.cs.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.vt.cs.models.Project;
import edu.vt.cs.ranking.RankingAlgorithm;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableEvalResult.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface EvalResult {
    @Value.Parameter
    Project getProject();

    @Value.Parameter
    int getBugId();

    @Value.Parameter
    @Nullable
    TriggeringMode getTriggeringMode();

    @Value.Parameter
    @Nullable
    RankingAlgorithm getRankingAlgorithm();

    @Value.Parameter
    @JsonIgnore
    Metrics getMetrics();

    default String getCsvFormat() {
        return String.format("%s, %d, %s, %s, %d, %d, %d, %f, %f, %s",
                getProject().name(),
                getBugId(),
                getTriggeringMode() == null ? "Unknown" : getTriggeringMode().name(),
                getRankingAlgorithm() == null ? "Unknown" : getRankingAlgorithm().name(),
                getMetrics().getTop1(),
                getMetrics().getTop5(),
                getMetrics().getTop10(),
                getMetrics().getMap(),
                getMetrics().getMrr(),
                getMetrics().getRanks()
        );
    }

    static EvalResult parseLine(String line) {
        var parts = line.split(",");
        String projectName = parts[0].trim();
        int bugId = Integer.parseInt(parts[1].trim());
        String triggeringModeName = parts[2].trim();
        String rankingAlgorithName = parts[3].trim();

        int top1 = Integer.parseInt(parts[4].trim());
        int top5 = Integer.parseInt(parts[5].trim());
        int top10 = Integer.parseInt(parts[6].trim());
        double map = Double.parseDouble(parts[7].trim());
        double mrr = Double.parseDouble(parts[8].trim());

        var startIdx = line.indexOf("[");
        var endIdx = line.indexOf("]");

        List<Integer> ranks = Stream.of(line.substring(startIdx + 1, endIdx).split(","))
                .map(String::trim)
                .map(Integer::parseInt).toList();

        var metrics = ImmutableMetrics.builder()
                .top1(top1)
                .top5(top5)
                .top10(top10)
                .map(map)
                .mrr(mrr)
                .ranks(ranks)
                .build();

        return ImmutableEvalResult.builder()
                .project(Project.valueOf(projectName))
                .bugId(bugId)
                .triggeringMode(TriggeringMode.valueOf(triggeringModeName))
                .rankingAlgorithm(RankingAlgorithm.valueOf(rankingAlgorithName))
                .metrics(metrics)
                .build();
    }
}
