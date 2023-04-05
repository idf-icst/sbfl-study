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

    @Value.Parameter
    public abstract double getTop1Pct();

    @Value.Parameter
    public abstract double getTop5Pct();

    @Value.Parameter
    public abstract double getTop10Pct();

    @Value.Parameter
    public abstract double getMapPct();

    @Value.Parameter
    public abstract double getMrrPct();

    public String toCsvLine() {
        return String.format("%d, %d, %d, %.2f, %.2f, %d, %.2f, %.2f, %.2f, %.2f, %.2f", getTop1(), getTop5(), getTop10(),
                getMap(), getMrr(), getCount(), getTop1Pct(), getTop5Pct(), getTop10Pct(), getMapPct(), getMrrPct());
    }

    public static String toScvHeaderLine() {
        return "Top 1(#), Top 5(#), Top 10(#), MAP(#), MRR(#), Instances(#), " +
                "Top 1(%), Top 5(%), Top 10(%), MAP(%), MRR(%)\n";
    }
}
