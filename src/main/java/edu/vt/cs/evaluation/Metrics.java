package edu.vt.cs.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableMetrics.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Metrics {
    @Value.Parameter
    public abstract int getTop1();

    @Value.Parameter
    public abstract int getTop5();

    @Value.Parameter
    public abstract int getTop10();

    @Value.Parameter
    public abstract double getMap();

    @Value.Parameter
    public abstract double getMrr();
}
