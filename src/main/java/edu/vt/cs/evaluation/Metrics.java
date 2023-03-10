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
    public abstract double getMAP();

    @Value.Parameter
    public abstract int getTop1();

    @Value.Parameter
    public abstract int getTop5();

    @Value.Parameter
    public abstract int getTop10();

    @Value.Parameter
    public abstract double getMRR();

    @Value.Parameter
    public abstract int getAlgos();

    @Value.Default
    @Override
    public String toString() {
        var map = getMAP() / getAlgos();
        var mrr = getMRR() / getAlgos();
        return String.format("%9.0f", Double.isNaN(map) ? 0.0 : map) + " : "
                + String.format("%9.0f", (double) getTop1() / getAlgos()) + " : "
                + String.format("%9.0f", (double) getTop5() / getAlgos()) + " : "
                + String.format("%9.0f", (double) getTop10() / getAlgos()) + " : "
                + String.format("%9.0f", Double.isNaN(getMRR()) ? 0.0 : getMRR());
    }
}
