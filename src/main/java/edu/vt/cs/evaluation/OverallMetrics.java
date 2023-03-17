package edu.vt.cs.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableOverallMetrics.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class OverallMetrics extends Metrics {
    @Value.Parameter
    public abstract int getCount();

    public String toCsvLine() {
        return String.format("%d, %d, %d, %f, %f, %d", getTop1(), getTop5(), getTop10(),
                getMap(), getMrr(), getCount());
    }
}
