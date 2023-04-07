package edu.vt.cs.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.vt.cs.evaluation.ImmutableMetrics;
import edu.vt.cs.evaluation.Metrics;
import edu.vt.cs.evaluation.TriggeringMode;
import edu.vt.cs.ranking.RankingAlgorithm;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableSpectrum.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Spectrum {

    @Value.Parameter
    @JsonIgnore
    @Nullable
    Collection<Entity> getEntities();

    @Value.Parameter
    int getTotalOfPassedTests();

    @Value.Parameter
    int getTotalOfFailedTests();

    @Value.Parameter
    Project getProject();

    @Value.Parameter
    Bug getBug();

    @Value.Parameter
    @Nullable
    TriggeringMode getTriggeringMode();

    @Value.Parameter
    @Nullable
    RankingAlgorithm getRankingAlgorithm();

    @Value.Parameter
    @Nullable
    List<Entity> getRankedEntitiesList();

    @Value.Parameter
    @Value.Default
    default boolean getIsEmpty() {
        return false;
    }

    static Spectrum getEmptySpectrum(Bug bug, TriggeringMode triggeringMode) {
        return ImmutableSpectrum.builder()
                .bug(bug)
                .triggeringMode(triggeringMode)
                .project(bug.getProject())
                .totalOfPassedTests(0)
                .totalOfFailedTests(0)
                .entities(List.of())
                .isEmpty(true)
                .build();
    }

    @Nullable
    @JsonIgnore
    default String getName() {
        return getProject().name() + "::"
                + getBug().getBugId() + "::"
                + (getTriggeringMode() == null ? "Not Set" : getTriggeringMode().name() + "::")
                + (getRankingAlgorithm() == null ? "Not set" : getRankingAlgorithm().name());
    }

    @JsonIgnore
    default Metrics compute() {
        Map<String, Integer> rankingMap = new HashMap<>();

        for (int i=0; i<getRankedEntitiesList().size(); i++) {
            var fqn = getRankedEntitiesList().get(i).getFQN();
            rankingMap.put(fqn, i + 1);
        }

        int top1 = 0;
        int top5 = 0;
        int top10 = 0;
        double map = 0;
        double mrr = 0;

        var ranks = getBug().getLocations()
                .stream()
                .map(l -> {
                    var fqn = (l.contains("#")
                            ? l.substring(0, l.lastIndexOf('#')).replaceAll("/", ".")
                            : l).trim().replace(".java", "");
                    return fqn;
                })
                .map(l -> rankingMap.getOrDefault(l, Integer.MAX_VALUE))
                .sorted()
                .toList();

        int pos = (int) ranks.stream().filter(rank -> rank < Integer.MAX_VALUE).count();

        int pk = 0;

        for (int k=1; k<=ranks.size(); k++) {
            int rank = ranks.get(k-1);

            if (rank <= 10) {
                top10++;
                if (rank <= 5) {
                    top5++;
                    if (rank == 1) {
                        top1++;
                    }
                }
            }

            if (rank < Integer.MAX_VALUE) {
                pk++;
                map += ((double) pk) / rank;
            }
        }

        map = pos == 0 ? 0 : map / pos;

        mrr = ((double) 1) / ranks.get(0);

        return ImmutableMetrics.builder()
                .top1(top1)
                .top5(top5)
                .top10(top10)
                .map(map)
                .mrr(mrr)
                .ranks(ranks)
                .build();
    }

    @JsonIgnore
    default int getTotalExecutedTests() {
        return this.getTotalOfPassedTests() + this.getTotalOfFailedTests();
    }
}
