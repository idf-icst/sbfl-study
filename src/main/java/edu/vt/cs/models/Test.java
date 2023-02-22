package edu.vt.cs.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableTest.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Test {

    @Value.Parameter
    Result getResult();

    /**
     *
     * If this test covers location index i
     */
    @Value.Parameter
    List<Boolean> getCoverageVector();

    /**
     * Total locations in a program snapshot is the same for each test
     */
    @Value.Parameter
    int getNumberOfLocations();
}
