package edu.vt.cs.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.vt.cs.evaluation.TriggeringMode;
import edu.vt.cs.ranking.RankingAlgorithm;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableSpectrum.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Spectrum {

    @Value.Parameter
    @JsonIgnore
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

    static Spectrum getEmptySpectrum(Bug bug) {
        return ImmutableSpectrum.builder()
                .bug(bug)
                .project(bug.getProject())
                .totalOfPassedTests(0)
                .totalOfFailedTests(0)
                .entities(List.of())
                .build();
    }

    default String getName() {
        return getProject().name() + "::"
                + getBug().getBugId() + "::"
                + (getTriggeringMode() == null ? "Not Set" : getTriggeringMode().name() + "::")
                + (getRankingAlgorithm() == null ? "Not set" : getRankingAlgorithm().name());
    }
}
