package edu.vt.cs.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableEntity.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Entity {

    @Value.Parameter
    int getId();

    @Value.Parameter
    String getFQN();

    /**
     *
     * Number of failed tests that cover this entity
     */
    @Value.Parameter
    int getNumberOfFailedTests();

    /**
     *
     * Number of passed tests that cover this entity
     */
    @Value.Parameter
    int getNumberOfPassedTests();

    @Value.Parameter
    @Nullable
    Double getScore();
}
